# User Activation Specification Delta

## MODIFIED Requirements

### Requirement: PasswordSetupToken Aggregate Persistence

The system SHALL persist PasswordSetupToken aggregates using Spring Data JDBC with direct domain object mapping.

#### Scenario: PasswordSetupToken persisted to database

- **WHEN** PasswordSetupToken is saved via PasswordSetupTokenRepository
- **THEN** Spring Data JDBC persists PasswordSetupToken directly without entity mapping
- **AND** Instant fields (createdAt, expiresAt, usedAt) stored as TIMESTAMP
- **AND** Token hash stored as SHA-256 string (64 characters)
- **AND** User ID stored as UUID (not a foreign key relationship)
- **AND** PasswordSetupToken implements Persistable<UUID> for new entity detection

#### Scenario: PasswordSetupToken loaded from database

- **WHEN** PasswordSetupToken is retrieved by ID or token hash
- **THEN** Spring Data JDBC reconstructs PasswordSetupToken domain object directly
- **AND** Instant fields reconstructed from TIMESTAMP columns
- **AND** No version field needed (no concurrent updates expected)

#### Scenario: Active tokens queried for user

- **WHEN** PasswordSetupTokenRepository.findActiveTokensForUser(userId, currentTime) is called
- **THEN** Custom @Query executed with named parameters
- **AND** Query filters by userId, null usedAt, and expiresAt > currentTime
- **AND** List of active PasswordSetupToken objects returned

#### Scenario: Expired tokens deleted

- **WHEN** PasswordSetupTokenRepository.deleteByExpiresAtBefore(expirationTime) is called
- **THEN** @Modifying @Query executed in transaction
- **AND** All tokens with expiresAt < expirationTime deleted
- **AND** Number of deleted rows returned

#### Scenario: All tokens for user invalidated

- **WHEN** PasswordSetupTokenRepository.deleteAllByUserId(userId) is called
- **THEN** @Modifying @Query executed in transaction
- **AND** All tokens for user deleted
- **AND** Supports password reset flow (invalidate old tokens before creating new)

---

## REMOVED Requirements

### Requirement: JPA Entity Mapping for PasswordSetupToken

**Reason**: Spring Data JDBC eliminates the need for separate entity classes (PasswordSetupTokenEntity) and manual
mapper classes (PasswordSetupTokenMapper). Domain objects are persisted directly.

**Migration**:

- Remove PasswordSetupTokenEntity.java
- Remove PasswordSetupTokenMapper.java
- Remove PasswordSetupTokenJpaRepository.java
- PasswordSetupToken domain class now implements Persistable<UUID> and includes Spring Data JDBC annotations
- PasswordSetupTokenRepository implementation delegates directly to PasswordSetupTokenJdbcRepository
- Custom JPQL queries converted to native SQL queries with @Query annotation
