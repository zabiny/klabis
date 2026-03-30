## MODIFIED Requirements

### Requirement: Membership Suspension Request

The system SHALL accept membership suspension requests with reason and optional note. Suspension takes effect immediately upon request. Before processing, the system SHALL check whether the member is the last owner of any user group and require resolution if so.

#### Scenario: Valid suspension request
- **WHEN** authenticated user with MEMBERS:UPDATE permission submits POST request to /api/members/{id}/suspend
- **AND** request contains valid suspension reason (ODHLASKA, PRESTUP, OTHER)
- **AND** the member is not the last owner of any user group
- **THEN** membership suspension is processed immediately
- **AND** HTTP 204 No Content status is returned
- **AND** response includes Location header pointing to the member resource
- **AND** suspendedAt is set to current timestamp

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

#### Scenario: Suspension of last owner of a training group
- **WHEN** authenticated user attempts to suspend a member who is the sole owner of a training group
- **THEN** the system returns a warning response listing the affected groups
- **AND** requires designation of a successor owner for each training group before suspension can proceed

#### Scenario: Suspension of last owner of a family or free group
- **WHEN** authenticated user attempts to suspend a member who is the sole owner of a family or free group
- **THEN** the system returns a warning response listing the affected groups
- **AND** requires either designation of a successor owner or dissolution of each affected group before suspension can proceed

#### Scenario: Suspension proceeds after group ownership resolved
- **WHEN** authenticated user resolves all group ownership conflicts (successors designated or groups dissolved)
- **AND** resubmits the suspension request
- **THEN** the suspension is processed normally
