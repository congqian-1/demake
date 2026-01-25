-- MES系统数据库测试脚本
-- 用于验证数据库结构和初始化数据

-- 1. 验证所有表是否创建成功
SELECT '检查所有表...' AS status;

SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'mes'
    AND TABLE_NAME LIKE 'mes_%'
ORDER BY TABLE_NAME;

-- 2. 验证索引是否创建成功
SELECT '检查索引...' AS status;

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'mes'
    AND TABLE_NAME LIKE 'mes_%'
    AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- 3. 验证邮件配置初始化数据
SELECT '检查邮件配置...' AS status;

SELECT 
    id,
    notification_type,
    recipient_emails,
    is_enabled,
    remark
FROM mes_email_config;

-- 4. 插入测试批次数据
SELECT '插入测试数据...' AS status;

INSERT INTO mes_batch (
    batch_no, batch_description, batch_status, data_source,
    batch_push_time, batch_receive_time, raw_json_data,
    create_time, update_time
) VALUES (
    'BATCH-TEST-001',
    '数据库测试批次',
    'PENDING',
    'PUSH',
    NOW(),
    NOW(),
    '{"test": "data"}',
    NOW(),
    NOW()
);

-- 5. 插入测试工单数据
INSERT INTO mes_work_order (
    work_order_no, batch_no, customer_name, product_model,
    order_quantity, planned_start_date, planned_end_date,
    work_order_status, priority, remark,
    create_time, update_time
) VALUES (
    'WO-TEST-001',
    'BATCH-TEST-001',
    '测试客户',
    'MODEL-TEST',
    100,
    NOW(),
    DATE_ADD(NOW(), INTERVAL 7 DAY),
    'PENDING',
    'HIGH',
    '数据库测试工单',
    NOW(),
    NOW()
);

-- 6. 插入测试优化文件数据
INSERT INTO mes_optimization_file (
    batch_no, file_name, file_path, file_size,
    file_upload_time, file_status, remark,
    create_time, update_time
) VALUES (
    'BATCH-TEST-001',
    'test_optimization.txt',
    '/test/optimization.txt',
    1024,
    NOW(),
    'UPLOADED',
    '数据库测试文件',
    NOW(),
    NOW()
);

-- 7. 验证测试数据
SELECT '验证测试数据...' AS status;

SELECT 
    b.batch_no,
    b.batch_description,
    b.batch_status,
    COUNT(DISTINCT w.id) AS work_order_count,
    COUNT(DISTINCT f.id) AS file_count
FROM mes_batch b
LEFT JOIN mes_work_order w ON b.batch_no = w.batch_no
LEFT JOIN mes_optimization_file f ON b.batch_no = f.batch_no
WHERE b.batch_no = 'BATCH-TEST-001'
GROUP BY b.batch_no, b.batch_description, b.batch_status;

-- 8. 查看批次详情
SELECT '批次详情:' AS status;
SELECT * FROM mes_batch WHERE batch_no = 'BATCH-TEST-001'\G

SELECT '工单详情:' AS status;
SELECT * FROM mes_work_order WHERE batch_no = 'BATCH-TEST-001'\G

SELECT '优化文件详情:' AS status;
SELECT * FROM mes_optimization_file WHERE batch_no = 'BATCH-TEST-001'\G

-- 9. 清理测试数据（可选）
-- 取消注释以下语句来清理测试数据
/*
DELETE FROM mes_optimization_file WHERE batch_no = 'BATCH-TEST-001';
DELETE FROM mes_work_order WHERE batch_no = 'BATCH-TEST-001';
DELETE FROM mes_batch WHERE batch_no = 'BATCH-TEST-001';
SELECT '测试数据已清理' AS status;
*/
