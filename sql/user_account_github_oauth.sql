CREATE TABLE IF NOT EXISTS `user_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(100) NOT NULL COMMENT '系统用户名',
  `github_id` bigint DEFAULT NULL COMMENT 'GitHub 用户ID',
  `github_login` varchar(100) DEFAULT NULL COMMENT 'GitHub 登录名',
  `nickname` varchar(100) NOT NULL COMMENT '昵称',
  `avatar` varchar(500) DEFAULT NULL COMMENT '头像地址',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(30) DEFAULT NULL COMMENT '手机号',
  `password_hash` varchar(255) DEFAULT NULL COMMENT '站内密码哈希',
  `bio` varchar(500) DEFAULT NULL COMMENT '简介',
  `role` varchar(30) NOT NULL DEFAULT 'USER' COMMENT '角色',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account_username` (`username`),
  UNIQUE KEY `uk_user_account_github_id` (`github_id`),
  UNIQUE KEY `uk_user_account_github_login` (`github_login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='站点用户账号表（GitHub OAuth）';

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'password_hash'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `password_hash` varchar(255) DEFAULT NULL COMMENT ''站内密码哈希'' AFTER `email`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'phone'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `phone` varchar(30) DEFAULT NULL COMMENT ''手机号'' AFTER `email`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'github_id'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `github_id` bigint DEFAULT NULL COMMENT ''GitHub 用户ID'' AFTER `username`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'github_login'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `github_login` varchar(100) DEFAULT NULL COMMENT ''GitHub 登录名'' AFTER `github_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'bio'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `bio` varchar(500) DEFAULT NULL COMMENT ''简介'' AFTER `password_hash`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'role'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `role` varchar(30) NOT NULL DEFAULT ''USER'' COMMENT ''角色'' AFTER `bio`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'status'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `status` tinyint(1) NOT NULL DEFAULT ''1'' COMMENT ''状态'' AFTER `role`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'last_login_time'
  ),
  'SELECT 1',
  'ALTER TABLE `user_account` ADD COLUMN `last_login_time` datetime DEFAULT NULL COMMENT ''最后登录时间'' AFTER `status`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- admin 测试账号会在后端启动时自动补齐/修正：
-- 用户名：admin
-- 角色：ADMIN
-- 初始密码：123456
