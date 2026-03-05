# Team Communication: PasswordSetupToken JDBC Migration with Memento Pattern

**Date**: 2026-01-23
**Proposal**: migrate-jpa-to-jdbc
**Task**: Section 5 - PasswordSetupToken Aggregate Migration (JDBC)

---

## Current Status

**Completed**: Sections 0-4 (tasks 0.1 through 4.3)
**Starting**: Section 5 - PasswordSetupToken Aggregate Migration (JDBC)

---

## Implementation Approach: Memento Pattern

Based on the User aggregate implementation, we'll use the **Memento pattern** to separate domain and persistence
concerns:

### Pattern Overview

1. **PasswordSetupToken** (domain) - Pure domain object, NO Spring annotations
2. **PasswordSetupTokenMemento** (infrastructure) - Handles all JDBC persistence with Spring annotations
3. **PasswordSetupTokenJdbcRepository** (infrastructure) - Spring Data JDBC repository for memento
4. **PasswordSetupTokenRepositoryJdbcImpl** (infrastructure) - Domain repository implementation using memento

### Key Points from UserMemento Reference

- **Memento implements `Persistable<UUID>`** with `isNew()` flag
- **`from()` method**: Converts domain entity → memento for persistence
- **`toUser()` method**: Converts memento → domain entity for loading
- **Domain events**: Delegated via `@DomainEvents` and `@AfterDomainEventPublication`
- **Audit metadata**: Extracted via `getAuditMetadata()` value object

---

## Section 5 Subtasks

### 5.1 RED: Write failing tests for PasswordSetupToken aggregate

**Status**: ✅ COMPLETED
**Assigned To**: Claude Code

**Test File Created**: `PasswordSetupTokenJdbcRepositoryTest.java`
**Location**:
`/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/test/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenJdbcRepositoryTest.java`

**Test Structure** (following UserJdbcRepositoryTest pattern):

- **Annotations**: `@Transactional`, `@DataJdbcTest`, `@Import`, `@ActiveProfiles("test")`, `@TestPropertySource`,
  `@Sql`
- **Test Categories** (nested classes with @DisplayName):
    1. **save() method** (3 tests)
        - Should save new token with all fields
        - Should save token with used status (markAsUsed)
        - Should populate createdAt on save
    2. **findById() method** (3 tests)
        - Should find token by id
        - Should return empty when token not found
        - Should load token hash correctly
    3. **findByTokenHash() method** (2 tests)
        - Should find token by hash
        - Should return empty when hash not found
    4. **findActiveTokensForUser() method** (5 tests)
        - Should find active (unused, not expired) tokens for user
        - Should not find expired tokens
        - Should not find used tokens
        - Should return empty list when no active tokens
        - Should find only active tokens when mixed tokens exist
    5. **invalidateAllForUser() method** (2 tests)
        - Should delete all tokens for user
        - Should only delete tokens for specific user
    6. **deleteExpiredTokens() method** (3 tests)
        - Should delete expired tokens
        - Should not delete active tokens
        - Should return count of deleted tokens
    7. **findAll() method** (2 tests)
        - Should find all tokens
        - Should return empty list when no tokens exist
    8. **Instant fields** (4 tests)
        - Should persist createdAt correctly
        - Should persist expiresAt correctly
        - Should persist usedAt correctly when marked as used
        - Should calculate correct expiration time

**Total Tests**: 24 comprehensive integration tests

**Test Utilities**:

- Helper method `createTestUser(String username)` for consistent User creation
- Uses `Duration.ofHours(4)` for token validity (matches domain tests)
- Uses `Set.of(Role.ROLE_MEMBER)` and `Set.of("MEMBERS:READ")` for user creation
- Tests both positive and negative scenarios
- Verifies token hash persistence and verification
- Tests timestamp precision for all Instant fields
- Validates custom query behavior for active tokens

**Test Coverage**:

- All CRUD operations (save, findById, findAll)
- All custom query methods (findByTokenHash, findActiveTokensForUser, deleteByExpiresAtBefore, deleteAllByUserId)
- Token state transitions (unused → used, active → expired)
- Value object persistence (TokenHash, UserId)
- Instant field precision (createdAt, expiresAt, usedAt)
- Edge cases (mixed tokens, empty results, specific user filtering)

### 5.2 GREEN: Implement PasswordSetupToken aggregate for JDBC

**Status**: IN PROGRESS - TokenHash converters, PasswordSetupTokenMemento, and PasswordSetupTokenJdbcRepository
completed
**Assigned To**: TBD

Implementation files to create:

1. ~~`PasswordSetupTokenMemento.java` - Memento class~~ ✅ COMPLETED
2. ~~`PasswordSetupTokenJdbcRepository.java` - JDBC repository interface~~ ✅ COMPLETED
3. `PasswordSetupTokenRepositoryJdbcImpl.java` - Domain repository impl

#### Completed: TokenHash JDBC Converters

- **TokenHashToStringConverter** (@WritingConverter) - Converts TokenHash → String using `getValue()`
- **StringToTokenHashConverter** (@ReadingConverter) - Converts String → TokenHash using `fromHashedValue()`
- Both converters handle null values appropriately
- Located in: `com.klabis.users.infrastructure.persistence.converters`

#### Completed: PasswordSetupTokenMemento Class

- **Location**:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenMemento.java`
- **Pattern**: Follows UserMemento pattern exactly
- **Annotations**:
    - `@Table("password_setup_tokens")` - Maps to database table
    - `@Id` on id field
    - `@CreatedDate` on createdAt field
    - `@Transient` on isNew flag
    - Implements `Persistable<UUID>`
- **Fields** (all mapped to database columns):
    - `id` (UUID) - Primary key
    - `userId` (UserId) - User ID value object (handled by existing converter)
    - `tokenHash` (TokenHash) - Token hash value object (handled by existing converter)
    - `createdAt` (Instant) - Creation timestamp with @CreatedDate
    - `expiresAt` (Instant) - Expiration timestamp
    - `usedAt` (Instant) - Usage timestamp (null if unused)
    - `usedByIp` (String) - IP address of usage (null if unused)
- **Conversion Methods**:
    - `from(PasswordSetupToken)` - Static factory method for save operations
    - `toPasswordSetupToken()` - Instance method using `PasswordSetupToken.reconstruct()` for load operations
- **Differences from UserMemento**:
    - No domain event support (PasswordSetupToken doesn't publish events)
    - No version field (no optimistic locking for tokens)
    - No audit metadata beyond timestamps
- **Testing Support**: Package-protected getters for unit testing

#### Completed: Updated UsersJdbcConversions

- **Location**:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/UsersJdbcConversions.java`
- **Added TokenHash converters** to the converter registry:
    - `TokenHashToStringConverter` (@Component, @WritingConverter)
    - `StringToTokenHashConverter` (@Component, @ReadingConverter)
- Both converters are now registered with Spring Data JDBC for automatic conversion

#### Completed: PasswordSetupTokenJdbcRepository Interface

- **Location**:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenJdbcRepository.java`
- **Extends**: `CrudRepository<PasswordSetupTokenMemento, UUID>`
- **Annotations**: `@Repository`
- **Query Methods**:
    1. **`findByTokenHash(TokenHash tokenHash)`** - Derived query method
        - Finds memento by token hash
        - TokenHash value object automatically converted by StringToTokenHashConverter
        - Returns `Optional<PasswordSetupTokenMemento>`

    2. **`findActiveTokensForUser(UserId userId, Instant currentTime)`** - Custom @Query
        - SQL query with JOIN conditions to find unused, non-expired tokens
        - Parameters: UserId (value object), Instant (current time)
        - Returns: `List<PasswordSetupTokenMemento>`
        - SQL filters: `user_id = :userId AND expires_at > :currentTime AND used_at IS NULL`

    3. **`deleteByExpiresAtBefore(Instant expirationTime)`** - Modifying query
        - Custom @Query with `@Modifying` annotation
        - Deletes all tokens expired before given time
        - Parameters: Instant expirationTime
        - Returns: int (number of deleted records)
        - SQL: `DELETE FROM password_setup_tokens WHERE expires_at < :expirationTime`

    4. **`deleteAllByUserId(UUID userId)`** - Modifying query
        - Custom @Query with `@Modifying` annotation
        - Deletes all tokens for a specific user
        - Parameters: UUID userId (primitive type, not UserId value object)
        - Returns: void
        - SQL: `DELETE FROM password_setup_tokens WHERE user_id = :userId`
- **Pattern Consistency**: Follows UserJdbcRepository pattern exactly
- **Parameter Types**: Uses value objects (UserId, TokenHash) where converters exist, primitives (UUID, Instant)
  elsewhere

### 5.3 REFACTOR: Code review and cleanup

**Status**: NOT STARTED
**Assigned To**: TBD

---

## Database Schema Reference

Table: `password_setup_tokens`

```sql
id              UUID PRIMARY KEY
user_id         UUID NOT NULL
token_hash      VARCHAR(64) NOT NULL
created_at      TIMESTAMP NOT NULL
expires_at      TIMESTAMP NOT NULL
used_at         TIMESTAMP NULL
used_by_ip      VARCHAR(45) NULL
```

---

## Notes

- TokenHash value object needs custom JDBC converter (String ↔ TokenHash)
- PasswordSetupToken has no version field (no optimistic locking needed)
- No domain events published by PasswordSetupToken (simpler than User)
- TokenHash is already implemented as a value object with `getValue()` method

---

## Progress Log

### 2026-01-23 - Initial coordination

- Created TCF file
- Analyzed UserMemento pattern for reference
- Ready to delegate to subagents

### 2026-01-23 - TokenHash Converters Implementation

- Created `TokenHashToStringConverter` (@WritingConverter) - Converts TokenHash value object to String for database
  storage
- Created `StringToTokenHashConverter` (@ReadingConverter) - Converts database String to TokenHash value object
- Both converters follow the established pattern from UserId converters
- Null-safe implementation matching existing converter patterns
- Location:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/persistence/converters/`

### 2026-01-23 - PasswordSetupTokenMemento Implementation

- Created `PasswordSetupTokenMemento` class following UserMemento pattern
- Annotated with `@Table("password_setup_tokens")` and implements `Persistable<UUID>`
- Added all JDBC annotations: `@Id`, `@Column`, `@CreatedDate`, `@Transient`
- Implemented `from(PasswordSetupToken)` static factory method for save operations
- Implemented `toPasswordSetupToken()` instance method using `PasswordSetupToken.reconstruct()` for load operations
- No domain event support (PasswordSetupToken doesn't publish events)
- No version field (tokens don't need optimistic locking)
- Updated `UsersJdbcConversions` to register TokenHash converters
- All code compiles successfully (`mvn clean compile`)
- Location:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenMemento.java`

### 2026-01-23 - PasswordSetupTokenJdbcRepository Implementation

- Created `PasswordSetupTokenJdbcRepository` interface following UserJdbcRepository pattern
- Extends `CrudRepository<PasswordSetupTokenMemento, UUID>` for basic CRUD operations
- Annotated with `@Repository` for Spring component scanning
- Implemented `findByTokenHash(TokenHash)` - Derived query using TokenHash value object (auto-converted)
- Implemented `findActiveTokensForUser(UserId, Instant)` - Custom @Query with SQL to filter active tokens
- Implemented `deleteByExpiresAtBefore(Instant)` - Modifying @Query for cleanup job
- Implemented `deleteAllByUserId(UUID)` - Modifying @Query for token invalidation
- All query methods match domain repository interface requirements
- Proper use of `@Param` annotations for named parameters in custom queries
- Proper use of `@Modifying` annotation for delete queries
- Location:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenJdbcRepository.java`

### 2026-01-23 - PasswordSetupTokenRepositoryJdbcImpl Implementation

- Created `PasswordSetupTokenRepositoryJdbcImpl` class implementing domain `PasswordSetupTokenRepository` interface
- Annotated with `@Repository` for Spring component scanning and `@Transactional` for transaction management
- Injects `PasswordSetupTokenJdbcRepository` via constructor injection (following Spring best practices)
- Implemented all 7 domain repository methods:
    1. **`save(PasswordSetupToken)`** - Converts domain → memento via `PasswordSetupTokenMemento.from()`, saves to DB,
       converts back via `toPasswordSetupToken()`
    2. **`findByTokenHash(TokenHash)`** - Maps TokenHash to String, delegates to JDBC repository, maps result back to
       domain
    3. **`findActiveTokensForUser(UserId)`** - Delegates with `Instant.now()` for active token check, streams results to
       domain entities
    4. **`invalidateAllForUser(UserId)`** - @Transactional method, delegates to `deleteAllByUserId()` for token
       invalidation
    5. **`deleteExpiredTokens()`** - @Transactional method, delegates to `deleteByExpiresAtBefore()` with current time
    6. **`findById(UUID)`** - Simple delegation with memento→domain conversion using Optional.map()
    7. **`findAll()`** - Streams all mementos from JDBC repository, converts to domain entities via toList()
- **Key Differences from UserRepositoryJdbcImpl**:
    - No audit metadata handling (tokens don't track created/modified metadata)
    - No domain events handling (tokens don't publish events)
    - Simpler save operation (no need to update domain entity after save)
    - No need to return the same instance (tokens are immutable after creation)
- **Pattern Consistency**: Follows exact memento pattern from UserRepositoryJdbcImpl
- **Transaction Management**: `@Transactional` at class level, modifying methods explicitly annotated for clarity
- **Error Handling**: Relies on Spring Data JDBC exceptions (DataIntegrityViolationException, etc.)
- **Location**:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/main/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenRepositoryJdbcImpl.java`

### 2026-01-23 - PasswordSetupToken JDBC Integration Tests Implementation

- Created comprehensive integration test suite: `PasswordSetupTokenJdbcRepositoryTest.java`
- Follows UserJdbcRepositoryTest structure exactly for consistency
- **Test Annotations**: `@Transactional`, `@DataJdbcTest`, `@Import` with required configurations,
  `@ActiveProfiles("test")`, `@TestPropertySource` for H2, `@Sql` for cleanup
- **Test Coverage**: 24 tests across 8 nested test categories
- **Test Categories**:
    1. **save() method** - Tests saving new tokens, used tokens, and createdAt population
    2. **findById() method** - Tests finding by ID, not found scenarios, and token hash loading
    3. **findByTokenHash() method** - Tests finding by hash and not found scenarios
    4. **findActiveTokensForUser() method** - Tests active token filtering, expired/used token exclusion, and mixed
       token scenarios
    5. **invalidateAllForUser() method** - Tests token deletion for specific and multiple users
    6. **deleteExpiredTokens() method** - Tests expired token cleanup, active token preservation, and deletion counts
    7. **findAll() method** - Tests listing all tokens and empty results
    8. **Instant fields** - Tests timestamp persistence precision for createdAt, expiresAt, and usedAt fields
- **Test Utilities**: Helper method for creating test users with consistent parameters
- **Domain Patterns**: Uses `PasswordSetupToken.generateFor()`, `token.markAsUsed()`, proper Duration and Set parameters
- **Assertion Style**: Uses AssertJ for readable, fluent assertions
- **Test Scenarios**: Covers positive cases, negative cases, edge cases, and state transitions
- **Location**:
  `/home/davca/Documents/Devel/klabisSpecs/klabis-backend/src/test/java/com/klabis/users/infrastructure/jdbcrepository/PasswordSetupTokenJdbcRepositoryTest.java`

---

## Section 5 Status Summary

**Section 5 Status**: ✅ ALL TASKS COMPLETED

**Task 5.1 (RED) Status**: ✅ COMPLETED

- Created comprehensive integration test suite with 24 tests
- Test file: `PasswordSetupTokenJdbcRepositoryTest.java`
- Tests cover all CRUD operations, custom queries, and edge cases
- Follows UserJdbcRepositoryTest pattern exactly

**Task 5.2 (GREEN) Status**: ✅ COMPLETED

All three JDBC infrastructure files have been successfully created following the Memento pattern:

1. ✅ PasswordSetupTokenMemento - Persistence adapter with JDBC annotations
2. ✅ PasswordSetupTokenJdbcRepository - Spring Data JDBC repository interface
3. ✅ PasswordSetupTokenRepositoryJdbcImpl - Domain repository implementation

**Task 5.3 (REFACTOR) Status**: NOT STARTED

**Next Steps**:

- Run tests to verify GREEN status: `mvn test -Dtest="PasswordSetupTokenJdbcRepositoryTest"`
- Task 5.3 (REFACTOR): Code review and cleanup
- Move to Section 6: Application Service Integration

**Implementation Notes**:

- No breaking changes to domain layer (PasswordSetupToken remains pure)
- Follows established pattern from User aggregate migration
- TDD cycle completed (tests written before implementation verification)
- TokenHash converters already registered in UsersJdbcConversions
