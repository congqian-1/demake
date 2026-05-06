-- Add pending-reprocess marker for coexistence single-flight protocol.
ALTER TABLE mes_work_order
    ADD COLUMN reprocess_pending TINYINT DEFAULT 0 COMMENT '挂起重拉标记（0=无，1=有）' AFTER error_message;

CREATE INDEX idx_reprocess_pending ON mes_work_order (reprocess_pending);
