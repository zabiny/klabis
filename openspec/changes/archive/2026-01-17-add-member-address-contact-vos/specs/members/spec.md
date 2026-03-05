## MODIFIED Requirements

### Requirement: Mandatory Personal Information

The system SHALL require the following personal information fields for all new members:

- First name (Jmeno)
- Last name (Prijmeni)
- Date of birth
- Nationality
- Gender
- **Address (full postal address with street, city, postal code, country)**

#### Scenario: Submit member with all mandatory fields including address

- **WHEN** user submits member data with all mandatory fields populated including complete address
- **THEN** validation passes for personal information
- **AND** member is created successfully
- **AND** address is stored as structured Address value object

#### Scenario: Submit member missing address

- **WHEN** user submits member data missing any address field (street, city, postalCode, or country)
- **THEN** validation fails with HTTP 400
- **AND** error response indicates which address fields are missing

#### Scenario: Submit member with invalid address

- **WHEN** user submits member data with invalid address (e.g., empty street, invalid country code)
- **THEN** validation fails with HTTP 400
- **AND** error response indicates address validation errors

## ADDED Requirements

### Requirement: Address Value Object

The system SHALL capture and validate member postal addresses as a structured Address value object with street, city,
postal code, and country fields.

**Address fields**:

- `street` - Street address including house/building number (required, max 200 characters)
- `city` - City or town name (required, max 100 characters)
- `postalCode` - Postal/ZIP code (required, max 20 characters, alphanumeric)
- `country` - Country code (required, ISO 3166-1 alpha-2 format, e.g., "CZ", "US")

**Validation rules**:

- All fields are required (no null or blank values)
- Street must not exceed 200 characters
- City must not exceed 100 characters
- Postal code must not exceed 20 characters and contain only alphanumeric characters and hyphens
- Country must be valid ISO 3166-1 alpha-2 code

#### Scenario: Create Address with valid data

- **GIVEN** street is "Hlavní 123"
- **AND** city is "Praha"
- **AND** postalCode is "11000"
- **AND** country is "CZ"
- **WHEN** Address.of() is called with these values
- **THEN** Address value object is created successfully
- **AND** all fields are populated correctly

#### Scenario: Reject Address with missing street

- **GIVEN** street is null or blank
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates street is required

#### Scenario: Reject Address with missing city

- **GIVEN** city is null or blank
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates city is required

#### Scenario: Reject Address with missing postal code

- **GIVEN** postalCode is null or blank
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates postal code is required

#### Scenario: Reject Address with invalid postal code format

- **GIVEN** postalCode contains invalid characters (spaces, special chars except hyphen)
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates postal code format is invalid

#### Scenario: Reject Address with missing country

- **GIVEN** country is null or blank
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates country is required

#### Scenario: Reject Address with invalid country code

- **GIVEN** country is not a valid ISO 3166-1 alpha-2 code (e.g., "XX", "123", "ABC")
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates country code is invalid

#### Scenario: Reject Address with oversized fields

- **GIVEN** street exceeds 200 characters OR city exceeds 100 characters OR postalCode exceeds 20 characters
- **WHEN** Address.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates which field exceeds maximum length

### Requirement: EmailAddress Value Object

The system SHALL validate email addresses using RFC 5322 basic format (must contain @ symbol and valid domain).

#### Scenario: Valid email address accepted

- **GIVEN** email value is "john@example.com"
- **WHEN** EmailAddress.of() is called
- **THEN** EmailAddress value object is created successfully

#### Scenario: Invalid email without @ rejected

- **GIVEN** email value is "johnexample.com"
- **WHEN** EmailAddress.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates email must contain @ symbol

#### Scenario: Invalid email without domain rejected

- **GIVEN** email value is "john@"
- **WHEN** EmailAddress.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates email must have domain

#### Scenario: Blank email rejected

- **GIVEN** email value is null or blank
- **WHEN** EmailAddress.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates email is required

### Requirement: PhoneNumber Value Object

The system SHALL validate phone numbers using E.164 international format (starts with +, followed by country code and
number with optional spaces).

#### Scenario: Valid E.164 phone number accepted

- **GIVEN** phone value is "+420123456789"
- **WHEN** PhoneNumber.of() is called
- **THEN** PhoneNumber value object is created successfully

#### Scenario: Valid E.164 phone with spaces accepted

- **GIVEN** phone value is "+420 123 456 789"
- **WHEN** PhoneNumber.of() is called
- **THEN** PhoneNumber value object is created successfully
- **AND** spaces are preserved in value

#### Scenario: Invalid phone without + prefix rejected

- **GIVEN** phone value is "420123456789"
- **WHEN** PhoneNumber.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates phone must start with + (E.164 format)

#### Scenario: Invalid phone with letters rejected

- **GIVEN** phone value is "+420ABC456789"
- **WHEN** PhoneNumber.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates phone can only contain digits and spaces after +

#### Scenario: Blank phone rejected

- **GIVEN** phone value is null or blank
- **WHEN** PhoneNumber.of() is called
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates phone number is required

## MODIFIED Requirements

### Requirement: Contact Information

The system SHALL require one email address and one phone number, either:

- Directly for the member, OR
- For the member's legal guardian (when member is a minor)

#### Scenario: Adult member with own contact details

- **WHEN** member is 18 years or older
- **THEN** one email and one phone number must be provided for the member
- **AND** email is stored as EmailAddress value object
- **AND** phone is stored as PhoneNumber value object

#### Scenario: Minor with guardian contact details

- **WHEN** member is under 18 years old
- **THEN** one email and one phone number must be provided for legal guardian
- **AND** guardian contact details are captured separately from member
- **AND** member email/phone may be null

#### Scenario: Missing contact information

- **WHEN** neither member nor guardian has required contact information
- **THEN** validation fails with HTTP 400
- **AND** error indicates missing email or phone requirement

### Requirement: Member Details Response Format

The member details endpoint SHALL return complete member information in a HATEOAS-compliant HAL+FORMS format with proper
ISO-8601 date serialization and structured address and contact information.

#### Scenario: Response contains all personal information with structured address

- **WHEN** a member details response is returned
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID)
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth as ISO-8601 date string (YYYY-MM-DD)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE, OTHER)
    - **`address` - Object containing street, city, postalCode, country (ISO 3166-1 alpha-2)**
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - **`email` - Single email address string**
    - **`phone` - Single phone number string**
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `active` - Boolean indicating if member is active
    - `guardian` - Guardian information object (present if member has guardian)

#### Scenario: Address serialized as structured object

- **WHEN** a member has address with street="Hlavní 123", city="Praha", postalCode="11000", country="CZ"
- **THEN** the response SHALL serialize address as:
  ```json
  "address": {
    "street": "Hlavní 123",
    "city": "Praha",
    "postalCode": "11000",
    "country": "CZ"
  }
  ```

#### Scenario: Email serialized as single string

- **WHEN** a member has email "john@example.com"
- **THEN** the response SHALL serialize email as:
  ```json
  "email": "john@example.com"
  ```

#### Scenario: Phone serialized as single string

- **WHEN** a member has phone "+420123456789"
- **THEN** the response SHALL serialize phone as:
  ```json
  "phone": "+420123456789"
  ```
