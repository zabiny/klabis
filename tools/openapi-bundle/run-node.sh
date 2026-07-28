#!/usr/bin/env bash
# Wrapper so Gradle Exec tasks can run Node scripts regardless of what PATH the
# calling process (e.g. IntelliJ IDEA's own Gradle daemon) was launched with.
# IDE-launched processes don't source the interactive shell profile, so nvm's
# PATH mutation never happens there and a bare "node" call fails with
# "A problem occurred starting process 'command 'node''".
set -uo pipefail

export NVM_DIR="${NVM_DIR:-$HOME/.nvm}"
if [ -s "$NVM_DIR/nvm.sh" ]; then
  # shellcheck disable=SC1091
  \. "$NVM_DIR/nvm.sh"
  # nvm's internal functions can return non-zero on success (PATH-manipulation quirk),
  # so this isn't guarded by `set -e` — only the final `exec node` failing should abort.
  nvm use 24 >/dev/null || true
fi

exec node "$@"
