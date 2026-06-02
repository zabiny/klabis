## 1. Easy & mechanical lint rules

- [x] 1.1 Run `eslint --fix` for auto-fixable rules; review the diff so only `no-useless-escape` (56) and `prefer-const` (1) changed (D2)
- [x] 1.2 Convert the two `require()` calls in `tailwind.config.ts` to ESM imports (`no-require-imports`) (D2)
- [x] 1.3 Remove or prefix the 7 `no-unused-vars` bindings (D2)
- [x] 1.4 Confirm `npm run lint` error count dropped by ~66 (only `no-explicit-any` remaining) and `npm run build` still type-checks; commit

## 2. Eliminate no-explicit-any (batched)

- [x] 2.1 Inventory the 271 `no-explicit-any` occurrences grouped by file/area (D3)
- [x] 2.2 Batch 1: fix `any` in the first file group with accurate types — generated API types / generics / `unknown`+narrowing, no `eslint-disable`, no `as any` relocation (D1); gate with `tsc` build + affected tests + eslint; commit
- [x] 2.3 Batch 2: next file group, same rules and gate (D1, D3); commit
- [ ] 2.4 Batch 3: next file group, same rules and gate (D1, D3); commit
- [ ] 2.5 Continue batching until `no-explicit-any` count is 0 (add commits as needed, one per area); each batch must keep the whole-frontend build green (D3)
- [ ] 2.6 Confirm `npm run lint` exits 0 with zero errors

## 3. Stabilize AuthorizationServerPromptNoneTest

- [ ] 3.1 Diagnose the `SQLTransientConnectionException` root cause (Hikari pool exhaustion under full-context `@SpringBootTest` + per-test `bootstrapDataLoader.run`) (D4)
- [ ] 3.2 Fix the connection lifecycle / test datasource-pool configuration so the test reliably acquires a connection — without weakening the four assertions and without retry-until-pass (D4)
- [ ] 3.3 If the root cause is production connection handling rather than test-only, STOP and reassess (would be functional → spec-driven) (D4 risk)
- [ ] 3.4 Run `AuthorizationServerPromptNoneTest` repeatedly (e.g. several consecutive runs) to confirm it no longer flakes; commit

## 4. Verification

- [ ] 4.1 Frontend: `npm run lint` exits 0 and `npm run build` passes (test-runner)
- [ ] 4.2 Backend: the authorization-server tests and the broader suite pass; only the pre-existing unrelated `testdomain` failures (ModularEventsTest / EventLoggingTests) remain, if still present on main (test-runner)
- [ ] 4.3 Push branch, open PR, confirm Frontend Lint and Gradle Tests CI checks are green
