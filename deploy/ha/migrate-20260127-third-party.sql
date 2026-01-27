-- Migration for updated third-party batch push payload
-- Run in MySQL: USE mes; then execute this file

ALTER TABLE mes_batch
  MODIFY COLUMN product_time datetime NOT NULL COMMENT '生产日期',
  ADD COLUMN nesting_time datetime NULL COMMENT '开料/排样时间' AFTER product_time,
  ADD COLUMN ymba014 varchar(100) NULL COMMENT '线路/区域信息' AFTER simple_batch_num,
  ADD COLUMN ymba016 varchar(20) NULL COMMENT '属性标识' AFTER ymba014;

ALTER TABLE mes_work_order
  ADD COLUMN route_id varchar(100) NULL COMMENT '线路ID' AFTER route,
  ADD COLUMN delivery_time datetime NULL COMMENT '交付日期' AFTER order_type,
  ADD COLUMN nesting_time datetime NULL COMMENT '开料/排样时间' AFTER delivery_time,
  ADD COLUMN ymba014 varchar(100) NULL COMMENT '线路/区域信息' AFTER nesting_time,
  ADD COLUMN ymba015 varchar(100) NULL COMMENT '工位/区域信息' AFTER ymba014,
  ADD COLUMN ymba016 varchar(20) NULL COMMENT '属性标识' AFTER ymba015,
  ADD COLUMN part0 varchar(100) NULL COMMENT '部件字段' AFTER ymba016,
  ADD COLUMN condition0 varchar(100) NULL COMMENT '条件字段' AFTER part0,
  ADD COLUMN part_time0 datetime NULL COMMENT '部件时间字段' AFTER condition0,
  ADD COLUMN zuz int NULL COMMENT '组/套标记' AFTER part_time0;
