# Implementation Guide: Sections 3 and 4

## Executive Summary

This guide provides step-by-step instructions for implementing Sections 3 (Move Password Management Classes) and Section
4 (Implement Event-Driven Communication) from the tasks.md file.

**IMPORTANT**: This is a COORDINATION document. The actual code changes will be made by the coordinator or human team
members. This guide provides the exact changes needed.

## Section 4: Implement Event-Driven Communication (DO THIS FIRST)

**Why first?** Because User must support event publishing before we can move PasswordSetupService to the users module.
Otherwise, the password setup flow will break.

### Step 4.1: Create UserCreatedEvent

**File**: `klabis-backend/src/main/java/com/klabis/users/domain/UserCreatedEvent.java`

```java
package com.klabis.users.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain event published when a new User is created.
 *
 * <p>This event triggers password setup flow for new users.
 * Published by User aggregate during user creation.
 *
 * <p><b>Event Publishing:</b> Published via Spring Modulith's transactional outbox pattern.
 * Ensures reliable, exactly-once event delivery with guaranteed consistency.
 */
public class UserCreatedEvent {

    private final UUID eventId;
    private final UUID userId;
    private final String registrationNumber;
    private final Instant occurredAt;

    /**
     * Creates a new UserCreatedEvent.
     *
     * @param userId the unique identifier of the created user
     * @param registrationNumber the user's registration number (username)
     */
    public UserCreatedEvent(UUID userId, String registrationNumber) {
        this(UUID.randomUUID(), userId, registrationNumber, Instant.now());
    }

    /**
     * Creates a new UserCreatedEvent with explicit event ID and timestamp.
     * Useful for testing and event reconstruction.
     *
     * @param eventId unique identifier for this event
     * @param userId the unique identifier of the created user
     * @param registrationNumber the user's registration number (username)
     * @param occurredAt the timestamp when this event occurred
     */
    public UserCreatedEvent(UUID eventId, UUID userId, String registrationNumber, Instant occurredAt) {
        this.eventId = Objects.requireNonNull(eventId, "Event ID is required");
        this.userId = Objects.requireNonNull(userId, "User ID is required");
        this.registrationNumber = Objects.requireNonNull(registrationNumber, "Registration number is required");
        this.occurredAt = Objects.requireNonNull(occurredAt, "Occurred at timestamp is required");
    }

    /**
     * Factory method to create event from User aggregate.
     *
     * @param user the user that was created
     * @return new UserCreatedEvent
     */
    public static UserCreatedEvent fromUser(User user) {
        return new UserCreatedEvent(user.getId(), user.getUsername());
    }

    // Getters

    public UUID getEventId() {
        return eventId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserCreatedEvent that = (UserCreatedEvent) o;
        return Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }

    @Override
    public String toString() {
        return "UserCreatedEvent{" +
               "eventId=" + eventId +
               ", userId=" + userId +
               ", registrationNumber='" + registrationNumber + '\'' +
               ", occurredAt=" + occurredAt +
               '}';
    }
}
```

### Step 4.2: Update User Domain Class

**File**: `klabis-backend/src/main/java/com/klabis/users/domain/User.java`

**Changes needed**:

1. Add imports:

```java
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.AfterDomainEventPublication;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
```

2. Add field:

```java
private final List<Object> domainEvents = new ArrayList<>();
```

3. Add methods (after the constructors, before validation methods):

```java
/**
 * Register a domain event to be published.
 *
 * @param event the domain event to register
 */
protected void registerEvent(Object event) {
    this.domainEvents.add(event);
}

/**
 * Get all domain events registered on this aggregate.
 * <p>
 * Annotated with @DomainEvents to enable Spring Modulith automatic event publishing.
 * Spring Data will automatically collect and publish these events via the outbox pattern.
 *
 * @return unmodifiable list of domain events
 */
@DomainEvents
public List<Object> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
}

/**
 * Clear all domain events (typically called after publishing).
 * <p>
 * Annotated with @AfterDomainEventPublication to ensure events are cleared
 * after they have been successfully published to the outbox.
 */
@AfterDomainEventPublication
public void clearDomainEvents() {
    this.domainEvents.clear();
}
```

4. Update `User.create()` method to publish event:

```java
public static User create(
        String username,
        String passwordHash,
        Set<Role> roles,
        Set<String> authorities) {

    Objects.requireNonNull(username, "User name is required");
    validateRequired(passwordHash, "Password hash");
    validateAuthorities(authorities);

    User user = new User(
            UUID.randomUUID(),
            username,
            passwordHash,
            roles,
            authorities,
            AccountStatus.ACTIVE,
            true, // accountNonExpired
            true, // accountNonLocked
            true, // credentialsNonExpired
            true  // enabled
    );

    // Register domain event
    user.registerEvent(new UserCreatedEvent(user.getId(), username));

    return user;
}
```

5. Update `User.create()` with AccountStatus:

```java
public static User create(
        String registrationNumber,
        String passwordHash,
        Set<Role> roles,
        Set<String> authorities,
        AccountStatus accountStatus) {

    Objects.requireNonNull(registrationNumber, "Registration number is required");
    validateRequired(passwordHash, "Password hash");
    Objects.requireNonNull(accountStatus, "Account status is required");
    validateAuthorities(authorities);

    User user = new User(
            UUID.randomUUID(),
            registrationNumber,
            passwordHash,
            roles,
            authorities,
            accountStatus,
            true, // accountNonExpired
            true, // accountNonLocked
            true, // credentialsNonExpired
            true  // enabled
    );

    // Register domain event
    user.registerEvent(new UserCreatedEvent(user.getId(), registrationNumber));

    return user;
}
```

6. Update `User.createPendingActivation()`:

```java
public static User createPendingActivation(
        String registrationNumber,
        String passwordHash,
        Set<Role> roles,
        Set<String> authorities) {

    Objects.requireNonNull(registrationNumber, "User name is required");
    validateRequired(passwordHash, "Password hash");
    validateAuthorities(authorities);

    User user = new User(
            UUID.randomUUID(),
            registrationNumber,
            passwordHash,
            roles,
            authorities,
            AccountStatus.PENDING_ACTIVATION,
            true, // accountNonExpired
            true, // accountNonLocked
            true, // credentialsNonExpired
            false // enabled = false until activated with password
    );

    // Register domain event
    user.registerEvent(new UserCreatedEvent(user.getId(), registrationNumber));

    return user;
}
```

### Step 4.3: Update UserEntity

**File**: `klabis-backend/src/main/java/com/klabis/users/infrastructure/persistence/UserEntity.java`

**Complete replacement** (remove Lombok, extend AbstractAggregateRoot):

```java
package com.klabis.users.infrastructure.persistence;

import com.klabis.users.domain.AccountStatus;
import com.klabis.users.domain.Role;
import jakarta.persistence.*;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * JPA entity for User.
 *
 * Maps User aggregate to database schema.
 * Separate from MemberEntity (Users and Members are separate aggregates).
 * Extends AbstractAggregateRoot to support domain event publishing.
 */
@Entity
@Table(name = "users")
public class UserEntity extends AbstractAggregateRoot<UserEntity> {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "registration_number", nullable = false, unique = true, length = 7)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "account_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles;

    @Column(name = "authorities", nullable = false, columnDefinition = "TEXT")
    private String authorities; // JSON string

    @Column(name = "account_non_expired", nullable = false)
    private boolean accountNonExpired;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked;

    @Column(name = "credentials_non_expired", nullable = false)
    private boolean credentialsNonExpired;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    // Default constructor for JPA
    protected UserEntity() {
    }

    // Full constructor
    public UserEntity(
            UUID id,
            String username,
            String passwordHash,
            Set<Role> roles,
            String authorities,
            AccountStatus accountStatus,
            boolean accountNonExpired,
            boolean accountNonLocked,
            boolean credentialsNonExpired,
            boolean enabled) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.authorities = authorities;
        this.accountStatus = accountStatus;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public String getAuthorities() {
        return authorities;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public boolean isEnabled() {
        return enabled;
    }

    // Domain event support methods (from AbstractAggregateRoot)

    protected UserEntity andEvent(Object event) {
        registerEvent(event);
        return this;
    }

    protected UserEntity andEvents(Collection<Object> events) {
        events.forEach(this::andEvent);
        return this;
    }
}
```

### Step 4.4: Update UserMapper

**File**: `klabis-backend/src/main/java/com/klabis/users/infrastructure/persistence/UserMapper.java`

**Change**: Update `toEntity()` method to pass events:

```java
/**
 * Map domain User to JPA UserEntity.
 */
public UserEntity toEntity(User user) {
    try {
        String authoritiesJson = objectMapper.writeValueAsString(user.getAuthorities());
        return new UserEntity(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRoles(),
                authoritiesJson,
                user.getAccountStatus(),
                user.isAccountNonExpired(),
                user.isAccountNonLocked(),
                user.isCredentialsNonExpired(),
                user.isEnabled()
        ).andEvents(user.getDomainEvents());  // ADD THIS LINE
    } catch (JsonProcessingException e) {
        log.error("Failed to serialize authorities to JSON", e);
        throw new RuntimeException("Failed to serialize authorities", e);
    }
}
```

### Step 4.5: Create PasswordSetupEventListener

**File**: `klabis-backend/src/main/java/com/klabis/users/application/PasswordSetupEventListener.java`

```java
package com.klabis.users.application;

import com.klabis.users.domain.UserCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event listener for UserCreatedEvent.
 *
 * Handles password setup token generation when a new user is created.
 * This replaces the MemberCreatedEventHandler's password setup responsibility.
 *
 * <p>This handler runs:
 * <ul>
 *   <li>After the transaction commits (ensures user exists in database)</li>
 *   <li>Asynchronously (doesn't block the command handler response)</li>
 *   <li>With failure tolerance (logs errors but doesn't affect the transaction)</li>
 * </ul>
 */
@Component
public class PasswordSetupEventListener {

    private static final Logger log = LoggerFactory.getLogger(PasswordSetupEventListener.class);

    private final PasswordSetupService passwordSetupService;

    public PasswordSetupEventListener(PasswordSetupService passwordSetupService) {
        this.passwordSetupService = passwordSetupService;
    }

    /**
     * Handles UserCreatedEvent by generating password setup token.
     *
     * <p>The {@link ApplicationModuleListener} annotation provides:
     * <ul>
     *   <li>Event externalization via Spring Modulith's outbox pattern</li>
     *   <li>Automatic retry on failures</li>
     *   <li>Separate transaction for event processing</li>
     * </ul>
     *
     * @param event the user created event
     */
    @ApplicationModuleListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserCreated(UserCreatedEvent event) {
        log.info("Processing UserCreatedEvent (eventId: {}) for user: {}",
                event.getEventId(), event.getUserId());

        try {
            // Generate password setup token
            passwordSetupService.generateTokenForUser(event.getUserId());

            log.info("Password setup token generated for user {}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to generate password setup token for user {}: {}",
                    event.getUserId(), e.getMessage(), e);
            // Event will be retried automatically by Spring Modulith
            throw e; // Re-throw to trigger retry
        }
    }
}
```

### Step 4.6: Update PasswordSetupService (Temporary)

**File**: `klabis-backend/src/main/java/com/klabis/members/application/PasswordSetupService.java`

**Add new method** (until we move the service to users module):

```java
/**
 * Generates a password setup token for a user ID.
 *
 * <p>Overloaded version that accepts UUID instead of User entity.
 * This is used by PasswordSetupEventListener which only receives the userId.
 *
 * @param userId the user ID requiring password setup
 * @throws IllegalArgumentException if user is null or not found
 */
@Transactional
public void generateTokenForUser(UUID userId) {
    Assert.notNull(userId, "User ID is required");

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

    generateToken(user);
}
```

**Note**: This method will be removed when PasswordSetupService is moved to users module and can directly use User.

### Step 4.7: Update MemberCreatedEventHandler

**File**: `klabis-backend/src/main/java/com/klabis/members/application/MemberCreatedEventHandler.java`

**Remove password setup logic** (now handled by PasswordSetupEventListener):

```java
package com.klabis.members.application;

import com.klabis.members.domain.MemberCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Event handler for MemberCreatedEvent.
 *
 * Handles side effects that should occur after a member is successfully created.
 * Password setup is now handled by PasswordSetupEventListener in users module.
 *
 * <p>This handler is configured to run asynchronously after the transaction commits.
 */
@Component
public class MemberCreatedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(MemberCreatedEventHandler.class);

    /**
     * Handles MemberCreatedEvent.
     *
     * <p>Password setup is now triggered by UserCreatedEvent in the users module.
     * This handler can be used for other member-related event handling if needed.
     *
     * @param event the member created event
     */
    @ApplicationModuleListener
    public void onMemberCreated(MemberCreatedEvent event) {
        log.info("Processing MemberCreatedEvent (eventId: {}) for registration number: {}",
                event.getEventId(), event.getRegistrationNumber().getValue());

        // Password setup is now handled by PasswordSetupEventListener (users module)
        // Add other member-related event handling here if needed

        log.info("MemberCreatedEvent processed successfully for event {}",
                event.getEventId());
    }
}
```

---

## Section 3: Move Password Management Classes

### Step 3.1: Move PasswordSetupService

**Action**: Move file and update package declaration

**From**: `klabis-backend/src/main/java/com/klabis/members/application/PasswordSetupService.java`
**To**: `klabis-backend/src/main/java/com/klabis/users/application/PasswordSetupService.java`

**Changes**:

1. Update package declaration:

```java
package com.klabis.users.application;
```

2. Update imports:

```java
// Remove these imports:
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.PasswordSetupTokenRepository;
import com.klabis.members.domain.RegistrationNumber;

// Add these imports (if not already present):
import com.klabis.users.domain.PasswordSetupTokenRepository;
import com.klabis.users.domain.User;
import com.klabis.users.domain.UserRepository;
```

3. **CRITICAL**: Refactor `sendPasswordSetupEmail(Member member, String plainToken)` method:

**Option A**: If User has email field (recommended):

```java
/**
 * Sends a password setup email to the user.
 *
 * @param user the user to send the email to
 * @param plainToken the plain text token (not hashed)
 */
public void sendPasswordSetupEmail(User user, String plainToken) {
    Assert.notNull(user, "User is required");
    Assert.hasText(plainToken, "Token is required");

    String email = user.getEmail(); // Assumes User has getEmail() method
    String firstName = user.getFirstName(); // Assumes User has firstName

    sendPasswordSetupEmail(firstName, email, plainToken);
}
```

**Option B**: Keep Member dependency temporarily (interim solution):

```java
/**
 * Sends a password setup email to the member.
 *
 * <p>TEMPORARY: This method keeps Member dependency until User has email field.
 * TODO: Remove Member dependency after User.email is added.
 *
 * @param member the member to send the email to
 * @param plainToken the plain text token (not hashed)
 */
public void sendPasswordSetupEmail(Member member, String plainToken) {
    Assert.notNull(member, "Member is required");
    Assert.hasText(plainToken, "Token is required");

    String primaryEmail = member.getPrimaryEmail();
    sendPasswordSetupEmail(
            member.getFirstName(),
            primaryEmail,
            plainToken
    );
}
```

4. **CRITICAL**: Refactor `requestNewToken()` method:

**Option A**: If User has email field:

```java
@Auditable(
        event = AuditEventType.PASSWORD_SETUP_TOKEN_REQUESTED,
        description = "New password setup token requested for registration number {#registrationNumber}"
)
@Transactional
public void requestNewToken(String registrationNumber) {
    // 0. Check rate limit
    rateLimiter.checkLimit(registrationNumber);

    // 1. Find user by registration number
    User user = userRepository.findByUsername(registrationNumber)
            .orElseThrow(() -> new TokenValidationException("User not found"));

    // 2. Check account status
    if (user.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
        throw new TokenValidationException(
                "Account is not in pending activation status. Token reissuance is only available for accounts awaiting activation."
        );
    }

    // 3. Generate new token
    GeneratedTokenResult result = generateToken(user);

    // 4. Send password setup email
    sendPasswordSetupEmail(user, result.plainToken());

    log.info("Password setup token reissued for user {}", user.getId());
}
```

**Option B**: Keep Member lookup temporarily:

```java
@Auditable(
        event = AuditEventType.PASSWORD_SETUP_TOKEN_REQUESTED,
        description = "New password setup token requested for registration number {#registrationNumber}"
)
@Transactional
public void requestNewToken(String registrationNumber) {
    // 0. Check rate limit
    rateLimiter.checkLimit(registrationNumber);

    // 1. Parse and validate registration number
    RegistrationNumber regNum = new RegistrationNumber(registrationNumber);

    // 2. Find user by registration number
    User user = userRepository.findByUsername(registrationNumber)
            .orElseThrow(() -> new TokenValidationException("User not found"));

    // 3. Check account status
    if (user.getAccountStatus() != AccountStatus.PENDING_ACTIVATION) {
        throw new TokenValidationException(
                "Account is not in pending activation status. Token reissuance is only available for accounts awaiting activation."
        );
    }

    // 4. Find member (TEMPORARY: needed for email sending)
    Member member = memberRepository.findByRegistrationId(regNum)
            .orElseThrow(() -> new TokenValidationException("Member not found"));

    // 5. Generate new token
    GeneratedTokenResult result = generateToken(user);

    // 6. Send password setup email
    sendPasswordSetupEmail(member, result.plainToken());

    log.info("Password setup token reissued for user {}", user.getId());
}
```

5. Remove `generateTokenForUser()` method (added in Section 4.6):

```java
// REMOVE THIS TEMPORARY METHOD - no longer needed after moving to users module
```

### Step 3.2: Move PasswordSetupController

**From**: `klabis-backend/src/main/java/com/klabis/members/presentation/PasswordSetupController.java`
**To**: `klabis-backend/src/main/java/com/klabis/users/presentation/PasswordSetupController.java`

**Changes**:

1. Update package declaration:

```java
package com.klabis.users.presentation;
```

2. Update imports:

```java
// Remove:
import com.klabis.members.application.PasswordSetupService;

// Add:
import com.klabis.users.application.PasswordSetupService;
```

3. **NO CHANGES** to endpoint mappings or request/response DTOs

### Step 3.3: Move PasswordComplexityValidator

**From**: `klabis-backend/src/main/java/com/klabis/members/application/PasswordComplexityValidator.java`
**To**: `klabis-backend/src/main/java/com/klabis/users/domain/PasswordComplexityValidator.java`

**Changes**:

1. Update package declaration:

```java
package com.klabis.users.domain;
```

2. Update imports:

```java
// Remove:
import com.klabis.members.domain.Member;
import com.klabis.members.application.PasswordSetupService;

// Add:
import com.klabis.users.application.PasswordSetupService;
```

3. **Keep both validate() methods** (basic and with Member context)
4. TODO comment on Member-dependent validate() method:

```java
/**
 * Validates a password against complexity requirements.
 *
 * <p>TEMPORARY: This method has Member dependency for checking personal information.
 * TODO: Remove Member dependency after User has personal information fields.
 *
 * @param password the password to validate
 * @param member the member (for checking personal information)
 * @throws PasswordSetupService.PasswordValidationException if validation fails
 */
public void validate(String password, Member member) {
    // ... existing code ...
}
```

### Step 3.4-3.7: Move Infrastructure Classes

**Files to move**:

1. PasswordSetupTokenEntity.java
2. PasswordSetupTokenRepositoryImpl.java
3. PasswordSetupTokenJpaRepository.java
4. PasswordSetupTokenMapper.java

**Changes for each file**:

1. Update package declaration:

```java
package com.klabis.users.infrastructure.persistence;
```

2. Update imports in each file:

```java
// Remove:
import com.klabis.members.domain.PasswordSetupTokenRepository;
import com.klabis.members.infrastructure.persistence.*;

// Add:
import com.klabis.users.domain.PasswordSetupTokenRepository;
import com.klabis.users.domain.PasswordSetupToken;
import com.klabis.users.domain.TokenHash;
```

3. **NO OTHER CHANGES** needed

---

## Testing After Implementation

### Unit Tests

Run all unit tests in users module:

```bash
cd klabis-backend
mvn test -Dtest="com.klabis.users.**"
```

### Integration Tests

Run all integration tests:

```bash
cd klabis-backend
mvn verify
```

### Event-Driven Communication Test

Create new integration test:

**File**: `klabis-backend/src/test/java/com/klabis/users/UserCreationEventIntegrationTest.java`

```java
package com.klabis.users;

import com.klabis.users.domain.User;
import com.klabis.users.domain.UserRepository;
import com.klabis.users.domain.UserCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest
class UserCreationEventIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPublishUserCreatedEventWhenUserIsCreated(Scenario scenario) {
        // Given
        String registrationNumber = "ZBM0001";
        String passwordHash = "$2a$10$dummyHash";

        // When
        User user = User.createPendingActivation(
                registrationNumber,
                passwordHash,
                Set.of(),
                Set.of("MEMBERS:READ")
        );
        userRepository.save(user);

        // Then
        scenario.stimulate(() -> user)
                .andWaitForEventOfType(UserCreatedEvent.class)
                .toMatch(event -> {
                    assertThat(event.getUserId()).isEqualTo(user.getId());
                    assertThat(event.getRegistrationNumber()).isEqualTo(registrationNumber);
                });
    }
}
```

### Manual Testing

1. Start application:

```bash
cd klabis-backend
mvn spring-boot:run
```

2. Register new member via API:

```bash
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{
    "registrationNumber": "ZBM9001",
    "firstName": "Jan",
    "lastName": "Novak",
    "dateOfBirth": "1990-01-01",
    "nationality": "CZE",
    "gender": "MALE",
    "emails": ["jan.novak@example.com"],
    "phones": ["+420123456789"]
  }'
```

3. Verify password setup email is sent
4. Verify `UserCreatedEvent` is in event_publication table
5. Verify password setup token is created in password_setup_tokens table

---

## Validation Checklist

### Section 4: Event-Driven Communication

- [ ] UserCreatedEvent class created
- [ ] User domain class has domainEvents list
- [ ] User domain class has @DomainEvents annotation
- [ ] User domain class has @AfterDomainEventPublication annotation
- [ ] User.create() methods call registerEvent()
- [ ] UserEntity extends AbstractAggregateRoot<UserEntity>
- [ ] UserEntity has andEvent() and andEvents() methods
- [ ] UserMapper.toEntity() calls .andEvents()
- [ ] PasswordSetupEventListener created
- [ ] PasswordSetupEventListener has @ApplicationModuleListener
- [ ] PasswordSetupEventListener has @Async
- [ ] MemberCreatedEventHandler updated (password logic removed)
- [ ] Unit tests pass for User domain
- [ ] Integration tests pass for event publishing
- [ ] Manual test: User creation publishes event

### Section 3: Move Password Classes

- [ ] PasswordSetupService moved to users.application
- [ ] PasswordSetupService package declaration updated
- [ ] PasswordSetupService imports updated
- [ ] PasswordSetupService Member dependency handled
- [ ] PasswordSetupController moved to users.presentation
- [ ] PasswordSetupController package declaration updated
- [ ] PasswordSetupController imports updated
- [ ] PasswordComplexityValidator moved to users.domain
- [ ] PasswordComplexityValidator package declaration updated
- [ ] PasswordComplexityValidator imports updated
- [ ] PasswordSetupTokenEntity moved to users.infrastructure.persistence
- [ ] PasswordSetupTokenRepositoryImpl moved to users.infrastructure.persistence
- [ ] PasswordSetupTokenJpaRepository moved to users.infrastructure.persistence
- [ ] PasswordSetupTokenMapper moved to users.infrastructure.persistence
- [ ] All infrastructure classes have updated package declarations
- [ ] All infrastructure classes have updated imports
- [ ] Test files moved to users module
- [ ] Test files have updated package declarations
- [ ] Test files have updated imports
- [ ] All tests pass

---

## Rollback Plan

If issues arise during implementation:

1. **Revert all changes**:

```bash
git reset --hard HEAD
git clean -fd
```

2. **Database**: No changes needed (no schema changes in this refactor)

3. **Frontend**: No changes needed (API endpoints unchanged)

---

## Next Steps After Implementation

1. **Add User.email field** (if Option A chosen):
    - Add email column to users table
    - Add getEmail() method to User domain
    - Update PasswordSetupService to use User.email
    - Remove Member dependency from PasswordSetupService

2. **Update documentation**:
    - Update README.md
    - Update ARCHITECTURE.md
    - Update API.md if needed

3. **Clean up**:
    - Remove old password classes from members module
    - Remove unused imports
    - Run code formatter
    - Run static analysis

---
**End of Implementation Guide**
