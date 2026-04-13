# Capability: Application Navigation

## Purpose

Defines how the application presents its main navigation menu and how navigation behaves after user actions such as form submissions. The menu is driven by authorization-gated API links; its layout adapts to the current device (desktop sidebar vs. mobile bottom bar).

## Requirements

### Requirement: Main Menu is Driven by Authorization-Gated Links

The system SHALL derive the application's main menu from the links returned by the root API response. Each menu entry SHALL correspond to a link the current user is authorized to receive; links the user is not authorized to receive SHALL NOT appear in the menu.

#### Scenario: User sees only menu items they are authorized for

- **WHEN** an authenticated user opens the application
- **THEN** the main menu shows a menu item for every destination the current user has authorization for
- **AND** destinations the user has no authorization for are not shown

#### Scenario: User with no authorized destinations sees an empty menu state

- **WHEN** an authenticated user has no authorization for any navigable destination
- **THEN** the main menu shows an empty-menu message instead of menu items

### Requirement: Desktop Sidebar Splits Menu Into Main and Administrative Sections

On desktop devices, the system SHALL present the main menu as two separately-labelled sections: a main section containing everyday destinations, and an administrative section containing management destinations. The administrative section groups items such as training groups, category presets, and family groups.

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
- **AND** administrative destinations (training groups, category presets, family groups) are not shown in the bottom navigation

#### Scenario: Regular member on a mobile device sees the same bottom nav as a manager

- **WHEN** a user without administrative authorizations uses the application on a mobile device
- **THEN** the bottom navigation bar shows the same everyday destinations it shows to a manager, filtered to only those the user is authorized for

### Requirement: Administrative Pages Are Currently Reachable Only on Desktop

The system SHALL acknowledge that administrative pages (training groups, category presets, family groups) are reachable only through the desktop sidebar. A mobile user — even one with administrative authorizations — SHALL NOT have a navigation affordance to these pages in the current release. This is an explicit, documented gap intended to be closed by a future change.

#### Scenario: Manager on mobile cannot navigate to an admin page through the menu

- **WHEN** a user with administrative authorizations is on a mobile device
- **AND** the user looks for an administrative destination in the bottom navigation
- **THEN** the destination is not present in the navigation
- **AND** the user has no alternative in-app affordance to reach the destination without switching to a desktop viewport

### Requirement: Successful Create Submissions Navigate to the New Resource

The system SHALL navigate the user to the newly created resource immediately after a successful create-style form submission. A create-style submission is any form submission that results in a `201 Created` response carrying a `Location` header that points at the new resource.

Edit submissions (`200 OK` / `204 No Content` without a `Location` header) and delete submissions SHALL NOT navigate the user elsewhere; they continue to refresh the current page.

#### Scenario: Manager creates a family group and lands on its detail page

- **WHEN** a user with MEMBERS:MANAGE permission submits the "create family group" form successfully
- **THEN** the system shows a success toast
- **AND** the user is navigated to the detail page of the newly created family group
- **AND** the family groups list the user came from is no longer visible

#### Scenario: Manager creates a training group and lands on its detail page

- **WHEN** a user with GROUPS:TRAINING permission submits the "create training group" form successfully
- **THEN** the user is navigated to the detail page of the newly created training group

#### Scenario: Member creates a free group and lands on its detail page

- **WHEN** an authenticated member submits the "create free group" form successfully
- **THEN** the user is navigated to the detail page of the newly created free group

#### Scenario: Manager edits an existing event and stays on the current page

- **WHEN** a user with EVENTS:MANAGE permission submits an edit form for an existing event
- **THEN** the current page is refreshed with the updated values
- **AND** the user is not navigated elsewhere

#### Scenario: Manager deletes a category preset and stays on the current page

- **WHEN** a user with EVENTS:MANAGE permission confirms deletion of a category preset
- **THEN** the preset is removed from the list
- **AND** the user stays on the category presets list page

#### Scenario: Successful create submission without a Location header does not navigate

- **WHEN** a create-style form submission succeeds but the backend response does not include a `Location` header
- **THEN** the user is not navigated elsewhere
- **AND** the current page is refreshed
