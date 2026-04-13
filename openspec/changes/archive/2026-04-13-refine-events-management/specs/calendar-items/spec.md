## MODIFIED Requirements

### Requirement: Automatic Synchronization from Events

The system SHALL automatically create, update, and delete calendar items when events are published, updated, or cancelled. When the originating event has no location, the generated calendar item is created without location information — the calendar item's description is assembled only from the values that are actually available (location, organizer, website), and is left empty when none of these are present.

#### Scenario: Calendar item created when event is published

- **WHEN** an event is published (transitions from DRAFT to ACTIVE)
- **THEN** a calendar item is automatically created with the event's name and date
- **AND** the calendar item's description is assembled from the event's location (when set), organizer, and optional website
- **AND** the calendar item is read-only (cannot be manually edited or deleted)

#### Scenario: Calendar item created from event without a location

- **WHEN** an event is published that has no location
- **THEN** a calendar item is automatically created with the event's name and date
- **AND** the calendar item's description contains the organizer (and website when present) without any location prefix

#### Scenario: Calendar item updated when event is updated

- **WHEN** an event's name, date, location, or organizer is updated
- **THEN** the linked calendar item is automatically updated with the new values
- **AND** the calendar item's description reflects only the values currently present on the event

#### Scenario: Calendar item deleted when event is cancelled

- **WHEN** an event is cancelled
- **THEN** the linked calendar item is automatically removed from the calendar

#### Scenario: Finishing an event does not affect the calendar item

- **WHEN** an event transitions from ACTIVE to FINISHED
- **THEN** the linked calendar item remains visible on the calendar
