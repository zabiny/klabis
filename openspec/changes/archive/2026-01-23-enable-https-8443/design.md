# Design: Enable HTTPS on Port 8443

## Context

The Klabis backend currently serves all traffic over HTTP on port 8080. This includes:

- OAuth2 authorization flows with sensitive authorization codes and tokens
- Member personal data including GDPR-protected fields (birth numbers, contact information)
- User authentication credentials
- API requests with access tokens

The application is built with Spring Boot 3.x and uses Spring Security with OAuth2 Authorization Server. The codebase
follows Clean Architecture with multiple modules (members, users, common, config). The system is currently in
development phase with in-memory H2 database.

**Constraints:**

- Must support local development without complex infrastructure
- Must maintain compatibility with existing Spring Security OAuth2 configuration
- Must not require external certificate authorities for development
- Must support multiple deployment environments (dev, test, prod)
- Must maintain backward compatibility path for existing developers

**Stakeholders:**

- Backend developers (need simple local HTTPS setup)
- Frontend developers (need HTTPS endpoints for OAuth2 integration)
- Operations team (need production-ready SSL configuration)
- End users (need secure data transmission)

## Goals / Non-Goals

### Goals

- Enforce HTTPS for all HTTP traffic in all profiles
- Enable HTTP/2 protocol for improved performance
- Provide simple certificate generation for local development
- Support production-grade certificate configuration
- Update all OAuth2 URLs to use HTTPS
- Maintain test automation without manual certificate setup
- Provide clear migration documentation

### Non-Goals

- Mutual TLS (mTLS) / client certificate authentication
- Certificate rotation automation (manual renewal acceptable)
- Let's Encrypt integration (production will use existing certificate management)
- HTTP/3 protocol support (HTTP/2 is sufficient)
- Automatic HTTP to HTTPS redirect (HTTPS-only is cleaner)
- Certificate revocation list (CRL) or OCSP stapling
- Custom SSL context per endpoint

## Decisions

### Decision 1: HTTPS-Only on Port 8443 (No HTTP Fallback)

**What:** Disable HTTP completely. Server listens only on port 8443 with HTTPS.

**Why:**

- Prevents accidental plaintext transmission of sensitive data
- Simpler configuration (no dual-port setup)
- Forces correct client behavior from day one
- Eliminates need for HTTP-to-HTTPS redirect logic
- Aligns with security best practices (no mixed content)

**Alternatives Considered:**

1. **HTTP + HTTPS with redirect** - More user-friendly but adds complexity and security risk of accidentally using HTTP
2. **HTTP in dev, HTTPS in prod** - Reduces dev/prod parity, can mask SSL-related bugs until production
3. **Dual-port with no redirect** - Worst option, defeats purpose of HTTPS

**Trade-offs:**

- ✅ Better security (no plaintext fallback)
- ✅ Simpler configuration
- ❌ Requires certificate trust in development
- ❌ Less forgiving for misconfigured clients

### Decision 2: Self-Signed Certificates for Development

**What:** Provide script to generate self-signed certificates for localhost. Developers must trust certificate in their
OS/browser.

**Why:**

- No external dependencies or costs
- Complete control over certificate properties
- Works offline
- Simple automation with keytool
- Industry-standard approach for local HTTPS development

**Alternatives Considered:**

1. **mkcert** - Simpler trust setup but requires additional tool installation
2. **Pre-committed certificate** - Security risk, all developers share same private key
3. **Real CA certificate** - Unnecessary cost/complexity for development

**Trade-offs:**

- ✅ Zero external dependencies
- ✅ No cost
- ✅ Works with standard Java tooling
- ❌ Requires manual trust step
- ❌ Browser security warnings until trusted

### Decision 3: Spring Boot Properties for SSL Configuration

**What:** Configure SSL via `application.yml` with profile-specific overrides. Production uses environment variables for
sensitive values.

**Why:**

- Spring Boot native approach (no custom code)
- Profile-specific configuration built-in
- Clear separation of dev vs prod settings
- Environment variable override for production secrets
- Works with existing Spring Boot deployment patterns

**Alternatives Considered:**

1. **Programmatic SSL configuration** - More flexible but adds complexity and custom code
2. **External config server** - Overkill for current deployment model
3. **Kubernetes secrets** - Future option but not applicable to current infrastructure

**Configuration Structure:**

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: ${SSL_KEYSTORE_PATH:classpath:keystore/dev-keystore.p12}
    key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}
    key-store-type: PKCS12
    key-alias: ${SSL_KEY_ALIAS:dev-klabis}
```

**Trade-offs:**

- ✅ Uses Spring Boot conventions
- ✅ Simple to understand and modify
- ✅ Environment variable override for production
- ❌ Less flexible than programmatic config
- ❌ Cannot easily support multiple certificates per environment

### Decision 4: Update All URLs to HTTPS (No Dual-Protocol Support)

**What:** Change all hardcoded and default URLs from `http://localhost:8080` to `https://localhost:8443`. Update
environment variable defaults. This is a breaking change.

**Why:**

- Consistency across codebase
- Forces HTTPS adoption
- Eliminates mixed content issues
- Simplifies testing (one protocol to test)
- Clear migration path

**Alternatives Considered:**

1. **Protocol-relative URLs** - Not applicable to backend configuration
2. **Runtime protocol detection** - Added complexity, no real benefit
3. **Gradual migration** - Prolongs insecure state, complicates testing

**Affected Components:**

- OAuth2 issuer URLs (SecurityConfiguration, AuthorizationServerConfiguration)
- OAuth2 redirect URIs (BootstrapDataLoader)
- Email base URLs (PasswordSetupService, application.yml)
- OpenAPI server URL (OpenApiConfig)
- Test configurations (TestConfigurationHelper)
- HTTP client configs (http-client.env.json)
- Documentation examples (README.md, API.md, etc.)

**Trade-offs:**

- ✅ Consistent security posture
- ✅ No mixed content bugs
- ✅ Clear breaking change boundary
- ❌ Breaking change for existing developers
- ❌ All examples must be updated

### Decision 5: Committed Test Certificate for CI/CD

**What:** Include a test-only certificate in `src/test/resources/keystore/test-keystore.p12` committed to repository
with documented password.

**Why:**

- Enables automated testing without manual certificate setup
- CI pipelines work without additional configuration
- Consistent test environment across all developers
- Test certificate is not sensitive (no production data)

**Security Considerations:**

- Test certificate MUST NOT be used in production (enforced by configuration)
- Password is public knowledge (documented in code)
- Certificate is clearly labeled "TEST ONLY"
- Production configuration MUST fail if test certificate is detected

**Alternatives Considered:**

1. **Generate certificate in CI** - Slower builds, more complexity
2. **No HTTPS in tests** - Reduces test coverage of SSL-related features
3. **Mock SSL** - Doesn't test real TLS configuration

**Trade-offs:**

- ✅ Zero CI setup overhead
- ✅ Faster test execution
- ✅ Consistent test results
- ❌ Public certificate/password in repo (acceptable for test-only)

### Decision 6: TLS 1.2+ with Modern Cipher Suites

**What:** Enforce TLS 1.2 as minimum version. Enable strong cipher suites (ECDHE, AES-GCM). Disable weak ciphers (DES,
RC4).

**Why:**

- Industry best practices (TLS 1.0/1.1 deprecated)
- Compliance with security standards
- Forward secrecy with ECDHE
- Modern browsers support TLS 1.2+

**Configuration:**

```yaml
server:
  ssl:
    enabled-protocols: TLSv1.2,TLSv1.3
    ciphers: TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
```

**Trade-offs:**

- ✅ Strong security posture
- ✅ Forward secrecy
- ✅ Modern browser compatibility
- ❌ No support for legacy clients (acceptable trade-off)

## Risks / Trade-offs

### Risk: Developer Friction During Migration

**Impact:** Medium - Developers must generate and trust certificates

**Mitigation:**

- Provide automated certificate generation script
- Comprehensive trust instructions for all OS platforms
- Clear error messages when certificate is missing or untrusted
- Video tutorial for certificate setup (optional)

### Risk: OAuth2 Client Configuration Breakage

**Impact:** High - All OAuth2 clients must update redirect URIs

**Mitigation:**

- Clear documentation of required changes
- Migration checklist in README
- Update all example configurations
- Test files demonstrate correct HTTPS usage

### Risk: CI/CD Pipeline Failures

**Impact:** Medium - CI builds may fail if certificate not available

**Mitigation:**

- Commit test certificate to repository
- Document CI-specific configuration
- Pre-configure test profile with test certificate
- Validate certificate in tests before starting application

### Risk: Production Certificate Misconfiguration

**Impact:** High - Application fails to start with invalid certificate

**Mitigation:**

- Fail fast on startup with clear error messages
- Validate certificate before starting server
- Document production certificate requirements
- Provide certificate validation script
- Example production configuration in documentation

### Risk: Mixed Content Errors in Frontend

**Impact:** Medium - Frontend may fail to load HTTP resources

**Mitigation:**

- Update frontend CORS configuration
- Update all frontend URLs to HTTPS
- Document browser console errors
- Provide troubleshooting guide

## Migration Plan

### Phase 1: Certificate Generation and Configuration (Day 1)

1. Create certificate generation script (`generate-dev-cert.sh`)
2. Add SSL configuration to `application.yml`
3. Update `.gitignore` to exclude keystores
4. Commit test certificate for CI

### Phase 2: Code Updates (Day 1-2)

1. Update OAuth2 issuer URLs in configuration classes
2. Update redirect URIs in BootstrapDataLoader
3. Update email base URLs
4. Update OpenAPI configuration
5. Update test configurations

### Phase 3: Documentation and Examples (Day 2-3)

1. Update README with HTTPS setup instructions
2. Update all documentation files with HTTPS URLs
3. Update example scripts
4. Update HTTP client configuration files
5. Create migration guide for developers

### Phase 4: Testing and Validation (Day 3-4)

1. Test server startup on all profiles
2. Test OAuth2 flows end-to-end
3. Test password setup email links
4. Test member registration E2E
5. Run full integration test suite
6. Test static frontend with HTTPS

### Phase 5: Rollout (Day 5)

1. Merge changes to feature branch
2. Communicate breaking changes to team
3. Schedule team walkthrough of migration steps
4. Merge to main branch after team approval
5. Update deployment documentation

### Rollback Plan

If critical issues are found:

1. Revert commit(s) to restore HTTP on port 8080
2. Document issue in GitHub issue
3. Keep migration branch for future retry
4. Schedule postmortem to identify problems

## Open Questions

1. **Production Certificate Provider**: Which certificate authority will be used for production? (Let's Encrypt,
   corporate CA, commercial CA?)
    - **Answer needed from**: Operations team
    - **Blocking**: Production deployment documentation

2. **Certificate Renewal Process**: What is the process for renewing production certificates?
    - **Answer needed from**: Operations team
    - **Blocking**: Production runbook

3. **Load Balancer SSL Termination**: Will production use SSL termination at load balancer or end-to-end SSL?
    - **Answer needed from**: Infrastructure team
    - **Impact**: May change production SSL configuration approach

4. **Frontend Deployment**: How is the frontend deployed? Same origin or different origin?
    - **Answer needed from**: Frontend team
    - **Impact**: CORS configuration and OAuth2 redirect URIs

5. **Browser Compatibility Requirements**: Are there specific legacy browser versions that must be supported?
    - **Answer needed from**: Product team
    - **Impact**: TLS version and cipher suite selection

## References

- Spring Boot SSL
  Configuration: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.webserver.configure-ssl
- OAuth2 with HTTPS: https://datatracker.ietf.org/doc/html/rfc6749#section-3.1
- TLS Best Practices: https://wiki.mozilla.org/Security/Server_Side_TLS
- Self-Signed Certificates: https://www.baeldung.com/spring-boot-https-self-signed-certificate
