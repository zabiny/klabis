# Member Termination Specification

## Purpose

This specification defines requirements for terminating club membership based on resignation request. It encompasses the complete termination workflow including reason tracking, timestamp recording, and audit trail creation.

## Requirements

### Requirement: Membership Termination Request

The system SHALL accept membership termination requests with reason and optional note. Termination takes effect immediately upon request.

#### Scenario: Valid termination request

- **WHEN** authenticated user with MEMBERS:UPDATE permission submits POST request to /api/members/{id}/terminate
- **AND** request contains valid deactivation reason (ODHLASKA, PRESTUP, OTHER)
- **THEN** membership termination is processed immediately
- **AND** HTTP 200 OK status is returned
- **AND** response includes updated member resource with termination details
- **AND** deactivatedAt is set to current timestamp

#### Scenario: Missing required reason field

- **WHEN** authenticated user submits termination request without reason field
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates reason is required
- **AND** no changes are made to the member

#### Scenario: Invalid deactivation reason

- **WHEN** authenticated user submits termination request with invalid reason value
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message lists valid reason values (ODHLASKA, PRESTUP, OTHER)

#### Scenario: Unauthorized user attempts termination

- **WHEN** authenticated user without MEMBERS:UPDATE permission attempts to terminate membership
- **THEN** HTTP 403 Forbidden is returned
- **AND** error response indicates insufficient permissions

### Requirement: Membership Status Update

The system SHALL update member status from active to inactive upon successful termination.

#### Scenario: Member status changes to inactive

- **WHEN** membership termination is processed successfully
- **THEN** member active status is set to false
- **AND** deactivation reason is stored
- **AND** deactivation timestamp is stored
- **AND** deactivation note is stored if provided
- **AND** terminator user ID is stored

#### Scenario: Already terminated member termination attempt

- **WHEN** user attempts to terminate a member that is already inactive
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates member is already terminated
- **AND** no changes are made to the member

#### Scenario: Concurrent termination attempts

- **WHEN** two users attempt to terminate the same member simultaneously
- **THEN** the first termination succeeds
- **AND** the second termination receives HTTP 409 Conflict
- **AND** error message indicates member was already terminated

### Requirement: Termination Response Format

The system SHALL return complete termination information in HATEOAS-compliant HAL+FORMS format.

#### Scenario: Response includes termination details

- **WHEN** membership termination is successful
- **THEN** response Content-Type is application/prs.hal-forms+json
- **AND** response includes all member fields with updated status
- **AND** response includes deactivationReason (ODHLASKA, PRESTUP, OTHER)
- **AND** response includes deactivatedAt as ISO-8601 datetime string
- **AND** response includes deactivationNote if provided
- **AND** response includes deactivatedBy (user ID of terminator)

#### Scenario: Response includes hypermedia links

- **WHEN** membership termination succeeds
- **THEN** response includes `self` link to the updated member resource
- **AND** response includes `collection` link to members list
- **AND** response does NOT include `terminate` link (member already terminated)

### Requirement: Domain Event Publishing

The system SHALL publish a domain event upon successful membership termination for integration with other modules.

#### Scenario: MemberTerminatedEvent is published

- **WHEN** membership termination is successfully committed to database
- **THEN** MemberTerminatedEvent is published
- **AND** event contains memberId
- **AND** event contains deactivationReason
- **AND** event contains deactivatedAt timestamp
- **AND** event contains terminatedBy user ID

#### Scenario: Event publishing failure does not block termination

- **WHEN** membership termination succeeds but event publishing fails
- **THEN** member termination is committed
- **AND** event failure is logged for retry
- **AND** response indicates member was terminated successfully
