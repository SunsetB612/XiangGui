# 向归 (XiangGui)


## 项目简介
待补充

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Java | 21 | 编程语言 |
| MyBatis | 3.0.4 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 6.0+ | 缓存与会话存储 |
| SpringDoc | 2.3.0 | API 文档生成 |
| Maven | 3.9+ | 构建工具 |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- Redis 6.0+

### 1. 克隆项目

```bash
git clone <repository-url>
cd XiangGui
```

### 2. 初始化数据库

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/init.sql
```

### 3. 配置应用

编辑 `src/main/resources/application-dev.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/xianggui?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: your_username
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: localhost
      port: 6379
```

### 4. 运行项目

```bash
# 开发环境
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或打包后运行
mvn clean package
java -jar target/app-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 5. 访问服务

- **API 服务**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API 文档**: http://localhost:8080/v3/api-docs

## 文档

| 文档 | 说明 |
|------|------|
| [docs/api.md](docs/api.md) | API 接口详细文档 |
| [docs/guide.md](docs/guide.md) | 开发者入门指南 |

## API 接口概览

### 认证管理接口 (/api/v1/auth)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/register/sms-code` | 发送注册验证码 |
| POST | `/register` | 用户注册 |
| POST | `/login/password` | 密码登录 |
| POST | `/login/sms` | 短信验证码登录 |
| POST | `/password/reset-sms` | 发送重置密码验证码 |
| POST | `/password/reset` | 重置密码 |
| GET | `/captcha` | 获取图形验证码 |
| GET | `/check-username` | 检查用户名是否可用 |

## 核心功能

- **用户注册**: 手机号验证码注册，用户名唯一性校验
- **用户登录**: 密码登录、短信验证码登录，支持记住登录状态
- **密码管理**: 密码重置，SHA-256 + Salt 加密存储
- **安全防护**: JWT Token 认证，登录失败次数限制，账号锁定机制

## 项目结构

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

## 开发规范

### 分层架构

```
Controller -> Service -> Mapper/DAO
    ↓              ↓           ↓
  HTTP处理      业务逻辑     数据访问
```




