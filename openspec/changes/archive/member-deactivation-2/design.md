## Context

The system currently supports Member termination (setting `active=false`) but the corresponding User account remains ACTIVE and enabled. This means terminated members can still authenticate and access protected API endpoints. The `MemberLifecycleE2ETest` test (STEP 11) explicitly tests for this behavior and currently fails because User suspension is not implemented.

**Current State:**
- Member aggregate has `terminateMembership()` command that publishes `MemberTerminatedEvent`
- User aggregate has `AccountStatus` enum (ACTIVE, PENDING_ACTIVATION, SUSPENDED) but no `suspend()` method
- Members module already depends on Users module (uses UserId, UserService)
- User.username equals Member.registrationNumber.value() (this is the link between aggregates)
- Member.id and User.id are different UUIDs (not the same despite similar naming)

**Existing Patterns:**
- UserService already provides methods like `findUserByUsername(String)`
- MemberService uses UserId and UserService for member registration
- Direct service calls within same transaction for consistency

## Goals / Non-Goals

**Goals:**
- Automatically suspend User accounts when corresponding Member is terminated
- Enable User reactivation when Member is reactivated (future-proofing)
- Maintain bounded context isolation (users module must not depend on members)
- Use direct service calls within same transaction for consistency

**Non-Goals:**
- Member reactivation UI/API (out of scope, only domain service support)
- Manual User suspension API (admin-only user suspension remains for future work)
- Event-driven cross-module integration (simpler direct call approach)
- Token revocation on suspension (existing tokens remain valid until expiration - this is acceptable)

## Decisions

### Decision 1: Direct Service Call (Not Event-Driven)

**Choice:** MemberService calls UserService directly to suspend/reactivate User accounts

**Rationale:**
- Members module already depends on UserService (no new dependency created)
- Simpler than event-driven cross-module integration
- Atomic operation within same transaction - all or nothing
- Easier to test and reason about
- No event delivery failures to handle
- No need for event handlers in users module

**Alternatives Considered:**
- Event-driven integration: Would require users module to listen to members events (creates circular dependency)
- Shared kernel with shared interface: Over-engineering for this use case
- Database trigger: Bypasses domain logic, harder to test

### Decision 2: User Lookup via UserId

**Choice:** Find User by `UserId` which equals `Member.id.value()` via `MemberId.toUserId()`

**Rationale:**
- `MemberId.toUserId()` provides direct mapping to `UserId` (same UUID value)
- UserRepository already has `findById(UserId)` method via `Users` interface
- No need to add new repository methods
- More direct than username lookup - uses primary key
- Consistent with existing Member-User ID mapping pattern

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

### Decision 4: Idempotent Service Operations

**Choice:** UserService checks if User is already in target state before making changes

**Rationale:**
- Multiple terminations of same Member should not cause errors
- No-op if already in target state is safer than throwing exceptions
- Graceful handling of edge cases (missing User accounts)

### Decision 5: UserService Methods (Not Domain Commands)

**Choice:** Add `suspendUser()` and `reactivateUser()` to UserService (not command records in User)

**Rationale:**
- UserService is the application service layer - coordinates use cases
- User aggregate provides `suspend()` and `reactivate()` methods for state transitions
- Keeps domain model simple - no command records needed for stateless operations
- Consistent with existing UserService patterns

## Implementation Details

### User Aggregate Changes

Add two new methods following existing value-oriented pattern:

```java
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

### UserService Changes

Add two new methods:

```java
public void suspendUser(UserId userId) {
    findById(userId).ifPresent(user -> {
        if (user.getAccountStatus() != AccountStatus.SUSPENDED) {
            save(user.suspend());
        }
    });
}

public void reactivateUser(UserId userId) {
    findById(userId).ifPresent(user -> {
        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            save(user.reactivate());
        }
    });
}
```

### MemberService Changes

Call UserService after Member state changes:

```java
// In terminateMember() method
Member terminated = member.terminateMembership(command);
memberRepository.save(terminated);
userService.suspendUser(member.getId().toUserId());

// In reactivateMember() method (future)
Member reactivated = member.reactivateMembership(command);
memberRepository.save(reactivated);
userService.reactivateUser(member.getId().toUserId());
```

### Member Aggregate Changes

Add reactivation command (future-proofing):

```java
// Command
public record ReactivateMembership(UserId reactivatedBy) {}

// Handler
public Member handle(ReactivateMembership command) {
    if (this.active) {
        throw new IllegalStateException("Member is already active");
    }
    // Create reactivated member with active=true, cleared deactivation fields
    Member reactivated = new Member(...);
    reactivated.registerEvent(MemberReactivatedEvent.fromMember(reactivated, command));
    return reactivated;
}
```

## Risks / Trade-offs

### Risk: Transaction Failure

**Risk:** User suspension fails but Member termination succeeds

**Mitigation:**
- Both operations in same transaction - all or nothing
- If UserService fails, entire transaction rolls back
- Member termination is atomic with User suspension

**Acceptable:** This is actually better than event-driven approach - no inconsistent state possible.

### Risk: Missing User Account

**Risk:** Member exists but User account does not exist

**Mitigation:**
- Use `Optional.ifPresent()` - gracefully handle missing User
- Log warning for monitoring
- This should be rare in practice (registration creates both)

**Acceptable:** Edge case, not a blocker. Member terminates successfully even without User.

### Risk: Performance Impact

**Risk:** Additional database write within same transaction

**Mitigation:**
- User update is simple (status + enabled flag)
- Same transaction - no additional overhead
- Index on username for fast lookup

**Acceptable:** Negligible performance impact for security benefit.

## Migration Plan

No data migration needed - this is new behavior only.

**Deployment Steps:**
1. Deploy with UserService methods and MemberService changes
2. Feature is active for all new terminations/reactivations

**Rollback:**
- Remove MemberService calls to UserService
- No database changes to rollback
- Existing Users remain in current state

## Open Questions

None - design is straightforward based on existing UserService patterns.
