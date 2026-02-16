#!/bin/bash
# Run backend tests with optional filtering
# Usage:
#   ./run-backend-tests.sh                         # Run all backend tests
#   ./run-backend-tests.sh "ClassName"             # Run specific test class
#   ./run-backend-tests.sh "ClassName.methodName"  # Run specific test method

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)/backend"
TEST_FILTER="${1:-}"

cd "$BACKEND_DIR" || exit 1

# Clean previous test results
rm -rf build/test-results/test

# Build gradle command
if [ -n "$TEST_FILTER" ]; then
    ./gradlew test --tests "$TEST_FILTER" 2>&1 || true
else
    ./gradlew test 2>&1 || true
fi
