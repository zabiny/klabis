## MODIFIED Requirements

### Requirement: Member List

The system SHALL display a paginated, sortable and filterable list of members. Users with MEMBERS:MANAGE authority see all members including inactive ones and see the email column. Other users see only active members. By default, the list shows only active members for every user. The list can be filtered by a fulltext query over first name, last name, and registration number. Users with MEMBERS:MANAGE authority can additionally filter by status (Aktivní / Neaktivní / Vše). Multiple filters combine with AND semantics. The per-row active-status column is not shown to any user — status is reflected in the active filter selection instead.

#### Scenario: Admin views member list with email column

- **WHEN** user with MEMBERS:MANAGE authority views the member list
- **THEN** each member row shows the member's email address

#### Scenario: Regular user does not see email column

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** the email column is not shown

#### Scenario: Active-status column is not shown

- **WHEN** a user of any authority views the member list
- **THEN** no per-row active-status column is displayed

#### Scenario: Default view shows only active members for admin

- **WHEN** user with MEMBERS:MANAGE authority opens the member list without an explicit status filter
- **THEN** only active members are shown
- **AND** the status filter control shows "Aktivní" as the active selection

#### Scenario: Admin can switch to deactivated members

- **WHEN** user with MEMBERS:MANAGE authority switches the status filter to "Neaktivní"
- **THEN** only inactive members are shown

#### Scenario: Admin can show all members

- **WHEN** user with MEMBERS:MANAGE authority switches the status filter to "Vše"
- **THEN** both active and inactive members are shown

#### Scenario: Regular user sees only active members

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** only active members are shown in the list

#### Scenario: Regular user does not see the status filter

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** the status filter control is not displayed

#### Scenario: Regular user filtering by status sees only active members

- **WHEN** user without MEMBERS:MANAGE authority requests the member list with a status filter value (e.g. Neaktivní or Vše)
- **THEN** only active members are shown
- **AND** no error is reported

#### Scenario: Admin sees suspend action for active member

- **WHEN** user with MEMBERS:MANAGE authority views an active member in the list
- **THEN** a "suspend membership" action is available for that member

#### Scenario: Admin sees resume action for inactive member

- **WHEN** user with MEMBERS:MANAGE authority views an inactive member in the list
- **THEN** a "resume membership" action is available for that member

#### Scenario: Regular user sees no management actions

- **WHEN** user without MEMBERS:MANAGE authority views the member list
- **THEN** no suspend, resume, or update actions are shown

#### Scenario: Permissions link shown for active members to authorized user

- **WHEN** user with MEMBERS:PERMISSIONS authority views the member list
- **THEN** a permissions link is available for each active member

#### Scenario: Permissions link not shown for inactive members

- **WHEN** user with MEMBERS:PERMISSIONS authority views the member list
- **THEN** inactive members do not show a permissions link

#### Scenario: User can search members by first name

- **WHEN** a user types part of a first name into the search field
- **THEN** only members whose first name contains the typed text are shown

#### Scenario: User can search members by last name

- **WHEN** a user types part of a last name into the search field
- **THEN** only members whose last name contains the typed text are shown

#### Scenario: User can search members by registration number

- **WHEN** a user types part of a registration number into the search field
- **THEN** only members whose registration number contains the typed text are shown

#### Scenario: Search is case-insensitive

- **WHEN** a user types "NOVAK" into the search field
- **AND** there is a member whose last name is "Novák"
- **THEN** that member appears in the results

#### Scenario: Search ignores diacritics

- **WHEN** a user types "cermak" (without diacritics) into the search field
- **AND** there is a member whose last name is "Čermák"
- **THEN** that member appears in the results

#### Scenario: Multi-word search matches members containing all words

- **WHEN** a user types "jan novak" into the search field
- **AND** there is a member whose first name contains "Jan" and last name contains "Novák"
- **THEN** that member appears in the results

#### Scenario: Multi-word search excludes members missing any word

- **WHEN** a user types "jan novak" into the search field
- **AND** there is a member whose name matches "Jan" but not "Novák" (in any of the searched columns)
- **THEN** that member does NOT appear in the results

#### Scenario: Filters combine with AND semantics

- **WHEN** a user combines fulltext search with the status filter (e.g. `q=novak` + status `Neaktivní`)
- **THEN** only members matching every active filter are shown

#### Scenario: Empty result shows a message

- **WHEN** the combination of active filters matches no members
- **THEN** the list displays a message indicating no members match the current filters

#### Scenario: Admin sees all members including inactive

- **WHEN** user with MEMBERS:MANAGE authority views the member list with status filter "Vše"
- **THEN** both active and inactive (suspended) members are shown

#### Scenario: Member list is paginated

- **WHEN** user views the member list
- **THEN** the list shows 10 members per page by default
- **AND** pagination controls allow navigating between pages

#### Scenario: Member list is sortable

- **WHEN** user sorts the member list by first name, last name, or registration number
- **THEN** the list reorders accordingly

#### Scenario: Default sort falls back to first name for identical last names

- **WHEN** user views the member list without an explicit sort
- **THEN** members are ordered by last name ascending
- **AND** members with the same last name are ordered by first name ascending

#### Scenario: Invalid sort field shows error

- **WHEN** user attempts to sort by an unsupported field (e.g., email)
- **THEN** the system shows an error listing the allowed sort fields (firstName, lastName, registrationNumber)
