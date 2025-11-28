-- 初始化数据库
CREATE DATABASE IF NOT EXISTS xianggui;
USE xianggui;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名，2-20位中英文数字下划线',
    mobile VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号，唯一索引',
    password_hash VARCHAR(128) COMMENT '加密密码，注册时可为空',
    avatar_config JSON COMMENT '虚拟形象配置(JSON格式)',
    user_status TINYINT DEFAULT 1 COMMENT '状态：1-正常, 0-禁用, 2-未完成注册',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    last_login_at DATETIME COMMENT '最后登录时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME COMMENT '软删除时间',
    
    INDEX idx_mobile (mobile),
    INDEX idx_username (username),
    INDEX idx_status_created (user_status, created_at)
) COMMENT='用户主表' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 测试数据
INSERT INTO users (username, mobile, user_status, created_at, updated_at) VALUES 
('test_user', '13800138000', 1, NOW(), NOW()),
('admin', '13900139000', 1, NOW(), NOW());
