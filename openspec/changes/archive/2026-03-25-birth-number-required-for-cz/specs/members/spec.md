## MODIFIED Requirements

### Requirement: Birth Number Management

The system SHALL manage birth numbers (rodne cislo) for Czech nationals with mandatory availability, format validation, GDPR-compliant encryption, consistency validation, API exposure, and audit trail.

- **Required** when Czech nationality is selected
- Disabled/unavailable for non-Czech nationalities; changing nationality from Czech to non-Czech clears any stored birth number
- Must validate format RRMMDD/XXXX or RRMMDDXXXX
- Must encrypt birth number in database using AES-256
- Should validate consistency with date of birth and gender

Birth number format:
- **Standard format**: RRMMDD/XXXX (10 digits with slash separator)
- **Alternative format**: RRMMDDXXXX (10 digits without separator)
- **RR**: Year of birth (last 2 digits)
- **MM**: Month of birth (01-12 for males, 51-62 for females born after 1954, 21-32 for males born after 2004, 71-82 for females born after 2004)
- **DD**: Day of birth (01-31)
- **XXXX**: Sequential number

#### Scenario: Czech national must provide birth number

- **WHEN** user creates or updates a member with Czech (CZ) nationality
- **AND** birth number is not provided
- **THEN** validation fails with HTTP 400
- **AND** error message indicates birth number is required for Czech nationals

#### Scenario: Czech national provides valid birth number

- **WHEN** user creates or updates a member with Czech (CZ) nationality
- **AND** birth number is provided in valid format
- **THEN** system accepts the member data

#### Scenario: Non-Czech national cannot enter birth number

- **WHEN** user creates or updates a member with non-Czech nationality
- **THEN** birth number field is hidden and any previously entered value is cleared

#### Scenario: Changing nationality clears birth number

- **WHEN** user changes member nationality from Czech to non-Czech
- **THEN** birth number is automatically cleared

#### Scenario: Valid birth number with slash is accepted

- **WHEN** user enters birth number in format "RRMMDD/XXXX" (e.g., "901231/1234")
- **THEN** system accepts the birth number

#### Scenario: Valid birth number without slash is normalized

- **WHEN** user enters birth number in format "RRMMDDXXXX" (e.g., "9012311234")
- **THEN** system accepts the birth number and normalizes it to format with slash "RRMMDD/XXXX"

#### Scenario: Invalid birth number format is rejected

- **WHEN** user enters birth number not matching valid format (e.g., "abc123", "90123", "901231/12345")
- **THEN** validation fails with HTTP 400
- **AND** error message states "Birth number must be in format RRMMDD/XXXX or RRMMDDXXXX"

#### Scenario: Birth number with invalid date is rejected

- **WHEN** user enters birth number with invalid date components (e.g., "901332/1234" - day 32)
- **THEN** validation fails with HTTP 400
- **AND** error message states "Birth number contains invalid date"

#### Scenario: Birth number date matches member date of birth

- **WHEN** user enters birth number with date components matching member's date of birth
- **THEN** system accepts the birth number

#### Scenario: Birth number date conflicts with member date of birth

- **WHEN** user enters birth number with date NOT matching member's date of birth
- **THEN** system shows warning "Birth number date (DD.MM.RRRR) does not match member's date of birth"
- **AND** member creation/update proceeds

#### Scenario: Birth number gender matches member gender

- **WHEN** user enters birth number with month indicating gender matching member's gender
- **THEN** system accepts the birth number

#### Scenario: Birth number gender conflicts with member gender

- **WHEN** user enters birth number with month indicating gender NOT matching member's gender
- **THEN** system shows warning "Birth number indicates different gender than selected"
- **AND** member creation/update proceeds

#### Scenario: Birth number is encrypted at rest

- **WHEN** member with birth number is saved to database
- **THEN** birth number column contains encrypted value using AES-256 encryption

#### Scenario: Birth number is decrypted on retrieval

- **WHEN** member data is loaded from database
- **THEN** birth number is automatically decrypted for application use

#### Scenario: Birth number included in member detail response

- **WHEN** authorized user requests member details (GET /api/members/{id})
- **THEN** response includes birthNumber field (or null if not set)

#### Scenario: Birth number included in member registration request

- **WHEN** user creates new member (POST /api/members)
- **THEN** request accepts birthNumber field (required for CZ nationality)

#### Scenario: Birth number included in member update request

- **WHEN** user updates member (PATCH /api/members/{id})
- **THEN** request accepts optional birthNumber field

#### Scenario: Birth number access is logged

- **WHEN** user retrieves member data containing birth number
- **THEN** system creates audit log entry with: user ID, member ID, timestamp, action "VIEW_BIRTH_NUMBER"

#### Scenario: Birth number modification is logged

- **WHEN** user creates or updates birth number
- **THEN** system creates audit log entry with: user ID, member ID, timestamp, action "MODIFY_BIRTH_NUMBER"

#### Scenario: Frontend shows birth number field for Czech nationality

- **WHEN** member form displays with Czech (CZ) nationality selected
- **THEN** birth number field is visible and marked as required

#### Scenario: Frontend hides birth number field for non-Czech nationality

- **WHEN** member form displays with non-Czech nationality selected
- **THEN** birth number field is not visible

#### Scenario: Frontend hides birth number when nationality changes from Czech

- **WHEN** user changes nationality from Czech to non-Czech in member form
- **THEN** birth number field is hidden
- **AND** any entered birth number value is cleared
