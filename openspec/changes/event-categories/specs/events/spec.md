## MODIFIED Requirements

### Requirement: Create Event

The system SHALL allow users with EVENTS:MANAGE permission to create events. Required fields: name, event date, location, organizer code. Optional: website URL, coordinator, registration deadline, categories.

#### Scenario: Manager creates an event with all required fields

- **WHEN** user with EVENTS:MANAGE permission submits the event creation form with all required fields
- **THEN** the event is created in DRAFT status
- **AND** appears in the event list

#### Scenario: Manager creates an event with optional fields

- **WHEN** user with EVENTS:MANAGE permission fills in optional fields (website URL, coordinator, registration deadline, categories) and submits
- **THEN** the event is created with all provided data

#### Scenario: Create event button not shown without permission

- **WHEN** user without EVENTS:MANAGE permission views the events list
- **THEN** no create event button is shown

#### Scenario: Form shows validation errors for invalid data

- **WHEN** user submits the event form with missing required fields or invalid formats
- **THEN** the form shows inline validation errors for each issue

#### Scenario: Invalid website URL shows error

- **WHEN** user enters a website URL that is not a valid http/https URL
- **THEN** the form shows an error that the URL must be a valid web address

#### Scenario: Non-existent coordinator shows error

- **WHEN** user references a coordinator member that does not exist
- **THEN** the form shows an error that the coordinator was not found

#### Scenario: Registration deadline after event date shows error

- **WHEN** user sets a registration deadline after the event date
- **THEN** the form shows an error that the deadline must be on or before the event date

### Requirement: Update Event

The system SHALL allow users with EVENTS:MANAGE permission to update events in DRAFT or ACTIVE status. Editable fields include categories.

#### Scenario: Manager updates a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission edits and saves a DRAFT event
- **THEN** the event is updated with the new values

#### Scenario: Manager updates an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission edits and saves an ACTIVE event
- **THEN** the event is updated with the new values

#### Scenario: Finished event cannot be edited

- **WHEN** user attempts to edit a FINISHED event
- **THEN** the system shows an error that finished events cannot be modified

#### Scenario: Cancelled event cannot be edited

- **WHEN** user attempts to edit a CANCELLED event
- **THEN** the system shows an error that cancelled events cannot be modified

#### Scenario: Update action not shown without permission

- **WHEN** user without EVENTS:MANAGE permission views an event
- **THEN** no edit action is available

### Requirement: Event Detail Page

The application SHALL display the event detail page with categories (when defined) and allow managers to edit them inline.

#### Scenario: Event detail shows registration deadline

- **WHEN** user views the detail page for an event with a registration deadline set
- **THEN** the event information section shows the registration deadline as a formatted date

#### Scenario: Event detail hides registration deadline when not set

- **WHEN** user views the detail page for an event without a registration deadline
- **THEN** no registration deadline row is shown in the event information section

#### Scenario: Inline edit includes registration deadline field

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the registration deadline field is editable as a date picker

#### Scenario: Event create/edit form includes registration deadline

- **WHEN** a manager creates or edits an event via the form
- **THEN** the form includes a registration deadline date picker field

#### Scenario: Event detail shows categories

- **WHEN** user views the detail page for an event with categories defined
- **THEN** the categories are displayed as individual pills/tags

#### Scenario: Event detail hides categories when not set

- **WHEN** user views the detail page for an event without categories
- **THEN** no categories row is shown

#### Scenario: Inline edit includes categories field

- **WHEN** a manager edits an event inline on the detail page
- **THEN** the categories field is editable

### Requirement: Get Event Detail

The system SHALL display complete event detail including categories. DRAFT events are only visible to users with EVENTS:MANAGE permission.

#### Scenario: User views event detail

- **WHEN** authenticated user navigates to an event detail page
- **THEN** all event information is displayed (name, date, location, organizer, website, coordinator, registration deadline, categories)

#### Scenario: Regular user cannot access DRAFT event detail

- **WHEN** user without EVENTS:MANAGE permission navigates to a DRAFT event's detail page
- **THEN** the page shows not found

#### Scenario: Manager views DRAFT event detail

- **WHEN** user with EVENTS:MANAGE permission navigates to a DRAFT event's detail page
- **THEN** the full event detail is displayed

#### Scenario: DRAFT event actions available to manager

- **WHEN** user with EVENTS:MANAGE permission views a DRAFT event
- **THEN** available actions are: edit, publish, cancel, sync from ORIS (if ORIS-imported)

#### Scenario: ACTIVE event actions available to manager

- **WHEN** user with EVENTS:MANAGE permission views an ACTIVE event
- **THEN** available actions are: edit, cancel, finish, sync from ORIS (if ORIS-imported)

#### Scenario: FINISHED or CANCELLED event has no management actions

- **WHEN** user views a FINISHED or CANCELLED event
- **THEN** no edit, publish, cancel, finish, or sync actions are available

#### Scenario: Event detail shows registration deadline

- **WHEN** user views event detail for an event with a registration deadline
- **THEN** the registration deadline is displayed

#### Scenario: Event detail without registration deadline

- **WHEN** user views event detail for an event without a registration deadline
- **THEN** no registration deadline row is shown
