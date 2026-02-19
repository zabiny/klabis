# members Specification (Delta)

## MODIFIED Requirements

### Requirement: Conditional "Rodne Cislo" Field

The system SHALL conditionally enable the "rodne cislo" (Czech ID number) field based on nationality with complete format validation and encryption:

- Available only when Czech nationality is selected
- Disabled/unavailable for non-Czech nationalities
- **NEW**: Must validate format RRMMDD/XXXX or RRMMDDXXXX
- **NEW**: Must encrypt birth number in database using AES-256
- **NEW**: Should validate consistency with date of birth and gender

#### Scenario: Czech nationality selected

- **WHEN** user selects Czech (CZ) as nationality
- **THEN** "rodne cislo" field is enabled and available for input

#### Scenario: Non-Czech nationality selected

- **WHEN** user selects any non-Czech nationality
- **THEN** "rodne cislo" field is disabled and cannot be entered
- **AND** any previously entered value is cleared

#### Scenario: Valid birth number format is accepted

- **WHEN** user enters birth number in format "RRMMDD/XXXX" (e.g., "901231/1234")
- **THEN** system accepts the birth number

#### Scenario: Valid birth number without slash is normalized

- **WHEN** user enters birth number in format "RRMMDDXXXX" (e.g., "9012311234")
- **THEN** system accepts and normalizes to format with slash "RRMMDD/XXXX"

#### Scenario: Invalid birth number format is rejected

- **WHEN** user enters birth number not matching valid format
- **THEN** validation fails with HTTP 400
- **AND** error message states "Birth number must be in format RRMMDD/XXXX or RRMMDDXXXX"

#### Scenario: Birth number with invalid date is rejected

- **WHEN** user enters birth number with invalid date components (e.g., month 13 or day 32)
- **THEN** validation fails with HTTP 400
- **AND** error message states "Birth number contains invalid date"

#### Scenario: Birth number date conflicts with member date of birth

- **WHEN** user enters birth number with date NOT matching member's date of birth
- **THEN** system shows warning "Birth number date does not match member's date of birth"
- **AND** member creation/update proceeds

#### Scenario: Birth number is encrypted in database

- **WHEN** member with birth number is saved to database
- **THEN** birth number is encrypted using AES-256 encryption
- **AND** encrypted value is stored in birth_number column

#### Scenario: Birth number is decrypted on retrieval

- **WHEN** member data is loaded from database
- **THEN** birth number is automatically decrypted for API response

### Requirement: Optional Member Data

The system SHALL accept the following optional fields with enhanced validation:

- Chip number (numeric only)
- Bank account number **(NEW: with IBAN validation)**

#### Scenario: Submit member with optional fields

- **WHEN** user provides chip number and/or bank account
- **THEN** these values are stored with the member
- **AND** validation succeeds

#### Scenario: Submit member without optional fields

- **WHEN** user omits optional fields
- **THEN** member is created without these values
- **AND** validation succeeds

#### Scenario: Invalid chip number format

- **WHEN** user provides non-numeric chip number
- **THEN** validation fails with HTTP 400
- **AND** error indicates chip number must be numeric

#### Scenario: Valid IBAN bank account is accepted

- **WHEN** user provides bank account in valid IBAN format (e.g., "CZ6508000000192000145399")
- **THEN** bank account is accepted and stored

#### Scenario: IBAN with spaces is normalized

- **WHEN** user provides IBAN with spaces (e.g., "CZ65 0800 0000 1920 0014 5399")
- **THEN** bank account is accepted and normalized to format without spaces

#### Scenario: Valid domestic format is accepted

- **WHEN** user provides bank account in Czech domestic format (e.g., "123456/0800")
- **THEN** bank account is accepted and stored

#### Scenario: Invalid IBAN format is rejected

- **WHEN** user provides invalid IBAN format
- **THEN** validation fails with HTTP 400
- **AND** error message states "Invalid IBAN: {value}"

#### Scenario: IBAN with invalid checksum is rejected

- **WHEN** user provides IBAN with invalid checksum digits
- **THEN** validation fails with HTTP 400
- **AND** error message states "Invalid IBAN: {value}"

#### Scenario: Invalid domestic format is rejected

- **WHEN** user provides domestic format with invalid structure (e.g., "123456/08")
- **THEN** validation fails with HTTP 400
- **AND** error message states "Invalid domestic format: {value}"
