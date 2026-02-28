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
BOOTSTRAP_ADMIN_USERNAME='admin' \
BOOTSTRAP_ADMIN_PASSWORD='admin123' \
OAUTH2_CLIENT_SECRET='test-secret-123' \
JASYPT_ENCRYPTOR_PASSWORD='test-key-123' \
./gradlew bootRun
```

**Server Management:**
- Run in foreground (not `&`) to see logs
- Check if running: `lsof -i :8443` or `ps aux | grep bootRun`
- Stop: Ctrl+C or `pkill -f "bootRun"`
- Health check: `curl -k https://localhost:8443/actuator/health`
- DB resets on restart (H2 in-memory)

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
- Affects all `@WebMvcTest` controllers that import `SecurityConfiguration`
- This resolves `UnsatisfiedDependencyException` in component-scanned tests

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

### Test-Driven Development

- **Always use `test-runner` agent to run tests** — never invoke Gradle test commands directly
- Prefer running single tests and not the whole test suite - for performance
- Use `developer:tdd-best-practices` skill for TDD workflow guidance

### Large Refactorings with OpenSpec

- Use `developer:backend-developer` agent for multi-file refactoring (50+ files)
- Agent runs tests automatically and fixes failures
- OpenSpec tasks.md must be updated manually after completion (CLI doesn't auto-detect)
- Provide `design.md`, `proposal.md`, and `tasks.md` context files to agent
- Expect 1-4 hours for major refactoring across multiple modules

### Domain Type Safety Pattern

- **Domain/Service layers**: Use type-safe IDs (`MemberId`, `UserId`, `EventId`)
- **Controllers**: Convert UUID path variables to type-safe IDs: `new MemberId(uuid)`
- **Persistence**: Mementos continue using UUID (no database migrations)
- **API contracts**: DTOs continue using UUID (no breaking changes)
- **Benefit**: Compile-time type safety prevents wrong ID types between aggregates

## Application Profiles

⚠️ **Development Status**: Application is currently in development. There is no production environment yet. All
instances run on in-memory H2 database (dev profile).

- **dev** (default): H2 in-memory database, SQL logging, H2 console enabled
- **test**: For integration tests, isolated H2 database
- **prod**: PostgreSQL, production settings, actuator endpoints secured (not yet deployed)

## Key Technologies

- **Java 17+**
- **Spring Boot 3.5.9**
- **Spring Modulith 1.4.6** - Event-driven modular application using hexagonal architecture
- **Spring Security** - OAuth2 Authorization Server + Resource Server
- **Spring Data JPA** - Hibernate ORM
- **Spring HATEOAS** - HAL+FORMS hypermedia
- **PostgreSQL** (prod) / H2 (dev/test)
- **Flyway** - Database migrations
- **Resilience4j** - Rate limiting
- **Jasypt** - Encryption for GDPR data
- **MapStruct** - DTO mapping
- **Gradle** (Kotlin DSL) - Build tool
- **JUnit 5** + **AssertJ** + **Mockito** - Testing
- **TestContainers** - Integration test database
