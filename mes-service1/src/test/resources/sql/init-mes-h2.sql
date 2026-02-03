CREATE TABLE IF NOT EXISTS mes_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_num VARCHAR(100) NOT NULL,
    batch_type INT NOT NULL,
    product_time TIMESTAMP,
    simple_batch_num VARCHAR(50),
    nesting_time TIMESTAMP,
    ymba014 VARCHAR(100),
    ymba016 VARCHAR(100),
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_batch_num ON mes_batch (batch_num);
CREATE INDEX IF NOT EXISTS idx_batch_type ON mes_batch (batch_type);
CREATE INDEX IF NOT EXISTS idx_is_deleted_batch ON mes_batch (is_deleted);
CREATE INDEX IF NOT EXISTS idx_product_time ON mes_batch (product_time);

CREATE TABLE IF NOT EXISTS mes_optimizing_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    batch_num VARCHAR(100) NOT NULL,
    optimizing_file_name VARCHAR(200) NOT NULL,
    station_code VARCHAR(50) NOT NULL,
    urgency TINYINT DEFAULT 0,
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_batch_file ON mes_optimizing_file (batch_num, optimizing_file_name);
CREATE INDEX IF NOT EXISTS idx_batch_id_opt ON mes_optimizing_file (batch_id);
CREATE INDEX IF NOT EXISTS idx_is_deleted_opt ON mes_optimizing_file (is_deleted);
CREATE INDEX IF NOT EXISTS idx_station_code ON mes_optimizing_file (station_code);

CREATE TABLE IF NOT EXISTS mes_work_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    optimizing_file_id BIGINT NOT NULL,
    batch_num VARCHAR(100) NOT NULL,
    work_id VARCHAR(100) NOT NULL,
    route VARCHAR(100) NOT NULL,
    route_id VARCHAR(100),
    order_type VARCHAR(50) NOT NULL,
    delivery_time TIMESTAMP,
    nesting_time TIMESTAMP,
    ymba014 VARCHAR(100),
    ymba015 VARCHAR(100),
    ymba016 VARCHAR(100),
    part0 VARCHAR(100),
    condition0 VARCHAR(500),
    part_time0 TIMESTAMP,
    zuz INT,
    prepackage_status VARCHAR(20) DEFAULT 'NOT_PULLED' NOT NULL,
    retry_count INT DEFAULT 0,
    last_pull_time TIMESTAMP,
    error_message CLOB,
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_work_id ON mes_work_order (work_id);
CREATE INDEX IF NOT EXISTS idx_batch_id_wo ON mes_work_order (batch_id);
CREATE INDEX IF NOT EXISTS idx_is_deleted_wo ON mes_work_order (is_deleted);
CREATE INDEX IF NOT EXISTS idx_last_pull_time ON mes_work_order (last_pull_time);
CREATE INDEX IF NOT EXISTS idx_optimizing_file_id ON mes_work_order (optimizing_file_id);
CREATE INDEX IF NOT EXISTS idx_prepackage_status ON mes_work_order (prepackage_status);

CREATE TABLE IF NOT EXISTS mes_prepackage_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    batch_num VARCHAR(100) NOT NULL,
    work_id VARCHAR(100) NOT NULL,
    order_num VARCHAR(100),
    consignor VARCHAR(200),
    contract_no VARCHAR(100),
    work_num VARCHAR(100),
    receiver VARCHAR(100),
    phone VARCHAR(50),
    ship_batch VARCHAR(100),
    install_address VARCHAR(500),
    customer VARCHAR(200),
    receive_region VARCHAR(200),
    space VARCHAR(100),
    pack_type VARCHAR(50),
    product_type VARCHAR(50),
    prepackage_info_size INT,
    total_set INT,
    max_package_no INT,
    production_num VARCHAR(100),
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_work_order ON mes_prepackage_order (work_order_id);
CREATE INDEX IF NOT EXISTS idx_batch_id_po ON mes_prepackage_order (batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_num_po ON mes_prepackage_order (batch_num);
CREATE INDEX IF NOT EXISTS idx_is_deleted_po ON mes_prepackage_order (is_deleted);
CREATE INDEX IF NOT EXISTS idx_order_num ON mes_prepackage_order (order_num);
CREATE INDEX IF NOT EXISTS idx_production_num ON mes_prepackage_order (production_num);
CREATE INDEX IF NOT EXISTS idx_work_id_po ON mes_prepackage_order (work_id);

CREATE TABLE IF NOT EXISTS mes_box (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prepackage_order_id BIGINT NOT NULL,
    batch_num VARCHAR(100) NOT NULL,
    work_id VARCHAR(100) NOT NULL,
    box_code VARCHAR(100) NOT NULL,
    building VARCHAR(100),
    house VARCHAR(100),
    room VARCHAR(100),
    setno INT,
    color VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_box_code ON mes_box (box_code);
CREATE INDEX IF NOT EXISTS idx_batch_num_box ON mes_box (batch_num);
CREATE INDEX IF NOT EXISTS idx_is_deleted_box ON mes_box (is_deleted);
CREATE INDEX IF NOT EXISTS idx_prepackage_order ON mes_box (prepackage_order_id);
CREATE INDEX IF NOT EXISTS idx_work_id_box ON mes_box (work_id);

CREATE TABLE IF NOT EXISTS mes_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    box_id BIGINT NOT NULL,
    batch_num VARCHAR(100) NOT NULL,
    work_id VARCHAR(100) NOT NULL,
    box_code VARCHAR(100) NOT NULL,
    package_no INT NOT NULL,
    length DECIMAL(10,2),
    width DECIMAL(10,2),
    depth DECIMAL(10,2),
    weight DECIMAL(10,2),
    part_count INT,
    box_type VARCHAR(50),
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_box_package ON mes_package (box_code, package_no);
CREATE INDEX IF NOT EXISTS idx_batch_num_pkg ON mes_package (batch_num);
CREATE INDEX IF NOT EXISTS idx_box_id ON mes_package (box_id);
CREATE INDEX IF NOT EXISTS idx_is_deleted_pkg ON mes_package (is_deleted);
CREATE INDEX IF NOT EXISTS idx_work_id_pkg ON mes_package (work_id);

CREATE TABLE IF NOT EXISTS mes_part (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_id BIGINT NOT NULL,
    box_id BIGINT NOT NULL,
    batch_num VARCHAR(100) NOT NULL,
    work_id VARCHAR(100) NOT NULL,
    part_code VARCHAR(100) NOT NULL,
    layer INT,
    piece INT,
    item_code VARCHAR(100),
    item_name VARCHAR(500),
    mat_name VARCHAR(100),
    item_length DECIMAL(10,2),
    item_width DECIMAL(10,2),
    item_depth DECIMAL(10,2),
    x_axis DECIMAL(10,2),
    y_axis DECIMAL(10,2),
    z_axis DECIMAL(10,2),
    sort_order INT,
    standard_list VARCHAR(2000),
    real_package_no VARCHAR(100),
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_part_code ON mes_part (part_code);
CREATE INDEX IF NOT EXISTS idx_batch_num_part ON mes_part (batch_num);
CREATE INDEX IF NOT EXISTS idx_box_id_part ON mes_part (box_id);
CREATE INDEX IF NOT EXISTS idx_is_deleted_part ON mes_part (is_deleted);
CREATE INDEX IF NOT EXISTS idx_package_id ON mes_part (package_id);
CREATE INDEX IF NOT EXISTS idx_work_id_part ON mes_part (work_id);

CREATE TABLE IF NOT EXISTS mes_work_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    part_code VARCHAR(100) NOT NULL,
    work_id VARCHAR(100),
    part_status VARCHAR(50) NOT NULL,
    station_code VARCHAR(50) NOT NULL,
    station_name VARCHAR(100),
    report_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_is_deleted_wr ON mes_work_report (is_deleted);
CREATE INDEX IF NOT EXISTS idx_part_code_wr ON mes_work_report (part_code);
CREATE INDEX IF NOT EXISTS idx_report_time ON mes_work_report (report_time);
CREATE INDEX IF NOT EXISTS idx_station_code_wr ON mes_work_report (station_code);
CREATE INDEX IF NOT EXISTS idx_work_id_wr ON mes_work_report (work_id);

CREATE TABLE IF NOT EXISTS mes_work_order_correction_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id BIGINT NOT NULL,
    work_id VARCHAR(100) NOT NULL,
    operator VARCHAR(100),
    operation_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    correction_reason VARCHAR(500),
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    part_count_before INT,
    part_count_after INT,
    result VARCHAR(20),
    error_message CLOB,
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_is_deleted_wocl ON mes_work_order_correction_log (is_deleted);
CREATE INDEX IF NOT EXISTS idx_operation_time ON mes_work_order_correction_log (operation_time);
CREATE INDEX IF NOT EXISTS idx_work_id_wocl ON mes_work_order_correction_log (work_id);
CREATE INDEX IF NOT EXISTS idx_work_order_id ON mes_work_order_correction_log (work_order_id);

CREATE TABLE IF NOT EXISTS mes_email_notification_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    smtp_host VARCHAR(100) NOT NULL,
    smtp_port INT NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(200) NOT NULL,
    from_address VARCHAR(100) NOT NULL,
    to_addresses VARCHAR(500) NOT NULL,
    enabled TINYINT DEFAULT 1,
    is_deleted TINYINT DEFAULT 0,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_is_deleted_email ON mes_email_notification_config (is_deleted);
