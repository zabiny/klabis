#!/bin/bash
# PreToolUse hook for Bash: blocks starting a new Gradle test/check/build/verify/package
# task while another Gradle test run is already in progress (prevents result overwrites).

input=$(cat)
command=$(echo "$input" | jq -r '.tool_input.command // empty')
project_dir=$(echo "$input" | jq -r '.cwd // empty')
[ -z "$project_dir" ] && project_dir="$CLAUDE_PROJECT_DIR"

if ! echo "$command" | grep -qE '(^|[^a-zA-Z])gradle(w)?[^a-zA-Z].*(test|check|build|verify|package)'; then
    exit 0
fi

if checkGradleRunningTests.sh -d "$project_dir" -s; then
    jq -n '{"hookSpecificOutput": {"hookEventName": "PreToolUse", "permissionDecision": "deny", "permissionDecisionReason": "❌ BLOCKED: Another Gradle test task is already running in this project. Wait for it to finish before starting a new one."}}'
    exit 0
fi

exit 0
