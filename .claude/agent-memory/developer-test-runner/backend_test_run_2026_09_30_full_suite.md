---
name: backend_test_run_2026_09_30_full_suite
description: Full backend test run on legal-guardians-refactor branch with SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true
metadata:
  type: project
---

**Date:** 2026-09-30
**Branch:** legal-guardians-refactor
**Run:** Full suite with SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true
**Duration:** ~17 minutes (foreground execution)

## Results

- **Total:** 3936
- **Passed:** 3936
- **Failed:** 0
- **Skipped:** 0

### Summary

All tests passed cleanly. No failures detected. No known pre-existing failures (ModularEventsTest, EventLoggingTests) reported.

Command:
```bash
SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true /path/to/run-backend-tests.sh --root /home/davca/Documents/Devel/klabis/backend
```

Notes:
- Java deprecation warnings present (expected, from compiler)
- OpenJDK bootstrap classpath warning (expected)
- Backend app on port 8443 left running (not stopped during tests)
