## MODIFIED Requirements

### Requirement: Member Sees Their Current Fee Level

The system SHALL allow a member to see their currently assigned fee level for the relevant year. The member's detail SHALL offer a "Členské příspěvky" button, and the page it opens SHALL display the member's current level. The relevant year is the year of the active campaign; when no campaign is active, it is the latest campaign year that is not in the past; otherwise it is the current calendar year.

#### Scenario: Member sees the assigned level on the profile

- **WHEN** a member opens their profile and clicks "Členské příspěvky"
- **THEN** the page shows the member's currently assigned fee level

#### Scenario: Member with no assigned level sees a prompt to choose

- **WHEN** a member who has not chosen a level for the upcoming year opens the "Členské příspěvky" page while voting is open
- **THEN** the page prompts the member to choose a level

#### Scenario: Member still sees the chosen level after the campaign is closed

- **GIVEN** a member chose a level in a campaign for the upcoming year
- **WHEN** that campaign has been closed and the member opens the "Členské příspěvky" page
- **THEN** the page still shows the chosen level for the campaign year
- **AND** the page indicates that the choice is locked

### Requirement: Audit Trail of Level Assignments

The system SHALL keep a record of the level each member was assigned in each year, for accounting purposes. The "Členské příspěvky" page SHALL show a member's fee level history, with one entry per year in which the member had a level. Each entry shows the year, the level name and how the level was assigned. Only the member, a membership administrator and a legal guardian of the member may open the page, and the button leading to it is shown only to them and only for an active member. Years without an assignment are not listed. The current level section of the page is shown only to the member themselves.

#### Scenario: Member opens their fee level history from the profile

- **WHEN** a member opens their profile and clicks "Členské příspěvky"
- **THEN** the system shows the "Členské příspěvky" page with the member's fee level history
- **AND** the history lists every year in which the member had an assigned level, newest year first

#### Scenario: History entry shows year, level and how it was assigned

- **WHEN** a member opens the "Členské příspěvky" page for a year in which they chose a level themselves
- **THEN** the entry for that year shows the year and the level name
- **AND** the entry shows that the level was the member's own choice

#### Scenario: Level assigned by an administrator is marked as such

- **WHEN** a membership administrator assigned a member's level for a year, and the "Členské příspěvky" page is opened for that member
- **THEN** the entry for that year shows that the level was assigned by an administrator

#### Scenario: Past assignments remain available

- **GIVEN** a member was assigned a level in a fee selection campaign that is now closed
- **WHEN** a membership administrator opens that member's detail and clicks "Členské příspěvky"
- **THEN** the history shows the level the member was assigned in that year

#### Scenario: Administrator sees only the history of another member

- **WHEN** a membership administrator opens the "Členské příspěvky" page of another member
- **THEN** the page shows the member's fee level history
- **AND** the page does not show the member's current level section or the controls for choosing a level

#### Scenario: Legal guardian sees the fee level history of their minor

- **GIVEN** a minor was assigned a fee level in a past year
- **WHEN** the minor's legal guardian opens the minor's detail and clicks "Členské příspěvky"
- **THEN** the history shows the level the minor was assigned in that year

#### Scenario: Legal guardian cannot open the fee page of a member who is not their minor

- **WHEN** a legal guardian without membership administration permission opens the detail of a member they are not guardian of
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Year without an assignment is not listed

- **GIVEN** a member had no fee level in some year, for example because they missed the choice
- **WHEN** the member's "Členské příspěvky" page is opened
- **THEN** that year does not appear in the history

#### Scenario: Fee page of an inactive member is not offered

- **WHEN** a membership administrator opens the detail of an inactive member
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Member without any assignment sees an empty history

- **WHEN** a member who has never had a fee level assigned opens their "Členské příspěvky" page
- **THEN** the history says that the member has no fee level history yet

#### Scenario: Member cannot open another member's fee page

- **WHEN** a member without membership administration permission opens another member's detail
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Returning from the fee page

- **WHEN** a user on a member's "Členské příspěvky" page clicks the back link
- **THEN** the system shows the detail of that member again
