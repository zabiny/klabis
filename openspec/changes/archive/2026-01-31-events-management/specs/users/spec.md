# users Specification (Delta)

## Purpose

This delta specification adds the EVENTS:MANAGE authority to the users module for event management authorization.

## MODIFIED Requirements

### Requirement: Authentication Principal

The system SHALL use the user's UUID as the authentication principal name for resolving member associations.

#### Scenario: Authentication principal contains user UUID

- **GIVEN** a user with ID "12345678-1234-1234-1234-123456789012"
- **AND** user successfully authenticates
- **THEN** SecurityContext authentication.getName() returns the user's UUID string
- **AND** this UUID can be used to resolve associated member records

#### Scenario: Username resolution

- **GIVEN** a User entity with userName "john.doe"
- **AND** UUID "12345678-1234-1234-1234-123456789012"
- **WHEN** authentication occurs
- **THEN** the authentication principal (username) is the UUID string, NOT "john.doe"
- **AND** "john.doe" is stored only in User.userName attribute for display purposes

**RATIONALE:** Using UUID as the authentication principal allows direct mapping to UserId and MemberId without
additional lookups. The userName field is kept for display/UI purposes only.

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities mapped to the current
authorization model.

#### Scenario: Valid authorities list

- **GIVEN** the system defines these valid authorities:
    - MEMBERS:CREATE
    - MEMBERS:READ
    - MEMBERS:UPDATE
    - MEMBERS:DELETE
    - MEMBERS:PERMISSIONS
    - EVENTS:MANAGE
- **WHEN** user requests to set permissions with any combination of these authorities
- **THEN** validation passes

#### Scenario: Authority validation error response

- **WHEN** request includes invalid authority
- **THEN** error response includes complete list of valid authorities
- **AND** error message clearly identifies which authority was invalid
