-- ================================================
-- PrePackagePullTask 集成测试数据脚本
-- ================================================

USE mes;

-- 1. 清理旧的测试数据
DELETE FROM mes_correction_log WHERE work_id LIKE 'TEST-%';
DELETE FROM mes_board WHERE work_id LIKE 'TEST-%';
DELETE FROM mes_package WHERE work_id LIKE 'TEST-%';
DELETE FROM mes_box_code WHERE work_id LIKE 'TEST-%';
DELETE FROM mes_prepackage_order WHERE work_id LIKE 'TEST-%';
DELETE FROM mes_work_order WHERE work_id LIKE 'TEST-%';
DELETE FROM mes_batch WHERE batch_num LIKE 'TEST-%';

SELECT '清理旧测试数据完成' AS status;

-- 2. 插入测试批次数据
INSERT INTO mes_batch (
    batch_num, batch_description, batch_status, data_source,
    batch_push_time, batch_receive_time, create_time, update_time
) VALUES (
    'TEST-001',
    '预包装拉取测试批次',
    'COMPLETED',
    'MANUAL',
    NOW(),
    NOW(),
    NOW(),
    NOW()
);

SELECT '批次数据已插入: TEST-001' AS status;

-- 3. 插入测试工单数据 - 状态为 NOT_PULLED（待拉取）
INSERT INTO mes_work_order (
    work_id, batch_id, batch_num, customer, product_type,
    work_num, pack_type, prepackage_status, retry_count, error_message,
    last_pull_time, create_time, update_time
) VALUES (
    'TEST-001-001',
    (SELECT id FROM mes_batch WHERE batch_num = 'TEST-001'),
    'TEST-001',
    '测试客户A',
    '板式家具',
    '100',
    '标准包装',
    'NOT_PULLED',  -- 关键：设置为 NOT_PULLED，定时任务会拉取这个工单
    0,
    NULL,
    NULL,
    NOW(),
    NOW()
),
(
    'TEST-001-002',
    (SELECT id FROM mes_batch WHERE batch_num = 'TEST-001'),
    'TEST-001',
    '测试客户B',
    '板式家具',
    '200',
    '标准包装',
    'NOT_PULLED',  -- 关键：设置为 NOT_PULLED
    0,
    NULL,
    NULL,
    NOW(),
    NOW()
),
(
    'TEST-001-003',
    (SELECT id FROM mes_batch WHERE batch_num = 'TEST-001'),
    'TEST-001',
    '测试客户C',
    '板式家具',
    '150',
    '标准包装',
    'NOT_PULLED',  -- 关键：设置为 NOT_PULLED
    0,
    NULL,
    NULL,
    NOW(),
    NOW()
);

SELECT '测试工单数据已插入: 3个工单，状态为NOT_PULLED' AS status;

-- 4. 验证测试数据
SELECT '验证测试数据...' AS status;

SELECT 
    w.work_id,
    w.batch_num,
    w.customer,
    w.prepackage_status,
    w.retry_count,
    b.batch_status AS batch_status
FROM mes_work_order w
LEFT JOIN mes_batch b ON w.batch_num = b.batch_num
WHERE w.work_id LIKE 'TEST-%'
ORDER BY w.work_id;

-- 5. 查询当前数据库中预包装数据统计
SELECT '当前预包装数据统计（拉取前）:' AS status;
SELECT 
    'mes_prepackage_order' AS table_name,
    COUNT(*) AS record_count
FROM mes_prepackage_order
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_box_code' AS table_name,
    COUNT(*) AS record_count
FROM mes_box_code
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_package' AS table_name,
    COUNT(*) AS record_count
FROM mes_package
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_board' AS table_name,
    COUNT(*) AS record_count
FROM mes_board
WHERE work_id LIKE 'TEST-%';

-- ================================================
-- 使用说明：
-- 1. 执行此脚本创建测试数据
-- 2. 启动 mes-service1 服务
-- 3. 等待定时任务执行（每秒一次）
-- 4. 查看日志：tail -f /logs/mes-service1/mes-service1.log | grep "预包装"
-- 5. 执行验证脚本检查数据是否入库
-- ================================================
