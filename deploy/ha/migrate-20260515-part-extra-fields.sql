ALTER TABLE mes_part
    ADD COLUMN workmanship VARCHAR(128) NULL COMMENT '工艺名称' AFTER process_code,
    ADD COLUMN order_number VARCHAR(128) NULL COMMENT '分单号' AFTER workmanship,
    ADD COLUMN sealing_flat_noodles VARCHAR(128) NULL COMMENT '封板条' AFTER order_number,
    ADD COLUMN texture VARCHAR(128) NULL COMMENT '纹理' AFTER sealing_flat_noodles,
    ADD COLUMN container_number VARCHAR(128) NULL COMMENT '柜号' AFTER texture,
    ADD COLUMN set_number VARCHAR(128) NULL COMMENT '套号' AFTER container_number,
    ADD COLUMN groove VARCHAR(128) NULL COMMENT '槽' AFTER set_number;
