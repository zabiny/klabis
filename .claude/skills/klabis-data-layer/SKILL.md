---
name: data-layer-knowledge
description: "Patterns for Spring Data JDBC persistence with DDD: Memento pattern, three-layer repository architecture, audit metadata, domain event delegation. Use when creating aggregates, repositories, or testing data layer."
---

# Data Layer Knowledge

Comprehensive guide for implementing the data/persistence layer in Spring Data JDBC applications following Domain-Driven Design and Hexagonal Architecture principles.

## Overview

This skill provides patterns and best practices for creating a clean separation between domain logic and persistence concerns using:

- **Memento Pattern** - Separate domain entities from persistence annotations
- **Three-Layer Repository Architecture** - Domain interface, JDBC repository, adapter
- **Hexagonal Architecture** - Clear boundaries between domain and infrastructure
- **Value Object Flattening** - Convert domain value objects to database columns
- **Domain Event Delegation** - Integrate with Spring Modulith outbox pattern

## Core Architecture

The data layer consists of:

1. **Domain Entity** - Pure business logic, no Spring annotations (`Member`)
2. **Memento** - Persistence adapter with Spring Data JDBC annotations (`MemberMemento`)
3. **Public API Interface** - *(Optional)* Module boundary for queries (`Members`) - only if other modules need read access
4. **Domain Repository Interface** - Internal port with write operations (`MemberRepository`)
5. **JDBC Repository** - Spring Data JDBC repository (`MemberJdbcRepository`)
6. **Repository Adapter** - Bridges domain and JDBC layers (`MemberRepositoryAdapter`)

**Repository Architecture:**

```mermaid
flowchart TB
    subgraph "Other Modules"
        OM[Other Module Services]
    end

    subgraph "Members Module"
        subgraph "Application Layer"
            AS[Application Services]
        end

        subgraph "Domain Layer"
            L1["Layer 1: Members<br/>(Public API Interface)<br/>@Port | public<br/>read-only queries"]
            L2["Layer 2: MemberRepository<br/>(Domain Repository)<br/>@SecondaryPort | internal<br/>extends Members + save()"]
        end

        subgraph "Infrastructure Layer"
            L4["Layer 4: MemberRepositoryAdapter<br/>(Repository Adapter)<br/>@SecondaryAdapter + @Repository<br/>implements Members + MemberRepository"]
            L3["Layer 3: MemberJdbcRepository<br/>(JDBC Repository)<br/>@Repository | package-private<br/>works with MemberMemento"]
            MM[MemberMemento<br/>@Table]
        end
    end

    subgraph "Database"
        DB[(members table)]
    end

    OM -->|queries only| L1
    AS -->|read + write| L2
    L2 -.->|extends| L1
    L4 -.->|implements| L1
    L4 -.->|implements| L2
    L4 -->|delegates to| L3
    L3 -->|persist/load| MM
    MM -->|maps to| DB

    style L1 fill:#e1f5ff,stroke:#0066cc,stroke-width:2px
    style L2 fill:#fff4e1,stroke:#cc6600,stroke-width:2px
    style L3 fill:#f0f0f0,stroke:#666,stroke-width:2px
    style L4 fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style MM fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
```

## Quick Reference

### Creating Memento

```java
@Table("members")
class MemberMemento implements Persistable<UUID> {
    @Id private UUID id;
    @Column("registration_number") private String registrationNumber;
    @CreatedDate private Instant createdAt;
    @Version private Long version;
    @Transient private Member member;
    @Transient private boolean isNew = true;

    public static MemberMemento from(Member member) {
        MemberMemento memento = new MemberMemento();
        // Copy all fields from domain to memento
        memento.member = member;
        memento.isNew = (member.getAuditMetadata() == null);
        return memento;
    }

    public Member toMember() {
        Member member = Member.reconstruct(/* all fields + audit metadata */);
        this.member = member;
        return member;
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        return this.member != null ? this.member.getDomainEvents() : List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.member != null) this.member.clearDomainEvents();
    }
}
```

### Public API Interface (Module Boundary) - OPTIONAL

**Only create if other modules need read-only access to this aggregate.**

```java
@Port
public interface Members {
    Optional<Member> findById(UserId id);
    Optional<Member> findByRegistrationNumber(RegistrationNumber registrationNumber);
    Optional<Member> findByEmail(String email);
    Page<Member> findAll(Pageable pageable);
    int countByBirthYear(int birthYear);
}
```

### Domain Repository Interface (Internal Port)

**Two variants depending on whether Public API exists:**

**Variant A: With Public API (aggregate accessed by other modules)**
```java
@SecondaryPort
public interface MemberRepository extends Members {
    Member save(Member member);
    // Inherits all query methods from Members
}
```

**Variant B: Without Public API (internal aggregate)**
```java
@SecondaryPort
public interface PasswordSetupTokenRepository {
    PasswordSetupToken save(PasswordSetupToken token);
    Optional<PasswordSetupToken> findByTokenHash(TokenHash tokenHash);
    List<PasswordSetupToken> findActiveTokensForUser(UserId userId);
    // ... all methods defined directly here
}
```

### JDBC Repository

```java
@Repository
interface MemberJdbcRepository extends
    CrudRepository<MemberMemento, UUID>,
    PagingAndSortingRepository<MemberMemento, UUID> {

    Optional<MemberMemento> findByRegistrationNumber(String registrationNumber);

    @Query("SELECT COUNT(*) FROM members WHERE EXTRACT(YEAR FROM date_of_birth) = :year")
    int countByBirthYear(@Param("year") int year);
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
        MemberMemento saved = jdbcRepository.save(MemberMemento.from(member));
        return saved.toMember();
    }

    @Override
    public Optional<Member> findById(UserId memberId) {
        return jdbcRepository.findById(memberId.uuid())
                .map(MemberMemento::toMember);
    }
}
```

### Test Structure

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

    @Test
    void shouldSaveNewMember() {
        // Given - create domain entity
        Member member = Member.create(registrationNumber, personalInfo, ...);

        // When - save via repository
        Member saved = memberRepository.save(member);

        // Then - verify data read from DB 
        Member read = memberRepository.findById(saved.getId());
        
        assertThat(read.getId()).isNotNull();
        assertThat(read.getFirstName()).isEqualTo("Jan");
        // ... verify all attributes
    }
}
```

## Detailed Patterns

For comprehensive documentation of all patterns, including:

- Memento pattern implementation
- Three-layer repository architecture
- Audit metadata handling
- Domain event publication
- Value object flattening
- Testing strategies
- Complete checklists

See: [references/repository-patterns.md](references/repository-patterns.md)

## When to Read References

**Always read [repository-patterns.md](references/repository-patterns.md) when:**

- Creating a new aggregate persistence layer
- Implementing Memento for domain entity
- Setting up repository adapter
- Writing repository tests
- Need detailed examples of any pattern

**Pattern sections available:**

1. Pattern 1: Memento for Domain Entity Persistence
2. Pattern 2: Three-Layer Repository Architecture
3. Pattern 3: Audit Metadata Handling
4. Pattern 4: Domain Event Publication
5. Pattern 5: Value Object Flattening
6. Testing Repository Implementations
7. Common Pitfalls

## Key Principles

**Separation of Concerns:**
- Domain entity = Pure business logic
- Memento = Persistence adapter
- Repository interface = Domain port
- Repository adapter = Infrastructure implementation

**Type Boundaries:**
- Domain repository uses domain types (Member, UserId)
- JDBC repository uses primitives (MemberMemento, UUID)
- Adapter converts between layers

**Visibility:**
- Memento = package-private
- JDBC repository = package-private
- Repository adapter = package-private (but `@Repository` for test discovery)
- Domain repository interface = internal to module

**Annotations:**
- Public API interface: `@Port` (jMolecules) - only if other modules need access
- Domain repository: `@SecondaryPort` (NO `@Repository` jMolecules)
- Repository adapter: `@SecondaryAdapter` + `@Repository` (jMolecules)
- JDBC repository: `@Repository` (Spring Data JDBC)

**Public API Decision:**
- ✅ Create Public API if aggregate is queried by other modules
- ❌ Skip Public API if aggregate is internal to its module only
- Domain repository extends Public API (if it exists) or stands alone (if not)

**Testing:**
- Use `@DataJdbcTest` + `includeFilters` for `@Repository` (jMolecules) to find repository adapters
- Test domain entities, not Memento
- Use domain factory methods for test data
- Transaction rollback for cleanup (no `@Sql` needed)
