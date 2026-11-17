---
name: frontend-test-run-2026-11-17-iteration2-six-files
description: Frontend Vitest iteration 2 verification (6 files on deepen-hal-collection) — 65/65 passed, ~3s per file
metadata:
  type: project
---

Iteration 2 verification on branch `deepen-hal-collection`, six files run one per command with `run-frontend-tests.sh --root /home/davca/Documents/Devel/klabis/frontend`:

- `src/hooks/useMembershipFeeTierOptions.test.ts` — 4/4
- `src/hooks/useLegalGuardians.test.tsx` — 3/3
- `src/hooks/useUpcomingDeadlines.test.ts` — 6/6
- `src/hooks/useMyUpcomingRegistrations.test.ts` — 3/3
- `src/components/dashboard/UpcomingDeadlinesWidget.test.tsx` — 19/19
- `src/pages/HomePage.test.tsx` — 30/30

Total 65/65, all green, no failures. Each single-file run completes in roughly 3-5 s of Vitest execution (build step dominates); 900 s Bash timeout was ample.

**Why:** the parent agent wanted per-file granularity for the iteration 2 slice, so a single combined filter was deliberately not used.

**How to apply:** a Vitest file-path filter (e.g. `"src/hooks/useLegalGuardians.test.tsx"`) reliably selects just that file; a bare hook name also works but may match more than one file — prefer the full relative path when per-file counts must be exact. See [[frontend-test-run-hang-2026-09-30]] for the one case where a per-file run hung.
