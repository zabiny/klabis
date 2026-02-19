# Implementation Tasks: Birth Number and Bank Account

## 1. Setup and Dependencies

- [x] 1.1 Add Apache Commons Validator dependency to build.gradle.kts for IBAN validation
- [x] 1.2 Configure Jasypt encryption properties in application.yml (JASYPT_ENCRYPTOR_PASSWORD)

## 2. Value Objects (TDD)

### BirthNumber Value Object

- [x] 2.1 Write BirthNumberTest with test cases for format validation (RRMMDD/XXXX and RRMMDDXXXX)
- [x] 2.2 Write BirthNumberTest with test cases for date validation (invalid month, invalid day)
- [x] 2.3 Write BirthNumberTest with test cases for normalization (adding slash if missing)
- [x] 2.4 Implement BirthNumber record with validation logic
- [x] 2.5 Verify BirthNumberTest passes (green)

### BankAccountNumber Value Object

- [x] 2.6 Write BankAccountNumberTest with test cases for valid IBAN format (e.g., "CZ6508000000192000145399")
- [x] 2.7 Write BankAccountNumberTest with test cases for IBAN with spaces (normalization)
- [x] 2.8 Write BankAccountNumberTest with test cases for valid domestic format (e.g., "123456/0800")
- [x] 2.9 Write BankAccountNumberTest with test cases for domestic format with long account (e.g., "1234567890/0800")
- [x] 2.10 Write BankAccountNumberTest with test cases for invalid IBAN format
- [x] 2.11 Write BankAccountNumberTest with test cases for invalid IBAN checksum
- [x] 2.12 Write BankAccountNumberTest with test cases for invalid domestic format (wrong bank code length)
- [x] 2.13 Write BankAccountNumberTest for format auto-detection (IBAN vs DOMESTIC)
- [x] 2.14 Implement BankAccountNumber record with dual format support (IBAN + domestic)
- [x] 2.15 Implement format detection logic in BankAccountNumber
- [x] 2.16 Verify BankAccountNumberTest passes (green)

## 3. Domain Layer - Member Aggregate (TDD)

- [x] 3.1 Write MemberTest for birth number conditional validation (only for CZ nationality)
- [x] 3.2 Write MemberTest for birth number rejection when nationality is non-Czech
- [x] 3.3 Write MemberTest for bank account number with valid value object
- [x] 3.4 Write MemberTest for null bank account number (optional field)
- [x] 3.5 Add birthNumber field to Member.java (type: BirthNumber, nullable)
- [x] 3.6 Add bankAccountNumber field to Member.java (type: BankAccountNumber, nullable)
- [x] 3.7 Add getters for birthNumber and bankAccountNumber
- [x] 3.8 Update Member.createWithId() to accept birthNumber and bankAccountNumber parameters
- [x] 3.9 Implement validateBirthNumberNationality() method in Member
- [x] 3.10 Update Member.reconstruct() to include new value object fields
- [x] 3.11 Update Member.UpdateMemberDetails command record with new fields
- [x] 3.12 Update Member.handle(UpdateMemberDetails) to process new value objects
- [x] 3.13 Verify all MemberTest tests pass (green)

## 4. Database Schema

- [x] 4.1 Add birth_number column to members table in V001__initial_schema.sql (VARCHAR(255) for encrypted data)
- [x] 4.2 Add bank_account_number column to members table in V001__initial_schema.sql (VARCHAR(50))
- [x] 4.3 Add column comments explaining birth_number encryption and bank_account purpose

## 5. Persistence Layer - Encryption (TDD)

- [x] 5.1 Write StringEncryptionConverterTest for encryption/decryption
- [x] 5.2 Implement StringEncryptionConverter (AttributeConverter) using Jasypt StringEncryptor
- [x] 5.3 Configure Jasypt StringEncryptor bean in application configuration
- [x] 5.4 Verify StringEncryptionConverterTest passes
- [x] 5.5 Update MemberMemento with birthNumber (String, encrypted) and bankAccountNumber (String, plain) fields
- [x] 5.6 Add @Convert(converter = StringEncryptionConverter.class) to birthNumber field in MemberMemento
- [x] 5.7 Update MemberMemento.fromMember() to convert value objects to String (birthNumber.value(), bankAccountNumber.value())
- [x] 5.8 Update MemberMemento.toMember() to reconstruct value objects from String (BirthNumber.of(), BankAccountNumber.of())
- [x] 5.9 Handle null values when converting between String and value objects
- [x] 5.10 Add audit logging in MemberMemento.toMember() when birthNumber is accessed

## 6. Persistence Layer - Repository Tests (TDD)

- [x] 6.1 Write MemberRepositoryTest for saving member with birth number (verify encrypted in DB)
- [x] 6.2 Write MemberRepositoryTest for loading member with birth number (verify decrypted correctly)
- [x] 6.3 Write MemberRepositoryTest for saving member with bank account number
- [x] 6.4 Write MemberRepositoryTest for saving member with null birth number and bank account (backwards compatibility)
- [x] 6.5 Verify all MemberRepositoryTest integration tests pass

## 7. Application Layer - DTOs

- [x] 7.1 Add birthNumber field to RegisterMemberRequest (String, optional, with @Pattern validation)
- [x] 7.2 Add bankAccountNumber field to RegisterMemberRequest (String, optional)
- [x] 7.3 Add birthNumber field to UpdateMemberRequest (String, optional)
- [x] 7.4 Add bankAccountNumber field to UpdateMemberRequest (String, optional)
- [x] 7.5 Add birthNumber field to MemberDetailsResponse (String, optional)
- [x] 7.6 Add bankAccountNumber field to MemberDetailsResponse (String, optional)

## 8. Application Layer - Services (TDD)

- [x] 8.1 Write RegistrationServiceTest for registering member with birth number (CZ nationality)
- [x] 8.2 Write RegistrationServiceTest for rejecting member with birth number (non-Czech nationality)
- [x] 8.3 Write RegistrationServiceTest for registering member with bank account number
- [x] 8.4 Update RegistrationService.registerMember() to convert birthNumber String → BirthNumber value object
- [x] 8.5 Update RegistrationService.registerMember() to convert bankAccountNumber String → BankAccountNumber value object
- [x] 8.6 Handle null values when creating value objects (optional fields)
- [x] 8.7 Verify RegistrationServiceTest passes
- [x] 8.8 Write ManagementServiceTest for updating member with birth number
- [x] 8.9 Write ManagementServiceTest for updating member with bank account number
- [x] 8.10 Update ManagementService.updateMember() to convert String → value objects
- [x] 8.11 Verify ManagementServiceTest passes

## 9. Mapper Updates

- [x] 9.1 Update MemberMapper.toDetailsResponse() to include birthNumber and bankAccountNumber
- [x] 9.2 Convert BirthNumber value object to String (birthNumber.value()) in mapper
- [x] 9.3 Convert BankAccountNumber value object to String (bankAccountNumber.value()) in mapper
- [x] 9.4 Handle null values for optional value objects (null-safe mapping)

## 10. API Layer - E2E Tests (TDD)

- [x] 10.1 Write MemberRegistrationE2ETest for creating member with birth number and bank account (happy path)
- [x] 10.2 Write MemberRegistrationE2ETest for rejecting birth number with non-CZ nationality (business rule violation)
- [x] 10.3 Write GetMemberE2ETest for retrieving member with birth number and bank account in response
- [x] 10.4 Write UpdateMemberApiTest for updating member with new birth number and bank account
- [x] 10.5 Verify all E2E tests pass

## 11. Dropped

## 12. Test Data Updates

- [x] 12.1 Update test data SQL files (member-with-email.sql) to include null birth_number and bank_account_number
- [x] 12.2 Update test data SQL files (member-without-email.sql) to include null values
- [x] 12.3 Create test data helper methods in MemberTestDataBuilder for members with birth number
- [x] 12.4 Create test data helper methods in MemberManagementDtosTestDataBuilder for DTOs with new fields

## 13. Integration Testing

- [x] 13.1 Run full test suite (./gradlew test) and ensure all tests pass
- [x] 13.2 Test birth number encryption manually (inspect H2 database via H2 console)
- [x] 13.3 Test birth number decryption by retrieving member via API
- [x] 13.4 Test IBAN validation with various formats (with/without spaces, invalid checksum)
- [x] 13.5 Test domestic format validation (valid: "123456/0800", invalid: "123456/08")
- [x] 13.6 Test bank account format auto-detection (IBAN vs domestic)
- [x] 13.7 Test conditional validation (CZ nationality allows birth number, others reject)

## 14. Documentation

- [x] 14.1 Update backend/CLAUDE.md with Jasypt configuration instructions (if needed)
- [x] 14.2 Add comments to BirthNumber explaining format and validation rules
- [x] 14.3 Add comments to BankAccountNumber explaining dual format support (IBAN and domestic)
- [x] 14.4 Document JASYPT_ENCRYPTOR_PASSWORD environment variable requirement

## 15. Code Review and Cleanup

- [x] 15.1 Review all changes for KISS principle compliance
- [x] 15.2 Remove any unnecessary comments or dead code
- [x] 15.3 Check test coverage (>80% for all new code, 100% for domain logic)
- [x] 15.4 Run linter/formatter (if configured)

## 16. GitHub Issue Update

- [x] 16.1 Update issue #3 checklist - mark "Rodné číslo" as completed
- [x] 16.2 Update issue #3 checklist - mark "Číslo bankovního účtu" as completed
- [x] 16.3 Add comment to issue #3 with concise implementation summary
- [x] 16.4 Add label `BackendCompleted` to issue #3
