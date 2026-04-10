## Context

MES 当前通过第三方 `/infc/any` 拉取预包装数据，并将订单层字段落在 `mes_prepackage_order`。上游新增 `type` 和 `FDD8` 后，现有链路没有对应字段，数据会在拉取后丢失，前端也无法获取。

本次变更涉及跨层链路：
- 第三方响应解析（`ThirdPartyPrepackageResponseDTO`）
- 标准 DTO 映射（`PrepackageDataDTO`、`ThirdPartyPrepackageMapper`）
- 持久化层（`MesPrepackageOrder`、初始化建表 SQL、迁移 SQL、H2 建表 SQL）
- 返回预包装订单实体的查询接口组装逻辑

## Goals / Non-Goals

**Goals:**
- 将 `/infc/any` 返回的 `type`、`FDD8` 持久化到 `mes_prepackage_order`。
- 确保覆盖保存流程（`savePrePackageDataWithOverwrite`）会刷新这两个字段。
- 所有返回预包装订单数据的前端查询接口都返回这两个字段。
- 兼容大小写差异的上游字段命名。

**Non-Goals:**
- 不调整拉取调度策略和重试语义。
- 不调整批次/工单去重规则。
- 不做离线历史回填，仅通过后续覆盖拉取更新历史数据。

## Decisions

### 1) Store fields at prepackage-order level
`type` 和 `FDD8` 在当前第三方报文中属于订单层信息，因此放在 `mes_prepackage_order`，不放到箱码、包件或板件表。

Alternatives considered:
- 放到 `mes_work_order`：不采用。字段来源是预包装报文，且同工单可能有多次覆盖快照，语义不稳定。
- 放到 JSON 扩展列：不采用。会降低查询 DTO 的明确性，也不利于后续字段治理。

### 2) Keep API response field names aligned with upstream semantics
接口输出保持 `type` 和 `fdd8`，并在解析层兼容上游 `FDD8` 的命名方式。

Alternatives considered:
- Java 字段名直接用 `FDD8`：不采用。可读性较差且不符合 Java 命名习惯。
- 将 `type` 改为业务别名：不采用。会引入与第三方字段映射歧义。

### 3) Update all prepackage-order DTO projections, not just one endpoint
所有代表 `mes_prepackage_order` 的返回 DTO 都补齐这两个字段，确保层级接口和详情接口的一致性。

Alternatives considered:
- 只改 `/part/{partCode}/detail`：不采用。其余接口会不一致，前端需要做接口分支兜底。

## Risks / Trade-offs

- [Risk] 字段命名不一致（`FDD8` vs `fdd8`）导致部分报文解析为空。
  → Mitigation: 在第三方 DTO 与标准 DTO 上补充 `@JsonProperty` / `@JsonAlias` 双向兼容。

- [Risk] 历史记录在下次拉取前新字段为空。
  → Mitigation: 以 `null` 作为兼容默认值，通过定时拉取和手工重拉逐步刷新。

- [Risk] 某些 DTO 投影路径漏改，导致接口返回不完整。
  → Mitigation: 在多个查询接口上补充断言，覆盖预包装订单节点字段校验。
