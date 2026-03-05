# Member Registration Specification

## ADDED Requirements

### Requirement: Member Creation API

The system SHALL provide a RESTful API endpoint for creating new club members with HATEOAS hypermedia controls using
HAL+FORMS format.

#### Scenario: Create member with valid data

- **WHEN** authenticated user with MEMBERS:CREATE permission submits member data via POST to /api/members
- **THEN** member is created with generated registration number
- **AND** response includes HAL+FORMS links for viewing, editing, and related actions
- **AND** HTTP 201 Created status is returned with Location header

#### Scenario: Unauthorized user attempts creation

- **WHEN** user without MEMBERS:CREATE permission attempts to create member
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes error details with problem+json media type

#### Scenario: Invalid data submission

- **WHEN** user submits incomplete or invalid member data
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field
- **AND** response includes HAL+FORMS template showing required fields

### Requirement: Mandatory Personal Information

The system SHALL require the following personal information fields for all new members:

- First name (Jmeno)
- Last name (Prijmeni)
- Date of birth
- Nationality
- Gender
- Address (full postal address)

#### Scenario: Submit member with all mandatory fields

- **WHEN** user submits member data with all mandatory fields populated
- **THEN** validation passes for personal information
- **AND** member is created successfully

#### Scenario: Submit member missing mandatory field

- **WHEN** user submits member data missing any mandatory field
- **THEN** validation fails with HTTP 400
- **AND** error response indicates which mandatory fields are missing

### Requirement: Registration Number Generation

The system SHALL automatically generate a unique registration number in format XXXYYDD where:

- XXX = club code (3-character alphanumeric, configured in application.yml)
- YY = member's birth year (last 2 digits)
- DD = sequential number for members born in the same year (2 digits, starting at 01)

#### Scenario: Generate registration number for first member with given birth year

- **WHEN** first member born in 2001 is registered
- **THEN** registration number is formatted as XXX0101 (e.g., ZBM0101 for club ZBM)

#### Scenario: Generate registration number for subsequent members with same birth year

- **WHEN** additional members born in 2001 are registered
- **THEN** registration number increments sequence (e.g., ZBM0102, ZBM0103)

#### Scenario: Different birth years have independent sequences

- **WHEN** member born in 2005 is registered after member born in 2001
- **THEN** registration number starts new sequence for birth year 2005 (e.g., ZBM0501)

#### Scenario: Registration number uniqueness

- **WHEN** registration number is generated
- **THEN** system verifies uniqueness across all members
- **AND** prevents duplicate registration numbers

### Requirement: Conditional "Rodne Cislo" Field

The system SHALL conditionally enable the "rodne cislo" (Czech ID number) field based on nationality:

- Available only when Czech nationality is selected
- Disabled/unavailable for non-Czech nationalities

#### Scenario: Czech nationality selected

- **WHEN** user selects Czech (CZ) as nationality
- **THEN** "rodne cislo" field is enabled and available for input

#### Scenario: Non-Czech nationality selected

- **WHEN** user selects any non-Czech nationality
- **THEN** "rodne cislo" field is disabled and cannot be entered
- **AND** any previously entered value is cleared

### Requirement: Contact Information

The system SHALL require at least one email address and at least one phone number, either:

- Directly for the member, OR
- For the member's legal guardian (when member is a minor)

#### Scenario: Adult member with own contact details

- **WHEN** member is 18 years or older
- **THEN** at least one email and one phone number must be provided for the member

#### Scenario: Minor with guardian contact details

- **WHEN** member is under 18 years old
- **THEN** at least one email and one phone number must be provided for legal guardian
- **AND** guardian contact details are captured separately from member

#### Scenario: Missing contact information

- **WHEN** neither member nor guardian has required contact information
- **THEN** validation fails with HTTP 400
- **AND** error indicates missing email or phone requirement

### Requirement: Optional Member Data

The system SHALL accept the following optional fields:

- Chip number (numeric only)
- Bank account number

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

### Requirement: Welcome Email

The system SHALL send a welcome email with OAuth2 account activation link upon successful member creation.

#### Scenario: Successful member creation triggers welcome email

- **WHEN** member is successfully created
- **THEN** welcome email is queued for sending asynchronously
- **AND** email contains link to OAuth2 account activation (Spring Authorization Server)
- **AND** email is sent to member's primary email address (or guardian's email for minors)

#### Scenario: Email failure does not block creation

- **WHEN** member creation succeeds but email sending fails
- **THEN** member creation is committed
- **AND** email failure is logged for retry
- **AND** response indicates member was created successfully

#### Scenario: Resend welcome email

- **WHEN** admin requests to resend welcome email for existing member
- **THEN** new welcome email is queued for sending
- **AND** previous activation links remain valid

### Requirement: HATEOAS Hypermedia Controls

The system SHALL include hypermedia links in all member-related API responses following HAL+FORMS specification:

- `self` - Link to member resource
- `edit` - Link to update member (if authorized)
- `collection` - Link to members collection
- Form templates for available actions

**Note**: Links for member deactivation/activation will be added in a separate change.

#### Scenario: Member creation response includes hypermedia links

- **WHEN** member is created successfully
- **THEN** response includes `self` link to member resource
- **AND** response includes `edit` link if user has MEMBERS:UPDATE permission
- **AND** response includes `collection` link to members list
- **AND** response includes HAL+FORMS template for future updates

#### Scenario: Unauthorized user receives limited links

- **WHEN** user without edit permission views member
- **THEN** response includes `self` link
- **AND** response excludes `edit` link
- **AND** HAL+FORMS templates reflect available actions only

### Requirement: Authorization

The system SHALL restrict member creation to users with MEMBERS:CREATE permission.

#### Scenario: Authorized user creates member

- **WHEN** authenticated user with MEMBERS:CREATE permission submits creation request
- **THEN** authorization check passes
- **AND** member creation proceeds

#### Scenario: User without permission attempts creation

- **WHEN** authenticated user without MEMBERS:CREATE permission submits creation request
- **THEN** authorization check fails
- **AND** HTTP 403 Forbidden is returned

#### Scenario: Unauthenticated request

- **WHEN** unauthenticated request is made to create member
- **THEN** HTTP 401 Unauthorized is returned
- **AND** response includes authentication challenge
