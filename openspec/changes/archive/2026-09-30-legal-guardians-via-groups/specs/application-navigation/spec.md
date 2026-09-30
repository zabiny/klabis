## MODIFIED Requirements

### Requirement: Desktop Sidebar Splits Menu Into Main and Administrative Sections

On desktop devices, the system SHALL present the main menu as two separately-labelled sections: a main section containing everyday destinations, and an administrative section containing management destinations. The administrative section groups items such as training groups, category presets, legal guardians ("Zákonní zástupci"), and membership fees.

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

### Requirement: Mobile Bottom Navigation Shows Only Everyday Destinations

On mobile devices, the system SHALL render a bottom navigation bar containing only destinations from the main section. Administrative destinations SHALL NOT appear in the mobile bottom navigation.

#### Scenario: Manager on a mobile device sees only everyday destinations in the bottom nav

- **WHEN** a user with administrative authorizations uses the application on a mobile device
- **THEN** the bottom navigation bar shows the everyday destinations (home, calendar, events, members, groups)
- **AND** administrative destinations (training groups, category presets, legal guardians) are not shown in the bottom navigation

#### Scenario: Regular member on a mobile device sees the same bottom nav as a manager

- **WHEN** a user without administrative authorizations uses the application on a mobile device
- **THEN** the bottom navigation bar shows the same everyday destinations it shows to a manager, filtered to only those the user is authorized for

### Requirement: Administrative Pages Are Currently Reachable Only on Desktop

The system SHALL acknowledge that administrative pages (training groups, category presets, legal guardians) are reachable only through the desktop sidebar. A mobile user — even one with administrative authorizations — SHALL NOT have a navigation affordance to these pages in the current release. This is an explicit, documented gap intended to be closed by a future change.

#### Scenario: Manager on mobile cannot navigate to an admin page through the menu

- **WHEN** a user with administrative authorizations is on a mobile device
- **AND** the user looks for an administrative destination in the bottom navigation
- **THEN** the destination is not present in the navigation
- **AND** the user has no alternative in-app affordance to reach the destination without switching to a desktop viewport

## ADDED Requirements

### Requirement: Home Page Offers The User's Own Profile

The system SHALL offer a "Můj profil" entry on the home page to every logged-in user who has a profile: a club member is taken to their member detail, a non-member legal guardian to their legal guardian profile. Users without either profile do not see the entry.

#### Scenario: Member opens own profile from the home page

- **WHEN** a logged-in member clicks "Můj profil" on the home page
- **THEN** their member detail opens

#### Scenario: Non-member guardian opens own profile from the home page

- **WHEN** a logged-in non-member legal guardian clicks "Můj profil" on the home page
- **THEN** their legal guardian profile opens

#### Scenario: User without a profile does not see the entry

- **WHEN** the bootstrap administrator without a member or guardian profile opens the home page
- **THEN** no "Můj profil" entry is shown
