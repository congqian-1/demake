# Research Decisions: 批次与包装层级查询

## Decision 1: 批次查询主键
- Decision: 批次查询使用 `batch_num` 作为唯一标识（路径参数）。
- Rationale: 规格明确批次号为 UK，现有业务语义稳定，适合外部调用。
- Alternatives considered: 使用批次 `id`（内部主键）但对外不友好且需泄露内部标识。

## Decision 2: 预包装查询主键
- Decision: 预包装查询使用 `order_num` 作为默认路径参数；当只有工单号时，通过可选查询参数 `workId` 进行解析（两者同时出现时优先 `order_num`）。
- Rationale: 规格允许订单号或工单号作为唯一标识；订单号直观、稳定；保留工单号入口减少调用方改造成本。
- Alternatives considered: 单独提供 `/work-orders/{workId}/prepackage` 端点（增加 API 数量与维护成本）。

## Decision 3: 层级组装策略
- Decision: 采用分层批量查询 + 内存组装，避免 N+1（批次→优化文件→工单→预包装→箱码→包件→板件→报工）。
- Rationale: 性能目标要求 5s/3s 内返回；分层 IN 查询可控且实现简单。
- Alternatives considered: 全量多表 JOIN（SQL 复杂、返回行爆炸）、多轮逐条查询（N+1 风险）。

## Decision 4: 报工关联方式
- Decision: 按 `part_code` 批量查询 `mes_work_report`，按板件码挂载到板件节点，缺失返回空数组。
- Rationale: 规格要求仅匹配板件码；报工表无物理外键，批量 IN 查询更稳定。
- Alternatives considered: 逐板件查询（性能差）、通过工单维度关联（不准确）。

## Decision 5: API 返回结构与空层级
- Decision: 返回完整层级结构，缺失层级固定为 `[]` 空集合；标识不存在返回 404 与明确消息。
- Rationale: 满足 FR-006/FR-007，便于前端与调用方稳定解析。
- Alternatives considered: 省略空层级（破坏契约、前端处理复杂）。
