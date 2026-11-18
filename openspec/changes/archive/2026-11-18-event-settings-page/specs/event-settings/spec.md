# Spec Delta

## Purpose

Sdružuje nastavení, která řídí fungování akcí (disciplíny, typy akcí a šablony kategorií), na jednu stránku „Nastavení akcí" se záložkami, aby je správci akcí našli na jednom místě.

## ADDED Requirements

### Requirement: Event Settings Page With Tabs

The system SHALL provide a page "Nastavení akcí" with tabs "Disciplíny", "Typy akcí" and "Šablony kategorií". Each tab SHALL show the content that was previously available on its own administration page.

#### Scenario: Manager opens event settings

- **WHEN** a user with EVENTS:MANAGE authority opens "Nastavení akcí" from the Administrace section
- **THEN** the page shows the tabs "Disciplíny", "Typy akcí" and "Šablony kategorií"
- **AND** the first tab "Disciplíny" is selected and shows the discipline catalog

#### Scenario: Manager switches between tabs

- **WHEN** a manager selects the tab "Typy akcí"
- **THEN** the list of event types is shown
- **AND** the browser address reflects the selected tab

#### Scenario: Selected tab survives page reload

- **GIVEN** a manager has selected the tab "Šablony kategorií"
- **WHEN** the manager reloads the page or opens the same address in a new browser tab
- **THEN** the tab "Šablony kategorií" is selected again

#### Scenario: Tab content works as before

- **WHEN** a manager creates an event type on the tab "Typy akcí"
- **THEN** the new type appears in the list on that tab
- **AND** the manager remains on the same tab

### Requirement: Event Settings Tabs Follow Authorization

The system SHALL show only the tabs whose content the current user is authorized to see, and SHALL NOT offer the page to a user who is authorized for none of them.

#### Scenario: User without EVENTS:MANAGE has no event settings

- **WHEN** a user without EVENTS:MANAGE authority views the main menu
- **THEN** no "Nastavení akcí" entry is shown
- **AND** opening an event settings address directly is denied

#### Scenario: Tab the user cannot access is hidden

- **GIVEN** a user is authorized for the event types and category presets but not for the discipline catalog
- **WHEN** the user opens "Nastavení akcí"
- **THEN** only the tabs "Typy akcí" and "Šablony kategorií" are shown
- **AND** the first available tab is selected

### Requirement: Previous Administration Addresses Lead to Event Settings

The system SHALL keep the previous addresses of the discipline catalog, event types and category presets working by opening the corresponding tab of "Nastavení akcí".

#### Scenario: Old bookmark opens the matching tab

- **WHEN** a manager opens a saved link to the former "Typy akcí" page
- **THEN** "Nastavení akcí" opens with the tab "Typy akcí" selected
