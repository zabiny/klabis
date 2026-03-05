# Members API Specification

## ADDED Requirements

### Requirement: List All Members

The system SHALL provide an API endpoint to retrieve a list of all registered members with summary information.

#### Scenario: Retrieve list of members successfully

- **WHEN** an authenticated user with MEMBERS:READ permission makes a GET request to /api/members
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain a collection of member summaries in HAL+FORMS JSON format
- **AND** each member summary SHALL include firstName, lastName, and registrationNumber
- **AND** the response SHALL include HATEOAS links for navigation (self link)

#### Scenario: Empty member list

- **WHEN** an authenticated user makes a GET request to /api/members
- **AND** no members exist in the system
- **THEN** the system SHALL return HTTP 200 OK
- **AND** the response SHALL contain an empty collection
- **AND** the response SHALL include HATEOAS links for navigation

#### Scenario: Unauthorized access denied

- **WHEN** an unauthenticated user makes a GET request to /api/members
- **THEN** the system SHALL return HTTP 401 Unauthorized
- **AND** the response SHALL include an appropriate error message

#### Scenario: Forbidden without required permission

- **WHEN** an authenticated user without MEMBERS:READ permission makes a GET request to /api/members
- **THEN** the system SHALL return HTTP 403 Forbidden
- **AND** the response SHALL indicate insufficient permissions

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
