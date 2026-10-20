#!/usr/bin/env bash
# PreToolUse guard for the e2e-tester agent: it may write only E2E test code, its own memory and temporary files.
file=$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty')
root="${CLAUDE_PROJECT_DIR:-$(pwd)}"
case "$file" in
    /*) ;;
    *) file="$root/$file" ;;
esac
file=$(realpath -m "$file")

case "$file" in
    "$root"/frontend/e2e/* | "$root"/frontend/playwright.config.ts | "$root"/.claude/agent-memory/e2e-tester/* | /mnt/ramdisk/*)
        exit 0 ;;
esac

echo "e2e-tester may not modify $file: only frontend/e2e/**, frontend/playwright.config.ts and its agent memory are writable. Report the needed production change instead." >&2
exit 2
