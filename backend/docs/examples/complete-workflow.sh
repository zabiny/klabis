#!/bin/bash

# complete-workflow.sh
# Complete workflow: Authenticate as admin, create member, and setup password
#
# This script demonstrates the full lifecycle of member registration and password setup.

set -e

BACKEND_URL="${BACKEND_URL:-https://localhost:8443}"

echo "=== Klabis Member Registration & Password Setup Workflow ==="
echo ""

# === STEP 1: Authenticate as Admin ===
echo "Step 1: Authenticating using client credentials..."
echo "  Client ID: klabis-web"
echo "  Client Secret: test-secret-123"
echo ""

TOKEN_RESPONSE=$(curl -s -X POST "$BACKEND_URL/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "klabis-web:test-secret-123" \
  -d "grant_type=client_credentials&scope=MEMBERS:CREATE,MEMBERS:READ")

ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token // empty' 2>/dev/null || echo "")

if [ -z "$ACCESS_TOKEN" ]; then
  echo "Error: Failed to obtain access token"
  echo "Response: $TOKEN_RESPONSE"
  exit 1
fi

echo "  ✓ Access token obtained"
echo ""

# === STEP 2: Create Member ===
echo "Step 2: Creating new member..."
echo ""

MEMBER_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/members" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jan",
    "lastName": "Novak",
    "gender": "MALE",
    "nationality": "CZ",
    "dateOfBirth": "1990-01-15",
    "birthPlace": "Praha",
    "address": {
      "street": "Vinohradska 123",
      "city": "Praha",
      "zipCode": "12000",
      "country": "CZ"
    },
    "contact": {
      "email": "jan.novak@example.com",
      "phone": "+420601123456"
    }
  }')

# Use grep to extract registration number if jq is not available
REG_NUMBER=$(echo "$MEMBER_RESPONSE" | grep -o '"registrationNumber":"[^"]*' | cut -d'"' -f4)

if [ -z "$REG_NUMBER" ]; then
  echo "Error: Failed to create member"
  echo "Response: $MEMBER_RESPONSE"
  exit 1
fi

echo "  ✓ Member created: $REG_NUMBER"
echo "  ✓ Password setup email sent to: jan.novak@example.com"
echo ""

# === STEP 3: List Members ===
echo "Step 3: Listing members..."
echo ""

curl -s -X GET "$BACKEND_URL/api/members?page=0&size=10&sort=lastName,asc" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/hal+json" \
  | grep -o '"registrationNumber":"[^"]*' | cut -d'"' -f4 | while read -r reg_num; do
    echo "  - $reg_num"
  done

echo ""

# === STEP 4: Get Specific Member ===
echo "Step 4: Getting member details for $REG_NUMBER..."
echo ""

MEMBER_DETAILS=$(curl -s -X GET "$BACKEND_URL/api/members/$REG_NUMBER" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Accept: application/hal+json")

FIRST_NAME=$(echo "$MEMBER_DETAILS" | grep -o '"firstName":"[^"]*' | cut -d'"' -f4)
LAST_NAME=$(echo "$MEMBER_DETAILS" | grep -o '"lastName":"[^"]*' | cut -d'"' -f4)
ACCOUNT_STATUS=$(echo "$MEMBER_DETAILS" | grep -o '"accountStatus":"[^"]*' | cut -d'"' -f4)

echo "  Name: $FIRST_NAME $LAST_NAME"
echo "  Registration Number: $REG_NUMBER"
echo "  Account Status: $ACCOUNT_STATUS"
echo ""

# === STEP 5: Simulate Password Setup Request ===
echo "Step 5: Requesting password setup token..."
echo "  (In production, user receives this link via email)"
echo ""

REQUEST_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/auth/password-setup/request" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"jan.novak@example.com\"}")

echo "  ✓ Password setup requested"
echo "  ✓ Check email for password setup link"
echo ""

echo "=== Workflow Complete ==="
echo ""
echo "Summary:"
echo "  - Admin authenticated: ✓"
echo "  - Member created: ✓ (registration number: $REG_NUMBER)"
echo "  - Password setup email sent: ✓"
echo ""
echo "Next steps:"
echo "  1. User checks email for password setup link"
echo "  2. User clicks link and sets password"
echo "  3. User account becomes ACTIVE"
echo "  4. User can log in with their credentials"
