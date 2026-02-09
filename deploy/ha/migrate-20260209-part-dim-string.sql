-- Change mes_part dimension/axis columns to string
ALTER TABLE mes_part
    MODIFY COLUMN item_length VARCHAR(64) NULL COMMENT '板件长',
    MODIFY COLUMN item_width VARCHAR(64) NULL COMMENT '板件宽',
    MODIFY COLUMN item_depth VARCHAR(64) NULL COMMENT '板件高',
    MODIFY COLUMN x_axis VARCHAR(64) NULL COMMENT 'X轴坐标',
    MODIFY COLUMN y_axis VARCHAR(64) NULL COMMENT 'Y轴坐标',
    MODIFY COLUMN z_axis VARCHAR(64) NULL COMMENT 'Z轴坐标';
