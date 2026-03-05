# Members Specification Delta

## MODIFIED Requirements

### Requirement: Get Member by ID

The system SHALL provide a REST API endpoint to retrieve complete details of a specific member by their unique
identifier, including termination details if membership has been terminated.

#### Scenario: Retrieve existing active member by ID

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members/{id}
- **AND** the member with the given ID exists
- **AND** the member is active
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL include all member details:
    - Unique identifier (UUID)
    - Registration number (XXXYYSS format)
    - First name
    - Last name
    - Date of birth
    - Nationality
    - Gender
    - Address (street, city, postal code, country)
    - Rodne cislo (if Czech nationality)
    - Email addresses (all)
    - Phone numbers (all)
    - Guardian information (if applicable)
    - Chip number (if provided)
    - Bank account number (if provided)
    - Active status: true
    - Deactivation reason: null
    - Deactivated at: null
    - Deactivation note: null
    - Deactivated by: null
- **AND** the response SHALL include HATEOAS links:
    - `self` - Link to the current member resource
    - `collection` - Link to the members list
    - `edit` - Link to update member (if user has MEMBERS:UPDATE permission)
    - `terminate` - Link to terminate membership (if user has MEMBERS:UPDATE permission and member is active)

#### Scenario: Retrieve existing terminated member by ID

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members/{id}
- **AND** the member with the given ID exists
- **AND** the member is terminated (inactive)
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL include all member details:
    - Unique identifier (UUID)
    - Registration number (XXXYYSS format)
    - First name
    - Last name
    - Date of birth
    - Nationality
    - Gender
    - Address (street, city, postal code, country)
    - Rodne cislo (if Czech nationality)
    - Email addresses (all)
    - Phone numbers (all)
    - Guardian information (if applicable)
    - Chip number (if provided)
    - Bank account number (if provided)
    - Active status: false
    - Deactivation reason: one of ODHLASKA, PRESTUP, OTHER
    - Deactivated at: ISO-8601 datetime string
    - Deactivation note: text string or null
    - Deactivated by: user ID (UUID)
- **AND** the response SHALL include HATEOAS links:
    - `self` - Link to the current member resource
    - `collection` - Link to the members list
    - `edit` - Link to update member (if user has MEMBERS:UPDATE permission)
    - Termination link SHALL NOT be present (member already terminated)

### Requirement: Member Details Response Format

The member details endpoint SHALL return complete member information in a HATEOAS-compliant HAL+FORMS format with proper ISO-8601 date serialization, structured address and contact information, and termination details when applicable.

#### Scenario: Active member response contains all fields

- **WHEN** a member details response is returned for an active member
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID), also UserId due to 1:1 relationship
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth as ISO-8601 date string (YYYY-MM-DD)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE, OTHER)
    - `address` - Object containing street, city, postalCode, country (ISO 3166-1 alpha-2)
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - `email` - Single email address string
    - `phone` - Single phone number string
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `active` - Boolean true
    - `deactivationReason` - null
    - `deactivatedAt` - null
    - `deactivationNote` - null
    - `deactivatedBy` - null
    - `guardian` - Guardian information object (present if member has guardian)

#### Scenario: Terminated member response contains termination details

- **WHEN** a member details response is returned for a terminated member
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID)
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth as ISO-8601 date string (YYYY-MM-DD)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE, OTHER)
    - `address` - Object containing street, city, postalCode, country
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - `email` - Single email address string
    - `phone` - Single phone number string
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `active` - Boolean false
    - `deactivationReason` - One of: ODHLASKA, PRESTUP, EXPIRACE, OTHER
    - `deactivatedAt` - ISO-8601 datetime string (YYYY-MM-DDTHH:MM:SS)
    - `deactivationNote` - Text string or null
    - `deactivatedBy` - User ID (UUID) of the user who performed termination
    - `guardian` - Guardian information object (present if member has guardian)

### Requirement: HATEOAS Links for Member Details

The member details response SHALL include hypermedia links following HAL+FORMS specification to enable API discoverability and navigation, including conditional link for membership termination.

#### Scenario: Terminate link included for active members

- **WHEN** a user with MEMBERS:UPDATE permission views an active member
- **THEN** the response SHALL include a `terminate` link
- **AND** the link SHALL point to POST /api/members/{id}/terminate
- **AND** the link SHALL use the rel "terminate"

#### Scenario: Terminate link excluded for terminated members

- **WHEN** a user views a terminated member
- **THEN** the response SHALL NOT include a `terminate` link
- **AND** only links for available actions are included

#### Scenario: Terminate link excluded for users without permission

- **WHEN** an authenticated user without MEMBERS:UPDATE permission views a member
- **THEN** the response SHALL NOT include a `terminate` link
- **AND** only links for authorized actions are included
