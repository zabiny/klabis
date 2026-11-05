## MODIFIED Requirements

### Requirement: Event Categories

The system SHALL support a list of race categories on an event through the **Categories facet**. Each category has a name (e.g., "M21", "W35", "D10") representing an age/gender group available for that event, and MAY have its own entry fee. A category keeps a stable identity independent of its name, so renaming a category does not affect members already registered for it. Categories exist only when the Categories facet is active; an event without the Categories facet has no categories. When the facet is active, its category list MAY be empty (for example, while the event is still a draft). Category names SHALL be unique within an event.

#### Scenario: Event created with categories

- **WHEN** event manager creates an event whose type pre-fills the Categories facet and enters a list of categories in it
- **THEN** the event is saved with the Categories facet active and the specified categories

#### Scenario: Event created without categories

- **WHEN** event manager creates an event whose type has no Categories facet and does not activate it
- **THEN** the event is saved with no categories and no categories section

#### Scenario: Event updated with categories

- **GIVEN** an event with the Categories facet active
- **WHEN** event manager edits the Categories facet and modifies the categories list
- **THEN** the event is saved with the updated categories

#### Scenario: Renaming a category keeps existing registrations attached

- **GIVEN** an event has a category with members registered for it
- **WHEN** event manager renames that category in the Categories facet
- **THEN** the registrations remain attached to the renamed category
- **AND** the registration list shows the new category name for those members

#### Scenario: Category name must be unique within an event

- **WHEN** event manager submits a Categories facet whose category list contains the same name twice
- **THEN** the form shows an error that category names must be unique

#### Scenario: Removing a category that has registrations

- **GIVEN** an event has a category with members registered for it
- **WHEN** event manager removes that category from the Categories facet and saves
- **THEN** the facet is saved without the category
- **AND** the affected registrations are preserved with no category assigned
- **AND** the event manager is informed how many registrations were affected

#### Scenario: Categories displayed on event detail page

- **WHEN** user views the detail page for an event with the Categories facet active and categories defined
- **THEN** the categories are displayed as individual pills/tags in the categories section

#### Scenario: No categories row when event has no categories

- **WHEN** user views the detail page for an event without the Categories facet
- **THEN** no categories section is shown

#### Scenario: Categories editable inline on detail page

- **GIVEN** an event with the Categories facet active
- **WHEN** event manager clicks edit on the categories section
- **THEN** the categories field is editable allowing to add or remove individual category entries

### Requirement: ORIS Import Includes Categories

The system SHALL import event categories from ORIS by activating and filling the Categories facet. Categories are extracted from the event's class definitions provided by ORIS, and each imported category retains its ORIS origin so that later synchronizations can match it reliably.

#### Scenario: Import event with categories from ORIS

- **WHEN** event manager imports an event from ORIS that has class definitions
- **THEN** the imported event has the Categories facet active with categories corresponding to the ORIS class names

#### Scenario: Import event without categories from ORIS

- **WHEN** event manager imports an event from ORIS that has no class definitions
- **THEN** the imported event has the Categories facet active with an empty category list

### Requirement: Sync Event from ORIS

The system SHALL allow users with EVENTS:MANAGE permission to manually synchronize an ORIS-imported event, re-fetching all data from ORIS and overwriting local values including the categories held in the Categories facet. Categories originating from ORIS are matched against the incoming data by their ORIS origin rather than by name, so a category renamed in ORIS is updated in place instead of being replaced. Categories added manually to an ORIS-imported event are not removed by synchronization.

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

