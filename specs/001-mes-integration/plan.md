# Technical Implementation Plan: MES 系统对接集成

**Feature Branch**: `001-mes-integration`  
**Created**: 2026-01-25  
**Spec Version**: Draft (已通过需求质量检查)  
**Target Module**: `mes-service1`  
**Estimated Duration**: 10-12 工作日

---

## 目录

1. [技术架构设计](#1-技术架构设计)
2. [数据库设计](#2-数据库设计)
3. [接口设计](#3-接口设计)
4. [核心功能实现](#4-核心功能实现)
5. [任务分解与排期](#5-任务分解与排期)
6. [技术风险与对策](#6-技术风险与对策)
7. [测试策略](#7-测试策略)

---

## 1. 技术架构设计

### 1.1 技术栈选型

基于现有项目技术栈（Macula Boot 5.0.15 + Spring Boot + MyBatis-Plus）：

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| **Web框架** | Spring Boot 3.x (Macula Boot) | 已集成 `macula-boot-starter-web` |
| **ORM框架** | MyBatis-Plus | 已集成 `macula-boot-starter-mybatis-plus` |
| **数据库** | MySQL 8.0+ | 现有配置：`jdbc:mysql://127.0.0.1:3306/mes` |
| **对象映射** | MapStruct | 已集成 `macula-boot-starter-mapstruct` |
| **定时任务** | Spring @Scheduled | Spring原生支持，符合FR-009 |
| **HTTP客户端** | OkHttp (Feign) | 现有配置启用 `feign.okhttp.enabled=true` |
| **邮件服务** | Spring Mail | 需新增依赖 `spring-boot-starter-mail` |
| **重试机制** | Spring Retry | 需新增依赖 `spring-retry` |
| **API文档** | SpringDoc (OpenAPI 3) | 已集成 `macula-boot-starter-springdoc` |

### 1.2 模块划分

```
mes-service1/
├── controller/          # 接口控制层
│   ├── BatchController.java          # 批次推送接口 (FR-001)
│   └── PartQueryController.java      # 产线查询接口 (FR-019/FR-020/FR-021)
│   └── WorkReportController.java     # 报工接口 (FR-026)
├── service/             # 业务逻辑层
│   ├── BatchService.java             # 批次数据处理
│   ├── PrePackageService.java        # 预包装数据拉取
│   ├── PartQueryService.java         # 板件查询服务
│   ├── WorkReportService.java        # 报工服务
│   └── EmailNotificationService.java # 邮件通知服务
├── scheduled/           # 定时任务
│   └── PrePackagePullTask.java       # 预包装数据拉取定时任务 (FR-008)
├── integration/         # 第三方集成
│   └── MesApiClient.java             # 第三方MES接口客户端
├── mapper/              # 数据访问层
│   ├── BatchMapper.java
│   ├── WorkOrderMapper.java
│   ├── PrePackageInfoMapper.java
│   ├── PartMapper.java
│   └── WorkReportMapper.java
├── pojo/
│   ├── entity/          # 数据库实体
│   ├── dto/             # 数据传输对象 (Request/Response)
│   └── bo/              # 业务对象
└── converter/           # 对象转换器 (MapStruct)
```

### 1.3 分层架构

```
┌─────────────────────────────────────────────────┐
│  Controller Layer (REST API)                    │
│  - BatchController                              │
│  - PartQueryController                          │
│  - WorkReportController                         │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Service Layer (Business Logic)                 │
│  - BatchService                                 │
│  - PrePackageService (+ Retry Logic)            │
│  - PartQueryService                             │
│  - WorkReportService                            │
│  - EmailNotificationService                     │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Integration Layer (3rd Party API)              │
│  - MesApiClient (OkHttp/Feign)                  │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Data Access Layer (MyBatis-Plus)               │
│  - Mapper Interfaces                            │
└─────────────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────────────┐
│  Database (MySQL)                               │
│  - 8 Tables (Batch, WorkOrder, PrePackage...)   │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│  Scheduled Task (Async)                         │
│  - PrePackagePullTask (@Scheduled)              │
│    └─> PrePackageService                        │
└─────────────────────────────────────────────────┘
```

---

## 2. 数据库设计

### 2.1 表结构设计

#### 2.1.1 批次表 (mes_batch)

```sql
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
```

**说明**：
- `batch_type` 改为 `TINYINT` 类型，映射接口的整数值
- 移除批次级的 `urgency` 和 `optimizing_file_list`（这些属于优化文件层级）

#### 2.1.2 优化文件表 (mes_optimizing_file)

```sql
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_optimizing_file_batch` FOREIGN KEY (`batch_id`) REFERENCES `mes_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优化文件表';
```

**说明**：
- 新增表，用于存储批次推送接口中的 `optimizingFileList` 数据
- 一个批次包含多个优化文件
- `station_code` 和 `urgency` 属于优化文件层级（不是批次层级）

---

#### 2.1.3 工单表 (mes_work_order)

```sql
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_work_order_batch` FOREIGN KEY (`batch_id`) REFERENCES `mes_batch` (`id`),
  CONSTRAINT `fk_work_order_optimizing_file` FOREIGN KEY (`optimizing_file_id`) REFERENCES `mes_optimizing_file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';
```

**说明**：
- 移除 `optimizing_file_name`、`station_code`、`urgency` 字段（这些属于优化文件表）
- 添加 `optimizing_file_id` 外键，关联到优化文件表
- 一个工单属于一个优化文件

#### 2.1.4 预包装订单表 (mes_prepackage_order)

```sql
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_prepackage_work_order` FOREIGN KEY (`work_order_id`) REFERENCES `mes_work_order` (`id`),
  CONSTRAINT `fk_prepackage_batch` FOREIGN KEY (`batch_id`) REFERENCES `mes_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预包装订单表';
```

**说明**：
- 新增 `batch_id` 和 `batch_num` 冗余字段，方便按批次统计和查询（如：查询某批次的所有预包装订单）
- 添加 `idx_batch_id` 和 `idx_batch_num` 索引，优化批次维度查询性能
- 添加外键约束 `fk_prepackage_batch`，保证数据一致性
- 补充所有接口返回的字段（phone、ship_batch、customer、receive_region、space、pack_type、production_num等）
- `order_num` 对应接口的 `orderNum`
- `consignor` 对应接口的 `consignor`（客户名称/WMS货主）

#### 2.1.5 箱码表 (mes_box)

```sql
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_box_prepackage_order` FOREIGN KEY (`prepackage_order_id`) REFERENCES `mes_prepackage_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='箱码表';
```

**说明**：
- `house` 对应接口的 `house`（不是 `house_type`）
- `setno` 对应接口的 `setno`（第几套，不是 `sets`）
- 新增 `batch_num` 冗余字段，方便按批次查询箱码

#### 2.1.6 包件表 (mes_package)

```sql
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_package_box` FOREIGN KEY (`box_id`) REFERENCES `mes_box` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='包件表';
```

**说明**：
- **新增表**，对应接口响应的 `boxInfoList[]` 数据
- 三层结构：订单 → 箱码 → **包件** → 板件
- 一个箱码包含多个包件（`packageNo`）
- 板件属于包件（不是直接属于箱码）
- 新增 `batch_num` 冗余字段，方便按批次统计包件数量

#### 2.1.7 板件表 (mes_part)

```sql
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
  KEY `idx_is_deleted` (`is_deleted`),
  CONSTRAINT `fk_part_package` FOREIGN KEY (`package_id`) REFERENCES `mes_package` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='板件表';
```

**说明**：
- 字段名全部调整为与接口一致（`partCode` → `part_code`，`itemCode` → `item_code`，`matName` → `mat_name` 等）
- 外键改为 `package_id`（板件属于包件，不是直接属于箱码）
- 保留 `box_id` 作为冗余字段，方便按箱码查询
- 新增 `batch_num` 冗余字段，方便按批次统计板件数量、查询生产进度
- `standard_list` 存储对象数组（如 `[{"00041":1,"00311":1}]`）

#### 2.1.8 报工记录表 (mes_work_report)

```sql
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
```

**说明**：
- `part_barcode` 改名为 `part_code`（与板件表的 `part_code` 保持一致）
- 不设置物理外键约束，保证报工记录独立性

#### 2.1.9 工单数据修正日志表 (mes_work_order_correction_log)

```sql
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
```

#### 2.1.10 邮件通知配置表 (mes_email_notification_config)

```sql
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
```

**初始化数据**：

```sql
INSERT INTO `mes_email_notification_config` 
  (`smtp_host`, `smtp_port`, `username`, `password`, `from_address`, `to_addresses`) 
VALUES 
  ('smtp.qq.com', 587, '243219169@qq.com', 'YOUR_AUTH_CODE', '243219169@qq.com', '243219169@qq.com');
```

### 2.2 索引设计说明

| 表名 | 索引类型 | 字段 | 用途 |
|------|---------|------|------|
| mes_batch | 唯一索引 | batch_num | 保证批次号唯一性，批次推送幂等性 |
| mes_batch | 普通索引 | batch_type | 按批次类型查询 |
| mes_batch | 普通索引 | is_deleted | 逻辑删除查询过滤 |
| mes_optimizing_file | 唯一索引 | (batch_num, optimizing_file_name) | 保证同一批次下优化文件名唯一 |
| mes_optimizing_file | 普通索引 | station_code | 按工位编码查询 |
| mes_work_order | 唯一索引 | work_id | 保证工单号唯一性 |
| mes_work_order | 普通索引 | prepackage_status | 定时任务查询"未拉取"工单 |
| mes_work_order | 普通索引 | is_deleted | 逻辑删除查询过滤 |
| mes_prepackage_order | 唯一索引 | work_order_id | 一个工单只有一条预包装订单记录 |
| mes_prepackage_order | 普通索引 | batch_id | 按批次ID统计预包装订单 |
| mes_prepackage_order | 普通索引 | batch_num | 按批次号查询预包装订单 |
| mes_prepackage_order | 普通索引 | order_num | 按订单号查询 |
| mes_prepackage_order | 普通索引 | production_num | 按生产编号查询 |
| mes_box | 唯一索引 | box_code | 保证箱码唯一性 |
| mes_box | 普通索引 | batch_num | 按批次号查询箱码 |
| mes_package | 唯一索引 | (box_code, package_no) | 保证同一箱码下包件编号唯一 |
| mes_package | 普通索引 | batch_num | 按批次号统计包件数量 |
| mes_part | 唯一索引 | part_code | 保证部件条码唯一性，产线查询主键 |
| mes_part | 普通索引 | batch_num | 按批次号统计板件数量、查询生产进度 |
| mes_part | 普通索引 | is_deleted | 逻辑删除查询过滤（含已失效板件） |
| mes_work_report | 普通索引 | part_code | 报工记录查询 |
| mes_work_report | 普通索引 | report_time | 生产轨迹时间顺序查询 |
| mes_work_report | 普通索引 | is_deleted | 逻辑删除查询过滤 |
| 所有表 | - | created_by, updated_by | 审计字段，记录创建人和更新人 |

### 2.3 数据库 ER 图

```
mes_batch (批次表)
  ├── id (PK)
  ├── batch_num (UK) - 批次号
  ├── batch_type - 批次类型（1~6）
  └── product_time - 生产日期
       │
       ├──> mes_optimizing_file (优化文件表)【1:N】
       │      ├── id (PK)
       │      ├── batch_id (FK) → mes_batch.id
       │      ├── optimizing_file_name - 优化文件名
       │      ├── station_code - 工位编码
       │      └── urgency - 是否加急
       │           │
       │           └──> mes_work_order (工单表)【1:N】
       │                  ├── id (PK)
       │                  ├── batch_id (FK) → mes_batch.id
       │                  ├── optimizing_file_id (FK) → mes_optimizing_file.id
       │                  ├── work_id (UK) - 工单号
       │                  ├── route - 线路
       │                  ├── order_type - 订单类型
       │                  └── prepackage_status - 拉取状态
       │                       │
       │                       └──> mes_prepackage_order (预包装订单表)【1:1】
       │                              ├── id (PK)
       │                              ├── work_order_id (FK, UK) → mes_work_order.id
       │                              ├── order_num - 订单号
       │                              ├── consignor - 客户名称
       │                              ├── receiver - 收货人
       │                              └── install_address - 安装地址
       │                                   │
       │                                   └──> mes_box (箱码表)【1:N】
       │                                          ├── id (PK)
       │                                          ├── prepackage_order_id (FK)
       │                                          ├── box_code (UK) - 箱码
       │                                          ├── building - 楼栋
       │                                          ├── house - 户型
       │                                          └── room - 房间号
       │                                               │
       │                                               └──> mes_package (包件表)【1:N】
       │                                                      ├── id (PK)
       │                                                      ├── box_id (FK)
       │                                                      ├── package_no - 第几包
       │                                                      ├── length/width/depth - 尺寸
       │                                                      ├── weight - 重量
       │                                                      └── box_type - 纸箱类型
       │                                                           │
       │                                                           └──> mes_part (板件表)【1:N】
       │                                                                  ├── id (PK)
       │                                                                  ├── package_id (FK)
       │                                                                  ├── part_code (UK) - 部件条码
       │                                                                  ├── layer/piece - 层/片
       │                                                                  ├── item_code - 板件ID
       │                                                                  ├── mat_name - 花色
       │                                                                  ├── item_length/width/depth - 尺寸
       │                                                                  ├── x_axis/y_axis/z_axis - 坐标
       │                                                                  ├── sort_order - 分拣顺序
       │                                                                  └── standard_list - 标准码集合
       │
       └──> mes_work_report (报工记录表)【独立表，无物理外键】
              ├── id (PK)
              ├── part_code - 部件条码（逻辑关联 mes_part.part_code）
              ├── part_status - 板件状态
              ├── station_code - 工位编码
              └── report_time - 报工时间

mes_work_order_correction_log (修正日志表)
  ├── id (PK)
  ├── work_order_id → mes_work_order.id
  ├── operator - 操作人
  ├── correction_reason - 修正原因
  └── result - 修正结果

mes_email_notification_config (邮件配置表)
  ├── id (PK)
  ├── smtp_host/smtp_port
  ├── from_address/to_addresses
  └── enabled - 是否启用
```

**关键关系总结**：
1. **批次 → 优化文件 → 工单**：三层级联关系（1:N:N）
2. **工单 → 订单 → 箱码 → 包件 → 板件**：五层级联关系（1:1:N:N:N）
3. **报工记录独立**：不设物理外键，仅通过 `part_code` 逻辑关联

所有表统一包含以下审计字段，用于数据追溯和问题排查：

| 字段名 | 类型 | 说明 | 填充规则 |
|-------|------|------|---------|
| `is_deleted` | TINYINT | 逻辑删除标识（0=正常，1=已删除） | 默认0，删除时更新为1 |
| `created_by` | VARCHAR(100) | 创建人 | 插入时填充，来源如下 |
| `created_time` | DATETIME | 创建时间 | 数据库自动填充（CURRENT_TIMESTAMP） |
| `updated_by` | VARCHAR(100) | 更新人 | 更新时填充 |
| `updated_time` | DATETIME | 更新时间 | 数据库自动填充（ON UPDATE CURRENT_TIMESTAMP） |

**created_by/updated_by 填充规则**：

| 操作场景 | created_by/updated_by 值 | 说明 |
|---------|--------------------------|------|
| **第三方MES推送批次** | `THIRD_PARTY_MES` | 批次和工单数据由第三方系统推送 |
| **系统定时任务拉取** | `SYSTEM_PULL_TASK` | 预包装订单、箱码、板件由定时任务拉取 |
| **产线客户端报工** | `PRODUCTION_CLIENT` 或操作员工号 | 报工记录由产线系统提交 |
| **管理员数据修正** | 管理员账号（如 `admin`） | 手动重置工单状态时记录 |
| **邮件通知发送** | `SYSTEM_EMAIL_SERVICE` | 邮件发送日志（如有） |

**逻辑删除使用场景**：

1. **板件软删除**（`mes_part.is_deleted=1`）：
   - 工单数据重新拉取时，上游删除的板件标记为已删除
   - 保留板件记录供报工关联，但查询时过滤
   - 报工记录仍然有效，`part_barcode` 可追溯

2. **报工记录不删除**（`mes_work_report.is_deleted` 保持0）：
   - 报工记录永久保留，不做逻辑删除
   - 即使关联板件已失效，报工记录仍可查询

3. **其他表逻辑删除**：
   - 批次、工单、预包装订单等表支持逻辑删除
   - 查询时添加条件 `WHERE is_deleted = 0`
   - MyBatis-Plus 可配置全局逻辑删除插件

**MyBatis-Plus 配置**（`application.yml`）：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted  # 逻辑删除字段名
      logic-delete-value: 1          # 删除后的值
      logic-not-delete-value: 0      # 未删除的值
```

### 2.4 审计字段使用规范

所有表统一包含以下审计字段，用于数据追溯和问题排查：

| 字段名 | 类型 | 说明 | 填充规则 |
|-------|------|------|---------|\n| `is_deleted` | TINYINT | 逻辑删除标识（0=正常，1=已删除） | 默认0，删除时更新为1 |
| `created_by` | VARCHAR(100) | 创建人 | 插入时填充，来源如下 |
| `created_time` | DATETIME | 创建时间 | 数据库自动填充（CURRENT_TIMESTAMP） |
| `updated_by` | VARCHAR(100) | 更新人 | 更新时填充 |
| `updated_time` | DATETIME | 更新时间 | 数据库自动填充（ON UPDATE CURRENT_TIMESTAMP） |

**created_by/updated_by 填充规则**：

| 操作场景 | created_by/updated_by 值 | 说明 |
|---------|--------------------------|------|
| **第三方MES推送批次** | `THIRD_PARTY_MES` | 批次、优化文件、工单数据由第三方系统推送 |
| **系统定时任务拉取** | `SYSTEM_PULL_TASK` | 预包装订单、箱码、包件、板件由定时任务拉取 |
| **产线客户端报工** | `PRODUCTION_CLIENT` 或操作员工号 | 报工记录由产线系统提交 |
| **管理员数据修正** | 管理员账号（如 `admin`） | 手动重置工单状态时记录 |
| **邮件通知发送** | `SYSTEM_EMAIL_SERVICE` | 邮件发送日志（如有） |

**逻辑删除使用场景**：

1. **板件软删除**（`mes_part.is_deleted=1`）：
   - 工单数据重新拉取时，上游删除的板件标记为已删除
   - 保留板件记录供报工关联，但查询时过滤
   - 报工记录仍然有效，`part_code` 可追溯

2. **报工记录不删除**（`mes_work_report.is_deleted` 保持0）：
   - 报工记录永久保留，不做逻辑删除
   - 即使关联板件已失效，报工记录仍可查询

3. **其他表逻辑删除**：
   - 批次、工单、预包装订单等表支持逻辑删除
   - 查询时添加条件 `WHERE is_deleted = 0`
   - MyBatis-Plus 可配置全局逻辑删除插件

**MyBatis-Plus 配置**（`application.yml`）：

```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: isDeleted  # 逻辑删除字段名
      logic-delete-value: 1          # 删除后的值
      logic-not-delete-value: 0      # 未删除的值
```

### 2.5 事务边界设计

| 操作场景 | 事务范围 | 隔离级别 | 说明 |
|---------|---------|---------|------|
| **批次推送** | BatchService.saveBatch() | READ_COMMITTED | 批次+工单数据原子性入库 |
| **预包装拉取** | PrePackageService.pullAndSave() | READ_COMMITTED | 预包装订单+箱码+板件数据原子性入库 |
| **数据修正** | PrePackageService.repullAndUpdate() | READ_COMMITTED | 删除旧数据+插入新数据+更新状态，事务保证原子性（FR-037） |
| **报工记录** | WorkReportService.saveReport() | 无事务 | 单表插入，不依赖其他表 |

---

## 3. 接口设计

### 3.1 批次推送接口 (FR-001)

**端点**: `POST /api/v1/third-party/batch/push`

**Request DTO**:

```java
@Data
@ApiModel("批次推送请求")
public class BatchPushRequest {
    @NotBlank(message = "批次号不能为空")
    @ApiModelProperty("批次号")
    private String batchNum;
    
    @ApiModelProperty("简易批次号")
    private String simpleBatchNum;
    
    @NotNull(message = "批次类型不能为空")
    @ApiModelProperty("批次类型：1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板")
    private Integer batchType;
    
    @NotNull(message = "生产日期不能为空")
    @ApiModelProperty("生产日期（格式：yyyy-MM-dd）")
    private LocalDate productTime;
    
    @NotNull(message = "优化文件列表不能为空")
    @Size(min = 1, message = "至少包含1个优化文件")
    @ApiModelProperty("优化文件列表")
    private List<OptimizingFileInfo> optimizingFileList;
    
    @Data
    @ApiModel("优化文件信息")
    public static class OptimizingFileInfo {
        @NotBlank(message = "优化文件名不能为空")
        @ApiModelProperty("优化文件名称")
        private String optimizingFileName;
        
        @NotBlank(message = "工位编码不能为空")
        @ApiModelProperty("工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002）")
        private String station;
        
        @ApiModelProperty("是否加急（0=不加急，1=加急）")
        private Integer urgency;
        
        @NotNull(message = "工单列表不能为空")
        @Size(min = 1, message = "每个优化文件至少包含1个工单")
        @ApiModelProperty("该优化文件所属的全部工单列表")
        private List<WorkOrderInfo> workOrderList;
    }
    
    @Data
    @ApiModel("工单信息")
    public static class WorkOrderInfo {
        @NotBlank(message = "工单号不能为空")
        @ApiModelProperty("工单号")
        private String workId;
        
        @NotBlank(message = "线路不能为空")
        @ApiModelProperty("线路（如"线路1"）")
        private String route;
        
        @NotBlank(message = "订单类型不能为空")
        @ApiModelProperty("订单类型")
        private String orderType;
    }
}
```

**Response DTO**:

```java
@Data
@ApiModel("标准响应")
public class StandardResponse<T> {
    @ApiModelProperty("状态码（0=成功，1=失败）")
    private Integer status;
    
    @ApiModelProperty("响应消息")
    private String msg;
    
    @ApiModelProperty("响应时间戳")
    private Long time;
    
    @ApiModelProperty("响应数据")
    private T data;
    
    public static <T> StandardResponse<T> success() {
        StandardResponse<T> response = new StandardResponse<>();
        response.setStatus(0);
        response.setMsg("成功");
        response.setTime(System.currentTimeMillis());
        return response;
    }
    
    public static <T> StandardResponse<T> fail(String message) {
        StandardResponse<T> response = new StandardResponse<>();
        response.setStatus(1);
        response.setMsg(message);
        response.setTime(System.currentTimeMillis());
        return response;
    }
}
```

**Controller 实现**:

```java
@RestController
@RequestMapping("/api/v1/third-party/batch")
@Tag(name = "批次管理", description = "第三方MES系统批次推送接口")
public class BatchController {
    
    @Autowired
    private BatchService batchService;
    
    @PostMapping("/push")
    @Operation(summary = "批次推送", description = "接收第三方MES系统推送的批次及工单数据")
    public StandardResponse<Void> pushBatch(@Valid @RequestBody BatchPushRequest request) {
        try {
            batchService.saveBatch(request);
            return StandardResponse.success();
        } catch (IllegalArgumentException e) {
            // 参数校验失败
            return StandardResponse.fail(e.getMessage());
        } catch (Exception e) {
            log.error("批次推送失败: batchNum={}", request.getBatchNum(), e);
            return StandardResponse.fail("批次推送失败: " + e.getMessage());
        }
    }
}
```

### 3.2 产线查询接口 (FR-019/FR-020/FR-021)

#### 3.2.1 板件码查询工单与批次信息

**端点**: `GET /api/v1/production/part/{partCode}/work-order-and-batch`

**Response DTO**:

```java
@Data
@ApiModel("板件工单批次信息")
public class PartWorkOrderBatchResponse {
    @ApiModelProperty("工单信息")
    private WorkOrderInfo workOrder;
    
    @ApiModelProperty("优化文件信息")
    private OptimizingFileInfo optimizingFile;
    
    @ApiModelProperty("批次信息")
    private BatchInfo batch;
    
    @Data
    @ApiModel("工单信息")
    public static class WorkOrderInfo {
        private String workId;
        private String route;
        private String orderType;
        private String prepackageStatus;
        // 工单表的全部字段
    }
    
    @Data
    @ApiModel("优化文件信息")
    public static class OptimizingFileInfo {
        private String optimizingFileName;
        private String stationCode;
        private Integer urgency;
    }
    
    @Data
    @ApiModel("批次信息")
    public static class BatchInfo {
        private String batchNum;
        private Integer batchType; // 1~6
        private LocalDate productTime;
        private String simpleBatchNum;
    }
}
```

#### 3.2.2 板件码查询包装数据

**端点**: `GET /api/v1/production/part/{partBarcode}/package`

**Response DTO**:

```java
@Data
@ApiModel("板件包装信息")
public class PartPackageResponse {
    @ApiModelProperty("箱码信息")
    private BoxInfo box;
    
    @ApiModelProperty("订单信息")
    private OrderInfo order;
    
    @ApiModelProperty("板件在箱内的位置信息")
    private PositionInfo position;
    
    @Data
    @ApiModel("箱码信息")
    public static class BoxInfo {
        private String boxCode;
        private String building;
        private String houseType;
        private String room;
        private Integer sets;
        private String color;
        // 箱内所有板件列表
        private List<PartInfo> partList;
    }
    
    @Data
    @ApiModel("订单信息")
    public static class OrderInfo {
        private String orderNo;
        private String customerName;
        private String contractNo;
        private String receiver;
        private String installAddress;
        private String productType;
    }
    
    @Data
    @ApiModel("板件位置信息")
    public static class PositionInfo {
        private Integer layer;    // 层
        private Integer piece;    // 片
        private Integer sortOrder; // 分拣顺序
    }
}
```

#### 3.2.3 板件码查询板件详细信息

**端点**: `GET /api/v1/production/part/{partBarcode}/detail`

**Response DTO**:

```java
@Data
@ApiModel("板件详细信息")
public class PartDetailResponse {
    private String partBarcode;
    private String partId;
    private String partDesc;
    private String color;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private BigDecimal coordX;
    private BigDecimal coordY;
    private BigDecimal coordZ;
    private Integer sortOrder;
    private List<Map<String, Integer>> standardList; // JSON解析后的标准码集合
    // ... 全部字段
}
```

#### 3.2.4 查询接口通用错误处理

```java
@RestController
@RequestMapping("/api/v1/production/part")
@Tag(name = "产线查询", description = "产线客户端板件查询接口")
public class PartQueryController {
    
    @Autowired
    private PartQueryService partQueryService;
    
    @GetMapping("/{partBarcode}/work-order-and-batch")
    public StandardResponse<PartWorkOrderBatchResponse> queryWorkOrderAndBatch(
            @PathVariable String partBarcode) {
        try {
            PartWorkOrderBatchResponse response = partQueryService.queryWorkOrderAndBatch(partBarcode);
            return StandardResponse.success(response);
        } catch (PartNotFoundException e) {
            return StandardResponse.fail("板件码不存在");
        } catch (WorkOrderUpdatingException e) {
            // FR-040a: 返回HTTP 409
            return StandardResponse.fail("工单数据更新中，请稍后重试");
        } catch (Exception e) {
            log.error("查询失败: partBarcode={}", partBarcode, e);
            return StandardResponse.fail("查询失败");
        }
    }
    
    // ... 其他查询方法类似
}
```

**自定义异常**:

```java
@ResponseStatus(HttpStatus.CONFLICT) // HTTP 409
public class WorkOrderUpdatingException extends RuntimeException {
    public WorkOrderUpdatingException(String workId) {
        super("工单数据更新中，请稍后重试。工单号: " + workId);
    }
}
```

### 3.3 报工接口 (FR-026)

**端点**: `POST /api/v1/production/work-report`

**Request DTO**:

```java
@Data
@ApiModel("报工请求")
public class WorkReportRequest {
    @NotBlank(message = "部件条码不能为空")
    @ApiModelProperty("部件条码")
    private String partCode;
    
    @NotBlank(message = "板件状态不能为空")
    @ApiModelProperty("板件状态值")
    private String partStatus;
    
    @NotBlank(message = "工位编码不能为空")
    @ApiModelProperty("工位编码")
    private String stationCode;
    
    @ApiModelProperty("工位名称")
    private String stationName;
}
```

**Controller 实现**:

```java
@RestController
@RequestMapping("/api/v1/production/work-report")
@Tag(name = "报工管理", description = "产线客户端报工接口")
public class WorkReportController {
    
    @Autowired
    private WorkReportService workReportService;
    
    @PostMapping
    @Operation(summary = "板件报工", description = "记录板件在各工位的生产状态")
    public StandardResponse<Void> submitWorkReport(@Valid @RequestBody WorkReportRequest request) {
        try {
            workReportService.saveWorkReport(request);
            return StandardResponse.success();
        } catch (DuplicateWorkReportException e) {
            // FR-030: 幂等性处理
            return StandardResponse.fail("重复报工：相同板件在该工位的上一次报工状态相同");
        } catch (Exception e) {
            log.error("报工失败: partCode={}, stationCode={}", 
                request.getPartCode(), request.getStationCode(), e);
            return StandardResponse.fail("报工失败");
        }
    }
}
```

---

## 4. 核心功能实现

### 4.1 批次数据接收 (Story 1)

**BatchService.saveBatch()**:

```java
@Service
@Transactional
public class BatchServiceImpl implements BatchService {
    
    @Autowired
    private BatchMapper batchMapper;
    
    @Autowired
    private OptimizingFileMapper optimizingFileMapper;
    
    @Autowired
    private WorkOrderMapper workOrderMapper;
    
    @Autowired
    private BatchConverter batchConverter;
    
    @Override
    public void saveBatch(BatchPushRequest request) {
        // 1. 参数校验（已通过@Valid完成）
        
        // 2. 检查批次是否已存在（幂等性处理 FR-007）
        Batch existingBatch = batchMapper.selectOne(
            new LambdaQueryWrapper<Batch>()
                .eq(Batch::getBatchNum, request.getBatchNum())
        );
        
        Long batchId;
        if (existingBatch != null) {
            // 更新策略：删除旧数据，插入新数据
            log.info("批次已存在，执行更新策略: batchNum={}", request.getBatchNum());
            
            // 删除旧工单
            workOrderMapper.delete(
                new LambdaQueryWrapper<WorkOrder>()
                    .eq(WorkOrder::getBatchId, existingBatch.getId())
            );
            
            // 删除旧优化文件
            optimizingFileMapper.delete(
                new LambdaQueryWrapper<OptimizingFile>()
                    .eq(OptimizingFile::getBatchId, existingBatch.getId())
            );
            
            // 更新批次基础信息
            Batch batch = new Batch();
            batch.setId(existingBatch.getId());
            batch.setBatchType(request.getBatchType());
            batch.setProductTime(request.getProductTime());
            batch.setSimpleBatchNum(request.getSimpleBatchNum());
            batch.setUpdatedBy("THIRD_PARTY_MES");
            batchMapper.updateById(batch);
            
            batchId = existingBatch.getId();
        } else {
            // 新增批次
            Batch batch = new Batch();
            batch.setBatchNum(request.getBatchNum());
            batch.setBatchType(request.getBatchType());
            batch.setProductTime(request.getProductTime());
            batch.setSimpleBatchNum(request.getSimpleBatchNum());
            batch.setIsDeleted(0);
            batch.setCreatedBy("THIRD_PARTY_MES");
            batchMapper.insert(batch);
            
            batchId = batch.getId();
        }
        
        // 3. 保存优化文件和工单
        for (BatchPushRequest.OptimizingFileInfo fileInfo : request.getOptimizingFileList()) {
            // 保存优化文件
            OptimizingFile optimizingFile = new OptimizingFile();
            optimizingFile.setBatchId(batchId);
            optimizingFile.setBatchNum(request.getBatchNum());
            optimizingFile.setOptimizingFileName(fileInfo.getOptimizingFileName());
            optimizingFile.setStationCode(fileInfo.getStation());
            optimizingFile.setUrgency(fileInfo.getUrgency() != null ? fileInfo.getUrgency() : 0);
            optimizingFile.setIsDeleted(0);
            optimizingFile.setCreatedBy("THIRD_PARTY_MES");
            optimizingFileMapper.insert(optimizingFile);
            
            // 保存该优化文件下的所有工单
            for (BatchPushRequest.WorkOrderInfo workOrderInfo : fileInfo.getWorkOrderList()) {
                WorkOrder workOrder = new WorkOrder();
                workOrder.setBatchId(batchId);
                workOrder.setOptimizingFileId(optimizingFile.getId());
                workOrder.setBatchNum(request.getBatchNum());
                workOrder.setWorkId(workOrderInfo.getWorkId());
                workOrder.setRoute(workOrderInfo.getRoute());
                workOrder.setOrderType(workOrderInfo.getOrderType());
                workOrder.setPrepackageStatus("NOT_PULLED"); // FR-005
                workOrder.setRetryCount(0);
                workOrder.setIsDeleted(0);
                workOrder.setCreatedBy("THIRD_PARTY_MES");
                workOrderMapper.insert(workOrder);
            }
        }
        
        int totalWorkOrders = request.getOptimizingFileList().stream()
            .mapToInt(file -> file.getWorkOrderList().size())
            .sum();
        
        log.info("批次数据保存成功: batchNum={}, fileCount={}, workOrderCount={}", 
            request.getBatchNum(), request.getOptimizingFileList().size(), totalWorkOrders);
    }
}
            request.getBatchNum(), request.getWorkOrderList().size());
    }
    
}
```

### 4.2 预包装数据自动拉取 (Story 2)

#### 4.2.1 定时任务实现 (FR-008)

```java
@Component
@Slf4j
public class PrePackagePullTask {
    
    @Autowired
    private PrePackageService prePackageService;
    
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /**
     * 定时任务：每1秒执行一次
     * FR-016: 执行互斥机制，同一时间只有一个任务实例
     */
    @Scheduled(fixedDelay = 1000) // 上次执行完成后1秒执行下次
    public void pullPrePackageData() {
        // 检查是否已有任务在运行
        if (!isRunning.compareAndSet(false, true)) {
            log.info("上一次定时任务尚未完成，跳过本次调度");
            return;
        }
        
        try {
            log.info("开始执行预包装数据拉取任务");
            prePackageService.pullPendingWorkOrders();
        } catch (Exception e) {
            log.error("预包装数据拉取任务执行失败", e);
        } finally {
            isRunning.set(false);
        }
    }
}
```

#### 4.2.2 预包装数据拉取服务 (FR-010)

```java
@Service
@Slf4j
public class PrePackageServiceImpl implements PrePackageService {
    
    @Autowired
    private WorkOrderMapper workOrderMapper;
    
    @Autowired
    private PrePackageOrderMapper prePackageOrderMapper;
    
    @Autowired
    private BoxMapper boxMapper;
    
    @Autowired
    private PartMapper partMapper;
    
    @Autowired
    private MesApiClient mesApiClient;
    
    @Autowired
    private EmailNotificationService emailNotificationService;
    
    @Autowired
    private WorkOrderCorrectionLogMapper correctionLogMapper;
    
    @Override
    public void pullPendingWorkOrders() {
        // 1. 查询待拉取工单（FR-008）
        List<WorkOrder> pendingWorkOrders = workOrderMapper.selectList(
            new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getPrepackageStatus, "NOT_PULLED")
                .orderByAsc(WorkOrder::getCreatedTime)
                .last("LIMIT 50") // 每次处理50个，避免单次任务过长
        );
        
        if (pendingWorkOrders.isEmpty()) {
            log.info("无待拉取预包装数据的工单");
            return;
        }
        
        log.info("发现 {} 个待拉取预包装数据的工单", pendingWorkOrders.size());
        
        // 2. 逐个拉取
        for (WorkOrder workOrder : pendingWorkOrders) {
            pullSingleWorkOrder(workOrder);
        }
    }
    
    @Transactional
    public void pullSingleWorkOrder(WorkOrder workOrder) {
        try {
            // 更新状态为"拉取中"（FR-032）
            workOrder.setPrepackageStatus("PULLING");
            workOrderMapper.updateById(workOrder);
            
            // 调用第三方接口（FR-010，含重试逻辑）
            PrePackageDataResponse response = pullWithRetry(
                workOrder.getBatchNum(), 
                workOrder.getWorkId()
            );
            
            if (response == null || response.isEmpty()) {
                // 无预包装数据（FR-018）
                workOrder.setPrepackageStatus("NO_DATA");
                workOrder.setLastPullTime(LocalDateTime.now());
                workOrderMapper.updateById(workOrder);
                log.info("工单无预包装数据: workId={}", workOrder.getWorkId());
                return;
            }
            
            // 保存预包装数据（FR-012/FR-013）
            savePrePackageData(workOrder, response);
            
            // 更新状态为"已拉取"（FR-015）
            workOrder.setPrepackageStatus("PULLED");
            workOrder.setLastPullTime(LocalDateTime.now());
            workOrder.setRetryCount(0);
            workOrder.setErrorMessage(null);
            workOrder.setUpdatedBy("SYSTEM_PULL_TASK"); // 系统定时任务
            workOrderMapper.updateById(workOrder);
            
            log.info("预包装数据拉取成功: workId={}, partCount={}", 
                workOrder.getWorkId(), countParts(response));
            
        } catch (Exception e) {
            handlePullFailure(workOrder, e);
        }
    }
    
    /**
     * 指数退避重试（FR-017）
     */
    private PrePackageDataResponse pullWithRetry(String batchNum, String workId) {
        int maxRetries = 3;
        int[] retryDelays = {1000, 2000, 4000}; // 1s, 2s, 4s
        
        for (int i = 0; i <= maxRetries; i++) {
            try {
                return mesApiClient.getPrePackageInfo(batchNum, workId);
            } catch (Exception e) {
                if (i < maxRetries) {
                    log.warn("预包装数据拉取失败，将在 {} ms 后重试 (第{}次重试): workId={}", 
                        retryDelays[i], i + 1, workId, e);
                    try {
                        Thread.sleep(retryDelays[i]);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试等待被中断", ie);
                    }
                } else {
                    log.error("预包装数据拉取失败，已达最大重试次数: workId={}", workId, e);
                    throw e;
                }
            }
        }
        throw new RuntimeException("重试逻辑异常");
    }
    
    /**
     * 保存预包装数据（FR-013）
     */
    @Transactional
    private void savePrePackageData(WorkOrder workOrder, PrePackageDataResponse response) {
        // 1. 保存预包装订单（包含批次号冗余）
        PrePackageOrder order = new PrePackageOrder();
        order.setWorkOrderId(workOrder.getId());
        order.setBatchId(workOrder.getBatchId());  // 冗余批次ID
        order.setBatchNum(workOrder.getBatchNum()); // 冗余批次号
        order.setWorkId(workOrder.getWorkId());
        
        // 接口返回的字段
        order.setOrderNum(response.getOrderNum());
        order.setConsignor(response.getConsignor());
        order.setContractNo(response.getContractNo());
        order.setWorkNum(response.getWorkNum());
        order.setReceiver(response.getReceiver());
        order.setPhone(response.getPhone());
        order.setShipBatch(response.getShipBatch());
        order.setInstallAddress(response.getInstallAddress());
        order.setCustomer(response.getCustomer());
        order.setReceiveRegion(response.getReceiveRegion());
        order.setSpace(response.getSpace());
        order.setPackType(response.getPackType());
        order.setProductType(response.getProductType());
        order.setPrepackageInfoSize(response.getPrepackageInfoSize());
        order.setTotalSet(response.getTotalSet());
        order.setMaxPackageNo(response.getMaxPackageNo());
        order.setProductionNum(response.getProductionNum());
        
        order.setIsDeleted(0);
        order.setCreatedBy("SYSTEM_PULL_TASK");
        prePackageOrderMapper.insert(order);
        
        // 2. 遍历预包装信息列表（prePackageInfo）
        for (PrePackageDataResponse.PrePackageInfo pkgInfo : response.getPrePackageInfo()) {
            // 保存箱码
            Box box = new Box();
            box.setPrepackageOrderId(order.getId());
            box.setBatchNum(workOrder.getBatchNum()); // 冗余批次号
            box.setWorkId(workOrder.getWorkId());
            box.setBoxCode(pkgInfo.getBoxCode());
            box.setBuilding(pkgInfo.getBuilding());
            box.setHouse(pkgInfo.getHouse());  // 注意：接口字段是house，不是houseType
            box.setRoom(pkgInfo.getRoom());
            box.setSetno(pkgInfo.getSetno()); // 注意：接口字段是setno，不是sets
            box.setColor(pkgInfo.getColor());
            box.setIsDeleted(0);
            box.setCreatedBy("SYSTEM_PULL_TASK");
            boxMapper.insert(box);
            
            // 3. 遍历箱码内的包件列表（boxInfoList）
            for (PrePackageDataResponse.BoxInfoDetail boxDetail : pkgInfo.getBoxInfoList()) {
                // 保存包件
                Package pkg = new Package();
                pkg.setBoxId(box.getId());
                pkg.setBatchNum(workOrder.getBatchNum()); // 冗余批次号
                pkg.setWorkId(workOrder.getWorkId());
                pkg.setBoxCode(box.getBoxCode());
                pkg.setPackageNo(boxDetail.getPackageNo());
                pkg.setLength(boxDetail.getLength());
                pkg.setWidth(boxDetail.getWidth());
                pkg.setDepth(boxDetail.getDepth());
                pkg.setWeight(boxDetail.getWeight());
                pkg.setPartCount(boxDetail.getPartCount());
                pkg.setBoxType(boxDetail.getBoxType());
                pkg.setIsDeleted(0);
                pkg.setCreatedBy("SYSTEM_PULL_TASK");
                packageMapper.insert(pkg);
                
                // 4. 遍历包件内的板件列表（partInfoList）
                for (PrePackageDataResponse.PartInfo partInfo : boxDetail.getPartInfoList()) {
                    Part part = new Part();
                    part.setPackageId(pkg.getId());     // 板件属于包件
                    part.setBoxId(box.getId());         // 冗余箱码ID
                    part.setBatchNum(workOrder.getBatchNum()); // 冗余批次号
                    part.setWorkId(workOrder.getWorkId());
                    
                    // 接口字段映射（注意字段名）
                    part.setPartCode(partInfo.getPartCode());    // 不是partBarcode
                    part.setLayer(partInfo.getLayer());
                    part.setPiece(partInfo.getPiece());
                    part.setItemCode(partInfo.getItemCode());    // 不是partId
                    part.setItemName(partInfo.getItemName());    // 不是partDesc
                    part.setMatName(partInfo.getMatName());      // 不是color
                    part.setItemLength(partInfo.getItemLength()); // 不是length
                    part.setItemWidth(partInfo.getItemWidth());
                    part.setItemDepth(partInfo.getItemDepth());
                    part.setXAxis(partInfo.getXAxis());          // 不是coordX
                    part.setYAxis(partInfo.getYAxis());
                    part.setZAxis(partInfo.getZAxis());
                    part.setSortOrder(partInfo.getSortOrder());
                    
                    // FR-014: 处理 standardList（JSON类型，存储对象数组）
                    if (partInfo.getStandardList() != null) {
                        part.setStandardList(JsonUtils.toJson(partInfo.getStandardList()));
                    }
                    
                    part.setIsDeleted(0);
                    part.setCreatedBy("SYSTEM_PULL_TASK");
                    partMapper.insert(part);
                }
            }
        }
        
        log.info("预包装数据保存成功: workId={}, boxCount={}, partCount={}", 
            workOrder.getWorkId(), 
            response.getPrePackageInfo().size(),
            response.getPrePackageInfo().stream()
                .mapToInt(info -> info.getBoxInfoList().stream()
                    .mapToInt(detail -> detail.getPartInfoList().size())
                    .sum())
                .sum());
    }
    
    /**
     * 处理拉取失败（FR-017/FR-017a）
     */
    private void handlePullFailure(WorkOrder workOrder, Exception e) {
        workOrder.setRetryCount(workOrder.getRetryCount() + 1);
        workOrder.setErrorMessage(e.getMessage());
        workOrder.setLastPullTime(LocalDateTime.now());
        
        if (workOrder.getRetryCount() >= 3) {
            // 标记为"拉取失败"（FR-017）
            workOrder.setPrepackageStatus("FAILED");
            workOrderMapper.updateById(workOrder);
            
            // 发送邮件通知（FR-017a）
            emailNotificationService.sendPullFailureNotification(
                workOrder.getBatchNum(),
                workOrder.getWorkId(),
                workOrder.getRetryCount(),
                e.getMessage()
            );
            
            log.error("预包装数据拉取失败，已发送邮件通知: workId={}, retryCount={}", 
                workOrder.getWorkId(), workOrder.getRetryCount(), e);
        } else {
            // 保持"未拉取"状态，等待下次定时任务重试
            workOrder.setPrepackageStatus("NOT_PULLED");
            workOrderMapper.updateById(workOrder);
            
            log.warn("预包装数据拉取失败，保持未拉取状态: workId={}, retryCount={}", 
                workOrder.getWorkId(), workOrder.getRetryCount(), e);
        }
    }
    
    /**
     * 工单数据修正与重新拉取（Story 7, FR-031-FR-041）
     */
    @Override
    @Transactional
    public void repullAndUpdate(String workId, String operator, String reason) {
        // 1. 查询工单
        WorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getWorkId, workId)
        );
        
        if (workOrder == null) {
            throw new IllegalArgumentException("工单不存在: " + workId);
        }
        
        // 2. 记录修正前数据
        String oldStatus = workOrder.getPrepackageStatus();
        int partCountBefore = countExistingParts(workOrder.getId());
        
        // 3. 重置状态为"未拉取"（FR-031）
        workOrder.setPrepackageStatus("NOT_PULLED");
        workOrder.setRetryCount(0);
        workOrder.setErrorMessage(null);
        workOrder.setUpdatedBy(operator); // 记录操作人
        workOrderMapper.updateById(workOrder);
        
        // 4. 记录修正日志（FR-039）
        WorkOrderCorrectionLog log = new WorkOrderCorrectionLog();
        log.setWorkOrderId(workOrder.getId());
        log.setWorkId(workId);
        log.setOperator(operator);
        log.setOperationTime(LocalDateTime.now());
        log.setCorrectionReason(reason);
        log.setOldStatus(oldStatus);
        log.setNewStatus("NOT_PULLED");
        log.setPartCountBefore(partCountBefore);
        log.setCreatedBy(operator);
        correctionLogMapper.insert(log);
        
        log.info("工单状态已重置为未拉取，等待定时任务重新拉取: workId={}, operator={}", 
            workId, operator);
        
        // 注意：实际的数据覆盖将由定时任务在拉取时执行
    }
    
    /**
     * 重新拉取时的数据覆盖逻辑（FR-033-FR-036）
     */
    @Transactional
    private void savePrePackageDataWithOverwrite(WorkOrder workOrder, PrePackageDataResponse response) {
        // 1. 查询旧数据
        PrePackageOrder oldOrder = prePackageOrderMapper.selectOne(
            new LambdaQueryWrapper<PrePackageOrder>()
                .eq(PrePackageOrder::getWorkOrderId, workOrder.getId())
        );
        
        if (oldOrder != null) {
            // 2. 标记旧板件为删除（软删除，保留报工记录关联）
            partMapper.update(
                new Part().setIsDeleted(1).setUpdatedBy("SYSTEM_DATA_CORRECTION"),
                new LambdaUpdateWrapper<Part>()
                    .eq(Part::getWorkId, workOrder.getWorkId())
                    .eq(Part::getIsDeleted, 0)
            );
            
            // 3. 删除旧包件（物理删除）
            List<Box> oldBoxes = boxMapper.selectList(
                new LambdaQueryWrapper<Box>()
                    .eq(Box::getPrepackageOrderId, oldOrder.getId())
            );
            for (Box box : oldBoxes) {
                packageMapper.delete(
                    new LambdaQueryWrapper<Package>()
                        .eq(Package::getBoxId, box.getId())
                );
            }
            
            // 4. 删除旧箱码（物理删除）
            boxMapper.delete(
                new LambdaQueryWrapper<Box>()
                    .eq(Box::getPrepackageOrderId, oldOrder.getId())
            );
            
            // 5. 删除旧订单（物理删除）
            prePackageOrderMapper.deleteById(oldOrder.getId());
        }
        
        // 6. 插入新数据（与首次拉取逻辑相同）
        savePrePackageData(workOrder, response);
        
        log.info("预包装数据已覆盖: workId={}, 旧板件已软删除，报工记录已保留", 
            workOrder.getWorkId());
    }
    
    private int countExistingParts(Long workOrderId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        return partMapper.selectCount(
            new LambdaQueryWrapper<Part>()
                .eq(Part::getWorkId, workOrder.getWorkId())
                .eq(Part::getIsDeleted, 0)
        );
    }
}
```

### 4.3 第三方MES接口客户端 (FR-010)

```java
@Service
@Slf4j
public class MesApiClient {
    
    @Value("${mes.third-party.base-url}")
    private String baseUrl;
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    
    public MesApiClient() {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 调用第三方MES系统获取预包装信息（FR-011）
     */
    public PrePackageDataResponse getPrePackageInfo(String batchNum, String workId) {
        // 构建请求体（FR-011）
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("service", Map.of("name", "getPrepackageInfo"));
        requestBody.put("payload", Map.of(
            "batchNum", batchNum,
            "workId", workId
        ));
        
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            
            Request request = new Request.Builder()
                .url(baseUrl + "/prepackage/query")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("第三方接口调用失败: HTTP " + response.code());
                }
                
                String responseBody = response.body().string();
                return objectMapper.readValue(responseBody, PrePackageDataResponse.class);
            }
            
        } catch (IOException e) {
            log.error("调用第三方MES接口失败: batchNum={}, workId={}", batchNum, workId, e);
            throw new RuntimeException("第三方接口调用失败", e);
        }
    }
}
```

**配置文件** (`application.yml`):

```yaml
mes:
  third-party:
    base-url: http://third-party-mes-api:8080/api  # 第三方MES系统地址
```

### 4.4 邮件通知服务 (FR-017a/FR-017b)

```java
@Service
@Slf4j
public class EmailNotificationServiceImpl implements EmailNotificationService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private EmailNotificationConfigMapper configMapper;
    
    @Override
    public void sendPullFailureNotification(String batchNum, String workId, 
                                           int retryCount, String errorMessage) {
        try {
            // 查询邮件配置
            EmailNotificationConfig config = configMapper.selectOne(
                new LambdaQueryWrapper<EmailNotificationConfig>()
                    .eq(EmailNotificationConfig::getEnabled, 1)
                    .last("LIMIT 1")
            );
            
            if (config == null) {
                log.warn("邮件通知未配置或已禁用，跳过发送");
                return;
            }
            
            // 构建邮件内容
            String subject = "【MES系统】预包装数据拉取失败通知";
            String content = buildFailureEmailContent(batchNum, workId, retryCount, errorMessage);
            
            // 发送邮件
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(config.getFromAddress());
            helper.setTo(config.getToAddresses().split(","));
            helper.setSubject(subject);
            helper.setText(content, true); // HTML格式
            
            mailSender.send(message);
            
            log.info("预包装拉取失败通知邮件已发送: workId={}, recipients={}", 
                workId, config.getToAddresses());
            
        } catch (Exception e) {
            // FR-017b: 邮件发送失败不阻塞主业务流程
            log.error("邮件发送失败: workId={}", workId, e);
        }
    }
    
    private String buildFailureEmailContent(String batchNum, String workId, 
                                            int retryCount, String errorMessage) {
        return String.format("""
            <html>
            <body>
                <h3>预包装数据拉取失败</h3>
                <table border="1" cellpadding="5" cellspacing="0">
                    <tr><td>失败时间</td><td>%s</td></tr>
                    <tr><td>批次号</td><td>%s</td></tr>
                    <tr><td>工单号</td><td>%s</td></tr>
                    <tr><td>重试次数</td><td>%d</td></tr>
                    <tr><td>失败原因</td><td>%s</td></tr>
                </table>
                <p>请及时检查第三方MES系统状态，必要时手动重置工单状态为"未拉取"以触发重新拉取。</p>
            </body>
            </html>
            """,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            batchNum,
            workId,
            retryCount,
            errorMessage
        );
    }
}
```

**Maven依赖** (`mes-service1/pom.xml`):

```xml
<!-- 邮件服务 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

**配置文件** (`application.yml`):

```yaml
spring:
  mail:
    host: smtp.qq.com
    port: 587
    username: 243219169@qq.com
    password: ${MAIL_AUTH_CODE}  # 从环境变量读取授权码
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true
```

### 4.5 产线查询服务 (Story 3/4/5)

```java
@Service
public class PartQueryServiceImpl implements PartQueryService {
    
    @Autowired
    private PartMapper partMapper;
    
    @Autowired
    private WorkOrderMapper workOrderMapper;
    
    @Autowired
    private BatchMapper batchMapper;
    
    @Override
    public PartWorkOrderBatchResponse queryWorkOrderAndBatch(String partCode) {
        // 1. 查询板件
        Part part = partMapper.selectOne(
            new LambdaQueryWrapper<Part>()
                .eq(Part::getPartCode, partCode)
                .eq(Part::getIsDeleted, 0)
        );
        
        if (part == null) {
            throw new PartNotFoundException(partCode);
        }
        
        // 2. 查询工单
        WorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getWorkId, part.getWorkId())
        );
        
        // FR-040/FR-040a: 检查工单是否处于"更新中"状态
        if ("UPDATING".equals(workOrder.getPrepackageStatus())) {
            throw new WorkOrderUpdatingException(workOrder.getWorkId());
        }
        
        // 3. 查询优化文件
        OptimizingFile optimizingFile = optimizingFileMapper.selectById(workOrder.getOptimizingFileId());
        
        // 4. 查询批次
        Batch batch = batchMapper.selectById(workOrder.getBatchId());
        
        // 5. 组装返回数据
        PartWorkOrderBatchResponse response = new PartWorkOrderBatchResponse();
        response.setWorkOrder(convertToWorkOrderInfo(workOrder));
        response.setOptimizingFile(convertToOptimizingFileInfo(optimizingFile));
        response.setBatch(convertToBatchInfo(batch));
        
        return response;
    }
    
    @Override
    public PartPackageResponse queryPackage(String partCode) {
        // 类似逻辑：查询板件→包件→箱码→订单
        // ... 实现细节略
    }
    
    @Override
    public PartDetailResponse queryDetail(String partCode) {
        Part part = partMapper.selectOne(
            new LambdaQueryWrapper<Part>()
                .eq(Part::getPartCode, partCode)
                .eq(Part::getIsDeleted, 0)
        );
        
        if (part == null) {
            throw new PartNotFoundException(partCode);
        }
        
        // FR-021: 返回板件全部字段
        return convertToDetailResponse(part);
    }
}
```

### 4.6 报工服务 (Story 6)

```java
@Service
public class WorkReportServiceImpl implements WorkReportService {
    
    @Autowired
    private WorkReportMapper workReportMapper;
    
    @Autowired
    private PartMapper partMapper;
    
    @Override
    public void saveWorkReport(WorkReportRequest request) {
        // 1. 查询板件（可选校验，FR-026未强制要求板件必须存在）
        Part part = partMapper.selectOne(
            new LambdaQueryWrapper<Part>()
                .eq(Part::getPartCode, request.getPartCode())
                .eq(Part::getIsDeleted, 0)
        );
        
        // 2. 幂等性检查（FR-030）
        WorkReport lastReport = workReportMapper.selectOne(
            new LambdaQueryWrapper<WorkReport>()
                .eq(WorkReport::getPartCode, request.getPartCode())
                .eq(WorkReport::getStationCode, request.getStationCode())
                .orderByDesc(WorkReport::getReportTime)
                .last("LIMIT 1")
        );
        
        if (lastReport != null && lastReport.getPartStatus().equals(request.getPartStatus())) {
            throw new DuplicateWorkReportException(
                "相同状态的重复报工: partCode=" + request.getPartCode() + 
                ", stationCode=" + request.getStationCode() + 
                ", status=" + request.getPartStatus()
            );
        }
        
        // 3. 保存报工记录
        WorkReport report = new WorkReport();
        report.setPartCode(request.getPartCode());
        report.setWorkId(part != null ? part.getWorkId() : null); // 冗余字段
        report.setPartStatus(request.getPartStatus());
        report.setStationCode(request.getStationCode());
        report.setStationName(request.getStationName());
        report.setReportTime(LocalDateTime.now());
        report.setIsDeleted(0);
        report.setCreatedBy("PRODUCTION_CLIENT"); // 产线客户端
        
        workReportMapper.insert(report);
        
        log.info("报工记录已保存: partCode={}, stationCode={}, status={}", 
            request.getPartCode(), request.getStationCode(), request.getPartStatus());
    }
}
```

---

## 5. 任务分解与排期

### 5.1 任务优先级划分

根据规格文档的用户故事优先级（P1/P2/P3）：

| 任务ID | 任务名称 | 优先级 | 依赖任务 | 预计工时 |
|-------|---------|-------|---------|---------|
| **T1** | **数据库设计与初始化** | P0 | - | 1天 |
| T1.1 | 编写8张表的DDL脚本 | P0 | - | 0.5天 |
| T1.2 | 创建索引和外键约束 | P0 | T1.1 | 0.25天 |
| T1.3 | 编写初始化数据脚本（邮件配置） | P0 | T1.1 | 0.25天 |
| **T2** | **Maven依赖与配置** | P0 | - | 0.5天 |
| T2.1 | 添加spring-boot-starter-mail | P0 | - | 0.1天 |
| T2.2 | 添加spring-retry | P0 | - | 0.1天 |
| T2.3 | 配置application.yml（邮件、第三方API地址） | P0 | - | 0.3天 |
| **T3** | **批次推送接口（Story 1, P1）** | P1 | T1, T2 | 2天 |
| T3.1 | 创建Entity、Mapper（Batch、WorkOrder） | P1 | T1 | 0.5天 |
| T3.2 | 创建DTO（BatchPushRequest/Response） | P1 | - | 0.3天 |
| T3.3 | 实现BatchService（幂等性处理） | P1 | T3.1, T3.2 | 0.7天 |
| T3.4 | 实现BatchController | P1 | T3.3 | 0.3天 |
| T3.5 | 单元测试（5个验收场景） | P1 | T3.4 | 0.2天 |
| **T4** | **预包装数据拉取（Story 2, P1）** | P1 | T3 | 3天 |
| T4.1 | 创建Entity、Mapper（PrePackageOrder、Box、Part） | P1 | T1 | 0.5天 |
| T4.2 | 实现MesApiClient（OkHttp） | P1 | T2.3 | 0.5天 |
| T4.3 | 实现PrePackageService（拉取+重试） | P1 | T4.1, T4.2 | 1天 |
| T4.4 | 实现PrePackagePullTask（定时任务+互斥） | P1 | T4.3 | 0.5天 |
| T4.5 | 实现EmailNotificationService | P1 | T2.1 | 0.3天 |
| T4.6 | 集成测试（6个验收场景） | P1 | T4.4, T4.5 | 0.2天 |
| **T5** | **产线查询接口（Story 3/4/5, P2）** | P2 | T4 | 2天 |
| T5.1 | 创建查询DTO（3个Response） | P2 | - | 0.3天 |
| T5.2 | 实现PartQueryService（3个查询方法） | P2 | T4.1 | 0.8天 |
| T5.3 | 实现PartQueryController | P2 | T5.2 | 0.4天 |
| T5.4 | 单元测试（11个验收场景） | P2 | T5.3 | 0.5天 |
| **T6** | **报工接口（Story 6, P3）** | P3 | T4 | 1天 |
| T6.1 | 创建Entity、Mapper（WorkReport） | P3 | T1 | 0.3天 |
| T6.2 | 创建DTO（WorkReportRequest） | P3 | - | 0.1天 |
| T6.3 | 实现WorkReportService（幂等性） | P3 | T6.1, T6.2 | 0.4天 |
| T6.4 | 实现WorkReportController | P3 | T6.3 | 0.2天 |
| **T7** | **数据修正功能（Story 7, P2）** | P2 | T4, T5 | 1.5天 |
| T7.1 | 创建Entity、Mapper（WorkOrderCorrectionLog） | P2 | T1 | 0.2天 |
| T7.2 | 实现repullAndUpdate方法（覆盖+保留） | P2 | T4.3, T7.1 | 0.8天 |
| T7.3 | 修改PrePackageService支持"更新中"状态 | P2 | T7.2 | 0.3天 |
| T7.4 | 集成测试（7个验收场景） | P2 | T7.3 | 0.2天 |
| **T8** | **集成测试与联调** | P1 | T3-T7 | 1天 |
| T8.1 | 端到端测试（模拟第三方MES） | P1 | T4 | 0.5天 |
| T8.2 | 边界场景测试（11个边界场景） | P1 | All | 0.3天 |
| T8.3 | 性能测试（21个成功标准） | P1 | All | 0.2天 |
| **T9** | **文档与部署** | P1 | T8 | 0.5天 |
| T9.1 | 编写API文档（SpringDoc） | P1 | T3-T7 | 0.2天 |
| T9.2 | 编写部署文档（环境变量、配置） | P1 | T2 | 0.2天 |
| T9.3 | 代码评审与优化 | P1 | All | 0.1天 |

### 5.2 开发排期（10个工作日）

```
第1天: T1 + T2 + T3.1-T3.2
第2天: T3.3-T3.5
第3天: T4.1-T4.3
第4天: T4.4-T4.6
第5天: T5.1-T5.2
第6天: T5.3-T5.4 + T7.1
第7天: T7.2-T7.4
第8天: T6.1-T6.4
第9天: T8.1-T8.3
第10天: T9.1-T9.3
```

**关键路径**: T1 → T2 → T3 → T4 → T5 → T7 → T8 → T9

### 5.3 里程碑

| 里程碑 | 完成标志 | 时间节点 |
|-------|---------|---------|
| **M1: 数据库就绪** | 8张表创建完成，索引和初始数据就绪 | Day 1 |
| **M2: P1功能完成** | 批次推送+预包装拉取+邮件通知 全部测试通过 | Day 4 |
| **M3: P2功能完成** | 产线查询+数据修正 全部测试通过 | Day 7 |
| **M4: P3功能完成** | 报工功能测试通过 | Day 8 |
| **M5: 集成测试通过** | 端到端+边界场景+性能测试 全部通过 | Day 9 |
| **M6: 功能上线就绪** | 文档完善，代码评审通过 | Day 10 |

---

## 6. 技术风险与对策

### 6.1 高风险项

| 风险ID | 风险描述 | 影响 | 概率 | 应对策略 |
|-------|---------|------|------|---------|
| **R1** | 第三方MES接口频繁超时或不稳定 | 高 | 中 | 1. 指数退避重试（已设计）<br>2. 邮件通知及时告警<br>3. 增加接口监控和日志 |
| **R2** | 定时任务执行时间超过1秒（大量工单积压） | 高 | 中 | 1. 每次处理50个工单上限<br>2. 互斥机制避免并发<br>3. 监控任务执行时长 |
| **R3** | 数据修正时并发冲突（产线查询+重新拉取） | 高 | 低 | 1. "更新中"状态+HTTP 409<br>2. 报工表独立，不受影响<br>3. 事务保证原子性 |
| **R4** | 预包装数据结构变化（第三方接口升级） | 中 | 低 | 1. 使用动态JSON解析<br>2. 版本兼容性设计<br>3. 接口契约测试 |
| **R5** | 邮件服务不可用（QQ邮箱限流/SMTP故障） | 低 | 低 | 1. 邮件发送失败不阻塞主流程<br>2. 记录ERROR日志<br>3. 运维可通过SQL查询失败工单 |

### 6.2 性能风险

| 性能指标 | 目标 (SC) | 风险点 | 应对策略 |
|---------|----------|-------|---------|
| **批次推送响应时间** | 500ms | 大批次（10+工单）事务时间长 | 1. 批量插入优化<br>2. 索引优化<br>3. 连接池配置 |
| **定时任务吞吐量** | 50工单/秒 | 单个工单拉取时间超预期 | 1. 限制单次处理数量<br>2. 第三方接口超时设置10s<br>3. 异步处理（后期优化） |
| **产线查询响应时间** | 200ms | 多表关联查询慢 | 1. 冗余字段设计<br>2. 索引优化<br>3. 查询缓存（可选） |

---

## 7. 测试策略

### 7.1 单元测试

**覆盖率目标**: 核心业务逻辑 ≥ 80%

**重点测试类**:
- `BatchServiceImpl`: 批次幂等性、工单批量保存
- `PrePackageServiceImpl`: 重试逻辑、数据覆盖、状态转换
- `PartQueryServiceImpl`: 边界场景（板件不存在、工单更新中）
- `WorkReportServiceImpl`: 报工幂等性

**示例测试用例**（JUnit 5 + Mockito）:

```java
@SpringBootTest
class PrePackageServiceTest {
    
    @Autowired
    private PrePackageService prePackageService;
    
    @MockBean
    private MesApiClient mesApiClient;
    
    @Test
    @DisplayName("预包装数据拉取成功，状态更新为PULLED")
    void testPullSuccess() {
        // Given
        WorkOrder workOrder = createTestWorkOrder("WO001", "NOT_PULLED");
        PrePackageDataResponse mockResponse = createMockResponse();
        when(mesApiClient.getPrePackageInfo(anyString(), anyString()))
            .thenReturn(mockResponse);
        
        // When
        prePackageService.pullSingleWorkOrder(workOrder);
        
        // Then
        WorkOrder updated = workOrderMapper.selectById(workOrder.getId());
        assertEquals("PULLED", updated.getPrepackageStatus());
        assertEquals(0, updated.getRetryCount());
    }
    
    @Test
    @DisplayName("预包装数据拉取失败3次，状态更新为FAILED并发送邮件")
    void testPullFailureAndEmailNotification() {
        // Given
        WorkOrder workOrder = createTestWorkOrder("WO002", "NOT_PULLED");
        when(mesApiClient.getPrePackageInfo(anyString(), anyString()))
            .thenThrow(new RuntimeException("接口超时"));
        
        // When
        assertThrows(RuntimeException.class, () -> {
            prePackageService.pullSingleWorkOrder(workOrder);
        });
        
        // Then
        WorkOrder updated = workOrderMapper.selectById(workOrder.getId());
        assertEquals("FAILED", updated.getPrepackageStatus());
        assertEquals(3, updated.getRetryCount());
        // 验证邮件发送（通过日志或Mock验证）
    }
}
```

### 7.2 集成测试

**目标**: 验证所有验收场景（31个）

**测试工具**: Testcontainers (MySQL) + WireMock (第三方API Mock)

**示例场景**（Story 2场景3）:

```java
@SpringBootTest
@Testcontainers
class PrePackagePullIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");
    
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();
    
    @Test
    @DisplayName("第三方接口调用失败，指数退避重试3次后标记失败")
    void testRetryLogicWithExponentialBackoff() {
        // Given: 模拟第三方接口全部失败
        wireMock.stubFor(post("/prepackage/query")
            .willReturn(aResponse().withStatus(500)));
        
        // When: 触发定时任务
        prePackagePullTask.pullPrePackageData();
        
        // Then: 验证重试3次
        wireMock.verify(exactly(3), postRequestedFor(urlEqualTo("/prepackage/query")));
        
        // 验证工单状态为FAILED
        WorkOrder workOrder = workOrderMapper.selectOne(
            new LambdaQueryWrapper<WorkOrder>().eq(WorkOrder::getWorkId, "WO001")
        );
        assertEquals("FAILED", workOrder.getPrepackageStatus());
        
        // 验证邮件已发送（检查邮件服务调用）
    }
}
```

### 7.3 边界场景测试

**测试清单**（对应规格文档的11个边界场景）:

- [ ] 批次推送时网络异常（超时/中断）
- [ ] 预包装数据拉取失败（重试+邮件）
- [ ] 同一批次号重复推送（幂等性）
- [ ] 板件码不存在或关联数据缺失
- [ ] 定时任务长时间运行（互斥机制）
- [ ] 预包装数据结构异常（容错解析）
- [ ] 报工数据幂等性（状态转换去重）
- [ ] 系统重启或宕机恢复（状态持久化）
- [ ] 工单数据修正时的并发冲突（HTTP 409）
- [ ] 重新拉取时板件数量变化（软删除+报工保留）
- [ ] 重新拉取过程中的报工请求（独立写入）

### 7.4 性能测试

**测试工具**: JMeter

**测试场景**（对应21个成功标准）:

| 测试场景 | 并发数 | 目标指标 | 验收标准 |
|---------|-------|---------|---------|
| **批次推送** | 100/s | 响应时间 ≤ 500ms | SC-001 |
| **定时任务吞吐** | 50工单/s | 处理完成 | SC-002 |
| **产线查询** | 500/s | 90% ≤ 200ms | SC-004 |
| **报工接口** | 200/s | 响应时间 ≤ 100ms | SC-005 |

---

## 8. 部署与运维

### 8.1 环境变量配置

```bash
# 第三方MES系统地址
MES_THIRD_PARTY_BASE_URL=http://third-party-mes-api:8080/api

# 邮件服务授权码
MAIL_AUTH_CODE=your_qq_mail_auth_code

# 数据库密码
SPRING_DATASOURCE_PASSWORD=your_db_password
```

### 8.2 数据库初始化

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE mes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 执行DDL脚本
mysql -u root -p mes < scripts/ddl/001-create-tables.sql

# 3. 执行初始化数据脚本
mysql -u root -p mes < scripts/ddl/002-init-data.sql
```

### 8.3 应用启动

```bash
# 启动 mes-service1
cd mes-service1
./start-service1.sh
```

### 8.4 健康检查

```bash
# Spring Boot Actuator健康检查
curl http://localhost:8080/actuator/health

# 定时任务状态监控
curl http://localhost:8080/actuator/scheduledtasks
```

---

## 9. 后续优化方向

基于当前规格要求完成后，可考虑以下优化：

1. **定时任务分布式化**: 引入Redis分布式锁，支持多实例部署
2. **查询性能优化**: 引入Redis缓存（`macula-boot-starter-redis`已注释）
3. **报工记录查询接口**: 补充按板件码/工单查询报工历史
4. **管理后台UI**: 提供失败工单管理界面（替代SQL手动操作）
5. **批次/工单直接查询**: 补充按批次号/工单号查询接口
6. **异步消息队列**: 将预包装拉取改为消息驱动（如RabbitMQ）

---

## 附录

### A. 数据库DDL脚本

完整DDL脚本路径：`scripts/ddl/001-create-tables.sql`

### B. 第三方接口Mock示例

WireMock配置示例：`src/test/resources/wiremock/mes-api-mock.json`

### C. 技术栈版本清单

| 组件 | 版本 |
|------|------|
| Macula Boot | 5.0.15 |
| Spring Boot | 3.x (由Macula Boot管理) |
| MyBatis-Plus | 3.x |
| MySQL | 8.0+ |
| OkHttp | 4.x |
| Spring Mail | 3.x |

---

**计划审批**: 待技术评审通过后开始实施  
**预计完成日期**: 2026-02-10（10个工作日后）  
**负责人**: 开发团队  
**审批人**: 技术负责人
