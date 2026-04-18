---
name: backend-patterns
description: Backend implementation patterns. Use this skill proactively whenever implementing, modifying, or fixing any backend Java code in this project — including aggregates, domain commands, application services (ports), REST controllers with HATEOAS affordances (klabisLinkTo/klabisAfford), JDBC persistence (memento pattern, repository adapters), domain events and listeners, field-level authorization (@OwnerVisible, @HasAuthority, PatchField), or adding new modules. This is the authoritative source for how Klabis backend code should be structured.
user-invocable: false
version: 0.5.0
---

# Klabis Backend Patterns

Project-specific architecture patterns for the Klabis Spring Modulith application. These patterns are derived from the `members` module as the canonical reference implementation.

For generic framework knowledge, refer to the other `developer:*` skills. This skill covers **Klabis-specific conventions** only.

## Module Package Structure

Every Spring Modulith module follows this exact layout:

```
com.klabis.<module>/
├── domain/                    # Pure domain — NO Spring imports
│   ├── <Aggregate>.java       # Aggregate root (extends KlabisAggregateRoot)
│   ├── <Aggregate>Repository.java  # Domain port interface
│   └── ...value objects, enums
│
├── application/               # Orchestration layer
│   ├── <Feature>Port.java     # @PrimaryPort, Interface with nested command record
│   ├── <Feature>Service.java  # @Service implementation
│   └── <Module>Configuration.java  # @Configuration for module beans (if needed)
│
├── infrastructure/
│   ├── restapi/               # REST controllers, DTOs, mappers
│   │   ├── <Aggregate>Controller.java  # @RestController @PrimaryAdapter
│   │   ├── <Aggregate>Mapper.java      # MapStruct @Mapper
│   │   └── ...request/response records
│   │
│   ├── jdbc/                  # Persistence
│   │   ├── <Aggregate>RepositoryAdapter.java  # @SecondaryAdapter
│   │   ├── <Aggregate>JdbcRepository.java     # Spring Data interface
│   │   └── <Aggregate>Memento.java            # @Table persistence class
│   │
│   └── listeners/             # Cross-module event listeners (if any)
│       └── <Module>EventsListener.java  # @PrimaryAdapter @Component
│
├── <Aggregate>Id.java         # Type-safe ID record — in root if referenced by other modules
├── <Aggregate>CreatedEvent.java  # Domain events — in root if consumed by other modules
└── <Module>Dto.java           # Cross-module read DTO (if needed)
```

**Root vs. domain decision:** Classes referenced by other modules stay in root package (public API). Everything else goes into `domain/`. To check cross-module usage:
```bash
grep -rh "import com.klabis.<module>" src/main/java/com/klabis/<other-modules>/ --include="*.java" | sort -u
```

## Domain Layer

### Aggregate Root

```java
@AggregateRoot
public class Member extends KlabisAggregateRoot<Member, MemberId> {

    // Commands as nested records INSIDE aggregate, annotated @RecordBuilder
    @RecordBuilder public record RegisterMember(MemberId id, RegistrationNumber regNum, ...) {}
    @RecordBuilder public record SuspendMembership(UserId suspendedBy, DeactivationReason reason, String note) {}

    // Factory method — validates and registers domain event
    public static Member register(RegisterMember command) {
        Member member = new Member(command.id(), ...);
        member.registerEvent(MemberCreatedEvent.fromMember(member));
        return member;
    }

    // Reconstruction — bypasses validation, for persistence loading
    public static Member reconstruct(MemberId id, ..., AuditMetadata auditMetadata) { ... }

    // Command handlers — mutate state, register events
    public void handle(SuspendMembership command) {
        this.active = false;
        this.suspendedAt = Instant.now();
        registerEvent(MemberSuspendedEvent.fromMember(this, command));
    }
}
```

### Audit Metadata

All aggregates inherit `AuditMetadata` from `KlabisAggregateRoot` — populated by the persistence layer after save via `updateAuditMetadata()`. The aggregate itself never sets it. Fields: `createdAt`, `createdBy`, `lastModifiedAt`, `lastModifiedBy`, `version` (optimistic locking).

In `reconstruct()`, pass the stored `AuditMetadata` and call `group.updateAuditMetadata(auditMetadata)`. For new aggregates (factory method), leave it null — the Memento sets it after the first save.

Key rules:
- No Spring annotations in domain classes (exception: `org.springframework.util.Assert` is allowed in command records for validation)
- Commands are nested records in the aggregate. Patch commands typically include a `.from(Aggregate)` factory method to populate from current state.
- Separate business factory method (`register()`, `create()`, etc) methods (with validations) and `reconstruct()` (bypass validation, used for loading from DB) factory methods
- Domain events registered via `registerEvent()` inherited from `KlabisAggregateRoot`

### Type-Safe Identifiers

```java
@ValueObject
public record MemberId(UUID value) implements Identifier {
    public UserId toUserId() { return new UserId(value); }  // present only when aggregates have 1:1 relation
    public static MemberId fromUserId(UserId userId) { return new MemberId(userId.uuid()); }  // present only when aggregates have 1:1 relation
}
```

Always create a dedicated `<Aggregate>Id` record. Never pass raw `UUID` between aggregates.

### Value Objects

```java
@ValueObject
public record EmailAddress(String value) {
    public EmailAddress {  // Compact constructor validates
        Objects.requireNonNull(value);
        if (!value.matches(EMAIL_PATTERN)) throw new IllegalArgumentException("...");
        value = value.trim();
    }
    public static EmailAddress of(String value) { return new EmailAddress(value); }
}
```

## Application Service Layer

### Service Interface (Port) with Command Record

```java
@PrimaryPort
public interface RegistrationPort {

    record RegisterNewMember(
        PersonalInformation personalInformation,
        EmailAddress email,
        PhoneNumber phone
    ) {}

    Member registerMember(RegisterNewMember command);
}
```

### Service Implementation

```java
@Service
class RegistrationService implements RegistrationPort {

    private final MemberRepository memberRepository;
    private final UserService userService;  // Cross-module dependency

    @Transactional
    @Override
    public Member suspendMember(MemberId memberId, Member.SuspendMembership command) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberNotFoundException(memberId));
        member.handle(command);
        Member saved = memberRepository.save(member);

        userService.suspendUser(member.getId().toUserId());  // Cross-aggregate coordination
        return saved;
    }
}
```

Key rules:
- `@Transactional` on implementation methods
- Cross-aggregate coordination in the same transaction inside service
- Constructor injection only — no field injection

### Exception Hierarchy

Domain and application exceptions extend `BusinessRuleViolationException` (abstract, unchecked):

```java
// Domain exception — thrown inside aggregate or domain service
public class MemberNotFoundException extends BusinessRuleViolationException { ... }
public class DuplicateRegistrationException extends BusinessRuleViolationException { ... }
```

`MvcExceptionHandler` catches `BusinessRuleViolationException` globally → HTTP 400. Individual subclasses can be caught separately for different HTTP status codes (e.g., 404, 409). No manual conversion in service layer — exceptions propagate naturally.

## REST API Layer

### Controller Annotations Stack

```java
@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/members", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Members", description = "...")
@ExposesResourceFor(Member.class)
@SecurityRequirement(name = "KlabisAuth", scopes = {Authority.MEMBERS_SCOPE})
class MemberController { ... }
```

### `@HasAuthority` Method/Class-Level Authorization

`@HasAuthority(Authority.X)` is the type-safe alternative to `@PreAuthorize("hasAuthority('X:Y')")` for **single-authority global checks**. Use `@PreAuthorize` only when you need boolean logic, parameter access, or context-specific rules.

Class-level applies to all methods; method-level overrides it:

```java
@RestController
@HasAuthority(Authority.MEMBERS_READ)         // default for all endpoints
class MemberController {
    @GetMapping ResponseEntity<?> list() { ... }            // requires MEMBERS:READ

    @PostMapping
    @HasAuthority(Authority.MEMBERS_CREATE)   // overrides class-level
    ResponseEntity<?> create() { ... }
}
```

Enforcement: `HasAuthorityMethodInterceptor` (AuthorizationAdvisor). Failure throws `AccessDeniedException` → 403. Apply at controller layer only, not service layer.

### Field-Level Authorization on Controller Methods

```java
@PatchMapping("/{id}")
@HasAuthority(Authority.MEMBERS_MANAGE)
@OwnerVisible
ResponseEntity<Void> updateMember(@PathVariable @OwnerId UUID id,
                                  @Valid @RequestBody UpdateMemberRequest request) {
    MemberId memberId = new MemberId(id);  // Convert UUID → type-safe ID at boundary
    managementService.updateMember(memberId, UpdateMemberRequestMapper.toCommand(request));
    return ResponseEntity.noContent().build();
}
```

Field-level authorization on request DTO (`@HasAuthority`, `@OwnerVisible` on `PatchField<T>` components) is enforced by `RequestBodyFieldAuthorizationAdvice`. Single command path — no role-based branching in controller.

### HATEOAS — EntityModelWithDomain + Postprocessor Pattern

**Primary choice for creating `EntityModel` instances in controllers that load a domain aggregate.** Controllers focus on returning data; all link/affordance customization lives in a dedicated postprocessor that receives BOTH the DTO-shaped `EntityModel<T>` and the domain aggregate `D`.

**Controller — use `entityModelWithDomain(dto, domain)` instead of `EntityModel.of(dto)`:**

```java
@GetMapping("/{id}")
ResponseEntity<EntityModel<MemberDetailsResponse>> getMember(@PathVariable UUID id) {
    Member member = managementService.getMember(new MemberId(id));
    return ResponseEntity.ok(entityModelWithDomain(memberMapper.toDetailsResponse(member), member));
}
```

The controller does NOT add links/affordances inline. It just wraps the DTO with the domain aggregate and returns. `EntityModelWithDomain<T, D>` is a subclass of `EntityModel<T>` that piggy-backs the domain object; the domain is `@JsonIgnore`-annotated so it never leaks into the response body.

**Postprocessor — extend `ModelWithDomainPostprocessor<T, D>`:**

```java
@MvcComponent
class MemberDetailsPostprocessor extends ModelWithDomainPostprocessor<MemberDetailsResponse, Member> {

    @Override
    public void process(EntityModel<MemberDetailsResponse> dtoModel, Member member) {
        klabisLinkTo(methodOn(MemberController.class).getMember(member.getId().uuid()))
            .map(link -> {
                var self = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(MemberController.class).updateMember(member.getId().uuid(), null, null)));
                if (member.isActive()) {
                    self = self.andAffordances(klabisAfford(methodOn(MemberController.class).suspendMember(member.getId().uuid(), null, null)));
                } else {
                    self = self.andAffordances(klabisAfford(methodOn(MemberController.class).resumeMember(member.getId().uuid(), null)));
                }
                return self;
            })
            .ifPresent(dtoModel::add);
    }
}
```

**Why this pattern:**
- State-driven affordances read from the real aggregate (`member.isActive()`) — no reliance on whether the DTO field has already been filtered by Jackson field-level security.
- Controllers stay small; all hypermedia shaping is externalized. Multiple postprocessors can compose for the same endpoint (e.g. cross-module concerns: a training-groups postprocessor adding a `trainingGroup` link to member details).
- Cross-module postprocessors live in the consuming module — they declare their dependency on the DTO+domain pair explicitly via generics.
- `domainItem` is `@JsonIgnore` — safe from serialization.

**Fallback — plain `EntityModel.of(dto)`:** acceptable only when there is no domain aggregate in scope (e.g., pure DTO projections, synthetic summaries, `RootModel` navigation). For standard aggregate-backed endpoints use the postprocessor pattern.

**Static import:**
```java
import static com.klabis.common.ui.HalFormsSupport.entityModelWithDomain;
```

### HATEOAS Rules (NON-NEGOTIABLE)

Use `klabisLinkTo()` (returns `Optional<WebMvcLinkBuilder>`) and `klabisAfford()` — not standard Spring HATEOAS helpers.

- Links (`withSelfRel()`, `withRel()`) — ONLY for GET endpoints
- Affordances (`klabisAfford()`) — ONLY for POST/PUT/PATCH/DELETE endpoints
- POST/PUT/PATCH/DELETE return 204 No Content or 201 Created with Location header — no response body
- `klabisAfford` handles authorization internally — do not duplicate authorization checks

### Root Navigation Postprocessors

Root navigation (`/api`) is **NOT** an aggregate-backed endpoint — `RootModel` is just a marker for the entry point and there is no domain object to piggy-back. Use a plain `RepresentationModelProcessor<EntityModel<RootModel>>`. Place the class at the end of the file containing the referenced controller, annotated `@MvcComponent`:

```java
@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MemberController.class).listMembers(Pageable.unpaged(), null))
            .ifPresent(link -> model.add(link.withRel("members")));
        return model;
    }
}
```

Same HATEOAS rules apply — no affordances to POST endpoints.

### Choosing the postprocessor type

| Situation | Use |
|---|---|
| Controller loads an aggregate and returns its detail/summary | `ModelWithDomainPostprocessor<Dto, Aggregate>` — controller returns `entityModelWithDomain(dto, aggregate)` |
| Root navigation (`RootModel`) | Plain `RepresentationModelProcessor<EntityModel<RootModel>>` — no domain involved |
| Cross-module link enrichment where consuming module knows only the DTO's marker interface and the publishing controller does not expose the aggregate | Plain `RepresentationModelProcessor<EntityModel<MarkerInterface>>` |

### Current User Parameters (`@ActingUser` / `@ActingMember`)

`CurrentUserArgumentResolver` resolves two annotations in controller method parameters:

**`@ActingUser CurrentUserData`** — resolves the authenticated user from the JWT token. Falls back gracefully when no member is associated with the user (e.g., admin-only users):

```java
@GetMapping("/me")
ResponseEntity<EntityModel<MemberDetailsResponse>> getMyProfile(@ActingUser CurrentUserData currentUser) {
    // currentUser is resolved from the authenticated JWT token
}
```

**`@ActingMember MemberId`** — resolves the authenticated user's `MemberId` from the JWT `memberIdUuid` claim. Throws `MemberProfileRequiredException` (HTTP 403) if the user has no member profile. Use this instead of manually calling `requireMemberProfile(currentUser)`:

```java
@PostMapping("/{id}/invite")
ResponseEntity<Void> inviteMember(@PathVariable UUID id,
                                  @ActingMember MemberId actingMember,
                                  @RequestBody InviteRequest request) {
    // actingMember is guaranteed to be a member — throws 403 otherwise
}
```

Use `@ActingUser` when the endpoint is accessible to non-member users (admins). Use `@ActingMember` when the endpoint requires a member profile.

### DTO → Command Mapping

Use `@Mapper` (MapStruct) for straightforward field mapping; manual mapper class for complex PATCH operations.

## JDBC Persistence Layer (Memento Pattern)

### Memento Class

```java
@Table("members")
class MemberMemento implements Persistable<UUID> {

    @Id @Column("id") private UUID id;

    // Flattened value objects — no nested objects in DB
    @Column("first_name") private String firstName;
    @Column("email") private String email;
    @Column("street") private String street;   // from Address VO

    // Audit (Spring Data auditing)
    @CreatedDate @Column("created_at") private Instant createdAt;
    @LastModifiedDate @Column("modified_at") private Instant lastModifiedAt;
    @Version @Column("version") private Long version;

    @Transient private Member member;   // Domain reference for event delegation
    @Transient private boolean isNew = true;

    // Domain → Memento (save path)
    public static MemberMemento from(Member member) {
        MemberMemento m = new MemberMemento();
        m.id = member.getId().value();
        m.firstName = member.getPersonalInformation().getName().firstName();
        m.email = member.getEmail().value();
        m.member = member;
        m.isNew = (member.getAuditMetadata() == null);
        return m;
    }

    // Memento → Domain (load path) via Member.reconstruct()
    public Member toMember() {
        return Member.reconstruct(new MemberId(this.id), ...);
    }

    // Domain event delegation (Spring Modulith mechanism)
    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.member != null ? this.member.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.member != null) this.member.clearDomainEvents();
    }

    @Override
    public boolean isNew() { return this.isNew; }
}
```

### Repository Adapter

```java
@SecondaryAdapter
@Repository
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJdbcRepository jdbcRepository;

    @Override
    public Member save(Member member) {
        return jdbcRepository.save(MemberMemento.from(member)).toMember();
    }

    @Override
    public Optional<Member> findById(MemberId id) {
        return jdbcRepository.findById(id.value()).map(MemberMemento::toMember);
    }
}
```

### Spring Data Repository

```java
@Repository
interface MemberJdbcRepository extends
        CrudRepository<MemberMemento, UUID>,
        PagingAndSortingRepository<MemberMemento, UUID> {

    Optional<MemberMemento> findByRegistrationNumber(String registrationNumber);

    @Query("SELECT COUNT(*) FROM members WHERE ...")
    int countByBirthYear(@Param("birthYear") int birthYear);
}
```

## Domain Events

### Event Structure

```java
@DomainEvent
public class MemberCreatedEvent {
    private final UUID eventId;        // Always include for idempotency
    private final MemberId memberId;
    private final Instant occurredAt;
    // ... domain-relevant data (denormalized for listener convenience)

    public static MemberCreatedEvent fromMember(Member member) { ... }

    @Override
    public String toString() {
        // Exclude PII fields (GDPR compliance)
        return "MemberCreatedEvent{eventId=" + eventId + ", memberId=" + memberId + "}";
    }
}
```

### Cross-Module Event Listeners

```java
@PrimaryAdapter
@Component
public class MemberEventsListener {

    @ApplicationModuleListener
    public void on(MemberCreatedEvent event) {
        // React to cross-module domain event
    }
}
```

Use `@ApplicationModuleListener` (Spring Modulith) for cross-module event handling. Use `@PrimaryAdapter` on ALL inbound adapters — REST controllers AND event listeners.

## Field-Level Authorization on Response DTOs

Filter individual response fields and HAL+FORMS template properties based on the authenticated user's authorities. Implemented via a custom Jackson 3 `ValueSerializerModifier` — annotations go directly on record components, no interface needed.

### Pattern: Annotated Record (no interface)

Security annotations are placed directly on record components. `FieldSecurityBeanSerializerModifier` (extends `ValueSerializerModifier`) evaluates them during Jackson serialization. This avoids the need for a separate interface — records are final so Spring Security's `AuthorizationAdvisorProxyFactory` (JDK proxy) would require an interface, which is unnecessary boilerplate. Module registered via `@JacksonComponent` on `FieldSecurityJacksonModule`.

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)  // default: field hidden
record MemberDetailResponse(
    String firstName,  // always visible — no security annotation

    @PreAuthorize("hasAuthority('MEMBERS:MANAGE')")
    String birthNumber,  // hidden for unauthorized users

    @HasAuthority(Authority.MEMBERS_MANAGE)
    @HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)  // per-field override
    String bankAccountNumber  // masked as "***" for unauthorized users
) {}
```

Controller returns a plain record — no proxy call needed:

```java
@GetMapping("/{id}")
EntityModel<MemberDetailResponse> getMember(@PathVariable UUID id) {
    MemberDetailResponse response = memberMapper.toDetailResponse(member);
    return EntityModel.of(response)
        .add(klabisLinkTo(methodOn(MemberController.class).getMember(id)).withSelfRel()
            .andAffordances(klabisAfford(methodOn(MemberController.class).updateMember(id, null, null))));
}
```

### Ownership-Based Field Authorization (@OwnerVisible)

Fields can be made accessible to the data owner using `@OwnerVisible`. Uses OR semantics with authority annotations:

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
record MemberDetailResponse(
    @OwnerId MemberId id,                         // owner identifier
    String firstName,                              // always visible
    @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
    String birthNumber,                            // visible to admin OR owner
    @OwnerVisible
    String email,                                  // visible only to owner
    @HasAuthority(Authority.MEMBERS_MANAGE)
    String suspensionNote                          // visible only to admin
) {}
```

Owner discovery: single field whose type converts to UUID via `ConversionService` is used automatically. If ambiguous, annotate with `@OwnerId`. `OwnershipResolver` compares with `KlabisJwtAuthenticationToken.getMemberIdUuid()`.

In collections (`GET /members`), each item is evaluated independently — owner sees more on their own record.

### Key rules

- `OwnershipResolver` is lazy-resolved from `ApplicationContext` — eager injection causes `No ServletContext set` startup error
- `@JsonInclude(NON_NULL)` on the record — denied fields (handled by `NullDeniedHandler`) disappear from JSON
- Class-level `@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)` sets default deny behavior
- Per-field override with `@HandleAuthorizationDenied(handlerClass = MaskDeniedHandler.class)` for masked fields
- Both `@PreAuthorize` (SpEL) and `@HasAuthority` (type-safe) annotations are supported on record components
- `@OwnerVisible` adds ownership-based access with OR semantics
- No interface, no proxy — `FieldSecurityBeanSerializerModifier` handles everything during serialization

### Field-Level Authorization on Request DTOs (PATCH)

`PatchField<T>` components with `@PreAuthorize`, `@HasAuthority`, or `@OwnerVisible` annotations are enforced by `RequestBodyFieldAuthorizationAdvice`. Only provided fields are checked — absent fields are skipped. For `@OwnerVisible`, owner ID is read from the controller method's `@OwnerId @PathVariable` parameter.

```java
record UpdateMemberRequest(
    PatchField<String> email,  // no annotation — anyone can update

    @HasAuthority(Authority.MEMBERS_MANAGE)
    PatchField<String> birthNumber,  // only admin can update — 403 if unauthorized

    @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
    PatchField<String> chipNumber  // admin OR owner can update
) {}
```

Controller with `@OwnerId` path variable:
```java
@PatchMapping("/{id}")
@HasAuthority(Authority.MEMBERS_MANAGE)
@OwnerVisible
ResponseEntity<Void> updateMember(@PathVariable @OwnerId UUID id, @RequestBody UpdateMemberRequest request) { ... }
```

If an unauthorized user sends a provided `PatchField` for a protected field, `FieldAuthorizationException` is thrown → HTTP 403.

### Available denied handlers (`com.klabis.common.security.fieldsecurity`)

| Handler | Behavior | Use case |
|---|---|---|
| `NullDeniedHandler` | Field absent from JSON | Default — hide sensitive fields |
| `MaskDeniedHandler` | Field shows `"***"` | Show field existence without value |

### HAL+FORMS template filtering

`klabisAfford()` automatically filters HAL+FORMS template properties based on the same `@PreAuthorize` / `@HasAuthority` annotations on record component accessors. If user lacks authority for a field, the property is excluded from the PATCH template. No extra configuration needed.

### Reference implementation

- Serializer: `com.klabis.common.security.fieldsecurity.FieldSecurityBeanSerializerModifier`, `SecuredBeanPropertyWriter`
- Request auth: `com.klabis.common.security.fieldsecurity.RequestBodyFieldAuthorizationAdvice`
- Method auth: `com.klabis.common.security.HasAuthorityMethodInterceptor`
- Ownership: `OwnershipResolver`, `DefaultOwnershipResolver`, `@OwnerVisible`, `@OwnerId`
- Handlers: `com.klabis.common.security.fieldsecurity.NullDeniedHandler`, `MaskDeniedHandler`
- Test: `com.klabis.common.security.fieldsecurity.FieldLevelAuthorizationTest`
- HAL+FORMS filtering: `com.klabis.common.ui.HalFormsSupport` (`isPropertyAuthorized`)

## Coding Conventions

### Jackson 3 Annotation Changes (Spring Boot 4)

Spring Boot 4 uses Jackson 3, which moved some packages — but Spring Boot wrapper annotations changed names too:
- `@JsonComponent` → `@JacksonComponent` (Spring Boot annotation)
- `@JsonMixin` → `@JacksonMixin` (Spring Boot annotation)
- Core/databind packages: `tools.jackson.core`, `tools.jackson.databind`
- **Exception**: `@JsonCreator`, `@JsonValue`, `@JsonInclude` stay in `com.fasterxml.jackson.annotation` — NOT moved

### General

- Use package-protected visibility as default for new classes — make public only when accessed from another package
- Use `org.springframework.util.Assert` for parameter validation inside methods and command record compact constructors (not raw `if` throws)
- Use `@NonNull` (from `org.jspecify`) on required service parameters; handle defaults in controller before delegating
- Refactor methods with more than 4 parameters — introduce parameter objects or command records
- Use `@MvcComponent` annotation on components in the presentation (restapi) layer
- Do not use Lombok in domain classes — use records or plain Java
- Use `@RecordBuilder` (from `io.soabase.recordbuilder`) on command records, events, and response DTOs — generates builder classes

## `@MvcComponent` and `@WebMvcTest`

`@MvcComponent` (`com.klabis.common.mvc.MvcComponent`) is a project-specific marker for presentation-layer beans (postprocessors, link processors, MVC helpers). It is meta-annotated `@Component`, but it is NOT a generic alias — `MvcConfiguration` wires it up via a targeted component scan:

```java
@ComponentScan(
    basePackages = "com.klabis",
    includeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = MvcComponent.class),
    useDefaultFilters = false
)
@Configuration
class MvcConfiguration implements WebMvcConfigurer { ... }
```

**Consequences for tests:**
- `@WebMvcTest` auto-loads `MvcConfiguration`, which then scans **all `com.klabis.**` packages** and picks up every `@MvcComponent` bean — cross-package, cross-module.
- **Do NOT** list postprocessors or `@MvcComponent` beans in `@WebMvcTest(controllers = {...})` or `@Import({...})` — it is redundant. They are discovered automatically.
- If a postprocessor's constructor depends on a non-MVC bean (e.g. a JDBC `SomeRepository`), the test must provide it via `@MockitoBean SomeRepository someRepository;`. Do NOT work around this with `@Lazy` on the constructor parameter — `@Lazy` only defers resolution, it doesn't supply the missing bean at runtime.

**Consequences for production code:**
- `@MvcComponent` is the correct annotation for anything in `infrastructure/restapi/` — controllers, postprocessors (`ModelWithDomainPostprocessor`, plain `RepresentationModelProcessor`), Jackson modules, HAL helpers.
- Cross-module postprocessors (e.g. a `groups.familygroup` postprocessor enriching a `Member` response) live in the consuming module and still just need `@MvcComponent`; the central scan finds them regardless of package.

## Additional Resources

For detailed patterns and examples:
- **`references/aggregate-checklist.md`** — Step-by-step checklist for implementing a new aggregate
- **`references/testing-guide.md`** — Testing patterns: unit, repository, controller, integration, E2E
