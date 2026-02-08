# Members Specification Delta

## MODIFIED Requirements

### Requirement: Member Aggregate Persistence

The system SHALL persist Member aggregates using Spring Data JDBC with direct domain object mapping and custom
converters for value objects.

#### Scenario: Member aggregate persisted to database

- **WHEN** Member aggregate is saved via MemberRepository
- **THEN** Spring Data JDBC persists Member directly without entity mapping
- **AND** Value objects (Address, EmailAddress, PhoneNumber) converted to JSON via custom converters
- **AND** Guardian information stored as individual columns
- **AND** Optimistic locking version field incremented automatically
- **AND** Audit fields (createdAt, createdBy, lastModifiedAt, lastModifiedBy) populated automatically

#### Scenario: Member aggregate loaded from database

- **WHEN** Member is retrieved by ID, registrationNumber, or email
- **THEN** Spring Data JDBC reconstructs Member domain object directly
- **AND** Value objects reconstructed from JSON via custom converters
- **AND** Guardian information reconstructed from individual columns
- **AND** Version field loaded for optimistic locking
- **AND** Member implements Persistable<UUID> for new entity detection

#### Scenario: Member value objects persisted as JSON

- **WHEN** Member has Address value object
- **THEN** AddressToStringConverter serializes Address to JSON string
- **AND** JSON stored in address column (TEXT type)
- **AND** On load, StringToAddressConverter deserializes JSON to Address object
- **AND** Conversion errors handled gracefully (null or default value)

#### Scenario: Member queries with Spring Data JDBC

- **WHEN** MemberRepository.findByEmail(email) is called
- **THEN** Spring Data JDBC derived query executed
- **AND** Member returned if email matches
- **WHEN** MemberRepository.countByDateOfBirthBetween(start, end) is called
- **THEN** Custom @Query executed with named parameters
- **AND** Count of members in age range returned

#### Scenario: Member pagination with Spring Data JDBC

- **WHEN** MemberRepository.findAll(Pageable) is called
- **THEN** Spring Data JDBC executes paginated query
- **AND** Page of Member domain objects returned
- **AND** Total count and page metadata included

#### Scenario: Optimistic locking prevents concurrent member updates

- **WHEN** two transactions load same Member and both attempt to save
- **THEN** first save succeeds and increments version
- **AND** second save fails with OptimisticLockingFailureException
- **AND** Application enforces retry or conflict resolution

---

## ADDED Requirements

### Requirement: Custom Type Converters for Value Objects

The system SHALL provide custom Spring Data JDBC converters to persist and retrieve value objects as JSON.

#### Scenario: Address value object conversion

- **WHEN** Member contains Address value object
- **THEN** AddressToStringConverter (@WritingConverter) serializes Address to JSON
- **AND** StringToAddressConverter (@ReadingConverter) deserializes JSON to Address
- **AND** Converters registered in JdbcConfiguration.userConverters()

#### Scenario: EmailAddress value object conversion

- **WHEN** Member contains EmailAddress value object
- **THEN** EmailAddressToStringConverter serializes EmailAddress to string
- **AND** StringToEmailAddressConverter deserializes string to EmailAddress
- **AND** Validation occurs in value object constructor

#### Scenario: PhoneNumber value object conversion

- **WHEN** Member contains PhoneNumber value object
- **THEN** PhoneNumberToStringConverter serializes PhoneNumber to string
- **AND** StringToPhoneNumberConverter deserializes string to PhoneNumber

#### Scenario: Converter error handling

- **WHEN** JSON deserialization fails (malformed data)
- **THEN** Converter logs error
- **AND** Returns null or default value to prevent application crash
- **AND** Application layer validates member integrity

---

## REMOVED Requirements

### Requirement: JPA Entity Mapping for Member

**Reason**: Spring Data JDBC eliminates the need for separate entity classes (MemberEntity) and manual mapper classes (
MemberMapper). Domain objects are persisted directly.

**Migration**:

- Remove MemberEntity.java
- Remove MemberMapper.java
- Remove MemberJpaRepository.java
- Member domain class now implements Persistable<UUID> and includes Spring Data JDBC annotations
- MemberRepository implementation delegates directly to MemberJdbcRepository
- Value objects require custom converters instead of JPA @Embedded or denormalized columns
