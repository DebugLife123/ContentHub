-- Immutable mock-payment ledger and idempotency.
-- Legacy subscriptions are represented once with estimated=1 because historical
-- payment amount/timestamp data does not exist; do not infer exact history.
USE contenthub;

CREATE TABLE IF NOT EXISTS `subscription_payments` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `subscription_id` BIGINT NOT NULL,
  `plan_id` BIGINT NOT NULL,
  `creator_id` BIGINT NOT NULL,
  `idempotency_key` VARCHAR(128) NOT NULL,
  `amount_snapshot` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `plan_name_snapshot` VARCHAR(100) DEFAULT NULL,
  `duration_days_snapshot` INT DEFAULT NULL,
  `paid_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estimated` TINYINT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subscription_payment_user_key` (`user_id`,`idempotency_key`),
  KEY `idx_subscription_payment_creator` (`creator_id`),
  KEY `idx_subscription_payment_subscription` (`subscription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Immutable subscription payment ledger';

INSERT INTO `subscription_payments`
  (`user_id`,`subscription_id`,`plan_id`,`creator_id`,`idempotency_key`,
   `amount_snapshot`,`plan_name_snapshot`,`duration_days_snapshot`,`paid_at`,`estimated`)
SELECT s.user_id, s.id, s.plan_id, s.creator_id,
       CONCAT('legacy-subscription-', s.id),
       COALESCE(p.price, 0), p.name, p.duration_days,
       COALESCE(s.create_time, s.start_time, CURRENT_TIMESTAMP), 1
FROM subscriptions s
LEFT JOIN subscription_plans p ON p.id = s.plan_id
LEFT JOIN subscription_payments x ON x.subscription_id = s.id
WHERE x.id IS NULL;
