ALTER TABLE mes_prepackage_order
    ADD COLUMN type VARCHAR(64) NULL COMMENT '类型（第三方 type）' AFTER product_type,
    ADD COLUMN fdd8 VARCHAR(128) NULL COMMENT '扩展字段（第三方 FDD8）' AFTER type;
