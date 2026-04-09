SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'article'
      AND COLUMN_NAME = 'author_id'
  ),
  'SELECT 1',
  'ALTER TABLE `article` ADD COLUMN `author_id` bigint DEFAULT NULL COMMENT ''作者用户ID'' AFTER `id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'moment'
      AND COLUMN_NAME = 'author_id'
  ),
  'SELECT 1',
  'ALTER TABLE `moment` ADD COLUMN `author_id` bigint DEFAULT NULL COMMENT ''作者用户ID'' AFTER `id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'forum_post'
      AND COLUMN_NAME = 'author_id'
  ),
  'SELECT 1',
  'ALTER TABLE `forum_post` ADD COLUMN `author_id` bigint DEFAULT NULL COMMENT ''作者用户ID'' AFTER `id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `article`
SET `author_id` = (SELECT `id` FROM `user_account` WHERE `username` = 'admin' LIMIT 1)
WHERE `author_id` IS NULL;

UPDATE `moment`
SET `author_id` = (SELECT `id` FROM `user_account` WHERE `username` = 'admin' LIMIT 1)
WHERE `author_id` IS NULL;

UPDATE `forum_post`
SET `author_id` = (SELECT `id` FROM `user_account` WHERE `username` = 'admin' LIMIT 1)
WHERE `author_id` IS NULL;
