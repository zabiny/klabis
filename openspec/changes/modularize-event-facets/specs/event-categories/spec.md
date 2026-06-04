## MODIFIED Requirements

### Requirement: Event Categories

The system SHALL support a list of race categories on an event through the **Categories facet**. Categories are string names (e.g., "M21", "W35", "D10") representing age/gender groups available for that event. Categories exist only when the Categories facet is active; an event without the Categories facet has no categories. When the facet is active, its category list MAY be empty (for example, while the event is still a draft).

#### Scenario: Event created with the Categories facet and categories

- **WHEN** event manager creates an event whose type pre-fills the Categories facet, and specifies a list of categories
- **THEN** the event is saved with the Categories facet active and the specified categories

#### Scenario: Event created without the Categories facet

- **WHEN** event manager creates an event whose type has no Categories facet, and adds no Categories facet
- **THEN** the event is saved with no categories and no categories section

#### Scenario: Event updated with categories

- **GIVEN** an event with the Categories facet active
- **WHEN** event manager edits the Categories facet and modifies the categories list
- **THEN** the event is saved with the updated categories

#### Scenario: Categories displayed on event detail page

- **WHEN** user views the detail page for an event with the Categories facet active and categories defined
- **THEN** the categories are displayed as individual pills/tags in the categories section

#### Scenario: No categories section when the facet is not active

- **WHEN** user views the detail page for an event without the Categories facet
- **THEN** no categories section is shown

#### Scenario: Categories editable inline on detail page

- **GIVEN** an event with the Categories facet active
- **WHEN** event manager clicks edit on the categories section
- **THEN** the categories field is editable allowing to add or remove individual category entries

### Requirement: ORIS Import Includes Categories

The system SHALL import event categories from ORIS by activating and filling the Categories facet. Categories are extracted from the event's class definitions provided by ORIS.

#### Scenario: Import event with categories from ORIS

- **WHEN** event manager imports an event from ORIS that has class definitions
- **THEN** the imported event has the Categories facet active with categories corresponding to the ORIS class names

#### Scenario: Import event without categories from ORIS

- **WHEN** event manager imports an event from ORIS that has no class definitions
- **THEN** the imported event has the Categories facet active with an empty category list

### Requirement: Sync Event from ORIS

The system SHALL allow users with EVENTS:MANAGE permission to manually synchronize an ORIS-imported event, re-fetching all data from ORIS and overwriting local values, including the Categories facet.

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

#### Scenario: Sync removes a category that has registrations

- **WHEN** event manager syncs from ORIS
- **AND** ORIS no longer includes a category that members have registered for
- **THEN** the category is removed from the event's Categories facet
- **AND** the existing registrations referencing that category are preserved
- **AND** a warning is logged
