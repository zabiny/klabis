## ADDED Requirements

### Requirement: HTTPS Protocol Enforcement

The system SHALL use HTTPS protocol on port 8443 for all HTTP communications in all profiles (dev, test, prod). HTTP on
port 8080 SHALL NOT be supported.

#### Scenario: Server starts with HTTPS enabled on port 8443

- **WHEN** application starts with any profile (dev, test, prod)
- **THEN** server listens on port 8443 with HTTPS enabled
- **AND** server does NOT listen on port 8080
- **AND** health check endpoint is accessible at `https://localhost:8443/actuator/health`

#### Scenario: HTTP request to port 8080 is rejected

- **WHEN** client attempts to connect to `http://localhost:8080`
- **THEN** connection is refused or times out
- **AND** client receives no response

#### Scenario: HTTPS request to port 8443 succeeds

- **WHEN** client sends HTTPS request to `https://localhost:8443/actuator/health`
- **THEN** server responds with 200 OK
- **AND** response is transmitted over encrypted TLS connection

### Requirement: HTTP/2 Protocol Support

The system SHALL enable HTTP/2 protocol for all HTTPS connections to improve performance through multiplexing, header
compression, and server push capabilities.

#### Scenario: HTTP/2 is enabled for HTTPS connections

- **WHEN** client connects via HTTPS to `https://localhost:8443`
- **THEN** server advertises HTTP/2 support via ALPN protocol negotiation
- **AND** client can use HTTP/2 for subsequent requests
- **AND** server responds with HTTP/2 status code and headers

#### Scenario: HTTP/2 with h2c (cleartext) is disabled

- **WHEN** client attempts to use HTTP/2 without TLS (h2c)
- **THEN** server rejects the connection
- **AND** only HTTP/2 over TLS (h2) is supported

#### Scenario: HTTP/1.1 fallback for legacy clients

- **WHEN** client does not support HTTP/2
- **THEN** server falls back to HTTP/1.1 over TLS
- **AND** request is processed successfully
- **AND** client receives proper HTTP/1.1 response

### Requirement: SSL Certificate Configuration

The system SHALL configure SSL/TLS certificates via Spring Boot application properties with profile-specific keystore
settings.

#### Scenario: Development profile uses development keystore

- **WHEN** application starts with dev profile
- **THEN** server loads SSL certificate from `classpath:keystore/dev-keystore.p12`
- **AND** server uses keystore password configured in application.yml
- **AND** server uses certificate with alias `dev-klabis`

#### Scenario: Test profile uses test keystore

- **WHEN** application starts with test profile
- **THEN** server loads SSL certificate from `classpath:keystore/test-keystore.p12`
- **AND** server uses keystore password configured in application.yml
- **AND** server uses certificate with alias `test-klabis`

#### Scenario: Production profile uses environment-configured keystore

- **WHEN** application starts with prod profile
- **THEN** server loads SSL certificate from path specified in `${SSL_KEYSTORE_PATH}` environment variable
- **AND** server uses keystore password from `${SSL_KEYSTORE_PASSWORD}` environment variable
- **AND** server uses certificate alias from `${SSL_KEY_ALIAS}` environment variable
- **AND** application fails to start if environment variables are missing

#### Scenario: Invalid keystore path causes startup failure

- **WHEN** keystore file does not exist at configured path
- **THEN** application fails to start with clear error message
- **AND** error message indicates missing keystore file and expected path

### Requirement: OAuth2 HTTPS Configuration

The system SHALL configure OAuth2 Authorization Server and all OAuth2-related URLs to use HTTPS protocol with port 8443.

#### Scenario: OAuth2 issuer URL uses HTTPS

- **WHEN** OAuth2 authorization server initializes
- **THEN** issuer URL is configured as `https://localhost:8443`
- **AND** JWT tokens include issuer claim with HTTPS URL
- **AND** JWKS endpoint is accessible at `https://localhost:8443/.well-known/jwks.json`

#### Scenario: OAuth2 redirect URIs use HTTPS

- **WHEN** OAuth2 client is registered during bootstrap
- **THEN** redirect URIs are configured with HTTPS protocol
- **AND** redirect URIs include `https://localhost:8443/auth/callback.html`
- **AND** redirect URIs include `https://localhost:8443/callback.html` (for static frontend)

#### Scenario: OAuth2 authorization code flow with HTTPS

- **WHEN** user initiates OAuth2 authorization code flow at `https://localhost:8443/oauth2/authorize`
- **THEN** authorization request is processed over HTTPS
- **AND** redirect to callback URL uses HTTPS protocol
- **AND** token endpoint `https://localhost:8443/oauth2/token` accepts code exchange over HTTPS

#### Scenario: Post-logout redirect URIs use HTTPS

- **WHEN** user initiates logout
- **THEN** post-logout redirect URI is configured with HTTPS protocol
- **AND** redirect to `https://localhost:8443` after successful logout

### Requirement: Application Base URLs

The system SHALL configure all application base URLs (email links, API documentation, frontend integration) to use HTTPS
protocol with port 8443.

#### Scenario: Password setup email links use HTTPS

- **WHEN** password setup email is generated
- **THEN** email contains link with HTTPS protocol
- **AND** link points to `https://localhost:8443/auth/password-setup.html?token=...`

#### Scenario: Member activation email links use HTTPS

- **WHEN** member activation email is generated
- **THEN** email contains link with HTTPS protocol
- **AND** link points to `https://localhost:8443/...` (activation endpoint)

#### Scenario: OpenAPI documentation uses HTTPS server URL

- **WHEN** OpenAPI documentation is generated
- **THEN** server URL is configured as `https://localhost:8443`
- **AND** Swagger UI uses HTTPS for API requests

#### Scenario: Frontend CORS allows HTTPS origins

- **WHEN** frontend makes request from HTTPS origin
- **THEN** CORS configuration allows `https://localhost:8443` origin
- **AND** CORS configuration allows credentials for HTTPS origin

### Requirement: Development Certificate Generation

The system SHALL provide tooling and documentation for generating self-signed SSL certificates for local development.

#### Scenario: Generate development certificate using provided script

- **WHEN** developer runs `generate-dev-cert.sh` script
- **THEN** script generates PKCS12 keystore file at `src/main/resources/keystore/dev-keystore.p12`
- **AND** keystore contains self-signed certificate with CN=localhost
- **AND** certificate is valid for localhost, 127.0.0.1, and ::1
- **AND** certificate has validity period of 365 days
- **AND** certificate uses key size of 2048 bits

#### Scenario: Development certificate trust documentation available

- **WHEN** developer accesses README.md
- **THEN** documentation includes instructions for trusting self-signed certificate
- **AND** instructions cover Linux (update-ca-certificates)
- **AND** instructions cover macOS (Keychain Access)
- **AND** instructions cover Windows (Certificate Manager)
- **AND** instructions include browser-specific steps (Chrome, Firefox, Edge)

#### Scenario: Keystore files excluded from version control

- **WHEN** keystore files are generated
- **THEN** `.gitignore` includes pattern `*.keystore` and `*.p12`
- **AND** development keystores are NOT committed to repository
- **AND** test keystore for CI IS committed to repository

### Requirement: Production Certificate Configuration

The system SHALL support production-grade SSL certificates configured via environment variables without hardcoded paths
or passwords.

#### Scenario: Production uses external certificate file

- **WHEN** production deployment starts
- **THEN** keystore path is loaded from `${SSL_KEYSTORE_PATH}` environment variable
- **AND** keystore password is loaded from `${SSL_KEYSTORE_PASSWORD}` environment variable
- **AND** certificate alias is loaded from `${SSL_KEY_ALIAS}` environment variable
- **AND** no default values are provided for production profile

#### Scenario: Production deployment documentation available

- **WHEN** administrator accesses deployment documentation
- **THEN** documentation describes process for obtaining production SSL certificates
- **AND** documentation describes certificate formats supported (PKCS12, JKS)
- **AND** documentation includes example environment variable configuration
- **AND** documentation includes certificate renewal procedures

#### Scenario: Missing production certificate configuration fails startup

- **WHEN** production profile starts without SSL_KEYSTORE_PATH
- **THEN** application fails to start
- **AND** error message indicates missing SSL configuration
- **AND** error message lists required environment variables

### Requirement: TLS Security Configuration

The system SHALL enforce secure TLS settings including minimum protocol version and secure cipher suites.

#### Scenario: Minimum TLS version enforced

- **WHEN** client attempts to connect with TLS 1.0 or TLS 1.1
- **THEN** connection is rejected
- **AND** only TLS 1.2 and TLS 1.3 connections are accepted

#### Scenario: Secure cipher suites configured

- **WHEN** TLS handshake occurs
- **THEN** server prefers strong cipher suites (ECDHE, AES-GCM)
- **AND** server disables weak cipher suites (DES, RC4, MD5)
- **AND** forward secrecy is enabled

#### Scenario: HTTP Strict Transport Security header sent

- **WHEN** client makes HTTPS request
- **THEN** response includes `Strict-Transport-Security` header
- **AND** header value includes `max-age=31536000` (1 year)
- **AND** header value includes `includeSubDomains` directive

### Requirement: Test Environment SSL Support

The system SHALL provide pre-configured SSL certificates for automated testing without requiring manual certificate
generation.

#### Scenario: CI build includes test certificate

- **WHEN** CI pipeline builds application
- **THEN** test keystore is available at `src/test/resources/keystore/test-keystore.p12`
- **AND** test keystore is committed to repository
- **AND** test keystore password is documented in test configuration

#### Scenario: Integration tests use HTTPS

- **WHEN** integration tests run with test profile
- **THEN** TestRestTemplate is configured to trust test certificate
- **AND** integration tests make requests to `https://localhost:8443`
- **AND** tests do not disable SSL verification globally

#### Scenario: MockMvc tests handle HTTPS

- **WHEN** MockMvc tests execute
- **THEN** tests use HTTPS scheme in request builders
- **AND** security context includes HTTPS indicator
- **AND** OAuth2 redirect URIs in test assertions use HTTPS

### Requirement: Documentation and Examples

The system SHALL provide comprehensive documentation and updated examples for HTTPS configuration.

#### Scenario: README includes HTTPS setup instructions

- **WHEN** developer reads README.md
- **THEN** Getting Started section includes HTTPS certificate generation step
- **AND** all curl command examples use `https://localhost:8443`
- **AND** OAuth2 configuration examples use HTTPS URLs
- **AND** health check examples use HTTPS URLs

#### Scenario: HTTP client configuration files updated

- **WHEN** developer opens `http-client.env.json`
- **THEN** baseUrl is configured as `https://localhost:8443`
- **AND** OAuth2 Token URL is `https://localhost:8443/oauth2/token`
- **AND** OAuth2 Auth URL is `https://localhost:8443/oauth2/authorize`
- **AND** Redirect URLs use HTTPS protocol

#### Scenario: All `.http` test files use HTTPS

- **WHEN** developer executes `.http` test files in IntelliJ
- **THEN** all request URLs use HTTPS protocol
- **AND** OAuth2 authentication configuration uses HTTPS endpoints
- **AND** variable substitutions result in HTTPS URLs

#### Scenario: Example scripts updated with HTTPS

- **WHEN** developer runs example scripts in `docs/examples/`
- **THEN** scripts use `https://localhost:8443` as BACKEND_URL
- **AND** OAuth2 token requests use HTTPS endpoint
- **AND** API requests use HTTPS URLs

### Requirement: Migration and Troubleshooting Guide

The system SHALL provide migration guide and troubleshooting documentation for developers transitioning from HTTP to
HTTPS.

#### Scenario: Migration checklist available

- **WHEN** existing developer upgrades to HTTPS-enabled version
- **THEN** documentation includes step-by-step migration checklist
- **AND** checklist includes certificate generation step
- **AND** checklist includes certificate trust step
- **AND** checklist includes environment variable update step
- **AND** checklist includes OAuth2 client configuration update step

#### Scenario: Common SSL issues documented

- **WHEN** developer encounters SSL-related error
- **THEN** troubleshooting guide includes solution for certificate trust issues
- **AND** guide includes solution for browser security warnings
- **AND** guide includes solution for OAuth2 redirect URI mismatches
- **AND** guide includes solution for port conflicts
- **AND** guide includes solution for keystore password errors

#### Scenario: Breaking changes clearly communicated

- **WHEN** developer reads changelog or migration guide
- **THEN** document clearly marks HTTPS as BREAKING CHANGE
- **AND** document explains that HTTP on port 8080 is no longer supported
- **AND** document explains that all clients must update to HTTPS URLs
- **AND** document explains certificate trust requirement for development
