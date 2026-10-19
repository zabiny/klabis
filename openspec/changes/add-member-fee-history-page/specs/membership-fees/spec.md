## MODIFIED Requirements

### Requirement: Audit Trail of Level Assignments

The system SHALL keep a record of the level each member was assigned in each year, for accounting purposes. A page SHALL show a member's fee level history, with one entry per year in which the member had a level. Each entry shows the year, the level name, the yearly fee and how the level was assigned. Only the member and a membership administrator may open the page.

#### Scenario: Member opens their fee level history from the profile

- **WHEN** a member opens their profile and clicks "Zobrazit historii příspěvků" in the membership fee section
- **THEN** the system shows the member's fee level history page
- **AND** the page lists every year in which the member had an assigned level, newest year first

#### Scenario: History entry shows level, yearly fee and how it was assigned

- **WHEN** a member opens their fee level history page for a year in which they chose a level themselves
- **THEN** the entry for that year shows the year, the level name and the yearly fee of that level
- **AND** the entry shows that the level was the member's own choice

#### Scenario: Level assigned by an administrator is marked as such

- **WHEN** a membership administrator assigned a member's level for a year, and the history page is opened for that member
- **THEN** the entry for that year shows that the level was assigned by an administrator

#### Scenario: Past assignments remain available

- **GIVEN** a member was assigned a level in a fee selection campaign that is now closed
- **WHEN** a membership administrator opens that member's detail and clicks "Zobrazit historii příspěvků"
- **THEN** the history page shows the level the member was assigned in that year

#### Scenario: Member without any assignment sees an empty history

- **WHEN** a member who has never had a fee level assigned opens their fee level history page
- **THEN** the page says that the member has no fee level history yet

#### Scenario: Member cannot open another member's fee level history

- **WHEN** a member without membership administration permission opens another member's detail
- **THEN** the "Zobrazit historii příspěvků" link is not displayed

#### Scenario: Returning from the history page

- **WHEN** a user on a member's fee level history page clicks the back link
- **THEN** the system shows the detail of that member again
