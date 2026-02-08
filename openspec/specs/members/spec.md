# members Specification

## Purpose

This specification defines requirements for the club member registration and management system. It encompasses the
complete member lifecycle including registration with mandatory and conditional personal information, registration
number generation (ZBM format), contact details management, and secure data handling for sensitive information like
birth numbers and contact data.

## Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a User entity first, then creating a Member entity that uses
the User's UserId.

#### Scenario: Member registration creates User then Member

- **WHEN** authenticated user with MEMBERS:CREATE permission submits member data via POST /api/members
- **THEN** the system creates a User entity with generated UserId
- **AND** the system creates a Member entity with the same UserId
- **AND** member is created with generated registration number
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

#### Scenario: User creation failure prevents member creation

- **WHEN** User creation fails during member registration
- **THEN** Member creation is not attempted
- **AND** transaction is rolled back
- **AND** error response indicates the failure reason

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

- XXX = club code (3-character alphanumeric, configured by system administrator)
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

The system SHALL require one email address and one phone number, either:

- Directly for the member, OR
- For the member's legal guardian (when member is a minor)

#### Scenario: Adult member with own contact details

- **WHEN** member is 18 years or older
- **THEN** one email and one phone number must be provided for the member
- **AND** email is validated and stored
- **AND** phone is validated and stored

#### Scenario: Minor with guardian contact details

- **WHEN** member is under 18 years old
- **THEN** one email and one phone number must be provided for legal guardian
- **AND** guardian contact details are captured separately from member
- **AND** member email/phone may be null

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

The system SHALL send a welcome email with account activation link upon successful member creation.

#### Scenario: Successful member creation triggers welcome email

- **WHEN** member is successfully created
- **THEN** welcome email sending is initiated
- **AND** email contains link to account activation
- **AND** email is sent to member's primary email address (or guardian's email for minors)

#### Scenario: Email failure does not block creation

- **WHEN** member creation succeeds but email sending fails
- **THEN** member creation is committed
- **AND** email failure is logged for retry
- **AND** response indicates member was created successfully

#### Scenario: Resend welcome email

- **WHEN** admin requests to resend welcome email for existing member
- **THEN** new welcome email sending is initiated
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

### Requirement: List All Members

The system SHALL provide an API endpoint to retrieve a paginated and sorted list of all registered members with summary
information.

#### Scenario: Retrieve paginated list with default parameters

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members without query
  parameters
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain the first page of member summaries (page 0)
- **AND** the page size SHALL be 10 items by default
- **AND** the members SHALL be sorted by lastName ascending, then firstName ascending by default
- **AND** the response SHALL include page metadata (size, totalElements, totalPages, number)
- **AND** the response SHALL include HATEOAS pagination links (self, first, last, next if applicable)

#### Scenario: Retrieve specific page of results

- **WHEN** an authenticated user makes a GET request to /api/members?page=2&size=20
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain page 2 (zero-based index) with 20 items per page
- **AND** the response SHALL include page metadata reflecting the requested page and size
- **AND** the response SHALL include pagination links adjusted for the requested page

#### Scenario: Sort members by firstName ascending

- **WHEN** an authenticated user makes a GET request to /api/members?sort=firstName,asc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted by firstName in ascending (A-Z) order
- **AND** the response SHALL include members in the correct sorted order

#### Scenario: Sort members by firstName descending

- **WHEN** an authenticated user makes a GET request to /api/members?sort=firstName,desc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted by firstName in descending (Z-A) order

#### Scenario: Sort members by lastName ascending

- **WHEN** an authenticated user makes a GET request to /api/members?sort=lastName,asc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted by lastName in ascending (A-Z) order

#### Scenario: Sort members by lastName descending

- **WHEN** an authenticated user makes a GET request to /api/members?sort=lastName,desc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted by lastName in descending (Z-A) order

#### Scenario: Sort members by registrationNumber ascending

- **WHEN** an authenticated user makes a GET request to /api/members?sort=registrationNumber,asc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted by registrationNumber in ascending order

#### Scenario: Sort members by registrationNumber descending

- **WHEN** an authenticated user makes a GET request to /api/members?sort=registrationNumber,desc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted by registrationNumber in descending order

#### Scenario: Multi-field sorting

- **WHEN** an authenticated user makes a GET request to /api/members?sort=lastName,asc&sort=firstName,asc
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the members SHALL be sorted first by lastName ascending
- **AND** members with the same lastName SHALL be sorted by firstName ascending

#### Scenario: Invalid sort field

- **WHEN** an authenticated user makes a GET request to /api/members?sort=email,asc
- **THEN** the system SHALL return HTTP 400 Bad Request
- **AND** the response SHALL indicate that the sort field is invalid
- **AND** the response SHALL list the allowed sort fields (firstName, lastName, registrationNumber)

#### Scenario: Invalid page number

- **WHEN** an authenticated user makes a GET request to /api/members?page=-1
- **THEN** the system SHALL return HTTP 400 Bad Request
- **AND** the response SHALL indicate that the page number must be non-negative

#### Scenario: Page size exceeds maximum

- **WHEN** an authenticated user makes a GET request to /api/members?size=200
- **THEN** the system SHALL return HTTP 400 Bad Request
- **AND** the response SHALL indicate that the page size exceeds the maximum allowed (100)

#### Scenario: Empty page beyond available data

- **WHEN** an authenticated user makes a GET request to /api/members?page=100
- **AND** only 3 pages of data exist (30 total members with default page size 10)
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain an empty collection
- **AND** the page metadata SHALL reflect the requested page number
- **AND** pagination links SHALL still be included (first, last, self)

#### Scenario: Single page of results

- **WHEN** an authenticated user makes a GET request to /api/members
- **AND** only 5 members exist in the system (less than page size)
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain all 5 members
- **AND** the page metadata SHALL show totalPages as 1
- **AND** the response SHALL NOT include next or prev links (only self, first, last)

### Requirement: Member Summary Response Format

The member list endpoint SHALL return summary information for each member in a consistent, HATEOAS-compliant format.

#### Scenario: Response contains required fields

- **WHEN** a member summary is returned in the API response
- **THEN** the summary SHALL include the member's firstName as a string
- **AND** the summary SHALL include the member's lastName as a string
- **AND** the summary SHALL include the member's registrationNumber as a string in format XXXYYSS (club code, birth
  year, sequence)

#### Scenario: Response uses HAL+FORMS media type

- **WHEN** the list members endpoint is called
- **THEN** the response Content-Type SHALL be application/prs.hal-forms+json
- **AND** the response SHALL follow HAL+FORMS specification for collections
- **AND** the response SHALL include _embedded object containing member summaries
- **AND** the response SHALL include _links object for hypermedia navigation

### Requirement: HATEOAS Links for Member List

The member list response SHALL include hypermedia links to enable API discoverability and navigation.

#### Scenario: Self link included

- **WHEN** the list members endpoint returns a response
- **THEN** the response SHALL include a self link pointing to /api/members
- **AND** the self link SHALL use the rel "self"

#### Scenario: Individual member links included

- **WHEN** the list members endpoint returns member summaries
- **THEN** each member summary SHALL include a self link pointing to /api/members/{memberId}
- **AND** the link SHALL enable navigation to the individual member resource

### Requirement: Pagination Query Parameters

The member list endpoint SHALL accept query parameters to control pagination behavior.

#### Scenario: Page parameter accepted

- **WHEN** the GET /api/members endpoint is called with a page query parameter
- **THEN** the system SHALL accept the page parameter as a zero-based page index
- **AND** the page parameter SHALL be optional (defaults to 0)
- **AND** negative page numbers SHALL be rejected with HTTP 400

#### Scenario: Size parameter accepted

- **WHEN** the GET /api/members endpoint is called with a size query parameter
- **THEN** the system SHALL accept the size parameter as the number of items per page
- **AND** the size parameter SHALL be optional (defaults to 10)
- **AND** the size parameter SHALL enforce a maximum value of 100
- **AND** size values greater than 100 SHALL be rejected with HTTP 400
- **AND** zero or negative size values SHALL be rejected with HTTP 400

### Requirement: Sorting Query Parameters

The member list endpoint SHALL accept query parameters to control sorting behavior.

#### Scenario: Sort parameter accepted

- **WHEN** the GET /api/members endpoint is called with a sort query parameter
- **THEN** the system SHALL accept the sort parameter in format "field,direction"
- **AND** the sort parameter SHALL be optional (defaults to "lastName,asc")
- **AND** multiple sort parameters SHALL be supported for multi-field sorting
- **AND** sort parameters SHALL be applied in the order specified

#### Scenario: Allowed sort fields validated

- **WHEN** the GET /api/members endpoint is called with a sort parameter
- **THEN** the system SHALL only allow sorting by: firstName, lastName, registrationNumber
- **AND** any other field names SHALL be rejected with HTTP 400 Bad Request
- **AND** the error message SHALL list the allowed sort fields

#### Scenario: Sort direction validated

- **WHEN** the GET /api/members endpoint is called with a sort parameter
- **THEN** the system SHALL only allow directions: asc, desc
- **AND** direction is case-insensitive (ASC, asc, Asc all accepted)
- **AND** invalid directions SHALL be rejected with HTTP 400 Bad Request

### Requirement: Paginated Response Format

The member list endpoint SHALL return pagination metadata and navigation links in the response.

#### Scenario: Page metadata included

- **WHEN** the list members endpoint returns a paginated response
- **THEN** the response SHALL include a "page" object with the following fields:
    - size: number of items per page
    - totalElements: total number of members across all pages
    - totalPages: total number of pages available
    - number: current page number (zero-based)

#### Scenario: Pagination links included

- **WHEN** the list members endpoint returns a paginated response
- **THEN** the response _links object SHALL include:
    - self: link to current page with current query parameters
    - first: link to first page (page=0)
    - last: link to last page
- **AND** if not on the first page, the response SHALL include:
    - prev: link to previous page
- **AND** if not on the last page, the response SHALL include:
    - next: link to next page
- **AND** all pagination links SHALL preserve the sort and size query parameters

#### Scenario: Empty collection with pagination

- **WHEN** the list members endpoint returns an empty result set
- **THEN** the response SHALL include page metadata with totalElements=0 and totalPages=0
- **AND** the response SHALL include _embedded.members as an empty array
- **AND** the response SHALL include pagination links (self, first, last)

### Requirement: HAL+FORMS Response Format with Pagination

The paginated member list endpoint SHALL return responses in HAL+FORMS format with pagination support.

#### Scenario: Response uses PagedModel structure

- **WHEN** the list members endpoint is called with pagination
- **THEN** the response Content-Type SHALL be application/prs.hal-forms+json
- **AND** the response SHALL follow HAL+FORMS specification for paged collections
- **AND** the response SHALL include _embedded object containing member summaries
- **AND** the response SHALL include _links object for hypermedia navigation
- **AND** the response SHALL include page object with pagination metadata
- **AND** each member summary SHALL still include individual self links

### Requirement: Backward Compatibility

The enhanced member list endpoint SHALL maintain backward compatibility with clients not using pagination.

#### Scenario: Default pagination transparent to legacy clients

- **WHEN** an existing client makes a GET request to /api/members without pagination parameters
- **THEN** the system SHALL return HTTP 200 OK with paginated response
- **AND** the response SHALL include the first page of results (page 0, size 10)
- **AND** the response structure SHALL remain compatible (_embedded.members array)
- **AND** clients not parsing pagination metadata SHALL still receive valid member data

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

### Requirement: ISO-8601 Date and DateTime Serialization

All API endpoints SHALL serialize date and datetime fields using ISO-8601 format to ensure consistent and
standards-compliant JSON responses.

This requirement applies to ALL date and datetime fields across the entire API.

#### Scenario: Date fields serialize as ISO-8601 date strings

- **WHEN** any API endpoint returns a date field (e.g., member's dateOfBirth, event date)
- **THEN** the field SHALL be serialized as an ISO-8601 date string in format `YYYY-MM-DD`
- **EXAMPLE**: January 15, 1990 SHALL be serialized as `"1990-01-15"`

#### Scenario: Date fields handle edge cases (leap years, month boundaries)

- **WHEN** a date field represents February 29, 2000
- **THEN** the field SHALL be serialized as `"2000-02-29"`
- **AND** the format SHALL be valid ISO-8601

#### Scenario: DateTime fields serialize as ISO-8601 datetime strings

- **WHEN** any API endpoint returns a datetime field (e.g., event registration deadline, payment timestamp)
- **THEN** the field SHALL be serialized as an ISO-8601 datetime string in format `YYYY-MM-DDTHH:MM:SS`
- **EXAMPLE**: May 15, 1990 at 14:30:00 SHALL be serialized as `"1990-05-15T14:30:00"`

#### Scenario: DateTime fields with timezone offset include offset

- **WHEN** any API endpoint returns a datetime field with timezone offset
- **THEN** the field SHALL be serialized as ISO-8601 with offset (e.g., `"1990-05-15T14:30:00+02:00"`)

#### Scenario: All date/time fields across all endpoints use consistent format

- **WHEN** multiple endpoints return date/time fields
- **THEN** all fields SHALL use ISO-8601 format consistently
- **AND** clients SHALL be able to parse all date/time values using standard ISO-8601 parsers

### Requirement: Address Value Object

The system SHALL capture and validate member postal addresses as a structured address with street, city, postal code,
and country fields.

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
- **WHEN** an address is created with these values
- **THEN** the address is created successfully
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
- **WHEN** phone number is created
- **THEN** IllegalArgumentException is thrown
- **AND** error message indicates phone can only contain digits and spaces after +

#### Scenario: Blank phone rejected

- **GIVEN** phone value is null or blank
- **WHEN** phone number is created
- **THEN** validation fails with error
- **AND** error message indicates phone number is required

### Requirement: Member Update API

The system SHALL provide a RESTful API endpoint for updating member information using HTTP PATCH method with HATEOAS
hypermedia controls. The endpoint shall support both member self-edit and administrator editing through role-based field
access control.

#### Scenario: Member updates their own information successfully

- **WHEN** authenticated member submits PATCH request to /api/members/{id}
- **AND** the {id} matches the authenticated user's member ID (OAuth2 subject match)
- **AND** request contains valid updates to member-editable fields
- **THEN** member information is updated
- **AND** HTTP 200 OK status is returned
- **AND** response includes updated member representation
- **AND** response includes HAL+FORMS links (self, edit, collection)

#### Scenario: Admin updates member information successfully

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request to /api/members/{id}
- **AND** request contains valid updates to any editable field (including admin-only fields)
- **THEN** member information is updated
- **AND** HTTP 200 OK status is returned
- **AND** response includes updated member representation
- **AND** response includes HAL+FORMS links (self, edit, collection)

#### Scenario: Member attempts to edit another member's information without admin permission

- **WHEN** authenticated member submits PATCH request to /api/members/{otherId}
- **AND** the {otherId} does NOT match the authenticated user's member ID
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** HTTP 403 Forbidden is returned
- **AND** error response indicates members can only edit their own information or requires admin permission
- **AND** no changes are made to the target member

#### Scenario: Unauthenticated edit attempt

- **WHEN** unauthenticated request attempts to PATCH /api/members/{id}
- **THEN** HTTP 401 Unauthorized is returned
- **AND** response includes authentication challenge

#### Scenario: Invalid data submitted for update

- **WHEN** authenticated member submits PATCH request with invalid data
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field
- **AND** response includes HAL+FORMS template showing correct format
- **AND** no changes are made to the member

### Requirement: Update Authorization

The system SHALL implement dual authorization model for member updates based on user roles and permissions.

#### Scenario: Member self-edit authorization (OAuth2 subject match)

- **WHEN** authenticated user's OAuth2 subject matches the requested member ID
- **THEN** authorization check passes for self-edit
- **AND** user can edit only member-editable fields
- **AND** update operation proceeds

#### Scenario: Admin edit authorization (MEMBERS:UPDATE permission)

- **WHEN** authenticated user has MEMBERS:UPDATE permission
- **THEN** authorization check passes for admin edit
- **AND** user can edit any member (any ID)
- **AND** user can edit both member-editable and admin-only fields
- **AND** update operation proceeds

#### Scenario: Member without admin permission attempts to edit other member

- **WHEN** authenticated user's OAuth2 subject does not match the requested member ID
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** authorization check fails
- **AND** HTTP 403 Forbidden is returned
- **AND** error message indicates insufficient permissions

#### Scenario: Admin can edit any member including themselves

- **WHEN** authenticated user has MEMBERS:UPDATE permission
- **AND** the user's own member ID matches the requested member ID
- **THEN** authorization check passes
- **AND** user can edit admin-only fields on their own record
- **AND** update operation proceeds

### Requirement: Member-Editable Fields

The system SHALL allow members to update the following fields on their own record:

- Chip number (numeric only)
- Nationality (required)
- Address (street, city, postalCode, country - all required)
- Member email address
- Member phone number
- Bank account number
- Legal guardian email
- Legal guardian phone number
- IdentityCard value object (card number, validity date)
- DrivingLicenseGroup enum (B, BE, C, D, etc.)
- MedicalCourse value object (completion date, optional validity date)
- TrainerLicense value object (license number, validity date)
- Dietary restrictions text field (optional, max 500 characters)

#### Scenario: Member updates subset of fields

- **WHEN** member submits PATCH request with only some member-editable fields
- **THEN** only provided fields are updated
- **AND** non-provided fields remain unchanged
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Member attempts to update admin-only fields

- **WHEN** member submits PATCH request attempting to modify firstName, lastName, or dateOfBirth
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** these fields are ignored (not modified)
- **AND** only member-editable fields are processed
- **AND** HTTP 200 OK is returned (if other valid fields present)

### Requirement: Admin-Only Fields

The system SHALL allow users with MEMBERS:UPDATE permission to update the following admin-only fields on any member
record:

- First name (required)
- Last name (required)
- Date of birth (required)

#### Scenario: Admin updates firstName on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName field
- **THEN** firstName is updated
- **AND** validation ensures firstName is not blank
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates lastName on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with lastName field
- **THEN** lastName is updated
- **AND** validation ensures lastName is not blank
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates dateOfBirth on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with dateOfBirth field
- **THEN** dateOfBirth is updated
- **AND** validation ensures dateOfBirth is valid ISO-8601 date format
- **AND** validation ensures dateOfBirth is not in the future
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Admin updates all admin-only fields

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, and
  dateOfBirth
- **THEN** all three fields are updated
- **AND** all validation rules are enforced
- **AND** HTTP 200 OK is returned with updated member representation

#### Scenario: Non-admin attempts to update admin-only fields

- **WHEN** authenticated user without MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, or
  dateOfBirth
- **THEN** these fields are ignored (not modified)
- **AND** only member-editable fields are processed (if any)
- **AND** HTTP 200 OK is returned (if other valid fields present)

### Requirement: Contact Information Validation

The system SHALL require at least one email address and one phone number to be present after update, either from the
member or their legal guardian.

#### Scenario: Update removes all email addresses

- **WHEN** member submits update that removes all email addresses (both member and guardian)
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one email is required
- **AND** no changes are saved

#### Scenario: Update removes all phone numbers

- **WHEN** member submits update that removes all phone numbers (both member and guardian)
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one phone number is required
- **AND** no changes are saved

#### Scenario: Valid contact update

- **WHEN** member updates their email or phone while maintaining at least one of each
- **THEN** update succeeds
- **AND** new contact information is saved
- **AND** HTTP 200 OK is returned

### Requirement: Nationality and Rodne Cislo Validation

The system SHALL enforce conditional Rodne Cislo field based on nationality during member updates.

#### Scenario: Change to Czech nationality enables Rodne Cislo

- **WHEN** member updates nationality to Czech (CZ)
- **THEN** Rodne Cislo field becomes available for input
- **AND** member may provide Rodne Cislo value
- **AND** validation enforces Rodne Cislo format if provided

#### Scenario: Change from Czech to non-Czech nationality

- **WHEN** member updates nationality from Czech to non-Czech
- **THEN** Rodne Cislo field is cleared if previously present
- **AND** Rodne Cislo becomes unavailable for input
- **AND** update succeeds without Rodne Cislo value

#### Scenario: Rodne Cislo format validation

- **WHEN** member with Czech nationality provides invalid Rodne Cislo format
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates Rodne Cislo format is invalid
- **AND** no changes are saved

### Requirement: Address Update Validation

The system SHALL require all address fields (street, city, postalCode, country) to be provided when updating address.

#### Scenario: Complete address update

- **WHEN** member submits update with all address fields (street, city, postalCode, country)
- **THEN** address is updated
- **AND** validation ensures all fields are present
- **AND** country code is validated as ISO 3166-1 alpha-2

#### Scenario: Partial address update

- **WHEN** member submits update with only some address fields
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates all address fields are required
- **AND** no changes are saved

### Requirement: Chip Number Format Validation

The system SHALL validate that chip number contains only numeric characters when provided.

#### Scenario: Valid chip number

- **WHEN** member provides chip number with numeric characters only
- **THEN** chip number is accepted and saved
- **AND** update succeeds

#### Scenario: Invalid chip number format

- **WHEN** member provides chip number with non-numeric characters
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates chip number must be numeric
- **AND** no changes are saved

### Requirement: New Member Value Objects

The system SHALL support the following value objects and fields for member information:

- **IdentityCard** - ID card number and validity date
- **MedicalCourse** - Medical course completion and optional validity date
- **TrainerLicense** - Trainer license number and validity date
- **DrivingLicenseGroup** - Driving license categories (B, BE, C, D, etc.)
- **DietaryRestrictions** - Text field for dietary restrictions (used for food ordering at accommodation)

#### Scenario: Add IdentityCard

- **GIVEN** IdentityCard contains cardNumber and validityDate
- **WHEN** member provides IdentityCard with valid data
- **THEN** IdentityCard is created and stored
- **AND** cardNumber is stored as string (not blank, max 50 characters)
- **AND** validityDate is stored as date in ISO-8601 format
- **AND** validation ensures validityDate is not in the past
- **AND** update succeeds

#### Scenario: Reject IdentityCard with missing card number

- **GIVEN** IdentityCard.cardNumber is null or blank
- **WHEN** member attempts to add IdentityCard
- **THEN** validation fails with HTTP 400
- **AND** error message indicates card number is required

#### Scenario: Reject IdentityCard with expired validity

- **GIVEN** IdentityCard.validityDate is in the past
- **WHEN** member attempts to add IdentityCard
- **THEN** validation fails with HTTP 400
- **AND** error message indicates ID card is expired

#### Scenario: Add MedicalCourse

- **GIVEN** MedicalCourse contains completionDate and optional validityDate
- **WHEN** member provides MedicalCourse with valid data
- **THEN** MedicalCourse is created and stored
- **AND** completionDate is stored as date (required)
- **AND** validityDate is stored as optional date
- **AND** validation ensures validityDate is after completionDate if provided
- **AND** both dates are validated as ISO-8601 format
- **AND** update succeeds

#### Scenario: Add MedicalCourse without validity date

- **GIVEN** MedicalCourse contains only completionDate
- **WHEN** member provides MedicalCourse without validityDate
- **THEN** MedicalCourse is created and stored successfully
- **AND** validityDate remains null (course does not expire)

#### Scenario: Reject MedicalCourse with validity before completion

- **GIVEN** MedicalCourse.validityDate is before completionDate
- **WHEN** member attempts to add MedicalCourse
- **THEN** validation fails with HTTP 400
- **AND** error message indicates validity must be after completion date

#### Scenario: Add TrainerLicense

- **GIVEN** TrainerLicense contains licenseNumber and validityDate
- **WHEN** member provides TrainerLicense with valid data
- **THEN** TrainerLicense is created and stored
- **AND** licenseNumber is stored as string (not blank, max 50 characters)
- **AND** validityDate is stored as date in ISO-8601 format
- **AND** validation ensures validityDate is not in the past
- **AND** update succeeds

#### Scenario: Reject TrainerLicense with missing license number

- **GIVEN** TrainerLicense.licenseNumber is null or blank
- **WHEN** member attempts to add TrainerLicense
- **THEN** validation fails with HTTP 400
- **AND** error message indicates license number is required

#### Scenario: Reject TrainerLicense with expired validity

- **GIVEN** TrainerLicense.validityDate is in the past
- **WHEN** member attempts to add TrainerLicense
- **THEN** validation fails with HTTP 400
- **AND** error message indicates license is expired

#### Scenario: Set DrivingLicenseGroup

- **WHEN** member provides drivingLicenseGroup value
- **THEN** driving license group is saved
- **AND** validation ensures value is from allowed set (B, BE, C, C1, D, D1, etc.)
- **AND** update succeeds

#### Scenario: Reject invalid DrivingLicenseGroup

- **WHEN** member provides drivingLicenseGroup not in allowed set
- **THEN** validation fails with HTTP 400
- **AND** error message lists valid driving license groups

#### Scenario: Set dietary restrictions

- **WHEN** member provides dietaryRestrictions text field
- **THEN** dietary restrictions are saved as text string
- **AND** field can contain any text (optional, max 500 characters)
- **AND** update succeeds

#### Scenario: Clear dietary restrictions

- **WHEN** member provides dietaryRestrictions as null or empty string
- **THEN** dietary restrictions are cleared
- **AND** field is set to null
- **AND** update succeeds

### Requirement: PATCH Request Semantics

The system SHALL support HTTP PATCH semantics where only fields present in the request body are updated.

#### Scenario: Partial update with single field

- **WHEN** member submits PATCH request with only chipNumber field
- **THEN** only chipNumber is updated
- **AND** all other fields remain unchanged
- **AND** response includes complete updated member representation

#### Scenario: Partial update with multiple fields

- **WHEN** member submits PATCH request with chipNumber and address fields
- **THEN** only chipNumber and address are updated
- **AND** all other fields remain unchanged
- **AND** response includes complete updated member representation

#### Scenario: Empty request body

- **WHEN** member submits PATCH request with empty request body
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates at least one field must be provided for update

### Requirement: Member Update Response Format

The member update endpoint SHALL return complete updated member information in HATEOAS-compliant HAL+FORMS format.

#### Scenario: Response includes updated member data

- **WHEN** member information is successfully updated via PATCH
- **THEN** response Content-Type is application/prs.hal-forms+json
- **AND** response includes all member fields with updated values
- **AND** response includes _links object with hypermedia controls
- **AND** response includes _templates object for further actions

#### Scenario: Response includes hypermedia links

- **WHEN** member update succeeds
- **THEN** response includes `self` link to the updated member resource
- **AND** response includes `edit` link for further updates
- **AND** response includes `collection` link to members list

#### Scenario: Response includes dynamic HAL+FORMS templates based on user role

- **WHEN** member update succeeds
- **THEN** response includes _templates with update template
- **AND** template shows all updatable fields based on user role
- **AND** for members: template shows member-editable fields only
- **AND** for admins: template shows both member-editable and admin-only fields
- **AND** template indicates required vs optional fields
- **AND** template indicates which fields are read-only for current user

#### Scenario: Admin response includes admin-only fields in template

- **WHEN** authenticated user with MEMBERS:UPDATE permission retrieves member update template
- **THEN** template includes firstName, lastName, and dateOfBirth fields
- **AND** template includes all member-editable fields
- **AND** all fields are marked as editable

#### Scenario: Member response excludes admin-only fields from template

- **WHEN** authenticated member without admin permission retrieves update template
- **THEN** template excludes firstName, lastName, and dateOfBirth fields
- **AND** template includes only member-editable fields
- **AND** member-editable fields are marked as editable

### Requirement: Update Error Responses

The system SHALL return detailed error responses using problem+json format for validation and authorization failures.

#### Scenario: Validation error with multiple issues

- **WHEN** member submits update with multiple validation errors
- **THEN** HTTP 400 Bad Request is returned
- **AND** Content-Type is application/problem+json
- **AND** response includes array of validation errors
- **AND** each error indicates field and specific issue
- **AND** no changes are saved

#### Scenario: Authorization error includes helpful message

- **WHEN** member attempts to edit another member's information
- **THEN** HTTP 403 Forbidden is returned
- **AND** Content-Type is application/problem+json
- **AND** error message explains self-edit restriction
- **AND** error includes reference to correct member ID for editing

### Requirement: Concurrent Update Handling

The system SHALL handle concurrent updates to member information safely.

#### Scenario: Optimistic locking on concurrent updates

- **WHEN** two users attempt to update the same member simultaneously
- **THEN** the first update succeeds
- **AND** the second update receives HTTP 409 Conflict
- **AND** error message indicates the member was modified by another user
- **AND** error response includes current member data

#### Scenario: Retry after conflict

- **WHEN** member receives 409 Conflict response
- **THEN** member can retrieve current member data via GET
- **AND** member can submit PATCH with updated values
- **AND** subsequent update may succeed if no further conflicts occur

### Requirement: UserId Value Object

The system SHALL use unique identifiers (UserId) for both User and Member entities. The identifier SHALL wrap a UUID and
provide type safety to prevent accidental mixing of IDs from different aggregates. The identifier SHALL be immutable.

#### Scenario: Unique identifier wraps UUID value

- **WHEN** a UserId is created with a valid UUID
- **THEN** the identifier is successfully created
- **AND** the identifier exposes the UUID value
- **AND** the identifier provides equality and hashing based on the wrapped UUID
- **AND** the identifier is immutable

#### Scenario: Unique identifier prevents null UUID

- **WHEN** a UserId is attempted to be created with null UUID
- **THEN** validation fails with error
- **AND** error message indicates UUID cannot be null

#### Scenario: Unique identifier generation from string

- **WHEN** a UserId is created from a valid UUID string
- **THEN** the identifier is created from the parsed UUID
- **AND** if string is invalid UUID format, validation fails with error

### Requirement: Member-User ID Relationship

Every Member entity SHALL be created with an associated User entity, and the Member SHALL use the User's UserId as its
own UserId. The User is created automatically as part of the member registration flow, and the UserId is shared between
both aggregates.

#### Scenario: User created automatically during member registration

- **WHEN** member registration is processed
- **THEN** a User entity is created first with generated UserId
- **AND** a Member entity is created using the same UserId
- **AND** both entities share the same UserId (same UUID value)
- **AND** the relationship is established without requiring additional foreign key fields

#### Scenario: Member uses User's UserId

- **WHEN** a Member is created as part of member registration
- **THEN** the Member's UserId is set to the User's UserId
- **AND** the member and user entities share the same UserId instance
- **AND** no join operation is required for queries by user ID

#### Scenario: Query member by user ID leverages shared identifier

- **WHEN** querying for a member by user's UserId
- **THEN** no join operation is required between User and Member tables
- **AND** query is optimized by using the shared UserId directly
- **AND** performance is improved compared to foreign key lookups
