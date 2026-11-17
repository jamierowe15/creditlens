# CreditLens: Credit Decisioning Platform

A full-stack credit decisioning service: an applicant applies for a loan, a **rules engine**
decides in milliseconds (APPROVED / REFERRED / DECLINED) with **explainable reason codes**,
and an **underwriter** resolves referred applications. Every decision is written to an
**append-only audit trail**.

![CI](https://github.com/jamierowe15/creditlens/actions/workflows/ci.yml/badge.svg)

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Web (REST), Spring Data JPA / Hibernate, Bean Validation |
| Database | H2 (dev and tests, PostgreSQL mode), PostgreSQL 17 (Docker profile), Flyway migrations |
| Frontend | Angular 22, TypeScript, standalone components, signals, reactive forms, RxJS |
| Quality | JUnit 5, AssertJ, MockMvc integration tests, JaCoCo coverage, GitHub Actions CI |
| Operations | Spring Boot Actuator, Micrometer metrics (Prometheus format), correlation-id logging |
| API docs | OpenAPI 3 + Swagger UI |

## Features

- **Rules engine** (Strategy pattern): credit score bands, debt-to-income (including the new payment),
  loan-to-income, employment verification, minimum income. All rules run, so every reason is reported.
- **Risk-based pricing**: APR by credit-score band and product; amortized monthly payment in `BigDecimal`.
- **Live what-if preview**: the Angular form calls `POST /api/decisions/preview` as you type (debounced, `switchMap`).
- **Underwriter workflow**: only `REFERRED` applications can be reviewed (`409 Conflict` otherwise); a note is mandatory.
- **Audit trail**: who decided, when, from which status, under which rules version.
- **Secure by default**: server-side validation, RFC 9457 problem responses with no stack traces,
  PII masked in logs and responses, parameterized queries, strict CORS, security headers,
  optimistic locking (`@Version`), validated correlation ids (no log injection).

## Run it (Windows)

```powershell
powershell -ExecutionPolicy Bypass -File .\run-dev.ps1
```

Or manually, in two terminals:

```powershell
cd backend;  mvn spring-boot:run      # http://localhost:8080
cd frontend; npm install; npm start    # http://localhost:4200
```

| URL | What |
|---|---|
| http://localhost:4200 | Angular app |
| http://localhost:8080/swagger-ui.html | Interactive API docs |
| http://localhost:8080/actuator/health | Health check |
| http://localhost:8080/actuator/prometheus | Metrics (`creditlens_decisions_total`) |
| http://localhost:8080/h2-console | Database browser (JDBC URL `jdbc:h2:mem:creditlens`, user `sa`) |

Tests: `cd backend; mvn verify` then open `backend/target/site/jacoco/index.html`.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/applications` | Submit; engine decides; `201 Created` + `Location` |
| GET | `/api/applications?status=REFERRED&page=0&size=20` | Paged list, newest first |
| GET | `/api/applications/{id}` | One application |
| GET | `/api/applications/{id}/audit` | Decision history |
| POST | `/api/applications/{id}/review` | Underwriter approve/decline (`REFERRED` only) |
| POST | `/api/decisions/preview` | What-if decision, nothing saved |
| GET | `/api/stats` | Dashboard counts |

## Decision policy (rules version 2026.11-v1)

| Code | Outcome | Rule |
|---|---|---|
| INC1 | Decline | Annual income below $20,000 |
| EMP1 | Decline | Unemployed |
| EMP2 | Refer | Self-employed or student income needs verification |
| CS01 | Decline | Credit score below 600 |
| CS02 | Refer | Credit score 600-659 |
| DTI1 | Decline | (Existing debt + new payment) / monthly income above 50% |
| DTI2 | Refer | DTI between 40% and 50% |
| LTI1 | Decline | Requested amount above annual income |
| LTI2 | Refer | Requested amount above 50% of annual income |

Worst outcome wins: any Decline means DECLINED, otherwise any Refer means REFERRED, otherwise APPROVED.

## Project layout

```
backend/   Spring Boot API   (decision/ = pure-Java engine, web/ = REST, service/ = use cases)
frontend/  Angular app       (core/ = API + models, pages/ = screens, shared/ = reusable UI)
docs/      LECTURE.md, the full walkthrough of how and why everything works
```

All applicants and data are fictional. This is a portfolio project and is not affiliated with any bank.
