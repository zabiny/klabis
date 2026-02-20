# Data Layer Patterns Reference

This document describes patterns for implementing the data/persistence layer in the Klabis application, based on Spring Data JDBC with Domain-Driven Design and Hexagonal Architecture.

## Overview

The data layer consists of three main components:

1. **Domain Repository Interface** - Port in domain layer
2. **Infrastructure Adapter** - Implementation bridging domain and JDBC
3. **JDBC Repository** - Spring Data JDBC repository for persistence
4. **Memento** - Persistence object pattern for domain entities

## Pattern 1: Memento for Domain Entity Persistence

### Purpose
Separate domain entity (pure business logic) from persistence concerns (Spring Data JDBC annotations).

### Structure

```java
// Domain entity (pure, no Spring annotations)
@AggregateRoot
public class Member {
    @Identity
    private final UserId id;
    private RegistrationNumber registrationNumber;
    private PersonalInformation personalInformation;
    // ... other domain fields

    // Domain events (published via Memento delegation)
    private final List<Object> domainEvents = new ArrayList<>();

    @DomainEvents
    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}

// Memento (persistence adapter with Spring annotations)
@Table("members")
class MemberMemento implements Persistable<UUID> {
    @Id
    @Column("id")
    private UUID id;

    @Column("registration_number")
    private String registrationNumber;

    // Audit fields
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @Version
    @Column("version")
    private Long version;

    // Transient reference for domain event delegation
    @Transient
    private Member member;

    @Transient
    private boolean isNew = true;

    // Conversion: Domain -> Memento (for save)
    public static MemberMemento from(Member member) {
        MemberMemento memento = new MemberMemento();
        memento.id = member.getId().uuid();
        memento.registrationNumber = member.getRegistrationNumber().getValue();
        // ... copy all fields
        memento.member = member; // Store for event delegation
        memento.isNew = (member.getAuditMetadata() == null);
        return memento;
    }

    // Conversion: Memento -> Domain (for load)
    public Member toMember() {
        Member member = Member.reconstruct(
            new UserId(this.id),
            new RegistrationNumber(this.registrationNumber),
            // ... all domain fields
            getAuditMetadata()
        );
        this.member = member; // Store for event delegation
        return member;
    }

    // Domain event delegation to Spring Modulith
    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.member != null ? this.member.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.member != null) {
            this.member.clearDomainEvents();
        }
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }
}
```

### Key Points

- **Memento stores flattened primitives** matching database schema
- **Domain entity uses value objects** (PersonalInformation, Address, etc.)
- **`from()` method** converts domain to memento (save operation)
- **`toMember()` method** converts memento to domain (load operation)
- **`isNew` flag** based on audit metadata presence (null = new entity)
- **Domain events** delegated from Member to Memento for Spring Modulith

## Pattern 2: Three/Four-Layer Repository Architecture

The repository architecture has **3 or 4 layers** depending on module access requirements:

**4-Layer Architecture (for aggregates accessed by other modules):**
1. **Public API Interface** (`Members`) - Module boundary for other modules (queries only). Optional - only when aggregate is accessed from other modules
2. **Domain Repository Interface** (`MemberRepository`) - Internal port with write operations
3. **JDBC Repository** (`MemberJdbcRepository`) - Spring Data JDBC persistence
4. **Repository Adapter** (`MemberRepositoryAdapter`) - Implementation bridging layers 2 and 3

**When to use optional Public API Interface?**
- ✅ Aggregate is queried by other modules (e.g., Member, User, Event)

**Why separate layers?**
- **Layer 1** (Public API - optional) enforces module boundaries - other modules can only query, never write
- **Layer 2** (Domain Repository) adds write operations for internal use within module
- **Layer 3** (JDBC Repository) is Spring Data JDBC working with persistence objects (Memento)
- **Layer 4** (Repository Adapter) converts between domain types and persistence types

### Layer 1: Public API Interface (Module Boundary) - OPTIONAL

**Only create if other modules need read-only access to this aggregate.**

Located in domain package root, defines **read-only** operations for other modules:

```java
package com.klabis.members;

@Port
public interface Members {
    Optional<Member> findById(UserId id);
    Optional<Member> findByRegistrationNumber(RegistrationNumber registrationNumber);
    Optional<Member> findByEmail(String email);
    Page<Member> findAll(Pageable pageable);
    int countByBirthYear(int birthYear);
}
```

**Annotations:**
- `@Port` - jMolecules hexagonal architecture (primary port for queries)

**Key Points:**
- **Public visibility** - exposed to other modules
- **Read-only** - no `save()` or write operations
- Uses domain types (Member, UserId, RegistrationNumber)
- Located in domain package root (`com.klabis.members`)
- Other modules use this interface, never the internal repository

### Layer 2: Domain Repository Interface (Internal Port)

Located in 'persistence' package, adds **write operations** for internal use.

**Variant A: With Public API (extends public interface)**

```java
package com.klabis.members.infrastructure;

@SecondaryPort
public interface MemberRepository extends Members {
    Member save(Member member);
    // Inherits all query methods from Members
}
```

**Variant B: Without Public API (standalone interface)**
```java
package com.klabis.users.persistence;

@SecondaryPort
public interface PasswordSetupTokenRepository {
    PasswordSetupToken save(PasswordSetupToken token);
    Optional<PasswordSetupToken> findByTokenHash(TokenHash tokenHash);
    List<PasswordSetupToken> findActiveTokensForUser(UserId userId);
    void invalidateAllForUser(UserId userId);
    int deleteExpiredTokens();
    // ... all methods defined here (no inheritance)
}
```

**Annotations:**
- `@SecondaryPort` - jMolecules hexagonal architecture (secondary port for persistence)
- **NEVER** use `@Repository` (jMolecules) here - only on adapter!

**Key Points:**
- **Internal visibility** - used only within owning module
- Either extends Public API (if exists) or stands alone
- Adds write operations (`save()`, `delete()`)
- Adds read operations which are not accessed from outside of the module
- Uses domain types (Member, UserId)
- Located in persistence package (`com.klabis.{module}.persistence`)

### Layer 3: JDBC Repository (Spring Data JDBC)

Spring Data JDBC repository working with Memento:

```java
package com.klabis.members.infrastructure.jdbc;

@Repository
interface MemberJdbcRepository extends
        CrudRepository<MemberMemento, UUID>,
        PagingAndSortingRepository<MemberMemento, UUID> {

    // Derived query methods
    Optional<MemberMemento> findByRegistrationNumber(String registrationNumber);

    Optional<MemberMemento> findByEmailEqualsIgnoreCase(String email);

    // Custom @Query methods
    @Query("SELECT COUNT(*) FROM members WHERE EXTRACT(YEAR FROM date_of_birth) = :birthYear")
    int countByBirthYear(@Param("birthYear") int birthYear);
}
```

**Key Points:**
- Works with `MemberMemento`, not `Member`
- ID type is `UUID` (primitive), not `UserId` (value object)
- Package-private visibility (internal to persistence package)
- Inherits `findAll(Pageable)` from PagingAndSortingRepository
- Inherits `save()`, `findById()`, `findAll()` from CrudRepository

### Layer 4: Repository Adapter (Hexagonal Architecture Adapter)

Bridges domain repository and JDBC repository.

```java
package com.klabis.members.infrastructure.jdbc;

@SecondaryAdapter
@Repository  // jMolecules @Repository
class MemberRepositoryAdapter implements MemberRepository {

    private final MemberJdbcRepository jdbcRepository;

    public MemberRepositoryAdapter(MemberJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public Member save(Member member) {
        MemberMemento savedMemento = jdbcRepository.save(MemberMemento.from(member));
        return savedMemento.toMember();
    }

    @Override
    public Optional<Member> findById(UserId memberId) {
        return jdbcRepository.findById(memberId.uuid())
                .map(MemberMemento::toMember);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return jdbcRepository.findByEmailEqualsIgnoreCase(email)
                .map(MemberMemento::toMember);
    }
}
```

**Annotations:**
- `@SecondaryAdapter` - jMolecules hexagonal architecture annotation
- `@Repository` - `jMolecules` stereotype annotation

**Key Points:**
- Implements Layer 1 (`Members`) if is it used 
- Implements Layer 2 (`MemberRepository`)
- Bridges Layer 2 (domain) and Layer 3 (JDBC) by converting types
- Converts domain types (Member, UserId) ↔ persistence types (MemberMemento, UUID)
- Package-private visibility (infrastructure detail)
- All conversions use `MemberMemento.from()` and `.toMember()`

## Pattern 3: Audit Metadata Handling

### AuditMetadata Value Object

```java
public record AuditMetadata(
    Instant createdAt,
    String createdBy,
    Instant lastModifiedAt,
    String lastModifiedBy,
    Long version
) {}
```

### Memento Audit Fields

```java
@CreatedDate
@Column("created_at")
private Instant createdAt;

@CreatedBy
@Column("created_by")
private String createdBy;

@LastModifiedDate
@Column("modified_at")
private Instant lastModifiedAt;

@LastModifiedBy
@Column("modified_by")
private String lastModifiedBy;

@Version
@Column("version")
private Long version;

public AuditMetadata getAuditMetadata() {
    if (this.createdAt == null) {
        return null;
    }
    return new AuditMetadata(
        this.createdAt,
        this.createdBy,
        this.lastModifiedAt,
        this.lastModifiedBy,
        this.version
    );
}
```

### Domain Entity Audit Access

```java
// In Member.java
private AuditMetadata auditMetadata;

public Instant getCreatedAt() {
    return auditMetadata != null ? auditMetadata.createdAt() : null;
}

public static Member reconstruct(..., AuditMetadata auditMetadata) {
    Member member = new Member(...);
    member.auditMetadata = auditMetadata;
    return member;
}
```

### isNew() Determination

```java
// In MemberMemento.from()
memento.isNew = (member.getAuditMetadata() == null);

// In Persistable<UUID>
@Override
public boolean isNew() {
    return this.isNew;
}
```

**Logic:**
- New members have `auditMetadata == null` → INSERT
- Existing members have audit metadata → UPDATE

## Pattern 4: Domain Event Publication

### Domain Entity Events

```java
// In Member.java
private final List<Object> domainEvents = new ArrayList<>();

protected void registerEvent(Object event) {
    this.domainEvents.add(event);
}

@DomainEvents
public List<Object> getDomainEvents() {
    return Collections.unmodifiableList(domainEvents);
}

public void clearDomainEvents() {
    this.domainEvents.clear();
}
```

### Memento Delegation

```java
// In MemberMemento.java
@Transient
private Member member;

@DomainEvents
public List<Object> getDomainEvents() {
    return this.member != null ? this.member.getDomainEvents() : List.of();
}

@AfterDomainEventPublication
public void clearDomainEvents() {
    if (this.member != null) {
        this.member.clearDomainEvents();
    }
}
```

**Flow:**
1. Member registers domain event in business logic
2. MemberMemento stores transient reference to Member
3. Spring Data JDBC calls `@DomainEvents` on Memento before save
4. Memento delegates to Member's `getDomainEvents()`
5. Spring Modulith publishes events via outbox pattern
6. Spring Data JDBC calls `@AfterDomainEventPublication` on Memento
7. Memento delegates to Member's `clearDomainEvents()`

## Checklist for New Aggregate

When creating a new aggregate with persistence layer:

### 1. Domain Entity
- [ ] Add `@AggregateRoot` annotation
- [ ] Use `@Identity` on ID field
- [ ] Use value objects (not primitives)
- [ ] Add `@DomainEvents` and `clearDomainEvents()` methods
- [ ] Create static `reconstruct()` method for persistence layer
- [ ] Accept `AuditMetadata` in `reconstruct()` method

### 2. Memento Class
- [ ] Add `@Table("table_name")` annotation
- [ ] Implement `Persistable<UUID>`
- [ ] Add `@Id`, `@Column` on all fields
- [ ] Add audit fields with `@CreatedDate`, `@LastModifiedDate`, `@Version`
- [ ] Add `@Transient Member member` field
- [ ] Add `@Transient boolean isNew` field
- [ ] Create `static from(Member)` method (domain → memento)
- [ ] Create `toMember()` method (memento → domain)
- [ ] Implement `isNew()` based on audit metadata
- [ ] Add `@DomainEvents` delegation to Member
- [ ] Add `@AfterDomainEventPublication` delegation to Member
- [ ] Create `getAuditMetadata()` helper method
- [ ] Flatten all value objects to primitive fields

### 3. Public API Interface (Module Boundary)
- [ ] Add `@Port` (jMolecules hexagonal - primary port)
- [ ] Use **public** visibility (exposed to other modules)
- [ ] Define **read-only** query methods (NO `save()`)
- [ ] Use domain types (Member, UserId, value objects)
- [ ] Place in domain package root (`{module}`)
- [ ] Name as plural of aggregate (e.g., `Members`, `Events`)
- [ ] Add Javadoc explaining this is for inter-module queries

### 4. JDBC Repository
- [ ] Extend `CrudRepository<Memento, UUID>`
- [ ] Extend `PagingAndSortingRepository<Memento, UUID>` if pagination needed
- [ ] Add `@Repository` annotation (Spring Data JDBC)
- [ ] Use package-private visibility
- [ ] Add derived query methods (findBy...)
- [ ] Add `@Query` methods for complex queries
- [ ] Use primitive types (String, UUID), not value objects
- [ ] Place in `{module}.persistence.jdbc` package

### 5. Domain Repository Interface (Internal Port)
- [ ] Add `@SecondaryPort` (jMolecules hexagonal)
- [ ] Extend public API interface (e.g., `extends Members`)
- [ ] Add write operations (`save()`)
- [ ] Use domain types (Member, UserId)
- [ ] Use internal visibility (not public)
- [ ] Place in `{module}.persistence` package
- [ ] Add Javadoc with `@apiNote` about internal usage

### 6. Repository Adapter
- [ ] Add `@SecondaryAdapter` (jMolecules)
- [ ] Add `@Repository` (jMolecules)
- [ ] NO `@Component` or `@Transactional` (managed by Spring/caller)
- [ ] Implement domain repository interface
- [ ] Implement public API interface (e.g., `implements Members, MemberRepository`)
- [ ] Inject JDBC repository via constructor
- [ ] Convert all methods: domain types ↔ memento
- [ ] Use package-private visibility
- [ ] Place in `{module}.persistence.jdbc` package

### 7. Database Schema
- [ ] Create Flyway migration with table definition
- [ ] Add all flattened columns
- [ ] Add audit columns (created_at, created_by, modified_at, modified_by, version)
- [ ] Add indexes on foreign keys and frequently queried columns
- [ ] Add appropriate constraints (NOT NULL, UNIQUE, CHECK)

## Testing Repository Implementations

### Test Structure

Integration tests for repositories use `@DataJdbcTest` slice for Spring Data JDBC testing:

```java
@DisplayName("Member JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
    type = FilterType.ANNOTATION,
    value = {Repository.class})  // jMolecules @Repository - loads all adapters
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("save() method")
    class SaveMethod {
        @Test
        @DisplayName("should save new member with all required fields")
        void shouldSaveNewMember() {
            // Given - create domain entity using factory method
            PersonalInformation personalInfo = PersonalInformation.of(
                "Jan", "Novák", LocalDate.of(2005, 3, 15), "CZ", Gender.MALE
            );
            Address address = Address.of("Hlavní 123", "Praha", "11000", "CZ");
            Member member = Member.create(
                new RegistrationNumber("ZBM0501"),
                personalInfo,
                address,
                new EmailAddress("jan.novak@example.com"),
                new PhoneNumber("+420123456789"),
                null
            );

            // When - save via repository
            Member savedMember = memberRepository.save(member);

            // Then - verify all fields persisted and loaded correctly
            Member readMember = memberRepository.findById(savedMember.getId());
            
            assertThat(readMember.getId()).isNotNull();
            assertThat(readMember.getFirstName()).isEqualTo("Jan");
            assertThat(readMember.getEmail()).isEqualTo(new EmailAddress("jan.novak@example.com"));
            // ... test all fields 
        }
    }
}
```

### Test Configuration

**Use `@DataJdbcTest` with component scan filter:**

```java
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
    type = FilterType.ANNOTATION,
    value = {Repository.class})  // jMolecules @Repository annotation
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```

**Key Points:**
- `@DataJdbcTest` loads minimal Spring Data JDBC context
- `includeFilters` scans for `@Repository` (jMolecules) to load all repository adapters
- Enables Spring context caching across test classes (faster test execution)

### Test Data Setup

**Database cleanup happens automatically via transaction rollback:**
- Each test runs in a transaction (from `@DataJdbcTest`)
- Transaction rolls back after test completion
- No need for `@Sql` cleanup statements

**Create test data using domain factory methods:**

```java
// ✅ Good - use domain factory methods
Member member = MemberTestDataBuilder.aMember()
                .withRegistrationNumber("ZBM0501")
                .withPersonalInfo(personalInfo)
                .withAddress(address)
                // ... anything needed
                .build();

// ❌ Bad - don't use constructors directly
Member member = new Member(...); // Constructor is private
```

### Test Assertions

**Test domain entities (not persistence objects):**

```java
// ✅ Good - test domain entity
Member savedMember = memberRepository.save(member);
assertThat(savedMember.getFirstName()).isEqualTo("Jan");
assertThat(savedMember.getEmail().value()).isEqualTo("jan@example.com");

// ❌ Bad - don't test Memento directly
MemberMemento memento = jdbcRepository.save(MemberMemento.from(member));
assertThat(memento.firstName).isEqualTo("Jan"); // Wrong layer
```

**Use custom assertions for complex objects:**

```java
MemberAssert.assertThat(savedMember)
    .hasFirstName("Jan")
    .hasLastName("Novák")
    .hasGender(Gender.MALE)
    .isActive()
    .hasGuardian(null);
```

### Test Isolation

**Each test runs in a transaction (auto-rollback):**

### Test Checklist

When writing repository integration tests:

- [ ] Write single test for Domain repository interface (e.g., `MemberRepository`). No other tests are needed. 
- [ ] Use `@DataJdbcTest` with `includeFilters` for `@Repository` (jMolecules)
- [ ] Add `@AutoConfigureTestDatabase(replace = NONE)` to use H2
- [ ] Create test data using domain factory methods (not constructors)
- [ ] Organize tests by method using `@Nested` classes
- [ ] Test CRUD operations (save, findById, findAll)
- [ ] Test derived query methods (findByX)
- [ ] Test custom @Query methods
- [ ] Test pagination and sorting
- [ ] Test update operations via business methods
- [ ] Test value object flattening/reconstruction
- [ ] Test audit metadata population
- [ ] Test optimistic locking (version field)

## Common Pitfalls

1. **Don't mix domain and persistence types in repository interfaces**
   - ❌ `Optional<MemberMemento> findById(UserId id)` - Mixed types
   - ✅ Domain repository: `Optional<Member> findById(UserId id)`
   - ✅ JDBC repository: `Optional<MemberMemento> findById(UUID id)`

2. **Don't forget transient member reference in Memento**
   - Needed for domain event delegation
   - Set in both `from()` and `toMember()` methods

3. **Don't skip audit metadata handling**
   - Use `isNew = (member.getAuditMetadata() == null)` in `from()`
   - Pass audit metadata to `reconstruct()` in `toMember()`

4. **Don't expose Memento or JDBC repository outside persistence package**
   - Use package-private visibility
   - Only domain repository interface should be visible to application layer

5. **Don't validate in `reconstruct()` method**
   - Reconstruction is for loading from database (already validated)
   - Validation only in factory methods (`create()`, `createWithId()`)

6. **Don't forget to flatten value objects**
   - Memento must have primitive fields matching database columns
   - Convert value objects ↔ primitives in `from()` and `toMember()`

7. **Don't test persistence objects directly**
   - Test domain entities (Member), not Memento
   - Repository adapter should hide Memento from tests

8. **Don't share test data between tests**
   - Each test creates its own data
   - Use transaction rollback for cleanup
