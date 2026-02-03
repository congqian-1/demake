<!--
=============================================================================
同步影响报告 - 宪章 v1.2.1
=============================================================================

版本变化：1.2.0 → 1.2.1（PATCH）
更新日期：2026-02-02

修改的原则（标题翻译）：
- I. Modular Service Architecture → I. 模块化服务架构
- II. Multi-Environment Configuration → II. 多环境配置
- III. Build Consistency & Dependency Management → III. 构建一致性与依赖管理
- IV. Observability & Operational Excellence → IV. 可观测性与运维卓越
- V. Test-Driven Development (RECOMMENDED) → V. 测试驱动开发（推荐）
- VI. Java Coding Standards & Best Practices → VI. Java 编码规范与最佳实践
- VII. Test File & Intermediate Artifact Management → VII. 测试文件与中间产物管理

新增章节：无

移除章节：无

模板更新状态：
✅ .specify/templates/plan-template.md - 已对齐
✅ .specify/templates/spec-template.md - 已对齐
✅ .specify/templates/tasks-template.md - 已对齐
✅ .specify/templates/checklist-template.md - 已对齐
✅ .specify/templates/agent-file-template.md - 已对齐

后续 TODO：无

版本变更理由（PATCH）：
- 仅进行中文化与措辞澄清，不改变治理规则或原则本身
- 未新增/移除原则或章节，属于非语义性修订

=============================================================================
-->

# Tongzhou MES 宪章

## 核心原则

### I. 模块化服务架构

每个服务必须可独立部署、测试和扩展。服务按业务域组织（gateway、admin-bff、basic、service1、thirdparty、openapi），并保持清晰的 API 边界。API 模块（`mes-api/*-api`）提供契约优先接口，由服务实现消费。

**理由**：微服务架构支持团队独立迭代、故障隔离和弹性扩展。契约优先的 API 设计确保向后兼容和边界清晰。

**规则**：
- 每个对外暴露接口的服务，必须在 `mes-api/` 下有对应的 API 模块
- 服务之间不得直接依赖其他服务的实现，只能依赖 API 模块
- 服务间通信必须通过已定义的 API 契约（Feign 客户端）
- 共享配置必须集中在 `mes-parent` POM 中

### II. 多环境配置

系统必须支持五套部署环境，并通过 profile 进行配置切换：local（默认）、dev、stg、pet、prd。环境差异配置通过 Nacos 配置中心外置化管理。

**理由**：制造系统需要多阶段验证后才能进入生产。使用 profile 可避免环境特定代码变更，降低部署风险。

**规则**：
- 所有服务必须在 `bootstrap.yml` 中声明 `spring.profiles.active=@profile.active@`
- 敏感凭据（Nacos 密码、数据库密码）必须通过启动参数注入，禁止提交到源码
- local profile 必须尽量减少外部依赖（本地 Nacos、MySQL、Redis）
- 环境差异配置必须在服务 README 中有明确说明

### III. 构建一致性与依赖管理

Maven 多模块构建必须保证可复现构建与一致依赖版本。父 POM（`mes-parent`）使用 Macula Boot BOM 统一依赖版本。

**理由**：微服务需要保持版本兼容，避免运行时类路径冲突。依赖集中管理确保所有模块使用经过验证的兼容版本。

**规则**：
- 所有服务模块必须继承 `mes-parent`（版本 1.0.0-SNAPSHOT）
- API 模块必须设置 `maven.install.skip=false` 以发布到本地/远程仓库
- 服务模块必须设置 `maven.install.skip=true`（无需发布可执行 JAR）
- 构建顺序必须为：`mes-parent` → `mes-api` → 服务模块
- 构建服务前必须先安装其 API 依赖：`mvn install -pl mes-api/{service}-api -am -Dmaven.install.skip=false`

### IV. 可观测性与运维卓越

所有服务必须提供完善的日志、指标和健康检查。结构化日志与关联 ID 用于分布式追踪。

**理由**：制造系统需要 7x24 稳定运行。可观测性是快速定位问题和提升系统可靠性的前提。

**规则**：
- 所有服务必须使用集中式日志并可配置级别（默认：root 为 INFO，`com.tongzhou.mes` 为 DEBUG）
- 日志文件必须写入 `${user.home}/logs/${spring.application.name}/`
- 所有服务必须暴露 Spring Boot Actuator 健康检查端点
- 服务必须启用 Swagger/OpenAPI 文档（`springdoc.swagger-ui.enabled=true`）
- 服务必须实现统一异常处理并通过 REST API 返回有意义的错误信息

### V. 测试驱动开发（推荐）

对关键业务逻辑与 API 契约的测试是推荐实践。编写测试时必须遵循红-绿-重构循环。

**理由**：MES 涉及关键生产数据。虽然不强制覆盖所有功能，但核心逻辑与服务契约的测试能显著降低生产缺陷。

**规则**：
- Feign 客户端接口的契约测试：推荐
- 数据库操作与外部服务调用的集成测试：推荐
- 服务层复杂业务逻辑的单元测试：推荐
- 编写测试时必须遵循：先写测试 → 验证失败 → 实现功能 → 验证通过
- 测试必须使用 Spring Boot Test 框架与合适的测试切片（`@WebMvcTest`、`@DataJpaTest`）
- 所有构建必须支持 `-DskipTests` 以便快速迭代

### VI. Java 编码规范与最佳实践

所有 Java 代码必须遵循行业规范与最佳实践，以保证可维护性、可靠性与生产性能。

**理由**：统一编码规范可降低缺陷、提升可读性，并保证团队协作效率。遵循《阿里巴巴 Java 开发手册》和 Spring 官方实践能确保使用成熟、可靠的模式。

**规则**：

#### 6.1 阿里巴巴 Java 开发手册（嵩山版 2023）

- **命名规范（必须）**：
  - 类名必须使用 UpperCamelCase（例如：`OrderService`、`UserDTO`）
  - 方法/变量名必须使用 lowerCamelCase（例如：`getUserById`、`totalAmount`）
  - 常量必须使用 UPPER_SNAKE_CASE（例如：`MAX_RETRY_COUNT`、`DEFAULT_TIMEOUT`）
  - 包名必须小写（例如：`com.tongzhou.mes.service`）
  - 抽象类必须以 `Abstract` 或 `Base` 开头
  - 异常类必须以 `Exception` 结尾

- **代码格式（必须）**：
  - 缩进：4 个空格（禁止 Tab）
  - 单行长度不得超过 120 个字符
  - 方法长度不得超过 80 行
  - 类长度不得超过 500 行
  - 所有控制语句必须使用 `{}`，即使只有一行

- **面向对象（必须）**：
  - 除非真正无状态，否则避免使用静态方法
  - 构造函数不得调用可被覆盖的方法
  - 重写 `equals()` 必须同步重写 `hashCode()`
  - 所有重写方法必须使用 `@Override`
  - 优先使用组合而非继承

- **集合使用（必须）**：
  - 默认使用 `ArrayList`，只有在头部频繁插入/删除时使用 `LinkedList`
  - 默认使用 `HashMap`，若已知容量应指定初始容量：`new HashMap<>(expectedSize / 0.75 + 1)`
  - 禁止在遍历时修改集合（使用 Iterator.remove() 或先收集后修改）
  - 空集合返回使用 `Collections.emptyList()`，避免 `new ArrayList<>()`

- **并发编程（必须）**：
  - 线程池必须使用 `java.util.concurrent.Executors` 或 Spring 的 `@Async`
  - 禁止直接 `new Thread()`
  - 使用 `ThreadLocal` 必须在 `finally` 中调用 `remove()`
  - 优先使用 `ConcurrentHashMap`，避免 `Hashtable` 或 `Collections.synchronizedMap()`
  - 双重检查锁必须使用 `volatile`

- **异常处理（必须）**：
  - 禁止捕获 `Throwable` 或 `Error`，只捕获 `Exception` 及其子类
  - 禁止用异常做流程控制
  - 事务方法不得吞异常，必须抛出或触发回滚
  - 禁止空 catch；至少记录日志
  - 使用具体异常，避免泛化的 `RuntimeException`

#### 6.2 Spring / Spring Boot 最佳实践

- **依赖注入（必须）**：
  - 优先构造器注入，避免字段注入
  - 构造器注入的依赖必须标记为 `final`
  - 避免在字段上使用 `@Autowired`，改用构造器参数
  - 使用 `@RequiredArgsConstructor`（Lombok）减少样板代码

- **组件扫描（必须）**：
  - `@SpringBootApplication` 必须放在根包（例如：`com.tongzhou.mes.service1`）
  - 避免使用过宽的 `@ComponentScan` base package
  - 使用 `@Lazy` 避免循环依赖

- **配置管理（必须）**：
  - 使用 `@ConfigurationProperties` 替代多个 `@Value`
  - 所有环境差异配置必须外置到 `application.yml` 或 Nacos
  - 禁止在代码中硬编码 IP、端口、凭据或业务阈值
  - 使用 Spring profile 管理环境专属 Bean

- **REST API 设计（必须）**：
  - 遵循 REST 语义：GET（查询）、POST（创建）、PUT（全量更新）、PATCH（部分更新）、DELETE（删除）
  - 资源路径使用复数：`/api/v1/orders`，禁止 `/api/v1/order`
  - 状态码：200（OK）、201（Created）、204（No Content）、400（Bad Request）、401（Unauthorized）、404（Not Found）、500（Server Error）
  - 返回统一响应封装：`Result<T>`，包含 `code`、`message`、`data`
  - 使用 `@Valid` + Bean Validation（`@NotNull`、`@Size` 等）验证输入

- **事务管理（必须）**：
  - `@Transactional` 只允许标在 service 层，禁止标在 controller 或 repository
  - 必须设置 `rollbackFor = Exception.class` 以回滚受检异常
  - 避免长事务，尽量缩短事务时长
  - 禁止在事务内执行远程调用（HTTP、RPC）
  - 查询方法使用 `@Transactional(readOnly = true)`

#### 6.3 通用工程规范

- **日志规范（必须）**：
  - 使用 SLF4J API，禁止直接依赖 Logback/Log4j：`private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
  - 日志级别：ERROR（系统失败）、WARN（可恢复问题）、INFO（关键业务事件）、DEBUG（诊断细节）
  - 禁止记录敏感信息（密码、token、证件号、手机号）
  - 使用参数化日志：`log.info("User {} logged in", userId)`，禁止字符串拼接
  - 日志必须包含关联 ID（trace ID）用于分布式追踪

- **资源管理（必须）**：
  - `AutoCloseable` 必须使用 try-with-resources
  - 如果无法使用 try-with-resources，必须在 `finally` 中关闭
  - HTTP 客户端、数据库、Redis 必须设置合理超时
  - 使用连接池（JDBC 使用 Druid，Redis 使用 Lettuce）

- **性能考虑（必须）**：
  - 避免 N+1 查询；使用 JOIN 或批量查询
  - 大结果集必须分页；禁止无 LIMIT 的 `SELECT *`
  - 频繁读取且低变更数据应缓存到 Redis
  - 耗时操作使用异步处理（`@Async`、MQ）
  - 优化 SQL：为 WHERE/ORDER BY 列建立索引，禁止 `SELECT *`

#### 6.4 MySQL 最佳实践

- **表结构设计（必须）**：
  - 主键：使用 `BIGINT AUTO_INCREMENT` 或分布式 ID（Snowflake、UUID）
  - 尽量避免 `NULL`；使用默认值（`DEFAULT ''`、`DEFAULT 0`）
  - 合理选择类型：布尔用 `TINYINT`，金额用 `DECIMAL`，字符串用合适长度的 `VARCHAR(N)`
  - 为外键与高频查询字段建立索引
  - 必须包含 `created_time`、`updated_time`、`is_deleted`（软删除）字段

- **查询优化（必须）**：
  - 动态查询使用 MyBatis Plus Wrapper，禁止字符串拼接
  - 使用分页（PageHelper 或 MyBatis Plus `Page<T>`）
  - 禁止 `SELECT *`，必须指定列
  - 使用 `EXPLAIN` 分析执行计划
  - WHERE / JOIN / ORDER BY 列必须建立索引

- **事务隔离（必须）**：
  - 默认隔离级别：`READ_COMMITTED`（避免脏读）
  - 仅在必要时使用 `REPEATABLE_READ`（MySQL 默认）
  - 除非必须，禁止 `SERIALIZABLE`（性能损耗大）
  - 高并发更新使用乐观锁（version 字段）

#### 6.5 Redis 最佳实践

- **Key 设计（必须）**：
  - 采用命名空间前缀：`mes:service1:user:{userId}`（层级结构）
  - 所有缓存 Key 必须设置 TTL
  - Key 长度应控制在 50 字符以内，保持可读
  - 分隔符统一使用 `:`

- **数据结构（必须）**：
  - STRING：简单 KV，缓存对象（JSON 序列化）
  - HASH：对象属性，避免存储大对象
  - LIST：消息队列、时间线（大列表谨慎使用）
  - SET：唯一集合、标签、关注
  - ZSET：排行、时间序列

- **缓存策略（必须）**：
  - Cache-Aside：先读缓存 → 未命中 → 读 DB → 写缓存
  - 防止缓存穿透：空值短 TTL 或布隆过滤器
  - 防止缓存击穿：热点 Key 使用分布式锁（Redisson）
  - 防止缓存雪崩：TTL 加随机偏移或多级缓存

- **运维（必须）**：
  - 批量操作使用 pipeline 减少网络往返
  - 遍历 Key 使用 `SCAN`，禁止 `KEYS`
  - 避免存储大值（>1MB），必要时拆分
  - 监控慢查询（`SLOWLOG`），优化 >10ms 命令

#### 6.6 消息队列（MQ）最佳实践

- **消息设计（必须）**：
  - 消息体必须包含 message ID、时间戳与业务上下文
  - 序列化使用 JSON（便于调试与可读性）
  - 消息大小建议 < 1MB
  - 消费者必须实现幂等

- **可靠性保证（必须）**：
  - 生产者启用事务消息或确认回调
  - 消费者必须手动 ACK，禁止自动确认
  - 必须实现指数退避重试
  - 超过最大重试必须进入死信队列（DLQ）

#### 6.7 高并发、高可用与数据一致性

- **并发模式（必须）**：
  - 关键区段使用分布式锁（Redisson、Zookeeper）
  - 必须实现限流（Sentinel、Guava RateLimiter）以避免过载
  - 线程池必须使用有界队列，防止资源耗尽
  - 禁止在事件循环或响应式流中执行阻塞操作

- **高可用（必须）**：
  - 依赖失败时必须提供降级响应
  - 断路器（Sentinel、Hystrix）必须快速失败，避免级联故障
  - 必须提供依赖健康检查（`/actuator/health`）
  - 服务发现必须使用 Nacos

- **数据一致性（必须）**：
  - 分布式事务使用 Seata（如启用）保证 ACID
  - 跨服务操作使用消息队列实现最终一致性
  - 必须实现补偿事务（Saga）回滚逻辑
  - 高并发更新使用乐观锁防止覆盖
  - 使用唯一请求 ID 实现幂等

- **监控与告警（必须）**：
  - 通过 Actuator 暴露指标：`/actuator/metrics`、`/actuator/prometheus`
  - 定义 SLO：响应时间（p95 < 200ms）、可用性（> 99.9%）、错误率（< 0.1%）
  - 对异常指标告警：CPU > 80%、内存 > 85%、错误率突增、慢查询
  - 使用分布式追踪（SkyWalking、Zipkin）进行跨服务排障

### VII. 测试文件与中间产物管理

开发过程中生成的测试文件、Mock 服务、测试脚本与中间产物必须与生产代码隔离，并按功能分支组织。这些中间产物不得提交到版本控制。

**理由**：开发与测试会产生大量中间文件（Mock API、测试脚本、临时文档），它们支撑开发过程但不属于交付物。隔离中间产物可避免仓库污染，保持 Git 历史整洁，且便于多分支并行开发。

**规则**：

#### 7.1 测试目录结构（必须）

- 所有测试相关中间产物必须放在仓库根目录 `test/`
- 按分支组织测试产物，结构如下：
  ```
  test/
  ├── {branch-name}/              # 例如：001-mes-integration、002-user-auth
  │   ├── mock/                   # Mock 服务（API 服务器、桩）
  │   ├── scripts/                # 测试脚本（bash、shell）
  │   ├── data/                   # 测试数据（JSON、CSV、SQL）
  │   └── docs/                   # 测试文档（指南、报告）
  └── shared/                     # 跨分支共享的测试工具
      ├── utils/                  # 可复用的测试辅助脚本
      └── fixtures/               # 通用测试夹具
  ```
- 分支目录名必须与实际 Git 分支名一致（例如分支 `001-mes-integration` 对应 `test/001-mes-integration/`）
- 切换分支时，测试产物必须保持隔离
- 跨分支可复用的工具可放在 `test/shared/`

#### 7.2 Mock 服务（必须）

- Mock API 服务必须放在 `test/{branch-name}/mock/`
- Mock 文件名必须表明用途：`mock-{service-name}-server.{ext}`
  - 示例：`mock-third-party-api-server.js`、`mock-auth-server.py`
- Mock 服务必须提供健康检查端点（例如 `/health`、`/status`）
- Mock 服务必须记录请求日志以便排查：`{service-name}.log`
- Mock 数据必须真实但不得包含客户真实数据或凭据
- Mock 服务推荐使用非标准端口（9000+）避免冲突

#### 7.3 测试脚本（必须）

- 测试脚本必须放在 `test/{branch-name}/scripts/`
- 脚本名必须使用 kebab-case 且含义明确：`test-{feature}-{type}.sh`
  - 示例：`test-batch-push-flow.sh`、`test-integration-full.sh`、`verify-all-tables.sh`
- 脚本头部必须包含使用说明
- 脚本必须返回正确的退出码：0（成功）、非 0（失败）
- 脚本必须在退出或失败时清理临时资源（进程、文件）
- 数据库初始化/清理脚本必须可重复执行（幂等）

#### 7.4 测试数据（必须）

- 测试数据文件必须放在 `test/{branch-name}/data/`
- 文件名必须表明内容：`{entity}-{purpose}.{format}`
  - 示例：`batch-samples.json`、`work-orders-mock.csv`、`schema-init.sql`
- 测试数据可以版本控制（与日志/输出不同）
- 禁止包含敏感数据（密码、token）；必须使用占位符（如 `<REPLACE_ME>`）
- 大型二进制测试文件（图片、PDF）应由程序生成而非提交

#### 7.5 测试文档（必须）

- 测试文档必须放在 `test/{branch-name}/docs/`
- 文档类型：
  - **测试指南**：如何运行测试（例如 `TESTING-GUIDE.md`）
  - **API 参考**：Mock API 文档（例如 `MOCK-API-REFERENCE.md`）
  - **测试报告**：结果摘要、覆盖率报告（例如 `TEST-REPORT-2026-01-25.md`）
  - **环境说明**：测试环境准备（例如 `SETUP.md`）
- 文档文件名必须使用 UPPER-KEBAB-CASE：`{PURPOSE}.md`
- 测试文档推荐包含：
  - 前置条件（依赖、需要的服务）
  - 执行步骤
  - 预期结果与验证标准
  - 常见问题排查

#### 7.6 Git 忽略（必须）

- `test/` 目录必须在 `.gitignore` 中整体忽略
- `.gitignore` 必须包含：
  ```gitignore
  # 测试文件与中间产物
  test/
  ```
- 例外：若 `test/shared/` 中的某些工具对团队有价值，必须经过团队明确批准并修改宪章后才可提交
- 测试中间产物不得出现在 PR 或提交历史中
- 校验方式：`git status` 不得出现 `test/` 相关变更

#### 7.7 生命周期管理（必须）

- 功能分支创建时应同步创建对应 `test/{branch-name}/`
- 功能分支合并关闭后，可删除对应测试产物
- 若测试产物可用于回归，应在分支关闭前移动至 `test/shared/`
- 长期分支（如 `develop`、`staging`）可保留长期测试目录
- 必须在 `test/{branch-name}/docs/PROCESSES.md` 中记录长期运行的 Mock 服务或后台进程

#### 7.8 CI/CD 集成（推荐）

- CI 流水线推荐支持执行 `test/{branch-name}/scripts/` 中的测试
- CI 不得因缺少 `test/` 目录而失败（本地开发专用）
- CI 可以生成测试报告并放在 `test/{branch-name}/docs/`（不提交）
- CI 推荐校验测试产物未进入提交（建议使用 pre-commit hook）

## 开发标准

### 技术栈

- **框架**：Spring Boot 2.7.18（基于 Macula Boot 5.0.15）
- **语言**：Java（版本由 Macula Boot parent 决定）
- **服务发现**：Nacos
- **API 网关**：Spring Cloud Gateway
- **认证**：OAuth2 Resource Server（JWT）
- **数据库**：MySQL 8+（Druid 连接池）
- **ORM**：MyBatis Plus
- **缓存**：Redis
- **API 文档**：SpringDoc OpenAPI 3
- **Feign 客户端**：OpenFeign + OkHttp
- **前端**：Vue.js（mes-admin）

### 代码组织

服务模块采用统一包结构：

```
com.tongzhou.mes.{service}/
├── {Service}Application.java      # @SpringBootApplication 入口
├── controller/                     # REST 接口
├── service/                        # 业务逻辑
├── mapper/                         # MyBatis Plus 数据访问
├── entity/                         # 实体
├── dto/                           # DTO
├── converter/                     # MapStruct 转换
└── config/                        # 服务配置
```

API 模块仅暴露接口与 DTO：

```
com.tongzhou.mes.{service}.api/
├── feign/                         # Feign 客户端接口
├── dto/                           # 共享 DTO
└── constants/                     # 共享常量
```

### 安全要求

- 所有服务必须通过 `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` 校验 JWT
- 公共接口必须在 `macula.security.ignore-urls` 中显式白名单
- 密码不得提交到版本控制
- 数据库凭据必须外置到环境配置
- 非本地环境必须启用 HTTPS

## 质量门禁

### 功能开发前

- [ ] 宪章检查：确认功能符合模块化架构原则
- [ ] API 契约定义：涉及服务间通信时，必须先设计 API 模块
- [ ] 环境依赖确认：明确需要的环境（Nacos、MySQL、Redis）
- [ ] 测试产物位置：需要测试产物时，创建 `test/{branch-name}/` 目录结构

### 合并到主分支前

- [ ] 构建通过：所有相关模块 `mvn clean install` 成功
- [ ] 代码规范合规：Java 代码符合原则 VI（编码规范）
  - [ ] 命名规范检查（类、方法、变量、常量）
  - [ ] 无静态滥用，OOP 设计合理
  - [ ] 异常处理符合规范（无空 catch、事务回滚）
  - [ ] 日志使用参数化格式且不包含敏感信息
  - [ ] 资源管理使用 try-with-resources
  - [ ] SQL 优化（避免 N+1、建索引、分页）
  - [ ] Redis Key 命名与 TTL 合规
  - [ ] 并发代码使用线程池与正确锁机制
- [ ] 测试产物隔离：所有测试产物位于 `test/{branch-name}/`（原则 VII）
  - [ ] `src/` 或生产目录中无测试产物
  - [ ] `git status` 无 `test/` 变更（已忽略）
  - [ ] Mock 服务有文档且端口不冲突
  - [ ] 测试脚本可执行且包含使用说明
- [ ] 代码评审：至少一次同行评审，聚焦 API 契约、异常处理、配置外置
- [ ] 手工验证：本地环境完成验证
- [ ] 文档更新：README 与 API 文档同步
- [ ] 无敏感信息提交：扫描密码、token 等

### 生产部署前

- [ ] 多环境验证：dev → stg → pet 验证完成
- [ ] 性能基线：启动时间、内存占用符合要求
- [ ] 回滚预案：保留上一个版本可回滚
- [ ] 运维交接：部署步骤、配置变更、告警规则已记录

## 治理

### 修订流程

1. 提交修改提案（必须包含理由与影响分析）
2. MAJOR 或 MINOR 版本变更需团队批准
3. 按语义化版本更新宪章版本号
4. 同步影响到相关模板与文档
5. 向团队发布变更通知

### 宪章版本规则

- **MAJOR**（X.0.0）：不兼容变更（如移除原则、改变服务架构模型）
- **MINOR**（X.Y.0）：新增原则或章节
- **PATCH**（X.Y.Z）：澄清、措辞优化、错别字修正

### 合规要求

- 所有代码评审必须验证核心原则（I-VII）
- Java 代码必须符合原则 VI（编码规范）：阿里规范、Spring 最佳实践、中间件规范
- 测试产物必须符合原则 VII（测试文件管理）：`test/` 隔离、分支组织、Git 忽略
- 任何违反“必须”的情况必须在代码或 PR 中明确说明理由
- 复杂度违规（如绕过 API 模块）必须在 `plan.md` 的复杂度跟踪表中记录
- 编码规范违规（如使用 `new Thread()`、缺少事务）必须在合并前修复
- 测试产物泄漏（提交 `test/`、在 `src/` 放 Mock）必须在评审中拒绝
- 本宪章优先于未记录的约定俗成

### 执行

团队负责人在以下环节审核宪章合规：
- 架构设计评审（实现前）
- PR 评审（实现中）
- 复盘（交付后）

重复违规必须二选一：
1. 修订宪章以反映合理实践（若违规有合理性），或
2. 采取纠正措施恢复合规（若违规有害）

---

**版本**：1.2.1 | **批准生效**：2026-01-21 | **最近修订**：2026-02-02
