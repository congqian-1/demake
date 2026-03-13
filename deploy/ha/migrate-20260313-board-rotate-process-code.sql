ALTER TABLE mes_part
    ADD COLUMN rotate VARCHAR(32) NULL COMMENT '旋转字段' AFTER standard_code,
    ADD COLUMN process_code VARCHAR(128) NULL COMMENT '工艺代码' AFTER rotate;
