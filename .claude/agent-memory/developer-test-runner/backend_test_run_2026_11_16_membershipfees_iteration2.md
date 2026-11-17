---
name: backend-test-run-2026-11-16-membershipfees-iteration2
description: Backend membershipfees slice (com.klabis.membershipfees.*) 367/367 green on deepen-fee-year-lifecycle iteration 2, ~2m20s with --rerun-tasks
metadata:
  type: project
---

Run on branch `deepen-fee-year-lifecycle` after iteration 2 of the fee-year-lifecycle deepening: `run-backend-tests.sh --root backend --rerun-tasks "com.klabis.membershipfees.*"` → 367/367 passed, 0 failed, 0 skipped, 121 JUnit XML class files, wall clock ~2m20s.

**Why:** Caller needed the slice verified after code changes; a plain (non-rerun) invocation came back UP-TO-DATE with no fresh XML because an identical-filter run had happened ~1 minute earlier, so `--rerun-tasks` was needed to get a genuine execution.

**How to apply:** For this membershipfees slice budget ~5 min of Bash timeout (observed 2m20s incl. recompile). If the first run reports "no fresh JUnit XML", check whether an identical-filter run just finished — if provenance is unclear, re-run with `--rerun-tasks` rather than reporting the cached XML.
