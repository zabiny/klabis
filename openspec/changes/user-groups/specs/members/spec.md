## MODIFIED Requirements

### Requirement: Membership Suspension Request

The system SHALL accept membership suspension requests with reason and optional note. Suspension takes effect immediately upon request. The system SHALL check whether the member is the last owner of any user group and warn accordingly before proceeding.

#### Scenario: Valid suspension request

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits POST request to /api/members/{id}/suspend
- **AND** request contains valid suspension reason (ODHLASKA, PRESTUP, OTHER)
- **AND** the member is not the last owner of any user group
- **THEN** membership suspension is processed immediately
- **AND** HTTP 204 No Content status is returned
- **AND** response includes Location header pointing to the member resource
- **AND** suspendedAt is set to current timestamp

#### Scenario: Suspension of last owner of a training group

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits suspension request for a member
- **AND** the member is the last owner of a training group
- **AND** no new owner has been designated
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates the member is the last owner of a training group and a new owner must be designated before suspension

#### Scenario: Suspension of last owner of a family or free group

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits suspension request for a member
- **AND** the member is the last owner of a family or free group
- **AND** no resolution has been provided (new owner or dissolution)
- **THEN** HTTP 409 Conflict is returned
- **AND** error message indicates the member is the last owner of a group and a new owner must be designated or the group must be dissolved before suspension

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
