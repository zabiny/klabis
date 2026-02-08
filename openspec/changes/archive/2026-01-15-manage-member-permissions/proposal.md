# Proposal: Manage Member Permissions

**Change ID:** `manage-member-permissions`
**Status:** Draft
**Created:** 2026-01-14
**Author:** AI Assistant

## Overview

Enable users with `MEMBERS:PERMISSIONS` authority to manage permissions (authorities) for other users through the Users
API. This provides granular permission management while maintaining system security through safeguards.

## Business Context

Currently, member permissions and roles are fixed at user creation time. There is no API to modify a member's
authorities after they are created. Club administrators need the ability to:

- Grant or revoke individual authorities (e.g., `MEMBERS:CREATE`, `MEMBERS:UPDATE`) for members
- Manage member access control without requiring database changes or redeployment
- Track permission changes for audit and compliance purposes

## Scope

### In Scope

- New API endpoints:
    - `GET /api/users/{id}/permissions` to retrieve current user authorities
    - `PUT /api/users/{id}/permissions` to update user authorities
- Support for granular authority assignment (individual authorities like `MEMBERS:CREATE`, `MEMBERS:READ`,
  `MEMBERS:PERMISSIONS`, etc.)
- Authorization check: requires `MEMBERS:PERMISSIONS` authority (dedicated permission for managing permissions)
- Business rule: System must maintain at least one user with `MEMBERS:PERMISSIONS` authority (prevent permission
  management lockout)
- Audit logging: All permission changes recorded with who made the change and when
- HATEOAS links for permission management operations

### Out of Scope

- Role management (changing user roles) - authorities only, roles are organizational labels
- Bulk permission updates for multiple users
- Permission templates or permission groups
- UI/Frontend implementation (API only)

## User Stories

**As a** club administrator with `MEMBERS:PERMISSIONS` authority
**I want to** view permissions assigned to any user
**So that** I can understand their current access level before making changes

**As a** club administrator with `MEMBERS:PERMISSIONS` authority
**I want to** change permissions for any other user
**So that** I can control access to specific features without system admin intervention

**As a** system administrator
**I want to** ensure at least one admin always exists
**So that** the system cannot be locked out due to permission misconfiguration

**As a** compliance officer
**I want to** see a complete audit trail of permission changes
**So that** I can track who changed what permissions and when

## Dependencies

- Existing `auth` spec (extends authorization requirements)
- Existing audit infrastructure (`AuditEventType`, `Auditable`)
- Users module with User aggregate and UserRepository

## Risks and Mitigations

| Risk                          | Impact | Mitigation                                                                   |
|-------------------------------|--------|------------------------------------------------------------------------------|
| Permission management lockout | High   | Enforce business rule: at least one user with MEMBERS:PERMISSIONS must exist |
| Permission escalation         | Medium | Only predefined authorities can be assigned; validation enforced             |
| Audit trail gaps              | Medium | Use existing audit infrastructure; log before and after states               |
| Complexity for users          | Low    | Clear error messages; HATEOAS links guide available operations               |

## Design Notes

### API Design

- Endpoints:
    - `GET /api/users/{id}/permissions` - Retrieve user authorities
        - Response: List of authority strings (e.g., `["MEMBERS:CREATE", "MEMBERS:READ"]`)
        - Error responses: 403 if unauthorized, 404 if user not found
    - `PUT /api/users/{id}/permissions` - Update user authorities
        - Request body: List of authority strings (e.g., `["MEMBERS:CREATE", "MEMBERS:READ"]`)
        - Response: Updated user details with new authorities
        - Error responses: 403 if unauthorized, 409 if violates admin lockout rule
- Separate Users context provides clear separation between user accounts and member profiles

### Domain Model

- User aggregate gains method: `updateAuthorities(Set<String> newAuthorities)`
- Business rule enforcement in domain layer
- Audit logging via `@Auditable` annotation on command handler (USER_PERMISSIONS_CHANGED event)

### Security Considerations

- Authorization: `@PreAuthorize("hasAuthority('MEMBERS:PERMISSIONS')")`
- Dedicated authority for permission management (separate from MEMBERS:UPDATE)
- No role changes through this API (roles remain immutable via API)
- Prevent last admin removal through domain validation

## Success Criteria

1. Users with `MEMBERS:PERMISSIONS` can view current authorities of any user via GET endpoint
2. Users with `MEMBERS:PERMISSIONS` can grant/revoke individual authorities via PUT endpoint
3. System prevents removal of `MEMBERS:PERMISSIONS` from last user who has it (prevent lockout)
4. All permission changes are logged in audit trail with timestamp and actor
5. API returns appropriate error codes (400, 401, 403, 404, 409) with clear messages following RFC 7807 Problem Details
   format
6. HATEOAS links expose permission management capabilities with affordances
7. Integration tests verify authorization and business rules for both endpoints
8. E2E tests demonstrate complete permission management flow (view and update)

## Related Changes

None (new capability)

## Questions and Open Issues

None at this time.
