-- ============================================================================
-- MES 系统对接集成 - 数据库表结构脚本
-- Feature: 001-mes-integration
-- Created: 2026-01-25
-- ============================================================================

-- 1. 批次表 (mes_batch)
CREATE TABLE `mes_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（唯一）',
  `batch_type` TINYINT NOT NULL COMMENT '批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板）',
  `product_time` DATE NOT NULL COMMENT '生产日期',
  `simple_batch_num` VARCHAR(50) DEFAULT NULL COMMENT '简易批次号',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_num` (`batch_num`),
  KEY `idx_product_time` (`product_time`),
  KEY `idx_batch_type` (`batch_type`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='批次表';

-- 2. 优化文件表 (mes_optimizing_file)
CREATE TABLE `mes_optimizing_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID（外键）',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（冗余字段）',
  `optimizing_file_name` VARCHAR(200) NOT NULL COMMENT '优化文件名称',
  `station_code` VARCHAR(50) NOT NULL COMMENT '工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002）',
  `urgency` TINYINT DEFAULT 0 COMMENT '是否加急（0=不加急/1=加急）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_file` (`batch_num`, `optimizing_file_name`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_station_code` (`station_code`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优化文件表';

-- 3. 工单表 (mes_work_order)
CREATE TABLE `mes_work_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID（外键）',
  `optimizing_file_id` BIGINT NOT NULL COMMENT '优化文件ID（外键）',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（冗余字段，方便查询）',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号（唯一）',
  `route` VARCHAR(100) NOT NULL COMMENT '线路',
  `order_type` VARCHAR(50) NOT NULL COMMENT '订单类型',
  `prepackage_status` VARCHAR(20) NOT NULL DEFAULT 'NOT_PULLED' COMMENT '预包装数据拉取状态（NOT_PULLED=未拉取/PULLING=拉取中/PULLED=已拉取/FAILED=拉取失败/NO_DATA=无预包装数据/UPDATING=更新中）',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `last_pull_time` DATETIME DEFAULT NULL COMMENT '最后拉取时间',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息（拉取失败时）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_id` (`work_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_optimizing_file_id` (`optimizing_file_id`),
  KEY `idx_prepackage_status` (`prepackage_status`),
  KEY `idx_last_pull_time` (`last_pull_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

-- 4. 预包装订单表 (mes_prepackage_order)
CREATE TABLE `mes_prepackage_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` BIGINT NOT NULL COMMENT '工单ID（外键）',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID（冗余字段，方便按批次统计）',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（冗余字段，方便查询）',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号（冗余字段）',
  `order_num` VARCHAR(100) DEFAULT NULL COMMENT '订单号',
  `consignor` VARCHAR(200) DEFAULT NULL COMMENT '客户名称（WMS货主）',
  `contract_no` VARCHAR(100) DEFAULT NULL COMMENT '合同编号',
  `work_num` VARCHAR(100) DEFAULT NULL COMMENT '工单号',
  `receiver` VARCHAR(100) DEFAULT NULL COMMENT '收货人',
  `phone` VARCHAR(50) DEFAULT NULL COMMENT '联系电话',
  `ship_batch` VARCHAR(100) DEFAULT NULL COMMENT '出货批次号',
  `install_address` VARCHAR(500) DEFAULT NULL COMMENT '安装地址',
  `customer` VARCHAR(200) DEFAULT NULL COMMENT '终端客户名',
  `receive_region` VARCHAR(200) DEFAULT NULL COMMENT '收货地区',
  `space` VARCHAR(100) DEFAULT NULL COMMENT '产品所属空间',
  `pack_type` VARCHAR(50) DEFAULT NULL COMMENT '包件类型',
  `product_type` VARCHAR(50) DEFAULT NULL COMMENT '产品类型',
  `prepackage_info_size` INT DEFAULT NULL COMMENT '预包装总包数',
  `total_set` INT DEFAULT NULL COMMENT '总套数',
  `max_package_no` INT DEFAULT NULL COMMENT '一套内的总包数',
  `production_num` VARCHAR(100) DEFAULT NULL COMMENT '生产编号',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_work_order` (`work_order_id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_batch_num` (`batch_num`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_order_num` (`order_num`),
  KEY `idx_production_num` (`production_num`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预包装订单表';

-- 5. 箱码表 (mes_box)
CREATE TABLE `mes_box` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `prepackage_order_id` BIGINT NOT NULL COMMENT '预包装订单ID（外键）',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（冗余字段，方便按批次查询）',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号（冗余字段）',
  `box_code` VARCHAR(100) NOT NULL COMMENT '箱码',
  `building` VARCHAR(100) DEFAULT NULL COMMENT '楼栋',
  `house` VARCHAR(100) DEFAULT NULL COMMENT '户型',
  `room` VARCHAR(100) DEFAULT NULL COMMENT '房间号',
  `setno` INT DEFAULT NULL COMMENT '第几套',
  `color` VARCHAR(50) DEFAULT NULL COMMENT '颜色',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_box_code` (`box_code`),
  KEY `idx_prepackage_order` (`prepackage_order_id`),
  KEY `idx_batch_num` (`batch_num`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='箱码表';

-- 6. 包件表 (mes_package)
CREATE TABLE `mes_package` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `box_id` BIGINT NOT NULL COMMENT '箱码ID（外键）',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（冗余字段，方便按批次查询）',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号（冗余字段）',
  `box_code` VARCHAR(100) NOT NULL COMMENT '箱码（冗余字段）',
  `package_no` INT NOT NULL COMMENT '第几包',
  `length` DECIMAL(10,2) DEFAULT NULL COMMENT '长度（单位：mm）',
  `width` DECIMAL(10,2) DEFAULT NULL COMMENT '宽度（单位：mm）',
  `depth` DECIMAL(10,2) DEFAULT NULL COMMENT '高度（单位：mm）',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量（单位：kg）',
  `part_count` INT DEFAULT NULL COMMENT '部件数',
  `box_type` VARCHAR(50) DEFAULT NULL COMMENT '纸箱类型（如"地盖"）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_box_package` (`box_code`, `package_no`),
  KEY `idx_box_id` (`box_id`),
  KEY `idx_batch_num` (`batch_num`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包件表';

-- 7. 板件表 (mes_part)
CREATE TABLE `mes_part` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `package_id` BIGINT NOT NULL COMMENT '包件ID（外键）',
  `box_id` BIGINT NOT NULL COMMENT '箱码ID（冗余字段）',
  `batch_num` VARCHAR(100) NOT NULL COMMENT '批次号（冗余字段，方便按批次查询）',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号（冗余字段）',
  `part_code` VARCHAR(100) NOT NULL COMMENT '部件条码（唯一）',
  `layer` INT DEFAULT NULL COMMENT '第几层',
  `piece` INT DEFAULT NULL COMMENT '第几片',
  `item_code` VARCHAR(100) DEFAULT NULL COMMENT '板件ID',
  `item_name` VARCHAR(500) DEFAULT NULL COMMENT '板件描述',
  `mat_name` VARCHAR(100) DEFAULT NULL COMMENT '花色（分拣要的花色）',
  `item_length` DECIMAL(10,2) DEFAULT NULL COMMENT '板件长',
  `item_width` DECIMAL(10,2) DEFAULT NULL COMMENT '板件宽',
  `item_depth` DECIMAL(10,2) DEFAULT NULL COMMENT '板件高',
  `x_axis` DECIMAL(10,2) DEFAULT NULL COMMENT 'X轴坐标',
  `y_axis` DECIMAL(10,2) DEFAULT NULL COMMENT 'Y轴坐标',
  `z_axis` DECIMAL(10,2) DEFAULT NULL COMMENT 'Z轴坐标',
  `sort_order` INT DEFAULT NULL COMMENT '分拣出板顺序',
  `standard_list` JSON DEFAULT NULL COMMENT '标准码集合（JSON格式，如[{"00041":1,"00311":1}]）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除/关联板件已失效）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_part_code` (`part_code`),
  KEY `idx_package_id` (`package_id`),
  KEY `idx_box_id` (`box_id`),
  KEY `idx_batch_num` (`batch_num`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='板件表';

-- 8. 报工记录表 (mes_work_report)
CREATE TABLE `mes_work_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `part_code` VARCHAR(100) NOT NULL COMMENT '部件条码（外键关联，但不设物理外键）',
  `work_id` VARCHAR(100) DEFAULT NULL COMMENT '工单号（冗余字段）',
  `part_status` VARCHAR(50) NOT NULL COMMENT '板件状态值',
  `station_code` VARCHAR(50) NOT NULL COMMENT '工位编码',
  `station_name` VARCHAR(100) DEFAULT NULL COMMENT '工位名称',
  `report_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人（产线客户端系统ID或操作员）',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_part_code` (`part_code`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_station_code` (`station_code`),
  KEY `idx_report_time` (`report_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报工记录表';

-- 9. 工单数据修正日志表 (mes_work_order_correction_log)
CREATE TABLE `mes_work_order_correction_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `work_order_id` BIGINT NOT NULL COMMENT '工单ID',
  `work_id` VARCHAR(100) NOT NULL COMMENT '工单号',
  `operator` VARCHAR(100) DEFAULT NULL COMMENT '操作人',
  `operation_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `correction_reason` VARCHAR(500) DEFAULT NULL COMMENT '修正原因',
  `old_status` VARCHAR(20) DEFAULT NULL COMMENT '修正前状态',
  `new_status` VARCHAR(20) DEFAULT NULL COMMENT '修正后状态',
  `part_count_before` INT DEFAULT NULL COMMENT '修正前板件数量',
  `part_count_after` INT DEFAULT NULL COMMENT '修正后板件数量',
  `result` VARCHAR(20) DEFAULT NULL COMMENT '修正结果（SUCCESS/FAILED）',
  `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人（系统/管理员）',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_work_order_id` (`work_order_id`),
  KEY `idx_work_id` (`work_id`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单数据修正日志表';

-- 10. 邮件通知配置表 (mes_email_notification_config)
CREATE TABLE `mes_email_notification_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `smtp_host` VARCHAR(100) NOT NULL COMMENT 'SMTP服务器地址',
  `smtp_port` INT NOT NULL COMMENT 'SMTP端口',
  `username` VARCHAR(100) NOT NULL COMMENT '发件人账号',
  `password` VARCHAR(200) NOT NULL COMMENT '授权码（加密存储）',
  `from_address` VARCHAR(100) NOT NULL COMMENT '发件人邮箱地址',
  `to_addresses` VARCHAR(500) NOT NULL COMMENT '收件人地址列表（逗号分隔）',
  `enabled` TINYINT DEFAULT 1 COMMENT '是否启用（0=禁用，1=启用）',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '删除标识（0=正常，1=已删除）',
  `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` VARCHAR(100) DEFAULT NULL COMMENT '更新人',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件通知配置表';
