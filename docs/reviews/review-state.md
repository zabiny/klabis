# Deep Review — State

- **Started:** 2026-07-06
- **Report:** `docs/reviews/2026-07-deep-review.md`
- **Commit at start:** `74d736e19fdb324b4f84b7a0395adad873cafa00`

## Phase checklist

- [x] 0. Inventory & plan
- [x] 1. Architecture review (whole system: Spring Modulith boundaries, dependency direction, API surface, frontend data flow) — 6 findings (1 HIGH, 3 MEDIUM, 2 LOW)
- [x] 2. Common module — security-critical cross-cutting: `common/security`, `common/users`, `common/encryption`, `common/ratelimit`, `common/email`, `authorizationserver` — 5 findings (1 CRITICAL, 1 HIGH, 1 MEDIUM, 2 LOW). NOTE: password setup/change *flows* skipped per reviewer request — Phase 3 or a follow-up should still deep-dive `PasswordSetupServiceImpl`, `PasswordChangeService`, token endpoints for their own correctness/security.
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

- **Phase 2:** `authorizationserver/KlabisUserDetailsService` imports `common.users.domain.User`/`UserPermissions` directly (lines 6-7) — check whether these are return types of the `UserService` primary port (acceptable) or deeper coupling; ADR-001 said "consume primary ports only". Also: `spring-boot-h2console` as `runtimeOnly` in prod builds.
- **Phase 3:** `common` is an OPEN Modulith module (finding A1 context) — while reviewing common infrastructure, note any types that only exist to serve one business module (candidates to move out). `CustomMetricsTrackingAspect` uses `KlabisApplication.class.getPackage()` as event filter — verify it filters what the javadoc claims. **Also (deferred from Phase 2):** deep-dive the password setup/change flows — `common/users/application/PasswordSetupServiceImpl` (token generation/validation/complete, rate-limit key = registrationNumber, `requestNewToken` uses generic response but `validateToken`/`completePasswordSetup` may leak account-status via distinct exceptions — check user-enumeration surface), `PasswordChangeService`, `PasswordComplexityValidator`, `TokenCleanupJob`, and `PasswordSetupController.getClientIpAddress` (trusts `X-Forwarded-For` unconditionally — spoofable, affects `usedByIp` audit and could be used to evade any IP-based control). `PerKeyRateLimiter.checkLimit` never expires per-key counters except via the cache's 1h `expireAfterWrite`, and holds a growing `ConcurrentHashMap<String,Object> locks` that is never cleaned — unbounded memory under many distinct keys.
- **Phase 5 (events/oris/calendar):** `events.domain.Money.parseCurrency` silently falls back to CZK on null/blank/invalid currency codes — data-correctness risk for imported ORIS events with foreign/malformed currencies. `events.Money` and `finance.Money` are divergent implementations (finance has arithmetic + invariant asserts; events has parsing + fallback). Calendar has direct read access to the `events.domain` named interface (Event aggregate) — verify it never mutates. `EventsExceptionHandler` maps `DuplicateRegistrationException` with empty detail (`""`) — check the SPA shows something sensible on double-registration.
- **Phase 6 (finance/membershipfees):** `membershipfees` imports `events.domain.EventType` (cross-module entity exposure via named interface) and `events.infrastructure.bootstrap.EventTypeDataBootstrap` constants (infra→infra coupling in `MembershipFeeTiersDataBootstrap`). `YearlyFeeChargeMarkerRepository` in `CampaignProcessor` looks like the double-charge guard — verify idempotency (invariant #6).
- **Phase 8:** stale/missing ADRs (root CLAUDE.md references decisions not present in docs/design-decisions.md — only ADR-001 exists). Committed frontend bundles (finding A6). Snapshot dependency `oris-client:0db715d9-SNAPSHOT`. Check `.github/workflows` assumptions about committed static assets. **Config/security:** `application.yml` hardcodes `springdoc.swagger-ui.oauth.client-secret: apispec` and the auth-server issuer defaults to `https://localhost:8443` (finding S1 context) — verify prod overrides. `server.ssl.enabled: false` in base `application.yml` (TLS presumably terminated upstream — confirm). `spring.h2.console` enablement + `h2FilterChain` requires only `authenticated()` (any logged-in user could reach the H2 console if enabled in prod — cross-check with the runtime-enabled h2console dependency noted in Phase 0). Encryption `algorithm` defaults via `${jasypt.encryptor.algorithm:...}` — fine. Verify `KLABIS_JASYPT_PASSWORD` has no committed default anywhere.
- **Phase 9:** `HalNavigator2` + HAL hooks are the frontend's complexity concentration (69 consumer files) — deep-dive there. `api/setup.ts` typed openapi-fetch client is nearly unused (2 files) — check if it's a second idiom worth keeping. `AdminModeContext` doc comment references SandplacePage (finding A5).
- **Phase 10:** `HomePage.tsx:129` maps `rel === 'admin'` card to `/sandplace` (finding A5) — find intended admin target when reviewing pages.

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
