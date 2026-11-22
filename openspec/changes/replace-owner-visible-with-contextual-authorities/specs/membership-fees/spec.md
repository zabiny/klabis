# Spec Delta

## MODIFIED Requirements

### Requirement: Member Chooses a Fee Type for the Upcoming Year

The system SHALL allow each adult member, and the legal guardian of a minor member on the minor's behalf, to choose, change or withdraw one published membership fee type for the upcoming year in the "Členský příspěvek" section until the voting deadline. A minor never chooses for themself. After the deadline or once the campaign is closed the choice is locked. Nobody else - neither a membership administrator nor an owner of a group the member belongs to - can choose or withdraw a type.

#### Scenario: Member chooses a type

- **WHEN** a member selects one of the offered membership fee types in the "Členský příspěvek" section before the voting deadline
- **THEN** the member becomes assigned to that type for that year
- **AND** the member does not leave the "Členské příspěvky" page to make the choice

#### Scenario: Offered types show their yearly price

- **WHEN** a member opens the "Členské příspěvky" page while voting is open
- **THEN** every membership fee type offered in the "Členský příspěvek" section shows its name and its yearly price

#### Scenario: Member changes the choice before the deadline

- **WHEN** a member who already chose a type selects a different offered type before the voting deadline
- **THEN** the member's assignment moves to the newly selected type

#### Scenario: Member cannot change the choice after the deadline

- **WHEN** a member attempts to change their chosen type after the voting deadline
- **THEN** the system rejects the request as the voting is closed

#### Scenario: Previous year's type is offered as a default

- **WHEN** a member opens the "Členský příspěvek" section for the upcoming year and chose a type in the previous year that is also published for the upcoming year
- **THEN** the section pre-selects the previous year's type as a non-binding default
- **AND** the choice is recorded only after the member explicitly confirms it

#### Scenario: Member withdraws the choice before the deadline

- **WHEN** a member who chose a type for the upcoming year withdraws the choice in the "Členský příspěvek" section before the voting deadline
- **THEN** the member is no longer assigned to any type for that year
- **AND** the section prompts the member to choose a type again
- **AND** if the member does not choose a type before the voting deadline, the sanction for a missing choice applies

#### Scenario: Member withdraws a type assigned by an administrator

- **GIVEN** a membership administrator assigned a type to a member for the upcoming year
- **WHEN** the member withdraws that type in the "Členský příspěvek" section before the voting deadline
- **THEN** the member is no longer assigned to any type for that year

#### Scenario: Member cannot withdraw the choice after the deadline

- **WHEN** a member with an assigned type opens the "Členské příspěvky" page after the voting deadline while the campaign is still active
- **THEN** the section shows the type as locked
- **AND** the option to change or withdraw the choice is not offered

#### Scenario: Legal guardian chooses a type for their minor

- **GIVEN** voting is open and the minor has not chosen a type
- **WHEN** the minor's legal guardian opens the minor's "Členské příspěvky" page and selects one of the offered types in the "Členský příspěvek" section
- **THEN** the minor becomes assigned to that type for that year

#### Scenario: Non-member legal guardian chooses a type for their minor

- **GIVEN** voting is open and the legal guardian is not a club member
- **WHEN** the guardian selects an offered type for the minor on the minor's "Členské příspěvky" page
- **THEN** the minor becomes assigned to that type for that year

#### Scenario: Legal guardian withdraws or changes the minor's choice before the deadline

- **GIVEN** voting is open and the minor has a type for the upcoming year
- **WHEN** the minor's legal guardian changes the type or withdraws it
- **THEN** the minor's assignment moves to the new type or the minor is no longer assigned to any type

#### Scenario: Administrator cannot choose a type for a member

- **WHEN** a membership administrator who is not the member's legal guardian opens the member's "Členské příspěvky" page while voting is open
- **THEN** the page does not show the "Členský příspěvek" section

#### Scenario: Legal guardian cannot choose for a member who is not their minor

- **WHEN** a legal guardian attempts to choose a type for a member who is not their minor
- **THEN** the system rejects the request as forbidden

#### Scenario: Minor cannot choose a type for themself

- **GIVEN** voting is open
- **WHEN** a minor member opens their own profile
- **THEN** the "Členské příspěvky" button is not displayed
- **AND** the minor cannot choose, change or withdraw a type

#### Scenario: Free group owner cannot choose for a group member

- **GIVEN** voting is open and a user owns a free group that delegates "Úprava údajů člena"
- **WHEN** the owner opens the detail of a member of that group who is not their minor
- **THEN** the "Členské příspěvky" button is not displayed

### Requirement: Fee Type Section on the Member's Fees Page

The "Členské příspěvky" page of a member SHALL show a "Členský příspěvek" section above the history only while a fee selection campaign is active, and only to an adult member and to a legal guardian who may choose for them. The section shows the chosen type or a prompt to choose one and holds the controls for choosing, changing and withdrawing it. Outside an active campaign the chosen type is visible only in the history.

#### Scenario: Member sees the section with the assigned type during an active campaign

- **GIVEN** a fee selection campaign is active and voting is open
- **WHEN** a member who chose a type opens their profile and clicks "Členské příspěvky"
- **THEN** the page shows the "Členský příspěvek" section above the history with the member's chosen type
- **AND** the section offers to change or withdraw it

#### Scenario: Member with no chosen type sees a prompt to choose

- **GIVEN** a fee selection campaign is active and voting is open
- **WHEN** a member who has not chosen a type opens the "Členské příspěvky" page
- **THEN** the section prompts the member to choose one of the offered types

#### Scenario: Section is locked after the voting deadline

- **GIVEN** the voting deadline has passed and the campaign has not been closed yet
- **WHEN** a member who chose a type opens the "Členské příspěvky" page
- **THEN** the section shows the chosen type and indicates that the choice is locked

#### Scenario: Member who missed the deadline is told the voting is closed

- **GIVEN** the voting deadline has passed and the campaign has not been closed yet
- **WHEN** a member who did not choose a type opens the "Členské příspěvky" page
- **THEN** the section says that the voting is closed and no type was chosen

#### Scenario: Section is hidden when no campaign is active

- **WHEN** a member opens the "Členské příspěvky" page while no fee selection campaign is active
- **THEN** the page does not show the "Členský příspěvek" section
- **AND** the member's chosen type for past years is visible only in the history table

#### Scenario: Section is hidden after the campaign is closed

- **GIVEN** a member chose a type in a campaign that has since been closed
- **WHEN** the member opens the "Členské příspěvky" page
- **THEN** the page does not show the "Členský příspěvek" section
- **AND** the history shows the chosen type for the campaign year

#### Scenario: Legal guardian sees the section for their minor

- **GIVEN** a fee selection campaign is active and voting is open
- **WHEN** the minor's legal guardian opens the minor's "Členské příspěvky" page
- **THEN** the page shows the "Členský příspěvek" section with the minor's chosen type, or a prompt to choose one
- **AND** the section offers to choose, change or withdraw it

#### Scenario: Administrator without the right to choose does not see the section

- **GIVEN** a fee selection campaign is active
- **WHEN** a membership administrator who is neither the member nor their legal guardian opens the member's "Členské příspěvky" page
- **THEN** the page does not show the "Členský příspěvek" section
- **AND** the page shows the member's fee type history

### Requirement: Audit Trail of Fee Type Assignments

The system SHALL keep a record of the membership fee type each member was assigned in each year, and the "Členské příspěvky" page SHALL show it as a history with one entry per year, newest first. Each entry shows the year, the type name, the yearly price of the type as published for that year, and how the type was assigned. Only an adult member themself, a membership administrator and a legal guardian of the member may open the page; a minor member cannot open their own fee page.

#### Scenario: Member opens their fee type history from the profile

- **WHEN** a member opens their profile and clicks "Členské příspěvky"
- **THEN** the system shows the "Členské příspěvky" page with the member's fee type history
- **AND** the history lists every year in which the member had an assigned type, newest year first

#### Scenario: History entry shows year, type, price and how it was assigned

- **WHEN** a member opens the "Členské příspěvky" page for a year in which they chose a type themselves
- **THEN** the entry for that year shows the year and the type name
- **AND** the entry shows the yearly price the member pays for that type in that year
- **AND** the entry shows that the type was the member's own choice

#### Scenario: Price in the history is not changed by later catalog edits

- **GIVEN** a member was assigned a type in a past year
- **WHEN** a membership administrator later changes the yearly fee of that type in the catalog
- **THEN** the history entry for the past year still shows the price that was published for that year

#### Scenario: Type assigned by an administrator is marked as such

- **WHEN** a membership administrator assigned a member's type for a year, and the "Členské příspěvky" page is opened for that member
- **THEN** the entry for that year shows that the type was assigned by an administrator

#### Scenario: Past assignments remain available

- **GIVEN** a member was assigned a type in a fee selection campaign that is now closed
- **WHEN** a membership administrator opens that member's detail and clicks "Členské příspěvky"
- **THEN** the history shows the type and price the member was assigned in that year

#### Scenario: Administrator sees only the history of another member

- **WHEN** a membership administrator opens the "Členské příspěvky" page of another member
- **THEN** the page shows the member's fee type history
- **AND** the page does not show the "Členský příspěvek" section

#### Scenario: Legal guardian sees the fee type history of their minor

- **GIVEN** a minor was assigned a fee type in a past year
- **WHEN** the minor's legal guardian opens the minor's detail and clicks "Členské příspěvky"
- **THEN** the history shows the type and price the minor was assigned in that year

#### Scenario: Legal guardian cannot open the fee page of a member who is not their minor

- **WHEN** a legal guardian without membership administration permission opens the detail of a member they are not guardian of
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Year without an assignment is not listed

- **GIVEN** a member had no fee type in some year, for example because they missed the choice
- **WHEN** the member's "Členské příspěvky" page is opened
- **THEN** that year does not appear in the history

#### Scenario: Fee page of an inactive member is not offered

- **WHEN** a membership administrator opens the detail of an inactive member
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Member without any assignment sees an empty history

- **WHEN** a member who has never had a fee type assigned opens their "Členské příspěvky" page
- **THEN** the history says that the member has no fee type history yet

#### Scenario: Member cannot open another member's fee page

- **WHEN** a member without membership administration permission opens another member's detail
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Returning from the fee page

- **WHEN** a user on a member's "Členské příspěvky" page clicks the back link
- **THEN** the system shows the detail of that member again

#### Scenario: Minor cannot open their own fee page

- **WHEN** a minor member opens their own profile
- **THEN** the "Členské příspěvky" button is not displayed

#### Scenario: Free group owner cannot open a group member's fee page

- **GIVEN** a user owns a free group and has no membership administration permission
- **WHEN** the owner opens the detail of a member of that group who is not their minor
- **THEN** the "Členské příspěvky" button is not displayed
