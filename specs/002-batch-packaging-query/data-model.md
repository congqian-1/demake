# Data Model: 批次与包装层级查询

## Entities

### Batch (mes_batch)
- Fields: id (PK), batch_num (UK), batch_type, product_time
- Relationships: 1:N optimizing_files, 1:N work_orders
- Validation: batch_num required, unique

### OptimizingFile (mes_optimizing_file)
- Fields: id (PK), batch_id (FK), optimizing_file_name, station_code, urgency
- Relationships: N:1 batch, 1:N work_orders
- Validation: batch_id required

### WorkOrder (mes_work_order)
- Fields: id (PK), batch_id (FK), optimizing_file_id (FK), work_id (UK), route, order_type, prepackage_status
- Relationships: N:1 batch, N:1 optimizing_file, 1:1 prepackage_order
- Validation: work_id required, unique

### PrepackageOrder (mes_prepackage_order)
- Fields: id (PK), work_order_id (FK, UK), order_num, consignor, receiver, install_address
- Relationships: 1:1 work_order, 1:N boxes
- Validation: order_num required; work_order_id required

### Box (mes_box)
- Fields: id (PK), prepackage_order_id (FK), box_code (UK), building, house, room
- Relationships: N:1 prepackage_order, 1:N packages
- Validation: box_code required, unique

### Package (mes_package)
- Fields: id (PK), box_id (FK), package_no, length, width, depth, weight, box_type
- Relationships: N:1 box, 1:N parts
- Validation: box_id required

### Part (mes_part)
- Fields: id (PK), package_id (FK), part_code (UK), layer, piece, item_code, mat_name, item_length, item_width, item_depth, x_axis, y_axis, z_axis, sort_order, standard_list
- Relationships: N:1 package, 1:N work_reports (logical)
- Validation: part_code required, unique

### WorkReport (mes_work_report)
- Fields: id (PK), part_code, part_status, station_code, report_time
- Relationships: Logical N:1 part (by part_code)
- Validation: part_code required

## Relationships Summary

- Batch → OptimizingFile (1:N)
- Batch → WorkOrder (1:N)
- OptimizingFile → WorkOrder (1:N)
- WorkOrder → PrepackageOrder (1:1)
- PrepackageOrder → Box (1:N)
- Box → Package (1:N)
- Package → Part (1:N)
- Part → WorkReport (1:N, logical by part_code)

## State/Transitions

- No state transitions are introduced by this feature (read-only queries).
