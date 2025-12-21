# STS (水质浊度监测系统) 后端

这是一个基于 Spring Boot 的水质浊度监测系统 (STS) 的后端项目。它负责处理用户认证、水源监测点管理、传感器数据接收、以及调用 Dify AI 工作流生成分析报告。

## 1\. 项目结构 (Packages)

项目遵循标准的 Spring Boot 分层架构：

```
com.catowl.sts
│
├── StsApplication.java         # Spring Boot 启动类
│
├── config                      # 配置包
│   ├── CorsConfig.java         # 跨域资源共享 (CORS) 配置
│   ├── DifyConfig.java         # Dify 客户端 Bean 配置
│   ├── LocalDataTimeConfig.java # 全局日期时间格式化 (String -> LocalDateTime)
│   ├── RedisConfig.java        # Redis 序列化配置 (使用 Jackson)
│   ├── SecurityConfig.java     # Spring Security 核心配置 (JWT, 路径权限)
│   ├── SwaggerConfig.java      # Swagger (OpenAPI 3.0) API 文档配置
│   └── WebConfig.java          # Web MVC 配置 (静态资源处理器，如 /uploads/**)
│
├── controller                  # API 接口层 (RestControllers)
│   ├── AuthController.java     # 1. 用户认证与管理 (注册, 登录, 获取信息, 更新头像)
│   ├── WaterSourceController.java # 2. 水源监测点 (Dify会话) 管理 (CRUD)
│   ├── WaterDataController.java   # 3. 水质数据 (上传, 查询)
│   ├── ReportController.java      # 4. 分析报告 (生成, 查询, 发布, 删除)
│   └── PublicReportController.java # 5. 公开报告 (无需认证, 供二维码扫描)
│
├── exception                   # 异常处理包
│   ├── BadRequestException.java   # 400 错误
│   ├── InternetServerException.java # 500 错误
│   ├── UnauthorizedException.java # 401 错误
│   ├── Response.java              # 错误响应体
│   └── GlobalExceptionHandler.java # 全局异常处理器 (@RestControllerAdvice)
│
├── filter                      # 过滤器
│   └── JwtAuthenticationFilter.java # JWT 认证过滤器 (解析 Token, 存入 SecurityContext)
│
├── mapper                      # MyBatis Mapper 接口 (DAO)
│   ├── AuthMapper.java         # users 表操作
│   ├── ReportMapper.java       # analysis_reports, report_data_links 表操作
│   ├── WaterDataMapper.java    # water_quality_data 表操作
│   └── WaterSourceMapper.java  # water_sources 表操作
│
├── model                       # 数据模型
│   ├── dto                     # 数据传输对象 (Data Transfer Objects)
│   │   ├── Request             # API 请求体 (e.g., UserLoginRequest)
│   │   └── Response            # API 响应体 (e.g., MyApiResponse, ReportResponse)
│   ├── entity                  # 数据库实体类 (e.g., User, WaterSource)
│   └── vo                      # 视图对象 (View Objects)
│       └── ReportWithSourceDetails.java # 用于连表查询的报告详情
│
├── service                     # 业务逻辑接口
│   ├── PublicReportService.java
│   ├── ReportService.java
│   ├── UserService.java
│   ├── WaterDataService.java
│   └── WaterSourceService.java
│
├── service.impl                # 业务逻辑实现
│   ├── MyUserDetailsServiceImpl.java # Spring Security 用户加载服务
│   ├── PublicReportServiceImpl.java # 公开报告实现 (含缓存)
│   ├── ReportServiceImpl.java     # 报告业务实现 (Dify调用, 限流, 锁, 二维码)
│   ├── UserServiceImpl.java       # 用户业务实现 (JWT, 密码加密, 文件上传)
│   ├── WaterDataServiceImpl.java  # 水质数据实现 (缓存, 限流)
│   └── WaterSourceServiceImpl.java # 水源业务实现 (Dify调用, 缓存)
│
└── utils                       # 工具类
    ├── JwtUtil.java            # JWT 生成和校验工具
    ├── MyQrCodeUtil.java       # 二维码生成工具 (ZXing)
    └── RedisCache.java         # Redis 操作封装
```

## 2\. 数据结构

### MySQL 数据库

本项目使用 `sts` 数据库，包含 5 个核心表：

1.  **`users` (用户表)**
      * 存储用户信息、加密密码和 Dify API Key。
2.  **`water_sources` (水源信息表)**
      * 核心业务表，代表一个监测点（如 "A号排污口"）。
      * 每个水源与一个用户 (`user_id`) 和一个 Dify 会话 (`dify_conversation_id`) 绑定。
3.  **`water_quality_data` (水质数据表)**
      * 存储传感器上传的原始数据（如浊度值）。
      * 通过 `source_id` 关联到 `water_sources` 表。
4.  **`analysis_reports` (分析报告表)**
      * 存储 Dify 生成的 AI 分析报告。
      * 通过 `source_id` 关联到 `water_sources`。
      * `is_published` 字段控制报告是否公开可见。
5.  **`report_data_links` (报告与数据的关联表)**
      * 多对多关联表，记录某份报告是基于哪些 `water_quality_data` 记录生成的。

### Redis 数据结构

Redis 在项目中用于缓存、限流和分布式锁，以提高性能和并发安全。

  * **用户登录 (Session):**

      * **Key:** `login:<UserId>`
      * **类型:** `String (JSON)`
      * **描述:** 存储 `User` 实体信息，用于 JWT 认证时快速获取用户信息。

  * **报告生成限流 (Rate Limit):**

      * **Key:** `ratelimit:report:user:<UserId>`
      * **类型:** `String (Counter)`
      * **描述:** 限制用户生成报告的频率（例如 10 分钟内 N 次）。

  * **报告生成锁 (Distributed Lock):**

      * **Key:** `lock:report:source:<SourceStrId>`
      * **类型:** `String`
      * **描述:** 使用 `setIfAbsent` (SETNX) 实现分布式锁，防止同一水源并发生成报告。

  * **公开报告缓存 (Cache):**

      * **Key:** `cache:report:public:<ReportStrId>`
      * **类型:** `String (JSON)`
      * **描述:** 缓存已发布的报告详情 (`ReportPublicResponse`)，加快二维码扫描后的公开访问速度。

  * **数据上传限流 (Rate Limit):**

      * **Key:** `ratelimit:upload:source:<SourceStrId>`
      * **类型:** `String (Counter)`
      * **描述:** 限制传感器上报数据的频率（例如每分钟 N 次）。

  * **水源 ID 缓存 (Cache):**

      * **Key:** `cache:source_id_by_strid:<SourceStrId>`
      * **类型:** `String (Number)`
      * **描述:** 缓存 `SourceStrId` (字符串) 到 `SourceId` (Long) 的映射，减少数据上报时对数据库的查询。

  * **水源详情缓存 (Cache):**

      * **Key:** `cache:source_details_by_strid:<SourceStrId><UserId>`
      * **类型:** `String (JSON)`
      * **描述:** 缓存水源的详细信息 (`WaterSourceResponse`)，用于水源详情页的快速加载。

## 3\. 主要技术栈

  * **核心框架:** Spring Boot 2.7.18
  * **安全认证:** Spring Security, JSON Web Tokens (JWT)
  * **数据库:** MySQL, MyBatis, Spring Data Redis
  * **API 文档:** Swagger (SpringFox OpenAPI 3.0)
  * **AI 集成:** `dify-java-client` (用于调用 Dify 聊天工作流)
  * **工具:** Lombok, ZXing (生成二维码), ULID (ID 生成)