#!/bin/bash
# Stops the local Klabis environment (backend on 8443, frontend on 3000)

PIDS=$(lsof -ti :8443 -ti :3000)

if [ -z "$PIDS" ]; then
  echo "Nothing running on ports 8443 or 3000."
  exit 0
fi

kill $PIDS
echo "Stopped PIDs: $PIDS"
