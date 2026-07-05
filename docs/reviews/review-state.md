# Deep Review — State

- **Started:** 2026-07-06
- **Report:** `docs/reviews/2026-07-deep-review.md`
- **Commit at start:** `74d736e19fdb324b4f84b7a0395adad873cafa00`

## Phase checklist

- [x] 0. Inventory & plan
- [ ] 1. Architecture review (whole system: Spring Modulith boundaries, dependency direction, API surface, frontend data flow)
- [ ] 2. Common module — security-critical cross-cutting: `common/security`, `common/users`, `common/encryption`, `common/ratelimit`, `common/email`, `authorizationserver`
- [ ] 3. Common module — framework infrastructure: `common/hateoas`, `common/mvc`, `common/patch`, `common/jdbc`, `common/pagination`, `common/validation`, `common/templating`, `common/ui`, `common/observability`, `common/logging`, `common/bootstrap`, `common/domain`, `common/exceptions`
- [ ] 4. Members module (domain, application, infrastructure) — registration numbers, GDPR data, member lifecycle
- [ ] 5. Events module + `oris` integration + calendar module — registrations, deadlines, ORIS sync
- [ ] 6. Finance + membershipfees modules — money handling, transactions, overdraft, fee campaigns
- [ ] 7. Groups module (familygroup, freegroup, traininggroup, common, application)
- [ ] 8. Persistence & migrations (V001–V004, memento pattern boundaries), configuration & profiles, build & dependencies, test layout/coverage strategy
- [ ] 9. Frontend — infrastructure: `src/api` (authorizedFetch, hateoas, klabisUserManager, token renewal), auth components, HalNavigator2, KlabisTable, contexts, hooks
- [ ] 10. Frontend — application: pages, feature components (events, finance, groups, members, membership-fees, dashboard), localization, utils
- [ ] Final. Synthesis & executive summary

## Critical data invariants (for the data-correctness lens)

1. **Money** — `Money` value objects (finance + events each have one) wrap `BigDecimal` + currency; account balance must equal the sum of its transactions; overdraft limited by `OverdraftPolicy`; transaction reversal must be idempotent (`TransactionAlreadyReversedException`).
2. **Event registrations** — one registration per member per event (`DuplicateRegistrationException`); registration deadlines are date-inclusive through the whole deadline day (recent fix `e75e267b` — verify consistency everywhere deadlines are checked).
3. **Registration numbers** — club member registration numbers (ZBMxxxx) must be unique and generated without collisions.
4. **GDPR / sensitive data** — birth certificate number (rodné číslo) encrypted at rest via Jasypt; must never leak into API responses, logs, or error messages.
5. **Event publication log** — Spring Modulith JDBC event publication acts as the transactional outbox; cross-module consistency depends on events being published in the same transaction as the aggregate write.
6. **Fee campaigns** — membership fee campaign lifecycle (open → closed, manual close); charges must not be double-applied to member accounts.
7. **Schema-vs-code drift** — dev runs H2 (in-memory, reset on restart), prod PostgreSQL; migrations are edited in place (V001 amended) rather than appended — a deliberate pre-release choice, but check H2/PG dialect divergence.

## Watch list (spots later phases must examine)

*(populated by Phase 1)*

## Carry-over notes

- Backend: 643 main Java files, 244 test files. Module sizes: common 156, events 116, groups 99, membershipfees 92, members 89, finance 42, calendar 41, authorizationserver 5, oris 2.
- Frontend: 336 TS/TSX files; `klabisApi.d.ts` is generated from OpenAPI — review the boundaries around it, not the file itself.
- `db/migration/V004__AddUnaccentExtension.java` is a Java-based Flyway migration — check H2/PG behavior split.
- Only one ADR exists (`ADR-001` on cross-module ports) although root CLAUDE.md references several more decisions (auto-config layout, optional `myClubId`, cache-manager fallback, enum choices, package structure) — flag doc drift in Phase 1/8.
- Backend CLAUDE.md policy: new domain DDL goes into V001 (edit in place, no new migration scripts) — pre-release choice, evaluate in Phase 8.
- Private Maven dependency `com.dpolach.api:oris-client` (snapshot `0db715d9-SNAPSHOT`) — supply-chain/reproducibility note for Phase 8.
- Frontend build artifacts are copied into `backend/src/main/resources/static` and committed to git — evaluate in Phase 1/8.
- `spring-boot-h2console` is enabled at runtime in production ("while in development phase", per build.gradle.kts comment) — security review in Phase 2/8.
- OAuth2 authorization server is embedded in the backend (`authorizationserver` package, 5 classes + `common/security`); frontend authenticates via oidc-client-ts against it.
- Observability stack in docker-compose: Zipkin, Prometheus, Grafana.
