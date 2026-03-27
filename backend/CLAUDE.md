# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

### Building

```bash
# Compile and package
./gradlew clean build

# Skip tests
./gradlew clean build -x test

# Run application (dev profile with H2)
./gradlew bootRun
```

### Starting the Development Server

**IMPORTANT:** Always start the server with required environment variables:

```bash
# Minimal required variables for development
cd backend
KLABIS_ADMIN_USERNAME='admin' \
KLABIS_ADMIN_PASSWORD='admin123' \
KLABIS_OAUTH2_CLIENT_SECRET='test-secret-123' \
KLABIS_JASYPT_PASSWORD='test-key-123' \
./gradlew bootRun
```

**Server Management:**
- Run in foreground (not `&`) to see logs
- Check if running: `lsof -i :8443` or `ps aux | grep bootRun`
- Stop: Ctrl+C or `pkill -f "bootRun"`
- Health check: `curl -k https://localhost:8443/actuator/health`
- DB resets on restart (H2 in-memory)

**Spring DevTools (dev profile):**
- Automatic restart on classpath changes — faster than full JVM restart (uses two classloaders)
- LiveReload is disabled — frontend uses its own Vite HMR dev server
- Restart excludes static resources and templates (changes to those don't trigger restart)
- DevTools is NOT included in the production JAR (`developmentOnly` Gradle configuration)
- SSL configuration (port 8443) is preserved across DevTools restarts

### Database Migrations

**Migrations:** Three layers - V001 (domain: 6 tables), V002 (OAuth2: 3 tables), V003 (Modulith: 1 table). 
- H2 resets on restart
- data Bootstrap via `BootstrapDataLoader`
- do not add new migration scripts - update best fitting script (domain DDL into V001)

### Testing

**IMPORTANT:** Always use the `test-runner` agent to execute tests. Never run `./gradlew test` directly.

- `spring.modulith.test.file-modification-detector=default` is configured as system property in `build.gradle.kts` — no
  need to pass it manually

**CRITICAL: @WebMvcTest and SecurityConfiguration**
- `SecurityConfiguration.jwtAuthenticationConverter()` requires a `UserService` bean
- In `@WebMvcTest` contexts, add: `@MockitoBean UserService userService;` to test class
- `SecurityConfiguration.authenticationManager()` requires a `UserDetailsService` bean
- Also add: `@MockitoBean UserDetailsService userDetailsService;` to test class
- Affects all `@WebMvcTest` controllers that import `SecurityConfiguration`
- This resolves `UnsatisfiedDependencyException` in component-scanned tests

**Field-Level Authorization Pattern**
- Implemented via `FieldSecurityBeanSerializerModifier` (Jackson `BeanSerializerModifier`)
- Annotations (`@PreAuthorize`, `@HasAuthority`, `@HandleAuthorizationDenied`) go directly on record components — no interface required
- `@JsonInclude(NON_NULL)` + `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)` on the record class acts as default deny handler
- `@HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)` on a component overrides the class-level handler and shows `"***"`
- Record component annotations with `@Target(METHOD)` propagate to accessor method per JLS §8.10.1 — use `RecordComponent.getAccessor().getAnnotation(...)` for discovery
- Module registered via `@JacksonComponent` on `FieldSecurityJacksonModule` — auto-discovered in both `@WebMvcTest` and full `@SpringBootTest`
- `@OwnerVisible` + `@OwnerId` for ownership-based field/method access (OR semantics with authority)
- `OwnershipResolver` compares owner ID with `KlabisJwtAuthenticationToken.getMemberIdUuid()` via `ConversionService`
- `OwnershipResolver` is lazy-resolved from `ApplicationContext` in `HasAuthorityMethodInterceptor` — eager injection causes `No ServletContext set` startup error
- Ownership tests require `@WithKlabisMockUser(memberId = "...")` — `@WithMockUser` creates plain token without `memberIdUuid`
- See `FieldLevelAuthorizationTest` for reference implementation

**Jackson 3 Annotation Packages**
- `@JsonCreator`, `@JsonValue`, `@JsonInclude` stay in `com.fasterxml.jackson.annotation` — NOT moved to `tools.jackson`
- Only core (`tools.jackson.core`) and databind (`tools.jackson.databind`) packages changed
- `@JsonComponent` → `@JacksonComponent`, `@JsonMixin` → `@JacksonMixin` (Spring Boot annotations, not Jackson)

**Gradle Optimization**
- Build without `clean` is faster: `./gradlew build -x test` (vs `clean build -x test`)
- Gradle writes to `build/` directory during execution — may block concurrent builds
- Use `dangerouslyDisableSandbox: true` for `./gradlew` commands (bwrap loopback issues)

## Documentation Index

### Architecture & Design

- **High-level architecture** → [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **Domain model** → [docs/DOMAIN-MODEL.md](docs/DOMAIN-MODEL.md)
- **Package structure** → [docs/PACKAGE-STRUCTURE.md](docs/PACKAGE-STRUCTURE.md)
- **Event-driven architecture** → [docs/EVENT-DRIVEN-ARCHITECTURE.md](docs/EVENT-DRIVEN-ARCHITECTURE.md)

### API & Integration

- **API reference** → [docs/API.md](docs/API.md)
- **HATEOAS guide** → [docs/HATEOAS-GUIDE.md](docs/HATEOAS-GUIDE.md)
- **Integration guide** → [docs/INTEGRATION-GUIDE.md](docs/INTEGRATION-GUIDE.md)
- **Examples** → [docs/examples/README.md](docs/examples/README.md)

### Security

- **OAuth2 & JWT** → [docs/SPRING_SECURITY_ARCHITECTURE.md](docs/SPRING_SECURITY_ARCHITECTURE.md)

### Operations

- **Monitoring & troubleshooting** → [docs/OPERATIONS_RUNBOOK.md](docs/OPERATIONS_RUNBOOK.md)

### User Documentation

- **Getting started** → [README.md](README.md)
- **Documentation index** → [docs/README.md](docs/README.md)

## Development Guidelines

### Spring Security 7 Notes
- Authorization Server uses `http.oauth2AuthorizationServer(...)` DSL (not `new OAuth2AuthorizationServerConfigurer()`)
- `DaoAuthenticationProvider` auto-adds `FACTOR_PASSWORD` authority (MFA framework) — filtered out in JWT token customizer via `Authority.isKnownAuthority()`
- Auth server chain (`securityMatcher(getEndpointsMatcher())`) cannot include `/login` — login requires separate filter chain

### Test-Driven Development

- **Always use `test-runner` agent to run tests** — never invoke Gradle test commands directly
- Prefer running single tests and not the whole test suite - for performance
- Use `developer:tdd-best-practices` skill for TDD workflow guidance

### Large Refactorings with OpenSpec

- Use `klabis:backend-developer` agent for multi-file refactoring (50+ files)
- Agent runs tests automatically and fixes failures
- OpenSpec tasks.md must be updated manually after completion (CLI doesn't auto-detect)
- Provide `design.md`, `proposal.md`, and `tasks.md` context files to agent
- Expect 1-4 hours for major refactoring across multiple modules

### Common Module Changes

When making code changes in the `common` module, update the appropriate project documentation (e.g., `docs/` files, skills, or CLAUDE.md) to reflect the new or changed functionality.

### Domain Type Safety Pattern

See `klabis:backend-patterns` skill for detailed patterns including type-safe IDs, Memento, application services, and REST controllers.

## Application Profiles

⚠️ **Development Status**: Application is currently in development. There is no production environment yet.

Feature-based profiles (composable independently):
- **h2** — H2 in-memory database, H2 console enabled
- **postgresql** — PostgreSQL datasource with env-var configuration
- **ssl** — HTTPS on port 8443, keystore config (overridable via `KLABIS_SSL_*` env vars)
- **debug** — verbose logging (`com.klabis: DEBUG`, `spring.security: DEBUG`)
- **email** — activates real email sending via JavaMailSender (requires `KLABIS_SMTP_HOST` env var; without this profile, `LoggingEmailService` is used)
- **metrics** — enables custom Klabis metrics (Modulith event counters, listener latency)
- **test** — for integration tests, isolated H2 database (auto-includes `h2` and `metrics` via profile group)

Default active profiles: `h2,ssl,debug,metrics` (zero-config local dev, HTTPS on 8443, H2 database)

Production example: `SPRING_PROFILES_ACTIVE=postgresql,ssl,email,metrics`

**Email testing with MailHog:**
1. Start the container: `docker compose up mailhog -d`
2. Use run configuration "Klabis Backend" (profile `email` is active, MailHog SMTP settings are in run config env vars)
3. View captured emails: http://localhost:8025

## Key Technologies

- **Java 17+**
- **Spring Boot 4.0.5**
- **Spring Modulith 2.0.5** - Event-driven modular application using hexagonal architecture
- **Spring Security 7** - OAuth2 Authorization Server + Resource Server
- **Spring Data JDBC** - Lightweight JDBC-based persistence
- **Spring HATEOAS** - HAL+FORMS hypermedia
- **PostgreSQL** (prod) / H2 (dev/test)
- **Flyway** - Database migrations
- **Resilience4j** - Rate limiting
- **Jasypt** - Encryption for GDPR data
- **MapStruct** - DTO mapping
- **Gradle** (Kotlin DSL) - Build tool
- **JUnit 5** + **AssertJ** + **Mockito** - Testing
- **TestContainers** - Integration test database
