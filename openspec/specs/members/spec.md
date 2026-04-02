# members Specification

## Purpose

This specification defines requirements for the club member registration and management system. It encompasses the
complete member lifecycle including registration with mandatory and conditional personal information, registration
number generation (ZBM format), contact details management, and secure data handling for sensitive information like
birth numbers and contact data. It also covers membership suspension workflow with reason tracking and audit trail,
birth number (rodné číslo) management with GDPR-compliant encryption, and bank account number storage for expense
reimbursement.

## Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a User entity first, then creating a Member entity with a unique member identifier that references the associated user.

#### Scenario: Member registration creates User then Member

- **WHEN** authenticated user with MEMBERS:CREATE permission submits member data via POST /api/members
- **THEN** the system creates a User entity with a generated unique identifier
- **AND** the system creates a Member entity with a member identifier that references the same user
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

The system SHALL automatically generate a unique registration number in format XXXYYSS where:

- XXX = club code (3-character alphanumeric, configured by system administrator)
- YY = member's birth year (last 2 digits)
- SS = sequential number for members born in the same year (2 digits, starting at 00)

#### Scenario: Generate registration number for first member with given birth year

- **WHEN** first member born in 2001 is registered
- **THEN** registration number is formatted as XXX0100 (e.g., ZBM0100 for club ZBM)

#### Scenario: Generate registration number for subsequent members with same birth year

- **WHEN** additional members born in 2001 are registered
- **THEN** registration number increments sequence (e.g., ZBM0101, ZBM0102)

#### Scenario: Different birth years have independent sequences

- **WHEN** member born in 2005 is registered after member born in 2001
- **THEN** registration number starts new sequence for birth year 2005 (e.g., ZBM0500)

#### Scenario: Registration number uniqueness

- **WHEN** registration number is generated
- **THEN** system verifies uniqueness across all members
- **AND** prevents duplicate registration numbers

### Requirement: Birth Number Management

The system SHALL manage birth numbers (rodné číslo) for Czech nationals with mandatory availability, format validation, GDPR-compliant encryption, consistency validation, API exposure, and audit trail.

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

The system SHALL accept the following optional fields with enhanced validation:

- Chip number (numeric only)
- Bank account number (see "Bank Account Management" requirement)

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

### Requirement: Bank Account Management

The system SHALL allow members to have an optional bank account number for expense reimbursement purposes, with format validation supporting both IBAN and Czech domestic format, plain text storage, and API exposure.

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
- **THEN** validation fails with HTTP 400
- **AND** error message states "Invalid IBAN: {value}"

#### Scenario: IBAN with invalid checksum is rejected

- **WHEN** user enters IBAN with invalid checksum digits
- **THEN** validation fails with HTTP 400
- **AND** error message states "Invalid IBAN: {value}"

#### Scenario: Invalid domestic format is rejected

- **WHEN** user enters domestic format with invalid bank code (e.g., "123456/08", "123456/08000")
- **THEN** validation fails with HTTP 400
- **AND** error message states "Invalid domestic format: {value}"

#### Scenario: Invalid domestic format missing slash is rejected

- **WHEN** user enters account number without slash separator (e.g., "1234560800")
- **THEN** validation fails with HTTP 400
- **AND** error message states "Cannot detect account format: {value}"

#### Scenario: Bank account number stored as-is

- **WHEN** member with bank account number is saved to database
- **THEN** bank account number is stored in normalized IBAN format without spaces

#### Scenario: Bank account included in member detail response

- **WHEN** authorized user requests member details (GET /api/members/{id})
- **THEN** response includes bankAccountNumber field (or null if not set)

#### Scenario: Bank account included in member registration request

- **WHEN** user creates new member (POST /api/members)
- **THEN** request accepts optional bankAccountNumber field

#### Scenario: Bank account included in member update request

- **WHEN** user updates member (PATCH /api/members/{id})
- **THEN** request accepts optional bankAccountNumber field

#### Scenario: Help text explains purpose

- **WHEN** user views bank account field in UI
- **THEN** help text states "For reimbursement of travel expenses and other club-related costs"

#### Scenario: Bank account optional nature is clear

- **WHEN** user views bank account field
- **THEN** field is marked as optional (not required)

### Requirement: Welcome Email on Registration

When a new member is successfully registered, the system SHALL send a welcome email to the member's primary email
address (or guardian's email for minors without email).

The system SHALL:

- Send email asynchronously after transaction commits
- Include member's name and registration number in the email
- Include activation link with secure token
- Send email in both HTML and plain-text formats
- Not fail member registration if email delivery fails

#### Scenario: Adult member receives welcome email

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register an adult member with email "jan@example.com"
- **THEN** the member is created successfully
- **AND** a welcome email is sent to "jan@example.com"
- **AND** the email contains an activation link that expires in 72 hours

#### Scenario: Minor member welcome email sent to guardian

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register a minor member without email
- **AND** the guardian has email "parent@example.com"
- **THEN** the member is created successfully
- **AND** a welcome email is sent to "parent@example.com"

#### Scenario: Member without any email address

- **GIVEN** a user with MEMBERS:CREATE authority
- **WHEN** they register an adult member without email
- **THEN** the member is created successfully
- **AND** no welcome email is sent
- **AND** a warning is logged

### Requirement: HATEOAS Hypermedia Controls

The system SHALL include hypermedia links in all member-related API responses following HAL+FORMS specification:

- `self` - Link to member resource
- `edit` - Link to update member (if authorized)
- `collection` - Link to members collection
- Form templates for available actions

**Note**: Links for member suspension/resumption will be added in a separate change.

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

The system SHALL provide an API endpoint to retrieve a paginated and sorted list of registered members with summary
information.

Each member summary SHALL include: id, firstName, lastName, registrationNumber, email, and active status.

The email and active fields SHALL be visible only to users with MEMBERS:MANAGE authority. Users without this authority SHALL receive null values for these fields.

Users with MEMBERS:MANAGE authority SHALL see all members (active and inactive). Users without MEMBERS:MANAGE authority SHALL see only active members.

Each member summary item SHALL include HATEOAS affordances reflecting available actions for the current user:
- For active members with MEMBERS:MANAGE authority: update member (PATCH) and suspend membership affordances
- For inactive members with MEMBERS:MANAGE authority: update member (PATCH) and resume membership affordances
- For users without MEMBERS:MANAGE authority: no action affordances (self link only)

Each active member summary item SHALL include a permissions link (`_links.permissions`) for users with MEMBERS:PERMISSIONS authority.

#### Scenario: Retrieve paginated list with default parameters

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members without query parameters
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain the first page of member summaries (page 0)
- **AND** the page size SHALL be 10 items by default
- **AND** the members SHALL be sorted by lastName ascending, then firstName ascending by default
- **AND** the response SHALL include page metadata (size, totalElements, totalPages, number)
- **AND** the response SHALL include HATEOAS pagination links (self, first, last, next if applicable)

#### Scenario: Admin user sees email and active status in summary

- **WHEN** a user with MEMBERS:MANAGE authority requests the member list
- **THEN** each member summary SHALL include the member's email address
- **AND** each member summary SHALL include the member's active status (boolean)

#### Scenario: Non-admin user does not see email and active status

- **WHEN** a user without MEMBERS:MANAGE authority requests the member list
- **THEN** each member summary SHALL have null values for email and active fields

#### Scenario: Admin user sees both active and inactive members

- **WHEN** a user with MEMBERS:MANAGE authority requests the member list
- **THEN** the response SHALL include both active and inactive members

#### Scenario: Non-admin user sees only active members

- **WHEN** a user without MEMBERS:MANAGE authority requests the member list
- **THEN** the response SHALL include only active members
- **AND** inactive (suspended) members SHALL NOT appear in the list

#### Scenario: Admin sees action affordances for active member

- **WHEN** a user with MEMBERS:MANAGE authority requests the member list
- **AND** the list contains an active member
- **THEN** the active member's summary item SHALL include a suspendMember affordance (POST)
- **AND** the active member's summary item SHALL include an updateMember affordance (PATCH)

#### Scenario: Admin sees action affordances for inactive member

- **WHEN** a user with MEMBERS:MANAGE authority requests the member list
- **AND** the list contains an inactive member
- **THEN** the inactive member's summary item SHALL include a resumeMember affordance (POST)
- **AND** the inactive member's summary item SHALL include an updateMember affordance (PATCH)

#### Scenario: Non-admin user does not see action affordances

- **WHEN** a user without MEMBERS:MANAGE authority requests the member list
- **THEN** member summary items SHALL only include a self link without action affordances

#### Scenario: Permissions link for active member

- **WHEN** a user with MEMBERS:PERMISSIONS authority requests the member list
- **AND** the list contains an active member
- **THEN** the active member's summary item SHALL include a permissions link

#### Scenario: No permissions link for inactive member

- **WHEN** a user with MEMBERS:PERMISSIONS authority requests the member list
- **AND** the list contains an inactive member
- **THEN** the inactive member's summary item SHALL NOT include a permissions link

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
identifier, including suspension details if membership has been suspended.

Users without MEMBERS:MANAGE authority SHALL NOT be able to access details of inactive (suspended) members. Such access SHALL result in HTTP 404 Not Found, consistent with the behavior of the member list for these users.

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
    - Email address
    - Phone number
    - Guardian information (if applicable)
    - Chip number (if provided)
    - Bank account number (if provided)
    - Active status: true
    - Suspension reason: null
    - Suspended at: null
    - Suspension note: null
    - Suspended by: null
- **AND** the response SHALL include HATEOAS links:
    - `self` - Link to the current member resource
    - `collection` - Link to the members list
    - `edit` - Link to update member (if user has MEMBERS:UPDATE permission)
    - `suspend` - Link to suspend membership (if user has MEMBERS:UPDATE permission and member is active)

#### Scenario: Admin retrieves existing suspended member by ID

- **WHEN** a user with MEMBERS:MANAGE authority makes a GET request to /api/members/{id}
- **AND** the member with the given ID exists
- **AND** the member is suspended (inactive)
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
    - Email address
    - Phone number
    - Guardian information (if applicable)
    - Chip number (if provided)
    - Bank account number (if provided)
    - Active status: false
    - Suspension reason: one of ODHLASKA, PRESTUP, OTHER
    - Suspended at: ISO-8601 datetime string
    - Suspension note: text string or null
    - Suspended by: user ID (UUID)
- **AND** the response SHALL include HATEOAS links:
    - `self` - Link to the current member resource
    - `collection` - Link to the members list
    - `edit` - Link to update member (if user has MEMBERS:UPDATE permission)
    - Suspend link SHALL NOT be present (member already suspended)

#### Scenario: Non-admin user attempts to access inactive member by ID

- **WHEN** a user without MEMBERS:MANAGE authority makes a GET request to /api/members/{id}
- **AND** the member with the given ID exists
- **AND** the member is inactive (suspended)
- **THEN** the system SHALL return HTTP 404 Not Found
- **AND** the response SHALL include error details with problem+json media type

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

The member details endpoint SHALL return complete member information in a HATEOAS-compliant HAL+FORMS format with proper ISO-8601 date serialization, structured address and contact information, and suspension details when applicable.

#### Scenario: Active member response contains all fields

- **WHEN** a member details response is returned for an active member
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID), also UserId due to 1:1 relationship
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth as ISO-8601 date string (YYYY-MM-DD)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE)
    - `address` - Object containing street, city, postalCode, country (ISO 3166-1 alpha-2)
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - `email` - Single email address string
    - `phone` - Single phone number string
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `identityCard` - Identity card object containing cardNumber and validityDate (present if provided)
    - `drivingLicenseGroup` - Driving license group (present if provided)
    - `medicalCourse` - Medical course object containing completionDate and optional validityDate (present if provided)
    - `trainerLicense` - Trainer license object containing licenseNumber and validityDate (present if provided)
    - `dietaryRestrictions` - Dietary restrictions text (present if provided)
    - `active` - Boolean true
    - `suspensionReason` - null
    - `suspendedAt` - null
    - `suspensionNote` - null
    - `suspendedBy` - null
    - `guardian` - Guardian information object (present if member has guardian)

#### Scenario: Suspended member response contains suspension details

- **WHEN** a member details response is returned for a suspended member
- **THEN** the response SHALL include:
    - `id` - Member's unique identifier (UUID)
    - `registrationNumber` - Unique registration number in format XXXYYSS
    - `firstName` - Member's first name
    - `lastName` - Member's last name
    - `dateOfBirth` - Member's date of birth as ISO-8601 date string (YYYY-MM-DD)
    - `nationality` - Member's nationality code (ISO 3166-1 alpha-2)
    - `gender` - Member's gender (MALE, FEMALE)
    - `address` - Object containing street, city, postalCode, country
    - `rodneCislo` - Czech ID number (present only for Czech nationality)
    - `email` - Single email address string
    - `phone` - Single phone number string
    - `chipNumber` - Chip number (present if provided)
    - `bankAccountNumber` - Bank account number (present if provided)
    - `identityCard` - Identity card object containing cardNumber and validityDate (present if provided)
    - `drivingLicenseGroup` - Driving license group (present if provided)
    - `medicalCourse` - Medical course object containing completionDate and optional validityDate (present if provided)
    - `trainerLicense` - Trainer license object containing licenseNumber and validityDate (present if provided)
    - `dietaryRestrictions` - Dietary restrictions text (present if provided)
    - `active` - Boolean false
    - `suspensionReason` - One of: ODHLASKA, PRESTUP, OTHER
    - `suspendedAt` - ISO-8601 datetime string (YYYY-MM-DDTHH:MM:SS)
    - `suspensionNote` - Text string or null
    - `suspendedBy` - User ID (UUID) of the user who performed suspension
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

The member details response SHALL include hypermedia links following HAL+FORMS specification to enable API discoverability and navigation, including conditional link to user permissions.

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

#### Scenario: Permissions link conditionally included

- **WHEN** an authenticated user with MEMBERS:PERMISSIONS authority views a member
- **THEN** the response SHALL include a `permissions` link
- **AND** the link SHALL point to /api/users/{id}/permissions (where id = member.id = userId)
- **AND** the link SHALL use the rel "permissions"
- **AND** the permissions management action is accessible directly from the member detail context without navigating to a separate page

#### Scenario: Permissions link excluded for users without permission

- **WHEN** an authenticated user without MEMBERS:PERMISSIONS authority views a member
- **THEN** the response SHALL NOT include a `permissions` link
- **AND** only links for authorized actions are included

#### Scenario: Suspend link included for active members

- **WHEN** a user with MEMBERS:UPDATE permission views an active member
- **THEN** the response SHALL include a `suspend` link
- **AND** the link SHALL point to POST /api/members/{id}/suspend
- **AND** the link SHALL use the rel "suspend"

#### Scenario: Suspend link excluded for suspended members

- **WHEN** a user views a suspended member
- **THEN** the response SHALL NOT include a `suspend` link
- **AND** only links for available actions are included

#### Scenario: Suspend link excluded for users without permission

- **WHEN** an authenticated user without MEMBERS:UPDATE permission views a member
- **THEN** the response SHALL NOT include a `suspend` link
- **AND** only links for authorized actions are included

#### Scenario: Unauthenticated users receive no permissions link

- **WHEN** an unauthenticated user views a member (if allowed)
- **THEN** the response SHALL NOT include a `permissions` link
- **AND** only publicly available links are included

### Requirement: Member Detail Response — Conditional Edit Template

The system SHALL include a PATCH template in the member detail response only for users authorized to edit the member, and the template SHALL contain only the fields the caller is permitted to modify.

- Users with MEMBERS:MANAGE authority: template contains all editable fields (firstName, lastName, dateOfBirth, gender, nationality, birthNumber, chipNumber, identityCard, medicalCourse, trainerLicense, drivingLicenseGroup, email, phone, address, dietaryRestrictions, bankAccountNumber, guardian)
- Users viewing their own profile (without MEMBERS:MANAGE): template contains only self-editable fields (email, phone, address, dietaryRestrictions)
- Users viewing another member's profile (without MEMBERS:MANAGE): no PATCH template is included

#### Scenario: Admin retrieves member detail

- **WHEN** user with MEMBERS:MANAGE authority requests GET /api/members/{id}
- **THEN** response includes a PATCH template with all editable fields
- **AND** response includes personal data, supplementary info, and documents sections

#### Scenario: Member retrieves own profile

- **WHEN** authenticated member requests GET /api/members/{id} where id matches their own member profile
- **THEN** response includes a PATCH template with only self-editable fields: email, phone, address, dietaryRestrictions
- **AND** personal data, supplementary info, and documents are included in the response

#### Scenario: Member retrieves another member's profile

- **WHEN** authenticated member requests GET /api/members/{id} for a different member
- **THEN** response does NOT include a PATCH template
- **AND** contact and address data are included in the response

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
- **AND** HTTP 204 No Content status is returned
- **AND** response includes Location header pointing to the updated member resource

#### Scenario: Admin updates member information successfully

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request to /api/members/{id}
- **AND** request contains valid updates to any editable field (including admin-only fields)
- **THEN** member information is updated
- **AND** HTTP 204 No Content status is returned
- **AND** response includes Location header pointing to the updated member resource

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

- Email address
- Phone number
- Address (street, city, postalCode, country — all required when updating address)
- Chip number (numeric only)
- Nationality (ISO 3166-1 alpha-2)
- Bank account number
- Legal guardian email
- Legal guardian phone number
- Identity card (card number and validity date)
- Driving license group (B, BE, C, C1, D, D1, etc.)
- Medical course (completion date and optional validity date)
- Trainer license (license number and validity date)
- Dietary restrictions text field (optional, max 500 characters)

#### Scenario: Member updates chip number

- **WHEN** authenticated member submits PATCH request to /api/members/{id} with chipNumber field
- **AND** the {id} matches the authenticated user's member ID
- **THEN** chip number is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates identity card

- **WHEN** authenticated member submits PATCH request with identityCard (cardNumber, validityDate)
- **AND** the {id} matches the authenticated user's member ID
- **THEN** identity card information is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates driving license group

- **WHEN** authenticated member submits PATCH request with drivingLicenseGroup field
- **AND** the {id} matches the authenticated user's member ID
- **THEN** driving license group is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates medical course

- **WHEN** authenticated member submits PATCH request with medicalCourse (completionDate, optional validityDate)
- **AND** the {id} matches the authenticated user's member ID
- **THEN** medical course information is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates trainer license

- **WHEN** authenticated member submits PATCH request with trainerLicense (licenseNumber, validityDate)
- **AND** the {id} matches the authenticated user's member ID
- **THEN** trainer license information is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates nationality

- **WHEN** authenticated member submits PATCH request with nationality field
- **AND** the {id} matches the authenticated user's member ID
- **THEN** nationality is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member changes nationality from Czech to non-Czech and birth number is cleared

- **WHEN** authenticated member submits PATCH request changing nationality from CZ to a non-Czech value
- **AND** the member previously had a birth number stored
- **THEN** birth number is cleared
- **AND** nationality is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates guardian contact information

- **WHEN** authenticated member submits PATCH request with guardian email and/or phone
- **AND** the {id} matches the authenticated user's member ID
- **THEN** guardian contact information is updated
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member updates subset of fields

- **WHEN** member submits PATCH request with only some member-editable fields
- **THEN** only provided fields are updated
- **AND** non-provided fields remain unchanged
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Member attempts to update admin-only fields

- **WHEN** member submits PATCH request containing firstName, lastName, dateOfBirth, gender, or birthNumber
- **AND** the user does NOT have MEMBERS:UPDATE permission
- **THEN** these fields are silently ignored
- **AND** only member-editable fields are processed
- **AND** HTTP 204 No Content is returned if at least one valid member-editable field is present

### Requirement: Admin-Only Fields

The system SHALL allow users with MEMBERS:UPDATE permission to update the following admin-only fields on any member record:

- First name (required, not blank)
- Last name (required, not blank)
- Date of birth (required, valid ISO-8601 date, not in the future)
- Gender (MALE, FEMALE)
- Birth number (Czech ID number — only for Czech nationality, cryptographically validated format)

#### Scenario: Admin updates firstName on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName field
- **THEN** firstName is updated
- **AND** validation ensures firstName is not blank
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Admin updates lastName on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with lastName field
- **THEN** lastName is updated
- **AND** validation ensures lastName is not blank
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Admin updates dateOfBirth on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with dateOfBirth field
- **THEN** dateOfBirth is updated
- **AND** validation ensures dateOfBirth is valid ISO-8601 date format
- **AND** validation ensures dateOfBirth is not in the future
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Admin updates gender on member record

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with gender field
- **THEN** gender is updated to one of MALE, FEMALE
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Admin updates birth number on Czech member

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with birthNumber field
- **AND** member has Czech nationality (CZ)
- **THEN** birth number is validated, encrypted, and stored
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Admin attempts to set birth number on non-Czech member

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with birthNumber field
- **AND** member does NOT have Czech nationality
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates birth number is only allowed for Czech nationals

#### Scenario: Admin updates all admin-only fields

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, dateOfBirth, gender
- **THEN** all fields are updated
- **AND** all validation rules are enforced
- **AND** HTTP 204 No Content is returned with Location header

#### Scenario: Non-admin attempts to update admin-only fields

- **WHEN** authenticated user without MEMBERS:UPDATE permission submits PATCH request with firstName, lastName, dateOfBirth, gender, or birthNumber
- **THEN** these fields are silently ignored
- **AND** only member-editable fields are processed if present
- **AND** HTTP 204 No Content is returned if at least one valid member-editable field is present

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
- **AND** HTTP 204 No Content is returned

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

The member update endpoint SHALL return HTTP 204 No Content with a Location header pointing to the updated member resource. The client can follow the Location header to retrieve the updated member representation with HAL+FORMS templates.

#### Scenario: Successful update response

- **WHEN** member information is successfully updated via PATCH
- **THEN** HTTP 204 No Content is returned
- **AND** response includes Location header pointing to the updated member resource (/api/members/{id})
- **AND** response body is empty

#### Scenario: Client retrieves updated member after PATCH

- **WHEN** client follows the Location header from a successful PATCH response
- **THEN** GET request returns the updated member with HAL+FORMS representation
- **AND** response includes _templates reflecting available actions based on user role
- **AND** for members: template shows member-editable fields only
- **AND** for admins: template shows both member-editable and admin-only fields

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

### Requirement: Type-Safe Member Identification

The system SHALL enforce type-safe member identification to prevent confusion between member identifiers and other entity identifiers (user, event).

#### Scenario: Member services require member-specific identifier

- **WHEN** calling member update, suspension, or query services
- **THEN** services require a member-specific identifier
- **AND** the system prevents accidental use of user or event identifiers where member identifier is required

#### Scenario: Member domain uses member identifier type

- **WHEN** accessing a Member entity's identifier
- **THEN** the identifier is of member-specific type
- **AND** implicit conversion to other identifier types is not allowed

#### Scenario: Member events contain member identifier

- **WHEN** a member-related domain event is published (MemberCreatedEvent, MemberSuspendedEvent)
- **THEN** the event contains member-specific identifier
- **AND** event consumers receive type-safe member reference

### Requirement: Member-User Identifier Relationship

The system SHALL maintain a 1:1 relationship between member and user identifiers while keeping them as distinct types for type safety.

#### Scenario: Member identifier references user identifier

- **WHEN** a Member is created
- **THEN** the member identifier and user identifier reference the same underlying value
- **AND** the system can convert between member and user identifiers when explicitly required
- **AND** no automatic conversion occurs (requires explicit code)

### Requirement: Membership Suspension Request

The system SHALL accept membership suspension requests with reason and optional note. Suspension takes effect immediately upon request. Before processing, the system SHALL check whether the member is the last owner of any user group and require resolution if so.

#### Scenario: Valid suspension request

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits POST request to /api/members/{id}/suspend
- **AND** request contains valid suspension reason (ODHLASKA, PRESTUP, OTHER)
- **AND** the member is not the last owner of any user group
- **THEN** membership suspension is processed immediately
- **AND** HTTP 204 No Content status is returned
- **AND** response includes Location header pointing to the member resource
- **AND** suspendedAt is set to current timestamp

#### Scenario: Missing required reason field

- **WHEN** authenticated user submits suspension request without reason field
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates reason is required
- **AND** no changes are made to the member

#### Scenario: Invalid suspension reason

- **WHEN** authenticated user submits suspension request with invalid reason value
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message lists valid reason values (ODHLASKA, PRESTUP, OTHER)

#### Scenario: Unauthorized user attempts suspension

- **WHEN** authenticated user without MEMBERS:UPDATE permission attempts to suspend membership
- **THEN** HTTP 403 Forbidden is returned
- **AND** error response indicates insufficient permissions

#### Scenario: Suspension of last owner of a training group

- **WHEN** authenticated user attempts to suspend a member who is the sole owner of a training group
- **THEN** the system returns a warning response listing the affected groups
- **AND** requires designation of a successor owner for each training group before suspension can proceed

#### Scenario: Suspension of last owner of a family or free group

- **WHEN** authenticated user attempts to suspend a member who is the sole owner of a family or free group
- **THEN** the system returns a warning response listing the affected groups
- **AND** requires either designation of a successor owner or dissolution of each affected group before suspension can proceed

#### Scenario: Suspension proceeds after group ownership resolved

- **WHEN** authenticated user resolves all group ownership conflicts (successors designated or groups dissolved)
- **AND** resubmits the suspension request
- **THEN** the suspension is processed normally

### Requirement: Membership Status Update

The system SHALL update member status from active to inactive upon successful suspension.

#### Scenario: Member status changes to inactive

- **WHEN** membership suspension is processed successfully
- **THEN** member active status is set to false
- **AND** suspension reason is stored
- **AND** suspension timestamp is stored
- **AND** suspension note is stored if provided
- **AND** suspender user ID is stored

#### Scenario: Already suspended member suspension attempt

- **WHEN** user attempts to suspend a member that is already inactive
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates member is already suspended
- **AND** no changes are made to the member

#### Scenario: Concurrent suspension attempts

- **WHEN** two users attempt to suspend the same member simultaneously
- **THEN** the first suspension succeeds
- **AND** the second suspension receives HTTP 409 Conflict
- **AND** error message indicates member was already suspended

### Requirement: Suspension Response Format

The system SHALL return HTTP 204 No Content with Location header upon successful membership suspension.

#### Scenario: Successful suspension response

- **WHEN** membership suspension is successful
- **THEN** HTTP 204 No Content is returned
- **AND** response includes Location header pointing to the member resource (/api/members/{id})
- **AND** response body is empty

#### Scenario: Client retrieves member after suspension

- **WHEN** client follows the Location header from a successful suspension response
- **THEN** GET request returns the member with updated suspension details
- **AND** response includes suspensionReason (ODHLASKA, PRESTUP, OTHER)
- **AND** response includes suspendedAt as ISO-8601 datetime string
- **AND** response includes suspensionNote if provided
- **AND** response includes suspendedBy (user ID of suspender)
- **AND** response does NOT include `suspend` link (member already suspended)

### Requirement: Suspension Domain Event Publishing

The system SHALL publish a domain event upon successful membership suspension for integration with other modules.

#### Scenario: MemberSuspendedEvent is published

- **WHEN** membership suspension is successfully committed to database
- **THEN** MemberSuspendedEvent is published
- **AND** event contains memberId
- **AND** event contains suspensionReason
- **AND** event contains suspendedAt timestamp
- **AND** event contains suspendedBy user ID

#### Scenario: Event publishing failure does not block suspension

- **WHEN** membership suspension succeeds but event publishing fails
- **THEN** member suspension is committed
- **AND** event failure is logged for retry
- **AND** response indicates member was suspended successfully

### Requirement: Member Detail Page — Layout Matches Mockup

The member detail page SHALL use a two-column layout and display content according to the available HAL template fields, without any client-side role detection logic.

#### Scenario: Detail page with full template (admin or self)

- **WHEN** member detail response includes a PATCH template
- **THEN** the page renders in a two-column layout: left column (personal data, contact, address), right column (supplementary info, documents and licenses)
- **AND** an "Upravit profil" button is shown

#### Scenario: Detail page without template (other member)

- **WHEN** member detail response has no PATCH template
- **THEN** only contact section and address section are displayed
- **AND** no action buttons are shown

#### Scenario: Admin detail shows action buttons with icons

- **WHEN** member detail response includes a full PATCH template (admin view)
- **THEN** the page header shows action buttons: "Upravit profil" (pencil icon), "Vložit / Vybrat" (banknote icon), "Oprávnění" (shield icon, visible only if permissions link present), "Ukončit členství" (user-x icon, red)

#### Scenario: Own profile shows membership and edit buttons

- **WHEN** member detail response includes a self-edit PATCH template (own profile view)
- **THEN** the page header shows: "Členské příspěvky" button and "Upravit profil" button
- **AND** "Oprávnění" and "Ukončit členství" buttons are NOT shown

### Requirement: Member Edit Form — Action Bar at Bottom

Edit forms (admin edit, self edit) SHALL place action buttons at the bottom of the form, not at the top.

#### Scenario: Edit form actions are at the bottom

- **WHEN** user opens member edit form
- **THEN** "Zrušit" and "Uložit změny" buttons appear after all form sections
- **AND** admin edit shows a badge "Admin — editace všech polí" in the page header
- **AND** in self-edit mode, fields not present in the template are displayed as read-only

### Requirement: Member Registration Page — Two-Column Layout

The member registration page SHALL use a two-column layout matching the approved mockup (`NIoA4` in `pencil/klabis-members.pen`).

#### Scenario: Registration page layout

- **WHEN** admin navigates to member registration page
- **THEN** the left column contains: personal data section, contact section, address section
- **AND** the right column contains supplementary information section only (no documents or licenses)
- **AND** "Zrušit" and "Registrovat člena" (user-plus icon) buttons appear at the bottom of the form

