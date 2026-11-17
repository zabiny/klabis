---
name: backend-test-run-2026-11-16-full-final-after-mdc-fix
description: Final full backend suite on deepen-fee-year-lifecycle after MDC leak fix — 4899 tests, 0 failures, 14 skipped, fully green
metadata:
  type: project
---

Final full backend suite (`SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`, `--rerun-tasks`, single foreground-turned-background Gradle invocation) on branch `deepen-fee-year-lifecycle`, 2026-11-16, after review fixes plus the MDC leak fix in `com.klabis.common.logging.RequestLoggingFilter`.

- Result: 4899 tests, 4899 passed, 0 failures, 0 errors, 14 skipped → fully green.
- The previously failing `MdcFilterIntegrationTest.shouldCleanUpMDCAfterRequest` (expected null, was `{"userId"="admin"}`) now passes; the whole class XML has 1 test, 0 failures.
- Wall clock: ~6 min (foreground 180 s then finished in background). 1800000 ms timeout is adequate for this suite.
- Skipped tests come from `@Disabled`/modulith optimization skips; the script's summary line does not print skipped, so count it from the JUnit XML attributes when the caller asks for skipped.

Related: [[backend-test-run-2026-11-16-full-final-modulith-skip]]
