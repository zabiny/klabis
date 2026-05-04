## 1. Acceptance test scaffold (Red)

- [x] 1.1 Inventory `backend/src/main/resources/static/` and document any other static resources besides `silent-renew.html` that must remain reachable; extend the design's exclusion list if needed
- [x] 1.2 Create `WebContentRoutingIntegrationTest` with a failing scenario for `/docs/index.html` after session login (expect HTML containing the developer manual marker, currently receives SPA shell — test must FAIL on `main`)
- [x] 1.3 Add passing-baseline scenarios that already work (`/swagger-ui.html`, `/v3/api-docs`, `/silent-renew.html`, SPA route `/events`, `/api/foo-bar-not-existing` with JSON Accept) — confirms test wiring without false positives
- [x] 1.4 Run the test class via the test-runner agent — confirm scenario 1.2 fails and the rest pass

  **Note:** The Red expectation could not be reproduced in MockMvc — all 6 scenarios pass on `main`. The production bug (`/docs/index.html` → SPA shell on `api.klabis.otakar.io`) does not manifest in the test slice, likely due to Tomcat/resource-handler-ordering differences between deployed and test environments. Per user direction, we proceed straight to Green; the integration test remains as a regression guard, and Task 4 (browser verification on the deployed environment) is the authoritative end-to-end check.

## 2. Replace SPA controller with filter (Green)

- [x] 2.1 Implement `SpaFallbackFilter` (`com.klabis.common.ui`) as `OncePerRequestFilter` registered with `@Order(Ordered.LOWEST_PRECEDENCE)`; SPA shell forward only when prior handler returned 404, request `Accept` contains `text/html`, and request path is not on the exclusion list (prefixes `/api/`, `/swagger-ui/`, `/v3/api-docs`, `/docs/`, `/oauth2/`, `/.well-known/`, `/actuator/`, `/h2-console/`, `/login`, `/logout`, `/error`; exact path `/silent-renew.html`; static asset extensions)
- [x] 2.2 Delete `SpaFallbackController` (`com.klabis.common.ui.SpaFallbackController`)
- [x] 2.3 Verify `FrontendController` (`/auth/callback` forward) remains untouched — it is unrelated to SPA fallback
- [x] 2.4 Run `WebContentRoutingIntegrationTest` via test-runner — scenario from 1.2 must now pass, no regression in 1.3
- [x] 2.5 Run the full backend module test suite via test-runner — no regressions (one pre-existing unrelated failure in `EventJdbcRepositoryTest$FilterByRegisteredBy`, not caused by this change)

## 3. Refactor and harden

- [x] 3.1 Extract the exclusion list into a single private `static final` constant inside `SpaFallbackFilter` (one source of truth) — implemented as three `static final List<String>` constants: `EXCLUDED_PREFIXES`, `EXCLUDED_EXACT`, `STATIC_ASSET_EXTENSIONS`
- [x] 3.2 Add a guard test: SPA filter does NOT touch a 200/302/etc. response coming from any other handler (only 404 with HTML accept triggers forward) — `filterPreservesSuccessfulHtmlResponses`
- [x] 3.3 Add a guard test: `Accept: application/json` request to an unknown path returns the original 404 (filter must not forward to `index.html`) — `filterDoesNotForwardJsonAcceptOnUnknownPath` and `unknownApiPathReturnsJsonNotSpaShell`
- [x] 3.4 Re-run the full backend test suite via test-runner — 2337/2339 pass; 2 failures are pre-existing flakes unrelated to this change (`EventJdbcRepositoryTest` known cancelled-event rule, `OidcRegisteredClientsBootstrapTest` JDBC pool flake confirmed via isolated rerun)

## 4. End-to-end verification on the deployed environment

- [ ] 4.1 Deploy the change to `https://api.klabis.otakar.io`
- [ ] 4.2 In a browser: log in via SPA, then navigate to `/docs/index.html` — confirm developer manual renders (not the SPA 404)
- [ ] 4.3 In a browser: open `/swagger-ui.html` directly — confirm Swagger UI renders
- [ ] 4.4 Confirm SPA navigation (`/events`, `/members`, deep-link refresh) still works

## 5. Documentation

- [ ] 5.1 Update `docs/developerManual` with a short note on the routing chain (request flow: Spring Security chains → `DispatcherServlet` / `ResourceHttpRequestHandler` → `SpaFallbackFilter` 404 fallback)
- [ ] 5.2 Sync the spec change into `openspec/specs/non-functional-requirements/spec.md` after archiving
