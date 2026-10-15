## MODIFIED Requirements

### Requirement: Unregister from Event

The system SHALL allow members to cancel their event registration before the event date and before the registration deadline (if set). Users with the EVENTS:REGISTRATIONS authority SHALL additionally be able to cancel any member's registration under the same timing rules. A single unregister operation serves both cases: it names the member whose registration is cancelled, and the unregister action is offered wherever the user is allowed to use it.

#### Scenario: Member unregisters before event date

- **WHEN** authenticated member clicks the unregister button for an event before the event date
- **AND** the registration deadline has not passed
- **THEN** the registration is cancelled
- **AND** the event row shows a register button again

#### Scenario: User with EVENTS:REGISTRATIONS unregisters another member

- **GIVEN** a member is registered for an event that has not started and whose registration deadline has not passed
- **WHEN** a user with EVENTS:REGISTRATIONS opens that member's registration and chooses "Odhlásit" and confirms
- **THEN** the member's registration is cancelled
- **AND** the registrations list no longer shows the member

#### Scenario: Unregister action is offered to a user with EVENTS:REGISTRATIONS on another member's registration

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** a user with EVENTS:REGISTRATIONS opens that member's registration
- **THEN** the registration shows the unregister action

#### Scenario: Member cannot unregister another member

- **GIVEN** a member without EVENTS:REGISTRATIONS
- **WHEN** they attempt to unregister a different member from an event
- **THEN** the system refuses the request as forbidden
- **AND** the other member's registration remains

#### Scenario: Event coordinator without EVENTS:REGISTRATIONS cannot unregister members

- **GIVEN** an event coordinator who does not hold EVENTS:REGISTRATIONS
- **WHEN** they open a member's registration for their event
- **THEN** no unregister action is offered

#### Scenario: Unregistration on or after event date not allowed

- **WHEN** member attempts to unregister on or after the event date
- **THEN** the system shows an error that unregistration is only allowed before the event date

#### Scenario: Unregistration after deadline not allowed

- **WHEN** member attempts to unregister from an event whose registration deadline has passed
- **THEN** the system shows an error that the registration deadline has passed

#### Scenario: Unregistration when not registered shows error

- **WHEN** member attempts to unregister from an event they are not registered for
- **THEN** the system shows an error that no registration was found

### Requirement: Registration Edit Dialog Offers Unregistering

When a user edits a registration that they are allowed to cancel — their own, or any member's for a user with EVENTS:REGISTRATIONS — the edit dialog SHALL offer an "Odhlásit" action. Choosing it SHALL lead to the same confirmation of unregistering as elsewhere in the application; the registration is cancelled only after confirmation. The action is not offered when unregistering is not allowed.

#### Scenario: Member unregisters from the edit dialog

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** the member opens the edit dialog of their registration and chooses "Odhlásit"
- **THEN** the edit dialog closes and the member is asked to confirm unregistering
- **AND** after confirming, the registration is cancelled and the list they came from shows the event as not registered

#### Scenario: Registration manager unregisters another member from the edit dialog

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** a user with EVENTS:REGISTRATIONS opens the edit dialog of that member's registration from the registrations list and chooses "Odhlásit"
- **THEN** they are asked to confirm unregistering
- **AND** after confirming, the registration is cancelled and the registrations list no longer shows the member

#### Scenario: Member declines to unregister

- **WHEN** the member chooses "Odhlásit" in the edit dialog and then dismisses the confirmation
- **THEN** the registration remains unchanged

#### Scenario: Unregister action is hidden when unregistering is not allowed

- **GIVEN** the registration deadline of the event has passed
- **WHEN** the member opens the edit dialog of their registration
- **THEN** no "Odhlásit" action is shown
