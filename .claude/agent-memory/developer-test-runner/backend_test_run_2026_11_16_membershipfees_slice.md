---
name: backend-test-run-2026-11-16-membershipfees-slice
description: membershipfees package slice (deepen-fee-year-lifecycle) 358/358 passed; 120 result XMLs, ~1 min wall clock
metadata:
  type: project
---

Run of the `com.klabis.membershipfees.*` test slice on branch `deepen-fee-year-lifecycle` (2026-11-16) finished GREEN.

- Command: `run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend "com.klabis.membershipfees.*"`
- Result: 358/358 passed, 0 failed, 0 skipped; 120 test-class XMLs, all under `com.klabis.membershipfees.*`
- Wall clock: ~1 minute (single turn, well under a 30-min budget)

- Re-run same day (17:13) after the FeeYearLifecycleService implementation: identical 358/358, 120 XMLs, ~1 min — result XMLs confirmed freshly written, not cached.

**Why:** the package wildcard filter works with Gradle `--tests` and correctly scopes to the module — it does not fall back to the full suite (full suite is ~4600 tests).

**How to apply:** for module slices use `"com.klabis.<module>.*"` as the single filter argument; a 30-minute Bash timeout is plenty, 10 minutes would also do. See [[backend-test-run-2026-11-16-fee-year-lifecycle-green]] for the single-class timing baseline (~35s).
