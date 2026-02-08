# user-permissions Specification Delta

This specification defines requirements for managing user permissions (authorities) through the Users API.

## ADDED Requirements

### Requirement: Get User Permissions API

The system SHALL provide a REST API endpoint for retrieving a user's current permissions (authorities) with proper
authorization.

#### Scenario: Get user permissions successfully

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits GET request to /api/users/{id}/permissions
- **AND** target user exists
- **THEN** HTTP 200 OK is returned
- **AND** response includes ONLY:
    - User ID
    - List of authorities (e.g., ["MEMBERS:CREATE", "MEMBERS:READ"])
- **AND** response includes HATEOAS links:
    - self: link to /api/users/{id}/permissions
    - update-permissions: link to PUT /api/users/{id}/permissions

#### Scenario: Unauthorized user attempts to get permissions

- **WHEN** authenticated user without MEMBERS:PERMISSIONS authority attempts to get permissions
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes problem+json error details
- **AND** error message indicates missing authority

#### Scenario: Get permissions for nonexistent user

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits request for nonexistent user ID
- **THEN** HTTP 404 Not Found is returned
- **AND** response includes problem+json error details
- **AND** error message indicates user not found

### Requirement: Update User Permissions API

The system SHALL provide a REST API endpoint for updating a user's permissions (authorities) with proper authorization
and business rule enforcement.

#### Scenario: Update user permissions with valid authorities

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits PUT request to /api/users/{id}/permissions
- **AND** request body contains valid authority list (e.g., ["MEMBERS:CREATE", "MEMBERS:READ"])
- **AND** target user exists
- **AND** operation does not violate admin lockout rule
- **THEN** system updates user's authorities to exactly the provided list
- **AND** HTTP 200 OK is returned
- **AND** response includes ONLY:
    - User ID
    - List of updated authorities
    - HATEOAS links (self, permissions with affordances)
- **AND** response format is same as GET /api/users/{id}/permissions
- **AND** permission change is recorded in audit log with actor, timestamp, old and new values

#### Scenario: Unauthorized user attempts to update permissions

- **WHEN** authenticated user without MEMBERS:PERMISSIONS authority attempts to update permissions
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes problem+json error details
- **AND** error message indicates missing authority

#### Scenario: Update nonexistent user's permissions

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority submits request for nonexistent user ID
- **THEN** HTTP 404 Not Found is returned
- **AND** response includes problem+json error details
- **AND** error message indicates user not found

#### Scenario: Invalid authority in request

- **WHEN** authenticated user submits request with invalid authority string (e.g., "INVALID:AUTHORITY")
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes problem+json error details listing valid authorities
- **AND** error message indicates which authority is invalid

#### Scenario: Empty authorities list

- **WHEN** authenticated user submits request with empty authorities list []
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes problem+json error details
- **AND** error message indicates at least one authority required

### Requirement: Prevent Permission Management Lockout

The system SHALL enforce that at least one user with MEMBERS:PERMISSIONS authority must exist at all times to prevent
complete permission management lockout.

#### Scenario: Attempt to remove MEMBERS:PERMISSIONS from last user who has it

- **WHEN** authenticated user attempts to update permissions for a user with MEMBERS:PERMISSIONS authority
- **AND** this is the only user in the system with MEMBERS:PERMISSIONS
- **AND** the new authority list does NOT include MEMBERS:PERMISSIONS
- **THEN** HTTP 409 Conflict is returned
- **AND** response includes problem+json error details
- **AND** error message indicates operation would remove last user with MEMBERS:PERMISSIONS

#### Scenario: Multiple users with MEMBERS:PERMISSIONS exist - can remove from one

- **WHEN** authenticated user attempts to remove MEMBERS:PERMISSIONS from a user
- **AND** at least one other user with MEMBERS:PERMISSIONS exists in the system
- **THEN** permission update succeeds
- **AND** HTTP 200 OK is returned

#### Scenario: User without MEMBERS:PERMISSIONS can have authorities modified freely

- **WHEN** authenticated user updates permissions for user without MEMBERS:PERMISSIONS
- **THEN** operation succeeds regardless of lockout check
- **AND** HTTP 200 OK is returned

#### Scenario: Update user's authorities without removing MEMBERS:PERMISSIONS

- **WHEN** authenticated user updates permissions for a user with MEMBERS:PERMISSIONS
- **AND** this is the only user with MEMBERS:PERMISSIONS
- **AND** the new authority list INCLUDES MEMBERS:PERMISSIONS
- **THEN** permission update succeeds
- **AND** HTTP 200 OK is returned

### Requirement: Permission Change Audit Trail

The system SHALL record all permission changes in the audit log with complete before/after details and actor information
using the @Auditable annotation pattern.

#### Scenario: Permission change creates audit log entry

- **WHEN** user successfully updates user permissions via PUT /api/users/{id}/permissions
- **THEN** @Auditable annotation on command handler method triggers audit logging
- **AND** audit log entry is created with:
    - Event type: USER_PERMISSIONS_CHANGED
    - Actor: authenticated user's registration number (extracted from SecurityContext)
    - Target: user ID being updated
    - IP address: extracted from HTTP request by AuditLogAspect
    - Old authorities list (before change)
    - New authorities list (after change)
    - Timestamp
- **AND** audit entry is persisted in same transaction as permission update

#### Scenario: Failed permission change does not create audit entry

- **WHEN** permission update fails validation or business rules (403, 404, 409, etc.)
- **THEN** @Auditable annotation does not create audit log entry (method throws exception before completion)
- **AND** existing permissions remain unchanged

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
- **WHEN** user requests to set permissions with any combination of these authorities
- **THEN** validation passes

#### Scenario: Authority validation error response

- **WHEN** request includes invalid authority
- **THEN** error response includes complete list of valid authorities
- **AND** error message clearly identifies which authority was invalid

## MODIFIED Requirements

None (this is a new capability extending existing auth spec)

## REMOVED Requirements

None
