# CreditLens Lecture: Full-Stack Credit Decisioning, From Zero to Interview-Ready

Target role: **Software Engineer I (Full Stack), Credit Decisions, Technology Solutions** (Toronto, req R_1518503, closes Dec 1, 2026)
Stack: Java 21, Spring Boot 3.5, Spring Data JPA, Flyway, H2/PostgreSQL, Angular 22, TypeScript, RxJS, JUnit 5, GitHub Actions

---

## 0. How to use this lecture

The setup script already built and ran everything. This lecture is for **understanding** it well enough to
rebuild it alone and to defend every line in an interview.

Work through it in this order. Each module ends with a "Prove it" task you do on your own machine.

| Module | Topic | Time |
|---|---|---|
| 1 | What the posting asks for, and how this project answers it | 20 min |
| 2 | Credit decisioning 101 (the business) | 45 min |
| 3 | Architecture and the life of one request | 30 min |
| 4 | Backend, layer by layer | 3 to 4 h |
| 5 | Frontend (Angular) | 2 to 3 h |
| 6 | Testing | 1 h |
| 7 | CI/CD | 30 min |
| 8 | Security checklist | 45 min |
| 9 | Production incident drill + root-cause analysis | 1 h |
| 10 | Using AI tools responsibly | 15 min |
| 11 | Rebuild-it-yourself exercises | 1 to 2 days |
| 12 | Interview questions with model answers | 1 h |
| 13 | Resume bullets | 15 min |
| 14 | Command cheat sheet | reference |

Rule for yourself: **do not move on from a module until you can explain it out loud without looking.**

---

## 1. The posting, decoded

The team builds "secure, scalable and resilient applications in Credit Decisions". Credit decisioning means:
a customer applies for credit (loan, line of credit, card), and a system decides yes, no, or "a human must
look at this", fast, consistently, explainably, and with a full audit trail.

| Posting says | Where CreditLens proves it |
|---|---|
| Java, Spring/Spring Boot | `backend/` (Spring Boot 3.5, Java 21) |
| RESTful APIs, microservices | `ApplicationController`, `DecisionController`; one self-contained service with its own DB, health checks and metrics, which is what a microservice is |
| Angular/TypeScript | `frontend/` (Angular 22, strict TypeScript) |
| SQL and relational databases (DB2, Oracle, PostgreSQL, MySQL) | Flyway SQL migrations, H2 in PostgreSQL mode, real PostgreSQL via Docker |
| Git/GitHub, CI/CD, automated testing, DevOps | GitHub repo, `.github/workflows/ci.yml`, JUnit + MockMvc tests, JaCoCo coverage |
| Secure coding, application security | Validation, problem responses, PII masking, CORS, security headers, optimistic locking (module 8) |
| Performance, scalability, resiliency | Paging with a size cap, indexes, `@BatchSize` to avoid N+1, stateless API, optimistic locking |
| Production troubleshooting using logs, metrics, monitoring | Correlation ids on every log line, Actuator health, Micrometer metrics, Prometheus endpoint (module 9) |
| Root-cause analysis, preventative fixes | Incident drill and RCA template (module 9) |
| AI-assisted development tools | Module 10 |
| Banking / credit decisioning (preferred) | The whole domain: DTI, score bands, reason codes, referral workflow, audit |
| Technical design, code reviews | Design decisions are explained in comments and in this lecture; practise defending them |

Be honest with yourself about one thing: the posting asks for **3+ years** of enterprise full-stack experience.
This project does not replace that. What it does is show that you already work the way their team works, which
is your best argument for an interview anyway. Apply, and let the project carry the conversation.

**Prove it:** Without looking, name 5 rows of that table and point to the file for each.

---

## 2. Credit decisioning 101

### 2.1 The vocabulary

| Term | Meaning | In CreditLens |
|---|---|---|
| Credit score | A number summarizing credit history. In Canada (Equifax, TransUnion) it runs **300 to 900**. | `creditScore`, validated 300-900 |
| DTI (debt-to-income) | Monthly debt payments / monthly gross income | `(existing debt + NEW payment) / (annual income / 12)` |
| LTI (loan-to-income) | Requested amount / annual income | `requestedAmount / annualIncome` |
| APR | Annual percentage rate, the yearly cost of borrowing | `PricingPolicy` |
| Amortization | Paying a loan in equal monthly payments that cover interest + principal | `LoanMath.monthlyPayment` |
| Referral | The automated system is not confident; a human underwriter decides | `REFERRED` status |
| Underwriter | The credit officer who reviews referred files | `POST /review` |
| Reason codes / adverse action | When credit is declined, the lender must be able to say why | `ReasonCode` enum |
| Risk-based pricing | Riskier borrowers pay higher rates | Score band -> APR |
| Policy / rules version | Which version of the rules made a decision | `rulesVersion` stored on every audit row |

### 2.2 Why REFER exists

A pure yes/no system is either too strict (declines good customers, loses revenue) or too loose (approves bad
loans, loses money). The middle band goes to a human who can look at documents (pay stubs, tax returns).
This is why self-employed and student income is REFER, not DECLINE: it is not bad, it is **unverified**.

### 2.3 The math, worked by hand

Avery: income $92,000, existing debt $450/month, score 781, wants $15,000 personal loan for 36 months.

1. **APR**: score 781 is in the 760+ band -> 7.49%. Personal loan adjustment 0.00 -> **7.49%**.
2. **Monthly rate**: r = 7.49 / 100 / 12 = 0.0062417
3. **Payment**: M = P * r / (1 - (1 + r)^-n) = 15000 * 0.0062417 / (1 - 1.0062417^-36) = **$466.52**
4. **Monthly income**: 92000 / 12 = $7,666.67
5. **DTI**: (450 + 466.52) / 7666.67 = **0.1195 (11.95%)** -> under 40%, pass
6. **LTI**: 15000 / 92000 = 0.163 -> under 50%, pass
7. All rules pass -> **APPROVED** for $15,000.

The key insight in step 5: DTI includes the **new** payment. Checking only existing debt would approve people
who cannot afford the loan they are asking for. Interviewers in credit love hearing that.

### 2.4 Why `BigDecimal` and never `double`

```java
System.out.println(0.1 + 0.2);   // 0.30000000000000004
```

Floating point cannot represent most decimal fractions exactly. Across millions of transactions those
errors add up to real money. `BigDecimal` is exact decimal arithmetic. Two traps to remember:

- `new BigDecimal("0.1")` (string) is exact; `new BigDecimal(0.1)` (double) copies the error. Always use strings.
- `new BigDecimal("0.5").equals(new BigDecimal("0.50"))` is **false** (different scale). Compare with `compareTo`.
  That is why every rule uses `compareTo`, and the tests use `isEqualByComparingTo`.

**Prove it:** Work out by hand (calculator allowed) the DTI for: income $60,000, debt $1,900, $10,000 at 9.49% over 36 months
(payment $320.28). You should get 0.4441, which is a REFER (DTI2). That is exactly the `dtiIncludesNewPayment` test.

---

## 3. Architecture

### 3.1 The big picture

```
 Browser (Angular, :4200)
   |  /api/*  (dev proxy: proxy.conf.json)
   v
 Spring Boot API (:8080)
   |-- CorrelationIdFilter     every request gets an id -> logs + response header
   |-- SecurityHeadersFilter   nosniff, no framing, no caching of financial data
   |-- Controllers (web/)      HTTP in/out, validation, status codes, DTOs
   |-- Service (service/)      use cases, transactions, audit, metrics
   |-- DecisionEngine          pure Java rules: no Spring, no DB, no HTTP
   |-- Repositories            Spring Data JPA -> SQL
   v
 Database (H2 in dev/tests, PostgreSQL in Docker)
   credit_application, application_reason_code, decision_audit, flyway_schema_history

 Side doors: /actuator/health, /actuator/prometheus, /swagger-ui.html, /h2-console (dev only)
```

### 3.2 Layering rule

Each layer only talks to the one below it. Controllers never touch repositories; the engine never touches
anything but its inputs. Why it matters:

- You can unit test the engine in milliseconds (no Spring startup).
- You could put the same service behind a message queue instead of HTTP without changing business logic.
- When a bug report says "wrong decision", you know it is in `decision/`; "wrong status code" is `web/`.

### 3.3 The life of `POST /api/applications`

1. Angular `NewApplication.submit()` -> `CreditApiService.submit()` -> `HttpClient.post('/api/applications')`.
2. `correlationIdInterceptor` adds `X-Correlation-Id: <uuid>`.
3. Angular dev server proxies `/api` to `localhost:8080`.
4. Tomcat receives it. `CorrelationIdFilter` validates the id and puts it in the **MDC** (logging context).
5. `SecurityHeadersFilter` adds defensive headers.
6. Spring MVC routes to `ApplicationController.submit`. Jackson turns JSON into `CreateApplicationRequest`.
7. `@Valid` runs Bean Validation. If any field fails -> `MethodArgumentNotValidException` -> `GlobalExceptionHandler` -> **400** with field errors. Stop.
8. `CreditApplicationService.submit` (inside a database transaction):
   1. `request.toProfile()` strips PII (name, email) out: the engine only sees finances.
   2. `engine.decide(profile)` -> `DecisionResult` (timed by Micrometer).
   3. `CreditApplication.submit(...)` builds the entity; `applyAutomatedDecision(...)` copies results in.
   4. `applications.save(app)` -> `INSERT INTO credit_application ...` and reason codes.
   5. `audits.save(...)` -> `INSERT INTO decision_audit ...`
   6. Counter `creditlens.decisions{outcome=APPROVED,source=auto}` +1.
   7. Log line with reference, outcome, risk and **masked** email, tagged with the correlation id.
   8. Transaction commits. Both inserts succeed or both roll back.
9. Controller maps entity -> `ApplicationResponse` (DTO) and returns **201 Created** + `Location: /api/applications/{id}`.
10. Angular navigates to `/applications/{id}`.

**Prove it:** Close this file and write those 10 steps from memory. Then set a breakpoint in
`CreditApplicationService.submit` (IntelliJ or VS Code), submit the form, and step through it.

---

## 4. Backend, layer by layer

### 4.1 Maven and Spring Boot

`pom.xml` inherits from `spring-boot-starter-parent`, which pins compatible versions of ~hundreds of libraries
(that is why most dependencies have no `<version>`). **Starters** are bundles:
`spring-boot-starter-web` = Spring MVC + Jackson + embedded Tomcat.

**Auto-configuration**: Boot sees H2 on the classpath + `spring.datasource.url` -> builds a DataSource.
Sees Flyway -> runs migrations at startup. Sees Actuator -> exposes `/actuator/*`. You configure by
**properties**, not by code.

**Profiles** (`application-<name>.yml`) switch configuration:

| Profile | When | What changes |
|---|---|---|
| `dev` (default) | `mvn spring-boot:run` | H2 console on, DEBUG logs, demo data seeded |
| `test` | `@ActiveProfiles("test")` in tests | No seeder: tests start from an empty DB |
| `postgres` | `-Dspring-boot.run.profiles=postgres` | Real PostgreSQL; secrets from env variables |

Maven lifecycle you will use: `mvn test` (unit + integration tests), `mvn verify` (tests + coverage report),
`mvn spring-boot:run` (start), `mvn package` (build `target/creditlens-api-1.0.0.jar`, run with `java -jar`).

### 4.2 The domain model (`domain/`)

`CreditApplication` is a JPA **entity**: a Java class mapped to a table.

| Annotation | Why |
|---|---|
| `@Entity @Table(name="credit_application")` | Map class to table |
| `@Id @GeneratedValue(strategy = IDENTITY)` | DB generates the id (identity column) |
| `@Column(precision=12, scale=2)` | Money: 12 digits, 2 after the point |
| `@Enumerated(EnumType.STRING)` | Store `"APPROVED"`, not `0`. With ORDINAL, inserting a new enum value in the middle silently changes the meaning of every existing row. |
| `@ElementCollection` | A list of simple values (reason codes) in a child table, no separate entity needed |
| `@BatchSize(size = 50)` | Loading 20 applications would otherwise run 1 + 20 queries (the **N+1 problem**). With batching: 1 + 1. |
| `@Version` | **Optimistic locking**. Hibernate adds `WHERE version = ?` to updates. If two underwriters review the same file at once, the second update matches 0 rows -> exception -> 409. No database locks held. |
| `@PreUpdate` | Hook that refreshes `updatedAt` on every update |

**Rich domain model**: there are no setters. The only ways to change state are
`applyAutomatedDecision(...)` and `review(...)`, and `review` refuses unless the status is REFERRED. The rule
"engine APPROVED/DECLINED decisions are final" lives **in the object**, so no future controller can bypass it.

`DecisionAudit` is **append-only**: insert, never update or delete. In regulated industries the history of a
decision is as important as the decision.

### 4.3 Flyway: the schema is code

`src/main/resources/db/migration/V1__init.sql` creates the tables. On startup Flyway checks the table
`flyway_schema_history` and runs any migration not yet applied, in version order.

- Never edit a migration that already ran anywhere: Flyway stores a checksum and will refuse to start. Add `V2__...sql`.
- `spring.jpa.hibernate.ddl-auto: none` means Hibernate never alters tables. In banks, schema changes go
  through review like any code; "Hibernate changed prod schema on deploy" is a classic incident.
- CHECK constraints (`credit_score BETWEEN 300 AND 900`) are **defense in depth**: even a buggy future code
  path cannot store an impossible score.
- Indexes: `(status, created_at)` serves the dashboard query "REFERRED, newest first".

Open the H2 console (http://localhost:8080/h2-console, JDBC URL `jdbc:h2:mem:creditlens`, user `sa`) and run:

```sql
SELECT * FROM flyway_schema_history;
SELECT reference_number, status, risk_score, dti_ratio FROM credit_application ORDER BY risk_score DESC;
SELECT a.reference_number, r.reason_code
FROM credit_application a JOIN application_reason_code r ON r.application_id = a.id;
SELECT status, COUNT(*), AVG(credit_score) FROM credit_application GROUP BY status;
```

### 4.4 The decision engine (`decision/`), the most important package

**Records.** `ApplicantProfile`, `DecisionContext`, `RuleResult`, `DecisionResult` are Java `record`s:
immutable data with generated constructor, accessors, `equals`, `hashCode`, `toString`. Immutable data cannot
be changed by another thread or another rule halfway through a decision.

**Strategy pattern.** `CreditRule` is an interface with `evaluate(DecisionContext)`. Each policy is one class:
`MinimumIncomeRule`, `EmploymentRule`, `CreditScoreRule`, `DebtToIncomeRule`, `LoanToIncomeRule`. The engine
loops over a `List<CreditRule>`. Adding a policy = adding a class + one line in `DecisionConfig`.
That is the **Open/Closed principle**: open for extension, closed for modification.

**The algorithm** (`DecisionEngine.decide`):

1. `buildContext`: compute monthly income, APR, payment, DTI, LTI **once**, so no rule can compute them differently.
2. Run **every** rule. No early exit: the applicant and the underwriter see all reasons, not just the first.
3. Worst severity wins: `DECLINE > REFER > PASS` (enum order, compared with `compareTo`).
4. Sort reasons most-serious-first.
5. Risk score 0-100: 50% credit score, 35% DTI, 15% LTI. It is **informational** for underwriters; the outcome
   comes only from the rules. (Mixing the two is a common design flaw: you cannot explain a decline caused by
   "the weighted blend was 61".)
6. Return an immutable `DecisionResult` stamped with `RULES_VERSION`.

**Why no Spring in this package?** `DecisionEngine` has zero annotations. `DecisionConfig` creates it with
`@Bean`. Result: `new DecisionConfig().decisionEngine()` in a plain JUnit test, no application startup,
runs in milliseconds. Keep business rules framework-free and they outlive every framework upgrade.

**Edge case worth mentioning in an interview:** income of 0 would divide by zero. `LoanMath.ratio` returns a
cap (9.9999) when the denominator is zero, and `MinimumIncomeRule` declines anyway. There is a test for it.

### 4.5 The service layer (`service/`)

```java
@Service
@Transactional
public class CreditApplicationService { ... }
```

- **`@Transactional`**: Spring wraps every public method in a DB transaction. If saving the audit row fails,
  the application insert is rolled back too. Read methods use `readOnly = true` (a hint that allows optimizations).
- **Constructor injection**: dependencies are `final` fields set in the constructor. No `@Autowired` on fields.
  Benefits: impossible to forget a dependency, easy to pass mocks in tests, object is immutable after construction.
- **Domain exceptions**: the service throws `ApplicationNotFoundException` and `InvalidStateTransitionException`.
  It knows nothing about HTTP; translating to 404/409 is the web layer's job.
- **Metrics**: a `Timer` around the engine and a `Counter` per outcome. Module 9 shows how you use them.
- **Logging**: SLF4J with `{}` placeholders (no string concatenation cost when the level is off). What is logged:
  reference number, outcome, risk, masked email. What is **not** logged: name, income, full email.

### 4.6 The web layer (`web/`): REST done properly

| Rule | Example here |
|---|---|
| Resources are nouns, plural | `/api/applications`, `/api/applications/{id}` |
| HTTP verbs carry meaning | GET reads, POST creates or triggers an action |
| Sub-resources for related data | `/api/applications/{id}/audit` |
| Actions that are not CRUD become sub-resource POSTs | `POST /api/applications/{id}/review` |
| Create returns **201 + Location** | `ResponseEntity.created(location)` |
| Lists are paged and capped | `page`, `size` (max 100), newest first |
| Errors use standard status codes | 400 invalid input, 404 missing, 409 wrong state / concurrent edit, 500 unexpected |

**DTO vs entity.** Controllers return `ApplicationResponse`, never `CreditApplication`. Reasons:
(1) you choose exactly what leaves the service (email is masked, version is hidden);
(2) the API contract stays stable when the table changes;
(3) no accidental lazy-loading or infinite JSON recursion.

**Validation.** `@Valid @RequestBody CreateApplicationRequest`: annotations on the record
(`@NotBlank`, `@Email`, `@Min(300) @Max(900)`, `@DecimalMin`, `@Digits`) run before your code. The Angular form
checks the same things, but **the server never trusts the client**: anyone can call the API with curl.

`PreviewRequest` deliberately has no name or email: a what-if needs no personal data, so it does not ask for any
(**data minimization**, a core privacy principle).

### 4.7 Error handling (`web/error/GlobalExceptionHandler`)

`@RestControllerAdvice` catches exceptions from every controller and turns them into **RFC 9457 Problem Details**:

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/api/applications",
  "errors": { "creditScore": "must be greater than or equal to 300", "email": "must be a well-formed email address" }
}
```

- It extends `ResponseEntityExceptionHandler`, which already maps Spring's own exceptions (bad JSON, unknown
  enum value, wrong type in a query param, unsupported method) to correct 4xx codes.
- The catch-all `Exception` handler logs the full stack trace **server-side** and returns only a generic message
  plus the correlation id. Stack traces in responses reveal library versions, class names and SQL to attackers.
- `server.error.include-stacktrace: never` closes the same hole for errors outside controllers.

### 4.8 Cross-cutting concerns (`config/`)

**CorrelationIdFilter.** Puts an id in SLF4J's **MDC** (Mapped Diagnostic Context, a per-thread map that the
log pattern prints as `[cid=...]`). Every log line for that request carries the id; the id goes back in the
response header; Angular generates it per call. When a user says "my submit failed at 2:14", support searches
logs for one id and sees the whole story. Two details that show maturity:

- The incoming header is validated against `[A-Za-z0-9-]{8,64}`. Otherwise an attacker could send
  `abc\nINFO fake log line` and forge entries in your logs (**log injection**).
- `MDC.remove` in `finally`: Tomcat reuses threads, so a leftover id would leak into the next request's logs.

**SecurityHeadersFilter.** `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (no clickjacking),
`Referrer-Policy: no-referrer`, `Cache-Control: no-store` (financial data must not sit in browser or proxy caches).

**WebConfig (CORS).** Browsers block JavaScript from calling another origin unless the server allows it. Only
`http://localhost:4200` is allowed, only GET/POST, only two headers. `allowedOrigins("*")` in a bank API is a finding in every security review.

**DataSeeder** (`@Profile("dev")`): inserts 10 fictional applications **through the real service**, so each
gets a genuine engine decision and audit row. Never seed by raw SQL inserts that bypass business rules.

### 4.9 Observability

| Endpoint | Use |
|---|---|
| `/actuator/health` | Load balancers and Kubernetes probe this. Includes DB status. |
| `/actuator/info` | App name + rules version (what is deployed right now?) |
| `/actuator/metrics/creditlens.decisions` | Decision counts; add `?tag=outcome:DECLINED` |
| `/actuator/metrics/creditlens.decision.latency` | Engine timing |
| `/actuator/prometheus` | Everything in Prometheus format for Grafana dashboards and alerts |

In a real deployment, health/metrics would not be public; they would sit on a separate management port or
behind authentication. Say that in an interview.

**Prove it (module 4):** Without looking, write `DebtToIncomeRule` from scratch, including both thresholds and
the `compareTo` reasoning. Then explain `@Version` to an imaginary interviewer in under 60 seconds.

---

## 5. Frontend (Angular 22)

### 5.1 Structure

```
src/app/
  app.ts, app.config.ts, app.routes.ts   shell, providers, routes
  core/      models.ts (types), credit-api.service.ts (HTTP), correlation-id.interceptor.ts, problem.ts
  pages/     dashboard.ts, new-application.ts, application-detail.ts
  shared/    status-badge.ts, decision-card.ts (reusable, input-only components)
```

### 5.2 Modern Angular concepts you must be able to explain

| Concept | Where | One-line explanation |
|---|---|---|
| Standalone components | every component (`imports: [...]`) | No NgModules; each component declares what it uses |
| `inject()` | `inject(CreditApiService)` | Dependency injection without a constructor |
| Signals | `signal()`, `computed()` | Reactive values; Angular re-renders exactly what reads a changed signal |
| Signal inputs | `input.required<ApplicationStatus>()` | Component inputs as signals, read with `status()` |
| Zoneless change detection | default in Angular 22 | No zone.js; signals tell Angular when to update. This is why all UI state is in signals. |
| Built-in control flow | `@if`, `@for (...; track x.id)`, `@else` | Replaces `*ngIf`/`*ngFor`; `track` lets Angular reuse DOM rows |
| Lazy routes | `loadComponent: () => import(...)` | Each page downloads only when visited (see the separate chunks in the build output) |
| Route params as inputs | `withComponentInputBinding()` + `id = input.required<string>()` | `:id` in the URL arrives as an input |
| Functional interceptor | `correlationIdInterceptor` | Middleware for every HTTP call |
| Reactive forms | `FormBuilder.nonNullable.group({...})` | Form state and validation defined in TypeScript, fully typed |

### 5.3 The live preview pipeline (the RxJS question you will get)

```ts
this.form.valueChanges.pipe(
  debounceTime(350),                       // wait until typing pauses
  map(() => this.financials()),            // read current values (null if invalid)
  filter((f): f is ApplicantFinancials => f !== null),  // skip invalid input
  switchMap(f => this.api.preview(f).pipe( // new value? CANCEL the old request
    catchError(err => { ...; return of(null); }),       // one error must not kill the stream
  )),
  takeUntilDestroyed(this.destroyRef),     // unsubscribe when leaving the page (no memory leak)
).subscribe(p => this.preview.set(p));
```

Why `switchMap` and not `mergeMap`: if a slow response for "income 6" arrives after the fast one for
"income 65000", `mergeMap` would show the wrong (stale) decision. `switchMap` cancels the stale request.
Why `catchError` **inside** the `switchMap`: an error in the outer stream completes it and the preview
would stop updating forever.

### 5.4 Talking to the API

- `CreditApiService` is the only place that knows URLs. Components call methods and get typed Observables.
- `models.ts` mirrors the Java DTOs. Union types (`'APPROVED' | 'REFERRED' | 'DECLINED'`) mean a typo is a
  compile error, not a runtime bug.
- `readProblem()` converts any error into a message + field errors. Status 0 means "could not reach the server",
  which gets its own friendly message.
- `proxy.conf.json` forwards `/api` from :4200 to :8080 in development, so the browser sees one origin.

### 5.5 Accessibility and UX details

Labels wrap inputs, the risk bar has an `aria-label`, filter buttons are a labelled group, disabled states
during submit prevent double submissions, server-side field errors are shown next to the right field.

**Prove it (module 5):** Add a "Term" column to the dashboard table, and make the "Needs review" filter the
default. Rebuild with `npm run build` and confirm zero errors.

---

## 6. Testing

```
        /\        few, slow, most realistic: end-to-end (browser)       -> exercise 11.8
       /  \       some: integration (Spring + DB + HTTP via MockMvc)    -> ApplicationApiIntegrationTest
      /____\      many, fast: unit (pure Java)                          -> DecisionEngineTest, LoanMathTest
```

| Test | Proves |
|---|---|
| `strongApplicantApproved` | Happy path, correct APR band, low risk |
| `creditScoreBands` (parameterized) | Boundaries 599/600/659 exactly. Bugs live at boundaries (`<` vs `<=`). |
| `dtiIncludesNewPayment` | The key business insight from 2.3 |
| `declineWinsAndAllReasonsListed` | Severity ordering + no short-circuit |
| `zeroIncomeDoesNotDivideByZero` | Edge case |
| `amortizedPaymentMatchesKnownValue` | Math checked against an independent known value ($888.49) |
| `invalidInputReturns400WithFieldErrors` | Validation + problem format |
| `referredApplicationCanBeApprovedByUnderwriterAndIsAudited` | Full workflow + audit trail |
| `reviewingAnApprovedApplicationReturns409` | State machine enforcement |
| `correlationIdIsEchoed...`, `unsafeCorrelationIdIsReplaced` | Cross-cutting security behaviour |

Techniques to name: **AAA** (Arrange, Act, Assert), parameterized tests, AssertJ fluent assertions,
`@SpringBootTest` + `@AutoConfigureMockMvc`, `@Transactional` on the test class (each test rolls back, so tests
never depend on each other), `@ActiveProfiles("test")` (no seed data, so counts are predictable).

Coverage: `mvn verify`, then open `backend/target/site/jacoco/index.html`. Aim for high coverage of
`decision/` (business rules); 100% everywhere is not the goal, meaningful assertions are.

**Prove it:** Break `CreditScoreRule` on purpose (`score < 600` -> `score <= 600`) and run `mvn test`.
Watch exactly one test fail and read its message. Revert.

---

## 7. CI/CD

`.github/workflows/ci.yml` runs on every push to `main` and every pull request:

1. **backend** job: checkout, Java 21 (Temurin) with Maven cache, `mvn -B verify` (compile, all tests, coverage), upload the JaCoCo report as a build artifact.
2. **frontend** job: checkout, Node 24, `npm install`, `npm run build` (production build type-checks every template).

The two jobs run in parallel. The README badge turns red if either fails.

What **CD** (continuous delivery) would add in a real team: build a Docker image, push it to a registry, deploy
to a test environment, run smoke tests (`/actuator/health`), then promote to production with approval gates.
Exercise 11.9 has you write the Dockerfile.

---

## 8. Security checklist (OWASP Top 10 lens)

| Risk | Mitigation in CreditLens | File |
|---|---|---|
| Injection (SQL) | Spring Data / JPA bind parameters; no string-built SQL | repositories |
| Injection (logs) | Correlation id whitelisted by regex | `CorrelationIdFilter` |
| Broken access control | **Not yet implemented**: anyone can review. Exercise 11.5 adds Spring Security roles. Say this honestly. | - |
| Sensitive data exposure | Email masked in responses and logs; PII never reaches the engine; `no-store` caching | `Masking`, `ApplicationResponse` |
| Security misconfiguration | Strict CORS, no stack traces, H2 console only in `dev`, secrets via env vars | `WebConfig`, `application*.yml` |
| Insecure design | State machine in the domain object; mandatory review note; append-only audit | `CreditApplication.review` |
| Data integrity / race conditions | `@Version` optimistic locking -> 409 | entity + handler |
| Input validation | Bean Validation on every request field + DB CHECK constraints | DTOs, `V1__init.sql` |
| Vulnerable components | Spring Boot BOM keeps versions aligned; add Dependabot (exercise) | `pom.xml` |
| Logging and monitoring failures | Correlation ids, metrics, audit trail | module 9 |

Knowing what is **missing** (auth) and saying so is a strong signal in an interview. Pretending it is
production-ready is a weak one.

---

## 9. Production incident drill

The posting says: troubleshoot with logs, metrics and monitoring; investigate incidents; perform root-cause
analysis. Practise exactly that.

### Drill A: "Approval rate dropped to zero"

1. **Simulate**: in `CreditScoreRule`, change `if (score < 660) return RuleResult.of(ReasonCode.CS02);` to `< 900`. Restart the backend. Submit 3 strong applications in the UI.
2. **Detect** (metrics): open `http://localhost:8080/actuator/metrics/creditlens.decisions?tag=outcome:APPROVED&tag=source:auto`. Count is not increasing; the REFERRED count is.
3. **Narrow** (data): H2 console:
   ```sql
   SELECT r.reason_code, COUNT(*) FROM application_reason_code r GROUP BY r.reason_code ORDER BY 2 DESC;
   ```
   Every new application has CS02.
4. **Correlate** (logs): copy the `X-Correlation-Id` from the browser dev tools (Network tab, response headers), search the backend console for it. You see `outcome=REFERRED risk=12`: a very low risk referred, which is suspicious.
5. **Root cause**: a rule threshold change. Check `/actuator/info` for the rules version: it did not change, so the change shipped without a version bump. **Two** problems.
6. **Fix + prevent**: revert; the boundary test `scoreOf660PassesTheScoreRule` would have caught it in CI, so the deeper cause is "tests were not run before deploy". Preventative action: branch protection requiring green CI.

### Drill B: "Users see 'Cannot reach the API'"

Stop the backend. The UI shows the status-0 message from `readProblem`. Check `/actuator/health` (connection
refused). Restart. Discuss: what would a load balancer health check do here?

### Drill C: "Two underwriters, one file"

Open the same REFERRED application in two browser tabs. Approve in tab 1, decline in tab 2. Tab 2 gets a
409 ("Only REFERRED applications can be reviewed"). Then explain the difference between this business-rule
409 and the `@Version` 409 (which happens when both requests are **in flight at the same moment**).

### RCA template (use it for each drill, keep the write-ups in `docs/incidents/`)

```
Title:
Date / duration / impact (who, how many, which money):
Detection (which alert, metric or user report):
Timeline (UTC):
Root cause (the 5 whys):
Contributing factors:
What went well / what went badly:
Corrective actions (immediate):
Preventative actions (long-term, each with an owner and date):
```

---

## 10. Using AI tools responsibly

The posting lists Copilot/Claude experience as a requirement and asks you to use them "responsibly". In an interview, a good answer covers:

- **You own every line.** AI output is a draft. You read it, test it, and could explain it without the tool. (That is what this lecture is for.)
- **Verify against reality.** Run the tests, read the docs, check versions. AI can be confidently wrong about APIs.
- **Never paste confidential data** (customer data, internal code, credentials) into tools that are not approved by your employer.
- **Use it for leverage**: generating test cases from a rule table, explaining an unfamiliar stack trace, drafting a migration, reviewing your own diff for missed edge cases.
- **Be honest**: "I built CreditLens with an AI assistant as a pair programmer, then rebuilt the decision engine myself and wrote the extra rules in the exercises" is a much stronger answer than pretending you did not use one.

---

## 11. Rebuild-it-yourself exercises

Do them in order. Commit each one separately with a clear message (`feat: add province rule`). Each one is a
genuine talking point.

1. **New rule.** Add `TermRule`: credit score under 660 and term over 60 months -> REFER with new code `TRM1`. Add the enum value, the class, register it in `DecisionConfig`, and a parameterized test. Bump `RULES_VERSION`.
2. **Migration V2.** Add a `province` column (`VARCHAR(2)`, e.g. ON, QC) with `V2__add_province.sql`. Make it required in the request (`@Pattern(regexp = "[A-Z]{2}")`), show it in the UI. Notice you did **not** touch V1.
3. **Search endpoint.** `GET /api/applications?minRisk=60` using a derived query or `@Query`. Add an integration test.
4. **Angular filter.** Add a "High risk only" toggle on the dashboard that uses exercise 3.
5. **Security (big one).** Add `spring-boot-starter-security`. Two in-memory users: `analyst` (can view) and `underwriter` (can view + review). Protect `/review` with `hasRole('UNDERWRITER')`. Use the authenticated user's name as `decidedBy` instead of trusting the request body. Update the tests with `@WithMockUser`.
6. **PostgreSQL for real.** Install Docker Desktop, `docker compose up -d`, run with the `postgres` profile, connect with pgAdmin or DBeaver and run the queries from 4.3.
7. **Frontend unit test.** Test `readProblem` and `DecisionCard.heading()` with the Angular test runner.
8. **End-to-end test.** Add Playwright: open the app, submit an application, assert the status badge.
9. **Docker image.** Write a multi-stage `backend/Dockerfile` (Maven build stage, then `eclipse-temurin:21-jre`), and add a CI step that builds it.
10. **Upgrade.** Spring Boot 4 is the current major line. Try upgrading on a branch, read the migration guide, and write down what broke. That is real enterprise work.
11. **Dependabot.** Add `.github/dependabot.yml` for Maven, npm and GitHub Actions.

---

## 12. Interview questions with model answers

**Q1. Walk me through your project.**
"CreditLens is a credit decisioning service. An applicant submits a request, a rules engine I wrote as plain Java returns approved, referred or declined with reason codes, and referred files go to an underwriter screen. Every decision lands in an append-only audit table with the rules version. It is Spring Boot 3.5 on Java 21 with JPA and Flyway, an Angular 22 front end, MockMvc integration tests, and GitHub Actions CI."

**Q2. Why is the engine plain Java instead of a Spring service?**
"Business rules should not depend on a framework. It is created as a bean in a config class, but the class itself has no annotations, so I unit test it with `new` in milliseconds, and it would survive a framework change."

**Q3. How would you add a new credit policy?**
"Implement `CreditRule`, register it in `DecisionConfig`, add a reason code, bump the rules version, add boundary tests. The engine does not change: Strategy pattern, Open/Closed."

**Q4. Why BigDecimal? Any gotchas?**
"Floating point cannot represent most decimals exactly. Gotchas: construct from strings, and compare with `compareTo` because `equals` also compares scale."

**Q5. What is the N+1 problem and did you hit it?**
"Loading a list, then one extra query per row for a child collection. Reason codes are an element collection; I added `@BatchSize` so a page of 20 loads them in one extra query instead of 20."

**Q6. Explain optimistic vs pessimistic locking.**
"Pessimistic locks the row while you work; safe but blocks others. Optimistic uses a version column: updates include `WHERE version = ?`; if someone else saved first, zero rows match and I return 409. Underwriter reviews rarely collide, so optimistic fits."

**Q7. How do you handle errors in the API?**
"One `@RestControllerAdvice` returns RFC 9457 problem details: 400 with per-field errors, 404, 409 for invalid state or concurrent edits, and a generic 500 with a correlation id. Stack traces stay in server logs."

**Q8. A user says their application 'just failed'. How do you investigate?**
"Get the correlation id from the response header or the error message, search logs for it, check health and the decision metrics for a wider pattern, then query the audit and reason-code tables. Then a root-cause write-up with a preventative action, for example a missing boundary test."

**Q9. Why DTOs instead of returning entities?**
"Control over what leaves the service, like masking the email, a stable contract when the schema changes, and no lazy-loading or recursion surprises."

**Q10. How do you keep database changes safe?**
"Flyway versioned migrations reviewed like code, Hibernate never touches the schema, never edit an applied migration, CHECK constraints as a second line of defense."

**Q11. What is `switchMap` and why did you use it?**
"It cancels the previous inner request when a new value arrives. For the live preview that prevents a slow, stale response from overwriting a newer one."

**Q12. Signals vs RxJS?**
"Signals hold synchronous UI state and drive rendering, which matters because Angular 22 is zoneless by default. RxJS is for async streams over time, like debounced form changes and HTTP. I use RxJS for the pipeline and set a signal at the end."

**Q13. What is missing before this could go to production?**
"Authentication and role-based authorization, securing the actuator endpoints, a real database with backups, secrets from a vault, rate limiting, and a deployment pipeline with environments. I would also add contract tests between the Angular app and the API."

**Q14. What does `@Transactional` actually do here?**
"Spring opens a transaction before `submit`, and commits after, so the application and its audit row are saved together or not at all. If an exception is thrown, it rolls back."

**Q15. Tell me about a bug you found.**
Use one you really hit while doing the exercises. If none: Drill A from module 9, framed as "I ran an incident drill on purpose to practise RCA".

---

## 13. Resume bullets (personal project, be accurate)

- Built **CreditLens**, a full-stack credit decisioning app (**Java 21, Spring Boot, Angular, TypeScript, SQL**) with a rules engine that returns approve/refer/decline outcomes with explainable reason codes and risk-based pricing.
- Designed **RESTful APIs** with Bean Validation, RFC 9457 error responses, paging and an underwriter review workflow enforced by a domain state machine and optimistic locking.
- Managed schema with **Flyway** migrations on H2/PostgreSQL; implemented an append-only audit trail recording actor, transition and rules version for every decision.
- Wrote **JUnit 5** unit tests and **MockMvc** integration tests with JaCoCo coverage; automated build and tests with **GitHub Actions** CI.
- Added production-readiness features: correlation-id logging, PII masking, security headers, strict CORS, **Actuator** health checks and **Micrometer/Prometheus** metrics; practised incident triage and root-cause analysis.

GitHub: github.com/jamierowe15/creditlens

---

## 14. Command cheat sheet (PowerShell)

```powershell
# Start everything
powershell -ExecutionPolicy Bypass -File .\run-dev.ps1

# Backend
cd backend
mvn spring-boot:run                                      # start (dev profile, seeded data)
mvn test                                                 # all tests
mvn -Dtest=DecisionEngineTest test                       # one test class
mvn verify                                               # tests + coverage report
mvn package; java -jar target\creditlens-api-1.0.0.jar   # run the packaged jar
mvn spring-boot:run "-Dspring-boot.run.profiles=postgres" # PostgreSQL profile (after docker compose up -d)

# Frontend
cd frontend
npm install
npm start                # http://localhost:4200 (proxies /api to :8080)
npm run build            # production build into dist/

# API by hand
Invoke-RestMethod http://localhost:8080/api/stats
Invoke-RestMethod http://localhost:8080/api/applications?status=REFERRED
$body = @{ annualIncome=60000; monthlyDebtPayments=1900; creditScore=720; requestedAmount=10000;
           termMonths=36; employmentStatus='FULL_TIME'; productType='PERSONAL_LOAN' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/decisions/preview -ContentType 'application/json' -Body $body

# Git
git status
git add -A; git commit -m "feat: add TermRule (TRM1)"
git push
```

Ports already in use? `netstat -ano | findstr :8080` then `taskkill /PID <pid> /F`.
