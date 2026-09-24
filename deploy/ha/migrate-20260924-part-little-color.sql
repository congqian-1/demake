-- 板件表增加小花色字段
ALTER TABLE mes_part
    ADD COLUMN little_color VARCHAR(128) DEFAULT NULL COMMENT '小花色' AFTER texture;
