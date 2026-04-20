#!/usr/bin/env bash
# PreToolUse hook for Bash. Exits 2 (blocking error) if the command calls
# ./gradlew or a standalone `gradle` binary directly. Concurrent Claude
# sandboxes share the host ~/.gradle cache lock, so direct calls deadlock.
# gradle-isolated.sh gives each sandbox its own GRADLE_USER_HOME while
# keeping downloads shared via GRADLE_RO_DEP_CACHE.
#
# Detection is intentionally narrow: a token must be the first word of the
# whole command or of a sub-command (after ;, &&, ||, |, or a subshell). This
# avoids false positives on `grep gradle`, `ls | grep gradle`, etc.

set -euo pipefail

cmd="$(jq -r '.tool_input.command // empty')"
[[ -z "$cmd" ]] && exit 0

# Split on shell separators into individual command starts. Everything between
# shell separators is a candidate; we only check its first token.
normalized="$(printf '%s' "$cmd" | tr ';|&()' '\n')"

while IFS= read -r segment; do
    # Strip leading whitespace and take the first token.
    first="$(printf '%s' "$segment" | sed -E 's/^[[:space:]]+//' | awk '{print $1}')"
    case "$first" in
        ./gradlew|gradle|gradlew)
            cat >&2 <<'EOF'
BLOCKED: Direct ./gradlew (or gradle) calls deadlock when multiple Claude sandboxes share the host ~/.gradle cache lock.

Use ./gradle-isolated.sh instead — same arguments, but per-sandbox GRADLE_USER_HOME plus a shared read-only dependency cache so downloads are not duplicated.

Example:  ./gradle-isolated.sh :backend:test
EOF
            exit 2
            ;;
    esac
done <<< "$normalized"

exit 0
