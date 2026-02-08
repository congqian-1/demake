ALTER TABLE mes_prepackage_order
    ADD COLUMN is_project TINYINT NULL COMMENT '是否项目(第三方 isProject)',
    ADD COLUMN customer_name VARCHAR(255) NULL COMMENT '客户名称(第三方 customerName)',
    ADD COLUMN fnumber VARCHAR(128) NULL COMMENT 'FNUMBER',
    ADD COLUMN dob VARCHAR(64) NULL COMMENT 'DOB',
    ADD COLUMN detailed_address VARCHAR(512) NULL COMMENT '详细地址(第三方 detailedAddress)';

ALTER TABLE mes_box
    ADD COLUMN unit VARCHAR(128) NULL COMMENT '单元(第三方 unit)';

ALTER TABLE mes_package
    ADD COLUMN box_type2 VARCHAR(64) NULL COMMENT '箱型二级(第三方 boxType2)';

ALTER TABLE mes_part
    ADD COLUMN standard_code VARCHAR(128) NULL COMMENT '标准码(第三方 standardCode)';
