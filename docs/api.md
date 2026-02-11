# 向归 API 接口文档

> 本文档详细描述了向归认证服务的所有 API 接口

## 目录

- [项目架构概述](#项目架构概述)
- [配置文件说明](#配置文件说明)
- [API 文档访问](#api-文档访问)
- [接口列表](#接口列表)
- [错误码列表](#错误码列表)
- [数据库设计](#数据库设计)
- [Redis Key 设计](#redis-key-设计)
- [安全措施](#安全措施)
- [启动项目](#启动项目)
- [架构改进说明](#架构改进说明)

---

## 项目架构概述

### 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Java | 21 | 编程语言 |
| MyBatis | 3.0.4 | ORM 框架 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存 |
| SpringDoc | 2.3.0 | API 文档 |
| Maven | 3.9+ | 构建工具 |

### 项目结构

```
src/
├── main/java/com/xianggui/app/
│   ├── AppApplication.java          # 启动类
│   ├── config/                      # 配置类
│   ├── controller/                  # 控制层
│   ├── service/                     # 业务层
│   ├── mapper/                      # 数据访问层
│   ├── entity/                      # 实体类
│   ├── dto/                         # 数据传输对象
│   ├── util/                        # 工具类
│   ├── common/                      # 公共类
│   └── exception/                   # 异常处理
└── resources/
    ├── mapper/*.xml                 # SQL 映射
    ├── application*.yaml            # 配置文件
    └── init.sql                     # 初始化脚本
```

---

## 配置文件说明

### 主配置文件 (application.yaml)

```yaml
spring:
  profiles:
    active: dev  # 激活开发环境配置
```

### 开发环境配置 (application-dev.yaml)

包含：
- 数据库连接（Hikari 连接池）
- Redis 配置
- 日志级别（DEBUG）
- JWT 密钥（开发环境默认值）
- 验证码过期时间
- 登录安全策略

### 生产环境配置 (application-prod.yaml)

包含：
- 环境变量驱动的数据库/Redis 配置
- 文件日志输出
- 更严格的 CORS 策略
- 必须通过环境变量设置 JWT 密钥

### 配置属性类 (AppProperties)

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expire: 604800
  captcha:
    sms:
      expire-seconds: 300
      rate-limit-seconds: 60
  security:
    login:
      max-fail-attempts: 5
      lock-duration-minutes: 30
```

---

## API 文档访问

启动应用后访问：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/v3/api-docs.yaml

---

## 接口列表

### 认证管理接口 (/api/v1/auth)

#### 1. 发送注册验证码

```http
POST /api/v1/auth/register/sms-code
Content-Type: application/json

请求体:
{
  "mobile": "13800138000",
  "username": "test_user"
}

成功响应 (200):
{
  "code": 200,
  "message": "验证码发送成功",
  "data": null,
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

错误响应:
- 400: 参数校验失败
- 4001: 手机号格式错误
- 4002: 用户名格式错误
- 4003: 手机号已注册
- 4004: 用户名已存在
- 4301: 请求过于频繁
```

#### 2. 用户注册

```http
POST /api/v1/auth/register
Content-Type: application/json

请求体:
{
  "username": "test_user",
  "mobile": "13800138000",
  "code": "123456"
}

成功响应 (200):
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "user_id": 12345,
    "username": "test_user",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 604800,
    "need_create_avatar": true
  },
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

错误响应:
- 400: 参数校验失败
- 4101: 验证码错误
- 4102: 验证码已过期
```

#### 3. 密码登录

```http
POST /api/v1/auth/login/password
Content-Type: application/json

请求体:
{
  "mobile": "13800138000",
  "password": "password123",
  "remember_me": true
}

成功响应 (200):
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "user_id": 12345,
    "username": "test_user",
    "mobile": "13800138000",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "token_type": "Bearer",
    "expires_in": 2592000,
    "avatar_created": true
  },
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

错误响应:
- 400: 参数校验失败
- 4201: 手机号或密码错误
- 4202: 账号已被锁定
- 4203: 该手机号未注册
```

#### 4. 短信验证码登录

```http
POST /api/v1/auth/login/sms
Content-Type: application/json

请求体:
{
  "mobile": "13800138000",
  "code": "123456"
}

成功响应: 同密码登录

错误响应:
- 400: 参数校验失败
- 4101: 验证码错误
- 4202: 账号已被锁定
- 4203: 该手机号未注册
```

#### 5. 发送重置密码验证码

```http
POST /api/v1/auth/password/reset-sms
Content-Type: application/json

请求体:
{
  "mobile": "13800138000"
}

成功响应 (200):
{
  "code": 200,
  "message": "验证码发送成功",
  "data": null,
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

错误响应:
- 400: 参数校验失败
- 4203: 该手机号未注册
- 4301: 请求过于频繁
```

#### 6. 重置密码

```http
POST /api/v1/auth/password/reset
Content-Type: application/json

请求体:
{
  "mobile": "13800138000",
  "code": "123456",
  "new_password": "newpassword123",
  "confirm_password": "newpassword123"
}

成功响应 (200):
{
  "code": 200,
  "message": "密码重置成功",
  "data": null,
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

错误响应:
- 400: 参数校验失败
- 4101: 验证码错误
- 4401: 密码格式错误
- 4402: 两次输入的密码不一致
```

#### 7. 获取图形验证码

```http
GET /api/v1/auth/captcha

成功响应 (200):
{
  "code": 200,
  "message": "success",
  "data": {
    "captcha_key": "captcha_1234567890",
    "image_data": "data:image/png;base64,iVBORw0KGgoAAAAN...",
    "expire_in": 300
  },
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}
```

#### 8. 检查用户名是否可用

```http
GET /api/v1/auth/check-username?username=test_user

成功响应 (200) - 可用:
{
  "code": 200,
  "message": "success",
  "data": {
    "available": true,
    "suggestions": null
  },
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

成功响应 (200) - 已存在:
{
  "code": 200,
  "message": "success",
  "data": {
    "available": false,
    "suggestions": ["test_user1", "test_user2"]
  },
  "timestamp": 1727164800000,
  "requestId": "req_123456"
}

错误响应:
- 400: 用户名格式错误
```

---

## 错误码列表

| 错误码 | 描述 | HTTP 状态码 |
|--------|------|------------|
| 400 | 参数校验失败 | 400 |
| 4001 | 手机号格式错误 | 200 |
| 4002 | 用户名格式错误 | 200 |
| 4003 | 手机号已注册 | 200 |
| 4004 | 用户名已存在 | 200 |
| 4101 | 验证码错误 | 200 |
| 4102 | 验证码已过期 | 200 |
| 4201 | 手机号或密码错误 | 200 |
| 4202 | 账号已被锁定 | 200 |
| 4203 | 该手机号未注册 | 200 |
| 4301 | 请求过于频繁 | 200 |
| 4401 | 密码格式错误 | 200 |
| 4402 | 密码不一致 | 200 |
| 5000 | 系统内部错误 | 200 |

---

## 数据库设计

### 用户表 (users)

```sql
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    mobile VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    password_hash VARCHAR(128) COMMENT '加密密码',
    avatar_config JSON COMMENT '虚拟形象配置',
    user_status TINYINT DEFAULT 1 COMMENT '状态: 1正常,0禁用,2未完成注册',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    last_login_at DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '软删除时间'
);
```

---

## Redis Key 设计

| Key 模式 | 说明 | 过期时间 |
|---------|------|---------|
| `sms:code:{mobile}:{biz_type}` | 短信验证码 | 5 分钟 |
| `sms:rate:limit:{mobile}` | 发送频率限制 | 60 秒 |
| `login:fail:count:{mobile}` | 登录失败计数 | 1 小时 |
| `login:lock:{mobile}` | 账号锁定 | 30 分钟 |
| `captcha:{captcha_key}` | 图形验证码 | 5 分钟 |
| `session:token:{token}` | 用户会话 | 7 天/30 天 |

---

## 安全措施

### 短信验证码

- 过期时间：5 分钟（可配置）
- 发送频率限制：60 秒内只能发送 1 次
- 每日发送上限：10 次

### 登录防护

- 连续失败 5 次锁定账号 30 分钟
- 失败计数在 1 小时后自动清除
- 锁定状态通过 Redis 存储

### 密码安全

- 使用 SHA-256 算法加盐加密
- 盐值长度 16 字节
- 密码格式要求：6-20 位，支持中英文、数字和特殊字符

### Token 管理

- 默认过期时间：7 天
- 记住登录时过期时间：30 天
- Token 存储在 Redis 中
- 支持 token 撤销和强制退出

---

## 启动项目

### 前置条件

1. MySQL 8.0+ 数据库
2. Redis 服务器
3. Java 21 JDK

### 环境变量配置（生产环境必需）

```bash
# 数据库
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=xianggui
export DB_USERNAME=root
export DB_PASSWORD=

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=

# JWT（必须设置强密钥）
export JWT_SECRET=

# CORS（生产环境限制域名）
export CORS_ALLOWED_ORIGINS=https://your-frontend.com
```

### 初始化数据库

```sql
-- 执行 init.sql
source /path/to/init.sql;
```

### 编译运行

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境
mvn clean package
java -jar target/app-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### 访问应用

- 服务地址：http://localhost:8080
- API 根路径：http://localhost:8080/api/v1/auth
- API 文档：http://localhost:8080/swagger-ui.html

---

## 架构改进说明

### 本次优化内容

1. **配置外部化**
   - 创建 `application-dev.yaml` 和 `application-prod.yaml`
   - 使用 `AppProperties` 类统一管理配置
   - JWT 密钥、验证码过期时间等敏感/可变配置全部外部化

2. **参数校验增强**
   - 所有 DTO 添加 `@Valid` 校验注解
   - Controller 层使用 `@Validated` 注解
   - 添加全局异常处理 `GlobalExceptionHandler`

3. **API 文档自动化**
   - 集成 SpringDoc OpenAPI
   - Controller 和 DTO 添加 Swagger 注解
   - 访问 `/swagger-ui.html` 查看交互式文档

4. **代码结构优化**
   - 添加 `config` 包管理配置类
   - 添加 `exception` 包管理异常处理
   - 工具类改为使用配置属性而非硬编码

5. **安全性提升**
   - JWT 密钥通过环境变量注入
   - 生产环境配置强制要求设置密钥
   - 验证码长度、过期时间可配置

### 后续扩展建议

1. 添加 JWT 验证拦截器进行权限管理
2. 集成真实短信服务（阿里云、腾讯云）
3. 完善图形验证码生成和验证逻辑
4. 添加日志记录和监控（Micrometer + Prometheus）
5. 实现虚拟形象相关接口
6. 添加单元测试和集成测试
