# Calendar Items Specification

## Purpose

Covers the calendar view where members see upcoming club activities. Calendar items are either created automatically from published events or manually by calendar managers. Defines how members browse the calendar, view item details, and how managers create, edit, and delete manual items.
## Requirements
### Requirement: View Calendar Items

The system SHALL allow authenticated members to view calendar items for a specified date range. The default view is the current month. The maximum range is 366 days.

#### Scenario: Member views calendar for a specific month

- **WHEN** authenticated member navigates to the calendar view for a specific month
- **THEN** all calendar items that intersect with that month are displayed
- **AND** items are sorted by start date ascending by default

#### Scenario: Calendar defaults to current month

- **WHEN** authenticated member opens the calendar without specifying a date range
- **THEN** the calendar shows items for the current month

#### Scenario: Date range exceeding one year shows error

- **WHEN** user requests a calendar view with a date range exceeding 366 days
- **THEN** the system shows an error that the date range must not exceed 366 days

#### Scenario: Calendar can be sorted

- **WHEN** user changes the sort order (e.g., by name descending)
- **THEN** the calendar items are reordered accordingly

#### Scenario: Invalid sort field shows error

- **WHEN** user attempts to sort by an unsupported field
- **THEN** the system shows an error listing the allowed sort fields

#### Scenario: Multi-day events span across month boundaries

- **WHEN** a calendar item starts in May and ends in June
- **AND** user views the June calendar
- **THEN** the item is included in the June view

#### Scenario: Calendar navigation shows next and previous month links

- **WHEN** user views the June 2026 calendar
- **THEN** a "next month" link navigates to July 2026
- **AND** a "previous month" link navigates to May 2026
- **AND** sort settings are preserved when navigating

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

### Requirement: Create Manual Calendar Item

The system SHALL allow users with CALENDAR:MANAGE permission to create manual calendar items. Name, start date, and end date are required. Description is optional; when provided, it SHALL NOT exceed 1000 characters. An empty or whitespace-only description submitted from the form SHALL be treated as no description.

#### Scenario: Manager creates a calendar item with a description

- **WHEN** user with CALENDAR:MANAGE permission submits the calendar item creation form with name, description, start date, and end date
- **THEN** the calendar item is created with the given description and appears in the calendar

#### Scenario: Manager creates a calendar item without a description

- **WHEN** user with CALENDAR:MANAGE permission submits the calendar item creation form with name, start date, and end date and leaves the description empty
- **THEN** the calendar item is created without a description and appears in the calendar

#### Scenario: Multi-day calendar item appears across all its dates

- **WHEN** user creates a calendar item spanning multiple days
- **THEN** the item appears in the calendar for all days in its range

#### Scenario: Create button not shown without permission

- **WHEN** user without CALENDAR:MANAGE permission views the calendar
- **THEN** no create calendar item button is shown

#### Scenario: Form shows validation errors for missing required fields

- **WHEN** user submits the calendar item form with a missing name, missing start date, missing end date, or invalid dates
- **THEN** the form shows inline validation errors

#### Scenario: Description exceeding length limit shows error

- **WHEN** user submits the calendar item form with a description longer than 1000 characters
- **THEN** the form shows an error that the description must not exceed 1000 characters

#### Scenario: End date before start date shows error

- **WHEN** user sets an end date before the start date
- **THEN** the form shows an error that the end date must be on or after the start date

### Requirement: Update Manual Calendar Item

The system SHALL allow users with CALENDAR:MANAGE permission to update manually created calendar items. Description remains optional on update; users may clear an existing description by submitting an empty value, and an empty or whitespace-only value SHALL be treated as no description. Event-linked calendar items cannot be manually edited.

#### Scenario: Manager updates a manual calendar item

- **WHEN** user with CALENDAR:MANAGE permission edits and saves a manual calendar item
- **THEN** the calendar item is updated with the new values

#### Scenario: Manager clears the description of a manual calendar item

- **WHEN** user with CALENDAR:MANAGE permission edits a manual calendar item and submits it with the description field cleared
- **THEN** the calendar item is saved without a description

#### Scenario: Event-linked calendar item cannot be manually edited

- **WHEN** user with CALENDAR:MANAGE permission attempts to edit an event-linked calendar item
- **THEN** the system shows an error that event-linked items cannot be manually edited

#### Scenario: Edit action not shown without permission

- **WHEN** user without CALENDAR:MANAGE permission views a calendar item
- **THEN** no edit action is available

### Requirement: Delete Manual Calendar Item

The system SHALL allow users with CALENDAR:MANAGE permission to delete manually created calendar items. Event-linked calendar items cannot be manually deleted.

#### Scenario: Manager deletes a manual calendar item

- **WHEN** user with CALENDAR:MANAGE permission confirms deletion of a manual calendar item
- **THEN** the item is removed from the calendar

#### Scenario: Event-linked calendar item cannot be manually deleted

- **WHEN** user with CALENDAR:MANAGE permission attempts to delete an event-linked calendar item
- **THEN** the system shows an error that event-linked items cannot be manually deleted

#### Scenario: Delete action not shown without permission

- **WHEN** user without CALENDAR:MANAGE permission views a calendar item
- **THEN** no delete action is available

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

