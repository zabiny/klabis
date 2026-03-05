## Context

### Current State

The member registration flow creates both Member and User aggregates in a single transaction within
`RegistrationService`. The current implementation:

```
RegistrationService.registerMember()
  ├─▶ UserService.createUserPendingActivation(username, passwordHash, authorities)
  │     └─▶ UserCreatedEvent(userId, username, accountStatus)  // NO email
  ├─▶ Member.createWithId(sharedId, ...)
  │     └─▶ MemberCreatedEvent(memberId, firstName, ..., email)  // HAS email
  └─▶ Transaction commit
        ├─▶ UserCreatedEvent → (no handler, ignored)
        └─▶ MemberCreatedEvent → MemberCreatedEventHandler sends password setup email
```

**Problem:** `MemberCreatedEventHandler` in the members module handles password setup, which is a User domain concern.
The User aggregate doesn't have email information, so UserCreatedEvent cannot trigger password setup.

**Constraints:**

- User domain should own password-related operations (DDD principle)
- Email is personal data owned by Member aggregate, not User
- Members → Users dependency is allowed (no circular dependency)
- Spring Modulith event-driven architecture must be maintained
- No breaking changes to public APIs

### Stakeholders

- **Users module:** Owns authentication, password lifecycle, user account management
- **Members module:** Owns member personal data, registration process
- **RegistrationService:** Coordinates member+user creation (lives in members module)

## Goals / Non-Goals

**Goals:**

- Move password setup email logic from members module to users module
- User module handles complete user lifecycle including activation
- Clean separation of concerns - Member doesn't know about password operations
- Maintain transactional integrity during registration (Member ID = User ID)
- No breaking changes to existing APIs

**Non-Goals:**

- Moving email from Member to User aggregate (email stays in Member)
- Changing the email template/subject beyond username greeting
- Modifying the password token generation logic
- Changing the transaction boundary (still single transaction for registration)
- Splitting Member and User creation into separate transactions

## Decisions

### 1. UserCreationParams Builder Pattern

**Decision:** Introduce `UserCreationParams` record with builder pattern for optional PII.

```java
public record UserCreationParams(
    String username,
    String passwordHash,
    Set<Authority> authorities,
    String email  // Optional PII
) {
    public static Builder builder() { ... }
}
```

**Rationale:**

- **Clean API:** Builder makes optional email explicit without method overloading explosion
- **Type safety:** Record ensures immutability and clear contract
- **Forward compatible:** Easy to add more optional fields in future (firstName, lastName, etc.)
- **Alternative considered:** Method overloading `createUserPendingActivation(..., String email)` - rejected as less
  readable when email is optional

**Trade-off:** Additional class vs. cleaner API. Builder is worth it for clarity.

### 2. UserCreatedEvent Enhancement

**Decision:** Add optional `email` field to `UserCreatedEvent` and new factory method.

```java
public class UserCreatedEvent {
    private final String email;  // Optional, may be null

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    // New factory method
    public static UserCreatedEvent fromUserWithEmail(User user, String email) {
        return new UserCreatedEvent(..., email);
    }
}
```

**Rationale:**

- **Event completeness:** UserCreatedEvent can now carry all data needed for password setup
- **Optional field:** Email is null for non-registration scenarios (admin-created users, etc.)
- **Factory method:** `fromUserWithEmail()` makes it explicit that PII is being added from external context
- **Alternative considered:** Separate `UserCreatedWithEmailEvent` - rejected as over-engineering, same event type

**Trade-off:** PII in event (GDPR consideration) vs. complete event. Mitigated by: event handlers already handle PII,
audit logging doesn't log full event.

### 3. Username in Email Greeting

**Decision:** Use `username` (registration number) instead of `firstName` in password setup email greeting.

**Before:** "Dear Jan Novák,"
**After:** "Dear ZBM0501,"

**Rationale:**

- **Domain alignment:** User aggregate only knows username, not firstName
- **No duplication:** Don't duplicate personal data in User context
- **Clarity:** Registration number is unique identifier, members know it
- **Alternative considered:** Add firstName to User aggregate - rejected as mixing concerns (personal data belongs in
  Member)

**Trade-off:** Less personalized greeting vs. clean domain boundaries. Acceptable - username is meaningful to members.

### 4. UserCreatedEventHandler in Users Module

**Decision:** Create `UserCreatedEventHandler` in users module to handle password setup.

```java
@Component
public class UserCreatedEventHandler {
    @ApplicationModuleListener
    public void onUserCreated(UserCreatedEvent event) {
        if (event.isPendingActivation() && event.getEmail().isPresent()) {
            // Generate token and send email
        }
    }
}
```

**Rationale:**

- **DDD correctness:** Password logic lives in User module where it belongs
- **Event-driven:** Maintains Spring Modulith async event processing
- **Testability:** Isolated handler, easy to unit test
- **Alternative considered:** Keep MemberCreatedEventHandler - rejected as violates SRP

### 5. MemberCreatedEventHandler Simplification

**Decision:** Remove password setup logic from `MemberCreatedEventHandler`, keep only logging.

```java
@Component
public class MemberCreatedEventHandler {
    @ApplicationModuleListener
    public void onMemberCreated(MemberCreatedEvent event) {
        log.info("Member created: {}", event.getRegistrationNumber());
        // Password setup removed - handled by UserCreatedEventHandler
    }
}
```

**Rationale:**

- **Single responsibility:** Member handler focuses on member-related concerns
- **Future-proof:** Ready for other member creation logic (welcome emails, notifications, etc.)
- **Alternative considered:** Delete handler entirely - rejected, we'll need it for future member lifecycle events

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                    AFTER: Registration Flow                     │
└─────────────────────────────────────────────────────────────────┘

RegistrationService (members module)
    │
    ├─▶ UserCreationParams.builder()
    │       .username(registrationNumber.getValue())
    │       .passwordHash(passwordHash)
    │       .authorities(Set.of(Authority.MEMBERS_READ))
    │       .email(email.value())  // ← PII from Member request
    │       .build()
    │
    ├─▶ UserService.createUserPendingActivation(params)
    │       └─▶ UserCreatedEvent(userId, username, email, status)  // HAS email
    │
    ├─▶ Member.createWithId(sharedId, ...)
    │       └─▶ MemberCreatedEvent(...)
    │
    └─▶ Transaction commit
            │
            ├─▶ UserCreatedEvent ─────────────┐
            │                                  ▼
            │                          UserCreatedEventHandler (users)
            │                          ┌─────────────────────────┐
            │                          │ 1. Check PENDING_ACTIVATION │
            │                          │ 2. Generate token       │
            │                          │ 3. Send email           │
            │                          │    To: email from event │
            │                          │    Greeting: "Dear ZBM0501" │
            │                          └─────────────────────────┘
            │
            └─▶ MemberCreatedEvent ────────▶ MemberCreatedEventHandler
                                                   └─▶ Log only
```

## Data Flow

### Registration Flow

1. **RegistrationService** receives `RegisterMemberRequest` with member data including email
2. Create value objects (EmailAddress, PhoneNumber, Address, etc.)
3. Build `UserCreationParams` with username, passwordHash, authorities, **email**
4. Call `userService.createUserPendingActivation(params)`
5. **UserServiceImpl** creates User aggregate with PENDING_ACTIVATION status
6. User registers `UserCreatedEvent` with email via `fromUserWithEmail()`
7. **UserServiceImpl** creates UserPermissions and saves both
8. Return shared UserId to RegistrationService
9. **RegistrationService** creates Member with same ID
10. Transaction commits
11. Spring Modulith publishes both events
12. **UserCreatedEventHandler** sends password setup email
13. **MemberCreatedEventHandler** logs member creation

### Event Handler Logic

**UserCreatedEventHandler:**

```java
if (event.getAccountStatus() == PENDING_ACTIVATION) {
    if (event.getEmail().isPresent()) {
        User user = userService.findById(event.getUserId());
        GeneratedTokenResult token = passwordSetupService.generateToken(user);
        passwordSetupService.sendPasswordSetupEmail(
            event.getUsername(),  // "ZBM0501"
            event.getEmail().get(),
            token.plainToken()
        );
    } else {
        log.warn("No email for user {}, skipping password setup", event.getUserId());
    }
}
```

## Risks / Trade-offs

### Risk 1: PII in UserCreatedEvent

**Risk:** Email (PII) now flows through UserCreatedEvent, which is a User domain event.

**Mitigation:**

- Event is handled within same JVM, not serialized externally
- Existing handlers already know how to handle PII (PasswordSetupService)
- Audit logging uses `toString()` which excludes PII (already implemented)
- Spring Modulith outbox pattern ensures reliable delivery without PII leak

### Risk 2: Email Duplication

**Risk:** Email exists in both MemberCreatedEvent and UserCreatedEvent during registration.

**Mitigation:**

- Accepted duplication for clean separation of concerns
- Both events published in same transaction - consistency guaranteed
- Email is source of truth in Member aggregate, UserCreatedEvent is just notification
- No additional storage cost - events are transient

### Risk 3: Backward Compatibility

**Risk:** Existing UserCreatedEvent consumers might expect no email field.

**Mitigation:**

- Email is optional (nullable), defaults to null
- `getEmail()` returns `Optional<String>` - safe API
- Existing `fromUser()` factory method unchanged - sets email to null
- Only new `fromUserWithEmail()` sets email
- No breaking changes to public APIs

### Trade-off: Username vs. FirstName in Email

**Trade-off:** Less personalized email greeting ("Dear ZBM0501" vs "Dear Jan") for cleaner domain boundaries.

**Acceptable because:**

- Registration number is meaningful identifier for members
- Email body still contains member's name from MemberCreatedEvent (if needed in future)
- Keeps User aggregate focused on authentication, not personal data

## Migration Plan

### Phase 1: Prepare New Components (Non-Breaking)

1. Create `UserCreationParams` record with builder
2. Add `email` field to `UserCreatedEvent` (optional, nullable)
3. Add `fromUserWithEmail()` factory method to `UserCreatedEvent`
4. Add new `createUserPendingActivation(UserCreationParams)` to `UserService` interface
5. Implement new method in `UserServiceImpl` (keep existing method as delegate)
6. Create `UserCreatedEventHandler` in users module (currently no-op, waits for events with email)

**Tests:** Unit tests for new components, integration tests pass (no email in events yet).

### Phase 2: Wire Up RegistrationService

7. Update `RegistrationService.registerMember()` to use builder:
   ```java
   UserCreationParams params = UserCreationParams.builder()
       .username(registrationNumber.getValue())
       .passwordHash(passwordHash)
       .authorities(Set.of(Authority.MEMBERS_READ))
       .email(email.value())
       .build();
   UserId sharedId = userService.createUserPendingActivation(params);
   ```

**Tests:** Update integration tests, verify UserCreatedEvent has email.

### Phase 3: Activate UserCreatedEventHandler

8. `UserCreatedEventHandler` now receives events with email
9. Verify password setup emails are sent correctly
10. Add logging to confirm UserCreatedEventHandler is processing events

**Tests:** E2E test for registration flow confirms password setup email sent.

### Phase 4: Cleanup MemberCreatedEventHandler

11. Remove password setup logic from `MemberCreatedEventHandler`
12. Keep only logging/audit trail
13. Remove dependencies: `PasswordSetupService`, `UserService`

**Tests:** Update MemberCreatedEventHandler tests, verify no password setup calls.

### Phase 5: Remove Dead Code (Optional)

14. Consider removing old `createUserPendingActivation(String, String, Set)` method if unused
15. Or keep for backward compatibility / non-registration scenarios

**Rollback Strategy:**

- Each phase is independently revertible
- Phase 1-2 are additive (no breaking changes)
- Phase 3 activates new flow (old flow still works as fallback)
- Phase 4 removes old flow (commit after verification)
- Keep git history clean with atomic commits per phase

## Open Questions

### Q1: Should User aggregate store email permanently?

**Status:** **DECIDED - No**

**Reasoning:** Email is personal data owned by Member. User aggregate is for authentication only. Keeping email in
UserCreatedEvent (transient) is sufficient for password setup. If future requirements need email in User (e.g., OAuth2
profile), revisit this decision.

### Q2: Should we add firstName/lastName to User aggregate for better emails?

**Status:** **DECIDED - No**

**Reasoning:** Violates domain boundaries. User = authentication, Member = personal data. Username (registration number)
is acceptable greeting. If business requires personalization, can enhance email template or add event correlation later.

### Q3: What about non-registration user creation (admin creates user)?

**Status:** **ANSWERED**

**Answer:** Old `createUserPendingActivation(username, passwordHash, authorities)` method remains. It delegates to new
method with `email = null`. UserCreatedEventHandler skips password setup when email is absent. Admin can manually set
password or separate workflow handles it.

### Q4: Should we use a separate event type `UserCreatedWithPIIEvent`?

**Status:** **DECIDED - No**

**Reasoning:** Over-engineering. Single event type with optional field is simpler. Factory method `fromUserWithEmail()`
makes intent explicit. If event types diverge significantly in future, can split then (YAGNI principle).

## Testing Strategy

### Unit Tests

- `UserCreationParamsTest`: Builder pattern, validation
- `UserCreatedEventTest`: Factory methods, email field, Optional handling
- `UserCreatedEventHandlerTest`: Password setup logic, email present/absent scenarios
- `MemberCreatedEventHandlerTest`: Verify only logging, no password setup

### Integration Tests

- `UserServiceImplTest`: New method with UserCreationParams, event publication
- `RegistrationServiceIntegrationTest`: End-to-end registration flow, UserCreatedEvent has email
- `PasswordSetupFlowE2ETest`: Verify email received with correct greeting

### Verification Tests

- Confirm UserCreatedEventHandler processes PENDING_ACTIVATION events
- Confirm password setup email contains username (registration number)
- Confirm MemberCreatedEventHandler no longer sends password setup
- Confirm transaction rolls back if User or Member creation fails

## Implementation Notes

- **Ordering matters:** Create User first (to get ID), then Member with same ID
- **Event publication:** Spring Modulith publishes after transaction commit
- **Idempotency:** UserCreatedEventHandler checks PENDING_ACTIVATION status (skip if already activated)
- **Error handling:** Exception in UserCreatedEventHandler triggers retry (Spring Modulith)
- **Logging:** Don't log full events (PII), use `toString()` which excludes sensitive data
- **Testing:** Use test doubles for PasswordSetupService in unit tests
