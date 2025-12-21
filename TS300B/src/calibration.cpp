#include <Arduino.h>
#include <Preferences.h>

// ==========================================================
// AZDM01 自动校准程序
// ==========================================================
// 功能：自动调节红外 LED 亮度 (PWM)，使传感器输出电压达到目标值 (3.8V)
// 结果：将最佳 PWM 占空比保存到 NVS，供主程序使用
// ==========================================================

// --- 引脚定义 ---
#define TURBIDITY_PIN 36 // 传感器输出 (ADC)
#define IR_ADJ_PIN 13    // 红外调节 (PWM)

// --- PWM 配置 ---
#define PWM_CHANNEL 0
#define PWM_FREQ 5000
#define PWM_RESOLUTION 8

// --- 校准目标 ---
// 目标：清水环境下，传感器输出电压应为 3.8V
const float TARGET_VOLTAGE = 3.8;

// --- 电压分压系数 ---
// ESP32 ADC 最大量程为 3.3V。如果传感器输出能达到 3.8V，
// 必须使用分压电阻将电压降至 3.3V 以下。
// 例如：使用两个等值电阻分压 (10k:10k)，则系数为 2.0 (读到 1.9V 代表实际 3.8V)
// 如果没有分压电阻 (直接连接)，请务必注意 ESP32 引脚不能承受 > 3.3V 电压！
// 这里默认设为 1.0 (假设您已处理好硬件电平匹配，或者目标电压就在 3.3V 以内)
const float VOLTAGE_DIVIDER_RATIO = 2.0;

Preferences preferences;
const char *NVS_NAMESPACE = "sts_config";
const char *NVS_KEY_IR_DUTY = "ir_duty";

void setup()
{
    Serial.begin(115200);
    delay(1000);

    Serial.println("\n\n===========================================");
    Serial.println("      AZDM01 传感器自动校准程序");
    Serial.println("===========================================");

    // 1. 初始化引脚
    pinMode(TURBIDITY_PIN, INPUT);

    ledcSetup(PWM_CHANNEL, PWM_FREQ, PWM_RESOLUTION);
    ledcAttachPin(IR_ADJ_PIN, PWM_CHANNEL);

    // 2. 提示用户
    Serial.println("请确保：");
    Serial.println("1. 传感器已完全浸入【清水】中");
    Serial.println("2. 环境光线较暗 (避光)");
    Serial.println("3. 硬件连接正确 (GPIO 36: OUT, GPIO 13: IR_ADJ)");
    Serial.println("-------------------------------------------");
    Serial.println("将在 5 秒后开始校准...");
    delay(5000);

    // 3. 开始搜索最佳 PWM
    Serial.println("开始搜索最佳红外亮度...");

    int bestDuty = -1;
    float minDiff = 100.0;
    float bestVoltage = 0.0;

    // 遍历 PWM 0 - 255
    for (int duty = 0; duty <= 255; duty++)
    {
        ledcWrite(PWM_CHANNEL, duty);
        delay(50); // 等待稳定

        // 读取电压 (多次平均)
        long sum = 0;
        for (int i = 0; i < 20; i++)
        {
            sum += analogRead(TURBIDITY_PIN);
            delay(1);
        }
        float adcValue = sum / 20.0;
        float pinVoltage = adcValue * (3.3 / 4095.0);
        float actualVoltage = pinVoltage * VOLTAGE_DIVIDER_RATIO;

        float diff = abs(actualVoltage - TARGET_VOLTAGE);

        // 打印进度 (每 10 个点打印一次，避免刷屏)
        if (duty % 10 == 0)
        {
            Serial.printf("Duty: %3d | Voltage: %.2f V | Diff: %.2f\n", duty, actualVoltage, diff);
        }

        if (diff < minDiff)
        {
            minDiff = diff;
            bestDuty = duty;
            bestVoltage = actualVoltage;
        }
    }

    // 4. 结果判定
    Serial.println("-------------------------------------------");
    if (bestDuty != -1)
    {
        Serial.printf("校准完成！\n");
        Serial.printf("最佳 PWM 占空比: %d\n", bestDuty);
        Serial.printf("对应电压: %.2f V (目标 %.2f V)\n", bestVoltage, TARGET_VOLTAGE);

        // 5. 保存到 NVS
        if (preferences.begin(NVS_NAMESPACE, false))
        {
            preferences.putInt(NVS_KEY_IR_DUTY, bestDuty);
            preferences.end();
            Serial.println(">> 参数已成功保存到 NVS (Flash) <<");
            Serial.println("您可以烧录主程序了。");
        }
        else
        {
            Serial.println("!! NVS 保存失败 !!");
        }
    }
    else
    {
        Serial.println("校准失败，未找到合适的值。");
    }
    Serial.println("===========================================");
}

void loop()
{
    // 校准完成后，什么都不做，只是闪烁 LED 提示
    delay(1000);
}
