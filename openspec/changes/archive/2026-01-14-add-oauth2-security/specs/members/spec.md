# Member Registration Specification Deltas

## ADDED Requirements

### Requirement: Automatic User Provisioning

The system SHALL automatically create a User account when a Member is successfully registered.

#### Scenario: User account created with member registration

- **WHEN** member is created via POST /api/members
- **THEN** User account is created in same transaction with registrationNumber matching member
- **AND** User is assigned ROLE_MEMBER
- **AND** User accountStatus is set to PENDING_ACTIVATION
- **AND** temporary password is generated and stored (BCrypt hashed)

#### Scenario: Transaction rollback on user creation failure

- **WHEN** member creation succeeds but user creation fails
- **THEN** entire transaction is rolled back
- **AND** neither Member nor User persisted to database
- **AND** HTTP 500 Internal Server Error returned

#### Scenario: Duplicate registrationNumber prevents creation

- **WHEN** member registration attempted with registrationNumber that already exists
- **THEN** unique constraint violation occurs
- **AND** transaction rolled back
- **AND** HTTP 409 Conflict returned

#### Scenario: Temporary password generated securely

- **WHEN** User account created during member registration
- **THEN** temporary password generated with secure random generator
- **AND** password meets minimum length requirement (12 characters)
- **AND** password includes alphanumeric and special characters
- **AND** password BCrypt-hashed before storage

## MODIFIED Requirements

### Requirement: Authorization

The system SHALL restrict member creation to authenticated users with MEMBERS:CREATE authority (derived from ROLE_ADMIN
or OAuth2 scope "members.write").

**Changed from:** "The system SHALL restrict member creation to users with MEMBERS:CREATE permission."

**Changes:**

- Specifies authentication required (not just authorization)
- Clarifies authority derived from ROLE_ADMIN or OAuth2 scope
- Adds OAuth2 scope mapping

#### Scenario: Admin user creates member

- **WHEN** authenticated user with ROLE_ADMIN submits member creation request
- **THEN** MEMBERS:CREATE authority derived from role
- **AND** authorization check passes
- **AND** member creation proceeds

#### Scenario: OAuth2 client with scope creates member

- **WHEN** OAuth2 client with scope "members.write" submits member creation request
- **THEN** MEMBERS:CREATE authority derived from scope
- **AND** authorization check passes
- **AND** member creation proceeds

#### Scenario: User without authority denied

- **WHEN** authenticated user with only ROLE_MEMBER (no MEMBERS:CREATE) attempts member creation
- **THEN** authorization check fails
- **AND** HTTP 403 Forbidden returned with ProblemDetail
- **AND** detail indicates "Access Denied: Insufficient authority"

#### Scenario: Unauthenticated request denied

- **WHEN** request without Authorization header (no JWT token) is made to POST /api/members
- **THEN** authentication check fails
- **AND** HTTP 401 Unauthorized returned
- **AND** WWW-Authenticate: Bearer header included

### Requirement: Audit Trail

The system SHALL record authenticated user's registrationNumber as creator and modifier of member records.

**Changed from:** Original spec did not specify auditor source (defaulted to "system")

**Changes:**

- Auditor extracted from SecurityContext (authenticated user's registrationNumber)
- Fallback to "system" for unauthenticated operations

#### Scenario: Authenticated user creates member

- **WHEN** user with registrationNumber ZBM0001 creates member via API
- **THEN** created_by field populated with "ZBM0001" from SecurityContext
- **AND** created_at field populated with current timestamp
- **AND** modified_by field populated with "ZBM0001"
- **AND** modified_at field populated with current timestamp

#### Scenario: Database migration creates bootstrap data

- **WHEN** database migration inserts admin user (no SecurityContext)
- **THEN** created_by field populated with "system"
- **AND** modified_by field populated with "system"

#### Scenario: User updates member

- **WHEN** user with registrationNumber ZBM0102 updates member
- **THEN** modified_by field updated to "ZBM0102" from SecurityContext
- **AND** modified_at field updated to current timestamp
- **AND** created_by and created_at remain unchanged
