## MODIFIED Requirements

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