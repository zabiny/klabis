## MODIFIED Requirements

### Requirement: Automatic Synchronization from Events

The system SHALL automatically maintain event-linked calendar items so that they always reflect the current state of published events. For each published event there SHALL always be one calendar item representing the event date. When the event has a registration deadline set, there SHALL additionally be one calendar item representing the registration deadline, labelled "Přihlášky - {event name}" and dated on the deadline day. When the originating event has no location, the event-date item is created without location information — its description is assembled only from the values that are actually available (location, organizer, website), and is left empty when none of these are present. The registration-deadline item has no description. Whenever the event is updated, the set of event-linked calendar items SHALL be updated to match the current event state: items that should exist are created or updated, and items that should no longer exist are removed. When an event is cancelled, every calendar item linked to that event SHALL be removed.

#### Scenario: Event-date calendar item created when event is published

- **WHEN** an event is published (transitions from DRAFT to ACTIVE)
- **THEN** a calendar item is automatically created with the event's name and date
- **AND** the calendar item's description is assembled from the event's location (when set), organizer, and optional website
- **AND** the calendar item is read-only (cannot be manually edited or deleted)

#### Scenario: Event-date calendar item created from event without a location

- **WHEN** an event is published that has no location
- **THEN** a calendar item is automatically created with the event's name and date
- **AND** the calendar item's description contains the organizer (and website when present) without any location prefix

#### Scenario: Registration-deadline calendar item created when a published event has a deadline

- **WHEN** an event with a registration deadline set is published
- **THEN** in addition to the event-date item, a second calendar item is automatically created
- **AND** the second item is labelled "Přihlášky - {event name}"
- **AND** the second item is dated on the registration deadline
- **AND** the second item has no description
- **AND** clicking the second item navigates to the source event's detail page

#### Scenario: No registration-deadline item created when the event has no deadline

- **WHEN** an event with no registration deadline is published
- **THEN** only the event-date calendar item is created
- **AND** no second calendar item appears for that event

#### Scenario: Event-date and registration-deadline items both appear on the same day when they coincide

- **WHEN** an event is published whose registration deadline equals the event date
- **THEN** two separate calendar items appear on that day
- **AND** one is the event-date item labelled with the event's name
- **AND** the other is the registration-deadline item labelled "Přihlášky - {event name}"

#### Scenario: Calendar items are updated when the event is updated

- **WHEN** an event's name, date, location, organizer, website, or registration deadline is updated
- **THEN** every event-linked calendar item for that event is reconciled with the new event state
- **AND** the event-date item reflects the current event date, name, and assembled description
- **AND** the registration-deadline item reflects the current deadline date and event name in its label

#### Scenario: Registration-deadline item appears when a deadline is added to an event

- **GIVEN** a published event with no registration deadline and no deadline calendar item
- **WHEN** the event is updated to add a registration deadline
- **THEN** a registration-deadline calendar item is created on the new deadline date
- **AND** the event-date calendar item remains unchanged apart from the update propagation

#### Scenario: Registration-deadline item disappears when a deadline is cleared

- **GIVEN** a published event with a registration deadline and an existing deadline calendar item
- **WHEN** the event is updated to clear the registration deadline
- **THEN** the registration-deadline calendar item is removed
- **AND** the event-date calendar item remains

#### Scenario: Event-linked calendar items are created on event update when missing

- **GIVEN** a published event whose event-linked calendar items do not currently exist (for example because of a prior failure)
- **WHEN** the event is updated
- **THEN** the expected event-linked calendar items for the current event state are created
- **AND** no stale items are left behind

#### Scenario: All calendar items deleted when event is cancelled

- **WHEN** an event is cancelled
- **THEN** every event-linked calendar item for that event is removed from the calendar
- **AND** this includes both the event-date item and the registration-deadline item when present

#### Scenario: Finishing an event does not affect calendar items

- **WHEN** an event transitions from ACTIVE to FINISHED
- **THEN** every event-linked calendar item for that event remains visible on the calendar
