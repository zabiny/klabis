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
- A PATCH command carries a full end-state snapshot: `update(cmd)` applies every field unconditionally, no per-field "was it set?" branching. It ships a `.from(Aggregate)` factory returning every field at its current value (the baseline). The REST adapter overlays the changed fields onto that baseline — see rest-adapter.md, "PATCH endpoints".
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

## Exception Hierarchy

Domain and application exceptions extend `BusinessRuleViolationException` (abstract, unchecked):

```java
// Domain exception — thrown inside aggregate or domain service
public class MemberNotFoundException extends BusinessRuleViolationException { ... }
public class DuplicateRegistrationException extends BusinessRuleViolationException { ... }
```

`MvcExceptionHandler` catches `BusinessRuleViolationException` globally → HTTP 400. Individual subclasses can be caught separately for different HTTP status codes (e.g., 404, 409). No manual conversion in service layer — exceptions propagate naturally.
