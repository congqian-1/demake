-- Add real package number to mes_part
ALTER TABLE mes_part
  ADD COLUMN real_package_no VARCHAR(100) NULL COMMENT '真实打包包号' AFTER standard_list;
