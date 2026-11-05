# Domain & Application Layers

The pure domain (aggregates, value objects, identifiers) and the application services that
orchestrate it. Nothing here may import Spring, with the single exception noted under the
aggregate rules.


## Aggregate Root

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

## Audit Metadata

All aggregates inherit `AuditMetadata` from `KlabisAggregateRoot` — populated by the persistence layer after save via `updateAuditMetadata()`. The aggregate itself never sets it. Fields: `createdAt`, `createdBy`, `lastModifiedAt`, `lastModifiedBy`, `version` (optimistic locking).

In `reconstruct()`, pass the stored `AuditMetadata` and call `group.updateAuditMetadata(auditMetadata)`. For new aggregates (factory method), leave it null — the Memento sets it after the first save.

Key rules:
- No Spring annotations in domain classes (exception: `org.springframework.util.Assert` is allowed in command records for validation)
- Commands are nested records in the aggregate, holding **plain domain types only** — never `JsonNullable` or any other adapter/wire type.
- A PATCH command carries a full end-state snapshot: `update(cmd)` applies every field unconditionally, no per-field "was it set?" branching. It ships a `.from(Aggregate)` factory returning every field at its current value (the baseline). The REST adapter overlays the changed fields onto that baseline — see `rest-controller.md`, "PATCH endpoints".
- Separate business factory method (`register()`, `create()`, etc) methods (with validations) and `reconstruct()` (bypass validation, used for loading from DB) factory methods
- Domain events registered via `registerEvent()` inherited from `KlabisAggregateRoot`

## Type-Safe Identifiers

```java
@ValueObject
public record MemberId(UUID value) implements Identifier {
    public UserId toUserId() { return new UserId(value); }  // present only when aggregates have 1:1 relation
    public static MemberId fromUserId(UserId userId) { return new MemberId(userId.uuid()); }  // present only when aggregates have 1:1 relation
}
```

Always create a dedicated `<Aggregate>Id` record. Never pass raw `UUID` between aggregates.

## Value Objects

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

## Service Interface (Port) with Command Record

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

## Service Implementation

```java
@Service
class ManagementService implements ManagementPort {

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

## Exception Hierarchy

Unchecked base classes in `com.klabis.common.exceptions`, each mapped globally by `MvcExceptionHandler` (ProblemDetail `detail` = exception message):

| Base class | HTTP | Example |
|---|---|---|
| `BusinessRuleViolationException` | 400 | `CannotRemoveLastOwnerException` |
| `ConflictException` (extends `BusinessRuleViolationException`) | 409 | `EventTypeInUseException`, `DisciplineNotEditableException` |
| `ResourceNotFoundException` | 404 | `MemberNotFoundException`, `EventTypeNotFoundException` |

Pick the base class matching the wanted status; do not add a module-level `@RestControllerAdvice` for it. A module-level advice is only for non-standard bodies (e.g. `SuspensionBlockedException` returning a structured warning, or `DuplicateRegistrationException` with an intentionally empty detail) and needs `@Order(Ordered.HIGHEST_PRECEDENCE)` to win over the global handler.

No manual conversion in the service layer — exceptions propagate to the handler.

## Adding a user-facing error

Every concrete subclass of the five bases must implement `com.klabis.common.exceptions.UserFacingError` (`ErrorTypeCatalogueTest` fails otherwise). The backend reports the kind of error and its parameters; the Czech wording lives in the frontend (`labels.errorTypes`, ADR-012).

1. **Exception.** `class XxxException extends <base> implements UserFacingError`. The type is derived from the class name (`XxxException` -> `/errors/xxx`); override `errorType()` and list all values in `errorTypes()` when one class covers several rules. `getMessage()` stays English (it goes into `detail` and the log).
2. **Parameters by name** in `messageParameters()`: names, labels, enum codes, ISO dates, amounts as `{amount, currency}` (`Money.asMessageParameter()`). Never identifiers. Do not use `type`, `title`, `status`, `detail`, `instance` or `field` as a name (use e.g. `invitationStatus`).
3. **Field.** If the error concerns one form field, return the request property name from `field()`; leave it out when several forms share the input. Add a reflection test that the name is a record component of the generated request DTO.
4. **Names from identifiers.** If the exception only knows an id, add an advice (`@Order(Ordered.HIGHEST_PRECEDENCE)`) that resolves the name through a port of the module and passes it to `UserFacingErrorResponses.of(ex, status, title, extraParameters)`; leave the parameter out when the name cannot be resolved or the lookup throws. Plain exceptions on a base need no advice — `MvcExceptionHandler` emits type, parameters and field.
5. **Spec.** Add `/errors/xxx` to the `ErrorType` enum in `docs/openapi/spec/_shared/problem.yaml` with a comment naming status, parameters (with types) and field; then `./gradlew openapiBundle` and `cd frontend && npm run openapi`.
6. **Tests.** Assert type, parameters, field and absence of identifiers (`ProblemDetailAssertions.noIdentifiersInParameters()`) in a controller test, and the type/parameters in a unit test. `ErrorTypeCatalogueTest` checks the enum entry.
7. **Czech template** in `frontend/src/localization/labels.ts` (`labels.errorTypes`, keyed by the type; `tsc` fails while a value is missing) with a wording for a missing optional parameter, plus a `labels` test.

Internal invariants (`Assert`, `IllegalArgumentException`) stay generic and reach the user as `/errors/invalid-request`; promote one to a typed exception only when a user can realistically trigger it.
