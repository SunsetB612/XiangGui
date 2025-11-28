# 香榭API接口文档

## 项目架构概述

### 技术栈
- **框架**: Spring Boot 4.0.0
- **语言**: Java 21
- **ORM**: MyBatis 3.0.3
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **JSON处理**: Jackson 2.20.1
- **构建工具**: Maven 3.9.9

### 项目结构
```
src/
├── main/
│   ├── java/com/xianggui/app/
│   │   ├── AppApplication.java          # 启动类
│   │   ├── controller/                  # 控制层
│   │   │   └── AuthController.java      # 认证相关接口
│   │   ├── service/                     # 业务层
│   │   │   └── AuthService.java         # 认证服务
│   │   ├── mapper/                      # 数据映射层
│   │   │   └── UserMapper.java          # 用户数据映射
│   │   ├── entity/                      # 实体类
│   │   │   └── User.java                # 用户实体
│   │   ├── dto/                         # 数据传输对象
│   │   │   ├── RegisterSmsCodeRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── RegisterResponse.java
│   │   │   ├── LoginPasswordRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── LoginSmsRequest.java
│   │   │   ├── ResetPasswordSmsRequest.java
│   │   │   ├── ResetPasswordRequest.java
│   │   │   ├── CaptchaResponse.java
│   │   │   └── CheckUsernameResponse.java
│   │   ├── util/                        # 工具类
│   │   │   ├── ValidationUtil.java      # 验证工具
│   │   │   ├── PasswordUtil.java        # 密码处理工具
│   │   │   ├── JwtUtil.java             # JWT令牌工具
│   │   │   └── RedisUtil.java           # Redis操作工具
│   │   └── common/                      # 公共类
│   │       ├── ApiResponse.java         # 统一响应格式
│   │       └── ErrorCode.java           # 错误码定义
│   └── resources/
│       ├── application.yaml             # 配置文件
│       ├── schema.sql                   # 数据库表定义
│       ├── init.sql                     # 初始化数据
│       └── mapper/
│           └── UserMapper.xml           # MyBatis SQL映射
├── test/
│   └── java/com/xianggui/app/
│       └── AppApplicationTests.java     # 测试类
└── pom.xml                              # Maven配置

```

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

## Redis Key设计

### 短信验证码
```
sms:code:{mobile}:{biz_type} = code
过期时间: 5分钟
```

### 发送频率限制
```
sms:rate:limit:{mobile} = timestamp
过期时间: 60秒
```

### 登录失败计数
```
login:fail:count:{mobile} = count
过期时间: 1小时
```

### 账号锁定
```
login:lock:{mobile} = {"lock_until": timestamp, "reason": "too_many_failures"}
过期时间: 30分钟
```

### 图形验证码
```
captcha:{captcha_key} = code
过期时间: 5分钟
```

### 用户会话
```
session:token:{token} = {"user_id": 123, "username": "test", "mobile": "13800138000"}
过期时间: 动态 (7天或30天)
```

## 公共响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1727164800000,
  "request_id": "req_123456"
}
```

## 接口列表

### 1. 认证相关接口 (/api/v1/auth)

#### 1.1 发送注册验证码
```
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
  "data": null
}

错误响应:
- 4001: 手机号格式错误
- 4002: 用户名格式错误
- 4003: 手机号已注册
- 4004: 用户名已存在
- 4301: 请求过于频繁，请60秒后重试
```

#### 1.2 用户注册
```
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
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 604800,
    "need_create_avatar": true
  }
}

错误响应:
- 4101: 验证码错误
- 4102: 验证码已过期，请重新获取
```

#### 1.3 密码登录
```
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
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 2592000,
    "avatar_created": true,
    "avatar_config": {"hair": "style1", "clothes": "style2"}
  }
}

错误响应:
- 4201: 手机号或密码错误
- 4202: 账号已被锁定，请30分钟后再试
- 4203: 该手机号未注册
```

#### 1.4 短信验证码登录
```
POST /api/v1/auth/login/sms
Content-Type: application/json

请求体:
{
  "mobile": "13800138000",
  "code": "123456"
}

成功响应: 同密码登录
```

#### 1.5 发送重置密码验证码
```
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
  "data": null
}

错误响应:
- 4203: 该手机号未注册
- 4301: 请求过于频繁，请60秒后重试
```

#### 1.6 重置密码
```
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
  "data": null
}

错误响应:
- 4101: 验证码错误
- 4401: 密码格式错误，支持6-20位中英文、数字或特殊字符
- 4402: 两次输入的密码不一致
```

#### 1.7 获取图形验证码
```
GET /api/v1/auth/captcha

成功响应 (200):
{
  "code": 200,
  "data": {
    "captcha_key": "captcha_123456",
    "image_data": "data:image/png;base64,iVBORw0KGgoAAAAN...",
    "expire_in": 300
  }
}
```

#### 1.8 检查用户名是否可用
```
GET /api/v1/auth/check-username?username=test_user

成功响应 (200):
{
  "code": 200,
  "data": {
    "available": true,
    "suggestions": null
  }
}

用户名已存在时:
{
  "code": 200,
  "data": {
    "available": false,
    "suggestions": ["test_user1", "test_user2"]
  }
}
```

## 错误码列表

| 错误码 | 描述 |
|--------|------|
| 4001 | 手机号格式错误 |
| 4002 | 用户名格式错误 |
| 4003 | 手机号已注册 |
| 4004 | 用户名已存在 |
| 4101 | 验证码错误 |
| 4102 | 验证码已过期 |
| 4201 | 手机号或密码错误 |
| 4202 | 账号已被锁定 |
| 4203 | 该手机号未注册 |
| 4301 | 请求过于频繁 |
| 4401 | 密码格式错误 |
| 4402 | 密码不一致 |

## 业务流程

### 注册流程
1. 用户输入手机号和用户名
2. 调用 `/register/sms-code` 获取验证码
3. 用户输入验证码
4. 调用 `/register` 完成注册
5. 返回token和用户信息，标记需要创建虚拟形象

### 登录流程
1. 用户输入手机号和密码（或验证码）
2. 调用 `/login/password` 或 `/login/sms` 进行登录
3. 系统检查账号锁定状态
4. 验证密码或验证码
5. 返回token和完整用户信息

### 密码找回流程
1. 用户输入手机号
2. 调用 `/password/reset-sms` 获取验证码
3. 用户输入新密码和验证码
4. 调用 `/password/reset` 重置密码
5. 返回成功消息

## 安全措施

### 短信验证码
- 过期时间: 5分钟
- 发送频率限制: 60秒内只能发送1次
- 每日发送限制: 可在Redis中配置

### 登录防护
- 连续失败5次锁定账号30分钟
- 失败计数在1小时后自动清除
- 锁定状态通过Redis存储

### 密码安全
- 使用SHA-256算法加盐加密
- 盐值长度16字节
- 密码格式要求: 6-20位，支持中英文、数字和特殊字符

### Token管理
- 默认过期时间: 7天
- 记住登录时过期时间: 30天
- Token存储在Redis中
- 支持token撤销和强制退出

## 启动项目

### 前置条件
1. MySQL 8.0+ 数据库
2. Redis 服务器
3. Java 21 JDK

### 配置文件修改
编辑 `application.yaml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://your-db-host:3306/xianggui?useSSL=false&serverTimezone=UTC&characterEncoding=utf8mb4
    username: your-db-user
    password: your-db-password
  redis:
    host: your-redis-host
    port: 6379
```

### 初始化数据库
```sql
-- 执行 init.sql 或 schema.sql
source /path/to/init.sql;
```

### 编译运行
```bash
# 编译
mvn clean compile

# 打包
mvn clean package

# 运行
mvn spring-boot:run
# 或
java -jar target/app-0.0.1-SNAPSHOT.jar
```

### 访问应用
- 服务地址: http://localhost:8080
- API根路径: http://localhost:8080/api/v1/auth

## 开发说明

### 添加新接口步骤
1. 在 `AuthController` 中添加方法
2. 在 `AuthService` 中实现业务逻辑
3. 如需要数据库操作，在 `UserMapper` 中添加方法和XML映射
4. 创建相应的DTO类用于请求/响应
5. 编写单元测试

### 扩展建议
1. 添加JWT验证拦截器进行权限管理
2. 集成真实短信服务（如阿里云、腾讯云）
3. 完善图形验证码生成和验证逻辑
4. 添加日志记录和监控
5. 实现虚拟形象相关接口
