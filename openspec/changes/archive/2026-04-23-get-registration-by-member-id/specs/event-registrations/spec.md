## MODIFIED Requirements

### Requirement: View Own Registration

The system SHALL allow members to view their own registration details including their SI card number. Event coordinators and managers SHALL be able to view any member's registration details including their SI card number.

#### Scenario: Member views their own registration

- **WHEN** authenticated member views their own registration for an event
- **THEN** the registration details are shown including SI card number and registration time
- **AND** unregister action is shown if unregistration is still allowed
- **AND** edit registration action is shown if registrations are still open

#### Scenario: Event coordinator views a specific member's registration

- **WHEN** authenticated event coordinator or manager views a member's registration for an event
- **THEN** the registration details are shown including the member's SI card number and registration time

#### Scenario: Own registration page shows not found when not registered

- **WHEN** authenticated member navigates to their own registration for an event they are not registered for
- **THEN** the system shows a not-found message

#### Scenario: Coordinator views registration for member who is not registered

- **WHEN** event coordinator navigates to a registration for a member who is not registered for the event
- **THEN** the system shows a not-found message

## REMOVED Requirements

### Requirement: View Own Registration via /me path

**Reason**: Replaced by `View Own Registration` with a stable, member-ID-addressed URL. The `/me` path was not content-addressable — the resource it returned depended on who was calling it rather than on the URL itself.

**Migration**: The registration detail page is now addressed by the member's ID. Frontend navigation follows the self link from HAL responses, which now points to `/{memberId}` instead of `/me`. No user-visible change — the page content and available actions remain the same.
