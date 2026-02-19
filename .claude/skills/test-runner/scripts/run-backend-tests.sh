#!/bin/bash
# Run backend tests with optional filtering and compile-only flag
# Usage:
#   ./run-backend-tests.sh                         # Run all backend tests
#   ./run-backend-tests.sh "ClassName"             # Run specific test class
#   ./run-backend-tests.sh "ClassName.methodName"  # Run specific test method
#   ./run-backend-tests.sh --compile-only           # Compile only, no tests

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)/backend"
COMPILE_ONLY=false
TEST_FILTER=""

# Parse arguments
for arg in "$@"; do
    case $arg in
        --compile-only)
            COMPILE_ONLY=true
            shift
            ;;
        *)
            if [ -z "$TEST_FILTER" ]; then
                TEST_FILTER="$arg"
            fi
            shift
            ;;
    esac
done

cd "$BACKEND_DIR" || exit 1

# Clean previous test results
rm -rf build/test-results/test

# Build gradle command
if [ "$COMPILE_ONLY" = true ]; then
    echo "Compiling backend only (no tests)..."
    ./gradlew compileJava --no-daemon 2>&1 || true
else
    if [ -n "$TEST_FILTER" ]; then
        ./gradlew test --no-daemon --tests "$TEST_FILTER" 2>&1 || true
    else
        ./gradlew test --no-daemon 2>&1 || true
    fi
fi
