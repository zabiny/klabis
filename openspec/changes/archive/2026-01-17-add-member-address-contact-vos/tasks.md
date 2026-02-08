# Implementation Tasks: Add Member Address and Contact Information Value Objects

## Overview

This change adds Address, EmailAddress, and PhoneNumber value objects to the Member aggregate, replacing the simplified
contact storage (CSV lists) with single validated contacts and adding missing address functionality. Implementation
follows TDD and Clean Architecture principles.

**Note**: Application uses in-memory H2 database only. No data migration needed - just update initial schema.

## Task Breakdown

### 1. Domain Layer - Address Value Object

Create Address value object with validation for street, city, postal code, and country (ISO 3166-1 alpha-2).

- [x] 1.1 Create Address value object class
    - Package: `com.klabis.members.domain` ✓ (Address.java)
    - Fields: street, city, postalCode, country (all final) ✓
    - Factory method: `Address.of()` ✓
    - Validation: all fields required, length limits, postal code alphanumeric only, country alpha-2 ✓
    - **Validation**: Unit tests for Address.of() with valid/invalid inputs (AddressTest.java)

- [x] 1.2 Write unit tests for Address value object
    - Test valid address creation (country="CZ") ✓
    - Test null/blank field rejection ✓
    - Test oversized field rejection ✓
    - Test invalid postal code format ✓
    - Test invalid country code format (not ISO 3166-1 alpha-2) ✓
    - **Validation**: All Address tests pass (AddressTest.java)

### 2. Domain Layer - Contact Value Objects

Create EmailAddress and PhoneNumber value objects with format validation.

- [x] 2.1 Create EmailAddress value object
    - Package: `com.klabis.members.domain` ✓ (EmailAddress.java)
    - Field: value (final String) ✓
    - Factory method: `EmailAddress.of()` ✓
    - Validation: RFC 5322 basic format (contains @ and domain) ✓
    - **Validation**: Unit tests for EmailAddress (EmailAddressTest.java)

- [x] 2.2 Write unit tests for EmailAddress
    - Test valid email creation ("john@example.com") ✓
    - Test invalid email without @ rejected ✓
    - Test invalid email without domain rejected ✓
    - Test blank email rejected ✓
    - **Validation**: All EmailAddress tests pass (EmailAddressTest.java)

- [x] 2.3 Create PhoneNumber value object
    - Package: `com.klabis.members.domain` ✓ (PhoneNumber.java)
    - Field: value (final String) ✓
    - Factory method: `PhoneNumber.of()` ✓
    - Validation: E.164 format (starts with +, digits and spaces only) ✓
    - **Validation**: Unit tests for PhoneNumber (PhoneNumberTest.java)

- [x] 2.4 Write unit tests for PhoneNumber
    - Test valid E.164 phone creation ("+420123456789") ✓
    - Test valid E.164 with spaces ("+420 123 456 789") ✓
    - Test invalid phone without + prefix rejected ✓
    - Test invalid phone with letters rejected ✓
    - Test blank phone rejected ✓
    - **Validation**: All PhoneNumber tests pass (PhoneNumberTest.java)

### 3. Domain Layer - Update Member Aggregate

Update Member aggregate to include Address field and replace email/phone sets with single EmailAddress and PhoneNumber.

- [x] 3.1 Add address field to Member aggregate
    - Add `private final Address address;` field ✓
    - Update `create()` factory method to accept Address parameter ✓
    - Update `reconstruct()` method to accept Address parameter ✓
    - Update constructor to accept Address ✓
    - Add `getAddress()` getter ✓
    - **Validation**: Member compiles successfully ✓

- [x] 3.2 Replace email/phone sets with single EmailAddress and PhoneNumber
    - Replace `Set<String> emails` with `EmailAddress email` ✓
    - Replace `Set<String> phones` with `PhoneNumber phone` ✓
    - Update `create()` to accept EmailAddress and PhoneNumber instead of sets ✓
    - Update `reconstruct()` to accept single email/phone ✓
    - Remove `getEmails()` and `getPhones()` getters ✓
    - Add `getEmail()` and `getPhone()` getters ✓
    - **Validation**: Member compiles successfully ✓

- [x] 3.3 Update Member validation rules
    - Remove contact validation from Member (now handled by value objects) ✓
    - Keep guardian validation (guardian required for minors) ✓
    - **Validation**: Member.create() validation tests updated and pass ✓

- [x] 3.4 Update Member unit tests
    - Update existing Member tests to use Address, EmailAddress, and PhoneNumber ✓
    - Update test data builders to create single email/phone instead of sets ✓
    - Update address examples to use ISO 3166-1 alpha-2 country codes (CZ, US, DE) ✓
    - Verify all Member invariant tests still pass ✓
    - **Validation**: All Member domain tests pass (MemberTest.java)

### 3.5 Domain Layer - Update GuardianInformation Value Object

Migrate GuardianInformation to use EmailAddress and PhoneNumber value objects for consistent validation.

- [x] 3.5.1 Update GuardianInformation to use EmailAddress and PhoneNumber
    - Replace `private final String email;` with `private final EmailAddress email;` ✓
    - Replace `private final String phone;` with `private final PhoneNumber phone;` ✓
    - Update constructor to accept EmailAddress and PhoneNumber instead of strings ✓
    - Update `getEmail()` to return EmailAddress (may need new method for String extraction) ✓ (added getEmailValue())
    - Update `getPhone()` to return PhoneNumber (may need new method for String extraction) ✓ (added getPhoneValue())
    - Update `hasEmail()` and `hasPhone()` methods to work with new types ✓ (removed as they were always true)
    - **Validation**: GuardianInformation compiles successfully ✓

- [x] 3.5.2 Write/update GuardianInformation unit tests
    - Test guardian creation with valid EmailAddress and PhoneNumber ✓
    - Test invalid email/phone rejected during construction ✓
    - Test round-trip serialization (domain → entity → domain) ✓
    - **Validation**: All GuardianInformation tests pass ✓

### 4. Infrastructure Layer - Update Database Schema

Update initial Flyway migration to include address and simplified contact columns.

- [x] 4.1 Update V001__create_members_table.sql
    - Add columns: `street` VARCHAR(200), `city` VARCHAR(100), `postal_code` VARCHAR(20), `country` VARCHAR(2) ✓
    - Replace `emails TEXT` with `email` VARCHAR(255) ✓
    - Replace `phones TEXT` with `phone` VARCHAR(50) ✓
    - All columns nullable (address can be null, email/phone validated at application layer) ✓
    - Add comment: country uses ISO 3166-1 alpha-2 format ✓
    - **Validation**: SQL script is valid and follows Flyway conventions ✓

- [x] 4.2 Verify schema on H2 startup
    - Run application with `mvn spring-boot:run` ✓
    - Check H2 console (http://localhost:8080/h2-console) ✓
    - Verify members table has address columns (street, city, postal_code, country) ✓
    - Verify members table has single email/phone columns (not TEXT arrays) ✓
    - **Validation**: Schema matches expected structure ✓

### 5. Infrastructure Layer - Update MemberEntity

Update JPA entity to map new address and simplified contact fields.

- [x] 5.1 Add address columns to MemberEntity
    - Add fields: street, city, postalCode, country with @Column annotations ✓
    - Country column: `@Column(name = "country", length = 2)` for ISO 3166-1 alpha-2 ✓
    - Update constructor to accept address fields ✓
    - Add getters for address fields ✓
    - **Validation**: MemberEntity compiles successfully ✓

- [x] 5.2 Replace email/phone TEXT columns with single VARCHAR columns
    - Change field: `emails` (String TEXT) → `email` (String VARCHAR(255)) ✓
    - Change field: `phones` (String TEXT) → `phone` (String VARCHAR(50)) ✓
    - Remove CSV parsing logic from getters (getEmails/getPhones methods) ✓
    - Update constructor to accept single email/phone strings ✓
    - Add simple getters for email and phone ✓
    - **Validation**: MemberEntity compiles successfully ✓

### 6. Infrastructure Layer - Update MemberMapper

Update mapper to convert between domain and entity representations.

- [x] 6.1 Update MemberMapper.toEntity()
    - Extract address fields from Member.getAddress() ✓
    - Extract email from EmailAddress.value() ✓
    - Extract phone from PhoneNumber.value() ✓
    - Build MemberEntity with address and contact fields ✓
    - **Validation**: Mapper compiles and converts Address/EmailAddress/PhoneNumber correctly ✓

- [x] 6.2 Update MemberMapper.toDomain()
    - Reconstruct Address from entity address fields ✓
    - Reconstruct EmailAddress from entity email field ✓
    - Reconstruct PhoneNumber from entity phone field ✓
    - Build Member domain object ✓
    - **Validation**: Mapper converts entity back to domain correctly ✓

- [x] 6.3 Write mapper unit tests
    - Test toEntity() with valid Address (country="CZ"), EmailAddress, PhoneNumber ✓
    - Test toDomain() with all fields populated ✓
    - Test round-trip conversion (domain → entity → domain) ✓
    - **Validation**: All mapper tests pass (MemberMapperTest.java)

- [x] 6.4 Update MemberMapper to handle GuardianInformation value objects
    - Update toEntity() to extract EmailAddress.value() and PhoneNumber.value() from guardian ✓
    - Update toDomain() to reconstruct EmailAddress and PhoneNumber for guardian ✓
    - Handle null guardian case (no guardian fields) ✓
    - **Validation**: Mapper correctly handles guardian email/phone as value objects ✓

### 7. Application Layer - Update DTOs

Update command and response DTOs to reflect new structure.

- [x] 7.1 Create AddressRequest DTO
    - Package: `com.klabis.members.application` ✓
    - Fields: street, city, postalCode, country ✓
    - Add validation annotations (@NotBlank, @Size, @Pattern for country alpha-2) ✓
    - **Validation**: DTO compiles with validation annotations ✓

- [x] 7.2 Update RegisterMemberCommand
    - Change `List<String> emails` to `String email` ✓
    - Change `List<String> phones` to `String phone` ✓
    - Add `AddressRequest address` field ✓
    - Update validation: email and phone required (@NotBlank, @Email) ✓
    - **Validation**: Command compiles with updated fields ✓

- [x] 7.3 Create AddressResponse DTO
    - Package: `com.klabis.members.application` ✓
    - Fields: street, city, postalCode, country ✓
    - **Validation**: DTO compiles ✓

- [x] 7.4 Update MemberResponse
    - Change `List<String> emails` to `String email` ✓
    - Change `List<String> phones` to `String phone` ✓
    - Add `AddressResponse address` field ✓
    - **Validation**: Response DTO compiles ✓

### 8. Application Layer - Update Command Handler

Update RegisterMemberCommandHandler to create Address, EmailAddress, and PhoneNumber value objects.

- [x] 8.1 Update command handler to build Address
    - Extract address fields from RegisterMemberCommand ✓
    - Call `Address.of()` to create Address value object ✓
    - Handle validation errors and wrap in domain exception ✓
    - **Validation**: Handler builds Address correctly ✓

- [x] 8.2 Update command handler to build EmailAddress and PhoneNumber
    - Call `EmailAddress.of(command.email)` to create EmailAddress value object ✓
    - Call `PhoneNumber.of(command.phone)` to create PhoneNumber value object ✓
    - Handle validation errors ✓
    - **Validation**: Handler builds EmailAddress and PhoneNumber correctly ✓

- [x] 8.2.5 Update command handler to build guardian EmailAddress and PhoneNumber
    - If guardian present, call `EmailAddress.of(guardian.email)` and `PhoneNumber.of(guardian.phone)` ✓
    - Pass EmailAddress and PhoneNumber to GuardianInformation constructor ✓
    - Handle validation errors for guardian email/phone ✓
    - **Validation**: Handler builds GuardianInformation with validated email/phone ✓

- [x] 8.3 Update Member.create() call
    - Pass Address, EmailAddress, and PhoneNumber to Member.create() ✓
    - Remove separate email/phone parameters (were sets, now single objects) ✓
    - **Validation**: Handler creates Member with new value objects ✓

- [x] 8.4 Update command handler unit tests
    - Update test data to include AddressRequest with alpha-2 country code ✓
    - Update test data to use single email/phone strings ✓
    - Test address validation failures ✓
    - Test email/phone validation failures ✓
    - **Validation**: All command handler tests pass

### 9. Presentation Layer - Update Controller

Update MemberController to accept and return new DTO structure.

- [x] 9.1 Update POST /api/members endpoint
    - Accept RegisterMemberRequest with single email/phone and address ✓
    - Validation happens automatically via @Valid annotation ✓
    - **Validation**: Endpoint compiles and accepts new request structure ✓

- [x] 9.2 Update GET /api/members/{id} endpoint
    - Map Address to AddressResponse ✓
    - Map EmailAddress to String email ✓
    - Map PhoneNumber to String phone ✓
    - **Validation**: Endpoint returns new response structure ✓

- [x] 9.3 Update controller tests
    - Update request JSON samples with single email/phone and address (country="CZ") ✓
    - Test member creation with valid address and contacts ✓
    - Test member creation with invalid address (missing fields, invalid country code) ✓
    - Test member creation with invalid email/phone format ✓
    - Test member retrieval returns structured address and single contacts ✓
    - **Validation**: All controller tests pass

### 10. Integration Tests

Create end-to-end integration tests for new functionality.

- [x] 10.1 Write member creation integration test with address
    - POST /api/members with complete address (country="CZ") and single email/phone ✓
    - Verify member saved to database with address columns populated ✓
    - Verify GET /api/members/{id} returns same address and contacts ✓
    - **Validation**: Integration test passes ✓

- [x] 10.2 Write integration test for address validation
    - POST /api/members with invalid address (invalid country code "XX") ✓
    - Verify validation error returned ✓
    - POST /api/members with valid address (country="US") ✓
    - Verify member created successfully ✓
    - **Validation**: Integration test passes ✓

- [x] 10.3 Write integration test for contact validation
    - POST /api/members with invalid email (no @) ✓
    - Verify validation error returned ✓
    - POST /api/members with invalid phone (no + prefix) ✓
    - Verify validation error returned ✓
    - **Validation**: Integration test passes ✓

### 11. API Documentation

Update OpenAPI/HTTP examples with new request/response structure.

- [x] 11.1 Update member creation .http example
    - Add address object to request body (country="CZ") ✓
    - Update emails array to single email string ✓
    - Update phones array to single phone string ✓
    - **Validation**: Example executes successfully via ijhttp ✓

- [x] 11.2 Update member retrieval .http example
    - Verify response includes address object with alpha-2 country code ✓
    - Verify response includes single email/phone strings ✓
    - **Validation**: Example shows updated response structure ✓

- [x] 11.3 Add examples for validation errors
    - Missing address fields ✓
    - Invalid country code (not alpha-2) ✓
    - Invalid email format (no @) ✓
    - Invalid phone format (no + prefix) ✓
    - **Validation**: Examples demonstrate all validation scenarios ✓

### 12. Code Quality & Cleanup

- [x] 12.1 Run all tests
    - Execute `mvn test` ✓
    - Verify all unit tests pass (domain, application, infrastructure) ✓
    - **Validation**: Test suite passes

- [x] 12.2 Run integration tests
    - Execute `mvn verify` ✓
    - Verify all integration tests pass ✓
    - **Validation**: Integration test suite passes

- [x] 12.3 Verify code coverage
    - Check coverage for new value objects (Address, EmailAddress, PhoneNumber) ✓
    - Ensure > 90% coverage for domain layer ✓
    - **Validation**: Coverage report shows good coverage ✓

- [x] 12.4 Run application and manual test
    - Start application with `mvn spring-boot:run` ✓
    - Execute member creation .http request with address (country="CZ") and single contacts ✓
    - Verify database has correct data (check H2 console) ✓
    - Execute member retrieval .http request ✓
    - Verify response matches expected structure ✓
    - **Validation**: Manual test successful ✓

- [x] 12.5 Code review checklist
    - All new value objects are immutable (final fields, no setters) ✓
    - All value objects have proper equals/hashCode/toString ✓
    - All validation errors have clear messages ✓
    - No business logic in infrastructure layer ✓
    - DTOs are in application layer, not exposed from domain ✓
    - Country codes use ISO 3166-1 alpha-2 format consistently ✓
    - **Validation**: Code review complete ✓

### 13. Documentation

- [x] 13.1 Update CLAUDE.md if needed
    - Document Address value object structure (ISO 3166-1 alpha-2 for country) ✓
    - Document simplified contact model (single email/phone) ✓
    - Note breaking API changes ✓
    - **Validation**: CLAUDE.md updated (in project root)

- [x] 13.2 Update migration notes
    - Document breaking changes in API (arrays → single strings) ✓
    - Document address requirement with alpha-2 country codes ✓
    - Document frontend migration path ✓
    - **Validation**: Migration guide complete

## Summary

**Total tasks**: 44+ implementation tasks across 13+ sections
**Key deliverables**:

- 3 new value objects (Address, EmailAddress, PhoneNumber)
- Updated GuardianInformation to use EmailAddress and PhoneNumber value objects
- Updated database schema (no migration needed - in-memory DB only)
- Simplified contact model (single email/phone instead of sets, for both member and guardian)
- Updated Member aggregate and all layers
- Updated command handler and mapper for guardian validation
- Breaking API changes with simplified request/response
- ISO 3166-1 alpha-2 country codes (CZ, US, DE)
- E.164 format validation for both member and guardian phone numbers
- Comprehensive test coverage (28+ new tests)

**Dependencies**:

- Section 1-3.5 (Domain layer) can be done in parallel
- Section 4-6 (Infrastructure) depends on domain layer completion
- Section 7-9 (Application/Presentation) depends on infrastructure layer
- Section 10-12 (Testing) depends on all implementation completion

**Key Changes**:

- GuardianInformation now requires valid EmailAddress and PhoneNumber at construction
- All email/phone validation happens at value object level (domain layer)
- Guardian validation happens during Member creation (consistent with member email/phone requirement)
