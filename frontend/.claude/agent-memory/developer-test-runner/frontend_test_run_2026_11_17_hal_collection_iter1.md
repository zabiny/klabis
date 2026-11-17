---
name: frontend-test-run-2026-11-17-hal-collection-iter1
description: Iteration 1 verification of deepen-hal-collection — 4 event/HAL test files, all green (203/203 total)
metadata:
  type: project
---

Four-file frontend Vitest slice for the `deepen-hal-collection` branch (2026-11-17), one file per `run-frontend-tests.sh` command, sequential foreground:

- `src/hooks/useHalCollection.test.tsx` — 13/13 passed
- `src/hooks/useEventTypes.test.ts` — 4/4 passed
- `src/pages/events/EventsPage.test.tsx` — 59/59 passed
- `src/pages/events/EventDetailPage.test.tsx` — 127/127 passed

**Why:** per-file runs are required because this project's hook tests can hang (see the main repo note `frontend_test_hang_2026_09_30.md`), and one bad file would otherwise stall the whole slice.

**How to apply:** frontend file-scoped runs finish in seconds to ~1–2 min each; a 900000 ms Bash timeout is ample. Pass the repo-relative test path as the filter argument.
