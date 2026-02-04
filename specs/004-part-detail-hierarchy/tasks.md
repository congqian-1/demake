# 任务清单：板件详情上层层级返回

**输入**：来自 `/specs/004-part-detail-hierarchy/` 的设计文档
**前置条件**：plan.md（必需）、spec.md（用户故事必需）、research.md、data-model.md、contracts/

**测试**：未要求新增测试任务，沿用现有测试覆盖

**组织方式**：按用户故事分组，确保每个故事可独立实现与测试。

## 阶段 1：初始化（共享基础）

**目的**：项目初始化与基础结构

- [X] T001 确认实现范围与约束（/Users/quancong/Documents/project/tongzhou/mes/specs/004-part-detail-hierarchy/plan.md）

---

## 阶段 2：基础能力（阻塞前置）

**目的**：必须先完成的基础设施

**⚠️ 严重阻塞**：本阶段未完成不得开始任何用户故事

- [X] T002 盘点现有板件详情返回结构与相关 DTO（mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PartDetailResponse.java）
- [X] T003 盘点上层实体 DTO 复用范围与字段（mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/hierarchy/*.java）

**检查点**：基础信息明确，用户故事可并行开始

---

## 阶段 3：用户故事 1 - 一次查询拿到完整层级（优先级：P1）🎯 MVP

**目标**：板件详情查询一次返回批次、优化文件、工单、预包装订单、箱子、包件等上层实体

**独立测试**：调用板件详情接口，校验上层实体字段完整且归属正确

### 用户故事 1 的实现

- [X] T004 [P] [US1] 扩展板件详情响应结构以承载上层实体（mes-service1/src/main/java/com/tongzhou/mes/service1/pojo/dto/PartDetailResponse.java）
- [X] T005 [US1] 在板件详情查询中组装上层层级实体（mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PartQueryServiceImpl.java）
- [X] T006 [US1] 补充 Mapper 查询上层实体数据（mes-service1/src/main/java/com/tongzhou/mes/service1/mapper/*.java）
- [X] T007 [US1] 更新接口契约与 Swagger 注释（mes-service1/src/main/java/com/tongzhou/mes/service1/controller/PartQueryController.java）

**检查点**：用户故事 1 已可独立运行与验证

---

## 阶段 4：用户故事 2 - 异常数据可读性（优先级：P2）

**目标**：缺失或异常上层实体时可清晰呈现缺失节点，不影响整体查询

**独立测试**：对缺失层级的板件查询，验证对应上层字段为 null 且整体成功返回

### 用户故事 2 的实现

- [X] T008 [US2] 统一缺失上层实体的返回约定为 null（mes-service1/src/main/java/com/tongzhou/mes/service1/service/impl/PartQueryServiceImpl.java）
- [X] T009 [US2] 更新文档与示例响应（docs/part-and-work-report-api.md）

**检查点**：用户故事 2 已可独立运行与验证

---

## 阶段 5：打磨与跨故事优化

**目的**：影响多个用户故事的改进

- [X] T010 [P] 同步 quickstart 示例验证步骤（/Users/quancong/Documents/project/tongzhou/mes/specs/004-part-detail-hierarchy/quickstart.md）
- [X] T011 验证契约文件与实现一致（/Users/quancong/Documents/project/tongzhou/mes/specs/004-part-detail-hierarchy/contracts/part-detail-hierarchy.openapi.yaml）

---

## 依赖关系与执行顺序

### 阶段依赖

- **初始化（阶段 1）**：无依赖，可立即开始
- **基础能力（阶段 2）**：依赖阶段 1 完成，阻塞所有用户故事
- **用户故事（阶段 3+）**：依赖阶段 2 完成
- **打磨（最终阶段）**：依赖用户故事完成

### 用户故事依赖

- **用户故事 1（P1）**：阶段 2 完成后即可开始
- **用户故事 2（P2）**：阶段 3 完成后即可开始

### 并行机会

- T004 可与其他 DTO 检查并行

---

## 并行示例：用户故事 1

```bash
任务："扩展板件详情响应结构以承载上层实体"
任务："补充 Mapper 查询上层实体数据"
```

---

## 实施策略

### MVP 优先（仅用户故事 1）

1. 完成阶段 1：初始化
2. 完成阶段 2：基础能力
3. 完成阶段 3：用户故事 1
4. **停止并验证**：独立验证板件详情上层实体返回

### 增量交付

1. 完成用户故事 1 → 独立验证
2. 完成用户故事 2 → 独立验证
3. 打磨与文档同步
