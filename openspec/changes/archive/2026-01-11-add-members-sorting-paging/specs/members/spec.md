# Members API Specification

## MODIFIED Requirements

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

## ADDED Requirements

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
