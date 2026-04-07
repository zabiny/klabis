# Event Categories Specification

## Purpose

Covers how race categories (age/gender classes like M21, W35, D10) are associated with events. Defines how categories are stored, edited, imported from ORIS, and synchronized.

## Requirements

### Requirement: Event Categories

The system SHALL support a list of race categories on each event. Categories are string names (e.g., "M21", "W35", "D10") representing age/gender groups available for that event. Categories are optional — an event may have no categories defined.

#### Scenario: Event created with categories

- **WHEN** event manager creates an event and specifies a list of categories
- **THEN** the event is saved with the specified categories

#### Scenario: Event created without categories

- **WHEN** event manager creates an event without specifying any categories
- **THEN** the event is saved with an empty category list

#### Scenario: Event updated with categories

- **WHEN** event manager edits an event and modifies the categories list
- **THEN** the event is saved with the updated categories

#### Scenario: Categories displayed on event detail page

- **WHEN** user views the detail page for an event with categories defined
- **THEN** the categories are displayed as individual pills/tags in the event information section

#### Scenario: No categories row when event has no categories

- **WHEN** user views the detail page for an event without any categories
- **THEN** no categories row is shown in the event information section

#### Scenario: Categories editable inline on detail page

- **WHEN** event manager clicks edit on an event detail page
- **THEN** the categories field is editable allowing to add or remove individual category entries

### Requirement: ORIS Import Includes Categories

The system SHALL import event categories from ORIS. Categories are extracted from the event's class definitions provided by ORIS.

#### Scenario: Import event with categories from ORIS

- **WHEN** event manager imports an event from ORIS that has class definitions
- **THEN** the imported event contains categories corresponding to the ORIS class names

#### Scenario: Import event without categories from ORIS

- **WHEN** event manager imports an event from ORIS that has no class definitions
- **THEN** the imported event has an empty category list

### Requirement: Sync Event from ORIS

The system SHALL allow users with EVENTS:MANAGE permission to manually synchronize an ORIS-imported event, re-fetching all data from ORIS and overwriting local values including categories.

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
- **THEN** the category is removed from the event's category list
- **AND** the existing registrations referencing that category are preserved
- **AND** a warning is logged
