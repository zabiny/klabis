<!--
This change is primarily an internal refactor. It restructures how the calendar
capability represents its two kinds of items (manual vs. event-linked) without
changing what users see.

Alongside the refactor, two existing requirements are re-worded to no longer
assume "at most one event-linked calendar item per event". The refactor itself
does not introduce a second event-linked item (that is the follow-up change),
but the refactor is what makes such an extension possible, and the spec should
stop asserting a one-to-one invariant that the codebase is about to outgrow.

All existing scenarios are preserved verbatim; only the normative wording above
each scenario block is adjusted.
-->

## MODIFIED Requirements

### Requirement: Get Calendar Item Detail

The system SHALL allow authenticated members to view detailed information about a specific calendar item.

#### Scenario: Member views calendar item detail

- **WHEN** authenticated member opens a calendar item's detail
- **THEN** all available fields are displayed: name, start date, end date
- **AND** the description is displayed only when the item has one

#### Scenario: Member views a calendar item without a description

- **WHEN** authenticated member opens the detail of a calendar item that has no description
- **THEN** the detail view shows the item without any description section or placeholder text

#### Scenario: Event-linked calendar item shows link to event

- **WHEN** user views a calendar item that originates from an event
- **THEN** a link to the related event is shown
- **AND** no edit or delete options are available (item is read-only)

#### Scenario: Manual calendar item shows edit and delete for authorized user

- **WHEN** user with CALENDAR:MANAGE permission views a manually created calendar item
- **THEN** edit and delete actions are available

#### Scenario: Manual calendar item shows no edit/delete without permission

- **WHEN** user without CALENDAR:MANAGE permission views a manually created calendar item
- **THEN** no edit or delete actions are available

### Requirement: Automatic Synchronization from Events

The system SHALL automatically create, update, and delete event-linked calendar items when events are published, updated, or cancelled. A published event MAY be represented by one or more event-linked calendar items in the calendar. When the originating event has no location, the generated calendar item is created without location information — the calendar item's description is assembled only from the values that are actually available (location, organizer, website), and is left empty when none of these are present.

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
- **THEN** every event-linked calendar item associated with that event is automatically updated to reflect the new values
- **AND** each calendar item's description reflects only the values currently present on the event

#### Scenario: Calendar item deleted when event is cancelled

- **WHEN** an event is cancelled
- **THEN** every event-linked calendar item associated with that event is automatically removed from the calendar

#### Scenario: Finishing an event does not affect the calendar item

- **WHEN** an event transitions from ACTIVE to FINISHED
- **THEN** every event-linked calendar item associated with that event remains visible on the calendar
