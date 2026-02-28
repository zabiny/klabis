#!/bin/bash
# Run frontend tests with optional filtering
# Usage:
#   ./run-frontend-tests.sh                    # Run all frontend tests
#   ./run-frontend-tests.sh "LoginComponent"   # Run tests matching filter

FRONTEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)/frontend"
TEST_FILTER="${1:-}"
OUTPUT_FILE="/tmp/claude/vitest-results.json"

cd "$FRONTEND_DIR" || exit 1

mkdir -p /tmp/claude

if [ -n "$TEST_FILTER" ]; then
    npx vitest run --reporter=json --outputFile="$OUTPUT_FILE" "$TEST_FILTER" 2>&1 || true
else
    npx vitest run --reporter=json --outputFile="$OUTPUT_FILE" 2>&1 || true
fi

echo "VITEST_JSON_OUTPUT=$OUTPUT_FILE"
