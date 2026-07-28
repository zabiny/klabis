## MODIFIED Requirements

### Requirement: Event Categories

The system SHALL support a list of race categories on each event. Each category has a name (e.g., "M21", "W35", "D10") representing an age/gender group available for that event, and MAY have its own entry fee. A category keeps a stable identity independent of its name, so renaming a category does not affect members already registered for it. Categories are optional — an event may have no categories defined. Category names SHALL be unique within an event.

#### Scenario: Event created with categories

- **WHEN** event manager creates an event and specifies a list of categories
- **THEN** the event is saved with the specified categories

#### Scenario: Event created without categories

- **WHEN** event manager creates an event without specifying any categories
- **THEN** the event is saved with an empty category list

#### Scenario: Event updated with categories

- **WHEN** event manager edits an event and modifies the categories list
- **THEN** the event is saved with the updated categories

#### Scenario: Renaming a category keeps existing registrations attached

- **GIVEN** an event has a category with members registered for it
- **WHEN** event manager renames that category
- **THEN** the registrations remain attached to the renamed category
- **AND** the registration list shows the new category name for those members

#### Scenario: Category name must be unique within an event

- **WHEN** event manager submits an event whose category list contains the same name twice
- **THEN** the form shows an error that category names must be unique

#### Scenario: Removing a category that has registrations

- **GIVEN** an event has a category with members registered for it
- **WHEN** event manager removes that category and saves the event
- **THEN** the event is saved without the category
- **AND** the affected registrations are preserved with no category assigned
- **AND** the event manager is informed how many registrations were affected

#### Scenario: Categories displayed on event detail page

- **WHEN** user views the detail page for an event with categories defined
- **THEN** the categories are displayed as individual pills/tags in the event information section

#### Scenario: No categories row when event has no categories

- **WHEN** user views the detail page for an event without any categories
- **THEN** no categories row is shown in the event information section

#### Scenario: Categories editable inline on detail page

- **WHEN** event manager clicks edit on an event detail page
- **THEN** the categories field is editable allowing to add or remove individual category entries

### Requirement: Sync Event from ORIS

The system SHALL allow users with EVENTS:MANAGE permission to manually synchronize an ORIS-imported event, re-fetching all data from ORIS and overwriting local values including categories. Categories originating from ORIS are matched against the incoming data by their ORIS origin rather than by name, so a category renamed in ORIS is updated in place instead of being replaced. Categories added manually to an ORIS-imported event are not removed by synchronization.

#### Scenario: Manager syncs an event from ORIS

- **WHEN** event manager clicks the "Sync from ORIS" action on an ORIS-imported event in DRAFT or ACTIVE status
- **THEN** the event data is refreshed from ORIS (name, date, location, organizer, website, registration deadline, categories)
- **AND** local changes are overwritten with ORIS data

#### Scenario: Sync action available only for ORIS-imported events

- **WHEN** user views an event that was created manually (not imported from ORIS)
- **THEN** the "Sync from ORIS" action is not available

#### Scenario: Sync action available only for editable events

- **WHEN** user views an ORIS-imported event in FINISHED or CANCELLED status
- **THEN** the "Sync from ORIS" action is not available

#### Scenario: Sync action not available when ORIS integration is inactive

- **WHEN** the ORIS integration is not active in the system
- **THEN** the "Sync from ORIS" action is not available on any event

#### Scenario: Sync renames a category that has registrations

- **GIVEN** an ORIS-imported event has a category with members registered for it
- **WHEN** event manager syncs from ORIS
- **AND** ORIS provides that same category under a changed name
- **THEN** the category name is updated
- **AND** the existing registrations remain attached to it

#### Scenario: Sync removes a category that has registrations

- **WHEN** event manager syncs from ORIS
- **AND** ORIS no longer includes a category that members have registered for
- **THEN** the category is removed from the event's category list
- **AND** the existing registrations are preserved with no category assigned
- **AND** a warning is logged

#### Scenario: Sync keeps manually added categories

- **GIVEN** an ORIS-imported event has a category that was added manually by the event manager
- **WHEN** event manager syncs from ORIS
- **THEN** the manually added category remains on the event

### Requirement: ORIS Import Includes Categories

The system SHALL import event categories from ORIS. Categories are extracted from the event's class definitions provided by ORIS, and each imported category retains its ORIS origin so that later synchronizations can match it reliably.

#### Scenario: Import event with categories from ORIS

- **WHEN** event manager imports an event from ORIS that has class definitions
- **THEN** the imported event contains categories corresponding to the ORIS class names

#### Scenario: Import event without categories from ORIS

- **WHEN** event manager imports an event from ORIS that has no class definitions
- **THEN** the imported event has an empty category list

## ADDED Requirements

### Requirement: Category Entry Fee

The system SHALL allow an event category to define its own entry fee. When a category has its own fee, that fee SHALL apply to members registered in the category instead of the event's base entry fee. Categories without their own fee SHALL use the event's base entry fee.

#### Scenario: Event manager sets a fee on a category

- **WHEN** event manager edits an event and sets an entry fee on one of its categories
- **THEN** the event is saved with the category fee
- **AND** the category fee is shown alongside the category on the event detail page

#### Scenario: Category without its own fee

- **WHEN** event manager saves a category without specifying an entry fee
- **THEN** the category uses the event's base entry fee
