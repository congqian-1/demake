-- ============================================================================
-- MES 系统对接集成 - 初始化数据脚本
-- Feature: 001-mes-integration
-- Created: 2026-01-25
-- ============================================================================

-- 邮件通知配置初始化数据
-- 注意：实际密码（授权码）需要通过环境变量或配置文件提供
INSERT INTO `mes_email_notification_config` 
  (`smtp_host`, `smtp_port`, `username`, `password`, `from_address`, `to_addresses`, `enabled`, `created_by`) 
VALUES 
  ('smtp.qq.com', 587, '243219169@qq.com', 'PLEASE_SET_AUTH_CODE_IN_ENV', '243219169@qq.com', '243219169@qq.com', 1, 'SYSTEM');

-- 说明：
-- 1. 实际部署时需将 'PLEASE_SET_AUTH_CODE_IN_ENV' 替换为QQ邮箱授权码
-- 2. 或通过application.yml配置spring.mail.password，数据库中的配置作为备用
-- 3. 收件人地址可根据实际需求修改（支持逗号分隔多个邮箱）
