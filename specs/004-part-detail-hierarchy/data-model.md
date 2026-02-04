# 数据模型

## 实体与关系

- 批次（Batch） 1 - N 优化文件（OptimizingFile）
- 优化文件 1 - N 工单（WorkOrder）
- 工单 1 - 1 预包装订单（PrepackageOrder）
- 预包装订单 1 - N 箱子（Box）
- 箱子 1 - N 包件（Package）
- 包件 1 - N 板件（Part）

## 字段要点（面向返回）

- Batch：id, batchNum, batchType, productTime
- OptimizingFile：id, batchId, optimizingFileName, stationCode, urgency
- WorkOrder：id, batchId, optimizingFileId, workId, route, orderType, prepackageStatus
- PrepackageOrder：id, workOrderId, orderNum, consignor, receiver, installAddress
- Box：id, prepackageOrderId, boxCode, building, house, room
- Package：id, boxId, packageNo, length, width, depth, weight, boxType
- Part：现有 PartDetailResponse 字段

## 约束与规则

- 返回中所有上层实体与板件必须存在真实归属关系
- 缺失层级固定返回为 null
