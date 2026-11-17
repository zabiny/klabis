---
name: backend-test-run-2026-11-16-membershipfees-full-slice
description: Whole com.klabis.membershipfees package slice run GREEN 367/367 on deepen-fee-year-lifecycle (2026-11-16)
metadata:
  type: project
---

Run of the entire `membershipfees` backend slice on branch `deepen-fee-year-lifecycle`, 2026-11-16.

- Command: `run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend "com.klabis.membershipfees.*"`
- Result: 367/367 passed, 0 failed (filter auto-enables `--full`)
- Observed wall clock: under ~2 minutes including recompilation after port fold refactor
- Package-wide filter `com.klabis.membershipfees.*` does cover nested packages (`application`, `infrastructure.restapi`, etc.) — use it for whole-module slices instead of enumerating classes.

Related: [[backend-test-run-2026-11-16-membershipfees-slice]] (358 tests, earlier snapshot), [[backend-test-run-2026-11-16-fee-year-lifecycle-final]].
