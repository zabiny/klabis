---
name: frontend-script-todo-misreported-as-failed
description: run-frontend-tests.sh lists it.todo entries under "FAILED TESTS"; verify against numFailedTests in .tmp/vitest-results.json before reporting failures
metadata:
  type: feedback
---

`run-frontend-tests.sh` prints `it.todo` entries under **FAILED TESTS**. Before reporting a
failure to the caller, cross-check `.tmp/vitest-results.json`: `success`, `numFailedTests` and
each assertion's `status` (`todo` vs `failed`).

**Why:** on 2026-11-17 the script reported "2934/2936 passed, FAILED TESTS (2)" while the JSON
said `success: true`, `numFailedTests: 0`, `numTodoTests: 2` — the two entries were `it.todo`
placeholders in `SuspensionWarningDialog.test.tsx`. `--skip-run --full` printed no extra detail
because there were no failure messages to print.

**How to apply:** never pass the script's "FAILED TESTS" list straight through as a red result.
Read the JSON, classify each non-passed assertion by `status`, and report todo/skipped separately
from genuine failures. See [[frontend-full-suite-2026-11-17-hal-collection]].
