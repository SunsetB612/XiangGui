# 向归项目入门指南

## 项目简介

向归是一款基于 Spring Boot 的认证服务系统，提供用户注册、登录、密码管理等核心功能。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 4.0.0 | 核心框架 |
| Java | 21 | 编程语言 |
| MyBatis | 3.0.3 | ORM框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | - | 缓存与会话存储 |
| SpringDoc | 2.3.0 | API文档 |

## 项目架构

```
┌─────────────────────────────────────────────────────────────┐
│                        Controller层                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  AuthController.java                                │   │
│  │  - 处理HTTP请求与响应                                │   │
│  │  - 参数校验（@Valid）                               │   │
│  │  - 调用Service层                                    │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                        Service层                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  AuthService.java                                   │   │
│  │  - 核心业务逻辑                                      │   │
│  │  - 业务规则校验                                      │   │
│  │  - 调用Mapper/Redis                                  │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    Repository/DAO层                         │
│  ┌─────────────────────┐  ┌─────────────────────────────┐  │
│  │  UserMapper.java    │  │  RedisUtil.java             │  │
│  │  - 数据库操作        │  │  - 缓存操作                  │  │
│  │  - SQL映射          │  │  - 会话管理                  │  │
│  └─────────────────────┘  └─────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 核心类详解

### 1. Controller层

#### AuthController.java
**作用**：处理所有认证相关的HTTP请求

```java
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    // 主要接口：
    // POST /register/sms-code    - 发送注册验证码
    // POST /register             - 用户注册
    // POST /login/password       - 密码登录
    // POST /login/sms            - 短信登录
    // POST /password/reset-sms   - 发送重置密码验证码
    // POST /password/reset       - 重置密码
    // GET  /captcha              - 获取图形验证码
    // GET  /check-username       - 检查用户名
}
```

**关键注解**：
- `@RestController`：声明RESTful控制器
- `@Validated`：开启方法参数校验
- `@Valid`：校验请求体DTO
- `@Operation`：Swagger接口文档

---

### 2. Service层

#### AuthService.java
**作用**：处理业务逻辑，是系统的核心

**主要职责**：
1. 验证码生成与验证
2. 用户注册与登录逻辑
3. 密码重置流程
4. 登录失败次数记录
5. 账号锁定机制

**关键方法**：
```java
// 发送注册验证码
public ApiResponse<Void> sendRegisterSmsCode(RegisterSmsCodeRequest request)

// 用户注册
public ApiResponse<RegisterResponse> register(RegisterRequest request)

// 密码登录
public ApiResponse<LoginResponse> loginByPassword(LoginPasswordRequest request)

// 短信验证码登录
public ApiResponse<LoginResponse> loginBySms(LoginSmsRequest request)
```

---

### 3. Repository/Mapper层

#### UserMapper.java
**作用**：数据库访问接口

```java
@Mapper
public interface UserMapper {
    int insert(User user);                          // 插入用户
    User selectByMobile(String mobile);             // 根据手机号查询
    int existsMobile(String mobile);                // 检查手机号是否存在
    int existsUsername(String username);            // 检查用户名是否存在
    int updatePassword(@Param("mobile") String mobile, 
                       @Param("passwordHash") String passwordHash);
    int updateLoginInfo(@Param("id") Long id, 
                        @Param("ip") String ip);
}
```

#### UserMapper.xml
**作用**：SQL映射文件，定义具体SQL语句

```xml
<!-- 示例：根据手机号查询用户 -->
<select id="selectByMobile" resultType="com.xianggui.app.entity.User">
    SELECT * FROM users WHERE mobile = #{mobile} AND deleted_at IS NULL
</select>
```

---

### 4. 工具类

#### JwtUtil.java
**作用**：JWT令牌生成与验证

```java
// 生成Token
public static String generateToken(Long userId, String username, String mobile, long expiresIn)

// 解析Token
public static TokenInfo parseToken(String token)

// 获取过期时间
public static long getDefaultExpiresIn(Boolean rememberMe)
```

#### RedisUtil.java
**作用**：Redis缓存操作

```java
// 短信验证码
public void setSmsCode(String mobile, String codeType, String code)
public String getSmsCode(String mobile, String codeType)

// 登录失败计数
public void recordLoginFailure(String mobile)
public boolean isAccountLocked(String mobile)

// 用户会话
public void setUserSession(String token, Long userId, String username, String mobile, long expiresIn)
```

#### PasswordUtil.java
**作用**：密码加密与验证

```java
// 密码加密（SHA-256 + Salt）
public static String hashPassword(String password)

// 密码验证
public static boolean verifyPassword(String password, String hashedPassword)
```

#### ValidationUtil.java
**作用**：参数格式校验

```java
public static boolean isValidMobile(String mobile)      // 手机号格式
public static boolean isValidUsername(String username)  // 用户名格式
public static boolean isValidPassword(String password)  // 密码格式
```

---

### 5. DTO（数据传输对象）

#### 请求DTO
| 类名 | 用途 | 关键字段 |
|------|------|---------|
| `RegisterSmsCodeRequest` | 发送注册验证码 | mobile, username |
| `RegisterRequest` | 用户注册 | username, mobile, code |
| `LoginPasswordRequest` | 密码登录 | mobile, password, rememberMe |
| `LoginSmsRequest` | 短信登录 | mobile, code |
| `ResetPasswordRequest` | 重置密码 | mobile, code, newPassword, confirmPassword |

#### 响应DTO
| 类名 | 用途 | 关键字段 |
|------|------|---------|
| `RegisterResponse` | 注册响应 | userId, username, token, expiresIn |
| `LoginResponse` | 登录响应 | userId, username, mobile, token, avatarCreated |
| `CaptchaResponse` | 验证码响应 | captchaKey, imageData, expireIn |
| `CheckUsernameResponse` | 用户名检查 | available, suggestions |

---

### 6. 异常处理

#### BusinessException.java
**作用**：业务异常，预期内的错误

```java
// 使用场景：余额不足、用户不存在、验证码错误等
throw new BusinessException(ErrorCode.INVALID_CODE, "验证码错误");
```

#### SystemException.java
**作用**：系统异常，非预期错误

```java
// 使用场景：数据库连接失败、Redis不可用等
throw new SystemException(ErrorCode.DB_ERROR, "数据库连接失败", cause);
```

#### GlobalExceptionHandler.java
**作用**：全局异常处理器

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        // 记录结构化日志，返回友好提示
    }
    
    @ExceptionHandler(SystemException.class)
    public ApiResponse<Void> handleSystemException(SystemException e) {
        // 记录完整堆栈，返回通用错误
    }
}
```

---

### 7. 配置类

#### AppProperties.java
**作用**：统一管理应用配置

```java
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private JwtProperties jwt;                    // JWT配置
    private CaptchaProperties captcha;            // 验证码配置
    private SecurityProperties security;          // 安全配置
    private CorsProperties cors;                  // 跨域配置
}
```

**配置示例**（application-dev.yaml）：
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:default_secret}
    access-token-expire: 604800  # 7天
  
  captcha:
    sms:
      expire-seconds: 300        # 5分钟
      rate-limit-seconds: 60     # 60秒限制
  
  security:
    login:
      max-fail-attempts: 5       # 最大失败次数
      lock-duration-minutes: 30  # 锁定30分钟
```

---

### 8. 公共类

#### ApiResponse.java
**作用**：统一API响应格式

```java
public class ApiResponse<T> {
    private Integer code;        // 响应码
    private String message;      // 响应消息
    private T data;              // 响应数据
    private Long timestamp;      // 时间戳
    private String requestId;    // 请求ID
}
```

**使用示例**：
```java
// 成功响应
return ApiResponse.success(data, "操作成功");

// 错误响应
return ApiResponse.error(ErrorCode.INVALID_CODE, "验证码错误");
```

#### ErrorCode.java
**作用**：错误码常量定义

```java
public class ErrorCode {
    public static final Integer INVALID_MOBILE = 4001;           // 手机号格式错误
    public static final Integer INVALID_USERNAME = 4002;         // 用户名格式错误
    public static final Integer MOBILE_ALREADY_REGISTERED = 4003; // 手机号已注册
    public static final Integer INVALID_CODE = 4101;             // 验证码错误
    public static final Integer INVALID_CREDENTIALS = 4201;      // 手机号或密码错误
    public static final Integer ACCOUNT_LOCKED = 4202;           // 账号已被锁定
}
```

---

## 数据流示例

### 用户注册流程

```
用户请求
    │
    ▼
┌─────────────────┐
│ AuthController  │ ── @Valid校验参数
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  AuthService    │ ── 业务逻辑处理
│                 │    1. 校验手机号/用户名
│                 │    2. 检查频率限制
│                 │    3. 生成验证码
│                 │    4. 保存到Redis
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌───────┐  ┌─────────┐
│UserMapper│  │RedisUtil│
└───────┘  └─────────┘
```

### 用户登录流程

```
用户请求 (mobile + password)
    │
    ▼
┌─────────────────┐
│ AuthController  │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  AuthService    │ ── 1. 查询用户
│                 │    2. 检查账号锁定
│                 │    3. 验证密码
│                 │    4. 生成JWT Token
│                 │    5. 保存会话到Redis
└────────┬────────┘
         │
         ▼
    ┌────┴────┐
    ▼         ▼
┌───────┐  ┌─────────┐
│UserMapper│  │RedisUtil│
└───────┘  └─────────┘
```

---

## 快速开始

### 1. 环境准备

```bash
# 1. 安装 Java 21
java -version

# 2. 安装 Maven
mvn -version

# 3. 启动 MySQL 和 Redis
# MySQL: 创建数据库 xianggui
# Redis: 默认端口 6379
```

### 2. 数据库初始化

```sql
-- 执行 init.sql
source /path/to/init.sql;
```

### 3. 配置文件

编辑 `application-dev.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xianggui?useSSL=false&serverTimezone=UTC
    username: root
    password: your_password
  
  redis:
    host: localhost
    port: 6379
```

### 4. 启动项目

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或打包后运行
mvn clean package
java -jar target/app-0.0.1-SNAPSHOT.jar
```

### 5. 访问API文档

- Swagger UI: http://localhost:8080/swagger-ui.html
- API Docs: http://localhost:8080/v3/api-docs

---

## 开发规范

### 1. 分层规范

| 层级 | 职责 | 禁止事项 |
|------|------|---------|
| Controller | HTTP请求处理、参数校验 | 禁止写业务逻辑 |
| Service | 业务逻辑、事务管理 | 禁止直接操作SQL |
| Mapper | 数据库访问 | 禁止写业务逻辑 |

### 2. 注释规范

每个类和方法必须包含：
- **类注释**：说明类的作用
- **方法注释**：说明方法用途
- **Why注释**：解释设计决策
- **Warning注释**：警示安全风险

```java
/**
 * 发送注册验证码
 * Why: 分离验证码发送与注册逻辑，支持重试机制
 * Warning: 受频率限制保护，防止短信轰炸
 */
```

### 3. 异常处理规范

- 业务异常使用 `BusinessException`
- 系统异常使用 `SystemException`
- 禁止直接返回错误响应，统一抛异常由全局处理器处理

### 4. 日志规范

```java
// 业务事件 - INFO级别
log.info("[业务事件] {event=USER_LOGIN, userId=123}");

// 业务异常 - WARN级别
log.warn("[业务异常] {code=4001, message=手机号格式错误}");

// 系统异常 - ERROR级别
log.error("[系统异常] {type=DB_ERROR}", exception);
```

---

## 目录结构速查

```
src/main/java/com/xianggui/app/
├── AppApplication.java              # 启动类
├── config/                          # 配置类
│   ├── AppProperties.java           # 应用配置
│   ├── OpenApiConfig.java           # API文档配置
│   └── WebConfig.java               # Web配置
├── controller/                      # 控制器层
│   └── AuthController.java          # 认证接口
├── service/                         # 业务层
│   └── AuthService.java             # 认证服务
├── mapper/                          # 数据访问层
│   └── UserMapper.java              # 用户数据访问
├── entity/                          # 实体类
│   └── User.java                    # 用户实体
├── dto/                             # 数据传输对象
│   ├── RegisterSmsCodeRequest.java
│   ├── RegisterRequest.java
│   ├── RegisterResponse.java
│   ├── LoginPasswordRequest.java
│   ├── LoginResponse.java
│   ├── LoginSmsRequest.java
│   ├── ResetPasswordSmsRequest.java
│   ├── ResetPasswordRequest.java
│   ├── CaptchaResponse.java
│   └── CheckUsernameResponse.java
├── util/                            # 工具类
│   ├── JwtUtil.java                 # JWT工具
│   ├── PasswordUtil.java            # 密码工具
│   ├── RedisUtil.java               # Redis工具
│   └── ValidationUtil.java          # 校验工具
├── common/                          # 公共类
│   ├── ApiResponse.java             # 统一响应
│   └── ErrorCode.java               # 错误码
└── exception/                       # 异常处理
    ├── BusinessException.java       # 业务异常
    ├── SystemException.java         # 系统异常
    └── GlobalExceptionHandler.java  # 全局异常处理
```

---

## 常见问题

### Q1: 如何添加新的接口？

1. 在 `AuthController` 添加方法
2. 在 `AuthService` 实现业务逻辑
3. 如需数据库操作，在 `UserMapper` 添加方法
4. 创建对应的DTO类
5. 添加Swagger注解

### Q2: 如何修改配置？

1. 开发环境：修改 `application-dev.yaml`
2. 生产环境：通过环境变量覆盖

### Q3: 如何查看日志？

```bash
# 开发环境日志在控制台输出
# 生产环境日志在 logs/ 目录下

tail -f logs/xianggui-app.log
```

---

## 联系方式

如有问题，请联系向归开发团队：dev@xianggui.com
