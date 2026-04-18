# Klabis Test Runner - Agent Memory

## parse-test-output.py Bug Fix (COMPLETED)

### Issue
The JUnit XML parser had a bug that caused it to report 0 tests passed even when tests executed successfully (e.g., 1445 tests completed).

### Root Cause
The parser was looking for testcase elements in nested testsuites only:
```python
for testsuite in root.findall(".//testsuite"):  # Only finds nested testsuites
    for testcase in testsuite.findall("testcase"):
```

However, each XML file's root element IS a `<testsuite>`, not a wrapper. The `.findall(".//testsuite")` finds 0 nested testsuites in most cases.

### Fix Applied
Changed line 83-84 to include the root testsuite:
```python
testsuites = [root] + root.findall(".//testsuite")
for testsuite in testsuites:
    for testcase in testsuite.findall("testcase"):
```

Now correctly reports: BACKEND TEST RESULTS: 1445/1445 passed

### File Modified
`/home/davca/Documents/Devel/klabis/scripts/parse-test-output.py`

## Test Runner Skill Notes

- Use `test-runner-skill` with `--backend` flag to run backend tests
- Use `test-runner-skill` with `--frontend` flag to run frontend tests
- Scripts location: `/home/davca/Documents/Devel/klabis/scripts/`
- Test results XML location: `/home/davca/Documents/Devel/klabis/backend/build/test-results/test/`
- Frontend test results JSON location: `/tmp/claude/vitest-results.json`
- Frontend tests require `/tmp/claude/` directory to exist for JSON output

## Frontend Test Execution (2026-03-11)

### MemberDetailPage Tests - FIXED
- Test date: 2026-03-11 14:35 CET
- Command: `bash scripts/run-frontend-tests.sh "MemberDetailPage"`
- Results: **23/23 passed** - All tests now passing
- Previous issue (mock setup error with useAuthorizedQuery) has been resolved

## Backend Tests - Calendar Module Simplification (2026-03-11)

### Full Suite Execution
- Test date: 2026-03-11 22:58 CET
- Command: `cd backend && ./gradlew test --no-build-cache`
- Results: **1457/1457 passed** - All tests passing, no regressions
- Duration: 2m 19s
- 8 tests skipped (expected)

### Calendar Module Test Results
- **38 test files** in calendar module
- **104 total calendar tests** - all passing
- **0 failures** - no regressions from simplification
- Covers: domain, application, infrastructure (JDBC, REST API), listeners

### Test Coverage Breakdown
- CalendarItemId tests: 6 test files
- Domain CalendarItem tests: 3 test files
- Application CalendarManagementService tests: 4 test files
- Application CalendarEventSync tests: 2 test files
- Infrastructure JDBC tests: 11 test files
- Infrastructure REST API tests: 8 test files
- Event listener tests: 1 test file
- Jackson serialization tests: 1 test file
- Root postprocessor tests: 1 test file

## Events Module - Architecture Violation Fix (2026-03-11)

### Issue
Architecture test failure: `LayerArchitectureTest.Domain layer should not depend on application layer`
- Root cause: `DuplicateRegistrationException` was in `com.klabis.events.application` but thrown from domain layer (`Event.registerMember()`)
- Domain layer cannot have dependencies on application layer (violates DDD principles)

### Solution
Moved exception to domain layer:
- Created: `/backend/src/main/java/com/klabis/events/domain/DuplicateRegistrationException.java`
- Updated imports in:
  - `Event.java` (domain)
  - `EventRegistrationController.java` (infrastructure/restapi)
  - `EventRegistrationControllerTest.java` (infrastructure/restapi test)
  - `EventTest.java` (domain test)
  - `EventRegistrationServiceTest.java` (application test)
- Deleted: `/backend/src/main/java/com/klabis/events/application/DuplicateRegistrationException.java`

### Result
- Full suite execution: **1457/1457 passed** - All tests passing, architecture violation fixed
- No regressions from events module changes
