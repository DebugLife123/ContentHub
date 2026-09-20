-- ============================================================
-- ContentHub 数据库建表脚本
-- 目标库: contenthub  (MySQL 8.0, utf8mb4)
-- ============================================================
DROP DATABASE IF EXISTS contenthub;
CREATE DATABASE contenthub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE contenthub;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(60) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname` VARCHAR(60) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `bio` VARCHAR(500) DEFAULT NULL COMMENT '个人简介',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER普通用户 / CREATOR创作者 / ADMIN管理员',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 创作者资料表
-- ----------------------------
DROP TABLE IF EXISTS `creator_profiles`;
CREATE TABLE `creator_profiles` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '关联用户ID',
  `display_name` VARCHAR(60) NOT NULL COMMENT '创作者展示名',
  `intro` VARCHAR(1000) DEFAULT NULL COMMENT '创作者介绍',
  `verified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否认证: 0否 1是',
  `subscriber_count` INT NOT NULL DEFAULT 0 COMMENT '订阅人数',
  `content_count` INT NOT NULL DEFAULT 0 COMMENT '内容数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者资料表';

-- ----------------------------
-- 3. 内容表（技术文章/教程/电子书/视频/PDF/代码模板/Prompt/数据集/专栏）
-- ----------------------------
DROP TABLE IF EXISTS `contents`;
CREATE TABLE `contents` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `creator_id` BIGINT NOT NULL COMMENT '创作者ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
  `content_type` VARCHAR(20) NOT NULL COMMENT '内容类型: ARTICLE/TUTORIAL/EBOOK/VIDEO/PDF/CODE/PROMPT/DATASET/COLUMN',
  `body` MEDIUMTEXT COMMENT '正文内容(富文本/Markdown)',
  `file_url` VARCHAR(500) DEFAULT NULL COMMENT '附件文件URL(电子书/PDF/视频/数据集等)',
  `access_type` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '访问类型: FREE免费 / SUBSCRIBED订阅可见',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT草稿 / PUBLISHED已发布 / OFFLINE已下架',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_type` (`content_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容表';

-- ----------------------------
-- 4. 订阅套餐表
-- ----------------------------
DROP TABLE IF EXISTS `subscription_plans`;
CREATE TABLE `subscription_plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `creator_id` BIGINT NOT NULL COMMENT '创作者ID',
  `name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '套餐描述',
  `price` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格(元)',
  `duration_days` INT NOT NULL DEFAULT 30 COMMENT '有效天数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE上架 / INACTIVE下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅套餐表';

-- ----------------------------
-- 5. 用户订阅记录表
-- ----------------------------
DROP TABLE IF EXISTS `subscriptions`;
CREATE TABLE `subscriptions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订阅ID',
  `user_id` BIGINT NOT NULL COMMENT '订阅用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `creator_id` BIGINT NOT NULL COMMENT '创作者ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '到期时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE生效中 / EXPIRED已过期 / CANCELED已取消',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅记录表';

-- ----------------------------
-- 6. 收藏表
-- ----------------------------
DROP TABLE IF EXISTS `favorites`;
CREATE TABLE `favorites` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content_id` BIGINT NOT NULL COMMENT '内容ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_content` (`user_id`,`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- ----------------------------
-- 7. 评论表
-- ----------------------------
DROP TABLE IF EXISTS `comments`;
CREATE TABLE `comments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `content_id` BIGINT NOT NULL COMMENT '内容ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID(楼中楼)',
  `body` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL / HIDDEN',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_content` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ----------------------------
-- 初始数据: 管理员 + 测试用户 (密码均为 123456, BCrypt)
-- ----------------------------
INSERT INTO `users` (`username`,`password`,`nickname`,`role`) VALUES
('admin',   '$2a$10$OMiHtHhnErZwNyE8K71dbOh.hkhfv9OVUmjyPwewRzek0ZBtFHU8u', '管理员',   'ADMIN'),
('creator', '$2a$10$OMiHtHhnErZwNyE8K71dbOh.hkhfv9OVUmjyPwewRzek0ZBtFHU8u', '示例创作者', 'CREATOR'),
('user',    '$2a$10$OMiHtHhnErZwNyE8K71dbOh.hkhfv9OVUmjyPwewRzek0ZBtFHU8u', '普通用户', 'USER');

INSERT INTO `creator_profiles` (`user_id`,`display_name`,`intro`,`verified`) VALUES
(2, '示例创作者', '分享前端 / Java / AI 实用教程', 1);

-- ----------------------------
-- 初始内容数据
-- ----------------------------
INSERT INTO `contents` (`creator_id`,`title`,`summary`,`cover`,`content_type`,`body`,`access_type`,`status`,`view_count`,`like_count`) VALUES
(2, '一个独立开发者的产品实验室', '从想法到上线，记录每一次真实的产品决策。', NULL, 'COLUMN', '# 产品实验室\n\n从想法到上线，记录每一次真实的产品决策。', 'SUBSCRIBED', 'PUBLISHED', 1280, 128),
(2, 'AI 工作流 Prompt 图鉴', '把重复工作交给 AI，把时间还给真正重要的事。', NULL, 'PROMPT', '# Prompt Atlas\n\n42 个可以直接复用的工作流模板。', 'SUBSCRIBED', 'PUBLISHED', 860, 86),
(2, '全栈项目启动模板 2.0', '开箱即用的工程底座，专为快速验证想法而生。', NULL, 'CODE', '# Ship Faster\n\nVue 3 + Spring Boot 全栈项目启动模板。', 'FREE', 'PUBLISHED', 2140, 214);
SET FOREIGN_KEY_CHECKS = 1;

