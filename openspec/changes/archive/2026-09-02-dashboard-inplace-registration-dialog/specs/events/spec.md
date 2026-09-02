## ADDED Requirements

### Requirement: Event List Rows Expose Shared-Offer Flags

The event list representation (used by the events table rows and the dashboard closing-deadlines widget rows) SHALL include, for each event, whether the shared transport offer and the shared accommodation offer are turned on, so that a registration form opened from a list row can show each shared-service choice exactly when the event offers it (per the event-registrations specification).

#### Scenario: Event list rows carry the shared-offer flags

- **GIVEN** an event with the shared transport offer on and the shared accommodation offer off
- **WHEN** the events list is fetched
- **THEN** that event's list row shows the shared transport offer as on
- **AND** the same row shows the shared accommodation offer as off

#### Scenario: Registration dialog opened from a list row shows choices per the offers

- **GIVEN** a list row (events table or dashboard closing-deadlines widget) of an event with the shared accommodation offer on and the shared transport offer off
- **WHEN** a member opens the registration dialog from that row
- **THEN** the dialog shows the shared accommodation tick box
- **AND** no shared transport tick box is shown
