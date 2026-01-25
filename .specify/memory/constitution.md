<!--
=============================================================================
SYNC IMPACT REPORT - Constitution v1.2.0
=============================================================================

Version Change: 1.1.0 → 1.2.0 (MINOR)
Updated: 2026-01-25

Modified Principles:
- None (existing principles unchanged)

Added Sections:
- VII. Test File & Intermediate Artifact Management (NEW PRINCIPLE)
  - Test file organization and isolation
  - Branch-specific test artifact management
  - Git exclusion requirements for intermediate files
  - Mock service and test script conventions
  - Documentation artifact handling

Removed Sections:
- None

Templates Status:
✅ plan-template.md - Aligned (Project Structure supports test/ directory)
✅ spec-template.md - Aligned (User scenarios support independent testing)
✅ tasks-template.md - Aligned (Test tasks can reference test/ directory structure)
✅ .gitignore updated - Added test/ directory exclusion

Follow-up TODOs: None

Rationale for MINOR bump:
- New principle (VII) added for test file and intermediate artifact management
- Establishes mandatory conventions for organizing test-related files
- Existing principles and rules remain unchanged (backward compatible)
- Quality gates extended to include test artifact verification

=============================================================================
-->

# Tongzhou MES Constitution

## Core Principles

### I. Modular Service Architecture

Each service MUST be independently deployable, testable, and scalable. Services are organized by business domain (gateway, admin-bff, basic, service1, thirdparty, openapi) with clear API boundaries. API modules (mes-api/*-api) provide contract-first interfaces consumed by service implementations.

**Rationale**: Microservices architecture enables independent team velocity, isolated failure domains, and flexible scaling. Contract-first API design ensures backward compatibility and clear service boundaries.

**Rules**:
- Each service MUST have a corresponding API module in `mes-api/` if it exposes interfaces to other services
- Services MUST NOT directly depend on other service implementations, only on API modules
- Service communication MUST go through defined API contracts (Feign clients)
- Shared configuration MUST reside in `mes-parent` POM

### II. Multi-Environment Configuration

The system MUST support five deployment environments with profile-based configuration: local (default), dev, stg, pet, prd. Environment-specific settings are externalized through Nacos configuration center.

**Rationale**: Manufacturing systems require rigorous testing through multiple stages before production deployment. Profile-based configuration prevents environment-specific code changes and reduces deployment risk.

**Rules**:
- All services MUST declare `spring.profiles.active=@profile.active@` in `bootstrap.yml`
- Sensitive credentials (Nacos password, database passwords) MUST be injected via startup parameters, NOT committed to source control
- Local profile MUST work with minimal external dependencies (local Nacos, MySQL, Redis)
- Environment-specific overrides MUST be documented in service README files

### III. Build Consistency & Dependency Management

Maven multi-module build MUST ensure reproducible builds with consistent dependency versions. Parent POM (`mes-parent`) centralizes dependency management using Macula Boot BOM.

**Rationale**: Microservices must maintain version compatibility to prevent runtime classpath conflicts. Centralized dependency management ensures all modules use tested, compatible library versions.

**Rules**:
- All service modules MUST inherit from `mes-parent` (version 1.0.0-SNAPSHOT)
- API modules MUST set `maven.install.skip=false` to publish to local/remote repositories
- Service modules MUST set `maven.install.skip=true` (no need to publish executable JARs)
- Build order MUST be: `mes-parent` → `mes-api` → service modules
- Before building a service, its API dependencies MUST be installed: `mvn install -pl mes-api/[service]-api -am -Dmaven.install.skip=false`

### IV. Observability & Operational Excellence

All services MUST provide comprehensive logging, metrics, and health checks. Structured logging with correlation IDs enables distributed tracing across service boundaries.

**Rationale**: Manufacturing systems require 24/7 availability. Operational visibility is mandatory for rapid issue diagnosis and system reliability.

**Rules**:
- All services MUST use centralized logging with configurable levels (default: INFO root, DEBUG for `com.tongzhou.mes`)
- Log files MUST be written to `${user.home}/logs/${spring.application.name}/`
- All services MUST expose Spring Boot Actuator health endpoints
- Services MUST use Swagger/OpenAPI for API documentation (`springdoc.swagger-ui.enabled=true`)
- Services MUST implement proper exception handling with meaningful error messages returned via REST APIs

### V. Test-Driven Development (RECOMMENDED)

Tests are RECOMMENDED for critical business logic and API contracts. When tests are written, they MUST follow the Red-Green-Refactor cycle.

**Rationale**: Manufacturing execution systems handle critical production data. While not mandatory for all features, tests for core business logic and service contracts significantly reduce production defects.

**Rules**:
- Contract tests RECOMMENDED for all Feign client interfaces in API modules
- Integration tests RECOMMENDED for database operations and external service calls
- Unit tests RECOMMENDED for complex business logic in service layers
- When tests are written: Write test → Verify it fails → Implement → Verify it passes
- Tests MUST use Spring Boot Test framework with appropriate test slices (`@WebMvcTest`, `@DataJpaTest`)
- All builds MUST support `-DskipTests` for rapid iteration scenarios

### VI. Java Coding Standards & Best Practices

All Java code MUST adhere to industry-recognized standards and best practices to ensure maintainability, reliability, and performance in production environments.

**Rationale**: Standardized coding practices reduce defects, improve code readability, and enable effective team collaboration. Following established guidelines from Alibaba Java Manual and Spring official documentation ensures battle-tested patterns are applied consistently across the codebase.

**Rules**:

#### 6.1 Alibaba Java Development Manual (Songshan Edition, 2023)

- **Naming Conventions (MANDATORY)**:
  - Class names MUST use UpperCamelCase (e.g., `OrderService`, `UserDTO`)
  - Method/variable names MUST use lowerCamelCase (e.g., `getUserById`, `totalAmount`)
  - Constants MUST use UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`, `DEFAULT_TIMEOUT`)
  - Package names MUST be lowercase (e.g., `com.tongzhou.mes.service`)
  - Abstract classes MUST start with `Abstract` or `Base`
  - Exception classes MUST end with `Exception`

- **Code Formatting (MANDATORY)**:
  - Indentation: 4 spaces (NO tabs)
  - Line length SHOULD NOT exceed 120 characters
  - Method length SHOULD NOT exceed 80 lines
  - Class length SHOULD NOT exceed 500 lines
  - Use `{}` for all control statements even single-line blocks

- **Object-Oriented Programming (MANDATORY)**:
  - AVOID using static methods unless truly stateless utility functions
  - Constructor MUST NOT call overridable methods
  - Override `equals()` MUST also override `hashCode()`
  - Use `@Override` annotation for all overridden methods
  - Prefer composition over inheritance

- **Collection Usage (MANDATORY)**:
  - Use `ArrayList` by default, `LinkedList` only when frequent insertions/deletions at head
  - Use `HashMap` by default, specify initial capacity if size known: `new HashMap<>(expectedSize / 0.75 + 1)`
  - NEVER modify collection while iterating (use Iterator.remove() or collect-then-modify pattern)
  - Use `Collections.emptyList()` instead of `new ArrayList<>()` for empty returns

- **Concurrent Programming (MANDATORY)**:
  - Use thread pools from `java.util.concurrent.Executors` or Spring's `@Async`
  - NEVER create threads via `new Thread()` directly
  - Use `ThreadLocal` with caution, MUST call `remove()` in `finally` block
  - Prefer `ConcurrentHashMap` over `Hashtable` or `Collections.synchronizedMap()`
  - Double-check locking MUST use `volatile` keyword

- **Exception Handling (MANDATORY)**:
  - NEVER catch `Throwable` or `Error`, only `Exception` and its subclasses
  - NEVER use exceptions for flow control
  - Transaction methods MUST NOT catch exceptions without rethrowing or rolling back
  - AVOID empty catch blocks; at minimum log the exception
  - Use specific exceptions, AVOID generic `RuntimeException`

#### 6.2 Spring / Spring Boot Best Practices

- **Dependency Injection (MANDATORY)**:
  - Use constructor injection (PREFERRED) over field injection
  - Mark dependencies as `final` when using constructor injection
  - AVOID `@Autowired` on fields; use constructor parameters
  - Use `@RequiredArgsConstructor` (Lombok) for constructor injection boilerplate

- **Component Scanning (MANDATORY)**:
  - Place `@SpringBootApplication` at root package (e.g., `com.tongzhou.mes.service1`)
  - AVOID `@ComponentScan` with broad base packages
  - Use `@Lazy` annotation to prevent circular dependencies

- **Configuration Management (MANDATORY)**:
  - Use `@ConfigurationProperties` instead of multiple `@Value` annotations
  - Externalize all environment-specific config to `application.yml` or Nacos
  - NEVER hardcode IPs, ports, credentials, or business thresholds in code
  - Use Spring profiles for environment-specific beans

- **REST API Design (MANDATORY)**:
  - Follow RESTful conventions: GET (query), POST (create), PUT (full update), PATCH (partial), DELETE (remove)
  - Use plural nouns for resources: `/api/v1/orders`, NOT `/api/v1/order`
  - HTTP status codes: 200 (OK), 201 (Created), 204 (No Content), 400 (Bad Request), 401 (Unauthorized), 404 (Not Found), 500 (Server Error)
  - Return standard response wrapper: `Result<T>` with `code`, `message`, `data` fields
  - Validate input using `@Valid` with Bean Validation annotations (`@NotNull`, `@Size`, etc.)

- **Transaction Management (MANDATORY)**:
  - Use `@Transactional` on service methods, NOT on controllers or repositories
  - Specify `rollbackFor = Exception.class` to rollback on checked exceptions
  - AVOID long-running transactions; keep them as short as possible
  - NEVER perform remote calls (HTTP, RPC) inside transactions
  - Use `@Transactional(readOnly = true)` for query-only methods (performance optimization)

#### 6.3 Common Engineering Standards

- **Logging Standards (MANDATORY)**:
  - Use SLF4J API, NOT direct Logback/Log4j dependencies: `private static final Logger log = LoggerFactory.getLogger(ClassName.class);`
  - Log levels: ERROR (system failure), WARN (recoverable issue), INFO (key business events), DEBUG (diagnostic details)
  - NEVER log sensitive data (passwords, tokens, ID numbers, phone numbers)
  - Use parameterized logging: `log.info("User {} logged in", userId)` NOT `log.info("User " + userId + " logged in")`
  - Include correlation ID (trace ID) in logs for distributed tracing

- **Resource Management (MANDATORY)**:
  - Use try-with-resources for `AutoCloseable` objects (files, connections, streams)
  - Close resources in `finally` block if try-with-resources not applicable
  - Set reasonable timeouts for HTTP clients, database connections, Redis operations
  - Use connection pools (Druid for JDBC, Lettuce for Redis) instead of direct connections

- **Performance Considerations (MANDATORY)**:
  - AVOID N+1 queries; use JOIN or batch queries
  - Use pagination for large result sets; NEVER `SELECT * FROM table` without LIMIT
  - Cache frequently accessed, rarely changed data in Redis
  - Use asynchronous processing (`@Async`, MQ) for time-consuming operations
  - Optimize SQL: Create indexes for WHERE/ORDER BY columns, AVOID `SELECT *`

#### 6.4 MySQL Best Practices

- **Schema Design (MANDATORY)**:
  - Primary key: Use `BIGINT AUTO_INCREMENT` or distributed ID generator (Snowflake, UUID)
  - AVOID `NULL` columns when possible; use default values (`DEFAULT ''`, `DEFAULT 0`)
  - Use appropriate data types: `TINYINT` for boolean, `DECIMAL` for money, `VARCHAR(N)` with proper length
  - Add indexes for foreign keys and frequently queried columns
  - Include `created_time`, `updated_time`, `is_deleted` (soft delete) columns

- **Query Optimization (MANDATORY)**:
  - Use MyBatis Plus wrapper for dynamic queries, AVOID string concatenation
  - LIMIT result sets: Use `PageHelper` or MyBatis Plus `Page<T>`
  - AVOID `SELECT *`; specify required columns explicitly
  - Use `EXPLAIN` to analyze query execution plans
  - Index columns used in WHERE, JOIN, ORDER BY clauses

- **Transaction Isolation (MANDATORY)**:
  - Default isolation level: `READ_COMMITTED` (prevent dirty reads)
  - Use `REPEATABLE_READ` only when necessary (MySQL default)
  - AVOID `SERIALIZABLE` unless absolutely required (performance penalty)
  - Use optimistic locking (version column) for high-concurrency updates

#### 6.5 Redis Best Practices

- **Key Design (MANDATORY)**:
  - Use namespace prefix: `mes:service1:user:{userId}` (hierarchical structure)
  - Set expiration (TTL) for all cache keys: `expire(key, duration)`
  - Key length SHOULD be concise but readable (< 50 characters)
  - Use consistent separators (`:` recommended)

- **Data Structures (MANDATORY)**:
  - STRING: Simple key-value, caching objects (JSON serialized)
  - HASH: Object properties, avoid storing large objects
  - LIST: Message queues, timeline data (use with caution for large lists)
  - SET: Unique elements, tags, followers
  - ZSET: Sorted rankings, time-series data

- **Caching Strategies (MANDATORY)**:
  - Cache-Aside pattern: Read from cache → Miss → Query DB → Update cache
  - Prevent cache penetration: Cache empty results with short TTL, use Bloom filter
  - Prevent cache breakdown: Use distributed locks (Redisson) for hot key updates
  - Prevent cache avalanche: Stagger TTLs (add random offset), use multi-level cache

- **Operations (MANDATORY)**:
  - Use pipeline for batch operations (reduce network round-trips)
  - Use `SCAN` instead of `KEYS` for key iteration (non-blocking)
  - Avoid storing large values (> 1MB); split into smaller chunks if necessary
  - Monitor slow queries (`SLOWLOG`), optimize commands taking > 10ms

#### 6.6 Message Queue (MQ) Best Practices

- **Message Design (MANDATORY)**:
  - Include message ID, timestamp, business context in payload
  - Use JSON for message serialization (human-readable, debuggable)
  - Keep message size reasonable (< 1MB recommended)
  - Design idempotent consumers (handle duplicate messages gracefully)

- **Reliability Guarantees (MANDATORY)**:
  - Producer: Enable transaction messages or confirmation callbacks
  - Consumer: Use manual acknowledgment (ACK), NOT auto-acknowledge
  - Implement retry mechanism with exponential backoff
  - Use dead-letter queue (DLQ) for messages exceeding max retries

#### 6.7 High Concurrency, High Availability, Data Consistency

- **Concurrency Patterns (MANDATORY)**:
  - Use distributed locks (Redisson, Zookeeper) for critical sections across instances
  - Implement rate limiting (Sentinel, Guava RateLimiter) to prevent overload
  - Use thread pools with bounded queues to prevent resource exhaustion
  - Avoid blocking operations in event loops or reactive streams

- **High Availability (MANDATORY)**:
  - Service degradation: Provide fallback responses when dependencies fail
  - Circuit breaker (Sentinel, Hystrix): Fail fast to prevent cascading failures
  - Implement health checks: `/actuator/health` endpoint with dependency checks
  - Use service registry (Nacos) for dynamic service discovery

- **Data Consistency (MANDATORY)**:
  - Distributed transactions: Use Seata for ACID guarantees (if enabled)
  - Eventual consistency: Use message queue (transactional message) for cross-service operations
  - Compensating transactions: Implement rollback logic for saga pattern
  - Optimistic locking: Use version field to prevent lost updates
  - Idempotency: Use unique request ID to prevent duplicate processing

- **Monitoring & Alerting (MANDATORY)**:
  - Expose metrics via Actuator: `/actuator/metrics`, `/actuator/prometheus`
  - Define SLOs: Response time (p95 < 200ms), availability (> 99.9%), error rate (< 0.1%)
  - Alert on anomalies: CPU > 80%, memory > 85%, error rate spike, slow queries
  - Use distributed tracing (SkyWalking, Zipkin) for cross-service debugging

### VII. Test File & Intermediate Artifact Management

All test-related files, mock services, test scripts, and intermediate artifacts generated during development MUST be isolated from production code and organized by feature branch. These artifacts MUST NOT be committed to version control.

**Rationale**: Development and testing generate numerous intermediate files (mock APIs, test scripts, temporary documentation) that support the development process but are not part of the deliverable product. Isolating these artifacts prevents repository clutter, maintains a clean Git history, and clearly separates production code from development tooling. Branch-specific organization enables parallel feature development without artifact conflicts.

**Rules**:

#### 7.1 Test Directory Structure (MANDATORY)

- All test-related artifacts MUST be placed in the `test/` directory at repository root
- Test artifacts MUST be organized by feature branch following this structure:
  ```
  test/
  ├── {branch-name}/              # e.g., 001-mes-integration, 002-user-auth
  │   ├── mock/                   # Mock services (API servers, stubs)
  │   ├── scripts/                # Test execution scripts (bash, shell)
  │   ├── data/                   # Test data files (JSON, CSV, SQL)
  │   └── docs/                   # Test-related documentation (guides, reports)
  └── shared/                     # Shared test utilities across branches
      ├── utils/                  # Reusable test helper scripts
      └── fixtures/               # Common test fixtures
  ```
- Feature branch name MUST match the actual Git branch name (e.g., `test/001-mes-integration/` for branch `001-mes-integration`)
- When switching branches, test artifacts from previous branches remain isolated
- Shared utilities that benefit multiple features MAY be placed in `test/shared/`

#### 7.2 Mock Services (MANDATORY)

- Mock API servers MUST be placed in `test/{branch-name}/mock/`
- Mock server filenames MUST indicate purpose: `mock-{service-name}-server.{ext}`
  - Examples: `mock-third-party-api-server.js`, `mock-auth-server.py`
- Mock servers MUST include a health check endpoint (e.g., `/health`, `/status`)
- Mock servers MUST log requests to facilitate debugging: `{service-name}.log`
- Mock data MUST be realistic but MUST NOT contain actual customer data or credentials
- Mock servers SHOULD run on non-standard ports (9000+) to avoid conflicts with real services

#### 7.3 Test Scripts (MANDATORY)

- Test execution scripts MUST be placed in `test/{branch-name}/scripts/`
- Script filenames MUST be descriptive and use kebab-case: `test-{feature}-{type}.sh`
  - Examples: `test-batch-push-flow.sh`, `test-integration-full.sh`, `verify-all-tables.sh`
- Scripts MUST include usage documentation in header comments
- Scripts MUST set appropriate exit codes: 0 (success), non-zero (failure)
- Scripts MUST be executable: `chmod +x test/{branch-name}/scripts/*.sh`
- Scripts MUST clean up temporary resources (processes, files) on exit or failure
- Database setup/teardown scripts MUST be idempotent (safe to run multiple times)

#### 7.4 Test Data (MANDATORY)

- Test data files MUST be placed in `test/{branch-name}/data/`
- Data filenames MUST indicate content: `{entity}-{purpose}.{format}`
  - Examples: `batch-samples.json`, `work-orders-mock.csv`, `schema-init.sql`
- Test data MUST be version-controlled within the `test/` structure (unlike logs/outputs)
- Sensitive data (passwords, tokens) MUST NOT be included; use placeholders like `<REPLACE_ME>`
- Large binary test files (images, PDFs) SHOULD be generated programmatically rather than committed

#### 7.5 Test Documentation (MANDATORY)

- Test-related documentation MUST be placed in `test/{branch-name}/docs/`
- Documentation types:
  - **Test guides**: How to run tests (e.g., `TESTING-GUIDE.md`)
  - **API references**: Mock API endpoint documentation (e.g., `MOCK-API-REFERENCE.md`)
  - **Test reports**: Results summaries, coverage reports (e.g., `TEST-REPORT-2026-01-25.md`)
  - **Setup instructions**: Environment setup for testing (e.g., `SETUP.md`)
- Documentation filenames MUST use UPPER-KEBAB-CASE for visibility: `{PURPOSE}.md`
- Test documentation SHOULD include:
  - Prerequisites (dependencies, services required)
  - Step-by-step execution instructions
  - Expected results and validation criteria
  - Troubleshooting common issues

#### 7.6 Git Exclusion (MANDATORY)

- The entire `test/` directory MUST be excluded from version control via `.gitignore`
- `.gitignore` MUST include these entries:
  ```gitignore
  # Test files and intermediate artifacts
  test/
  ```
- Exception: If certain test utilities in `test/shared/` are deemed valuable for the team, they MAY be committed after explicit team approval and constitution amendment
- Test artifacts MUST NEVER appear in pull requests or commit history
- Verify exclusion: `git status` MUST NOT show `test/` directory changes

#### 7.7 Lifecycle Management (MANDATORY)

- Test artifacts for a feature branch SHOULD be created when the feature branch is created
- Test artifacts MAY be deleted when the feature branch is merged and closed
- If test artifacts are reusable for regression testing, move to `test/shared/` before branch deletion
- Long-lived branches (e.g., `develop`, `staging`) MAY have persistent test directories
- Developers MUST document any long-running mock services or background processes in `test/{branch-name}/docs/PROCESSES.md`

#### 7.8 CI/CD Integration (RECOMMENDED)

- CI pipelines SHOULD be able to execute tests from `test/{branch-name}/scripts/`
- CI MUST NOT fail if `test/` directory is missing (local development only)
- CI MAY generate test reports and place them in `test/{branch-name}/docs/` (ephemeral, not committed)
- CI SHOULD verify that no `test/` artifacts leak into commits (pre-commit hook recommended)

## Development Standards

### Technology Stack

- **Framework**: Spring Boot 2.7.18 (via Macula Boot 5.0.15)
- **Language**: Java (version determined by Macula Boot parent)
- **Service Discovery**: Nacos
- **API Gateway**: Spring Cloud Gateway
- **Authentication**: OAuth2 Resource Server (JWT)
- **Database**: MySQL 8+ with Druid connection pool
- **ORM**: MyBatis Plus
- **Caching**: Redis
- **API Documentation**: SpringDoc OpenAPI 3
- **Feign Client**: OpenFeign with OkHttp
- **Frontend**: Vue.js (mes-admin)

### Code Organization

Services follow standardized package structure:

```
com.tongzhou.mes.[service]/
├── [Service]Application.java      # @SpringBootApplication entry point
├── controller/                     # REST endpoints
├── service/                        # Business logic layer
├── mapper/                         # MyBatis Plus data access
├── entity/                         # JPA/MyBatis entities
├── dto/                           # Data transfer objects
├── converter/                     # MapStruct converters
└── config/                        # Service-specific configuration
```

API modules expose only interfaces and DTOs:

```
com.tongzhou.mes.[service].api/
├── feign/                         # Feign client interfaces
├── dto/                           # Shared DTOs
└── constants/                     # Shared constants
```

### Security Requirements

- All services MUST validate JWT tokens via `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`
- Public endpoints MUST be explicitly whitelisted in `macula.security.ignore-urls`
- Passwords MUST NEVER be committed to version control
- Database credentials MUST be externalized to environment configuration
- Services MUST enforce HTTPS in non-local environments

## Quality Gates

### Before Feature Development Starts

- [ ] Constitution Check: Verify feature aligns with modular architecture principles
- [ ] API Contract Defined: If feature involves inter-service communication, API module MUST be designed first
- [ ] Environment Requirements: Identify which environments (Nacos, MySQL, Redis) are required
- [ ] Test Artifact Location: If feature requires test artifacts, create `test/{branch-name}/` directory structure

### Before Merging to Main Branch

- [ ] Build Success: `mvn clean install` completes without errors on all affected modules
- [ ] Code Standards Compliance: Java code follows Alibaba Java Manual and Spring best practices (Principle VI)
  - [ ] Naming conventions verified (classes, methods, variables, constants)
  - [ ] No static abuse, proper OOP design
  - [ ] Exception handling follows standards (no empty catches, proper transaction rollback)
  - [ ] Logging uses parameterized format, no sensitive data logged
  - [ ] Resource management uses try-with-resources
  - [ ] SQL queries optimized (no N+1, proper indexes, pagination applied)
  - [ ] Redis keys follow naming conventions with TTL set
  - [ ] Concurrent code uses thread pools, proper locking mechanisms
- [ ] Test Artifact Isolation: All test-related files confined to `test/{branch-name}/` (Principle VII)
  - [ ] No test artifacts in `src/` or production directories
  - [ ] `git status` shows no `test/` directory changes (properly ignored)
  - [ ] Mock services documented and ports non-conflicting
  - [ ] Test scripts are executable and include usage instructions
- [ ] Code Review: At least one peer review with focus on API contracts, error handling, and configuration externalization
- [ ] Manual Testing: Feature verified in local environment
- [ ] Documentation Updated: README and API documentation reflect changes
- [ ] No Secrets Committed: Scan for passwords, tokens, or sensitive data

### Before Production Deployment

- [ ] Multi-Environment Testing: Feature validated in dev → stg → pet progression
- [ ] Performance Baseline: Service startup time and memory footprint acceptable
- [ ] Rollback Plan: Previous version artifacts available for emergency rollback
- [ ] Operations Handoff: Deployment steps, configuration changes, and monitoring alerts documented

## Governance

### Amendment Process

1. Propose change via document review (must include rationale and impact analysis)
2. Team approval required for MAJOR or MINOR version changes
3. Update constitution version according to semantic versioning
4. Propagate changes to affected templates and documentation
5. Announce amendments to all team members

### Constitution Versioning

- **MAJOR** (X.0.0): Backward-incompatible changes (e.g., removing a principle, changing service architecture model)
- **MINOR** (X.Y.0): New principles or sections added (e.g., adding security requirements)
- **PATCH** (X.Y.Z): Clarifications, wording improvements, typo fixes

### Compliance

- All code reviews MUST verify adherence to Core Principles (Sections I-VII)
- Java code MUST comply with Principle VI (Coding Standards): Alibaba Java Manual, Spring best practices, middleware usage guidelines
- Test artifacts MUST comply with Principle VII (Test File Management): Isolated in `test/` directory, branch-specific organization, Git-ignored
- Any deviation from MUST requirements requires explicit justification documented in code or PR
- Complexity violations (e.g., bypassing API modules) MUST be tracked in `plan.md` Complexity Tracking table
- Code standards violations (e.g., using `new Thread()`, missing transaction annotations) MUST be identified and corrected before merge
- Test artifact leakage (committing `test/` files, placing mocks in `src/`) MUST be rejected in code review
- This constitution supersedes undocumented practices and tribal knowledge

### Enforcement

Team leads review constitution compliance during:
- Architecture design reviews (before implementation)
- Pull request reviews (during implementation)
- Retrospectives (after delivery)

Repeated violations require either:
1. Amending the constitution to reflect actual practice (if violations are justified), OR
2. Corrective action to bring code into compliance (if violations are harmful)

---

**Version**: 1.2.0 | **Ratified**: 2026-01-21 | **Last Amended**: 2026-01-25
