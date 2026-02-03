create table if not exists mes.mes_batch
(
    id               bigint auto_increment comment '主键'
        primary key,
    batch_num        varchar(100)                       not null comment '批次号（唯一）',
    batch_type       tinyint                            not null comment '批次类型（1=衣柜柜体/2=橱柜柜体/3=衣柜门板/4=橱柜门板/5=合并条码/6=补板）',
    product_time     datetime                           not null comment '生产日期',
    nesting_time     datetime                           null comment '开料/排样时间',
    simple_batch_num varchar(50)                        null comment '简易批次号',
    ymba014          varchar(100)                       null comment '线路/区域信息',
    ymba016          varchar(20)                        null comment '属性标识',
    is_deleted       tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by       varchar(100)                       null comment '创建人',
    created_time     datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by       varchar(100)                       null comment '更新人',
    updated_time     datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_batch_num
        unique (batch_num)
)
    comment '批次表';

create index idx_batch_type
    on mes.mes_batch (batch_type);

create index idx_is_deleted
    on mes.mes_batch (is_deleted);

create index idx_product_time
    on mes.mes_batch (product_time);

create table if not exists mes.mes_box
(
    id                  bigint auto_increment comment '主键'
        primary key,
    prepackage_order_id bigint                             not null comment '预包装订单ID（外键）',
    batch_num           varchar(100)                       not null comment '批次号（冗余字段，方便按批次查询）',
    work_id             varchar(100)                       not null comment '工单号（冗余字段）',
    box_code            varchar(100)                       not null comment '箱码',
    building            varchar(100)                       null comment '楼栋',
    house               varchar(100)                       null comment '户型',
    room                varchar(100)                       null comment '房间号',
    setno               int                                null comment '第几套',
    color               varchar(50)                        null comment '颜色',
    is_deleted          tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by          varchar(100)                       null comment '创建人',
    created_time        datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by          varchar(100)                       null comment '更新人',
    updated_time        datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_box_code
        unique (box_code)
)
    comment '箱码表';

create index idx_batch_num
    on mes.mes_box (batch_num);

create index idx_is_deleted
    on mes.mes_box (is_deleted);

create index idx_prepackage_order
    on mes.mes_box (prepackage_order_id);

create index idx_work_id
    on mes.mes_box (work_id);

create table if not exists mes.mes_email_notification_config
(
    id           bigint auto_increment comment '主键'
        primary key,
    smtp_host    varchar(100)                       not null comment 'SMTP服务器地址',
    smtp_port    int                                not null comment 'SMTP端口',
    username     varchar(100)                       not null comment '发件人账号',
    password     varchar(200)                       not null comment '授权码（加密存储）',
    from_address varchar(100)                       not null comment '发件人邮箱地址',
    to_addresses varchar(500)                       not null comment '收件人地址列表（逗号分隔）',
    enabled      tinyint  default 1                 null comment '是否启用（0=禁用，1=启用）',
    is_deleted   tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by   varchar(100)                       null comment '创建人',
    created_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by   varchar(100)                       null comment '更新人',
    updated_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '邮件通知配置表';

create index idx_is_deleted
    on mes.mes_email_notification_config (is_deleted);

create table if not exists mes.mes_optimizing_file
(
    id                   bigint auto_increment comment '主键'
        primary key,
    batch_id             bigint                             not null comment '批次ID（外键）',
    batch_num            varchar(100)                       not null comment '批次号（冗余字段）',
    optimizing_file_name varchar(200)                       not null comment '优化文件名称',
    station_code         varchar(50)                        not null comment '工位编码（C1A001/C1A002/CMA001/CMA002/YMA001/YMA002）',
    urgency              tinyint  default 0                 null comment '是否加急（0=不加急/1=加急）',
    is_deleted           tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by           varchar(100)                       null comment '创建人',
    created_time         datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by           varchar(100)                       null comment '更新人',
    updated_time         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_batch_file
        unique (batch_num, optimizing_file_name)
)
    comment '优化文件表';

create index idx_batch_id
    on mes.mes_optimizing_file (batch_id);

create index idx_is_deleted
    on mes.mes_optimizing_file (is_deleted);

create index idx_station_code
    on mes.mes_optimizing_file (station_code);

create table if not exists mes.mes_package
(
    id           bigint auto_increment comment '主键'
        primary key,
    box_id       bigint                             not null comment '箱码ID（外键）',
    batch_num    varchar(100)                       not null comment '批次号（冗余字段，方便按批次查询）',
    work_id      varchar(100)                       not null comment '工单号（冗余字段）',
    box_code     varchar(100)                       not null comment '箱码（冗余字段）',
    package_no   int                                not null comment '第几包',
    length       decimal(10, 2)                     null comment '长度（单位：mm）',
    width        decimal(10, 2)                     null comment '宽度（单位：mm）',
    depth        decimal(10, 2)                     null comment '高度（单位：mm）',
    weight       decimal(10, 2)                     null comment '重量（单位：kg）',
    part_count   int                                null comment '部件数',
    box_type     varchar(50)                        null comment '纸箱类型（如"地盖"）',
    is_deleted   tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by   varchar(100)                       null comment '创建人',
    created_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by   varchar(100)                       null comment '更新人',
    updated_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_box_package
        unique (box_code, package_no)
)
    comment '包件表';

create index idx_batch_num
    on mes.mes_package (batch_num);

create index idx_box_id
    on mes.mes_package (box_id);

create index idx_is_deleted
    on mes.mes_package (is_deleted);

create index idx_work_id
    on mes.mes_package (work_id);

create table if not exists mes.mes_part
(
    id            bigint auto_increment comment '主键'
        primary key,
    package_id    bigint                             not null comment '包件ID（外键）',
    box_id        bigint                             not null comment '箱码ID（冗余字段）',
    batch_num     varchar(100)                       not null comment '批次号（冗余字段，方便按批次查询）',
    work_id       varchar(100)                       not null comment '工单号（冗余字段）',
    part_code     varchar(100)                       not null comment '部件条码（唯一）',
    layer         int                                null comment '第几层',
    piece         int                                null comment '第几片',
    item_code     varchar(100)                       null comment '板件ID',
    item_name     varchar(500)                       null comment '板件描述',
    mat_name      varchar(100)                       null comment '花色（分拣要的花色）',
    item_length   decimal(10, 2)                     null comment '板件长',
    item_width    decimal(10, 2)                     null comment '板件宽',
    item_depth    decimal(10, 2)                     null comment '板件高',
    x_axis        decimal(10, 2)                     null comment 'X轴坐标',
    y_axis        decimal(10, 2)                     null comment 'Y轴坐标',
    z_axis        decimal(10, 2)                     null comment 'Z轴坐标',
    sort_order    int                                null comment '分拣出板顺序',
    standard_list json                               null comment '标准码集合（JSON格式，如[{"00041":1,"00311":1}]）',
    real_package_no varchar(100)                     null comment '真实打包包号',
    is_deleted    tinyint  default 0                 null comment '删除标识（0=正常，1=已删除/关联板件已失效）',
    created_by    varchar(100)                       null comment '创建人',
    created_time  datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by    varchar(100)                       null comment '更新人',
    updated_time  datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_part_code
        unique (part_code)
)
    comment '板件表';

create index idx_batch_num
    on mes.mes_part (batch_num);

create index idx_box_id
    on mes.mes_part (box_id);

create index idx_is_deleted
    on mes.mes_part (is_deleted);

create index idx_package_id
    on mes.mes_part (package_id);

create index idx_work_id
    on mes.mes_part (work_id);

create table if not exists mes.mes_prepackage_order
(
    id                   bigint auto_increment comment '主键'
        primary key,
    work_order_id        bigint                             not null comment '工单ID（外键）',
    batch_id             bigint                             not null comment '批次ID（冗余字段，方便按批次统计）',
    batch_num            varchar(100)                       not null comment '批次号（冗余字段，方便查询）',
    work_id              varchar(100)                       not null comment '工单号（冗余字段）',
    order_num            varchar(100)                       null comment '订单号',
    consignor            varchar(200)                       null comment '客户名称（WMS货主）',
    contract_no          varchar(100)                       null comment '合同编号',
    work_num             varchar(100)                       null comment '工单号',
    receiver             varchar(100)                       null comment '收货人',
    phone                varchar(50)                        null comment '联系电话',
    ship_batch           varchar(100)                       null comment '出货批次号',
    install_address      varchar(500)                       null comment '安装地址',
    customer             varchar(200)                       null comment '终端客户名',
    receive_region       varchar(200)                       null comment '收货地区',
    space                varchar(100)                       null comment '产品所属空间',
    pack_type            varchar(50)                        null comment '包件类型',
    product_type         varchar(50)                        null comment '产品类型',
    prepackage_info_size int                                null comment '预包装总包数',
    total_set            int                                null comment '总套数',
    max_package_no       int                                null comment '一套内的总包数',
    production_num       varchar(100)                       null comment '生产编号',
    is_deleted           tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by           varchar(100)                       null comment '创建人',
    created_time         datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by           varchar(100)                       null comment '更新人',
    updated_time         datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_work_order
        unique (work_order_id)
)
    comment '预包装订单表';

create index idx_batch_id
    on mes.mes_prepackage_order (batch_id);

create index idx_batch_num
    on mes.mes_prepackage_order (batch_num);

create index idx_is_deleted
    on mes.mes_prepackage_order (is_deleted);

create index idx_order_num
    on mes.mes_prepackage_order (order_num);

create index idx_production_num
    on mes.mes_prepackage_order (production_num);

create index idx_work_id
    on mes.mes_prepackage_order (work_id);

create table if not exists mes.mes_work_order
(
    id                 bigint auto_increment comment '主键'
        primary key,
    batch_id           bigint                                not null comment '批次ID（外键）',
    optimizing_file_id bigint                                not null comment '优化文件ID（外键）',
    batch_num          varchar(100)                          not null comment '批次号（冗余字段，方便查询）',
    work_id            varchar(100)                          not null comment '工单号（唯一）',
    route              varchar(100)                          not null comment '线路',
    route_id           varchar(100)                          null comment '线路ID',
    order_type         varchar(50)                           not null comment '订单类型',
    delivery_time      datetime                              null comment '交付日期',
    nesting_time       datetime                              null comment '开料/排样时间',
    ymba014            varchar(100)                          null comment '线路/区域信息',
    ymba015            varchar(100)                          null comment '工位/区域信息',
    ymba016            varchar(20)                           null comment '属性标识',
    part0              varchar(100)                          null comment '部件字段',
    condition0         varchar(100)                          null comment '条件字段',
    part_time0         datetime                              null comment '部件时间字段',
    zuz                int                                  null comment '组/套标记',
    prepackage_status  varchar(20) default 'NOT_PULLED'      not null comment '预包装数据拉取状态（NOT_PULLED=未拉取/PULLING=拉取中/PULLED=已拉取/FAILED=拉取失败/NO_DATA=无预包装数据/UPDATING=更新中）',
    retry_count        int         default 0                 null comment '重试次数',
    last_pull_time     datetime                              null comment '最后拉取时间',
    error_message      text                                  null comment '错误信息（拉取失败时）',
    is_deleted         tinyint     default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by         varchar(100)                          null comment '创建人',
    created_time       datetime    default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by         varchar(100)                          null comment '更新人',
    updated_time       datetime    default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_work_id
        unique (work_id)
)
    comment '工单表';

create index idx_batch_id
    on mes.mes_work_order (batch_id);

create index idx_is_deleted
    on mes.mes_work_order (is_deleted);

create index idx_last_pull_time
    on mes.mes_work_order (last_pull_time);

create index idx_optimizing_file_id
    on mes.mes_work_order (optimizing_file_id);

create index idx_prepackage_status
    on mes.mes_work_order (prepackage_status);

create table if not exists mes.mes_work_order_correction_log
(
    id                bigint auto_increment comment '主键'
        primary key,
    work_order_id     bigint                             not null comment '工单ID',
    work_id           varchar(100)                       not null comment '工单号',
    operator          varchar(100)                       null comment '操作人',
    operation_time    datetime default CURRENT_TIMESTAMP null comment '操作时间',
    correction_reason varchar(500)                       null comment '修正原因',
    old_status        varchar(20)                        null comment '修正前状态',
    new_status        varchar(20)                        null comment '修正后状态',
    part_count_before int                                null comment '修正前板件数量',
    part_count_after  int                                null comment '修正后板件数量',
    result            varchar(20)                        null comment '修正结果（SUCCESS/FAILED）',
    error_message     text                               null comment '错误信息',
    is_deleted        tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by        varchar(100)                       null comment '创建人（系统/管理员）',
    created_time      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by        varchar(100)                       null comment '更新人',
    updated_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '工单数据修正日志表';

create index idx_is_deleted
    on mes.mes_work_order_correction_log (is_deleted);

create index idx_operation_time
    on mes.mes_work_order_correction_log (operation_time);

create index idx_work_id
    on mes.mes_work_order_correction_log (work_id);

create index idx_work_order_id
    on mes.mes_work_order_correction_log (work_order_id);

create table if not exists mes.mes_work_report
(
    id           bigint auto_increment comment '主键'
        primary key,
    part_code    varchar(100)                       not null comment '部件条码（外键关联，但不设物理外键）',
    work_id      varchar(100)                       null comment '工单号（冗余字段）',
    part_status  varchar(50)                        not null comment '板件状态值',
    station_code varchar(50)                        not null comment '工位编码',
    station_name varchar(100)                       null comment '工位名称',
    report_time  datetime default CURRENT_TIMESTAMP null comment '报工时间',
    is_deleted   tinyint  default 0                 null comment '删除标识（0=正常，1=已删除）',
    created_by   varchar(100)                       null comment '创建人（产线客户端系统ID或操作员）',
    created_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    updated_by   varchar(100)                       null comment '更新人',
    updated_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
)
    comment '报工记录表';

create index idx_is_deleted
    on mes.mes_work_report (is_deleted);

create index idx_part_code
    on mes.mes_work_report (part_code);

create index idx_report_time
    on mes.mes_work_report (report_time);

create index idx_station_code
    on mes.mes_work_report (station_code);

create index idx_work_id
    on mes.mes_work_report (work_id);
