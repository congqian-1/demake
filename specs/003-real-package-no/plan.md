# 实施计划：板件真实包号

**分支**：`003-real-package-no` | **日期**：2026-02-03 | **规格说明**：[/Users/quancong/Documents/project/tongzhou/mes/specs/003-real-package-no/spec.md]
**输入**：来自 `/specs/003-real-package-no/spec.md` 的功能规格说明

**说明**：此模板由 `/speckit.plan` 命令填充。执行流程参见 `.specify/templates/commands/plan.md`。

## 摘要

新增板件真实包号字段，报工提交时写入该字段，并在批次层级、包装层级与板件详情查询中返回。范围限定在 mes-service1 的板件数据与查询/报工接口，不改变其他业务流程。

## 技术背景

**语言/版本**：Java 8  
**主要依赖**：Spring Boot 2.7.18、MyBatis Plus、Lombok  
**存储**：MySQL 8  
**测试**：JUnit 5、Spring Boot Test  
**目标平台**：Linux 服务器  
**项目类型**：多模块（mes-parent/mes-api/mes-service1 等）  
**性能目标**：查询链路在正常负载下保持稳定响应（与现有服务一致）  
**约束条件**：不引入新服务；遵循现有返回结构与异常处理规范  
**规模/范围**：单服务内字段与接口扩展（4 个接口）

## 宪章检查

*门禁：必须在阶段 0 研究前通过；阶段 1 设计后需复检。*

- 规则 I（模块化服务架构）：本需求仅变更 mes-service1 及其接口契约，不跨服务实现依赖，符合。
- 规则 II（多环境配置）：不引入新的环境变量与凭据，符合。
- 规则 III（构建一致性）：不新增依赖，保持既有 Maven 结构，符合。
- 规则 IV（可观测性）：沿用现有日志与异常处理，符合。
- 规则 V（测试驱动开发推荐）：新增接口字段与请求参数将补充/调整测试覆盖，符合。
- 规则 VI（Java 编码规范）：使用现有实体/DTO 结构与命名规范，符合。
- 规则 VII（测试产物管理）：本次不新增测试产物目录，符合。

**复检（阶段 1 设计后）**：通过

## 项目结构

### 文档（本功能）

```text
specs/003-real-package-no/
├── plan.md              # 本文件（/speckit.plan 输出）
├── research.md          # 阶段 0 输出（/speckit.plan）
├── data-model.md        # 阶段 1 输出（/speckit.plan）
├── quickstart.md        # 阶段 1 输出（/speckit.plan）
├── contracts/           # 阶段 1 输出（/speckit.plan）
└── tasks.md             # 阶段 2 输出（/speckit.tasks；不由 /speckit.plan 创建）
```

### 源码（仓库根目录）

```text
mes-parent/
mes-api/
mes-service1/
mes-gateway/
mes-admin-bff/
mes-basic/
mes-openapi/
mes-thirdparty/

test/
```

**结构说明**：本功能涉及 mes-service1 的实体、DTO、接口与测试；无跨服务改动。

## 复杂度跟踪

> **仅在宪章检查存在违规且必须说明时填写**

| 违规项 | 必要原因 | 放弃更简单方案的原因 |
|--------|----------|----------------------|
| 无 | 无 | 无 |
