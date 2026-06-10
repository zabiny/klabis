# Capability: Membership Fees

## Purpose

Allows membership administrators to maintain a catalog of fee levels, publish levels for a calendar year, and manage member assignments. Members can choose their fee level before a voting deadline. The system enforces sanctions for missing choices and generates yearly membership fee charges automatically.

## Requirements

### Requirement: Membership Fee Level Catalog

The system SHALL allow a membership administrator to maintain a catalog of membership fee levels. Each level defines a human-readable name, a yearly membership fee amount in CZK, and a set of membership payment rules. The catalog is a working template that is reused when publishing levels for a calendar year; editing the catalog SHALL NOT affect any level already published for a year.

#### Scenario: Administrator creates a fee level

- **WHEN** a membership administrator creates a fee level with a name, a yearly membership fee amount, and a set of membership payment rules
- **THEN** the level is added to the catalog and is available for publishing

#### Scenario: Administrator edits a catalog level

- **WHEN** a membership administrator edits the name, yearly fee, or rules of a catalog level
- **THEN** the catalog level reflects the changes
- **AND** levels already published for a calendar year remain unchanged

#### Scenario: Non-administrator cannot manage the catalog

- **WHEN** a member without membership administration permission attempts to create or edit a catalog level
- **THEN** the system rejects the request as unauthorized

### Requirement: Membership Payment Rules by Event Type and Ranking

A membership fee level SHALL define membership payment rules that determine how much a member with that level contributes towards an event entry. Each rule applies to a combination of event type and event ranking. A rule expresses the member's contribution either as a percentage of the event's base entry price or as a fixed amount in CZK.

#### Scenario: Administrator defines a percentage rule

- **WHEN** a membership administrator adds a rule for a given event type and ranking expressed as a percentage
- **THEN** the rule is stored on the level as a percentage of the base entry price

#### Scenario: Administrator defines a fixed amount rule

- **WHEN** a membership administrator adds a rule for a given event type and ranking expressed as a fixed amount in CZK
- **THEN** the rule is stored on the level as a fixed amount

#### Scenario: Rule combination is unique within a level

- **WHEN** a membership administrator adds a rule for an event type and ranking combination that already has a rule on the same level
- **THEN** the system rejects the request as a duplicate rule

### Requirement: Publishing Fee Levels for a Calendar Year

The system SHALL allow a membership administrator to publish a set of fee levels for a specific calendar year and to set a single voting deadline shared by all levels of that year. Publishing SHALL create, for each published level, a frozen-on-demand snapshot of the level (its yearly fee and membership payment rules) together with the year of validity. The snapshot is independent of the catalog from the moment of publishing.

#### Scenario: Administrator publishes levels for a year

- **WHEN** a membership administrator publishes a selected set of catalog levels for an upcoming calendar year with a voting deadline
- **THEN** each selected level becomes a published level for that year holding a complete snapshot of its yearly fee and rules
- **AND** members can choose among the published levels for that year

#### Scenario: Year cannot be published twice

- **WHEN** a membership administrator attempts to publish levels for a calendar year that has already been published
- **THEN** the system rejects the request

#### Scenario: Voting deadline applies to the whole year

- **WHEN** a membership administrator sets the voting deadline while publishing a year
- **THEN** the single deadline governs the choice window for every level published for that year

### Requirement: Member Chooses a Fee Level for the Upcoming Year

The system SHALL allow each member to choose, before the voting deadline, one published fee level for the upcoming calendar year. The choice SHALL be an explicit action by the member. A member MAY change their choice any number of times until the voting deadline. After the voting deadline the choice is locked and cannot be changed by the member.

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

### Requirement: Emergency Assignment by Administrator

The system SHALL allow a membership administrator to assign or change a member's fee level for a year, including after the voting deadline. This emergency assignment overrides the locked state of a member's choice.

#### Scenario: Administrator assigns a level after the deadline

- **WHEN** a membership administrator assigns a published level to a member after the voting deadline
- **THEN** the member becomes assigned to that level for that year

#### Scenario: Administrator changes a member's level after the deadline

- **WHEN** a membership administrator changes a member's already assigned level after the voting deadline
- **THEN** the member's assignment moves to the newly selected level

### Requirement: Sanction for a Missing Choice

When a member has not chosen a fee level by the voting deadline, the system SHALL prevent that member from registering for new events and SHALL deregister that member from all events whose registrations are still open. The sanction takes effect the day after the voting deadline.

#### Scenario: Member who missed the choice is blocked and deregistered

- **WHEN** the voting deadline has passed and a member did not choose a level
- **THEN** the member is prevented from registering for new events
- **AND** the member is deregistered from all events that still have open registrations

#### Scenario: Member who chose a level is not sanctioned

- **WHEN** the voting deadline has passed and a member did choose a level before it
- **THEN** the member is not blocked and remains registered for their events

### Requirement: Lifting the Sanction by Emergency Assignment

When a membership administrator performs an emergency assignment for a member who was sanctioned for a missing choice, the system SHALL lift the registration block for that member. Previously deregistered registrations SHALL NOT be restored automatically.

#### Scenario: Emergency assignment unblocks future registrations

- **WHEN** a membership administrator assigns a level to a member who was blocked for a missing choice
- **THEN** the member is allowed to register for events again

#### Scenario: Deregistered registrations are not restored automatically

- **WHEN** a membership administrator assigns a level to a member who was deregistered for a missing choice
- **THEN** the previously cancelled registrations remain cancelled
- **AND** restoring them requires a manual re-registration

### Requirement: Editing a Published Level Until First Surcharge

The system SHALL allow a membership administrator to edit the yearly fee and membership payment rules of a published level only until the first surcharge has been calculated on the basis of that published level. Once the first surcharge has been calculated, the published level's snapshot is frozen and SHALL NOT be edited; corrections require publishing a new level.

#### Scenario: Administrator edits a published level before any surcharge

- **WHEN** a membership administrator edits the rules of a published level for which no surcharge has yet been calculated
- **THEN** the published level's snapshot reflects the changes

#### Scenario: Editing is rejected after the first surcharge

- **WHEN** a membership administrator attempts to edit a published level for which at least one surcharge has already been calculated
- **THEN** the system rejects the request because the snapshot is frozen

### Requirement: Generating the Yearly Membership Fee

The system SHALL post the yearly membership fee to each member's financial account based on the level the member is assigned to for the year. The yearly fee SHALL be posted the day after the voting deadline. The yearly fee for a given member and year SHALL be posted at most once.

#### Scenario: Yearly fee is posted the day after the deadline

- **WHEN** the day after the voting deadline arrives
- **THEN** each member assigned to a published level for that year has the level's yearly fee posted to their financial account

#### Scenario: Yearly fee is not posted twice

- **WHEN** the yearly fee generation runs again for a member and year that already received the yearly fee
- **THEN** no additional fee is posted for that member and year

#### Scenario: Member without an assigned level receives no yearly fee

- **WHEN** the yearly fee generation runs and a member has no assigned level for that year
- **THEN** no yearly fee is posted for that member

### Requirement: Member Sees Their Current Fee Level

The system SHALL allow a member to see their currently assigned fee level for the relevant year. A widget on the member's profile SHALL display the member's current level.

#### Scenario: Member sees the assigned level on the profile

- **WHEN** a member opens their profile
- **THEN** the profile shows the member's currently assigned fee level

#### Scenario: Member with no assigned level sees a prompt to choose

- **WHEN** a member who has not chosen a level for the upcoming year opens the profile while voting is open
- **THEN** the profile prompts the member to choose a level

### Requirement: Audit Trail of Level Assignments

The system SHALL retain the history of which level each member was assigned to in each year for accounting purposes.

#### Scenario: Past assignments remain available

- **WHEN** a membership administrator reviews a member's fee level assignment for a past year
- **THEN** the system shows the level the member was assigned to in that year
