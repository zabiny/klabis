## ADDED Requirements

### Requirement: Events List Offers Editing Own Registration

The events list SHALL offer an "Upravit přihlášku" action on the row of an event the current user is registered for, whenever editing that registration is allowed. The action opens the registration edit dialog. It is not shown on rows of events the user is not registered for, nor where editing is not allowed.

#### Scenario: Registered member edits their registration from the events list

- **GIVEN** a member is registered for an active event with an open registration window
- **WHEN** the member views the events list
- **THEN** the row of that event shows an "Upravit přihlášku" action next to the unregister action
- **AND** choosing it opens the edit dialog of the member's registration

#### Scenario: Row without registration has no edit action

- **GIVEN** a member is not registered for an event
- **WHEN** the member views the events list
- **THEN** the row of that event shows the register action and no "Upravit přihlášku" action

#### Scenario: Edit action is hidden when editing is not allowed

- **GIVEN** a member is registered for an event whose registration deadline has passed
- **WHEN** the member views the events list
- **THEN** the row of that event shows no "Upravit přihlášku" action
