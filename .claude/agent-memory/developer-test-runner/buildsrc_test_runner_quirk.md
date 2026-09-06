---
name: buildsrc-test-runner-quirk
description: run-backend-tests.sh --module :buildSrc can exit 1 with "No fresh JUnit XML written" even when tests ran and XMLs are fresh; verify with ls and re-parse via --skip-run
metadata:
  type: feedback
---

When running `run-backend-tests.sh --root <backend> --module :buildSrc`, the script may fail with
`ERROR: Backend test execution failed before any tests produced results (gradle exit=0). No fresh
JUnit XML written to .../buildSrc/build/test-results/test` even though Gradle ran the tests and the
XML files were written seconds earlier.

**Why:** the script's freshness heuristic for detecting newly written JUnit XML misfires for the
`buildSrc` module results path; gradle itself exits 0 and results are valid.

**How to apply:** on that error, do not re-run or force `--rerun-tasks` blindly. Check
`ls -la --time-style=full-iso <root>/buildSrc/build/test-results/test/` — if XMLs are newer than the
run start, re-parse with `--skip-run` to get the report. buildSrc codegen suite (2026-09-05):
24 tests / 4 classes, ~2 min wall clock, 30 min timeout is more than enough.
