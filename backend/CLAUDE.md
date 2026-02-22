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

**IMPORTANT:** Always use the `test-runner` skill to execute tests. Never run `./gradlew test` directly.

- `spring.modulith.test.file-modification-detector=default` is configured as system property in `build.gradle.kts` — no
  need to pass it manually

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

- **Always use `test-runner` skill to run tests** — never invoke Gradle test commands directly
- Prefer running single tests and not the whole test suite - for performance
- Use `developer:tdd-best-practices` skill for TDD workflow guidance

### Test Types & Locations

**Module Names:**
- Core domain modules: `members`, `users`, `events`

**Unit Tests** (business logic in isolation):

- Domain entities: `src/test/java/com/klabis/{module}/domain/*Test.java`
- Application services: `src/test/java/com/klabis/{module}/application/*Test.java`
- Use pure JUnit 5, no Spring context
- Example: `MemberTest`, `GetUserPermissionsQueryHandlerTest`

**Integration Tests** (with Spring context and database):

- Repository tests: `src/test/java/com/klabis/{module}/infrastructure/persistence/*IntegrationTest.java`
- Controller tests: `src/test/java/com/klabis/{module}/presentation/*Test.java`
- Use appropriate spring slice - `@DataJpaTest`, or `@WebMvcTest`, or `@JsonTest` etc. in depends on what adapter is
  tested
- Example: `UserJpaRepositoryTest`, `UserControllerPermissionsTest`

**E2E Tests** (full application flow):

- Location: `src/test/java/com/klabis/{module}/*E2ETest.java`
- Use `@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.ALL_DEPENDENCIES)` (usually with
  `@AutoConfigureMockMvc`)
- Test complete user journeys across modules or aggregate root lifecycle
- Example: `MemberLifecycleE2ETest`
- **Scope:** Verify lifecycle PROGRESSION only — status codes and minimal navigation checks
- **Not in E2E:** Response JSON structure (→ controller unit tests), domain events (→ integration tests)
- **Never inject repositories** in E2E tests — too low-level; verify state only through API responses

### Integration Test Best Practices

**Module Boundaries & @Sql:**
- Don't inject cross-module repositories - use `@Sql` for FK setup
- `@Sql` requires compile-time constants (no string concat)
- Check schema constraints (column lengths, FK refs)
- Use realistic test values (e.g., "OOB" not "Organizer A")

**Root Navigation Postprocessors:**
- Add module collection links to `/api` root via `RepresentationModelProcessor<EntityModel<RootModel>>`
- Pattern: Create inner class in controller, annotate with `@Component`
- Example: `CalendarRootPostprocessor` adds "calendar" rel for calendar items collection
- Use `Pageable.unpaged()` for collection links (not `null`)

**TestDataBuilder Pattern:**
- Create test data builders in `src/test/java/` following existing patterns
- Examples: `EventTestDataBuilder`, `MemberTestDataBuilder`, `CalendarItemTestDataBuilder`
- Use fluent API with `withXxx()` methods and static factory `anXxx()`
- Support both manual and specialized builds (e.g., `buildManual()`, `buildEventLinked()`)

**Parameter Handling Pattern:**
- Service layer: Use `@NonNull` for required business parameters (strict validation)
- Controller layer: Handle default values before delegating to service
- Example: Date range is required in service, but defaults to current month in controller
- Keeps business logic clean while providing good UX

### Coding Conventions

#### Rules

- NON-NEGOTIABLE: use HalFormsSupport#klabisAfford and HalFormsSupport#klabisLinkTo to prepare HATEOAS navigation in controllers (never use WebMvcLinkBuilder#linkTo and WebMvcLinkBuilder#afford) methods.
- **HATEOAS link/affordance rules**: Links ONLY for GET endpoints, affordances ONLY for POST/PUT/DELETE/PATCH endpoints
- RepresentationModelProcessor must follow same HATEOAS rules as controllers (no links to POST endpoints)
- klabisAfford handles authorization internally - don't duplicate authorization checks in controllers and processors
- **Response bodies**: POST/PUT/PATCH/DELETE endpoints return 204 No Content (empty body) or 201 Created with Location header only 

#### Java Best Practices

- Use **Java Records** for value objects (DTOs, immutable domain objects)
- Use `org.springframework.util.Assert` for parameter validation in methods
- Use Lombok annotations sparingly (only for entities/infrastructure where needed)
- Use MapStruct for DTO ↔ Entity mapping
- Prefix test methods with `should` (e.g., `shouldCreateMemberWithValidData`)
- Use 'package protected' visibility as default for new classes. Make them public only when they need to be accessed
  from other package
- **Do not mock data objects** (entities, value objects, DTOs) in tests - use real instances instead
- **Refactor methods with more than 4 parameters** by introducing parameter objects, request models, or splitting the
  method
- use jMolecules annotations for "Ports and Adapters" (hexagonal) architecture
- use `@MvcComponent` annotation on components from presentation layer 

#### Naming conventions

- if tested class is set in Test's attribute, name such attribute as `testedSubject` 
- attribute with mocked instance shall have `Mock` postfix in the name (for example `userDetailServiceMock`) 

#### Security Considerations

- Sensitive data (e.g., rodne cislo) encrypted with Jasypt
- Passwords hashed with BCrypt
- Tokens stored as SHA-256 hashes, never plain text

## Common Development Pitfalls

### 1. Jackson Configuration ⚠️ **CRITICAL WARNING**

**DO NOT** create global `JacksonConfiguration` or `ObjectMapper` beans with custom `PolymorphicTypeValidator` settings.

**Why:** Global Jackson configuration affects ALL serialization/deserialization in the application, including:

- Spring Security authentication
- OAuth2 token processing
- Database entity mapping
- API request/response handling

**Symptoms of incorrect Jackson configuration:**

- Authentication fails with "Invalid credentials" even with correct password
- Security filter chains behave unexpectedly
- Deserialization errors for seemingly valid JSON

**Correct Approach:**

- Let Spring Boot auto-configure Jackson (default is usually sufficient)
- If you need custom serialization, use `@JsonSerialize`/`@JsonDeserialize` annotations on specific classes
- For OAuth2 issues, look for domain-specific solutions, not global Jackson changes

### 2. Background Process Management

**Problem:** Running `./gradlew bootRun &` in background makes debugging difficult.

**Issues:**

- Can't see startup logs in real-time
- Output files often empty or missing
- Hard to tell if server started successfully
- Process may not terminate properly

**Solution:** Run server in foreground mode during development:

```bash
# Good - Run in terminal where you can see logs
./gradlew bootRun

# Only use background for automated testing with explicit output redirection
./gradlew bootRun > /tmp/server.log 2>&1 &
```

### 3. Environment Variables

Required: `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`, `OAUTH2_CLIENT_SECRET`, `JASYPT_ENCRYPTOR_PASSWORD` (see startup commands above).

**JASYPT_ENCRYPTOR_PASSWORD:** Encryption key for GDPR-sensitive data (e.g., birth numbers). Use strong password in production. For development, any non-empty value works (e.g., `test-key-123`).

### 4. H2 Database

In-memory, resets on restart. H2 Console: https://localhost:8443/h2-console (`jdbc:h2:mem:klabis`, no auth needed).

### 5. Incremental Testing

Workflow: compile → start server → health check → simple endpoint → complex flow. Check logs immediately on failure. Don't make multiple changes without testing.

### 6. Cross-Module Repository Access

Don't inject repositories from other modules in tests. Use `@Sql` for FK setup instead.

### 7. @Sql String Concatenation

`@Sql` requires compile-time constants - no string concatenation with variables.

### 8. Column Length Constraints

Check schema first, use realistic values matching column limits (e.g., "OOB" not "Organizer A").

### 9. Spring Modulith Test Execution

Create temporal file in java resources to force all tests execution. Delete when not needed. 

## Command-Line Tool Usage

When using command-line tools (sed, awk, cat, etc.), **always quote file paths** to avoid issues with spaces and special
characters:

**Wrong:**

```bash
sed -n '138,148p' src/main/java/com/klabis/users/domain/User.java
```

**Correct:**

```bash
sed -n '138,148p' "src/main/java/com/klabis/users/domain/User.java"
```

## Commit Conventions

Follow Conventional Commits format:

```
<type>(<scope>): <description>

feat(members): add member registration endpoint
fix(email): correct welcome email template
test(members): add tests for registration number generation
docs(api): update authentication examples
refactor(users): extract password validation to service
```

**Common types:** feat, fix, test, docs, refactor, chore, perf, style, ci

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
