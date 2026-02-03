# Implementation Plan: 批次与包装层级查询

**Branch**: `002-batch-packaging-query` | **Date**: 2026-02-02 | **Spec**: `/Users/quancong/Documents/project/tongzhou/mes/specs/002-batch-packaging-query/spec.md`
**Input**: Feature specification from `/specs/002-batch-packaging-query/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

提供两个只读查询接口：按批次号返回批次全量层级（优化文件→工单→预包装订单→箱码→包件→板件→报工），按预包装订单号返回包装层级（箱码→包件→板件→报工）。采用服务端聚合与批量查询避免 N+1，缺失层级返回空集合，标识不存在返回明确 404。

## Technical Context

**Language/Version**: Java 8+ (Spring Boot 2.7.18 via Macula Boot 5.0.15)  
**Primary Dependencies**: Spring Boot, Spring Web, MyBatis Plus, Lombok, SpringDoc OpenAPI 3  
**Storage**: MySQL 8+ (mes_batch/mes_optimizing_file/mes_work_order/mes_prepackage_order/mes_box/mes_package/mes_part/mes_work_report)  
**Testing**: Spring Boot Test (RECOMMENDED), MyBatis Plus test slices if added  
**Target Platform**: Linux server (service1)  
**Project Type**: single (Java service module)  
**Performance Goals**: 批次≤10,000板件 5s 内返回；预包装≤2,000板件 3s 内返回  
**Constraints**: 只读查询、不修改数据；层级缺失返回空集合；不存在返回 404  
**Scale/Scope**: 2 个查询端点，单次返回完整层级结构

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- Modular Service Architecture: ✅ 本需求为 `mes-service1` 内 REST 查询，不引入跨服务依赖；如需外部调用则在 `mes-api` 定义接口（当前不需要）。
- Multi-Environment Configuration: ✅ 仅新增查询逻辑，不引入新配置或硬编码环境信息。
- Build Consistency: ✅ 继续沿用 `mes-parent` 依赖管理与模块结构。
- Observability: ✅ 控制器与服务层记录必要日志，错误返回明确消息。
- Java Coding Standards: ✅ 设计遵循命名、事务、日志等规范。
- Test Artifact Management: ✅ 本阶段不生成 test/ 目录下 artifacts。

Post-Design Re-check: ✅ 设计产物（research.md、data-model.md、contracts、quickstart.md）与以上约束一致，无新增违规项。

## Project Structure

### Documentation (this feature)

```text
specs/002-batch-packaging-query/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
mes-service1/
├── src/main/java/com/tongzhou/mes/service1/
│   ├── controller/
│   ├── service/
│   ├── mapper/
│   ├── entity/
│   └── pojo/dto/
└── src/main/resources/
```

**Structure Decision**: 在 `mes-service1` 内实现查询控制器、服务、Mapper 与 DTO；不新增跨服务 API 模块。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |
