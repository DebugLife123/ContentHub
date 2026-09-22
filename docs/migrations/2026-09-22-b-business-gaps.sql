-- ============================================================
-- 业务断口补全：创作者申请审核 / Skill 评论 / 站内通知 / 订阅终止退款
--
-- 用法（对已有数据库增量执行，可重复跑）：
--   docker exec -i contenthub-mysql mysql -uroot -p<密码> --default-character-set=utf8mb4 < 本文件
--
-- 全新初始化不需要这个文件：docs/database.sql 里已经包含同样的表结构。
-- ============================================================
USE contenthub;

-- ------------------------------------------------------------
-- 1. 创作者申请（把「申请即提权」改成「提交 -> 管理员审核」）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `creator_application` (
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

-- ------------------------------------------------------------
-- 2. Skill 评论（内容库的 comments 绑在 content_id 上，Skill 单开一张）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `skill_comment` (
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

-- ------------------------------------------------------------
-- 3. 站内通知（审核结果、申请结果等主动告诉用户）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `notification` (
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

-- ------------------------------------------------------------
-- 4. 订阅终止 / 退款：加一列记录终止时间，并放开 status 的取值说明
--    列的新增用 information_schema 判断，保证本文件可以重复执行
-- ------------------------------------------------------------
SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME = 'closed_time'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE `subscriptions` ADD COLUMN `closed_time` DATETIME NULL COMMENT ''提前终止/退款时间'' AFTER `end_time`',
  'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- status 取值由 ACTIVE/EXPIRED/CANCELED 扩展为 ACTIVE/EXPIRED/CANCELED/REFUNDED
ALTER TABLE `subscriptions`
  MODIFY COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
  COMMENT '状态: ACTIVE生效中 / EXPIRED已过期 / CANCELED已终止 / REFUNDED已退款';

-- ------------------------------------------------------------
-- 5. 校验
-- ------------------------------------------------------------
SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('creator_application','skill_comment','notification');

SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subscriptions' AND COLUMN_NAME IN ('status','closed_time');
