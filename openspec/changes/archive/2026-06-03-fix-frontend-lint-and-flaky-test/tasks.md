## 1. Easy & mechanical lint rules

- [x] 1.1 Run `eslint --fix` for auto-fixable rules; review the diff so only `no-useless-escape` (56) and `prefer-const` (1) changed (D2)
- [x] 1.2 Convert the two `require()` calls in `tailwind.config.ts` to ESM imports (`no-require-imports`) (D2)
- [x] 1.3 Remove or prefix the 7 `no-unused-vars` bindings (D2)
- [x] 1.4 Confirm `npm run lint` error count dropped by ~66 (only `no-explicit-any` remaining) and `npm run build` still type-checks; commit

## 2. Eliminate no-explicit-any (batched)

- [x] 2.1 Inventory the 271 `no-explicit-any` occurrences grouped by file/area (D3)
- [x] 2.2 Batch 1: fix `any` in the first file group with accurate types — generated API types / generics / `unknown`+narrowing, no `eslint-disable`, no `as any` relocation (D1); gate with `tsc` build + affected tests + eslint; commit
- [x] 2.3 Batch 2: next file group, same rules and gate (D1, D3); commit
- [x] 2.4 Batch 3: next file group, same rules and gate (D1, D3); commit
- [x] 2.5 Continue batching until `no-explicit-any` count is 0 (add commits as needed, one per area); each batch must keep the whole-frontend build green (D3)
- [x] 2.6 Confirm `npm run lint` exits 0 with zero errors

## 2b. Fix react-refresh/only-export-components errors

> Discovered during implementation: the baseline also has **10 `react-refresh/only-export-components` errors** (8 files) that the proposal mis-classified as warnings. They block `npm run lint` exit 0, so they are in scope. Fix by moving non-component exports (contexts, constants, helper hooks/functions) into sibling files; pure structural move, no behavior change.

- [x] 2b.1 Move non-component exports out of the 8 affected files (HalFormsCheckboxGroup, KlabisFieldsFactory, AdminModeContext, HalFormContext, HalRouteContext, ToastContext, BirthNumberConditionalField, ThemeContext; AuthContext2 also fixed) into sibling files; update imports; gate with `tsc` build + full test suite + eslint; commit

## 3. Stabilize AuthorizationServerPromptNoneTest

- [x] 3.1 Diagnose the `SQLTransientConnectionException` root cause (Hikari pool exhaustion under full-context `@SpringBootTest` + per-test `bootstrapDataLoader.run`) (D4) — root cause: `application-test.yml` overrode the datasource URL and dropped `DB_CLOSE_DELAY=-1`, so H2 dropped the in-memory DB whenever the pool briefly had no active connection between per-test bootstrap runs, invalidating pooled connections and exhausting the pool
- [x] 3.2 Fix the connection lifecycle / test datasource-pool configuration so the test reliably acquires a connection — without weakening the four assertions and without retry-until-pass (D4) — added `DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` to the test-profile H2 URL (test-only config, matches the h2 dev profile)
- [x] 3.3 If the root cause is production connection handling rather than test-only, STOP and reassess (would be functional → spec-driven) (D4 risk) — N/A: fix is test-profile H2 config only; production runs PostgreSQL, unaffected
- [x] 3.4 Run `AuthorizationServerPromptNoneTest` repeatedly (e.g. several consecutive runs) to confirm it no longer flakes; commit

## 4. Verification

- [x] 4.1 Frontend: `npm run lint` exits 0 and `npm run build` passes (test-runner) — lint 0 errors, 0 no-explicit-any, build passes, 1721/1721 tests green
- [x] 4.2 Backend: the authorization-server tests and the broader suite pass; only the pre-existing unrelated `testdomain` failures (ModularEventsTest / EventLoggingTests) remain, if still present on main (test-runner) — full suite 2806/2806 green
- [x] 4.3 Push branch, open PR, confirm Frontend Lint and Gradle Tests CI checks are green — PR #286 opened (https://github.com/zabiny/klabis/pull/286); all CI checks green (Frontend Lint + Gradle Tests pass)

## 5. Guard the clean lint state (added during implementation)

> Discovered during implementation: the `refresh-backend-server-resources` / `publish-frontend-resources` npm tasks ran only `test && build` — lint was never enforced when publishing the frontend into the backend, so lint debt could silently return. User opted to add a lint gate as part of this PR.

- [x] 5.1 Prepend `npm run lint &&` to both `refresh-backend-server-resources` and `publish-frontend-resources` scripts so publishing fails on any ESLint error (warnings still pass, since eslint exits 0 on warnings)

## 6. Stabilize MemberAccountCreationIntegrationTest (surfaced by CI)

> The PR #286 Gradle Tests run failed on a second flaky test, `MemberAccountCreationIntegrationTest`: a Spring Modulith Scenario race where the verification ran on `MemberCreatedEvent` arrival, before the finance-module listener had created the `MemberAccount`. Under CI load the verify callback won the race and `findById` was empty. Unrelated to this PR's other changes; fixed in scope at the user's request.

- [x] 6.1 Replace `andWaitForEventOfType(MemberCreatedEvent).toArriveAndVerify(...)` with `andWaitForStateChange(() -> memberAccountRepository.findById(memberId)).andVerify(...)` so the test waits for the account to actually exist; both assertions (account present, balance zero) preserved, no production code change, no retry/sleep; 8 consecutive runs green
