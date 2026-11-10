# E-Library Service

电子图书馆后端服务。提供书目浏览、详情查询、借阅、归还、我的借阅。

**技术栈**：Java 8 · Spring Boot 2.7.18 · Spring Data JPA · H2 · springdoc-openapi 1.6.15

---

## 1. 运行

```bash
mvn spring-boot:run
# 或
mvn clean package && java -jar target/e-library-1.0.0.jar
```

启动后：

| 用途 | 地址 |
|---|---|
| **Swagger UI（推荐，可直接试调全部接口）** | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| H2 控制台 | http://localhost:8080/h2-console （JDBC URL: `jdbc:h2:mem:elibrary`，用户 `sa`，密码留空） |
| 健康检查 | http://localhost:8080/actuator/health |

> 在 Swagger UI 右上角点 **Authorize**，填入任意用户标识（如 `alice`），即可为所有请求自动带上
> `X-User-Id` 请求头，无需手工逐个添加。

启动时会自动灌入 8 本示例书籍（见 `DataSeeder`），关闭方式：`library.seed.enabled=false`。

### 非 Swagger 方式

```bash
# 浏览（分页 + 关键字 + 分类）
curl -H "X-User-Id: alice" "http://localhost:8080/api/v1/books?keyword=clean&size=5"

# 详情（含可用许可数）
curl -H "X-User-Id: alice" http://localhost:8080/api/v1/books/1

# 借阅
curl -X POST -H "X-User-Id: alice" http://localhost:8080/api/v1/books/1/loans

# 我的借阅（activeOnly=false 可查全部历史）
curl -H "X-User-Id: alice" "http://localhost:8080/api/v1/loans?activeOnly=true"

# 归还
curl -X POST -H "X-User-Id: alice" http://localhost:8080/api/v1/loans/1/return
```

### 测试

```bash
mvn test
```

15 个用例，包含一个 10 线程抢最后 1 个许可的并发测试（断言恰好 1 成功、9 冲突）。

---

## 2. 需求假设

需求未明确的部分，均按下列假设实现。**这些假设本身就是设计的一部分**，换一组假设会让领域模型不同。

| # | 未明确点 | 采用的假设 | 理由 |
|---|---|---|---|
| A1 | 电子书是否受并发许可限制 | **有**：每本书有 `totalLicenses`，同一时刻最多 N 个有效借阅 | 数字内容按并发许可授权是业界常态。若假设"无限并发"，借阅就退化成一条流水记录，无法体现任何业务规则 |
| A2 | 借阅期限 | 默认 14 天，`library.loan.period-days` 可配 | 通行值，配置化避免硬编码 |
| A3 | 逾期如何处理 | **不修改状态、不跑定时任务**，`overdue` 在查询时由 `dueAt` 与当前时钟派生 | 见 §6 决策 3 |
| A4 | 同一用户能否重复借同一本 | 不允许，返回 `DUPLICATE_ACTIVE_LOAN` | A1 的自然推论 |
| A5 | 单人借阅上限 | 默认 5 本，`library.loan.max-per-user` 可配 | 展示"规则集中管理" |
| A6 | 预约、排队、续借 | **不做**，见 §9 Out of Scope | 范围控制 |
| A7 | "阅读"的含义 | 仅返回 `contentUrl` 元信息，不做全文/DRM/流式 | 属于内容交付，不属于本服务职责 |
| A8 | 用户体系 | 不做注册登录，`X-User-Id` 请求头模拟身份 | 由 `UserContextInterceptor` 统一解析，换成 JWT 只需替换该类 |
| A9 | "浏览"的含义 | 分页 + 关键字（标题/作者/ISBN）+ 分类过滤 | 假设最少且够用 |

---

## 3. 架构

单模块单体、四层结构，依赖方向 `interfaces → application → domain ← infrastructure`：

```
com.elibrary
├── interfaces/rest/          适配层：Controller、ApiError、全局异常处理、X-User-Id 拦截器
├── application/              用例层：BookQueryService、LoanService（事务边界）；view/ 为读模型
├── domain/                   领域层：model/ 实体、repository/ 仓储端口、service/ BorrowPolicy、exception/ 错误码
└── infrastructure/           实现层：persistence/ JPA 适配、config/ 装配、bootstrap/ 种子数据
```

**依赖倒置**：`domain` 不依赖 Spring Data、不依赖任何持久化技术，只声明
`BookRepository` / `LoanRepository` 两个端口，由 `infrastructure.JpaBookRepository` 等实现。
`BorrowPolicy` 是纯 POJO，在 `DomainConfig` 里手工装配 —— 因此单测不需要启动 Spring。


---

## 4. 领域模型

```
Book                               Loan
├── id                             ├── id
├── isbn (unique)                  ├── bookId
├── title / author / publisher     ├── userId
├── category / description         ├── borrowedAt
├── contentUrl                     ├── dueAt
└── totalLicenses  (并发上限)       ├── returnedAt (null = 未归还)
                                   └── status: ACTIVE | RETURNED
派生量（不存储）：
  availableCount = totalLicenses − count(该书的 ACTIVE 借阅)
  overdue        = status == ACTIVE && now > dueAt
```

**两个关键取舍：**

- **`availableCount` 不落库**。冗余计数会引入并发更新竞争与漂移风险，而 `COUNT(*)` 走索引足够快。
  确有性能需要时再加带失效策略的缓存，而不是在源头埋一个需要维护的一致性缺口。
- **`overdue` 不是状态**。它是时钟的函数，不是数据。落成状态就必须有一个定时任务去推进它，
  还要处理任务漏跑后的一致性修复 —— 用一个假状态换一个后台组件，不划算。

---

## 5. API

统一前缀 `/api/v1`，除浏览/详情外均需 `X-User-Id` 请求头。

| 方法 | 路径 | 说明 | 成功码 |
|---|---|---|---|
| GET | `/books?keyword=&category=&page=&size=` | 浏览/搜索，分页 | 200 |
| GET | `/books/{bookId}` | 详情，含 `availableCount` | 200 |
| POST | `/books/{bookId}/loans` | 借阅 | **201** + `Location: /api/v1/loans/{id}` |
| GET | `/loans?activeOnly=` | 我的借阅，`activeOnly=false` 返回全部历史 | 200 |
| POST | `/loans/{loanId}/return` | 归还 | 200 |

**设计理由：**

- `POST /books/{id}/loans` 而非 `POST /loans`：借阅是"在书上发生的动作"，从书派生语义更明确；
- 归还用 `POST /return` 而非 `DELETE /loans/{id}`：归还后记录仍然存在（带 `returnedAt` 的历史），资源没有消失；
- 403 与 404 严格区分：借别人的借阅单返回 `LOAN_NOT_OWNED`(403)，单子本身不存在才是 404。

### 错误响应

所有失败返回同一 envelope：

```json
{
  "code": "BOOK_UNAVAILABLE",
  "message": "All 1 license(s) are currently on loan",
  "timestamp": "2026-11-10T02:24:07.941Z",
  "path": "/api/v1/books/2/loans"
}
```

| code | HTTP | 触发条件 |
|---|---|---|
| `BOOK_NOT_FOUND` | 404 | 书籍不存在 |
| `BOOK_UNAVAILABLE` | 409 | 可用许可已用尽 |
| `DUPLICATE_ACTIVE_LOAN` | 409 | 同一用户已持有该书有效借阅 |
| `LOAN_LIMIT_EXCEEDED` | 409 | 达到单人并发上限 |
| `LOAN_NOT_FOUND` | 404 | 借阅单不存在 |
| `LOAN_NOT_OWNED` | 403 | 操作他人借阅单 |
| `LOAN_ALREADY_RETURNED` | 409 | 重复归还 |
| `UNAUTHORIZED` | 401 | 缺少 `X-User-Id` |
| `VALIDATION_ERROR` | 400 | 参数非法 |

---

## 6. 关键设计决策与取舍

### 决策 1：并发借阅用悲观锁 + 数据库唯一索引双保险

**问题**：`totalLicenses=1` 的书，两个请求同时借。先 `count` 再插入，两个请求都会看到"还剩 1 个"，双双成功 → 超借。

**方案对比**

| 方案 | 优点 | 缺点 |
|---|---|---|
| 乐观锁重试 | 无锁开销 | 冲突时需重试循环；高争用下重试风暴 |
| 悲观锁 `SELECT ... FOR UPDATE` | 语义直接，计数检查在锁内必然正确 | 持锁时间 = 事务时长 |
| 仅靠唯一索引 | 最轻 | 只能防"重复借"，防不住"许可超卖" |

**选择**：`findByIdForUpdate` 对书目行加悲观写锁（单热点行的场景下，一次短暂行锁
比重试循环更划算），再用 `loan.active_key` 上的唯一索引兜底：

```sql
-- H2 无部分索引，用生成列让已归还的借阅 key 为 NULL（NULL 在唯一索引中不冲突）
ALTER TABLE loan ADD COLUMN active_key VARCHAR(80)
    GENERATED ALWAYS AS (CASE WHEN returned_at IS NULL THEN CONCAT(book_id, ':', user_id) END);
CREATE UNIQUE INDEX ux_loan_active_book_user ON loan (active_key);
```

生产库写法见 `src/main/resources/data.sql` 注释（PostgreSQL 直接用 `WHERE returned_at IS NULL` 部分索引；
MySQL 用同一套生成列技巧）。**双保险的意义**：即使将来有人绕开服务层直接写库，数据库也不会产生超借。

`ConcurrentBorrowTest` 固化了这个保证：10 线程抢 1 个许可，断言恰好 1 成功。

### 决策 2：借阅规则集中在 `BorrowPolicy`，与用例编排分离

`LoanService` 只负责"取书 → 校验 → 建单 → 保存"的流程，规则（重复借、许可耗尽、超上限）全在
`BorrowPolicy`。新增一条规则（如"欠费未缴禁止借阅"）只改策略类，用例层不动。

### 决策 3：逾期派生而非存储

`Loan` 只有两个真实状态。`overdue` 由 `Clock` 注入后在读取时计算：

```java
public boolean isOverdue(Instant now) { return isActive() && now.isAfter(dueAt); }
```

`Clock` 通过 `DomainConfig` 注入，测试可以把时间拨快 30 天直接断言逾期，不需要 `Thread.sleep`。

### 决策 4：列表接口避免 N+1

浏览一页 10 本书，若逐本 `count` 就是 11 次查询。`countActiveByBookIds` 用一条
`GROUP BY book_id` 聚合查询补齐整页，`BookView.of(book, Map)` 消费结果。

### 决策 5：身份通过拦截器注入，而非散落在 Controller

`UserContextInterceptor` 在 `/api/**` 上解析 `X-User-Id` 写入 `UserContext`（ThreadLocal），
`afterCompletion` 中清理。接入真实认证时只替换拦截器实现，Controller 签名不变。
缺失该头直接 401，而非默认成匿名用户 —— 静默降级比报错更危险。

### 决策 6：不加 Lombok

不引入 Lombok，是为了让任何环境克隆后都能直接编译运行，不需要额外安装 IDE 插件；
可读性与可运行性优先于减少样板代码。Java 8 没有 record，
故用显式构造器 + getter，代价是样板代码，收益是零依赖摩擦。

---

## 7. 错误处理

- 领域异常 `DomainException` 携带 `ErrorCode`，`ErrorCode` 自带 HTTP 状态码映射；
- `GlobalExceptionHandler` 是**唯一**的错误出口，Controller 内不写 try-catch；
- 未预期异常记录完整堆栈但对外只返回通用信息，避免泄露内部细节。

---

## 8. 测试

| 测试类 | 层次 | 覆盖 |
|---|---|---|
| `LoanTest` | 领域单测（无 Spring） | 逾期派生、归还幂等性 |
| `BorrowPolicyTest` | 领域单测（无 Spring，Mockito） | 4 条借阅规则 + 越权归还 |
| `ConcurrentBorrowTest` | Spring 集成 | **10 线程抢 1 个许可，恰好 1 成功** |
| `LoanApiIntegrationTest` | MockMvc | 借还完整流程、许可耗尽、重复借、缺 header、404 envelope |

```bash
mvn test          # 15 个用例，全部通过
```

---

## 9. Out of Scope（明确不做的部分）

| 未实现 | 若要做会怎么加 |
|---|---|
| 预约排队 | 新增 `Reservation` 聚合 + 归还时触发出队；需考虑排队公平性与过期清理 |
| 续借 | `Loan` 加 `renewCount`，规则进 `BorrowPolicy`；需定义"有预约者不得续借" |
| 逾期罚款/通知 | 罚款需要金额规则与支付集成；通知需要事件 + 异步投递，不适合放进主事务 |
| 全文阅读 / DRM | 独立的流式交付服务，签名 URL + 短期令牌，与借阅解耦 |
| 用户注册登录 | 换成 Spring Security + JWT，仅替换 `UserContextInterceptor` |
| 缓存与压测 | `availableCount` 适合作旁路缓存；需先有真实读比 |
| 多租户 | 加 `tenantId` 到聚合根并由过滤器注入；影响所有查询，不宜事后补 |

---

## 10. 若继续演进（按优先级）

1. **幂等键**：`POST /loans` 支持 `Idempotency-Key`，防止客户端超时重试造成重复借阅；
2. **数据库迁移工具**：目前用 Hibernate `ddl-auto` + `data.sql`，生产应换 Flyway，索引 DDL 才有版本；
3. **可观测性**：借阅/归还打结构化日志（已有 `loan.created` 语义位），接 Micrometer 指标；
4. **分页上限**：当前 `size` 硬上限 100，避免大页拖垮 DB。

---

## 11. 项目结构速览

```
e-library/
├── pom.xml
├── src/main/java/com/elibrary/
│   ├── ELibraryApplication.java
│   ├── interfaces/rest/       BookController, LoanController, ApiError,
│   │                          GlobalExceptionHandler, UserContext(+Interceptor), WebConfig
│   ├── application/           BookQueryService, LoanService, view/{BookView,LoanView}
│   ├── domain/                model/{Book,Loan,LoanStatus}
│   │                          repository/{BookRepository,LoanRepository}
│   │                          service/BorrowPolicy
│   │                          exception/{DomainException,ErrorCode}
│   └── infrastructure/        persistence/ (JPA 适配), config/, bootstrap/DataSeeder
├── src/main/resources/        application.yml, data.sql
└── src/test/java/com/elibrary/ 4 个测试类
```