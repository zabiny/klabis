## Context

`npm run lint` reports 355 problems (348 errors) on a clean `main`; the dominant rule is
`@typescript-eslint/no-explicit-any` (271). `AuthorizationServerPromptNoneTest` is a
`@SpringBootTest` (full context) that re-runs `bootstrapDataLoader.run(...)` in `@BeforeEach`
and intermittently fails with `SQLTransientConnectionException` (Hikari pool exhausted).
Both are quality issues, not behavior bugs. The risk in this work is not *what* to do but
doing it *without changing behavior* — especially the `any` removals.

## Goals / Non-Goals

**Goals:**
- `npm run lint` exits 0; Frontend Lint CI green.
- `AuthorizationServerPromptNoneTest` passes reliably; Gradle Tests CI green.
- Zero change to runtime behavior, emitted bundle semantics, or test assertions.

**Non-Goals:**
- No new lint rules, no ESLint config tightening/loosening (only fix existing violations).
- No production-code change for the backend fix (test infrastructure only).
- No refactoring beyond what each lint rule requires.
- No attempt to fix lint warnings (7) that are not errors, unless trivially adjacent.

## Decisions

### D1: Replace `any` with accurate types, never silence the rule
For each `no-explicit-any`, introduce the precise type the value already has at runtime:
generated API types (`components['schemas'][...]`), proper generics, or `unknown` followed
by narrowing. Forbidden shortcuts: `eslint-disable` comments, `as any` relocation, or
widening that changes inference elsewhere. If a precise type is genuinely unknowable,
prefer `unknown` + a guarded narrow over `any`. This keeps emitted JS identical while
satisfying the rule.

### D2: Mechanical rules via `eslint --fix`, then audited
Run `eslint --fix` for the auto-fixable rules (`no-useless-escape`, `prefer-const`) and
review the diff. `no-require-imports` (tailwind.config.ts) and `no-unused-vars` are fixed
by hand (ESM import conversion / removing dead bindings). Mechanical, low risk.

### D3: Batch the `any` cleanup by file/area, gate each batch
271 occurrences across many files is too large for one pass. Split into batches by file
group (delegated to frontend-developer), and after each batch run `tsc` build + the
affected tests + `eslint` to confirm the count strictly decreases and nothing regresses.
This bounds blast radius and keeps each commit independently verifiable.

### D4: Fix the flaky test at the connection-lifecycle layer, not by weakening it
Diagnose why the pool is exhausted (full-context `@SpringBootTest` + per-test
`bootstrapDataLoader.run` likely leaking or over-allocating connections under the `test`
profile). Fix via correct connection handling / test datasource-pool configuration so the
test reliably gets a connection. The four assertions (HTTP 400 / login_required /
no `/login` / no Location header) MUST remain unchanged — do not add retries that mask a
real leak, and do not relax expectations.

### D5: Land as its own change off `main`, not bundled with the CSV PR
This is unrelated to the accommodation-list feature; it ships on its own branch
(`fix/frontend-lint-and-flaky-test`) so the CSV PR's signal is not entangled with the
lint cleanup.

## Risks / Trade-offs

- **[`any` → type change alters inference and breaks a different file]** → Gate each batch
  with `tsc` build over the whole frontend, not just the edited files; a type tightening
  that ripples is caught immediately.

- **[Large diff is hard to review]** → Batched commits per area with a passing build at each
  step; reviewers can read one area at a time.

- **[Flaky-test fix hides the leak instead of fixing it]** → Prefer a root-cause connection
  fix; explicitly forbid retry-until-pass. If the root cause turns out to be production
  connection handling (not test-only), stop and reassess — that would make the change
  functional and require spec-driven treatment.

- **[`eslint --fix` touches unintended lines]** → Review the auto-fix diff before committing;
  only `no-useless-escape` / `prefer-const` should change.
