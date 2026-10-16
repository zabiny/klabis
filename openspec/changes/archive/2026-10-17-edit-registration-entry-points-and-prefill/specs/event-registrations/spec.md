## MODIFIED Requirements

### Requirement: Unregister from Event

The system SHALL allow members to cancel their event registration before the event date and before the registration deadline (if set). Users with the EVENTS:REGISTRATIONS authority SHALL additionally be able to cancel any member's registration under the same timing rules. A single unregister operation serves both cases: it names the member whose registration is cancelled, and the unregister action is offered wherever the user is allowed to use it. The unregister action is reached from the registration edit dialog, where it asks for confirmation; the event detail page does not offer a separate unregister button.

#### Scenario: Member unregisters before event date

- **WHEN** authenticated member chooses "Odhlásit" in the registration edit dialog for an event before the event date and confirms
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

## ADDED Requirements

### Requirement: Event Detail Offers Edit Registration

The event detail page SHALL offer an "Upravit přihlášku" action to a member registered for the event whenever editing that registration is allowed. The action opens the registration edit dialog. It is not shown to members who are not registered, nor where editing is not allowed.

#### Scenario: Registered member sees the edit action on the event detail

- **GIVEN** a member registered for an upcoming event whose registration can still be edited
- **WHEN** the member opens the event detail page
- **THEN** the page shows an "Upravit přihlášku" action
- **AND** the page shows no separate "Odhlásit se z akce" button

#### Scenario: Member unregisters through the edit dialog

- **GIVEN** a member opened the registration edit dialog from the event detail page
- **WHEN** the member chooses "Odhlásit" and confirms
- **THEN** the registration is cancelled
- **AND** the event detail page shows the register action again

#### Scenario: Edit action is hidden when the member is not registered

- **GIVEN** a member who is not registered for the event
- **WHEN** the member opens the event detail page
- **THEN** the page shows the register action and no "Upravit přihlášku" action

#### Scenario: Edit action is hidden when editing is not allowed

- **GIVEN** a member registered for an event whose registration deadline has passed
- **WHEN** the member opens the event detail page
- **THEN** the page shows no "Upravit přihlášku" action

### Requirement: Edit Registration Form Is Prefilled With Existing Values

The registration edit form SHALL open with every value of the existing registration filled in — category, SI card number, rental request, internal and ORIS notes, and shared transport and accommodation choices where the event offers them — regardless of which place the form was opened from. Values MUST come from the existing registration, never from the member's profile.

#### Scenario: Edit form shows the existing registration values

- **GIVEN** a member registered with category "W35", SI card number "123456", an internal note and shared transport ticked
- **WHEN** the member opens the registration edit dialog from the dashboard, the events list or the event detail page
- **THEN** the category field shows "W35"
- **AND** the SI card number, notes and shared transport choice show the registered values

#### Scenario: Edit form SI card number differs from the profile

- **GIVEN** a member registered with an SI card number different from the one in their profile
- **WHEN** the member opens the registration edit dialog
- **THEN** the SI card number field shows the registered number, not the profile number

#### Scenario: Saving without changes keeps the registration unchanged

- **WHEN** the member opens the registration edit dialog and saves without changing any value
- **THEN** the registration keeps its category, SI card number, notes and shared service choices
