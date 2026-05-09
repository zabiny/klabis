## ADDED Requirements

### Requirement: Action Buttons Visually Indicate Their Effect

The system SHALL render action buttons in the events list with a visual treatment (color / variant) that indicates the nature of the action, so users can scan the table quickly:

- Actions that create or publish content (e.g. registering for an event, publishing a draft event) SHALL use a primary / success treatment.
- Actions that destructively cancel or remove (e.g. cancelling an event) SHALL use a destructive treatment.
- Actions that reverse a previous user action (e.g. unregistering from an event) SHALL use a warning treatment.
- Actions that change content without destruction (e.g. editing, syncing from ORIS) SHALL use a neutral treatment.

The exact colors are determined by the application theme; the requirement is that the four categories are visually distinguishable.

#### Scenario: Register button stands out as the primary action

- **WHEN** a member views the events list and an active event with open registration is shown
- **THEN** the "Přihlásit se" button on that row uses the primary / success visual treatment

#### Scenario: Cancel event button is visually destructive

- **WHEN** a manager views the events list
- **THEN** the "Zrušit akci" button on each manageable row uses the destructive visual treatment

#### Scenario: Unregister button is visually warning

- **WHEN** a member views the events list and an event they are registered for is shown
- **THEN** the "Odhlásit se z akce" button on that row uses the warning visual treatment

#### Scenario: Edit and sync buttons are visually neutral

- **WHEN** a manager views the events list
- **THEN** the "Upravit" and "Synchronizovat" buttons use the neutral visual treatment

#### Scenario: Publish draft event button is visually primary

- **WHEN** a manager views a DRAFT event row
- **THEN** the "Publikovat" button uses the primary / success visual treatment
