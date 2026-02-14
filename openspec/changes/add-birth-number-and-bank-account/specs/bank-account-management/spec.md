# Bank Account Management Specification

## ADDED Requirements

### Requirement: Optional bank account number field

The system SHALL allow members to have an optional bank account number for expense reimbursement purposes.

#### Scenario: Member can be created without bank account
- **WHEN** user creates new member without providing bank account number
- **THEN** member is created successfully with null bank account number

#### Scenario: Member can be created with bank account
- **WHEN** user creates new member with valid bank account number
- **THEN** member is created successfully with provided bank account number

#### Scenario: Member can update bank account number
- **WHEN** user updates existing member with new bank account number
- **THEN** member bank account number is updated

#### Scenario: Member can remove bank account number
- **WHEN** user updates existing member with null/empty bank account number
- **THEN** member bank account number is set to null

### Requirement: Bank account number format validation

The system SHALL validate bank account number format and accept both IBAN and Czech domestic format.

#### Scenario: Valid IBAN format is accepted
- **WHEN** user enters bank account number in valid IBAN format (e.g., "CZ6508000000192000145399")
- **THEN** system accepts the bank account number
- **AND** format is detected as IBAN

#### Scenario: IBAN with spaces is accepted and normalized
- **WHEN** user enters IBAN with spaces (e.g., "CZ65 0800 0000 1920 0014 5399")
- **THEN** system accepts and normalizes to format without spaces

#### Scenario: Valid domestic format is accepted
- **WHEN** user enters bank account in Czech domestic format (e.g., "123456/0800")
- **THEN** system accepts the bank account number
- **AND** format is detected as DOMESTIC

#### Scenario: Domestic format with long account number is accepted
- **WHEN** user enters domestic format with up to 10-digit account number (e.g., "1234567890/0800")
- **THEN** system accepts the bank account number

#### Scenario: Invalid IBAN format is rejected
- **WHEN** user enters invalid IBAN format (e.g., "CZXX1234", "CZ12")
- **THEN** system rejects with error message "Invalid IBAN: {value}"

#### Scenario: IBAN with invalid checksum is rejected
- **WHEN** user enters IBAN with invalid checksum digits
- **THEN** system rejects with error message "Invalid IBAN: {value}"

#### Scenario: Invalid domestic format is rejected
- **WHEN** user enters domestic format with invalid bank code (e.g., "123456/08", "123456/08000")
- **THEN** system rejects with error message "Invalid domestic format: {value}"

#### Scenario: Invalid domestic format missing slash is rejected
- **WHEN** user enters account number without slash separator (e.g., "1234560800")
- **THEN** system rejects with error message "Cannot detect account format: {value}"

### Requirement: Bank account number storage

The system SHALL store bank account numbers in plain text (non-encrypted) as they are not considered personally sensitive information under GDPR.

#### Scenario: Bank account number stored as-is
- **WHEN** member with bank account number is saved to database
- **THEN** bank account number is stored in normalized IBAN format without spaces

### Requirement: Bank account number API exposure

The system SHALL include bank account number in member API responses.

#### Scenario: Bank account included in member detail response
- **WHEN** authorized user requests member details (GET /api/members/{id})
- **THEN** response includes bankAccountNumber field (or null if not set)

#### Scenario: Bank account included in member registration request
- **WHEN** user creates new member (POST /api/members)
- **THEN** request accepts optional bankAccountNumber field

#### Scenario: Bank account included in member update request
- **WHEN** user updates member (PATCH /api/members/{id})
- **THEN** request accepts optional bankAccountNumber field

### Requirement: Bank account number usage documentation

The system SHALL provide clear indication that bank account is used for expense reimbursement.

#### Scenario: Help text explains purpose
- **WHEN** user views bank account field in UI
- **THEN** help text states "For reimbursement of travel expenses and other club-related costs"

#### Scenario: Bank account optional nature is clear
- **WHEN** user views bank account field
- **THEN** field is marked as optional (not required)
