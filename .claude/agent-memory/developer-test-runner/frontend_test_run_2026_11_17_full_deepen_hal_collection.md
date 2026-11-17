---
name: frontend-test-run-2026-11-17-full-deepen-hal-collection
description: Full frontend suite on branch deepen-hal-collection — 2933/2936 passed, 1 real failure in MemberDetailPage.test.tsx, 2 it.todo; script lists todo as failures
metadata:
  type: project
---

Full frontend Vitest run (2026-11-17, branch `deepen-hal-collection`, root `/home/davca/Documents/Devel/klabis/frontend`):
172 test files (927 suites), 2936 tests — 2933 passed, 1 failed, 2 `it.todo`, 0 skipped. ~32 s
(vitest startTime 01:02:39 → results written 01:03:11).

**Why:** final whole-SPA verification of the HAL collection refactor branch.

**How to apply:**
- `run-frontend-tests.sh` lists `it.todo` tests as FAILED (File `SuspensionWarningDialog.test.tsx`, 2 todos).
  Cross-check `.tmp/vitest-results.json` (`numFailedTests`) before believing the failure count — the
  script's "FAILED TESTS" list can include status `todo`.
- Real failure: `src/pages/members/MemberDetailPage.test.tsx:1057` — page test mocks
  `useAuthorizedQuery` ignoring `options.select`, but `useLegalGuardians` now goes through
  `useHalCollection`, which maps rows inside `select`; the mock returns the raw envelope so
  `data.items` is `undefined` → guardians render empty. Hook's own test applies `select`, so it passes.
