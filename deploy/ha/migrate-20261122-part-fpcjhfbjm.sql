-- 板件表增加第三方 MES 预包装字段 FPCJHFBJM，兼容尚未采集该字段的板件。
ALTER TABLE mes_part
    ADD COLUMN fpcjhfbjm VARCHAR(128) DEFAULT NULL COMMENT '第三方 MES 板件字段 FPCJHFBJM' AFTER groove;
