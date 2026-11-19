## 1. Baseline

- [x] 1.1 Run the backend `membershipfees` tests and frontend membership-fee vitest files on clean main; record results (known pre-existing failures excluded) so later "no change" is comparable
- [x] 1.2 Collect the rename inventory: `grep -rniE "tier" backend/src docs/openapi frontend/src frontend/e2e` and the `level` occurrences that denote the catalog item; verify the list matches the naming map in design.md D1

## 2. OpenAPI spec and generated code

- [x] 2.1 Rename paths, operation ids, schema names, HAL template names, `x-hal-input-type` values and descriptions in `docs/openapi/spec/membershipfees.yaml` (and references in `klabis.yaml` and other module specs) per design.md D1; run `./gradlew openapiBundle -PopenapiCheck` and verify it passes
- [x] 2.2 Run `./gradlew openapiBundle` and `npm run openapi`; verify generated `*Api` interfaces and `klabisApi.d.ts` / `halTypes.ts` contain only the new names

## 3. Backend

- [x] 3.1 Rename classes, ports, services, repositories, exceptions, controllers and mappers in `membershipfees` (IDE rename refactoring) and the references in `members` (ORIS sync adapter, registration options); verify `./gradlew compileJava compileTestJava` succeeds
- [x] 3.2 Rename the error type URI and any Czech-neutral identifiers in the exception handler; verify the handler tests pass
- [x] 3.3 Add a Flyway migration renaming the tables, columns, constraints and indexes per design.md D2 and update the memento/JDBC mapping; verify the repository tests and a startup with `example-data` succeed
- [x] 3.4 Update backend tests only mechanically (names, URLs); run the full backend test suite via the test-runner skill and verify results equal the baseline from 1.1

## 4. Frontend

- [x] 4.1 Rename pages, components, hooks, `KlabisFieldsFactory` case, HAL template names in code (`chooseTier` → `chooseFeeType`, `createTier`, `editTier`, `deleteTier`), tests and SPA routes in `App.tsx`; verify `npm run build` succeeds
- [x] 4.2 Run the frontend vitest files for membership fees and the fields factory (one file per command, with timeout) and verify they pass with only mechanical test changes

## 5. E2E and final verification

- [x] 5.1 Update E2E support and steps (`membershipFeesApi.ts`, `membershipFeesPages.ts`, step files, README mentions) to the new URLs and names; run the membership-fee scenarios with `npm run test:e2e` and verify they pass
- [x] 5.2 Final sweep: `grep -rniE "tier" backend/src docs/openapi frontend/src frontend/e2e` shows nothing except the old migration and `MembershipFeeGroup`-related comments; verify
- [x] 5.3 Run the full suites (`SKIP_OPTIMIZATIONS` + `--rerun-tasks` for the final backend run) and `npm run build`; verify results equal the baseline from 1.1
- [x] 5.4 Do a code review (code-reviewer agent) before committing, per project rules; verify findings are addressed

## Workflow follow-up

- Implement and merge this change before `membership-fee-type-member-page`.
- Archive after merge.
