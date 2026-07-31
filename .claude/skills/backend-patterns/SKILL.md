---
name: backend-patterns
description: Backend implementation patterns. Use this skill proactively whenever implementing, modifying, or fixing any backend Java code in this project — including aggregates, domain commands, application services (ports), REST controllers with HATEOAS affordances (klabisLinkTo/klabisAfford), JDBC persistence (memento pattern, repository adapters), domain events and listeners, field-level authorization (@OwnerVisible, @HasAuthority, JsonNullable), or adding new modules. This is the authoritative source for how Klabis backend code should be structured.
user-invocable: false
version: 0.6.0
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

### Cross-Module Ports Live in `<module>.application`

Ports consumed across module boundaries (jMolecules `@PrimaryPort` / `@SecondaryPort` used by another Modulith module) live in the `<module>.application` package, exposed via `@NamedInterface("application")` declared in that package's `package-info.java` — **not** in the module root package.

```java
// com/klabis/<module>/application/package-info.java
@org.springframework.modulith.NamedInterface("application")
package com.klabis.<module>.application;
```

Canonical examples: `events.application` (`EventDataProvider`, `EventScheduleQuery`), `members.application` (`MemberFinancialStatePort`, implemented by finance's `MemberFinancialStateAdapter`), `finance.application`. A consuming module imports the port from the foreign `<module>.application` named interface.

**A module depends only on another module's PRIMARY port — never on a foreign repository or any other secondary port.** Reach for the other module's `@PrimaryPort` application service; do not inject its `<Aggregate>Repository`. Example: `KlabisUserDetailsService` consumes `com.klabis.common.users.application.PermissionService` (a primary port), not `UserPermissionsRepository` (a secondary port). The Modulith `ModuleStructureVerificationTest` enforces these named-interface boundaries.

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

Field-level authorization on request DTO (`@HasAuthority`, `@OwnerVisible` on `JsonNullable<T>` components) is enforced by `RequestBodyFieldAuthorizationAdvice`. Single command path — no role-based branching in controller.

### HATEOAS — Controllers Return Plain DTOs; HalResponseBodyAdvice Wraps Them

**Canonical pattern for all new/migrated controllers.** Since the migration to spec-first OpenAPI generation, controller methods must return the plain JSON-payload type from the generated API interface (`ResponseEntity<SomeResponse>` / `ResponseEntity<Page<SomeResponse>>`) — the generator does not produce `EntityModel`/`PagedModel` return types. Hypermedia wrapping happens **after** the controller returns, via `HalResponseBodyAdvice` (a `ResponseBodyAdvice` in `com.klabis.common.ui`), driven by a request-scoped `HalResponseContext` that the controller populates with the domain object(s) behind the DTO.

**Controller — return the plain DTO, stash the domain object(s) in `HalResponseContext` before returning:**

```java
@GetMapping("/{id}")
ResponseEntity<MemberDetailsResponse> getMember(@PathVariable UUID id, @ActingUser CurrentUserData currentUser) {
    Member member = managementService.getMemberAndRecordView(new MemberId(id), currentUser.userId(), ...);

    HalResponseContext.setDomain(member);          // must run after everything that can throw
    return ResponseEntity.ok(memberMapper.toDetailsResponse(member));
}
```

For a paginated collection, use `setDomainList` — same order as the DTO `Page` content, paired 1:1 by index:

```java
@GetMapping
ResponseEntity<Page<MemberSummaryResponse>> listMembers(@ParameterObject Pageable pageable, ...) {
    Page<Member> memberPage = memberRepository.findAll(filter, pageable);

    HalResponseContext.setDomainList(memberPage.getContent());
    return ResponseEntity.ok(memberPage.map(memberMapper::toSummaryResponse));
}
```

**Always call `HalResponseContext.set*` last, after any code that can throw.** If the controller throws afterwards, `MvcExceptionHandler` returns a `ProblemDetail`; `HalResponseBodyAdvice` detects that and clears the context instead of wrapping the error body, but only if nothing between `set*` and the exception can leave stale context data for a *different* concern.

**What the advice does, automatically, with no controller involvement:**
- Single DTO → wraps it in `EntityModelWithDomain<T, D>` and runs it through every `RepresentationModelProcessor` bean — including `ModelWithDomainPostprocessor<Dto, Aggregate>` postprocessors.
- `Page<Dto>` → runs it through `PagedResourcesAssembler`, pairing each DTO with its domain object via `HalResponseContext`'s stashed list, then derives the **self link directly from the current request's path and query parameters** (no `klabisLinkTo` call needed for the self link — the controller method already ran and passed authorization for exactly this request).
- Non-HAL content types (e.g. `MemberOptionResponse` served as plain `application/json`) are left untouched — the advice checks `selectedContentType` and only wraps `HAL_JSON`/`HAL_FORMS_JSON` responses.
- A `ProblemDetail` error body is never wrapped, and the context is cleared so nothing leaks into a later request on the same thread pool.

**Postprocessor — extend `ModelWithDomainPostprocessor<T, D>`, which receives the DTO-shaped `EntityModel<T>` and the domain aggregate `D`:**

```java
@MvcComponent
class MemberDetailsPostprocessor extends ModelWithDomainPostprocessor<MemberDetailsResponse, Member> {

    @Override
    public void process(EntityModel<MemberDetailsResponse> dtoModel, Member member) {
        klabisLinkTo(methodOn(MembersApi.class).getMember(member.getId().uuid(), null))
            .map(link -> {
                var self = link.withSelfRel()
                    .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(member.getId().uuid(), null, null)));
                if (member.isActive()) {
                    self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).suspendMember(member.getId().uuid(), null, null)));
                } else {
                    self = self.andAffordances(klabisAfford(methodOn(MembersApi.class).resumeMember(member.getId().uuid(), null)));
                }
                return self;
            })
            .ifPresent(dtoModel::add);
    }
}
```

**Collection-level affordances (not per item) go on the `PagedModel` itself**, in a plain `RepresentationModelProcessor<PagedModel<EntityModel<Dto>>>` — the self link already exists (built by the advice), this processor only adds affordances that point at *other* endpoints:

```java
@MvcComponent
class MemberListPostprocessor implements RepresentationModelProcessor<PagedModel<EntityModel<MemberSummaryResponse>>> {

    @Override
    public PagedModel<EntityModel<MemberSummaryResponse>> process(PagedModel<EntityModel<MemberSummaryResponse>> pagedModel) {
        pagedModel.mapLink(IanaLinkRelations.SELF, selfLink -> (Link) selfLink
                .andAffordances(klabisAfford(methodOn(MembersApi.class).updateMember(null, null, null)))
                .andAffordances(klabisAfford(methodOn(RegistrationApi.class).registerMember(null, null))));
        return pagedModel;
    }
}
```

**Why this pattern:**
- Controllers return the exact type the OpenAPI-generated API interface requires — no `EntityModel`/`PagedModel` in the method signature, so the generated interface can be implemented directly once that migration lands for a module.
- State-driven affordances still read from the real aggregate (`member.isActive()`) — the postprocessor pipeline is unchanged, only how it gets invoked (advice vs. HATEOAS's own `HandlerMethodReturnValueHandler`, which only fires for return values that are already a `RepresentationModel`).
- The self link for a collection no longer needs a `klabisLinkTo(methodOn(...).listMembers(pageable, q, status, null))` call re-deriving the current request in the controller — it's built once, generically, by the advice for every paginated endpoint.
- Non-aggregate-backed responses (pure projections like `MemberOptionResponse`, served as plain JSON) are naturally skipped — no `HalResponseContext` entry means the advice passes the body through unchanged.

### HATEOAS Rules (NON-NEGOTIABLE)

Use `klabisLinkTo()` (returns `Optional<WebMvcLinkBuilder>`) and `klabisAfford()` — not standard Spring HATEOAS helpers.

- **`methodOn(...)` takes the generated `*Api` interface, never the controller class.** Write
  `methodOn(MembersApi.class)`, not `methodOn(MemberController.class)`. Java does not inherit
  parameter annotations from an interface, so an affordance recorded against the implementation only
  finds `@RequestBody` if that override happens to repeat it. When it does not, `HalFormsSupport`
  silently skips `HalFormsInputPayloadMetadata` and the `_templates` entry comes back with every
  field `readOnly: true` — no error, no failing link assertion, just a form the UI cannot submit.
  `AffordanceRoutingArchitectureTest` fails the build if a controller class reaches `methodOn`.
- Links (`withSelfRel()`, `withRel()`) — ONLY for GET endpoints
- Affordances (`klabisAfford()`) — ONLY for POST/PUT/PATCH/DELETE endpoints
- POST/PUT/PATCH/DELETE return 204 No Content or 201 Created with Location header — no response body
- `klabisAfford` handles authorization internally — do not duplicate authorization checks

#### Which annotations belong on the override

The interface is the declaration site for everything the framework reads. The override carries the
method body and nothing else.

| Annotation | Where it belongs | Why |
|---|---|---|
| `@RequestBody`, `@RequestParam`, `@PathVariable` | interface only | Spring MVC and `HalFormsSupport` both read them from there |
| `@NotNull`, `@Size`, `@Pattern`, … | interface only | see the HV000151 note below |
| `@Valid` | either | a cascade marker, not a constraint — repeating it is legal |
| `@Parameter`, `@Operation`, `@ApiResponse` | interface only, and generated | the generator emits them from the spec (`documentationProvider=springdoc`); a hand-written copy on the controller only drifts |

**Bean Validation is all-or-nothing.** Hibernate Validator rejects an override that *redefines* the
parameter constraint configuration of the method it overrides (`ConstraintDeclarationException:
HV000151`), and it compares the parameter list as a whole. So removing `@RequestBody` from a method
whose sibling parameter still carries `@NotNull` produces a signature that differs from the
interface's and fails **at request time**, not at compile time. Either the override declares the
interface's full constraint set, or none of it. Prefer none.

### Root Navigation Postprocessors

Root navigation (`/api`) is **NOT** an aggregate-backed endpoint — `RootModel` is just a marker for the entry point and there is no domain object to piggy-back. Use a plain `RepresentationModelProcessor<EntityModel<RootModel>>`. Place the class at the end of the file containing the referenced controller, annotated `@MvcComponent`:

```java
@MvcComponent
class MembersRootPostprocessor implements RepresentationModelProcessor<EntityModel<RootModel>> {
    @Override
    public EntityModel<RootModel> process(EntityModel<RootModel> model) {
        klabisLinkTo(methodOn(MembersApi.class).listMembers(Pageable.unpaged(), null))
            .ifPresent(link -> model.add(link.withRel("members")));
        return model;
    }
}
```

Same HATEOAS rules apply — no affordances to POST endpoints.

### Choosing the postprocessor type

| Situation | Use |
|---|---|
| Controller loads an aggregate and returns its detail/summary | `ModelWithDomainPostprocessor<Dto, Aggregate>` — controller calls `HalResponseContext.setDomain(aggregate)` before returning the plain DTO |
| Collection-level affordances to other endpoints | Plain `RepresentationModelProcessor<PagedModel<EntityModel<Dto>>>` — the self link itself is built by `HalResponseBodyAdvice`; this processor only adds affordances |
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

Controller returns a plain record — no proxy call needed. Field security applies during Jackson serialization regardless of when in the response pipeline the DTO gets wrapped into `EntityModel` (see the HATEOAS section above):

```java
@GetMapping("/{id}")
ResponseEntity<MemberDetailResponse> getMember(@PathVariable UUID id) {
    Member member = managementService.getMember(new MemberId(id));
    HalResponseContext.setDomain(member);
    return ResponseEntity.ok(memberMapper.toDetailResponse(member));
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

`JsonNullable<T>` components with `@PreAuthorize`, `@HasAuthority`, or `@OwnerVisible` annotations are enforced by `RequestBodyFieldAuthorizationAdvice`. Only present fields are checked — absent (undefined) fields are skipped. An explicit `null` counts as present, so it is still authorized. For `@OwnerVisible`, owner ID is read from the controller method's `@OwnerId @PathVariable` parameter.

```java
record UpdateMemberRequest(
    JsonNullable<String> email,  // no annotation — anyone can update

    @HasAuthority(Authority.MEMBERS_MANAGE)
    JsonNullable<String> birthNumber,  // only admin can update — 403 if unauthorized

    @HasAuthority(Authority.MEMBERS_MANAGE) @OwnerVisible
    JsonNullable<String> chipNumber  // admin OR owner can update
) {}
```

Controller with `@OwnerId` path variable:
```java
@PatchMapping("/{id}")
@HasAuthority(Authority.MEMBERS_MANAGE)
@OwnerVisible
ResponseEntity<Void> updateMember(@PathVariable @OwnerId UUID id, @RequestBody UpdateMemberRequest request) { ... }
```

If an unauthorized user sends a present `JsonNullable` for a protected field, `FieldAuthorizationException` is thrown → HTTP 403.

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
