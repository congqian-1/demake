-- 看板工序同步记录表：按 (batch_num, work_id) 唯一，每个工单独立记录同步结果。
CREATE TABLE IF NOT EXISTS mes_panel_process_sync (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_num    VARCHAR(100) NOT NULL                COMMENT '批次号',
    work_id      VARCHAR(100) NOT NULL                COMMENT '工单号',
    sync_result  VARCHAR(50)  DEFAULT NULL            COMMENT '同步结果：SUCCESS / FAILED',
    error_detail TEXT         DEFAULT NULL            COMMENT '失败原因详情',
    synced_at    DATETIME     DEFAULT NULL            COMMENT '同步完成时间',
    created_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_work (batch_num, work_id),
    KEY idx_batch_num (batch_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='看板工序同步记录表';
