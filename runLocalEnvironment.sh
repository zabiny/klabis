#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${TMPDIR:-/tmp}/klabis-local-env"
mkdir -p "$LOG_DIR"

BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

BACKEND_PORT=8443
FRONTEND_PORT=3000

BACKEND_TIMEOUT=120
FRONTEND_TIMEOUT=60

cleanup() {
    if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
        kill "$BACKEND_PID" 2>/dev/null || true
    fi
    if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
        kill "$FRONTEND_PID" 2>/dev/null || true
    fi
}

check_port() {
    lsof -i :"$1" -sTCP:LISTEN >/dev/null 2>&1
}

wait_for_port() {
    local port=$1 timeout=$2 label=$3
    local elapsed=0
    while ! check_port "$port"; do
        if (( elapsed >= timeout )); then
            echo "ERROR: $label did not start within ${timeout}s" >&2
            exit 1
        fi
        sleep 1
        elapsed=$(( elapsed + 1 ))
    done
}

if check_port "$BACKEND_PORT"; then
    echo "ERROR: Port $BACKEND_PORT already in use" >&2
    exit 1
fi
if check_port "$FRONTEND_PORT"; then
    echo "ERROR: Port $FRONTEND_PORT already in use" >&2
    exit 1
fi

trap cleanup EXIT

KLABIS_ADMIN_USERNAME='admin' \
KLABIS_ADMIN_PASSWORD='admin123' \
KLABIS_OAUTH2_CLIENT_SECRET='test-secret-123' \
KLABIS_JASYPT_PASSWORD='test-key-123' \
SPRING_PROFILES_ACTIVE='h2,ssl,debug,metrics,local-dev' \
"$SCRIPT_DIR/backend/gradlew" -p "$SCRIPT_DIR/backend" bootRun \
    >"$BACKEND_LOG" 2>&1 &
# local-dev profile registers klabis-web-local confidential client with refresh_token grant,
# enabling silent token renewal when frontend runs on http://localhost:3000 (cross-origin from backend).
BACKEND_PID=$!

cd "$SCRIPT_DIR/frontend"
# Copy local-dev env example on first run so the frontend uses the confidential klabis-web-local client
if [[ ! -f "$SCRIPT_DIR/frontend/.env.development.local" ]]; then
    cp "$SCRIPT_DIR/frontend/.env.development.local.example" "$SCRIPT_DIR/frontend/.env.development.local"
    echo "Created frontend/.env.development.local from example (local-dev OAuth2 client)"
fi
BROWSER=none npm run dev -- --open false >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
cd "$SCRIPT_DIR"

wait_for_port "$BACKEND_PORT" "$BACKEND_TIMEOUT" "Backend"
wait_for_port "$FRONTEND_PORT" "$FRONTEND_TIMEOUT" "Frontend"

trap - EXIT

printf '{"backendLog":"%s","frontendLog":"%s","backendPid":%d,"frontendPid":%d}\n' \
    "$BACKEND_LOG" "$FRONTEND_LOG" "$BACKEND_PID" "$FRONTEND_PID"
