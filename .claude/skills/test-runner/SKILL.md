---
name: test-runner
description: This skill should be used when the user asks to "run tests", "test backend", "test frontend", "run UserTest", "test --filter=LoginComponent", "check if tests pass", "run all tests", or mentions test execution and report. Supports running all tests or selected tests with detailed failure analysis and stacktrace filtering for klabis project.
context: fork
model: Haiku
version: 0.2.0
allowed-tools: Bash(./.claude/skills/test-runner/scripts/run-backend-tests.sh *), Bash(./.claude/skills/test-runner/scripts/run-frontend-tests.sh *)
---

# Klabis Test Runner

Runs backend (Gradle/JUnit) or frontend (Vitest) tests and displays a formatted report.

**CRITICAL:** ALWAYS execute the test scripts — never skip execution based on previous results or cached state. Tests must run fresh every time this skill is invoked.

## Execution Steps

### 1. Determine test type

- **Backend**: Java test classes, `.java` files, Gradle → backend
- **Frontend**: React/TypeScript, `.tsx` files, Vitest → frontend
- If unclear, ask the user

### 2. Extract optional filter

- `"run all tests"` → no filter
- `"test UserTest"` → filter: `UserTest`
- `"test UserTest.shouldValidateEmail"` → filter: `UserTest.shouldValidateEmail`
- `"test LoginComponent"` → filter: `LoginComponent`

### 3. Run tests

**IMPORTANT:** All Bash commands in steps 3 and 4 MUST use `dangerouslyDisableSandbox: true` — Gradle and npm require network access that the default sandbox blocks (bwrap loopback error).

**Backend:**
```bash
bash ./.claude/skills/test-runner/scripts/run-backend-tests.sh
# or with filter:
bash ./.claude/skills/test-runner/scripts/run-backend-tests.sh "UserTest"
```

**Frontend:**
```bash
bash ./.claude/skills/test-runner/scripts/run-frontend-tests.sh
# or with filter:
bash ./.claude/skills/test-runner/scripts/run-frontend-tests.sh "LoginComponent"
```

### 4. Parse results and display report

**Without filter** (standard mode — filters stacktraces to app code only):
```bash
python3 ./.claude/skills/test-runner/scripts/parse-test-output.py backend
```

**With filter** (full mode — complete output for failed tests):
```bash
python3 ./.claude/skills/test-runner/scripts/parse-test-output.py backend --full
```

Replace `backend` with `frontend` for frontend tests.

### 5. Display ONLY the parser output

Display the report exactly as produced by the parser. Do NOT add any additional text, commentary, or explanation before or after the report.

## How it works

- **Backend**: Script runs `./gradlew test`, parser reads JUnit XML files from `backend/build/test-results/test/`
- **Frontend**: Script runs `npx vitest run --reporter=json`, parser reads JSON from `/tmp/claude/vitest-results.json`
- **Standard mode** (no filter): Stacktraces filtered to `com.klabis.*` app code only
- **Full mode** (with filter): Complete stacktrace output for debugging specific tests
