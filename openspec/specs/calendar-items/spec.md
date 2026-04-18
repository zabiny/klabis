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
