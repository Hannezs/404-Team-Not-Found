#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <Ticker.h>
#include <time.h>
#include <Preferences.h> // <-- 用于 NVS 持久化存储
#include <ArduinoJson.h> // <-- 用于解析配置

// ==========================================================
// 1. WiFi 配置
// ==========================================================
const char *ssid = "123";          // 您的 WiFi 名称
const char *password = "zhang666"; // 您的 WiFi 密码

// ==========================================================
// 2. 服务器配置 (非常重要!!!)
// ==========================================================
// 如果您的 Spring Boot 在本地电脑运行，请填写电脑的局域网 IP (如 192.168.x.x)
// 如果您的 Spring Boot 在阿里云/腾讯云，请填写公网 IP，并确保【安全组】已开放 8080 端口
const char *server_ip = "47.92.234.121";
const int server_port = 8080;

// --- 后端 API 路径 ---
const char *API_UPLOAD_DATA = "/api/devices/upload";
const char *API_REGISTER_DEVICE = "/public/api/devices/register";
const char *API_GET_CONFIG = "/public/api/devices/config/"; // 后面会拼接 MAC 地址

// --- NVS (持久化存储) 键名 ---
Preferences preferences;
const char *NVS_NAMESPACE = "sts_config";
const char *NVS_KEY_APIKEY = "api_key";
const char *NVS_KEY_SOURCEID = "source_id";

// ==========================================================
// 3. 传感器配置
// ==========================================================

// [修改] 使用 GPIO 36 (Sensor VP)，这是 ADC1 通道，WiFi 工作时也能使用
// 原来的 GPIO 16 是 ADC2，开启 WiFi 后无法使用
#define TURBIDITY_PIN 36

// [新增] 红外 LED 调节引脚 (PWM)
// 注意：使用红板模块时，通常不需要外部控制红外 LED，它由模块内部控制。
// 因此这里虽然定义了，但实际接线时 GPIO 13 可以悬空。
#define IR_ADJ_PIN 13
#define PWM_CHANNEL 0
#define PWM_FREQ 5000
#define PWM_RESOLUTION 8

// --- 电压分压系数 ---
// [修改] 3.3V 供电模式下，红板输出电压 < 3.3V，无需分压电阻。
// 因此系数设为 1.0 (直连)
const float VOLTAGE_DIVIDER_RATIO = 1.0;

// --- [新增] 电压基准补偿 ---
// 3.3V 供电时，清水电压会大幅下降 (预计在 2.5V - 3.0V 之间)。
// 必须设置较大的偏移量，将电压"拉"回 4.2V，才能套用标准公式。
// 计算公式: 偏移量 = 4.2 - 实际清水电压
// 预设值: 假设清水电压为 2.8V，则偏移量 = 1.4
// ⚠️ 请务必在串口监视器查看 [SENSOR] SensorV 的值，然后修改此处！
float configVoltageOffset = 1.4;         // 改为变量，支持动态校准
const char *NVS_KEY_OFFSET = "v_offset"; // NVS 键名

const char *turbidity_unit = "NTU"; // 调试阶段建议先用电压(V)作为单位，校准后再改回 NTU

// --- NVS 键名补充 ---
const char *NVS_KEY_IR_DUTY = "ir_duty"; // 保存 PWM 占空比

// --- NTP (网络时间) 配置 ---
const char *ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 8 * 3600; // GMT+8
const int daylightOffset_sec = 0;

// --- 全局对象和状态机 ---
enum DeviceState
{
  STATE_BOOTING,
  STATE_PROVISIONING,
  STATE_RUNNING,
  STATE_ERROR
};
DeviceState currentState = STATE_BOOTING;

HTTPClient http;
Ticker dataUploader;    // 用于 RUNNING 状态
Ticker provisionPoller; // 用于 PROVISIONING 状态

String deviceMacAddress;  // "设备码"
String configApiKey;      // 从 NVS 加载的 Key
String configSourceStrId; // 从 NVS 加载的 Source ID
int currentIrDuty = 128;  // 当前红外 LED 亮度 (0-255)

// ==========================================================
// 辅助函数
// ==========================================================

// --- 初始化 PWM ---
void setupPWM()
{
  // 使用红板模块时，通常不需要 ESP32 输出 PWM。
  // 但为了兼容性，我们还是初始化它，只是您可以选择不接线。
  ledcSetup(PWM_CHANNEL, PWM_FREQ, PWM_RESOLUTION);
  ledcAttachPin(IR_ADJ_PIN, PWM_CHANNEL);
  ledcWrite(PWM_CHANNEL, currentIrDuty);
}

// --- 获取 MAC 地址 (设备码) ---
String getMacAddress()
{
  String mac = WiFi.macAddress();
  mac.replace(":", ""); // 移除冒号，转为 "AABBCC112233" 格式
  mac.toLowerCase();
  return mac;
}

// --- WiFi 连接 ---
bool setupWifi()
{
  delay(10);
  Serial.println("正在连接 WiFi...");
  WiFi.begin(ssid, password);
  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 20)
  {
    Serial.print(".");
    delay(500);
    retries++;
  }
  if (WiFi.status() == WL_CONNECTED)
  {
    Serial.println("\nWiFi 连接成功");
    Serial.print("IP 地址: ");
    Serial.println(WiFi.localIP());

    // [优化] 关闭 WiFi 节能模式，防止 WiFi 射频突发导致 ADC 电压波动
    WiFi.setSleep(false);

    return true;
  }
  else
  {
    Serial.println("\nWiFi 连接失败!");
    return false;
  }
}

// --- NTP 时间同步 ---
bool setupTime()
{
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
  Serial.print("正在同步网络时间(NTP)...");
  struct tm timeinfo;
  int retries = 0;
  while (!getLocalTime(&timeinfo) && retries < 10)
  {
    Serial.print(".");
    delay(500);
    retries++;
  }
  if (getLocalTime(&timeinfo))
  {
    Serial.println("\nNTP 时间同步成功");
    return true;
  }
  else
  {
    Serial.println("\nNTP 时间同步失败!");
    return false;
  }
}

// --- 获取 ISO 8601 格式的时间戳 ---
String getTimestamp()
{
  struct tm timeinfo;
  if (!getLocalTime(&timeinfo))
  {
    Serial.println("获取本地时间失败");
    return "";
  }
  char buffer[20];
  strftime(buffer, sizeof(buffer), "%Y-%m-%dT%H:%M:%S", &timeinfo);
  return String(buffer);
}

// --- 从 NVS 读取配置 ---
bool readConfig()
{
  Serial.println("正在从 NVS 读取配置...");
  if (!preferences.begin(NVS_NAMESPACE, true))
  { // true = 只读
    Serial.println("  NVS 打开失败 (只读)");
    return false;
  }

  configApiKey = preferences.getString(NVS_KEY_APIKEY, "");
  configSourceStrId = preferences.getString(NVS_KEY_SOURCEID, "");

  // 读取保存的 PWM 占空比，默认为 128
  currentIrDuty = preferences.getInt(NVS_KEY_IR_DUTY, 128);

  // [新增] 读取保存的电压偏移量
  float savedOffset = preferences.getFloat(NVS_KEY_OFFSET, -1.0);
  if (savedOffset > 0)
  {
    configVoltageOffset = savedOffset;
    Serial.printf("  加载电压偏移量: %.2f\n", configVoltageOffset);
  }
  else
  {
    Serial.printf("  使用默认电压偏移量: %.2f\n", configVoltageOffset);
  }

  // [安全检查] 如果读取到的值为 0 (可能是校准失败导致的)，则强制恢复为默认值 128
  if (currentIrDuty <= 0)
  {
    currentIrDuty = 128;
    Serial.println("  [警告] 检测到无效的红外亮度值 (0)，已重置为默认值 128");
  }

  Serial.printf("  加载红外 LED 亮度: %d\n", currentIrDuty);

  preferences.end();

  if (configApiKey.length() > 0 && configSourceStrId.length() > 0)
  {
    Serial.println("  配置已找到!");
    return true;
  }
  else
  {
    Serial.println("  配置未找到。");
    return false;
  }
}

// --- 将配置写入 NVS ---
bool saveConfig(String apiKey, String sourceId)
{
  Serial.println("正在向 NVS 写入配置...");
  if (!preferences.begin(NVS_NAMESPACE, false))
  { // false = 读写
    Serial.println("  NVS 打开失败 (读写)");
    return false;
  }

  if (preferences.putString(NVS_KEY_APIKEY, apiKey) == 0)
  {
    Serial.println("  API Key 写入失败!");
    preferences.end();
    return false;
  }

  if (preferences.putString(NVS_KEY_SOURCEID, sourceId) == 0)
  {
    Serial.println("  Source ID 写入失败!");
    preferences.end();
    return false;
  }

  preferences.end();
  Serial.println("  配置写入成功。");
  return true;
}

// ==========================================================
// 状态机核心功能
// ==========================================================

/**
 * [PROVISIONING 状态] 任务 1:
 * 向后端注册本设备
 */
void registerDeviceWithBackend()
{
  if (WiFi.status() != WL_CONNECTED)
    return;

  // 使用 ArduinoJson 构造 JSON，更安全
  JsonDocument doc;
  doc["macAddress"] = deviceMacAddress;
  String payload;
  serializeJson(doc, payload);

  String server_path = "http://" + String(server_ip) + ":" + String(server_port) + String(API_REGISTER_DEVICE);

  Serial.println("正在向后端注册设备...");
  Serial.print("Payload: ");
  Serial.println(payload);

  WiFiClient client;
  http.begin(client, server_path);
  http.addHeader("Content-Type", "application/json");

  int httpCode = http.POST(payload);

  if (httpCode == HTTP_CODE_OK || httpCode == HTTP_CODE_CREATED)
  {
    Serial.println("设备注册成功 (或已注册)。");
  }
  else
  {
    Serial.printf("[HTTP] 设备注册失败, 状态码: %d\n", httpCode);
    String response = http.getString();
    Serial.println(response);
  }
  http.end();
}

/**
 * [PROVISIONING 状态] 任务 2: (由 Ticker 定期调用)
 * 轮询后端以获取配置
 */
void pollForConfig()
{
  if (WiFi.status() != WL_CONNECTED)
    return;

  String server_path = "http://" + String(server_ip) + ":" + String(server_port) + String(API_GET_CONFIG) + deviceMacAddress;

  Serial.print("轮询配置: ");
  Serial.println(server_path);

  WiFiClient client;
  http.begin(client, server_path);
  int httpCode = http.GET();

  if (httpCode == HTTP_CODE_OK)
  {
    // --- 成功！用户已绑定 ---
    Serial.println("\n配置已获取!");
    String payload = http.getString();
    Serial.println(payload);

    // 停止轮询
    provisionPoller.detach();

    // 解析 JSON
    JsonDocument doc;
    DeserializationError error = deserializeJson(doc, payload);

    if (error)
    {
      Serial.print("JSON 解析失败: ");
      Serial.println(error.c_str());
      Serial.println("将继续轮询...");
      provisionPoller.attach_ms(15000, pollForConfig); // 15秒后重试
      http.end();
      return;
    }

    // 提取配置
    const char *apiKey = doc["deviceApiKey"];
    const char *sourceId = doc["sourceStrId"];

    if (apiKey && sourceId)
    {
      if (saveConfig(String(apiKey), String(sourceId)))
      {
        Serial.println("配置保存成功。设备将在 3 秒后重启...");
        delay(3000);
        ESP.restart();
      }
      else
      {
        Serial.println("配置保存失败! 正在重启...");
        delay(3000);
        ESP.restart();
      }
    }
    else
    {
      Serial.println("收到的 JSON 格式不正确 (未找到 deviceApiKey 或 sourceStrId)。");
      provisionPoller.attach_ms(15000, pollForConfig);
    }
  }
  else if (httpCode == HTTP_CODE_NOT_FOUND)
  {
    Serial.println("  设备尚未被认领。");
  }
  else
  {
    Serial.printf("[HTTP] 轮询失败, 状态码: %d\n", httpCode);
  }
  http.end();
}

/**
 * [RUNNING 状态] 任务 1: (由 Ticker 定期调用)
 * 发送传感器数据
 */
void sendHttpData()
{
  if (WiFi.status() != WL_CONNECTED)
  {
    Serial.println("[DATA] WiFi 未连接，跳过发送");
    return;
  }

  // --- 1. 修正后的传感器读取逻辑 ---
  // sensors.requestTemperatures(); // 已移除

  // [优化] ADC 预读取：先空读几次，让 ADC 电容充放电稳定，丢弃这些数据
  for (int k = 0; k < 5; k++)
  {
    analogReadMilliVolts(TURBIDITY_PIN);
    delay(2);
  }

  // [优化] 采样滤波：使用"截断平均值" (Trimmed Mean) 算法
  // 采集 40 个样本，排序后去掉最大的 10 个和最小的 10 个，取中间 20 个的平均值
  // 这种方法比简单的"去极值平均"更能抵抗突发噪声
  const int numSamples = 40;
  int samples[numSamples];

  for (int i = 0; i < numSamples; i++)
  {
    samples[i] = analogReadMilliVolts(TURBIDITY_PIN);
    delay(10); // 10ms 间隔
  }

  // 冒泡排序 (样本量小，冒泡足够快)
  for (int i = 0; i < numSamples - 1; i++)
  {
    for (int j = 0; j < numSamples - i - 1; j++)
    {
      if (samples[j] > samples[j + 1])
      {
        int temp = samples[j];
        samples[j] = samples[j + 1];
        samples[j + 1] = temp;
      }
    }
  }

  // 计算中间 20 个样本的平均值 (去掉头尾各 10 个)
  long mvSum = 0;
  int validCount = 0;
  for (int i = 10; i < 30; i++)
  {
    mvSum += samples[i];
    validCount++;
  }

  float avgMv = (float)mvSum / validCount;

  // 转换为电压 (V)
  float pinVoltage = avgMv / 1000.0;

  // 2. 还原传感器真实输出电压 (考虑分压电阻)
  float sensorVoltage = pinVoltage * VOLTAGE_DIVIDER_RATIO;

  // [新增] 应用电压偏移补偿 (用于校准基准值)
  float calcVoltage = sensorVoltage + configVoltageOffset;

  // 3. 计算浊度 (NTU) - 使用标准二次多项式公式
  // 公式: NTU = -1120.4*V^2 + 5742.3*V - 4352.9
  float turbidity_value = 0.0;

  if (calcVoltage >= 4.2)
  {
    turbidity_value = 0.0; // 清水
  }
  else if (calcVoltage <= 2.5)
  {
    turbidity_value = 3000.0; // 超过量程 (极浑浊)
  }
  else
  {
    turbidity_value = -1120.4 * (calcVoltage * calcVoltage) + 5742.3 * calcVoltage - 4352.9;
  }

  // 确保不出现负数
  if (turbidity_value < 0)
    turbidity_value = 0;

  Serial.printf("[SENSOR] PinV: %.2f V, SensorV: %.2f V, CalcV: %.2f V, NTU: %.2f\n",
                pinVoltage, sensorVoltage, calcVoltage, turbidity_value);

  // --- 2. 获取时间戳 ---
  String timestamp = getTimestamp();
  if (timestamp == "")
  {
    Serial.println("[DATA] 时间未同步，跳过发送");
    if (!setupTime())
      return;
    timestamp = getTimestamp();
    if (timestamp == "")
      return;
  }

  // --- 3. 使用 ArduinoJson 构造 Payload (更安全) ---
  JsonDocument doc;
  doc["sourceStrId"] = configSourceStrId;
  doc["turbidityValue"] = turbidity_value;
  doc["unit"] = turbidity_unit;
  doc["measuredAt"] = timestamp;

  String payload;
  serializeJson(doc, payload);

  Serial.print("[DATA] 准备发送 Payload: ");
  Serial.println(payload);

  // --- 4. 发送 HTTP POST 请求 ---
  String server_path = "http://" + String(server_ip) + ":" + String(server_port) + String(API_UPLOAD_DATA);

  WiFiClient client;
  http.begin(client, server_path);

  http.addHeader("Content-Type", "application/json");
  http.addHeader("Authorization", "ApiKey " + configApiKey);

  int httpCode = http.POST(payload);

  // 7. 处理响应
  if (httpCode > 0)
  {
    String response = http.getString();
    Serial.printf("[HTTP] 状态码: %d\n", httpCode);
    // Serial.printf("[HTTP] 响应: %s\n", response.c_str());
  }
  else
  {
    Serial.printf("[HTTP] POST 失败, 错误: %s\n", http.errorToString(httpCode).c_str());
  }
  http.end();
}

// ==========================================================
// SETUP 和 LOOP
// ==========================================================

// --- 执行清水校准 ---
void calibrateClearWater()
{
  Serial.println("\n[校准] 开始清水基准校准...");
  Serial.println("[校准] 请确保传感器已放入清水中且避光。");

  // 采集 50 次取平均
  long sum = 0;
  for (int i = 0; i < 50; i++)
  {
    sum += analogReadMilliVolts(TURBIDITY_PIN);
    delay(10);
  }
  float avgV = (sum / 50.0) / 1000.0;

  // 计算需要的偏移量
  // 目标: 加上偏移量后等于 4.2V
  float newOffset = 4.25 - avgV;

  Serial.printf("[校准] 实测电压: %.2f V\n", avgV);
  Serial.printf("[校准] 计算偏移量: %.2f (原偏移量: %.2f)\n", newOffset, configVoltageOffset);

  // 保存
  configVoltageOffset = newOffset;
  if (preferences.begin(NVS_NAMESPACE, false))
  {
    preferences.putFloat(NVS_KEY_OFFSET, configVoltageOffset);
    preferences.end();
    Serial.println("[校准] 新参数已保存到 NVS。");
  }
  else
  {
    Serial.println("[校准] NVS 保存失败!");
  }
}

void setup()
{
  Serial.begin(115200);
  delay(2000); // 等待串口稳定

  if (!setupWifi())
  {
    Serial.println("WiFi 连接失败。正在重启...");
    delay(5000);
    ESP.restart();
  }

  deviceMacAddress = getMacAddress();

  // 传感器初始化
  // sensors.begin(); // 已移除温度传感器

  // [修改] GPIO 36 是 Input Only 引脚，不支持上拉
  pinMode(TURBIDITY_PIN, INPUT);

  // 初始化 PWM (应用校准值)
  setupPWM();

  Serial.println("系统初始化完成。进入主循环...");
}

void loop()
{
  // 串口命令监听
  if (Serial.available())
  {
    String cmd = Serial.readStringUntil('\n');
    cmd.trim();
    if (cmd == "CAL")
    {
      calibrateClearWater();
    }
  }

  // 主状态机
  switch (currentState)
  {
  case STATE_BOOTING:
    Serial.println("[状态机] BOOTING...");
    if (readConfig())
    {
      // --- 找到配置 -> 进入运行模式 ---
      currentState = STATE_RUNNING;
      Serial.println("[状态机] 切换到 RUNNING");

      if (setupTime())
      {
        sendHttpData();                        // 立即发送一次
        dataUploader.attach(20, sendHttpData); // 每 20 秒发送一次
      }
      else
      {
        Serial.println("NTP 时间同步失败，无法进入 RUNNING 状态，重启...");
        delay(5000);
        ESP.restart();
      }
    }
    else
    {
      // --- 未找到配置 -> 进入配网模式 ---
      currentState = STATE_PROVISIONING;
      Serial.println("[状态机] 切换到 PROVISIONING");

      // 1. 立即注册
      registerDeviceWithBackend();

      // 2. 启动轮询 Ticker (每 15 秒)
      provisionPoller.attach_ms(15000, pollForConfig);
    }
    break;

  case STATE_PROVISIONING:
    if (WiFi.status() != WL_CONNECTED)
    {
      Serial.println("[PROV] WiFi 连接丢失! 尝试重连...");
      if (!setupWifi())
      {
        Serial.println("[PROV] 重连失败，重启...");
        delay(5000);
        ESP.restart();
      }
    }
    delay(1000);
    break;

  case STATE_RUNNING:
    if (WiFi.status() != WL_CONNECTED)
    {
      Serial.println("[RUN] WiFi 连接丢失! 尝试重连...");
      dataUploader.detach();
      if (setupWifi())
      {
        Serial.println("[RUN] 重连成功，重新同步时间...");
        if (setupTime())
        {
          dataUploader.attach(20, sendHttpData);
        }
      }
      else
      {
        Serial.println("[RUN] 重连失败，重启...");
        delay(5000);
        ESP.restart();
      }
    }
    delay(1000);
    break;

  case STATE_ERROR:
    Serial.println("[状态机] ERROR: 发生致命错误。 30 秒后重启...");
    delay(30000);
    ESP.restart();
    break;
  }
}