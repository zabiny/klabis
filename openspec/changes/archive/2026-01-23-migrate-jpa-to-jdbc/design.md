# Technical Design: Spring Data JDBC Migration

## Context

The Klabis backend currently uses Spring Data JPA with a deliberately denormalized design that avoids JPA relationships,
lazy loading, and cascading. This design already aligns with Spring Data JDBC's philosophy, making migration
straightforward while achieving better performance and simpler persistence model.

### Current Architecture Characteristics

- Clean layering: Domain → Application → Infrastructure
- Aggregate-oriented persistence (3 aggregates: User, Member, PasswordSetupToken)
- No JPA relationships (@OneToMany, @ManyToOne, etc.)
- Event-driven architecture with Spring Modulith
- Optimistic locking with @Version
- Auditing support (created/modified timestamps and users)
- UUID primary keys throughout

## Goals / Non-Goals

### Goals

- Replace JPA/Hibernate with Spring Data JDBC while maintaining domain model integrity
- Simplify persistence layer by eliminating entity-domain mapping
- Improve performance by removing Hibernate overhead
- Preserve existing features: auditing, optimistic locking, event publishing
- Maintain backward compatibility with database schema (minimal migrations)
- Keep domain layer pure (no persistence framework dependencies)

### Non-Goals

- Changing domain model or business logic
- Modifying REST API contracts
- Adding new features beyond migration
- Changing database schema structure (beyond JDBC conventions)
- Migrating existing data (schema is compatible)

## Decisions

### 1. Aggregate Root Design Pattern

**Decision:** Use direct domain objects as JDBC entities instead of separate entity classes

**Rationale:**

- Spring Data JDBC supports direct mapping of domain aggregates
- Eliminates mapping layer between domain and persistence
- Reduces code complexity and maintenance
- Aligns with DDD principle of persistence transparency

**Implementation:**

```java
// User aggregate (domain layer)
@Table("users")
public class User implements Persistable<UUID> {

    @Id
    private final UUID id;
    private final String registrationNumber;
    private final String passwordHash;
    private final AccountStatus accountStatus;

    // Collection handled by Spring Data JDBC
    @MappedCollection(idColumn = "user_id")
    private Set<UserRole> roles;

    // Value object - requires custom converter
    private Set<String> authorities;

    // Audit fields
    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant lastModifiedAt;

    // Optimistic locking
    @Version
    private Long version;

    // Transient field for new entity detection
    @Transient
    private boolean isNew = false;

    @Override
    public boolean isNew() {
        return isNew || id == null;
    }

    // Constructor for creation (marks as new)
    public User(String registrationNumber, String passwordHash) {
        this.id = UUID.randomUUID();
        this.registrationNumber = registrationNumber;
        this.passwordHash = passwordHash;
        this.isNew = true;
    }

    // Constructor for loading from database (not new)
    User(UUID id, String registrationNumber, String passwordHash,
         AccountStatus accountStatus, Long version) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.passwordHash = passwordHash;
        this.accountStatus = accountStatus;
        this.version = version;
        this.isNew = false;
    }
}
```

**Alternatives Considered:**

- **Separate entity classes (current JPA approach):** Rejected due to unnecessary mapping complexity
- **Anemic domain model:** Rejected as it violates DDD principles

---

### 2. Value Object Persistence

**Decision:** Use custom `@WritingConverter` and `@ReadingConverter` for complex value objects

**Rationale:**

- Spring Data JDBC requires explicit converters for non-simple types
- Preserves immutability of value objects
- Enables type-safe domain modeling
- Supports JSON serialization for complex structures

**Implementation:**

**a) Embedded Value Objects (Address, PhoneNumber, EmailAddress):**

```java
// Domain value object
public record Address(String street, String city, String postalCode, String country) {
    // Validation in constructor
}

// Writing converter (Value Object → String)
@WritingConverter
public class AddressToStringConverter implements Converter<Address, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convert(Address source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new ConverterException("Failed to convert Address", e);
        }
    }
}

// Reading converter (String → Value Object)
@ReadingConverter
public class StringToAddressConverter implements Converter<String, Address> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Address convert(String source) {
        try {
            return objectMapper.readValue(source, Address.class);
        } catch (JsonProcessingException e) {
            throw new ConverterException("Failed to parse Address", e);
        }
    }
}
```

**b) Collection Value Objects (Set<String> authorities):**

```java
// Writing converter (Set<String> → String JSON array)
@WritingConverter
public class StringSetToJsonConverter implements Converter<Set<String>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convert(Set<String> source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new ConverterException("Failed to convert Set<String>", e);
        }
    }
}

// Reading converter (String → Set<String>)
@ReadingConverter
public class JsonToStringSetConverter implements Converter<String, Set<String>> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<String> convert(String source) {
        try {
            return objectMapper.readValue(source,
                new TypeReference<Set<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptySet();
        }
    }
}
```

**c) Enum Persistence (AccountStatus, Gender, etc.):**

```java
// Enums are natively supported - stored as VARCHAR with enum name
public enum AccountStatus {
    ACTIVE, SUSPENDED, LOCKED, DELETED
}

// No converter needed - Spring Data JDBC handles automatically
```

**Alternatives Considered:**

- **Denormalize to individual columns:** Rejected due to schema explosion and loss of cohesion
- **Store as JSONB:** Rejected due to H2 compatibility requirements

---

### 3. Collection Handling (User Roles)

**Decision:** Use `@MappedCollection` with separate table for owned collections

**Rationale:**

- Spring Data JDBC treats collections as part of aggregate
- Automatic cascade operations (save/delete)
- Clean separation of concerns
- Preserves Set semantics without duplicates

**Implementation:**

```java
// User aggregate
@Table("users")
public class User {
    @Id
    private UUID id;

    // Collection mapped to separate table
    @MappedCollection(idColumn = "user_id", keyColumn = "role_key")
    private Set<UserRole> roles = new HashSet<>();

    // Business method
    public void assignRole(Role role) {
        this.roles.add(new UserRole(role));
    }
}

// Owned entity within aggregate
@Table("user_roles")
public class UserRole {

    private Role role;  // Enum stored as STRING

    UserRole(Role role) {
        this.role = role;
    }

    // No ID needed for Set - Spring uses role as natural key
    // No back-reference to User needed
}
```

**Database Schema:**

```sql
CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Behavior:**

- Saving `User` cascades to `user_roles` table
- Deleting `User` removes all associated roles
- Modifying roles requires saving the User aggregate

**Alternatives Considered:**

- **JSON array in users table:** Rejected due to query complexity and lack of referential integrity
- **@ElementCollection (JPA style):** Not available in Spring Data JDBC - correct approach is @MappedCollection

---

### 4. Event Publishing with Spring Modulith

**Decision:** Use Spring Data JDBC's `@DomainEvents` and `AbstractAggregateRoot` for event publishing

**Rationale:**

- Spring Data JDBC natively integrates with Spring Modulith
- Events are published automatically on save operations
- Transactional consistency (events stored in event_publication table)
- Cleaner than custom event handling

**Implementation:**

**a) Aggregate with Events:**

```java
@Table("users")
public class User extends AbstractAggregateRoot<User> {

    @Id
    private UUID id;

    private String registrationNumber;

    // Business method publishes domain event
    public User activate() {
        if (this.accountStatus != AccountStatus.ACTIVE) {
            this.accountStatus = AccountStatus.ACTIVE;

            // Register event for publishing
            registerEvent(new UserActivatedEvent(this.id, this.registrationNumber));
        }
        return this;
    }
}

// Domain event (immutable record)
public record UserActivatedEvent(UUID userId, String registrationNumber)
    implements DomainEvent {}
```

**b) Spring Modulith Configuration:**

```java
@Configuration
@EnableJdbcAuditing
@EnableApplicationModuleInitializer
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    @Bean
    ApplicationModuleInitializer initializer() {
        return new ApplicationModuleInitializer();
    }
}
```

**c) Event Listener (in different module):**

```java
@Service
public class UserEventHandler {

    private final EmailService emailService;

    @ApplicationModuleListener
    void on(UserActivatedEvent event) {
        // Handle event asynchronously
        emailService.sendActivationConfirmation(event.registrationNumber());
    }
}
```

**Event Publication Flow:**

1. User calls `user.activate()` → event registered
2. Repository saves `User` → Spring Data JDBC detects events
3. Events stored in `event_publication` table (same transaction)
4. Spring Modulith publishes events to listeners
5. Listeners process events (separate transaction)
6. Event marked as completed in `event_publication`

**Database Schema (Spring Modulith):**

```sql
CREATE TABLE event_publication (
    id UUID PRIMARY KEY,
    completion_date TIMESTAMP,
    event_type VARCHAR(512),
    listener_id VARCHAR(512),
    publication_date TIMESTAMP,
    serialized_event TEXT
);
```

**Alternatives Considered:**

- **Manual event publishing:** Rejected due to transaction complexity
- **Spring Events (@EventListener):** Rejected due to lack of persistence and guaranteed delivery

---

### 5. Auditing Configuration

**Decision:** Use Spring Data JDBC's `@EnableJdbcAuditing` with custom `AuditorAware` bean

**Rationale:**

- Native support in Spring Data JDBC
- Automatic population of audit fields
- Integrates with Spring Security
- No entity listener configuration needed

**Implementation:**

**a) Configuration:**

```java
@Configuration
@EnableJdbcAuditing(auditorAwareRef = "auditorProvider")
public class JdbcAuditingConfiguration {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName)
            .or(() -> Optional.of("system"));
    }
}
```

**b) Auditable Aggregate:**

```java
@Table("members")
public class Member {

    @Id
    private UUID id;

    // Audit fields - automatically populated
    @CreatedDate
    private Instant createdAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedDate
    private Instant lastModifiedAt;

    @LastModifiedBy
    private String lastModifiedBy;
}
```

**Behavior:**

- **On insert:** `createdAt`, `createdBy` set automatically
- **On update:** `lastModifiedAt`, `lastModifiedBy` updated
- **System operations:** Falls back to "system" when no authenticated user

**Alternatives Considered:**

- **Manual auditing in callbacks:** Rejected due to boilerplate
- **Database triggers:** Rejected due to portability concerns

---

### 6. Optimistic Locking

**Decision:** Use `@Version` annotation with `Long` type for version field

**Rationale:**

- Spring Data JDBC native support
- Prevents lost updates in concurrent scenarios
- Automatic version increment on save
- Throws `OptimisticLockingFailureException` on conflict

**Implementation:**

```java
@Table("users")
public class User {

    @Id
    private UUID id;

    private String registrationNumber;

    @Version
    private Long version;

    // Business logic methods...
}
```

**Behavior:**

```java
// Transaction 1: Load user
User user1 = userRepository.findById(userId).orElseThrow();
// version = 5

// Transaction 2: Load same user
User user2 = userRepository.findById(userId).orElseThrow();
// version = 5

// Transaction 1: Update and save
user1.updateEmail("[email protected]");
userRepository.save(user1);
// version incremented to 6

// Transaction 2: Try to save
user2.updateEmail("[email protected]");
userRepository.save(user2);
// Throws OptimisticLockingFailureException - version mismatch (5 != 6)
```

**Database Schema:**

```sql
ALTER TABLE users ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE members ADD COLUMN version BIGINT DEFAULT 0;
```

**Handling Conflicts:**

```java
@Transactional
public void updateUserEmail(UUID userId, String newEmail) {
    try {
        User user = userRepository.findById(userId).orElseThrow();
        user.updateEmail(newEmail);
        userRepository.save(user);
    } catch (OptimisticLockingFailureException e) {
        // Retry logic or user notification
        throw new ConcurrentModificationException("User was modified by another process");
    }
}
```

**Alternatives Considered:**

- **Pessimistic locking:** Rejected due to scalability concerns
- **No locking:** Rejected due to data integrity requirements

---

### 7. Custom Queries and Repository Patterns

**Decision:** Use derived query methods for simple cases, `@Query` for complex SQL, and `JdbcAggregateTemplate` for
dynamic queries

**Rationale:**

- Derived queries reduce boilerplate for common patterns
- `@Query` provides SQL control when needed
- `JdbcAggregateTemplate` enables programmatic query building
- Type-safe and testable

**Implementation:**

**a) Repository Interface (Domain Layer):**

```java
// Pure domain interface - no Spring dependencies
public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    long countActiveUsersWithAuthority(String authority);
    void delete(User user);
}
```

**b) Spring Data JDBC Repository (Infrastructure Layer):**

```java
@Repository
interface UserJdbcRepository extends CrudRepository<User, UUID> {

    // Derived query - automatically implemented
    Optional<User> findByRegistrationNumber(String registrationNumber);

    // Custom query - count active users with specific authority
    @Query("""
        SELECT COUNT(*)
        FROM users
        WHERE account_status = 'ACTIVE'
        AND authorities::jsonb @> :authority::jsonb
        """)
    long countActiveUsersWithAuthority(@Param("authority") String authority);

    // Query with pagination
    @Query("SELECT * FROM users WHERE account_status = :status ORDER BY created_at DESC")
    List<User> findByStatus(@Param("status") String status, Pageable pageable);

    // Modifying query
    @Modifying
    @Query("UPDATE users SET account_status = 'LOCKED' WHERE failed_login_attempts >= :threshold")
    int lockUsersWithFailedLogins(@Param("threshold") int threshold);
}
```

**c) Repository Implementation (Adapter Pattern):**

```java
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJdbcRepository jdbcRepository;

    public UserRepositoryImpl(UserJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public User save(User user) {
        return jdbcRepository.save(user);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jdbcRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return jdbcRepository.findByRegistrationNumber(username);
    }

    @Override
    public long countActiveUsersWithAuthority(String authority) {
        // Convert authority to JSON format for JSONB query
        String jsonAuthority = String.format("[\"%s\"]", authority);
        return jdbcRepository.countActiveUsersWithAuthority(jsonAuthority);
    }

    @Override
    public void delete(User user) {
        jdbcRepository.delete(user);
    }
}
```

**d) Complex Queries with JdbcAggregateTemplate:**

```java
@Repository
public class MemberQueryRepository {

    private final JdbcAggregateTemplate jdbcTemplate;

    public MemberQueryRepository(JdbcAggregateTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Member> findActiveMembers(int ageMin, int ageMax) {
        LocalDate birthDateMax = LocalDate.now().minusYears(ageMin);
        LocalDate birthDateMin = LocalDate.now().minusYears(ageMax + 1);

        return jdbcTemplate.find(
            query(
                where("isActive").is(true)
                    .and("dateOfBirth").between(birthDateMin, birthDateMax)
            ).sort(Sort.by("lastName", "firstName")),
            Member.class
        );
    }
}
```

**Alternatives Considered:**

- **JPQL:** Not available in Spring Data JDBC - use native SQL
- **Criteria API:** Use `JdbcAggregateTemplate` instead
- **Named queries:** Less flexible than `@Query` annotations

---

## Risks / Trade-offs

### Risk: Complex Value Object Mapping

- **Impact:** Custom converters add complexity
- **Mitigation:** Create comprehensive converter tests; document patterns clearly
- **Fallback:** Denormalize to individual columns if converters prove problematic

### Risk: Breaking Change for Existing Code

- **Impact:** All persistence code requires modification
- **Mitigation:**
    - Feature toggle to run both implementations in parallel
    - Comprehensive test suite to verify behavior
    - Incremental migration per aggregate
- **Rollback:** Keep JPA implementation until JDBC is fully validated

### Risk: Loss of Lazy Loading

- **Impact:** Potential performance degradation if aggregates grow large
- **Mitigation:**
    - Monitor aggregate sizes
    - Use projection queries for read-only views
    - Refactor large aggregates if needed
- **Note:** Current design already avoids lazy loading, so minimal impact expected

### Risk: JSONB Query Compatibility (H2 vs PostgreSQL)

- **Impact:** H2 doesn't support JSONB operators; queries may differ
- **Mitigation:**
    - Use JSON functions available in both databases
    - Test with both H2 (dev) and PostgreSQL (prod)
    - Consider migrating to PostgreSQL for development if queries diverge significantly

### Trade-off: Simpler Code vs Learning Curve

- **Pro:** Spring Data JDBC is simpler than JPA/Hibernate
- **Con:** Team must learn new patterns (converters, callbacks, aggregate loading)
- **Mitigation:** Training sessions, pair programming, comprehensive documentation

---

## Migration Plan

### Phase 1: Preparation (No Code Changes)

1. Add Spring Data JDBC dependencies to Maven POM (alongside JPA)
2. Create JDBC configuration classes (disabled by default)
3. Set up converter infrastructure
4. Create comprehensive test suite for current JPA behavior

### Phase 2: Parallel Implementation (Feature Toggle)

1. Implement JDBC repositories alongside JPA repositories
2. Add feature toggle to switch between implementations
3. Create converters for all value objects
4. Implement auditing and event publishing for JDBC
5. Run integration tests against both implementations

### Phase 3: Validation

1. Enable JDBC implementation in test environment
2. Run full test suite
3. Performance testing and benchmarking
4. Compare behavior between JPA and JDBC implementations
5. Fix any discrepancies

### Phase 4: Production Rollout

1. Deploy with feature toggle (default: JPA)
2. Enable JDBC for canary deployments (10% traffic)
3. Monitor metrics and error rates
4. Gradual rollout to 100% traffic
5. Remove feature toggle after stable period

### Phase 5: Cleanup

1. Remove JPA dependencies from Maven POM
2. Delete JPA entity classes and repositories
3. Remove JPA configuration
4. Update documentation

### Rollback Strategy

- Feature toggle allows instant revert to JPA
- No data migration required (schema compatible)
- Rollback window: 2 weeks after full production rollout

---

## Open Questions

### Q1: Should we migrate all aggregates simultaneously or one at a time?

- **Option A:** Migrate all three aggregates together (User, Member, PasswordSetupToken)
    - Pro: Consistent implementation, simpler feature toggle
    - Con: Higher risk, larger initial changeset

- **Option B:** Migrate one aggregate at a time
    - Pro: Lower risk, incremental validation
    - Con: Mixed persistence implementations, more complex testing

**Recommendation:** Option A - aggregates are independent, migration is straightforward

### Q2: How should we handle database schema differences between H2 and PostgreSQL?

- **Current approach:** H2 in MODE=PostgreSQL for compatibility
- **Alternative:** Use PostgreSQL for development (via Docker)
- **Recommendation:** Continue with H2 for fast test cycles; comprehensive PostgreSQL integration tests

### Q3: Should we use Liquibase to generate schema from JDBC mapping context?

- **Pro:** Schema always matches code
- **Con:** Loss of manual control, potential for unintended migrations
- **Recommendation:** Continue with manual Liquibase migrations for production safety; use generated schema as reference
  only

---

## Success Criteria

### Functional Requirements

- ✅ All existing tests pass with JDBC implementation
- ✅ Domain events published correctly via Spring Modulith
- ✅ Optimistic locking prevents concurrent modification issues
- ✅ Audit fields populated automatically
- ✅ Value objects persisted and retrieved correctly

### Performance Requirements

- ✅ Repository operations ≤ JPA performance (no regression)
- ✅ Memory usage reduced by ≥ 10% (no Hibernate session overhead)
- ✅ Application startup time improved by ≥ 20% (simpler framework)

### Code Quality Requirements

- ✅ Test coverage maintained at ≥ 80%
- ✅ No persistence framework dependencies in domain layer
- ✅ All converters have unit tests
- ✅ Documentation updated (README, architecture diagrams)

### Operational Requirements

- ✅ Zero downtime deployment
- ✅ Rollback capability maintained for 2 weeks
- ✅ Monitoring dashboards show expected metrics
- ✅ No production incidents related to migration
