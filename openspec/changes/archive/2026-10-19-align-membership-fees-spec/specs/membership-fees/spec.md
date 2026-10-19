## MODIFIED Requirements

### Requirement: Membership Fee Level Catalog

The system SHALL allow a membership administrator to maintain a catalog of membership fee levels. Each level defines a human-readable name, a yearly membership fee amount in CZK, and a set of membership payment rules. A level is created with its name and yearly fee; its membership payment rules are added afterwards on the level's detail page. The catalog is a working template that is reused when publishing levels for a calendar year; editing the catalog SHALL NOT affect any level already published for a year.

#### Scenario: Administrator creates a fee level

- **WHEN** a membership administrator creates a fee level with a name and a yearly membership fee amount
- **THEN** the level is added to the catalog and is available for publishing
- **AND** the level has no membership payment rules until the administrator adds them on the level's detail page

#### Scenario: Administrator edits a catalog level

- **WHEN** a membership administrator edits the name, yearly fee, or rules of a catalog level
- **THEN** the catalog level reflects the changes
- **AND** levels already published for a calendar year remain unchanged

#### Scenario: Non-administrator cannot manage the catalog

- **WHEN** a member without membership administration permission attempts to create or edit a catalog level
- **THEN** the system rejects the request as unauthorized

### Requirement: Member Chooses a Fee Level for the Upcoming Year

The system SHALL allow each member to choose, before the voting deadline, one published fee level for the upcoming calendar year. The choice SHALL be an explicit action by the member. Until the voting deadline a member MAY change their choice any number of times or withdraw their current level, including a level assigned by an administrator. After the voting deadline, or once the campaign is closed, the choice is locked and the member can neither change nor withdraw it.

#### Scenario: Member chooses a level

- **WHEN** a member selects one of the published levels for the upcoming year before the voting deadline
- **THEN** the member becomes assigned to that level for that year

#### Scenario: Member changes the choice before the deadline

- **WHEN** a member who already chose a level selects a different published level before the voting deadline
- **THEN** the member's assignment moves to the newly selected level

#### Scenario: Member cannot change the choice after the deadline

- **WHEN** a member attempts to change their chosen level after the voting deadline
- **THEN** the system rejects the request as the voting is closed

#### Scenario: Previous year's level is offered as a default

- **WHEN** a member opens the choice form for the upcoming year and chose a level in the previous year that is also published for the upcoming year
- **THEN** the form pre-fills the previous year's level as a non-binding default
- **AND** the choice is recorded only after the member explicitly confirms it

#### Scenario: Member withdraws the choice before the deadline

- **WHEN** a member who chose a level for the upcoming year withdraws the choice on the choice form before the voting deadline
- **THEN** the member is no longer assigned to any level for that year
- **AND** the profile prompts the member to choose a level again
- **AND** if the member does not choose a level before the voting deadline, the sanction for a missing choice applies

#### Scenario: Member withdraws a level assigned by an administrator

- **GIVEN** a membership administrator assigned a level to a member for the upcoming year
- **WHEN** the member withdraws that level on the choice form before the voting deadline
- **THEN** the member is no longer assigned to any level for that year

#### Scenario: Member cannot withdraw the choice after the deadline

- **WHEN** a member with an assigned level opens the choice form after the voting deadline or after the campaign has been closed
- **THEN** the form shows the level as locked
- **AND** the option to withdraw the choice is not offered

### Requirement: Member Sees Their Current Fee Level

The system SHALL allow a member to see their currently assigned fee level for the relevant year. A widget on the member's profile SHALL display the member's current level. The relevant year is the year of the active campaign; when no campaign is active, it is the latest campaign year that is not in the past; otherwise it is the current calendar year.

#### Scenario: Member sees the assigned level on the profile

- **WHEN** a member opens their profile
- **THEN** the profile shows the member's currently assigned fee level

#### Scenario: Member with no assigned level sees a prompt to choose

- **WHEN** a member who has not chosen a level for the upcoming year opens the profile while voting is open
- **THEN** the profile prompts the member to choose a level

#### Scenario: Member still sees the chosen level after the campaign is closed

- **GIVEN** a member chose a level in a campaign for the upcoming year
- **WHEN** that campaign has been closed and the member opens their profile
- **THEN** the profile still shows the chosen level for the campaign year
- **AND** the profile indicates that the choice is locked
