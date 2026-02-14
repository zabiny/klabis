# Implementation Tasks: Birth Number and Bank Account

## 1. Setup and Dependencies

- [ ] 1.1 Add Apache Commons Validator dependency to build.gradle.kts for IBAN validation
- [ ] 1.2 Configure Jasypt encryption properties in application.yml (JASYPT_ENCRYPTOR_PASSWORD)

## 2. Value Objects (TDD)

### BirthNumber Value Object

- [ ] 2.1 Write BirthNumberTest with test cases for format validation (RRMMDD/XXXX and RRMMDDXXXX)
- [ ] 2.2 Write BirthNumberTest with test cases for date validation (invalid month, invalid day)
- [ ] 2.3 Write BirthNumberTest with test cases for normalization (adding slash if missing)
- [ ] 2.4 Implement BirthNumber record with validation logic
- [ ] 2.5 Verify BirthNumberTest passes (green)

### BankAccountNumber Value Object

- [ ] 2.6 Write BankAccountNumberTest with test cases for valid IBAN format (e.g., "CZ6508000000192000145399")
- [ ] 2.7 Write BankAccountNumberTest with test cases for IBAN with spaces (normalization)
- [ ] 2.8 Write BankAccountNumberTest with test cases for valid domestic format (e.g., "123456/0800")
- [ ] 2.9 Write BankAccountNumberTest with test cases for domestic format with long account (e.g., "1234567890/0800")
- [ ] 2.10 Write BankAccountNumberTest with test cases for invalid IBAN format
- [ ] 2.11 Write BankAccountNumberTest with test cases for invalid IBAN checksum
- [ ] 2.12 Write BankAccountNumberTest with test cases for invalid domestic format (wrong bank code length)
- [ ] 2.13 Write BankAccountNumberTest for format auto-detection (IBAN vs DOMESTIC)
- [ ] 2.14 Implement BankAccountNumber record with dual format support (IBAN + domestic)
- [ ] 2.15 Implement format detection logic in BankAccountNumber
- [ ] 2.16 Verify BankAccountNumberTest passes (green)

## 3. Domain Layer - Member Aggregate (TDD)

- [ ] 3.1 Write MemberTest for birth number conditional validation (only for CZ nationality)
- [ ] 3.2 Write MemberTest for birth number rejection when nationality is non-Czech
- [ ] 3.3 Write MemberTest for bank account number with valid value object
- [ ] 3.4 Write MemberTest for null bank account number (optional field)
- [ ] 3.5 Add birthNumber field to Member.java (type: BirthNumber, nullable)
- [ ] 3.6 Add bankAccountNumber field to Member.java (type: BankAccountNumber, nullable)
- [ ] 3.7 Add getters for birthNumber and bankAccountNumber
- [ ] 3.8 Update Member.createWithId() to accept birthNumber and bankAccountNumber parameters
- [ ] 3.9 Implement validateBirthNumberNationality() method in Member
- [ ] 3.10 Update Member.reconstruct() to include new value object fields
- [ ] 3.11 Update Member.UpdateMemberDetails command record with new fields
- [ ] 3.12 Update Member.handle(UpdateMemberDetails) to process new value objects
- [ ] 3.13 Verify all MemberTest tests pass (green)

## 4. Database Schema

- [ ] 4.1 Add birth_number column to members table in V001__initial_schema.sql (VARCHAR(255) for encrypted data)
- [ ] 4.2 Add bank_account_number column to members table in V001__initial_schema.sql (VARCHAR(50))
- [ ] 4.3 Add column comments explaining birth_number encryption and bank_account purpose

## 5. Persistence Layer - Encryption (TDD)

- [ ] 5.1 Write StringEncryptionConverterTest for encryption/decryption
- [ ] 5.2 Implement StringEncryptionConverter (AttributeConverter) using Jasypt StringEncryptor
- [ ] 5.3 Configure Jasypt StringEncryptor bean in application configuration
- [ ] 5.4 Verify StringEncryptionConverterTest passes
- [ ] 5.5 Update MemberMemento with birthNumber (String, encrypted) and bankAccountNumber (String, plain) fields
- [ ] 5.6 Add @Convert(converter = StringEncryptionConverter.class) to birthNumber field in MemberMemento
- [ ] 5.7 Update MemberMemento.fromMember() to convert value objects to String (birthNumber.value(), bankAccountNumber.value())
- [ ] 5.8 Update MemberMemento.toMember() to reconstruct value objects from String (BirthNumber.of(), BankAccountNumber.of())
- [ ] 5.9 Handle null values when converting between String and value objects
- [ ] 5.10 Add audit logging in MemberMemento.toMember() when birthNumber is accessed

## 6. Persistence Layer - Repository Tests (TDD)

- [ ] 6.1 Write MemberRepositoryTest for saving member with birth number (verify encrypted in DB)
- [ ] 6.2 Write MemberRepositoryTest for loading member with birth number (verify decrypted correctly)
- [ ] 6.3 Write MemberRepositoryTest for saving member with bank account number
- [ ] 6.4 Write MemberRepositoryTest for saving member with null birth number and bank account (backwards compatibility)
- [ ] 6.5 Verify all MemberRepositoryTest integration tests pass

## 7. Application Layer - DTOs

- [ ] 7.1 Add birthNumber field to RegisterMemberRequest (String, optional, with @Pattern validation)
- [ ] 7.2 Add bankAccountNumber field to RegisterMemberRequest (String, optional)
- [ ] 7.3 Add birthNumber field to UpdateMemberRequest (String, optional)
- [ ] 7.4 Add bankAccountNumber field to UpdateMemberRequest (String, optional)
- [ ] 7.5 Add birthNumber field to MemberDetailsResponse (String, optional)
- [ ] 7.6 Add bankAccountNumber field to MemberDetailsResponse (String, optional)

## 8. Application Layer - Services (TDD)

- [ ] 8.1 Write RegistrationServiceTest for registering member with birth number (CZ nationality)
- [ ] 8.2 Write RegistrationServiceTest for rejecting member with birth number (non-CZ nationality)
- [ ] 8.3 Write RegistrationServiceTest for registering member with bank account number
- [ ] 8.4 Update RegistrationService.registerMember() to convert birthNumber String → BirthNumber value object
- [ ] 8.5 Update RegistrationService.registerMember() to convert bankAccountNumber String → BankAccountNumber value object
- [ ] 8.6 Handle null values when creating value objects (optional fields)
- [ ] 8.7 Verify RegistrationServiceTest passes
- [ ] 8.8 Write ManagementServiceTest for updating member with birth number
- [ ] 8.9 Write ManagementServiceTest for updating member with bank account number
- [ ] 8.10 Update ManagementService.updateMember() to convert String → value objects
- [ ] 8.11 Verify ManagementServiceTest passes

## 9. Mapper Updates

- [ ] 9.1 Update MemberMapper.toDetailsResponse() to include birthNumber and bankAccountNumber
- [ ] 9.2 Convert BirthNumber value object to String (birthNumber.value()) in mapper
- [ ] 9.3 Convert BankAccountNumber value object to String (bankAccountNumber.value()) in mapper
- [ ] 9.4 Handle null values for optional value objects (null-safe mapping)

## 10. API Layer - E2E Tests (TDD)

- [ ] 10.1 Write MemberRegistrationE2ETest for creating member with birth number and bank account (happy path)
- [ ] 10.2 Write MemberRegistrationE2ETest for rejecting birth number with non-CZ nationality (business rule violation)
- [ ] 10.3 Write GetMemberE2ETest for retrieving member with birth number and bank account in response
- [ ] 10.4 Write UpdateMemberApiTest for updating member with new birth number and bank account
- [ ] 10.5 Verify all E2E tests pass

## 11. Dropped

## 12. Test Data Updates

- [ ] 12.1 Update test data SQL files (member-with-email.sql) to include null birth_number and bank_account_number
- [ ] 12.2 Update test data SQL files (member-without-email.sql) to include null values
- [ ] 12.3 Create test data helper methods in MemberTestDataBuilder for members with birth number
- [ ] 12.4 Create test data helper methods in MemberManagementDtosTestDataBuilder for DTOs with new fields

## 13. Integration Testing

- [ ] 13.1 Run full test suite (./gradlew test) and ensure all tests pass
- [ ] 13.2 Test birth number encryption manually (inspect H2 database via H2 console)
- [ ] 13.3 Test birth number decryption by retrieving member via API
- [ ] 13.4 Test IBAN validation with various formats (with/without spaces, invalid checksum)
- [ ] 13.5 Test domestic format validation (valid: "123456/0800", invalid: "123456/08")
- [ ] 13.6 Test bank account format auto-detection (IBAN vs domestic)
- [ ] 13.7 Test conditional validation (CZ nationality allows birth number, others reject)

## 14. Documentation

- [ ] 14.1 Update backend/CLAUDE.md with Jasypt configuration instructions (if needed)
- [ ] 14.2 Add comments to BirthNumber explaining format and validation rules
- [ ] 14.3 Add comments to BankAccountNumber explaining dual format support (IBAN and domestic)
- [ ] 14.4 Document JASYPT_ENCRYPTOR_PASSWORD environment variable requirement

## 15. Code Review and Cleanup

- [ ] 15.1 Review all changes for KISS principle compliance
- [ ] 15.2 Remove any unnecessary comments or dead code
- [ ] 15.3 Check test coverage (>80% for all new code, 100% for domain logic)
- [ ] 15.4 Run linter/formatter (if configured)

## 16. GitHub Issue Update

- [ ] 16.1 Update issue #3 checklist - mark "Rodné číslo" as completed
- [ ] 16.2 Update issue #3 checklist - mark "Číslo bankovního účtu" as completed
- [ ] 16.3 Add comment to issue #3 with concise implementation summary
- [ ] 16.4 Add label `BackendCompleted` to issue #3
