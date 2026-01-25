-- ============================================================================
-- MES 系统对接集成 - 索引和外键约束脚本
-- Feature: 001-mes-integration
-- Created: 2026-01-25
-- 说明：所有基本索引已在001-create-tables.sql中创建，本脚本仅添加外键约束
-- ============================================================================

-- 添加外键约束

-- 优化文件表 → 批次表
ALTER TABLE `mes_optimizing_file`
  ADD CONSTRAINT `fk_optimizing_file_batch` 
  FOREIGN KEY (`batch_id`) REFERENCES `mes_batch` (`id`) ON DELETE CASCADE;

-- 工单表 → 批次表
ALTER TABLE `mes_work_order`
  ADD CONSTRAINT `fk_work_order_batch` 
  FOREIGN KEY (`batch_id`) REFERENCES `mes_batch` (`id`) ON DELETE CASCADE;

-- 工单表 → 优化文件表
ALTER TABLE `mes_work_order`
  ADD CONSTRAINT `fk_work_order_optimizing_file` 
  FOREIGN KEY (`optimizing_file_id`) REFERENCES `mes_optimizing_file` (`id`) ON DELETE CASCADE;

-- 预包装订单表 → 工单表
ALTER TABLE `mes_prepackage_order`
  ADD CONSTRAINT `fk_prepackage_work_order` 
  FOREIGN KEY (`work_order_id`) REFERENCES `mes_work_order` (`id`) ON DELETE CASCADE;

-- 预包装订单表 → 批次表
ALTER TABLE `mes_prepackage_order`
  ADD CONSTRAINT `fk_prepackage_batch` 
  FOREIGN KEY (`batch_id`) REFERENCES `mes_batch` (`id`) ON DELETE CASCADE;

-- 箱码表 → 预包装订单表
ALTER TABLE `mes_box`
  ADD CONSTRAINT `fk_box_prepackage_order` 
  FOREIGN KEY (`prepackage_order_id`) REFERENCES `mes_prepackage_order` (`id`) ON DELETE CASCADE;

-- 包件表 → 箱码表
ALTER TABLE `mes_package`
  ADD CONSTRAINT `fk_package_box` 
  FOREIGN KEY (`box_id`) REFERENCES `mes_box` (`id`) ON DELETE CASCADE;

-- 板件表 → 包件表
ALTER TABLE `mes_part`
  ADD CONSTRAINT `fk_part_package` 
  FOREIGN KEY (`package_id`) REFERENCES `mes_package` (`id`) ON DELETE CASCADE;

-- 注意：报工记录表不设置物理外键约束，保证报工记录独立性
-- 注意：工单数据修正日志表不设置物理外键约束，保证日志完整性

-- ============================================================================
-- 索引说明（已在001-create-tables.sql中创建）
-- ============================================================================
-- 1. 唯一索引保证数据唯一性：批次号、工单号、箱码、板件码
-- 2. 外键索引优化关联查询：batch_id、optimizing_file_id、work_order_id等
-- 3. 业务查询索引：prepackage_status（定时任务）、part_code（产线查询）、batch_num（批次统计）
-- 4. 时间索引：report_time（报工轨迹）、last_pull_time（拉取监控）
-- 5. 逻辑删除索引：is_deleted（过滤已删除记录）
