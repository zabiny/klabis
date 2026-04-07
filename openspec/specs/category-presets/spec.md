# Category Presets Specification

## Purpose

Covers management of reusable category preset templates that event managers can apply to events for quick category population.

## Requirements

### Requirement: Category Preset Management

The system SHALL allow users with EVENTS:MANAGE permission to manage category presets. A preset consists of a name and a list of category strings. Presets provide a convenient way to quickly populate event categories with predefined sets.

#### Scenario: Manager creates a category preset

- **WHEN** user with EVENTS:MANAGE permission navigates to the category presets page and creates a new preset with a name and list of categories
- **THEN** the preset is saved and appears in the presets list

#### Scenario: Manager edits a category preset

- **WHEN** user with EVENTS:MANAGE permission edits an existing preset's name or categories
- **THEN** the preset is updated
- **AND** existing events that previously used this preset are not affected

#### Scenario: Manager deletes a category preset

- **WHEN** user with EVENTS:MANAGE permission deletes a category preset
- **THEN** the preset is removed from the system
- **AND** existing events that previously used this preset are not affected

#### Scenario: Manager views category presets list

- **WHEN** user with EVENTS:MANAGE permission navigates to the category presets page
- **THEN** all presets are listed with their names and category counts

#### Scenario: Presets page not accessible without permission

- **WHEN** user without EVENTS:MANAGE permission attempts to access the category presets page
- **THEN** access is denied

### Requirement: Apply Preset to Event

The system SHALL allow event managers to apply a category preset when creating or editing an event. Applying a preset copies the preset's categories to the event — no ongoing link is maintained.

#### Scenario: Manager applies preset during event creation

- **WHEN** event manager selects a category preset while creating an event
- **THEN** the event's categories are populated with the preset's category list
- **AND** the manager can further modify the categories before saving

#### Scenario: Manager applies preset during event editing

- **WHEN** event manager selects a category preset while editing an existing event
- **THEN** the event's current categories are replaced with the preset's category list
- **AND** the manager can further modify the categories before saving

#### Scenario: Changing preset does not affect existing events

- **WHEN** a category preset is modified after being applied to events
- **THEN** the events that previously used the preset retain their original categories unchanged
