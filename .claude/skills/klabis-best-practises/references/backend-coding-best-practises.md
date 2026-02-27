
# Coding best practises

**Root Navigation Postprocessors:**
- Add module collection links to `/api` root via `RepresentationModelProcessor<EntityModel<RootModel>>`
- Pattern: Create inner class in controller, annotate with `@Component`
- Example: `CalendarRootPostprocessor` adds "calendar" rel for calendar items collection
- Use `Pageable.unpaged()` for collection links (not `null`)

**Parameter Handling Pattern:**
- Service layer: Use `@NonNull` for required business parameters (strict validation)
- Controller layer: Handle default values before delegating to service
- Example: Date range is required in service, but defaults to current month in controller
- Keeps business logic clean while providing good UX

# Coding conventions

## Rules

- NON-NEGOTIABLE: use HalFormsSupport#klabisAfford and HalFormsSupport#klabisLinkTo to prepare HATEOAS navigation in controllers (never use WebMvcLinkBuilder#linkTo and WebMvcLinkBuilder#afford) methods.
- **HATEOAS link/affordance rules**: Links ONLY for GET endpoints, affordances ONLY for POST/PUT/DELETE/PATCH endpoints
- RepresentationModelProcessor must follow same HATEOAS rules as controllers (no links to POST endpoints)
- klabisAfford handles authorization internally - don't duplicate authorization checks in controllers and processors
- **Response bodies**: POST/PUT/PATCH/DELETE endpoints return 204 No Content (empty body) or 201 Created with Location header only

## Java Best Practices

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
- **`reconstruct()` method is for persistence layer ONLY** — use it only in JDBC/Memento classes to load aggregates
  from DB.

## Naming conventions

- if tested class is set in Test's attribute, name such attribute as `testedSubject`
- attribute with mocked instance shall have `Mock` postfix in the name (for example `userDetailServiceMock`)

## Security Considerations

- Sensitive data (e.g., rodne cislo) encrypted with Jasypt
- Passwords hashed with BCrypt
- Tokens stored as SHA-256 hashes, never plain text

# Common Development Pitfalls

## 1. Jackson Configuration ⚠️ **CRITICAL WARNING**

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

## 2. Background Process Management

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

## 3. Environment Variables

Required: `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD`, `OAUTH2_CLIENT_SECRET`, `JASYPT_ENCRYPTOR_PASSWORD` (see startup commands above).

**JASYPT_ENCRYPTOR_PASSWORD:** Encryption key for GDPR-sensitive data (e.g., birth numbers). Use strong password in production. For development, any non-empty value works (e.g., `test-key-123`).

## 4. H2 Database

In-memory, resets on restart. H2 Console: https://localhost:8443/h2-console (`jdbc:h2:mem:klabis`, no auth needed).

## 5. Incremental Testing

Workflow: compile → start server → health check → simple endpoint → complex flow. Check logs immediately on failure. Don't make multiple changes without testing.

## 6. Cross-Module Repository Access

Don't inject repositories from other modules in tests. Use `@Sql` for FK setup instead.

## 7. @Sql String Concatenation

`@Sql` requires compile-time constants - no string concatenation with variables.

## 8. Column Length Constraints

Check schema first, use realistic values matching column limits (e.g., "OOB" not "Organizer A").

## 9. Spring Modulith Test Execution

Create temporal file in java resources to force all tests execution. Delete when not needed. 
