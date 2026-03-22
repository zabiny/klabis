## MODIFIED Requirements

### Requirement: List All Members

The system SHALL provide an API endpoint to retrieve a paginated and sorted list of all registered members with summary
information.

Each member summary SHALL include: id, firstName, lastName, registrationNumber, email, and active status.

The email and active fields SHALL be visible only to users with MEMBERS:MANAGE authority. Users without this authority SHALL receive null values for these fields.

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
