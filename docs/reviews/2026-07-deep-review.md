# Klabis Deep Review — July 2026

- **Review started:** 2026-07-06
- **Commit:** `74d736e19fdb324b4f84b7a0395adad873cafa00` (branch `main`)
- **Scope:** whole repository — Spring Boot backend (`backend/`, 643 main / 244 test Java files), React frontend (`frontend/`, 336 TS/TSX files), configuration, database migrations, build, and specifications workflow (`openspec/` reviewed only as a reference for expected behavior, not as review subject).
- **Out of scope:** generated code (`frontend/src/api/klabisApi.d.ts`, build outputs in `backend/src/main/resources/static/assets`), third-party `oris-client` internals, `pencil/` and `tools/` support directories (checked only for secrets/config issues in Phase 8).

## Review lenses

1. **Maintainability** — duplication, dead code, unclear ownership, tangled dependencies, missing tests around risky logic, growth pain points.
2. **Readability** — misleading names, oversized methods, non-obvious control flow, lying comments, inconsistent idioms between similar modules.
3. **Security** — authN/authZ gaps, injection, secrets in code/config, sensitive-data exposure in responses or logs, CSRF/session handling, unsafe deserialization, dependency risks.
4. **Data correctness** — critical invariants below: types for critical values, idempotency of retried writes, transaction boundaries, invariants in DB constraints vs. code only, migration safety and environment drift.

## Critical data invariants

1. **Money** — `Money` value objects (finance and events modules) wrap `BigDecimal` + currency; a member account's balance must equal the sum of its transactions; overdraft is bounded by `OverdraftPolicy`; transaction reversal must be idempotent.
2. **Event registrations** — at most one registration per member per event; registration deadlines are inclusive of the entire deadline day (fix `e75e267b`), and that semantic must hold at every deadline check.
3. **Registration numbers** — club registration numbers (ZBMxxxx) must be unique and collision-free under concurrent generation.
4. **GDPR-sensitive data** — birth certificate number (rodné číslo) is Jasypt-encrypted at rest and must never appear in API responses, logs, or error messages.
5. **Transactional outbox** — Spring Modulith JDBC event publication log carries cross-module consistency; domain events must be published in the same transaction as the aggregate write.
6. **Fee campaigns** — campaign lifecycle transitions are one-way; fee charges must not double-apply to member accounts on retry or re-run.
7. **Environment drift** — dev/test run H2 in-memory, production runs PostgreSQL; migrations are edited in place pre-release; H2/PostgreSQL dialect differences must not hide schema divergence.

## Phase log

### Phase 0 — Inventory & plan (2026-07-06)

**System shape.** Klabis is a modular monolith for orienteering club management. The backend is a single Gradle module (Spring Boot 4.0.5, Java 21, Spring Modulith 2.0.0) with jMolecules DDD/hexagonal annotations enforced via ByteBuddy weaving. Business modules under `com.klabis`: `members`, `events`, `calendar`, `finance`, `membershipfees`, `groups` (with `familygroup`/`freegroup`/`traininggroup` sub-features), `oris` (ORIS federation API integration via private `oris-client` dependency), `authorizationserver` (embedded Spring Authorization Server), and a large `common` module (156 files) providing HATEOAS/HAL-FORMS infrastructure, field-level authorization, patch support, JDBC memento helpers, rate limiting, email, encryption, and user identity.

**Persistence.** Spring Data JDBC with a memento pattern (`*Memento` classes in `infrastructure/jdbc`). Flyway migrations: V001 (domain schema, edited in place by convention), V002 (OAuth2 tables), V003 (Modulith event publication), V004 (Java migration adding the `unaccent` extension). H2 for dev/tests, PostgreSQL for production, Testcontainers for integration tests.

**API surface.** HAL+FORMS hypermedia REST API (Spring HATEOAS), OpenAPI spec exported to `docs/openapi/` and consumed by the frontend type generator. OAuth2/OIDC auth served by the embedded authorization server; Resilience4j rate limiting; Jasypt encryption for GDPR fields.

**Frontend.** React 19 + TypeScript + Vite SPA. oidc-client-ts for auth, TanStack Query + openapi-fetch/openapi-react-query for data, Formik + Yup forms, Tailwind. A HAL-navigation layer (`HalNavigator2`, `KlabisTable`, `hateoas.ts`) drives generic hypermedia UI. Production build is copied into `backend/src/main/resources/static` and committed.

**Testing.** 244 backend test files mirroring main packages plus `testdomain`/`config` helpers; Modulith and jMolecules ArchUnit tests present. Frontend uses Vitest + Testing Library; test files live beside sources.

**Plan.** Eleven further phases (architecture; two passes over `common`; one per business area; persistence/config/build; two frontend passes; synthesis) — see `review-state.md` for the checklist and the watch list carried between phases.

---
