#!/bin/bash

###############################################################################
# Development Certificate Generator for Klabis Backend
#
# This script generates a self-signed PKCS12 keystore with a certificate for
# localhost development. The certificate is suitable for HTTPS development
# and testing on port 8443.
#
# Usage:
#   ./generate-dev-cert.sh [keystore_path]
#
# Arguments:
#   keystore_path  - Optional. Path where keystore should be created.
#                    Default: keystore/dev-keystore.p12
#
# Examples:
#   ./generate-dev-cert.sh
#   ./generate-dev-cert.sh /path/to/custom/keystore.p12
#
# The generated keystore will have:
#   - Format: PKCS12
#   - Alias: localhost
#   - Password: changeit
#   - Validity: 365 days
#   - DN: CN=localhost, OU=Development, O=Klabis, L=Dev, ST=Dev, C=XX
#
# Security Notice:
#   This is a SELF-SIGNED certificate for DEVELOPMENT ONLY.
#   Do NOT use this certificate in production environments.
#   Browsers will show security warnings that must be manually accepted.
#
###############################################################################

set -e  # Exit on error

# Configuration
DEFAULT_KEYSTORE_PATH="keystore/dev-keystore.p12"
KEYSTORE_PASSWORD="changeit"
KEY_ALIAS="localhost"
VALIDITY_DAYS=365
KEY_SIZE="2048"
KEY_ALGORITHM="RSA"
SIGALG="SHA256withRSA"

# Parse arguments
KEYSTORE_PATH="${1:-$DEFAULT_KEYSTORE_PATH}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "Klabis Development Certificate Generator"
echo "=================================="
echo ""

# Create directory if it doesn't exist
KEYSTORE_DIR=$(dirname "$KEYSTORE_PATH")
if [ ! -d "$KEYSTORE_DIR" ]; then
    echo -e "${YELLOW}Creating directory: $KEYSTORE_DIR${NC}"
    mkdir -p "$KEYSTORE_DIR"
fi

# Check if keystore already exists
if [ -f "$KEYSTORE_PATH" ]; then
    echo -e "${YELLOW}Warning: Keystore already exists at $KEYSTORE_PATH${NC}"
    read -p "Overwrite existing keystore? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}Aborted.${NC}"
        exit 1
    fi
    rm -f "$KEYSTORE_PATH"
fi

echo "Generating self-signed certificate..."
echo "  Keystore path: $KEYSTORE_PATH"
echo "  Alias: $KEY_ALIAS"
echo "  Password: $KEYSTORE_PASSWORD"
echo "  Validity: $VALIDITY_DAYS days"
echo "  Key size: $KEY_SIZE bits"
echo ""

# Generate the certificate
keytool -genkeypair \
    -alias "$KEY_ALIAS" \
    -keyalg "$KEY_ALGORITHM" \
    -keysize "$KEY_SIZE" \
    -sigalg "$SIGALG" \
    -validity "$VALIDITY_DAYS" \
    -dname "CN=localhost, OU=Development, O=Klabis, L=Development, ST=Dev, C=XX" \
    -ext "SAN=DNS:localhost,IP:127.0.0.1,IP:::1" \
    -ext "BC=ca:true" \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$KEYSTORE_PASSWORD" \
    -keypass "$KEYSTORE_PASSWORD" \
    -noprompt

if [ $? -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ Certificate generated successfully!${NC}"
    echo ""
    echo "Certificate details:"
    keytool -list -v -keystore "$KEYSTORE_PATH" -storepass "$KEYSTORE_PASSWORD" -alias "$KEY_ALIAS"
    echo ""
    echo "=================================="
    echo "Next Steps:"
    echo "=================================="
    echo ""
    echo "1. Trust this certificate in your browser:"
    echo "   - Chrome/Edge: Settings > Privacy and security > Security > Manage certificates"
    echo "   - Firefox: Settings > Privacy & Security > Certificates > View Certificates"
    echo "   - Import: $KEYSTORE_PATH"
    echo "   - Password: $KEYSTORE_PASSWORD"
    echo ""
    echo "2. Configure application.yml to use this keystore:"
    echo "   server:"
    echo "     ssl:"
    echo "       key-store: $KEYSTORE_PATH"
    echo "       key-store-password: $KEYSTORE_PASSWORD"
    echo "       key-alias: $KEY_ALIAS"
    echo ""
    echo "3. Start the application:"
    echo "   ./gradlew bootRun"
    echo ""
    echo "4. Access the application at: https://localhost:8443"
    echo ""
    echo "See README.md for detailed instructions on trusting certificates."
    echo ""
else
    echo -e "${RED}✗ Failed to generate certificate${NC}"
    exit 1
fi
