-- ================================================
-- PrePackagePullTask 集成测试验证脚本
-- 用于验证定时任务执行后数据是否真正入库
-- ================================================

USE mes;

-- 1. 查询测试工单状态
SELECT '测试工单状态:' AS status;
SELECT 
    work_id,
    batch_num,
    customer,
    prepackage_status,
    retry_count,
    last_pull_time,
    error_message,
    DATE_FORMAT(create_time, '%Y-%m-%d %H:%i:%s') AS create_time,
    DATE_FORMAT(update_time, '%Y-%m-%d %H:%i:%s') AS update_time
FROM mes_work_order
WHERE work_id LIKE 'TEST-%'
ORDER BY work_id;

-- 2. 查询预包装订单数据
SELECT '预包装订单数据:' AS status;
SELECT 
    id,
    work_id,
    batch_num,
    order_num,
    consignor,
    receiver,
    ship_batch,
    total_set,
    max_package_no,
    DATE_FORMAT(created_time, '%Y-%m-%d %H:%i:%s') AS created_time
FROM mes_prepackage_order
WHERE work_id LIKE 'TEST-%'
ORDER BY work_id, id;

-- 3. 查询箱码数据
SELECT '箱码数据:' AS status;
SELECT 
    id,
    work_id,
    box_code,
    building,
    house,
    room,
    setno,
    color,
    DATE_FORMAT(created_time, '%Y-%m-%d %H:%i:%s') AS created_time
FROM mes_box_code
WHERE work_id LIKE 'TEST-%'
ORDER BY work_id, id
LIMIT 20;

-- 4. 查询包件数据
SELECT '包件数据:' AS status;
SELECT 
    id,
    work_id,
    box_code,
    package_no,
    length,
    width,
    depth,
    weight,
    part_count,
    box_type,
    DATE_FORMAT(created_time, '%Y-%m-%d %H:%i:%s') AS created_time
FROM mes_package
WHERE work_id LIKE 'TEST-%'
ORDER BY work_id, id
LIMIT 20;

-- 5. 查询板件数据
SELECT '板件数据:' AS status;
SELECT 
    id,
    work_id,
    part_code,
    layer,
    piece,
    item_code,
    item_name,
    mat_name,
    DATE_FORMAT(created_time, '%Y-%m-%d %H:%i:%s') AS created_time
FROM mes_board
WHERE work_id LIKE 'TEST-%'
ORDER BY work_id, id
LIMIT 20;

-- 6. 统计入库数据
SELECT '入库数据统计:' AS status;
SELECT 
    'mes_work_order' AS table_name,
    COUNT(*) AS total_count,
    SUM(CASE WHEN prepackage_status = 'PULLED' THEN 1 ELSE 0 END) AS pulled_count,
    SUM(CASE WHEN prepackage_status = 'NOT_PULLED' THEN 1 ELSE 0 END) AS not_pulled_count,
    SUM(CASE WHEN prepackage_status = 'FAILED' THEN 1 ELSE 0 END) AS failed_count
FROM mes_work_order
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_prepackage_order' AS table_name,
    COUNT(*) AS total_count,
    NULL AS pulled_count,
    NULL AS not_pulled_count,
    NULL AS failed_count
FROM mes_prepackage_order
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_box_code' AS table_name,
    COUNT(*) AS total_count,
    NULL AS pulled_count,
    NULL AS not_pulled_count,
    NULL AS failed_count
FROM mes_box_code
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_package' AS table_name,
    COUNT(*) AS total_count,
    NULL AS pulled_count,
    NULL AS not_pulled_count,
    NULL AS failed_count
FROM mes_package
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_board' AS table_name,
    COUNT(*) AS total_count,
    NULL AS pulled_count,
    NULL AS not_pulled_count,
    NULL AS failed_count
FROM mes_board
WHERE work_id LIKE 'TEST-%';

-- 7. 查询修正日志（如果有）
SELECT '修正日志:' AS status;
SELECT 
    id,
    work_id,
    batch_num,
    operator_name,
    correction_type,
    correction_reason,
    old_value,
    new_value,
    DATE_FORMAT(correction_time, '%Y-%m-%d %H:%i:%s') AS correction_time
FROM mes_correction_log
WHERE work_id LIKE 'TEST-%'
ORDER BY correction_time DESC;

-- 8. 详细数据关系验证
SELECT '数据关系验证:' AS status;
SELECT 
    w.work_id,
    COUNT(DISTINCT po.id) AS order_count,
    COUNT(DISTINCT bc.id) AS box_count,
    COUNT(DISTINCT pkg.id) AS package_count,
    COUNT(DISTINCT bd.id) AS board_count
FROM mes_work_order w
LEFT JOIN mes_prepackage_order po ON w.work_id = po.work_id
LEFT JOIN mes_box_code bc ON w.work_id = bc.work_id
LEFT JOIN mes_package pkg ON w.work_id = pkg.work_id
LEFT JOIN mes_board bd ON w.work_id = bd.work_id
WHERE w.work_id LIKE 'TEST-%'
GROUP BY w.work_id
ORDER BY w.work_id;

-- 9. 最近的数据变更
SELECT '最近数据变更:' AS status;
SELECT 
    'mes_work_order' AS table_name,
    work_id AS identifier,
    prepackage_status AS status_value,
    updated_time AS change_time
FROM mes_work_order
WHERE work_id LIKE 'TEST-%'

UNION ALL

SELECT 
    'mes_prepackage_order' AS table_name,
    work_id AS identifier,
    'INSERTED' AS status_value,
    updated_time AS change_time
FROM mes_prepackage_order
WHERE work_id LIKE 'TEST-%'

ORDER BY change_time DESC
LIMIT 10;

-- ================================================
-- 判断标准：
-- 1. mes_work_order.prepackage_status 应该从 'NOT_PULLED' 变为 'PULLED' 或 'NO_DATA' 或 'FAILED'
-- 2. 如果是 'PULLED'，则应该有对应的 mes_prepackage_order 数据
-- 3. mes_prepackage_order 应该有对应的 mes_box_code 数据
-- 4. mes_box_code 应该有对应的 mes_package 数据
-- 5. mes_package 应该有对应的 mes_board 数据
-- ================================================
