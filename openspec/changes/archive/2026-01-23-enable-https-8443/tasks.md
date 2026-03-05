# Implementation Tasks

## 1. Certificate Setup and Documentation

- [x] 1.1 Create `generate-dev-cert.sh` script for generating self-signed certificates (Uses keytool to generate PKCS12
  keystore with localhost certificate)
- [x] 1.2 Add certificate trust instructions to README.md (Cover Linux, macOS, Windows with browser-specific steps)
- [x] 1.3 Add `.keystore` to `.gitignore` (Prevent accidental commit of certificates)
- [x] 1.4 Create example keystore for CI/testing purposes (Committed test-only certificate with known password)

## 2. Application Configuration

- [x] 2.1 Update `application.yml` with server.ssl configuration
    - Add server.port: 8443
    - Add server.ssl.enabled: true
    - Add server.ssl.key-store configuration (Profile-specific paths)
    - Add server.ssl.key-store-password configuration
    - Add server.ssl.key-alias configuration
- [x] 2.2 Enable HTTP/2 protocol support
    - Add server.http2.enabled: true
    - Configure with ALPN (Application-Layer Protocol Negotiation)
    - Ensure HTTP/1.1 fallback for legacy clients
- [x] 2.3 Configure profile-specific SSL settings
    - dev profile: Use development keystore
    - test profile: Use test keystore
    - prod profile: Use environment-variable-driven keystore path
- [x] 2.4 Update OAuth2 issuer URLs in application.yml (Change http://localhost:8080 to https://localhost:8443)
- [x] 2.5 Update base URLs for email links (KLABIS_BASE_URL and APP_BASE_URL to HTTPS)
- [x] 2.6 Update frontend CORS allowed origins (Add https://localhost:8443 and profile-specific HTTPS origins)

## 3. Code Updates - OAuth2 Configuration

- [x] 3.1 Update `SecurityConfiguration.java` issuer default value (Change from http://localhost:8080
  to https://localhost:8443)
- [x] 3.2 Update `AuthorizationServerConfiguration.java` issuer default value
- [x] 3.3 Update `BootstrapDataLoader.java` redirect URIs (Change all http://localhost:8080 to https://localhost:8443)
- [x] 3.4 Update `BootstrapDataLoader.java` post-logout redirect URIs
- [x] 3.5 Update `OpenApiConfig.java` server URL (Change from http://localhost:8080 to https://localhost:8443)

## 4. Code Updates - Application Services

- [x] 4.1 Update `PasswordSetupService.java` base URL default (Change from http://localhost:8080
  to https://localhost:8443)

## 5. Test Updates

- [x] 5.1 Update test configuration classes with HTTPS URLs (`TestConfigurationHelper.java`)
- [x] 5.2 Update integration tests to use HTTPS TestRestTemplate configuration
- [x] 5.3 Add SSL context configuration for tests (Trust self-signed certificates in test environment)
- [x] 5.4 Update test data and assertions with HTTPS URLs

## 6. HTTP Client Configuration

- [x] 6.1 Update `http-client.env.json` OAuth2 configuration (baseUrl, apiBaseUrl, Token URL, Auth URL, Redirect URL to
  HTTPS)
- [x] 6.2 Update all `.http` files with HTTPS URLs
- [x] 6.3 Test OAuth2 flow with IntelliJ HTTP Client (Verify authorization code and client credentials flows work with
  HTTPS)

## 7. Documentation Updates

- [x] 7.1 Update `README.md` with HTTPS setup instructions
    - Getting Started section
    - Environment configuration
    - OAuth2 configuration examples
    - API endpoint examples (curl commands)
    - Health check endpoints
- [x] 7.2 Update `CLAUDE.md` with HTTPS references
    - Server management commands
    - Health check URLs
    - OAuth2 testing examples
- [x] 7.3 Update `.env.example` with HTTPS environment variable examples
- [x] 7.4 Update `docs/API.md` with HTTPS endpoint examples
- [x] 7.5 Update `docs/HATEOAS-GUIDE.md` with HTTPS examples
- [x] 7.6 Update `docs/SPRING_SECURITY_ARCHITECTURE.md` OAuth2 flow diagrams
- [x] 7.7 Update `docs/BACKEND_INTEGRATION.md` with HTTPS integration steps
- [x] 7.8 Update `docs/OPERATIONS_RUNBOOK.md` monitoring endpoints
- [x] 7.9 Update `docs/examples/*.sh` scripts with HTTPS URLs

## 8. Frontend Static Files

- [x] 8.1 Update `src/main/resources/static/api-client.js` (Update window.location.origin handling if needed)
- [x] 8.2 Update `src/main/resources/static/docs/*.md` with HTTPS URLs

## 9. Build and Environment

- [ ] 9.1 Update Maven configuration if needed (Ensure spring-boot-maven-plugin works with HTTPS)
- [ ] 9.2 Update CI/CD configuration (Add certificate generation step for CI builds)
- [ ] 9.3 Create deployment documentation for production certificates (Document process for obtaining and configuring
  production SSL certificates)

## 10. Testing and Validation

- [x] 10.1 Test server startup with HTTPS on all profiles (dev, test, prod)
- [x] 10.2 Test OAuth2 authorization code flow end-to-end with HTTPS
- [x] 10.3 Test OAuth2 client credentials flow with HTTPS
- [x] 10.4 Test password setup email links with HTTPS base URL
- [x] 10.5 Test member registration E2E with HTTPS
- [x] 10.6 Test static frontend with HTTPS
- [x] 10.7 Run all integration tests with HTTPS
- [x] 10.8 Verify health check and actuator endpoints accessible via HTTPS
- [x] 10.9 Test browser certificate trust and warning handling
- [x] 10.10 Test HTTP/2 protocol negotiation with ALPN
- [x] 10.11 Verify HTTP/2 is advertised in server response headers
- [x] 10.12 Test HTTP/1.1 fallback for clients without HTTP/2 support
- [x] 10.13 Test HTTP/2 multiplexing with concurrent requests

## 11. Migration Guide

- [x] 11.1 Create migration checklist for existing developers
- [x] 11.2 Document troubleshooting steps for common SSL issues
    - Certificate trust issues
    - Browser security warnings
    - OAuth2 redirect URI mismatches
    - Port conflicts
