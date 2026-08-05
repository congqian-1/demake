-- 板件表增加 FJYPH（初始简易批次号）字段
ALTER TABLE mes_part ADD COLUMN fj_yph VARCHAR(100) DEFAULT NULL COMMENT '初始简易批次号（FJYPH）' AFTER groove;
