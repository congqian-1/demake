-- Seed large hierarchical data for local mes database.
-- Adjust counts below before running.

USE mes;

SET @batch_count = 300;            -- 几百个批次
SET @files_per_batch = 20;         -- 每批次优化文件数
SET @work_orders_per_batch = 20;   -- 每批次工单数（与优化文件数匹配，1:1）
SET @boxes_per_workorder = 20;     -- 每工单箱码数（同时也对应包件数，1箱1包）
SET @parts_per_package = 20;       -- 每包件板件数
SET @seed_ts = UNIX_TIMESTAMP();

DROP PROCEDURE IF EXISTS seed_mes_hierarchy;
DELIMITER $$
CREATE PROCEDURE seed_mes_hierarchy()
BEGIN
  DECLARE batch_idx INT DEFAULT 1;
  DECLARE file_idx INT DEFAULT 1;
  DECLARE wo_idx INT DEFAULT 1;
  DECLARE box_idx INT DEFAULT 1;
  DECLARE part_idx INT DEFAULT 1;

  DECLARE batch_id BIGINT;
  DECLARE opt_id BIGINT;
  DECLARE work_order_id BIGINT;
  DECLARE prepackage_id BIGINT;
  DECLARE box_id BIGINT;
  DECLARE package_id BIGINT;

  DECLARE batch_num VARCHAR(100);
  DECLARE opt_name VARCHAR(200);
  DECLARE work_id VARCHAR(100);
  DECLARE box_code VARCHAR(100);
  DECLARE part_code VARCHAR(100);

  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_opt_files (
    seq INT PRIMARY KEY,
    id BIGINT NOT NULL
  );

  WHILE batch_idx <= @batch_count DO
    SET batch_num = CONCAT('SEED-', @seed_ts, '-', LPAD(batch_idx, 4, '0'));

    INSERT INTO mes_batch
      (batch_num, batch_type, product_time, nesting_time, simple_batch_num, ymba014, ymba016, created_by)
    VALUES
      (batch_num, 1, NOW(), NOW(), CONCAT('S-', LPAD(batch_idx, 4, '0')), 'LINE-A', 'A1', 'seed');
    SET batch_id = LAST_INSERT_ID();

    DELETE FROM tmp_opt_files;

    SET file_idx = 1;
    WHILE file_idx <= @files_per_batch DO
      SET opt_name = CONCAT('OPT-', batch_num, '-', LPAD(file_idx, 2, '0'));
      INSERT INTO mes_optimizing_file
        (batch_id, batch_num, optimizing_file_name, station_code, urgency, created_by)
      VALUES
        (batch_id, batch_num, opt_name, 'C1A001', 0, 'seed');
      SET opt_id = LAST_INSERT_ID();
      INSERT INTO tmp_opt_files (seq, id) VALUES (file_idx, opt_id);
      SET file_idx = file_idx + 1;
    END WHILE;

    SET wo_idx = 1;
    WHILE wo_idx <= @work_orders_per_batch DO
      SELECT id INTO opt_id FROM tmp_opt_files WHERE seq = ((wo_idx - 1) % @files_per_batch) + 1;
      SET work_id = CONCAT('WO-', batch_num, '-', LPAD(wo_idx, 2, '0'));

      INSERT INTO mes_work_order
        (batch_id, optimizing_file_id, batch_num, work_id, route, order_type, prepackage_status,
         retry_count, last_pull_time, created_by)
      VALUES
        (batch_id, opt_id, batch_num, work_id, 'LINE-A', 'STANDARD', 'PULLED',
         0, NOW(), 'seed');
      SET work_order_id = LAST_INSERT_ID();

      INSERT INTO mes_prepackage_order
        (work_order_id, batch_id, batch_num, work_id, order_num, consignor, work_num,
         receiver, phone, install_address, prepackage_info_size, total_set, max_package_no,
         production_num, created_by)
      VALUES
        (work_order_id, batch_id, batch_num, work_id,
         CONCAT('ORD-', work_id), 'SEED-CUSTOMER', work_id,
         'SEED-RECEIVER', '13800000000', 'SEED-ADDR',
         @boxes_per_workorder, 1, 1,
         CONCAT('PROD-', work_id), 'seed');
      SET prepackage_id = LAST_INSERT_ID();

      SET box_idx = 1;
      WHILE box_idx <= @boxes_per_workorder DO
        SET box_code = CONCAT('BOX-', work_id, '-', LPAD(box_idx, 2, '0'));
        INSERT INTO mes_box
          (prepackage_order_id, batch_num, work_id, box_code, building, house, room, setno, color, created_by)
        VALUES
          (prepackage_id, batch_num, work_id, box_code, 'A', '1-1', '101', 1, 'WHITE', 'seed');
        SET box_id = LAST_INSERT_ID();

        INSERT INTO mes_package
          (box_id, batch_num, work_id, box_code, package_no, length, width, depth, weight,
           part_count, box_type, created_by)
        VALUES
          (box_id, batch_num, work_id, box_code, 1, 1000, 600, 80, 30,
           @parts_per_package, 'STD', 'seed');
        SET package_id = LAST_INSERT_ID();

        SET part_idx = 1;
        WHILE part_idx <= @parts_per_package DO
          SET part_code = CONCAT('PART-', box_code, '-', LPAD(part_idx, 2, '0'));
          INSERT INTO mes_part
            (package_id, box_id, batch_num, work_id, part_code, layer, piece, item_code, item_name,
             mat_name, item_length, item_width, item_depth, x_axis, y_axis, z_axis, sort_order,
             standard_list, created_by)
          VALUES
            (package_id, box_id, batch_num, work_id, part_code, 1, part_idx,
             CONCAT('ITEM-', part_idx), CONCAT('板件-', part_idx), 'WHITE',
             1000, 600, 18, 10, 20, 30, part_idx,
             '[{"00041":1}]', 'seed');
          SET part_idx = part_idx + 1;
        END WHILE;

        SET box_idx = box_idx + 1;
      END WHILE;

      SET wo_idx = wo_idx + 1;
    END WHILE;

    SET batch_idx = batch_idx + 1;
  END WHILE;
END$$
DELIMITER ;

CALL seed_mes_hierarchy();

DROP PROCEDURE IF EXISTS seed_mes_hierarchy;
