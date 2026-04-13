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

- **WHEN** user views a calendar item that is linked to an event
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
