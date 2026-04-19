# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

### Prerequisites

The backend depends on `com.dpolach.api:oris-client` hosted in a private GitLab Maven registry (`gitlab.polach.cloud`). Gradle needs a read token:

```properties
# ~/.gradle/gradle.properties
gitlabZbmToken=<your-personal-token>
```

Alternatively, set env var `GITLAB_ZBM_TOKEN`. CI injects it via the same-named repository secret.

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

See root `CLAUDE.md` Quick Start section (`./runLocalEnvironment.sh`). Additional notes:

- Check if running: `lsof -i :8443` or `ps aux | grep bootRun`
- Stop: Ctrl+C or `pkill -f "bootRun"`
- Health check: `curl -k https://localhost:8443/actuator/health`
- DB resets on restart (H2 in-memory)
- Spring DevTools: auto-restart on classpath changes, LiveReload disabled (frontend uses Vite HMR), not included in production JAR

### Database Migrations

**Migrations:** Three layers - V001 (domain: 6 tables), V002 (OAuth2: 3 tables), V003 (Modulith: 1 table). 
- H2 resets on restart
- data Bootstrap via `BootstrapDataLoader`
- do not add new migration scripts - update best fitting script (domain DDL into V001)

### Testing

**IMPORTANT:** Always use the `test-runner` agent to execute tests. Never run `./gradlew test` directly.

**CRITICAL — never run Gradle / test-runner in parallel tool calls.** Gradle uses a single daemon and a shared `build/` directory; concurrent invocations deadlock on cache locks (`fileHashes.lock`) and require manual cleanup. Always invoke test-runner sequentially — even when running multiple test classes, batch them into one invocation or run them one after another.

- `spring.modulith.test.file-modification-detector=default` is configured as system property in `build.gradle.kts` — no
  need to pass it manually

**CRITICAL: @WebMvcTest and SecurityConfiguration**
- `SecurityConfiguration.jwtAuthenticationConverter()` requires a `UserService` bean
- In `@WebMvcTest` contexts, add: `@MockitoBean UserService userService;` to test class
- Affects all `@WebMvcTest` controllers that import `SecurityConfiguration`
- This resolves `UnsatisfiedDependencyException` in component-scanned tests

**Field-Level Authorization Pattern** — see `backend-patterns` skill for full details. Key gotchas:
- `OwnershipResolver` is lazy-resolved from `ApplicationContext` — eager injection causes `No ServletContext set` startup error
- Ownership tests require `@WithKlabisMockUser(memberId = "...")` — `@WithMockUser` creates plain token without `memberIdUuid`
- Record component annotations with `@Target(METHOD)` propagate to accessor method per JLS §8.10.1


**Gradle Optimization**
- Build without `clean` is faster: `./gradlew build -x test` (vs `clean build -x test`)
- Gradle writes to `build/` directory during execution — may block concurrent builds

## Documentation Index

- **Backend conventions** (aggregates, controllers, HATEOAS affordances, JDBC mementos, field-level authorization, testing) → load **`backend-patterns`** skill — single source of truth
- **Event flows & module dependencies** → [docs/EVENT-DRIVEN-ARCHITECTURE.md](docs/EVENT-DRIVEN-ARCHITECTURE.md)
- **External resources** (Spring Boot/Modulith/HATEOAS/Security reference, RFCs, DDD/outbox patterns) → [docs/README.md](docs/README.md)
- **Getting started** → [README.md](README.md)

## Development Guidelines

### Spring Security 7 Notes
- Authorization Server uses `http.oauth2AuthorizationServer(...)` DSL (not `new OAuth2AuthorizationServerConfigurer()`)
- Auth server chain (`securityMatcher(getEndpointsMatcher())`) cannot include `/login` — login requires separate filter chain

### Test-Driven Development

- **Always use `test-runner` agent to run tests** — never invoke Gradle test commands directly
- Prefer running single tests and not the whole test suite - for performance
- Use `developer:tdd-best-practices` skill for TDD workflow guidance

### Large Refactorings with OpenSpec

- Use `backend-developer` agent for multi-file refactoring (50+ files)
- Agent runs tests automatically and fixes failures
- OpenSpec tasks.md must be updated manually after completion (CLI doesn't auto-detect)
- Provide `design.md`, `proposal.md`, and `tasks.md` context files to agent
- Expect 1-4 hours for major refactoring across multiple modules

### Common Module Changes

The `common` package is a shared kernel used by every module (security primitives, HATEOAS helpers, value-object converters, validation, GDPR encryption, rate limiting). When you change anything in `common`, update the `backend-patterns` skill (and this file if behaviour changes) so future sessions stay accurate.

### Domain Type Safety Pattern

See `backend-patterns` skill for detailed patterns including type-safe IDs, Memento, application services, and REST controllers.

## Security Extension Points

These are the Klabis-specific hooks on top of Spring Security / Spring Authorization Server. The mainline filter-chain / JWT theory lives in the Spring docs (see [docs/README.md](docs/README.md)).

- **`KlabisAuthorizationServerCustomizer`** (`members.infrastructure.authorizationserver`) — Spring AS customizer that wires Klabis-specific behaviour into the authorization server chain (registered clients, token customizers).
- **JWT custom claims** — access tokens carry `registrationNumber`, `memberIdUuid`, and the user's `authorities`. `Authority.isKnownAuthority()` filters out the `FACTOR_PASSWORD` authority that `DaoAuthenticationProvider` auto-adds for the MFA framework.
- **`MemberIdToUuidConverter` + `CurrentUserArgumentResolver`** — resolve `@CurrentUser Member` parameters in controllers from the JWT subject (`memberIdUuid` claim). See `backend-patterns` skill.
- **`@HasAuthority(Authority.X)`** — type-safe alternative to `@PreAuthorize("hasAuthority('X:Y')")` for single-authority global checks. Method/class level. See `backend-patterns` skill.
- **Field-level authorization** — `@OwnerVisible`, `@HasAuthority`, `PatchField<T>` on record components. See `backend-patterns` skill.
- **`KlabisUserDetailsService`** — bridges `users` aggregate + `Authority` enum into Spring Security `UserDetails`.
- **Custom `AuthenticationEntryPoint`** — validates the OAuth2 `redirect_uri` against `RegisteredClientRepository` before redirecting (prevents open-redirector).

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
- **local-dev** — **local developer machines only**. Registers a second OAuth2 client `klabis-web-local` (confidential, `CLIENT_SECRET_POST`, PKCE required, `AUTHORIZATION_CODE` + `REFRESH_TOKEN` grants). This enables refresh-token-based silent token renewal when the frontend runs on `http://localhost:3000` (cross-origin from the backend on `:8443`). Spring AS refuses to issue refresh tokens to public clients, so this profile adds a confidential workaround client that stays out of production entirely. See `openspec/changes/enable-refresh-tokens-for-local-dev` for full rationale. **Never activate in any deployed environment.**

Default active profiles: `h2,ssl,debug,metrics` (zero-config local dev, HTTPS on 8443, H2 database)
`runLocalEnvironment.sh` adds `local-dev` automatically, so developers get refresh-token-based silent renew out of the box.

Production example: `SPRING_PROFILES_ACTIVE=postgresql,ssl,email,metrics`

**Email testing with MailHog:**
1. Start the container: `docker compose up mailhog -d`
2. Use run configuration "Klabis Backend" (profile `email` is active, MailHog SMTP settings are in run config env vars)
3. View captured emails: http://localhost:8025

## Key Technologies

- **Java 21+**
- **Spring Boot 4.0.5**
- **Spring Modulith 2.0.0** - Event-driven modular application using hexagonal architecture
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
