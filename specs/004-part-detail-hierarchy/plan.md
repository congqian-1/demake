# 实施计划：板件详情上层层级返回

**分支**：`004-part-detail-hierarchy` | **日期**：2026-02-04 | **规格说明**：/Users/quancong/Documents/project/tongzhou/mes/specs/004-part-detail-hierarchy/spec.md
**输入**：来自 `/specs/004-part-detail-hierarchy/spec.md` 的功能规格说明

**说明**：此模板由 `/speckit.plan` 命令填充。执行流程参见 `.specify/templates/commands/plan.md`。

## 摘要

为板件详情查询增加上层层级实体返回（批次、优化文件、工单、预包装订单、箱子、包件），并统一缺失层级的返回约定，保证一次查询可定位完整上下文。

## 技术背景

**语言/版本**：Java 8  
**主要依赖**：Spring Boot 2.7.18、MyBatis Plus、Lombok、SpringDoc OpenAPI 3  
**存储**：MySQL 8、Redis（本功能不新增缓存）  
**测试**：JUnit 5、Spring Boot Test  
**目标平台**：Linux 服务器  
**项目类型**：多模块（mes-service1）  
**性能目标**：查询接口 p95 < 200ms  
**约束条件**：不新增跨服务依赖；保持现有 API 路径不变  
**规模/范围**：单接口改造与响应结构扩展  

## 宪章检查

*门禁：必须在阶段 0 研究前通过；阶段 1 设计后需复检。*

- I 模块化服务架构：仅修改 mes-service1，未新增跨服务依赖，符合。
- II 多环境配置：无新增配置项，符合。
- III 构建一致性与依赖管理：沿用现有父 POM 与依赖管理，符合。
- IV 可观测性与运维卓越：不改变日志与健康检查，符合。
- V 测试驱动开发：新增/调整测试，符合推荐。
- VI Java 编码规范：遵守现有规范，符合。
- VII 测试文件与中间产物管理：不新增 test 目录产物，符合。

## 项目结构

### 文档（本功能）

```text
specs/004-part-detail-hierarchy/
├── plan.md              # 本文件（/speckit.plan 输出）
├── research.md          # 阶段 0 输出（/speckit.plan）
├── data-model.md        # 阶段 1 输出（/speckit.plan）
├── quickstart.md        # 阶段 1 输出（/speckit.plan）
├── contracts/           # 阶段 1 输出（/speckit.plan）
└── tasks.md             # 阶段 2 输出（/speckit.tasks；不由 /speckit.plan 创建）
```

### 源码（仓库根目录）

```text
mes-service1/
```

**结构说明**：本功能仅涉及 mes-service1 的板件详情查询接口与 DTO/mapper/service 相关逻辑。

## 复杂度跟踪

| 违规项 | 必要原因 | 放弃更简单方案的原因 |
|--------|----------|----------------------|
| 无 | 无 | 无 |

## 阶段 0：研究

- 已完成 research.md，明确缺失层级返回约定与层级范围

## 阶段 1：设计与契约

- 已完成 data-model.md、contracts/part-detail-hierarchy.openapi.yaml、quickstart.md

## 宪章复检

- 无新增违例，保持符合。
