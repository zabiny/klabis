## ADDED Requirements

### Requirement: Get Member by ID

The system SHALL provide a REST API endpoint to retrieve complete details of a specific member by their unique
identifier.

#### Scenario: Retrieve existing member by ID

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members/{id}
- **AND** the member with the given ID exists
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
    - Active status
- **AND** the response SHALL include HATEOAS links:
    - `self` - Link to the current member resource
    - `collection` - Link to the members list
    - `edit` - Link to update member (if user has MEMBERS:UPDATE permission)

#### Scenario: Member not found

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members/{id}
- **AND** no member exists with the given ID
- **THEN** the system SHALL return HTTP 404 Not Found
- **AND** the response SHALL include error details with problem+json media type
- **AND** the error message SHALL indicate that the member was not found

#### Scenario: Unauthorized access attempt

- **WHEN** an authenticated user without MEMBERS:READ permission attempts to retrieve a member
- **THEN** the system SHALL return HTTP 403 Forbidden
- **AND** the response SHALL include error details with problem+json media type

#### Scenario: Unauthenticated request

- **WHEN** an unauthenticated request is made to retrieve a member
- **THEN** the system SHALL return HTTP 401 Unauthorized
- **AND** the response SHALL include authentication challenge

### Requirement: Member Details Response Format

The member details endpoint SHALL return complete member information in a HATEOAS-compliant HAL+FORMS format.

#### Scenario: Response contains all personal information

- **WHEN** a member details response is returned
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID)
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth (ISO 8601 date format)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE, OTHER)
    - `address` - Object containing street, city, postalCode, country
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - `emails` - Array of email addresses
    - `phones` - Array of phone numbers
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `active` - Boolean indicating if member is active
    - `guardian` - Guardian information object (present if member has guardian)

#### Scenario: Guardian information included for minors

- **WHEN** a member has guardian information
- **THEN** the response SHALL include guardian object with:
    - `firstName` - Guardian's first name
    - `lastName` - Guardian's last name
    - `relationship` - Relationship to member
    - `email` - Guardian's email address
    - `phone` - Guardian's phone number

#### Scenario: Response uses HAL+FORMS media type

- **WHEN** the get member endpoint is called
- **THEN** the response Content-Type SHALL be application/prs.hal-forms+json
- **AND** the response SHALL include `_links` object for hypermedia navigation
- **AND** the response SHALL include the member data in the response body

### Requirement: HATEOAS Links for Member Details

The member details response SHALL include hypermedia links following HAL+FORMS specification to enable API
discoverability and navigation.

#### Scenario: Self link included

- **WHEN** the get member endpoint returns a response
- **THEN** the response SHALL include a `self` link pointing to /api/members/{id}
- **AND** the link SHALL use the rel "self"

#### Scenario: Collection link included

- **WHEN** the get member endpoint returns a response
- **THEN** the response SHALL include a `collection` link pointing to /api/members
- **AND** the link SHALL enable navigation back to the members list

#### Scenario: Edit link conditionally included

- **WHEN** a user with MEMBERS:UPDATE permission views a member
- **THEN** the response SHALL include an `edit` link
- **AND** the link SHALL point to the member resource for update operations

#### Scenario: Edit link excluded for unauthorized users

- **WHEN** a user without MEMBERS:UPDATE permission views a member
- **THEN** the response SHALL NOT include an `edit` link
- **AND** HAL+FORMS templates SHALL reflect available actions only
