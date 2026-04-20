#!/usr/bin/env bash
# Per-sandbox isolated Gradle wrapper.
#
# Problem: Claude Code agents can run concurrently in separate sandboxes. They all
# share the host's ~/.gradle cache, which serializes via global locks. When two
# agents call Gradle at the same time, one blocks the other with a "cache lock
# held by PID X" error — and PID X is a process in a different sandbox, so `ps`
# cannot even see it.
#
# Fix: give each sandbox its own GRADLE_USER_HOME on the ramdisk, but let it
# read already-downloaded dependency jars from the host's cache via
# GRADLE_RO_DEP_CACHE. Locks and build caches are per-sandbox; dependency
# downloads are shared.
#
# Sticky sandbox ID: written once to $TMPDIR/.gradle-isolation-id and reused for
# the lifetime of the sandbox, so all calls from the same sandbox share a
# Gradle daemon and build cache. $TMPDIR is cleaned up when the sandbox exits.

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ID_FILE="${TMPDIR:-/tmp}/.gradle-isolation-id"
if [[ -s "$ID_FILE" ]]; then
    SANDBOX_ID="$(cat "$ID_FILE")"
else
    SANDBOX_ID="$(date +%s)-$$-$RANDOM"
    printf '%s' "$SANDBOX_ID" > "$ID_FILE"
fi

export GRADLE_USER_HOME="/mnt/ramdisk/klabis/gradle-${SANDBOX_ID}"
mkdir -p "$GRADLE_USER_HOME"

HOST_GRADLE="${HOME}/.gradle"
if [[ -d "${HOST_GRADLE}/caches/modules-2" ]]; then
    export GRADLE_RO_DEP_CACHE="${HOST_GRADLE}/caches"
fi

exec "${ROOT}/backend/gradlew" --project-dir "${ROOT}/backend" "$@"
