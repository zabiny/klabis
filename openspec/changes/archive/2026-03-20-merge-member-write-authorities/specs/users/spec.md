## MODIFIED Requirements

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities mapped to the current
authorization model.

#### Scenario: Valid authorities list

- **GIVEN** the system defines these valid authorities:
    - MEMBERS:MANAGE
    - MEMBERS:READ
    - MEMBERS:PERMISSIONS
    - EVENTS:MANAGE
    - EVENTS:READ
    - CALENDAR:MANAGE
- **WHEN** user requests to set permissions with any combination of these authorities
- **THEN** validation passes

#### Scenario: Authority validation error response

- **WHEN** request includes invalid authority
- **THEN** error response includes complete list of valid authorities
- **AND** error message clearly identifies which authority was invalid

---

### Requirement: User Authorization

The system SHALL authorize API operations based on user authorities and OAuth2 scopes. **Authorization checks SHALL
query UserPermissions aggregate to determine user's direct authorities, and MAY consider group-based authorities in the
future.**

#### Scenario: User with required authority accesses member write endpoint

- **WHEN** authenticated user with MEMBERS:MANAGE authority calls POST /api/members
- **THEN** authorization check passes (authority from UserPermissions)
- **AND** endpoint handler executes

#### Scenario: User without required authority denied access to member write endpoint

- **WHEN** authenticated user without MEMBERS:MANAGE authority calls POST /api/members
- **THEN** authorization check fails
- **AND** system returns HTTP 403 Forbidden with ProblemDetail

---

### Requirement: OAuth2 Scope Mapping

The system SHALL map OAuth2 scopes to internal authorities for machine-to-machine access.

#### Scenario: Members write scope mapping

- **WHEN** OAuth2 client requests token with scope "members.write"
- **THEN** access token includes scope claim
- **AND** scope mapped to authority MEMBERS:MANAGE
- **AND** endpoint authorization uses authorities from both roles and scopes

## REMOVED Requirements

### Requirement: (authority values MEMBERS:CREATE, MEMBERS:UPDATE, MEMBERS:DELETE)

**Reason**: Replaced by unified MEMBERS:MANAGE authority. Granular create/update/delete permissions provided no real benefit — they were always assigned together.

**Migration**: All existing user permissions with MEMBERS:CREATE, MEMBERS:UPDATE, or MEMBERS:DELETE values SHALL be automatically migrated to MEMBERS:MANAGE via database migration.

## MODIFIED Requirements

### Requirement: Admin Bootstrap Authority

The admin user created on system startup SHALL receive MEMBERS:MANAGE authority (instead of MEMBERS:CREATE) to allow creating the first member.

#### Scenario: Admin user authenticates and creates first member

- **WHEN** admin user authenticates with admin / admin123
- **THEN** access token issued with MEMBERS:MANAGE authority
- **AND** admin can call POST /api/members to create first member
- **AND** created member can have different registrationNumber than admin user
