-- ============================================================
-- ContentHub 数据库建表脚本
-- 目标库: contenthub  (MySQL 8.0, utf8mb4)
--
-- 说明：本脚本同时挂载到 MySQL 容器的 /docker-entrypoint-initdb.d/，
--       首次创建数据卷时自动执行（见根目录 docker-compose.yml）。
--       因此脚本必须保持「可重复执行」——开头会 DROP DATABASE。
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
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED正常 / DISABLED已禁用（禁用后无法登录）',
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
-- 3. 内容分类表（计划表 6 标记为「必须」）
-- ----------------------------
DROP TABLE IF EXISTS `content_category`;
CREATE TABLE `content_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(60) NOT NULL COMMENT '分类名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值(越小越靠前)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED启用 / DISABLED禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容分类表';

-- ----------------------------
-- 4. 内容表（技术文章/系列教程/电子书/视频课程/PDF/数据集）
-- ----------------------------
DROP TABLE IF EXISTS `contents`;
CREATE TABLE `contents` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '内容ID',
  `creator_id` BIGINT NOT NULL COMMENT '创作者ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID -> content_category.id',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `cover` VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
  `content_type` VARCHAR(20) NOT NULL COMMENT '内容类型: ARTICLE/TUTORIAL/EBOOK/VIDEO/PDF/DATASET',
  `body` MEDIUMTEXT COMMENT '正文内容(富文本/Markdown)',
  `file_url` VARCHAR(500) DEFAULT NULL COMMENT '附件文件URL(电子书/PDF/视频/数据集等)',
  `access_type` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '访问类型: FREE免费 / SUBSCRIBED订阅可见',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT草稿 / PENDING待审核 / PUBLISHED已发布 / REJECTED已驳回 / OFFLINE已下架',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` INT NOT NULL DEFAULT 0 COMMENT '点赞数',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '审核驳回原因（仅 status=REJECTED 时有值）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_type` (`content_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内容表';

-- ----------------------------
-- 5. 订阅套餐表
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
-- 6. 用户订阅记录表
-- ----------------------------
DROP TABLE IF EXISTS `subscriptions`;
CREATE TABLE `subscriptions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订阅ID',
  `user_id` BIGINT NOT NULL COMMENT '订阅用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `creator_id` BIGINT NOT NULL COMMENT '创作者ID',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME NOT NULL COMMENT '到期时间',
  `closed_time` DATETIME DEFAULT NULL COMMENT '提前终止/退款时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE生效中 / EXPIRED已过期 / CANCELED已终止 / REFUNDED已退款',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_creator` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅记录表';

-- ----------------------------
-- 7. 收藏表
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
-- 8. 评论表
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
-- 9. 阅读记录表（计划表 6 列为「建议」，阶段 5 Day 46 使用）
-- ----------------------------
DROP TABLE IF EXISTS `reading_history`;
CREATE TABLE `reading_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `content_id` BIGINT NOT NULL COMMENT '内容ID',
  `progress` INT NOT NULL DEFAULT 0 COMMENT '阅读进度百分比 0-100',
  `last_read_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后阅读时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_content` (`user_id`,`content_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_content` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读记录表';

-- ----------------------------
-- 10. Skill 分类表（Skill 商城的栏目，与内容库的 content_category 相互独立）
-- ----------------------------
DROP TABLE IF EXISTS `skill_category`;
CREATE TABLE `skill_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(60) NOT NULL COMMENT '分类名称',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序值(越小越靠前)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED启用 / DISABLED禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_category_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 分类表';

-- ----------------------------
-- 11. Skill 表（由管理员在管理端维护，没有创作者投稿与审核环节）
-- ----------------------------
DROP TABLE IF EXISTS `skill`;
CREATE TABLE `skill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Skill ID',
  `name` VARCHAR(100) NOT NULL COMMENT '名称',
  `icon` VARCHAR(16) DEFAULT NULL COMMENT '图标(emoji)',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID -> skill_category.id',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '一句话简介',
  `why_included` VARCHAR(1000) DEFAULT NULL COMMENT '为什么收录',
  `author` VARCHAR(100) DEFAULT NULL COMMENT '上游作者',
  `repo` VARCHAR(200) DEFAULT NULL COMMENT '仓库，形如 owner/name',
  `official_url` VARCHAR(500) DEFAULT NULL COMMENT '官网/仓库地址',
  `install_command` VARCHAR(500) DEFAULT NULL COMMENT '安装命令',
  `stars` INT NOT NULL DEFAULT 0 COMMENT 'GitHub 星数(列表默认排序依据)',
  `version` VARCHAR(50) DEFAULT NULL COMMENT '版本号',
  `license` VARCHAR(50) DEFAULT NULL COMMENT '开源许可证',
  `size` VARCHAR(50) DEFAULT NULL COMMENT '体积，形如 4.0 MB',
  `downloads` INT NOT NULL DEFAULT 0 COMMENT '下载/安装次数',
  `security_level` INT NOT NULL DEFAULT 0 COMMENT '安全评级 0-5',
  `security_label` VARCHAR(50) DEFAULT NULL COMMENT '安全评级文案',
  `submitter` VARCHAR(100) DEFAULT NULL COMMENT '提交人(展示用)',
  `submit_time` DATE DEFAULT NULL COMMENT '提交时间(展示用)',
  `platforms` VARCHAR(300) DEFAULT NULL COMMENT '兼容平台，逗号分隔',
  `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签，逗号分隔',
  `features` TEXT COMMENT '功能特点，一行一条',
  `quick_start` TEXT COMMENT '快速上手步骤，JSON 数组 [{title,detail}]',
  `team_maintainers` INT NOT NULL DEFAULT 0 COMMENT '维护者数量',
  `team_contributors` INT NOT NULL DEFAULT 0 COMMENT '贡献者数量',
  `team_open_issues` INT NOT NULL DEFAULT 0 COMMENT '未解决 Issue 数',
  `team_last_commit` DATE DEFAULT NULL COMMENT '最近提交日期',
  `access_type` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '访问类型: FREE免费 / MEMBER会员解锁',
  `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态: DRAFT草稿 / PUBLISHED已上架 / OFFLINE已下架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_skill_name` (`name`),
  KEY `idx_skill_category` (`category_id`),
  KEY `idx_skill_status` (`status`),
  KEY `idx_skill_stars` (`stars`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 表';

-- ----------------------------
-- 12. 创作者申请表（申请 -> 管理员审核 -> 通过才升级角色）
-- ----------------------------
DROP TABLE IF EXISTS `creator_application`;
CREATE TABLE `creator_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '申请人',
  `intro` VARCHAR(500) DEFAULT NULL COMMENT '申请说明（想让管理员看到什么）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING待审核 / APPROVED已通过 / REJECTED已驳回',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '驳回原因，仅 REJECTED 时有值',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审核人',
  `review_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_ca_user` (`user_id`),
  KEY `idx_ca_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='创作者申请表';

-- ----------------------------
-- 13. Skill 评论表（内容库的 comments 绑在 content_id 上，Skill 单开一张）
-- ----------------------------
DROP TABLE IF EXISTS `skill_comment`;
CREATE TABLE `skill_comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `skill_id` BIGINT NOT NULL COMMENT 'Skill ID',
  `user_id` BIGINT NOT NULL COMMENT '评论用户ID',
  `body` VARCHAR(1000) NOT NULL COMMENT '评论内容',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL / HIDDEN',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_sc_skill` (`skill_id`),
  KEY `idx_sc_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Skill 评论表';

-- ----------------------------
-- 14. 站内通知表（审核结果、申请结果等主动告诉用户）
-- ----------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` BIGINT NOT NULL COMMENT '接收人',
  `type` VARCHAR(40) NOT NULL COMMENT 'CONTENT_APPROVED / CONTENT_REJECTED / CONTENT_OFFLINE / CREATOR_APPROVED / CREATOR_REJECTED',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `body` VARCHAR(1000) DEFAULT NULL COMMENT '正文',
  `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '关联业务类型: CONTENT / CREATOR_APPLICATION',
  `biz_id` BIGINT DEFAULT NULL COMMENT '关联业务ID，用于前端跳转',
  `read_flag` TINYINT NOT NULL DEFAULT 0 COMMENT '0未读 / 1已读',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_nt_user_read` (`user_id`,`read_flag`),
  KEY `idx_nt_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';

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
-- 初始数据: 内容分类
-- ----------------------------
-- 栏目按「内容形态」划分：内容库顶部横排标签即这 4 项
INSERT INTO `content_category` (`id`,`name`,`sort`,`status`) VALUES
(1, '技术文章', 10, 'ENABLED'),
(2, '电子书',   20, 'ENABLED'),
(3, '视频课程', 30, 'ENABLED'),
(4, 'PDF',      40, 'ENABLED');

-- ----------------------------
-- 初始数据: 内容（每个栏目至少一篇，便于演示栏目筛选）
-- ----------------------------
INSERT INTO `contents` (`creator_id`,`category_id`,`title`,`summary`,`cover`,`content_type`,`body`,`file_url`,`access_type`,`status`,`view_count`,`like_count`) VALUES
-- 技术文章
(2, 1, '从单体到分层：一次后端结构整理', '把 Controller 写成一锅粥之后，我是怎么收拾的。', NULL, 'ARTICLE', '# 分层的意义\n\nController 只做参数与响应，业务规则全部落在 Service。', NULL, 'FREE', 'PUBLISHED', 430, 41),
-- 电子书
(2, 2, '内容创作者增长手册', '从 0 到 1000 名订阅者的完整路径，含选题与定价方法。', NULL, 'EBOOK', '# 增长手册\n\n一份可以边读边执行的电子书。', 'https://example.com/files/creator-growth.epub', 'SUBSCRIBED', 'PUBLISHED', 940, 97),
-- 视频课程
(2, 3, 'Vue 3 组合式 API 实战课', '12 节视频，把 Composition API 用到真实项目里。', NULL, 'VIDEO', '# 实战课\n\n配套源码与每节课的讲义。', 'https://example.com/videos/vue3-course.mp4', 'SUBSCRIBED', 'PUBLISHED', 1520, 168),
-- PDF
(2, 4, '全栈项目架构手册（PDF）', '一册讲清前后端分层、鉴权与部署的工程决策。', NULL, 'PDF', '# 架构手册\n\n适合当作项目启动前的检查清单。', 'https://example.com/files/architecture.pdf', 'FREE', 'PUBLISHED', 1130, 120);

-- ----------------------------
-- 初始数据: 一条待审核内容（便于直接演示管理端审核流程）
-- ----------------------------
INSERT INTO `contents` (`creator_id`,`category_id`,`title`,`summary`,`content_type`,`body`,`access_type`,`status`) VALUES
(2, 1, '待审核：一次线上事故的复盘', '还在等管理员审核，未发布。', 'ARTICLE', '# 事故复盘\n\n这份内容当前处于 PENDING 状态，通过后才会出现在内容库。', 'SUBSCRIBED', 'PENDING');

-- ----------------------------
-- 初始数据: 订阅套餐（验收要求能创建 Pro / Premium 套餐）
-- ----------------------------
INSERT INTO `subscription_plans` (`creator_id`,`name`,`description`,`price`,`duration_days`,`status`) VALUES
(2, 'Pro 月度会员', '解锁该创作者的全部订阅内容，适合先试一个月。', 29.00, 30, 'ACTIVE'),
(2, 'Premium 年度会员', '一次性解锁一年，相当于只付 10 个月。', 299.00, 365, 'ACTIVE');

-- ----------------------------
-- 初始数据: Skill 分类
-- ----------------------------
INSERT INTO `skill_category` (`id`,`name`,`sort`,`status`) VALUES
(1, '开发工具',   10, 'ENABLED'),
(2, '写作与文档', 20, 'ENABLED'),
(3, '数据处理',   30, 'ENABLED'),
(4, '设计创意',   40, 'ENABLED'),
(5, '自动化',     50, 'ENABLED'),
(6, '安全合规',   60, 'ENABLED');

-- ----------------------------
-- 初始数据: Skill（全部为已上架；access_type=MEMBER 的需要会员解锁）
-- platforms / tags 逗号分隔，features 一行一条，quick_start 为 JSON 数组
-- ----------------------------
INSERT INTO `skill`
(`id`,`name`,`icon`,`category_id`,`summary`,`why_included`,`author`,`repo`,`official_url`,`install_command`,
 `stars`,`version`,`license`,`size`,`downloads`,`security_level`,`security_label`,`submitter`,`submit_time`,
 `platforms`,`tags`,`features`,`quick_start`,
 `team_maintainers`,`team_contributors`,`team_open_issues`,`team_last_commit`,`access_type`,`status`) VALUES
(1,'brainstorming','💡',1,
 '在进行任何创造性工作之前必须使用此技能，包括创建功能、构建组件、增加功能或修改行为。它会在实现前梳理用户意图、需求和设计。',
 '该 Skill 提供与 brainstorming 相关的可复用 Agent 能力，帮助统一团队工作流程并减少重复配置，适合纳入团队 Skill 库。',
 'obra','obra/superpowers','https://github.com/obra/superpowers','git clone https://github.com/obra/superpowers.git',
 12400,'v6.2.0','MIT','4.0 MB',18420,4,'4 级安全认证','张同学','2026-07-31',
 'Claude Code,Codex,Cursor','ai,brainstorming,coding,obra,sdc,skills,subagent-driven-development,superpowers',
 '在进行任何创造性工作之前必须使用此技能，包括创建功能、构建组件、增加功能或修改行为\n提供可复用的 Skill 能力\n支持 Agent 工作流程集成\n产出结构化的需求与设计文档，而不是直接开始写代码',
 '[{"title":"查看项目文档","detail":"先读仓库根目录的 README 与 skills 目录说明，确认版本要求。"},{"title":"克隆并安装 Skill","detail":"把仓库克隆到本地，按文档把 skill 目录链接到你的 Agent 配置中。"},{"title":"在会话中触发","detail":"在开始需求梳理时显式调用该技能，让它先提问再给方案。"}]',
 3,28,12,'2026-07-28','MEMBER','PUBLISHED'),
(2,'superpowers','🦸',5,
 '一整套可组合的 Agent 技能集合，覆盖需求梳理、方案设计、编码、测试与提交，把零散的提示词沉淀成可复用的工作流。',
 '它是 brainstorming、subagent-driven-development 等技能的载体，单独收录便于用户一次装齐整套能力。',
 'obra','obra/superpowers','https://github.com/obra/superpowers','git clone https://github.com/obra/superpowers.git',
 18200,'v6.2.0','MIT','12.6 MB',26310,4,'4 级安全认证','张同学','2026-07-31',
 'Claude Code,Codex,Cursor,Gemini CLI','agent,workflow,skills,superpowers,productivity',
 '把常用 Agent 技能集中到一个仓库，统一版本与更新\n技能之间可以互相调用，形成完整工作流\n内置技能清单与触发条件，避免模型「想到才用」\n支持按团队需要裁剪，只保留用得到的技能',
 '[{"title":"克隆仓库","detail":"仓库体积较大，建议浅克隆。"},{"title":"挑选需要的技能","detail":"不必全装，按团队实际流程保留子集。"},{"title":"验证触发","detail":"用一个真实小需求走一遍，确认技能会被正确触发。"}]',
 3,41,18,'2026-07-29','MEMBER','PUBLISHED'),
(3,'subagent-driven-development','🧵',1,
 '把一个大任务拆成若干子任务交给 subagent 并行推进，主会话只负责编排与验收，适合改动面大但彼此独立的开发任务。',
 '多文件、多模块的改造任务用单会话推进容易出现上下文漂移，这个 Skill 给出了可复用的拆分与验收方法。',
 'obra','obra/superpowers','https://github.com/obra/superpowers','git clone https://github.com/obra/superpowers.git',
 6800,'v6.2.0','MIT','3.1 MB',9120,4,'4 级安全认证','张同学','2026-07-30',
 'Claude Code,Codex','subagent,parallel,workflow,sdc',
 '自动把任务拆成可并行的子任务\n子任务各自独立上下文，减少相互污染\n主会话保留验收职责，不会把结论直接吞掉\n支持串行与并行两种编排模式',
 '[{"title":"确认任务可拆分","detail":"子任务之间有共享状态时，强行并行反而更慢。"},{"title":"定义验收标准","detail":"先写清楚每个子任务算完成的条件。"},{"title":"汇总与回归","detail":"主会话合并结果后跑一次完整测试。"}]',
 3,22,9,'2026-07-30','MEMBER','PUBLISHED'),
(4,'anthropic-skills','🏛️',1,
 '官方维护的 Skill 示例集合，给出 Skill 目录结构、描述文件写法与触发条件设计的参考实现。',
 '写自己的 Skill 之前先把官方示例读一遍，能省掉大量试错。',
 'anthropics','anthropics/skills','https://github.com/anthropics/skills','git clone https://github.com/anthropics/skills.git',
 9800,'v1.4.0','Apache-2.0','2.4 MB',15780,5,'5 级安全认证','官方收录','2026-07-22',
 'Claude Code,Claude Desktop','official,skills,reference,anthropic',
 '官方示例，目录结构与字段含义最权威\n覆盖文档处理、数据整理、代码审查等常见场景\n每个示例都附带 SKILL.md，可直接对照写自己的技能\n持续更新，跟着平台能力变化走',
 '[{"title":"浏览示例目录","detail":"按场景找到最接近你需求的那个示例。"},{"title":"复制并改造","detail":"复制目录后改 SKILL.md 的描述与触发条件。"}]',
 8,64,21,'2026-07-21','FREE','PUBLISHED'),
(5,'markitdown','📄',3,
 '把 PDF、Word、Excel、PPT、图片等格式统一转换成 Markdown，方便丢给模型做后续处理，保留标题层级与表格结构。',
 '内容平台最常见的预处理需求就是「把各种附件变成可检索的文本」，这个工具的覆盖面和维护活跃度都够。',
 'microsoft','microsoft/markitdown','https://github.com/microsoft/markitdown','git clone https://github.com/microsoft/markitdown.git',
 76400,'v0.1.2','MIT','6.8 MB',42150,5,'5 级安全认证','官方收录','2026-07-18',
 'Claude Code,Codex,Cursor','markdown,pdf,converter,microsoft,rag',
 '一份代码覆盖 PDF / Office / 图片 / 音频等常见格式\n尽量保留标题层级、列表与表格，而不是拍平成纯文本\n可当命令行工具用，也可作为库嵌进自己的流水线\n转换结果天然适合做 RAG 的语料预处理',
 '[{"title":"安装依赖","detail":"按文档装 Python 包与可选的多媒体依赖。"},{"title":"转换单个文件","detail":"先用命令行转一份 PDF，检查表格是否完整。"},{"title":"接进流水线","detail":"再作为库调用，批量处理上传的附件。"}]',
 6,132,87,'2026-07-17','FREE','PUBLISHED'),
(6,'mermaid-diagrammer','📊',4,
 '用文本描述直接生成流程图、时序图、类图与甘特图，适合把架构说明写进 Markdown 而不依赖外部绘图工具。',
 '技术文档里最难维护的就是图，文本化之后图和文档终于能一起 review。',
 'mermaid-js','mermaid-js/mermaid','https://github.com/mermaid-js/mermaid','git clone https://github.com/mermaid-js/mermaid.git',
 78200,'v11.2.0','MIT','18.3 MB',33800,5,'5 级安全认证','官方收录','2026-07-26',
 'Claude Code,Cursor,GitHub','diagram,mermaid,docs,visualization',
 '流程图、时序图、状态图、类图、甘特图一套语法全覆盖\n图表就是文本，可以进 Git 做版本对比\nGitHub、语雀、多数 Markdown 编辑器原生渲染\n主题可配置，能对齐项目的配色',
 '[{"title":"选一张图试手","detail":"从流程图开始，语法最简单。"},{"title":"接进文档","detail":"把图块直接贴进 Markdown，确认渲染环境支持。"}]',
 12,480,310,'2026-07-25','FREE','PUBLISHED'),
(7,'excel-analyst','📈',3,
 '让 Agent 直接读写 Excel：按自然语言筛选、透视、生成图表，并把结论写成带批注的表格回写。',
 '运营和财务的日常需求大量集中在 Excel 上，把它做成 Skill 比每次现写脚本稳定得多。',
 'datawizard','datawizard/excel-analyst','https://github.com/datawizard/excel-analyst','git clone https://github.com/datawizard/excel-analyst.git',
 5400,'v2.1.0','MIT','5.2 MB',6740,3,'3 级安全认证','李工','2026-07-12',
 'Claude Code,Codex','excel,analysis,report,automation',
 '支持多 Sheet 与合并单元格，不会读错表头\n按自然语言做分组、透视与同环比\n生成的结论可以直接回写成批注\n附带一份常用公式速查，减少来回确认',
 '[{"title":"准备样例表","detail":"先用一份脱敏数据验证读取效果。"},{"title":"描述你的分析意图","detail":"说清维度、指标和时间范围。"},{"title":"核对结果","detail":"重要结论建议手动复算一遍。"}]',
 2,14,6,'2026-07-11','MEMBER','PUBLISHED'),
(8,'security-audit','🛡️',6,
 '对改动做安全自查：检查越权、注入、敏感信息泄漏与依赖漏洞，输出可执行的修复清单而不是一堆告警。',
 '安全问题在评审阶段发现成本最低，把它固化成一个必跑的 Skill 比靠人记得住更靠谱。',
 'secops-lab','secops-lab/security-audit','https://github.com/secops-lab/security-audit','git clone https://github.com/secops-lab/security-audit.git',
 4300,'v1.8.3','Apache-2.0','7.4 MB',5210,4,'4 级安全认证','王工','2026-07-08',
 'Claude Code,Codex,Cursor','security,audit,dependencies,owasp',
 '按 OWASP 常见风险逐项检查，不遗漏越权与注入\n扫描依赖版本，标出已知漏洞与可升级版本\n检查日志与配置里是否写进了密钥\n输出带优先级的修复清单，可直接转成工单',
 '[{"title":"在提交前运行","detail":"建议接进 pre-push 钩子或 CI。"},{"title":"处理高危项","detail":"先修高危与密钥泄漏，其余排期。"}]',
 4,19,11,'2026-07-07','MEMBER','PUBLISHED'),
(9,'doc-writer','✍️',2,
 '按项目的文档规范生成 README、接口说明与变更日志草稿，写完还会自查有没有漏掉必填章节。',
 '「代码写完了文档没写」是绝大多数项目的常态，把它变成一条命令能显著提高文档覆盖率。',
 'openwriter','openwriter/doc-writer','https://github.com/openwriter/doc-writer','git clone https://github.com/openwriter/doc-writer.git',
 2900,'v1.2.1','MIT','1.9 MB',4480,4,'4 级安全认证','小林','2026-06-30',
 'Claude Code,Cursor','docs,readme,changelog,writing',
 '支持 README、接口说明、变更日志三类常用文档\n从代码与提交记录里抽取事实，不凭空编造\n生成后自查章节完整性，缺哪块会指出来\n可套用项目自己的文档模板',
 '[{"title":"放入模板","detail":"把团队的文档模板放进指定目录。"},{"title":"生成草稿","detail":"生成后一定要人工通读，事实性内容以代码为准。"}]',
 1,7,3,'2026-06-28','FREE','PUBLISHED'),
(10,'ci-automator','⚙️',5,
 '根据项目实际技术栈生成 GitHub Actions 工作流：构建、测试、镜像打包与发布，并说明每一步为什么这么配。',
 'CI 配置的坑集中在缓存与权限上，一份带注释的可用模板比翻文档快得多。',
 'devops-kit','devops-kit/ci-automator','https://github.com/devops-kit/ci-automator','git clone https://github.com/devops-kit/ci-automator.git',
 5100,'v3.0.2','MIT','4.6 MB',7320,3,'3 级安全认证','王工','2026-07-19',
 'Claude Code,Codex','ci,github-actions,devops,docker',
 '自动识别 Maven / npm / Gradle 等构建方式\n内置依赖缓存配置，流水线明显更快\n镜像构建与推送按分支区分策略\n生成的 YAML 带注释，便于后续自己改',
 '[{"title":"确认技术栈","detail":"多模块项目先说明模块划分。"},{"title":"生成本地验证","detail":"用 act 之类工具先在本地跑一遍。"},{"title":"按需裁剪","detail":"不需要的步骤直接删掉，别留着占时间。"}]',
 3,16,8,'2026-07-18','MEMBER','PUBLISHED'),
(11,'ui-reviewer','🎨',4,
 '对页面实现做设计走查：间距、层级、对比度、响应式断点与可访问性，按严重程度给出修改建议。',
 '设计走查最容易漏细节，把可量化的部分交给工具，人只需要判断审美与业务取舍。',
 'designops','designops/ui-reviewer','https://github.com/designops/ui-reviewer','git clone https://github.com/designops/ui-reviewer.git',
 3600,'v1.5.0','MIT','2.8 MB',3980,4,'4 级安全认证','阿珂','2026-07-05',
 'Claude Code,Cursor','design,accessibility,review,css',
 '检查间距与对齐是否符合项目的栅格约定\n检查文字对比度是否满足可访问性要求\n验证常用断点下的布局，指出溢出与遮挡\n按严重程度排序，先修影响可用性的问题',
 '[{"title":"提供项目设计变量","detail":"把颜色、间距的变量文件给它，结论才准。"},{"title":"跑一遍关键页面","detail":"优先首页、列表页与表单页。"}]',
 2,11,5,'2026-07-04','MEMBER','PUBLISHED'),
(12,'prompt-library','📚',2,
 '把团队沉淀的提示词整理成可检索、可版本化的库，按场景分类并标注适用模型与已知失效情况。',
 '提示词散落在聊天记录里等于没有沉淀，这个 Skill 给出了低成本的组织方式。',
 'promptworks','promptworks/prompt-library','https://github.com/promptworks/prompt-library','git clone https://github.com/promptworks/prompt-library.git',
 12800,'v4.1.0','CC-BY-4.0','9.7 MB',12640,3,'3 级安全认证','李工','2026-07-24',
 'Claude Code,Codex,Cursor,Gemini CLI','prompt,library,knowledge,team',
 '按场景与模型分类，支持关键词检索\n每条提示词都记录适用版本与失效时间\n改动走 Git，谁改的、为什么改一目了然\n支持导出给非技术同学直接使用',
 '[{"title":"先做一次存量清理","detail":"把散落的提示词集中进来，去重比新增更重要。"},{"title":"约定评审规则","detail":"新增与修改都走一次简单评审。"}]',
 5,37,14,'2026-07-23','MEMBER','PUBLISHED');

SET FOREIGN_KEY_CHECKS = 1;
