#!/bin/bash

# bulk-import.sh
# Usage: ./bulk-import.sh members.json
#
# This script demonstrates how to import multiple members from a JSON file.
# Requires: jq (https://stedolan.github.io/jq/)

set -e

JSON_FILE="$1"
BACKEND_URL="${BACKEND_URL:-https://localhost:8443}"

if [ -z "$JSON_FILE" ]; then
  echo "Usage: $0 <members.json>"
  exit 1
fi

if [ ! -f "$JSON_FILE" ]; then
  echo "Error: File '$JSON_FILE' not found"
  exit 1
fi

# Check if jq is installed
if ! command -v jq &> /dev/null; then
  echo "Error: jq is required but not installed."
  echo "Install jq: https://stedolan.github.io/jq/"
  exit 1
fi

# Authenticate
echo "Authenticating..."
TOKEN_RESPONSE=$(curl -s -X POST "$BACKEND_URL/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "klabis-web:test-secret-123" \
  -d "grant_type=client_credentials&scope=MEMBERS:CREATE,MEMBERS:READ")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty')

if [ -z "$ACCESS_TOKEN" ] || [ "$ACCESS_TOKEN" = "null" ]; then
  echo "Error: Failed to obtain access token"
  echo "Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "Access token obtained"

# Import members
echo "Importing members from $JSON_FILE..."

# Parse JSON and import each member
MEMBER_COUNT=$(jq '. | length' "$JSON_FILE")

SUCCESS_COUNT=0
FAILURE_COUNT=0

for i in $(seq 0 $(($MEMBER_COUNT - 1))); do
  MEMBER_DATA=$(jq ".[$i]" "$JSON_FILE")
  FIRST_NAME=$(echo "$MEMBER_DATA" | jq -r '.firstName')
  LAST_NAME=$(echo "$MEMBER_DATA" | jq -r '.lastName')

  echo "Creating member: $FIRST_NAME $LAST_NAME..."

  RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/members" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d "$MEMBER_DATA")

  REG_NUMBER=$(echo "$RESPONSE" | jq -r '.registrationNumber // empty')

  if [ -n "$REG_NUMBER" ] && [ "$REG_NUMBER" != "null" ]; then
    echo "  ✓ Created: $REG_NUMBER"
    ((SUCCESS_COUNT++))
  else
    echo "  ✗ Failed: $RESPONSE"
    ((FAILURE_COUNT++))
  fi
done

echo ""
echo "Import completed"
echo "Success: $SUCCESS_COUNT"
echo "Failed: $FAILURE_COUNT"
