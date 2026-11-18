# Spec Delta

## MODIFIED Requirements

### Requirement: Desktop Sidebar Splits Menu Into Main and Administrative Sections

On desktop devices, the system SHALL present the main menu as two separately-labelled sections: a main section containing everyday destinations, and an administrative section containing management destinations. The administrative section groups items such as event settings ("Nastavení akcí"), training groups, legal guardians ("Zákonní zástupci"), and membership fees.

#### Scenario: Manager sees the Administrace section with all authorized admin items

- **WHEN** a user with administrative authorizations views the desktop sidebar
- **THEN** the sidebar shows the main section with everyday destinations
- **AND** a separate Administrace section below it contains the administrative destinations the user is authorized for

#### Scenario: Regular member never sees the Administrace heading

- **WHEN** a user without any administrative authorizations views the desktop sidebar
- **THEN** the sidebar shows only the main section
- **AND** the Administrace heading is not rendered at all
- **AND** no empty-state or placeholder is shown where the Administrace section would otherwise appear

#### Scenario: Administrace section appears as soon as one admin item becomes available

- **WHEN** a user has authorization for at least one item that belongs to the administrative section
- **THEN** the Administrace section is rendered with only the authorized items
- **AND** the rest of the administrative items (for which the user has no authorization) are not shown

#### Scenario: Order within each section reflects the order the items arrive in

- **WHEN** the desktop sidebar renders menu items
- **THEN** items within each section appear in the order they were delivered by the root API response
- **AND** the main section is shown above the Administrace section

## ADDED Requirements

### Requirement: Main Menu Offers a "Nastavení akcí" Entry for Event Managers

The system SHALL include a single "Nastavení akcí" entry in the Administrace section for users authorized to manage events. Following the entry SHALL open the event settings page. The system SHALL NOT present disciplines, event types and category presets as separate menu entries.

#### Scenario: Event manager sees one event settings entry

- **WHEN** a user with EVENTS:MANAGE authority views the desktop sidebar
- **THEN** the Administrace section contains one entry "Nastavení akcí"
- **AND** there are no separate entries "Disciplíny", "Typy akcí" or "Šablony kategorií"

#### Scenario: Entry opens the event settings page

- **WHEN** a manager selects the entry "Nastavení akcí"
- **THEN** the event settings page opens
