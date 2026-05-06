# 批次推送并同步拉取接口说明

## 1. 接口总览

- 异步推送（保持原行为）: `POST /api/v1/third-party/batch/push`
  - 仅入库与重置拉取状态，不在接口内同步拉取。
- 同步推送（新增）: `POST /api/v1/third-party/batch/push-sync`
  - 入库后立即触发本次请求涉及工单的拉取与覆盖保存。
  - 对正在处理中的 `(batchNum, workId)` 立即返回 `PROCESSING`，不等待。
  - 处理结果由接口响应直接返回给第三方，不做回传调用。

## 2. `push-sync` 请求字段

`push-sync` 与原 `BatchPushRequest` 一致，不新增额外字段。

## 3. `push-sync` 响应字段

- 批次汇总：`batchNum`、`totalWorkOrders`、`successCount`、`failedCount`、`processingCount`、`totalBoardCount`
- 工单明细：`workOrders[]`
  - `workId`
  - `status`（`PULLED` / `NO_DATA` / `FAILED` / `PROCESSING`）
  - `boardCount`
  - `errorCode`
  - `errorMessage`
- 结束时间：`finishedAt`

## 4. 并发与幂等语义

- 业务去重：`batchNum + optimizingFileName + workId` 复用既有记录。
- 单飞行约束：同一 `(batchNum, workId)` 同时只允许一个执行者（`/push-sync` 或定时任务）实际拉取。
- `UPDATING` 期间若收到 `/push` 或人工重拉请求，不并发执行第二次，写入挂起重拉标记，当前执行完成后置回 `NOT_PULLED` 进入下一轮。

## 5. 重复数据失败中文映射

insert 路径若命中重复约束，返回明确中文失败原因：

- `uk_batch_work`：`工单重复：同一批次下工单号已存在，无法重复新增`
- `uk_batch_work_box`：`箱码重复：同一批次同一工单下箱码已存在，无法重复新增`
- `uk_batch_work_box_package`：`包件重复：同一批次同一工单同一箱码下包号已存在，无法重复新增`
- `uk_part_code`：`板件重复：板件编码已存在，无法重复新增`
- 未解析约束名：`数据唯一性冲突：存在重复记录，新增失败`

说明：update 路径不做重复前置校验，保持覆盖更新语义。
