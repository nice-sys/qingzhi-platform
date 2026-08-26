-- ============================================================
-- 青知共享平台 - 数据库初始化脚本
-- 数据库：qingzhi_platform
-- 字符集：utf8mb4  排序规则：utf8mb4_0900_ai_ci
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------
-- 0. 创建数据库（如不存在）
-- --------------------------------------------------------
DROP DATABASE IF EXISTS `qingzhi_platform`;
CREATE DATABASE `qingzhi_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;
USE `qingzhi_platform`;

-- --------------------------------------------------------
-- 1. 用户表（user）  PRD 4.1
-- --------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`         VARCHAR(50)     NOT NULL COMMENT '账号（学号/工号/Admin）',
    `password`         VARCHAR(64)     NOT NULL COMMENT 'MD5 加密后的密码',
    `name`             VARCHAR(50)              DEFAULT NULL COMMENT '姓名',
    `phone`            VARCHAR(20)              DEFAULT NULL COMMENT '手机号',
    `email`            VARCHAR(100)             DEFAULT NULL COMMENT '邮箱',
    `department`       VARCHAR(100)             DEFAULT NULL COMMENT '院系',
    `major`            VARCHAR(100)             DEFAULT NULL COMMENT '专业（仅学生）',
    `avatar_url`       VARCHAR(512)             DEFAULT NULL COMMENT '头像URL',
    `role`             TINYINT         NOT NULL DEFAULT 2 COMMENT '角色：0-管理员 1-教师 2-学生',
    `status`           TINYINT         NOT NULL DEFAULT 0 COMMENT '账号状态：0-正常 1-锁定',
    `login_fail_count` INT             NOT NULL DEFAULT 0 COMMENT '连续登录失败次数',
    `lock_time`        DATETIME                  DEFAULT NULL COMMENT '锁定开始时间',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=10001 DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- --------------------------------------------------------
-- 2. 资源表（resource）  PRD 4.2
-- --------------------------------------------------------
DROP TABLE IF EXISTS `resource`;
CREATE TABLE `resource` (
    `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`            VARCHAR(200)    NOT NULL COMMENT '资源标题',
    `description`      TEXT                      DEFAULT NULL COMMENT '资源描述',
    `course`           VARCHAR(100)    NOT NULL COMMENT '所属课程',
    `uploader_id`      BIGINT UNSIGNED NOT NULL COMMENT '上传者用户ID（FK -> user.id）',
    `file_name`        VARCHAR(255)    NOT NULL COMMENT '原始文件名（含扩展名）',
    `file_path`        VARCHAR(512)             DEFAULT NULL COMMENT '文件存储路径',
    `file_size`        BIGINT UNSIGNED          DEFAULT NULL COMMENT '文件大小（字节）',
    `file_ext`         VARCHAR(20)              DEFAULT NULL COMMENT '文件扩展名（pdf/docx/...）',
    `file_hash`        VARCHAR(64)              DEFAULT NULL COMMENT '文件哈希（MD5 或 SHA-256，秒传用）',
    `download_count`   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '下载次数',
    `review_status`    TINYINT         NOT NULL DEFAULT 0 COMMENT '审核状态：0-待审核 1-已通过 2-已拒绝',
    `reject_reason`    VARCHAR(500)             DEFAULT NULL COMMENT '审核拒绝理由',
    `review_admin_id`  BIGINT UNSIGNED          DEFAULT NULL COMMENT '审核管理员ID（FK -> user.id）',
    `review_time`      DATETIME                  DEFAULT NULL COMMENT '审核时间',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_uploader` (`uploader_id`),
    KEY `idx_course` (`course`),
    KEY `idx_review_status` (`review_status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_file_hash` (`file_hash`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='资源表';

-- --------------------------------------------------------
-- 3. 收藏表（favorite）  PRD 4.3
-- --------------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT UNSIGNED NOT NULL COMMENT '用户ID（FK -> user.id）',
    `resource_id`  BIGINT UNSIGNED NOT NULL COMMENT '资源ID（FK -> resource.id）',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_resource` (`user_id`, `resource_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_resource_id` (`resource_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- --------------------------------------------------------
-- 4. 文件存储表（file_storage）  PRD 4.4  加分项「秒传」
-- --------------------------------------------------------
DROP TABLE IF EXISTS `file_storage`;
CREATE TABLE `file_storage` (
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_hash`       VARCHAR(64)     NOT NULL COMMENT '文件哈希值（MD5/SHA-256）',
    `file_path`       VARCHAR(512)    NOT NULL COMMENT '实际存储路径',
    `file_size`       BIGINT UNSIGNED NOT NULL COMMENT '文件大小（字节）',
    `reference_count` INT UNSIGNED    NOT NULL DEFAULT 1 COMMENT '引用计数：多少个 Resource 引用该文件',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次上传时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_hash` (`file_hash`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文件存储表（秒传引用计数）';

-- ============================================================
-- 5. 预置初始管理员账号
--    账号：Admin    密码：Admin2026
--    密码算法：PasswordUtil.encrypt("Admin2026") = MD5(UTF-8) 32位小写
-- ============================================================
INSERT INTO `user` (
    `id`, `username`, `password`, `name`, `phone`, `email`, `department`, `major`,
    `role`, `status`, `login_fail_count`, `lock_time`, `create_time`, `update_time`
) VALUES (
    1,                               -- id
    'Admin',                         -- username
    'a41ae6c8735913b45643a8b790097993',  -- MD5("Admin2026")
    '超级管理员',                    -- name
    NULL,                            -- phone
    'admin@qingzhi.edu.cn',          -- email
    '系统管理',                      -- department
    NULL,                            -- major
    0,                               -- role = ADMIN
    0,                               -- status = 正常
    0,                               -- login_fail_count
    NULL,                            -- lock_time
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 自增值从 10000 开始，学号/工号业务从 10001 起
ALTER TABLE `user` AUTO_INCREMENT = 10001;

SET FOREIGN_KEY_CHECKS = 1;
