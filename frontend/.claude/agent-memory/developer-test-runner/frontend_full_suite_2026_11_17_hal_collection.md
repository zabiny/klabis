---
name: frontend-full-suite-2026-11-17-hal-collection
description: Full frontend Vitest suite timing (927 files, ~33 s) and the known 2 it.todo placeholders in SuspensionWarningDialog.test.tsx
metadata:
  type: project
---

Full unfiltered frontend Vitest run on `deepen-hal-collection` (2026-11-17) took **~33 s** of
vitest execution (start 01:06:18 → last endTime 01:06:51), 927 test files, 2934 passed,
2 `it.todo` placeholders, 0 real failures. Wrapper script adds `npm run openapi` pre-hook time on
top, so budget ~1–2 min wall clock for the whole `run-frontend-tests.sh` call, not 30 min.

**Why:** the full suite is fast here — there is no need to split it per file for runtime reasons
(per-file splitting is only for hang isolation of hook tests, see
[[frontend-test-run-2026-11-17-hal-collection-iter1]]).

**How to apply:** the two `it.todo` entries in `src/pages/members/SuspensionWarningDialog.test.tsx`
(lines 111–112, tasks 6.7) are permanent placeholders — treat them as todo/skipped, never as
regressions, even though the script prints them under FAILED TESTS ([[frontend-script-todo-misreported-as-failed]]).
