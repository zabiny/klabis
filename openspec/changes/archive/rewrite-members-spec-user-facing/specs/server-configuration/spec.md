## MODIFIED Requirements

### Requirement: HTTPS Protocol Enforcement

The system SHALL use HTTPS on port 8443 when the `ssl` profile is active, and HTTP on port 8080 without the `ssl` profile.

#### Scenario: Application runs on HTTPS when ssl profile is active

- **WHEN** the application starts with the `ssl` profile
- **THEN** the application is accessible via HTTPS on port 8443
- **AND** the HTTP port 8080 is not available

#### Scenario: Application runs on HTTP without ssl profile

- **WHEN** the application starts without the `ssl` profile
- **THEN** the application is accessible via HTTP on port 8080

### Requirement: HTTP/2 Protocol Support

The system SHALL support HTTP/2 for all HTTPS connections. Cleartext HTTP/2 (h2c) is disabled.

#### Scenario: Application supports HTTP/2 over HTTPS

- **WHEN** a client connects via HTTPS and supports HTTP/2
- **THEN** the server uses HTTP/2 for the connection

#### Scenario: Legacy clients use HTTP/1.1 fallback

- **WHEN** a client does not support HTTP/2
- **THEN** the server falls back to HTTP/1.1 and the request is processed normally

### Requirement: SSL Certificate Configuration

The system SHALL configure SSL/TLS certificates via system configuration with profile-specific keystore settings.

#### Scenario: SSL profile loads keystore from configured path

- **WHEN** application starts with the `ssl` profile
- **THEN** the SSL certificate is loaded from the configured keystore path
- **AND** the keystore password and certificate alias are taken from configuration

#### Scenario: Missing keystore file causes startup failure with clear error

- **WHEN** the keystore file does not exist at the configured path
- **THEN** the application fails to start
- **AND** the error message indicates the missing keystore file and its expected path

### Requirement: OAuth2 HTTPS Configuration

The system SHALL configure all OAuth2-related URLs to use HTTPS on port 8443.

#### Scenario: OAuth2 issuer URL uses HTTPS

- **WHEN** the OAuth2 authorization server initializes with the ssl profile
- **THEN** the issuer URL is configured as `https://localhost:8443`
- **AND** all OAuth2 redirect URIs use HTTPS

### Requirement: Application Base URLs

The system SHALL configure all application base URLs (email links, API documentation, frontend integration) to use HTTPS on port 8443.

#### Scenario: Password setup email links use HTTPS

- **WHEN** a password setup email is generated
- **THEN** the email contains an HTTPS link to the activation page

#### Scenario: Member activation email links use HTTPS

- **WHEN** a member activation email is generated
- **THEN** the email contains an HTTPS link to the activation endpoint

### Requirement: Development Certificate Generation

The system SHALL provide tooling for generating self-signed SSL certificates for local development.

#### Scenario: Developer generates a self-signed certificate

- **WHEN** developer runs the certificate generation script
- **THEN** a keystore file is generated with a self-signed certificate for localhost
- **AND** the certificate is valid for localhost, 127.0.0.1, and ::1

#### Scenario: Documentation covers certificate trust setup

- **WHEN** developer reads the README
- **THEN** instructions are available for trusting the self-signed certificate on Linux, macOS, and Windows

### Requirement: Production Certificate Configuration

The system SHALL support production-grade SSL certificates configured via environment variables.

#### Scenario: Production uses external certificate from environment variables

- **WHEN** production deployment starts
- **THEN** the keystore path, password, and alias are loaded from environment variables
- **AND** no default values are provided for the production profile

#### Scenario: Missing production certificate configuration causes startup failure

- **WHEN** production profile starts without the required SSL environment variables
- **THEN** the application fails to start with an error listing the required variables

### Requirement: TLS Security Configuration

The system SHALL enforce secure TLS settings: minimum TLS 1.2, strong cipher suites, and HTTP Strict Transport Security header.

#### Scenario: Outdated TLS versions are rejected

- **WHEN** a client attempts to connect using TLS 1.0 or TLS 1.1
- **THEN** the connection is rejected

#### Scenario: HTTPS responses include HSTS header

- **WHEN** client makes an HTTPS request
- **THEN** the response includes a Strict-Transport-Security header with a 1-year max-age

### Requirement: Test Environment SSL Support

The system SHALL provide pre-configured SSL certificates for automated testing.

#### Scenario: CI pipeline can build and run tests with HTTPS

- **WHEN** the CI pipeline runs integration tests
- **THEN** a test keystore committed to the repository is used
- **AND** tests make requests to `https://localhost:8443`

### Requirement: Documentation and Examples

The system SHALL provide comprehensive documentation for HTTPS setup and updated examples for developers.

#### Scenario: README includes HTTPS setup instructions

- **WHEN** developer reads the README
- **THEN** all example commands use `https://localhost:8443`
- **AND** HTTPS certificate generation step is included in the Getting Started section
