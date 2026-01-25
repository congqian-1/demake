-- 修复 mes_work_order_correction_log 表结构以匹配实体类
USE mes;

-- 删除旧表（如果需要保留数据，先备份）
DROP TABLE IF EXISTS `mes_work_order_correction_log`;

-- 创建新表（与 MesCorrectionLog 实体类匹配）
CREATE TABLE `mes_work_order_correction_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `work_order_id` BIGINT NOT NULL COMMENT '工单ID（外键）',
  `batch_num` VARCHAR(100) DEFAULT NULL COMMENT '批次号（冗余字段，方便按批次查询）',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号（冗余字段）',
  `correction_type` VARCHAR(50) DEFAULT NULL COMMENT '修正类型',
  `old_value` TEXT DEFAULT NULL COMMENT '修正前的值',
  `new_value` TEXT DEFAULT NULL COMMENT '修正后的值',
  `correction_reason` VARCHAR(500) DEFAULT NULL COMMENT '修正原因',
  `operator_id` VARCHAR(100) DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `correction_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '修正时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标识（0-未删除、1-已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_id` (`work_order_id`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_batch_num` (`batch_num`),
  KEY `idx_correction_time` (`correction_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单修正日志表';

SELECT 'Table mes_work_order_correction_log updated successfully' AS status;
