## ADDED Requirements

### Requirement: Main Menu Offers a "Členské příspěvky" Entry for Membership Administrators

The system SHALL include a single "Členské příspěvky" entry in the Administrace section of the main menu for members authorized to administer membership fees. Following the entry SHALL open the membership fees administration page. The system SHALL NOT present membership fee tiers and fee selection campaigns as two separate menu entries.

#### Scenario: Membership administrator sees one membership fees entry in Administrace

- **WHEN** a member with membership administration authorization views the desktop sidebar
- **THEN** the Administrace section shows a single "Členské příspěvky" entry
- **AND** no separate fee tier catalog or fee selection campaigns entries are shown

#### Scenario: Following the entry opens the membership fees page

- **WHEN** a membership administrator follows the "Členské příspěvky" entry
- **THEN** the membership fees administration page opens

#### Scenario: Member without membership administration does not see the entry

- **WHEN** a member without membership administration authorization views the main menu
- **THEN** the "Členské příspěvky" entry is not shown

## MODIFIED Requirements

### Requirement: Desktop Sidebar Splits Menu Into Main and Administrative Sections

On desktop devices, the system SHALL present the main menu as two separately-labelled sections: a main section containing everyday destinations, and an administrative section containing management destinations. The administrative section groups items such as training groups, category presets, family groups, and membership fees.

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
