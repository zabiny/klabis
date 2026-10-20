## MODIFIED Requirements

### Requirement: Audit Trail of Level Assignments

The system SHALL keep a record of the level each member was assigned in each year, for accounting purposes. A page SHALL show a member's fee level history, with one entry per year in which the member had a level. Each entry shows the year, the level name, the yearly fee and how the level was assigned. Only the member, a membership administrator and a legal guardian of the member may open the page. Years without an assignment are not listed.

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

#### Scenario: Legal guardian sees the fee level history of their minor

- **GIVEN** a minor was assigned a fee level in a past year
- **WHEN** the minor's legal guardian opens the minor's detail and clicks "Zobrazit historii příspěvků"
- **THEN** the history page shows the level the minor was assigned in that year

#### Scenario: Legal guardian cannot open the history of a member who is not their minor

- **WHEN** a legal guardian without membership administration permission opens the detail of a member they are not guardian of
- **THEN** the "Zobrazit historii příspěvků" link is not displayed

#### Scenario: Year without an assignment is not listed

- **GIVEN** a member had no fee level in some year, for example because they missed the choice
- **WHEN** the member's fee level history page is opened
- **THEN** that year does not appear in the history

#### Scenario: History of an inactive member is not offered

- **WHEN** a membership administrator opens the detail of an inactive member
- **THEN** the "Zobrazit historii příspěvků" link is not displayed

#### Scenario: Member without any assignment sees an empty history

- **WHEN** a member who has never had a fee level assigned opens their fee level history page
- **THEN** the page says that the member has no fee level history yet

#### Scenario: Member cannot open another member's fee level history

- **WHEN** a member without membership administration permission opens another member's detail
- **THEN** the "Zobrazit historii příspěvků" link is not displayed

#### Scenario: Returning from the history page

- **WHEN** a user on a member's fee level history page clicks the back link
- **THEN** the system shows the detail of that member again
