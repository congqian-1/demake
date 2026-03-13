SET @db_name = DATABASE();

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'mes_part'
          AND COLUMN_NAME = 'rotate'
    ),
    'SELECT ''column rotate already exists''',
    'ALTER TABLE mes_part ADD COLUMN rotate VARCHAR(32) NULL COMMENT ''旋转字段'' AFTER standard_code'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = @db_name
          AND TABLE_NAME = 'mes_part'
          AND COLUMN_NAME = 'process_code'
    ),
    'SELECT ''column process_code already exists''',
    'ALTER TABLE mes_part ADD COLUMN process_code VARCHAR(128) NULL COMMENT ''工艺代码'' AFTER rotate'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
