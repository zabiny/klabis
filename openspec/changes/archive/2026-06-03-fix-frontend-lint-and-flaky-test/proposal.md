## Why

The CI pipeline has two pre-existing red checks on `main` that are unrelated to any
single feature and block clean PR signal:

- **Frontend Lint / Test / Build** fails at the ESLint step with **355 problems
  (348 errors)**. A clean `main` checkout reproduces the exact same count, so every PR
  inherits a red frontend check regardless of its own changes. This masks real
  regressions and erodes trust in CI.
- **Gradle Tests** intermittently fails on `AuthorizationServerPromptNoneTest` with
  `java.sql.SQLTransientConnectionException` (Hikari connection pool exhausted). The
  test logic is correct; the failure is a flaky test-infrastructure issue.

Both are internal quality/tooling problems with no user-facing behavior change.

## What Changes

- Fix all current frontend ESLint errors so `npm run lint` exits clean:
  - `271×` `@typescript-eslint/no-explicit-any` — replace `any` with accurate types
    (generated API types, proper generics, `unknown` + narrowing, or precise local
    interfaces). Each replacement must preserve runtime behavior.
  - `56×` `no-useless-escape` — remove redundant escape characters in regex/string
    literals.
  - `7×` `@typescript-eslint/no-unused-vars` — remove or prefix unused bindings.
  - `2×` `@typescript-eslint/no-require-imports` — convert `require()` to ESM imports
    in `tailwind.config.ts`.
  - `1×` `prefer-const` — use `const` for the never-reassigned binding.
- Stabilize `AuthorizationServerPromptNoneTest` so the Hikari pool is not exhausted
  during the test run (e.g. correct datasource/pool configuration or connection
  lifecycle in the test setup), eliminating the intermittent
  `SQLTransientConnectionException` without weakening the assertions.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/users/spec.md` and `openspec/specs/non-functional-requirements/spec.md`
  — the closest specs to the authorization-server `prompt=none` behavior. Unaffected:
  the `prompt=none → HTTP 400` behavior under test is **not changed**; only the test's
  connection-pool stability is fixed so the existing assertion runs reliably.
- No spec under `openspec/specs/` mentions ESLint, TypeScript typing, or lint rules —
  frontend lint cleanup is purely internal code hygiene.

**Why no spec update is needed:**
The ESLint fixes are type-level and syntactic: replacing `any` with precise types,
dropping redundant escapes, unused variables, `require()`→`import`, and `let`→`const`.
None of these alter API request/response shapes, status codes, validation, authorization,
or any user-observable outcome. The `no-explicit-any` work is constrained to types that
reflect the values already flowing at runtime, so emitted JavaScript behavior is
unchanged. The backend change is test-only (test fixture/configuration), not production
code, and preserves the asserted behavior.

## Impact

- **Frontend:** many files touched by the `no-explicit-any` cleanup (types only), plus
  `tailwind.config.ts`; no change to bundled runtime behavior. After this change
  `npm run lint` passes and the **Frontend Lint** CI check goes green.
- **Backend:** `AuthorizationServerPromptNoneTest` (and possibly its test
  datasource/pool config) only; production code unaffected. **Gradle Tests** CI becomes
  reliable.
- **Developer workflow:** trustworthy CI signal; lint no longer reports a baseline of
  348 errors, so new violations are visible.
- **No database migration, no API change, no behavior change.**
