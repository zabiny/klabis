#!/bin/bash
# Installed by the agentic-sdlc install skill. Installs the pinned OpenSpec CLI that the
# SDLC agents call. Cloud sessions only; local sessions are expected to have it installed.
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

# The OpenSpec version the plugin's track schemas were generated with.
OPENSPEC_VERSION="1.14.2"

if command -v openspec >/dev/null 2>&1 \
  && [ "$(openspec --version 2>/dev/null)" = "$OPENSPEC_VERSION" ]; then
  exit 0
fi

npm install -g --no-fund --no-audit "@fission-ai/openspec@${OPENSPEC_VERSION}" >&2
