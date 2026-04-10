## ADDED Requirements

### Requirement: 预包装拉取 SHALL 持久化 type 与 FDD8
系统 SHALL 从第三方 `/infc/any` 预包装报文接收 `type` 与 `FDD8`，并持久化到 `mes_prepackage_order`。

#### Scenario: 首次拉取写入 type 与 FDD8
- **WHEN** 定时拉取或手工修正拉取保存某工单预包装数据，且报文包含 `type` 与 `FDD8`
- **THEN** 新建的预包装订单记录 MUST 在独立字段中保存这两个值
- **THEN** 落库值 MUST 与上游报文值一致

#### Scenario: 覆盖保存刷新 type 与 FDD8
- **WHEN** 覆盖流程替换同一工单记录的预包装层级数据
- **THEN** 重建后的预包装订单 MUST 写入最新 `type` 与 `FDD8`
- **THEN** 覆盖完成后旧值 MUST NOT 残留

### Requirement: 预包装订单查询响应 SHALL 返回 type 与 FDD8
凡是返回预包装订单实体信息的接口响应 SHALL 包含 `type` 与 `FDD8`（或规范化字段 `fdd8`）。

#### Scenario: 工单批次层级接口返回新增字段
- **WHEN** 客户端调用 `/api/v1/production/part/{partCode}/work-order-and-batch`
- **THEN** 响应中的预包装订单节点 MUST 包含 `type` 与 `fdd8`

#### Scenario: 包件层级接口返回新增字段
- **WHEN** 客户端调用 `/api/v1/production/part/{partCode}/package`
- **THEN** 响应中的预包装订单节点 MUST 包含 `type` 与 `fdd8`

#### Scenario: 板件详情接口返回新增字段
- **WHEN** 客户端调用 `/api/v1/production/part/{partCode}/detail`
- **THEN** 返回中的预包装订单摘要 MUST 包含 `type` 与 `fdd8`
