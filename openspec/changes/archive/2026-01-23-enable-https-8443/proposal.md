# Change: Enable HTTPS on Port 8443

## Why

The application currently runs on HTTP port 8080 for all environments, which exposes sensitive data (authentication
credentials, OAuth2 tokens, member personal information including GDPR-protected fields) to potential interception.
HTTPS is essential for:

- Protecting OAuth2 authorization codes and access tokens in transit
- Securing member personal data (birth numbers, contact information)
- Meeting security best practices for production deployments
- Ensuring browser compatibility (modern browsers restrict certain features to HTTPS)

## What Changes

- **BREAKING**: Replace HTTP (port 8080) with HTTPS (port 8443) for all profiles (dev, test, prod)
- Configure SSL/TLS in `application.yml` with profile-specific keystore settings
- Update all OAuth2 issuer URLs from `http://localhost:8080` to `https://localhost:8443`
- Update all redirect URIs and callback URLs to use HTTPS
- Update documentation, examples, and HTTP test files with HTTPS URLs
- Add instructions for generating self-signed certificates for local development
- Update frontend CORS configuration to accept HTTPS origins

## Impact

### Affected Specs

- **NEW**: `server-configuration` - New specification for server deployment and security configuration

### Affected Code

- `klabis-backend/src/main/resources/application.yml` - Add server.ssl configuration
- `klabis-backend/src/main/java/com/klabis/config/SecurityConfiguration.java` - OAuth2 issuer URL
- `klabis-backend/src/main/java/com/klabis/config/AuthorizationServerConfiguration.java` - OAuth2 issuer URL
- `klabis-backend/src/main/java/com/klabis/config/OpenApiConfig.java` - API server URL
- `klabis-backend/src/main/java/com/klabis/config/BootstrapDataLoader.java` - OAuth2 redirect URIs
- `klabis-backend/src/main/java/com/klabis/users/application/PasswordSetupService.java` - Email base URL
- `klabis-backend/http-client.env.json` - IntelliJ HTTP client OAuth2 configuration
- `klabis-backend/.env.example` - Environment variable examples
- `klabis-backend/README.md` - Documentation and examples
- All `.http` test files with OAuth2 requests
- Static frontend files (`src/main/resources/static/**`)

### Breaking Changes

- **BREAKING**: Existing clients using `http://localhost:8080` must update to `https://localhost:8443`
- **BREAKING**: Developers must generate and trust self-signed certificates for local development
- **BREAKING**: OAuth2 redirect URIs must be updated in all client configurations
- **BREAKING**: Frontend must be updated to use HTTPS for API calls

### Migration Notes

- Provide certificate generation script for development (`generate-dev-cert.sh`)
- Document certificate trust process for different operating systems
- Update all example curl commands and HTTP files
- Consider providing a migration checklist for developers
