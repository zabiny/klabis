## Context

The system currently supports Member termination (setting `active=false`) but the corresponding User account remains ACTIVE and enabled. This means terminated members can still authenticate and access protected API endpoints. The `MemberLifecycleE2ETest` test (STEP 11) explicitly tests for this behavior and currently fails because User suspension is not implemented.

**Current State:**
- Member aggregate has `terminateMembership()` command that publishes `MemberTerminatedEvent`
- User aggregate has `AccountStatus` enum (ACTIVE, PENDING_ACTIVATION, SUSPENDED) but no `suspend()` method
- Spring Modulith is configured for event-driven cross-module communication
- Members module already depends on Users module (uses UserId, UserService)

**Existing Patterns:**
- `UserCreatedEventHandler` in users module shows the pattern: `@ApplicationModuleListener` with separate transaction
- User.username equals Member.registrationNumber.value() (this is the link between aggregates)
- Member.id and User.id are different UUIDs (not the same despite similar naming)

## Goals / Non-Goals

**Goals:**
- Automatically suspend User accounts when corresponding Member is terminated
- Enable User reactivation when Member is reactivated (future-proofing)
- Maintain bounded context isolation (users module must not depend on members)
- Use event-driven integration via Spring Modulith

**Non-Goals:**
- Member reactivation UI/API (out of scope, only event/infrastructure support)
- Manual User suspension API (admin-only user suspension remains for future work)
- Soft-delete pattern for Users (accounts persist, only status changes)
- Token revocation on suspension (existing tokens remain valid until expiration - this is acceptable)

## Decisions

### Decision 1: Event-Driven Integration (Spring Modulith)

**Choice:** Users module subscribes to MemberTerminatedEvent via `@ApplicationModuleListener`

**Rationale:**
- Members module already depends on users - cannot create circular dependency
- Event-driven pattern maintains bounded context isolation
- Spring Modulith provides transactional outbox for reliable delivery
- Consistent with existing `UserCreatedEventHandler` pattern
- Separate transaction allows User suspension to retry independently

**Alternatives Considered:**
- Direct service call: Would require users → members dependency (circular)
- Shared kernel with shared interface: Over-engineering for this use case
- Database trigger: Bypasses domain logic, harder to test

### Decision 2: User Lookup via Username

**Choice:** Find User by `username` which equals `Member.registrationNumber.value()`

**Rationale:**
- MemberTerminatedEvent already contains `registrationNumber`
- UserRepository already has `findByUsername(String)` method
- No need to add new repository methods
- Explicit and clear relationship

**Trade-off:** Member.id and User.id are different UUIDs - must use username for lookup, not ID.

### Decision 3: AccountStatus.SUSPENDED (not enabled=false only)

**Choice:** Set both `accountStatus=SUSPENDED` AND `enabled=false`

**Rationale:**
- `AccountStatus.SUSPENDED` is the explicit domain state for suspended accounts
- Spring Security `User.isEnabled()` maps to the `enabled` field
- `User.isAuthenticatable()` checks both status and enabled flag
- Setting both ensures maximum compatibility with Spring Security

```java
public boolean isAuthenticatable() {
    return enabled
           && accountStatus == AccountStatus.ACTIVE
           && accountNonExpired
           && accountNonLocked
           && credentialsNonExpired;
}
```

### Decision 4: Idempotent Event Handlers

**Choice:** Event handlers check if User is already suspended/reactivated before acting

**Rationale:**
- Spring Modulith may retry events
- Multiple terminations of same Member should not cause errors
- No-op if already in target state is safer than throwing exceptions

### Decision 5: Handler Location and Naming

**Choice:** Create `users/integration/MemberTerminatedEventHandler.java`

**Rationale:**
- Separate `integration` package distinguishes cross-module handlers from internal logic
- Consistent with `users/passwordsetup/UserCreatedEventHandler` pattern
- Clear that this is cross-boundary code

## Implementation Details

### User Aggregate Changes

Add two new methods following existing value-oriented pattern:

```java
// Command records
public record SuspendAccount() {}
public record ReactivateAccount() {}

// Factory methods returning new instances (immutable)
public User suspend() {
    return new User(
        this.id,
        this.username,
        this.passwordHash,
        AccountStatus.SUSPENDED,
        this.accountNonExpired,
        this.accountNonLocked,
        this.credentialsNonExpired,
        false  // enabled = false
    );
}

public User reactivate() {
    return new User(
        this.id,
        this.username,
        this.passwordHash,
        AccountStatus.ACTIVE,
        this.accountNonExpired,
        this.accountNonLocked,
        this.credentialsNonExpired,
        true  // enabled = true
    );
}
```

### Member Aggregate Changes

Add reactivation command and event (future-proofing):

```java
// Command
public record ReactivateMembership(UserId reactivatedBy) {}

// Event
public class MemberReactivatedEvent {
    private final UUID eventId;
    private final UserId memberId;
    private final RegistrationNumber registrationNumber;
    private final Instant reactivatedAt;
    private final UserId reactivatedBy;
}
```

### Event Handlers

```java
@Component
@PrimaryAdapter
public class MemberTerminatedEventHandler {

    private final Users users;

    @ApplicationModuleListener
    @Transactional
    public void onMemberTerminated(MemberTerminatedEvent event) {
        users.findByUsername(event.getRegistrationNumber().value())
            .ifPresent(user -> {
                if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
                    users.save(user.suspend());
                }
            });
    }
}
```

## Risks / Trade-offs

### Risk: Event Processing Failure

**Risk:** User suspension fails but Member termination succeeds

**Mitigation:**
- Spring Modulith transactional outbox ensures reliable delivery
- Event handler runs in separate transaction - failure triggers retry
- Idempotent handler safe for retries
- Log failures for monitoring

**Acceptable:** Short gap between termination and suspension is acceptable business risk

### Risk: Missing User Account

**Risk:** Member exists but User account does not exist

**Mitigation:**
- Use `Optional.ifPresent()` - gracefully handle missing User
- Log warning for monitoring
- This should be rare in practice (registration creates both)

**Acceptable:** Edge case, not a blocker

### Risk: Reactivation Without Event

**Risk:** Member reactivated manually (DB update) without publishing event

**Mitigation:**
- All reactivation should go through domain command `handle(ReactivateMembership)`
- Document that direct DB updates bypass event publishing

**Acceptable:** This is a data integrity issue that exists for all event-driven features

## Migration Plan

No data migration needed - this is new behavior only.

**Deployment Steps:**
1. Deploy with event handlers (no impact - no existing terminated members)
2. Feature is active for all new terminations

**Rollback:**
- Simply remove/disable event handlers
- No database changes to rollback

## Open Questions

None - design is straightforward based on existing patterns.
