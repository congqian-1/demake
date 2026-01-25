-- ================================================
-- PrePackagePullTask 模拟测试脚本
-- 手动插入预包装数据到数据库（不依赖第三方MES）
-- ================================================

USE mes;

-- 1. 清理旧的测试数据
DELETE FROM mes_correction_log WHERE work_id LIKE 'MOCK-%';
DELETE FROM mes_board WHERE work_id LIKE 'MOCK-%';
DELETE FROM mes_package WHERE work_id LIKE 'MOCK-%';
DELETE FROM mes_box_code WHERE work_id LIKE 'MOCK-%';
DELETE FROM mes_prepackage_order WHERE work_id LIKE 'MOCK-%';
DELETE FROM mes_work_order WHERE work_id LIKE 'MOCK-%';
DELETE FROM mes_batch WHERE batch_num LIKE 'MOCK-%';

SELECT '清理旧测试数据完成' AS status;

-- 2. 插入测试批次数据
INSERT INTO mes_batch (
    batch_num, batch_description, batch_status, data_source,
    batch_push_time, batch_receive_time, create_time, update_time
) VALUES (
    'MOCK-001',
    '模拟预包装拉取测试批次',
    'COMPLETED',
    'MANUAL',
    NOW(),
    NOW(),
    NOW(),
    NOW()
);

SELECT '批次数据已插入: MOCK-001' AS status;

-- 3. 插入测试工单数据
INSERT INTO mes_work_order (
    work_id, batch_id, batch_num, customer, product_type,
    work_num, pack_type, prepackage_status, retry_count, error_message,
    last_pull_time, create_time, update_time
) VALUES (
    'MOCK-001-001',
    (SELECT id FROM mes_batch WHERE batch_num = 'MOCK-001'),
    'MOCK-001',
    '模拟客户A',
    '板式家具',
    '100',
    '标准包装',
    'NOT_PULLED',
    0,
    NULL,
    NULL,
    NOW(),
    NOW()
),
(
    'MOCK-001-002',
    (SELECT id FROM mes_batch WHERE batch_num = 'MOCK-001'),
    'MOCK-001',
    '模拟客户B',
    '板式家具',
    '200',
    '标准包装',
    'NOT_PULLED',
    0,
    NULL,
    NULL,
    NOW(),
    NOW()
);

SELECT '测试工单数据已插入: 2个工单，状态为NOT_PULLED' AS status;

-- 4. 直接插入预包装订单数据（模拟第三方MES返回的数据）
-- 工单 MOCK-001-001 的预包装订单
INSERT INTO mes_prepackage_order (
    work_order_id, batch_id, batch_num, work_id, order_num, consignor,
    contract_no, work_num, receiver, phone, ship_batch, install_address,
    customer, receive_region, space, pack_type, product_type,
    prepackage_info_size, total_set, max_package_no, production_num,
    create_time, update_time, created_time, updated_time
) VALUES (
    (SELECT id FROM mes_work_order WHERE work_id = 'MOCK-001-001'),
    (SELECT id FROM mes_batch WHERE batch_num = 'MOCK-001'),
    'MOCK-001',
    'MOCK-001-001',
    'ORDER-20250125-001',
    '模拟发货人',
    'CONTRACT-001',
    '100',
    '模拟收货人',
    '13800138000',
    'SHIP-001',
    '模拟安装地址',
    '模拟客户A',
    '模拟收货区域',
    '100',
    '标准包装',
    '板式家具',
    50,
    10,
    5,
    'PROD-001',
    NOW(),
    NOW(),
    NOW(),
    NOW()
);

SELECT '预包装订单数据已插入（工单 MOCK-001-001）' AS status;

-- 5. 插入箱码数据
INSERT INTO mes_box_code (
    prepackage_order_id, batch_num, work_id, box_code,
    building, house, room, setno, color,
    create_time, update_time, created_time, updated_time
) VALUES (
    (SELECT id FROM mes_prepackage_order WHERE work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'BOX-001',
    'A栋',
    '101',
    '客厅',
    '1',
    '白色',
    NOW(),
    NOW(),
    NOW(),
    NOW()
),
(
    (SELECT id FROM mes_prepackage_order WHERE work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'BOX-002',
    'A栋',
    '102',
    '主卧',
    '1',
    '白色',
    NOW(),
    NOW(),
    NOW(),
    NOW()
);

SELECT '箱码数据已插入（2个箱码）' AS status;

-- 6. 插入包件数据
INSERT INTO mes_package (
    box_id, batch_num, work_id, box_code, package_no,
    length, width, depth, weight, part_count, box_type,
    create_time, update_time, created_time, updated_time
) VALUES (
    (SELECT id FROM mes_box_code WHERE box_code = 'BOX-001' AND work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'BOX-001',
    'PKG-001-1',
    200.5,
    100.0,
    50.0,
    15.5,
    10,
    '标准箱',
    NOW(),
    NOW(),
    NOW(),
    NOW()
),
(
    (SELECT id FROM mes_box_code WHERE box_code = 'BOX-002' AND work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'BOX-002',
    'PKG-002-1',
    180.0,
    90.0,
    45.0,
    12.0,
    8,
    '标准箱',
    NOW(),
    NOW(),
    NOW(),
    NOW()
);

SELECT '包件数据已插入（2个包件）' AS status;

-- 7. 插入板件数据
INSERT INTO mes_board (
    package_id, box_id, batch_num, work_id, part_code,
    layer, piece, item_code, item_name, mat_name,
    item_length, item_width, item_depth, x_axis, y_axis, z_axis,
    sort_order, standard_list, is_deleted,
    create_time, update_time, created_time, updated_time
) VALUES (
    (SELECT id FROM mes_package WHERE package_no = 'PKG-001-1' AND work_id = 'MOCK-001-001'),
    (SELECT id FROM mes_box_code WHERE box_code = 'BOX-001' AND work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'PART-001-1-1',
    1,
    1,
    'ITEM-001',
    '模拟板件A',
    '颗粒板',
    2000.0,
    600.0,
    18.0,
    100.0,
    200.0,
    300.0,
    1,
    '[]',
    0,
    NOW(),
    NOW(),
    NOW(),
    NOW()
),
(
    (SELECT id FROM mes_package WHERE package_no = 'PKG-001-1' AND work_id = 'MOCK-001-001'),
    (SELECT id FROM mes_box_code WHERE box_code = 'BOX-001' AND work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'PART-001-1-2',
    2,
    2,
    'ITEM-002',
    '模拟板件B',
    '多层板',
    1800.0,
    500.0,
    15.0,
    150.0,
    180.0,
    250.0,
    2,
    '[]',
    0,
    NOW(),
    NOW(),
    NOW(),
    NOW()
),
(
    (SELECT id FROM mes_package WHERE package_no = 'PKG-002-1' AND work_id = 'MOCK-001-001'),
    (SELECT id FROM mes_box_code WHERE box_code = 'BOX-002' AND work_id = 'MOCK-001-001'),
    'MOCK-001',
    'MOCK-001-001',
    'PART-002-1-1',
    1,
    1,
    'ITEM-003',
    '模拟板件C',
    '密度板',
    1600.0,
    400.0,
    16.0,
    120.0,
    160.0,
    200.0,
    1,
    '[]',
    0,
    NOW(),
    NOW(),
    NOW(),
    NOW()
);

SELECT '板件数据已插入（3个板件）' AS status;

-- 8. 更新工单状态为已拉取
UPDATE mes_work_order
SET prepackage_status = 'PULLED',
    last_pull_time = NOW(),
    update_time = NOW()
WHERE work_id = 'MOCK-001-001';

SELECT '工单 MOCK-001-001 状态已更新为 PULLED' AS status;

-- 9. 验证数据
SELECT '验证模拟数据:' AS status;
SELECT 
    'mes_work_order' AS table_name,
    COUNT(*) AS record_count
FROM mes_work_order
WHERE work_id LIKE 'MOCK-%'

UNION ALL

SELECT 
    'mes_prepackage_order' AS table_name,
    COUNT(*) AS record_count
FROM mes_prepackage_order
WHERE work_id LIKE 'MOCK-%'

UNION ALL

SELECT 
    'mes_box_code' AS table_name,
    COUNT(*) AS record_count
FROM mes_box_code
WHERE work_id LIKE 'MOCK-%'

UNION ALL

SELECT 
    'mes_package' AS table_name,
    COUNT(*) AS record_count
FROM mes_package
WHERE work_id LIKE 'MOCK-%'

UNION ALL

SELECT 
    'mes_board' AS table_name,
    COUNT(*) AS record_count
FROM mes_board
WHERE work_id LIKE 'MOCK-%';

-- 10. 详细数据展示
SELECT '工单状态:' AS info;
SELECT work_id, customer, prepackage_status, last_pull_time FROM mes_work_order WHERE work_id LIKE 'MOCK-%';

SELECT '预包装订单:' AS info;
SELECT work_id, order_num, consignor, receiver, total_set FROM mes_prepackage_order WHERE work_id LIKE 'MOCK-%';

SELECT '箱码统计:' AS info;
SELECT work_id, box_code, building, house, room FROM mes_box_code WHERE work_id LIKE 'MOCK-%';

SELECT '包件统计:' AS info;
SELECT work_id, box_code, package_no, part_count, box_type FROM mes_package WHERE work_id LIKE 'MOCK-%';

SELECT '板件统计:' AS info;
SELECT work_id, part_code, item_name, mat_name FROM mes_board WHERE work_id LIKE 'MOCK-%' LIMIT 5;

SELECT '✓ 模拟数据插入完成，可以测试查询功能了！' AS status;
