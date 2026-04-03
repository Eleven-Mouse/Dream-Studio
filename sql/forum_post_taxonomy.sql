SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'forum_post'
      AND COLUMN_NAME = 'category_id'
  ),
  'SELECT 1',
  'ALTER TABLE `forum_post` ADD COLUMN `category_id` bigint DEFAULT NULL COMMENT ''分类ID'' AFTER `content`'
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
      AND COLUMN_NAME = 'tags'
  ),
  'SELECT 1',
  'ALTER TABLE `forum_post` ADD COLUMN `tags` varchar(256) DEFAULT NULL COMMENT ''标签ID列表，逗号分隔'' AFTER `category_id`'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS(
    SELECT 1
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'forum_post'
      AND INDEX_NAME = 'idx_forum_post_category_id'
  ),
  'SELECT 1',
  'ALTER TABLE `forum_post` ADD INDEX `idx_forum_post_category_id` (`category_id`)'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
