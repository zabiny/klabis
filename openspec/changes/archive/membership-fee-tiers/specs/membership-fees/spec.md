## RENAMED Requirements

- FROM: `### Requirement: Membership Fee Level Catalog`
- TO: `### Requirement: Membership Fee Tier Catalog`

- FROM: `### Requirement: Publishing Fee Levels for a Calendar Year`
- TO: `### Requirement: Publishing Fee Tiers for a Calendar Year`

- FROM: `### Requirement: Member Chooses a Fee Level for the Upcoming Year`
- TO: `### Requirement: Member Chooses a Fee Tier for the Upcoming Year`

- FROM: `### Requirement: Editing a Published Level Until First Surcharge`
- TO: `### Requirement: Editing a Published Tier Until First Surcharge`

- FROM: `### Requirement: Member Sees Their Current Fee Level`
- TO: `### Requirement: Member Sees Their Current Fee Tier`

- FROM: `### Requirement: Audit Trail of Level Assignments`
- TO: `### Requirement: Audit Trail of Tier Assignments`

## MODIFIED Requirements

### Requirement: Membership Fee Tier Catalog

The system SHALL allow a membership administrator to maintain a catalog of membership fee tiers. Each tier defines a human-readable name and a yearly membership fee amount in CZK. A tier is created with just its name and yearly fee; its membership payment rules are managed afterwards from the tier detail. The catalog is a working template that is reused when publishing tiers for a calendar year; editing the catalog SHALL NOT affect any tier already published for a year.

#### Scenario: Administrator creates a fee tier

- **WHEN** a membership administrator fills in the new-tier form with a name and a yearly membership fee and confirms it
- **THEN** the tier is added to the catalog with no rules yet
- **AND** the tier is available for publishing and for adding rules from its detail

#### Scenario: Administrator edits a catalog tier

- **WHEN** a membership administrator edits the name or yearly fee of a catalog tier
- **THEN** the catalog tier reflects the changes
- **AND** tiers already published for a calendar year remain unchanged

#### Scenario: Non-administrator cannot manage the catalog

- **WHEN** a member without membership administration permission views the tier catalog
- **THEN** the options to create or edit a tier are not available

### Requirement: Membership Payment Rules by Event Type and Ranking

A membership fee tier SHALL define membership payment rules that determine how much a member with that tier contributes towards an event entry. Each rule applies to a combination of event type and event ranking. The administrator manages rules one at a time from the tier detail: adding a new rule, editing an existing rule's value, or removing a rule. A rule expresses the member's contribution either as a percentage of the event's base entry price or as a fixed amount in CZK. The event type is chosen from the club's event-type catalog and the ranking is chosen from the ranking code list.

#### Scenario: Administrator adds a percentage rule

- **WHEN** a membership administrator opens the add-rule form on a tier detail, selects an event type, selects a ranking, chooses the percentage option, enters a percentage and confirms
- **THEN** the rule appears in the tier's rule table expressed as a percentage of the base entry price

#### Scenario: Administrator adds a fixed amount rule

- **WHEN** a membership administrator opens the add-rule form on a tier detail, selects an event type, selects a ranking, chooses the fixed amount option, enters an amount in CZK and confirms
- **THEN** the rule appears in the tier's rule table expressed as a fixed amount

#### Scenario: Administrator selects a ranking from the code list

- **WHEN** a membership administrator opens the add-rule form
- **THEN** the ranking field offers the available rankings from the ranking code list to choose from
- **AND** the administrator does not type the ranking as free text

#### Scenario: Administrator edits a rule's value

- **WHEN** a membership administrator edits an existing rule and changes its percentage or fixed amount
- **THEN** the rule in the tier's rule table shows the new value
- **AND** the rule keeps the same event type and ranking

#### Scenario: Administrator removes a rule

- **WHEN** a membership administrator removes a rule from a tier
- **THEN** the rule no longer appears in the tier's rule table

#### Scenario: Rule combination is unique within a tier

- **WHEN** a membership administrator adds a rule for an event type and ranking combination that already has a rule on the same tier
- **THEN** the system rejects the addition and informs the administrator that a rule for that combination already exists

### Requirement: Publishing Fee Tiers for a Calendar Year

The system SHALL allow a membership administrator to publish a set of fee tiers for a specific calendar year and to set a single voting deadline shared by all tiers of that year. Publishing SHALL create, for each published tier, a frozen-on-demand snapshot of the tier (its yearly fee and membership payment rules) together with the year of validity. The snapshot is independent of the catalog from the moment of publishing.

#### Scenario: Administrator publishes tiers for a year

- **WHEN** a membership administrator publishes a selected set of catalog tiers for an upcoming calendar year with a voting deadline
- **THEN** each selected tier becomes a published tier for that year holding a complete snapshot of its yearly fee and rules
- **AND** members can choose among the published tiers for that year

#### Scenario: Year cannot be published twice

- **WHEN** a membership administrator attempts to publish tiers for a calendar year that has already been published
- **THEN** the system rejects the request

#### Scenario: Voting deadline applies to the whole year

- **WHEN** a membership administrator sets the voting deadline while publishing a year
- **THEN** the single deadline governs the choice window for every tier published for that year

### Requirement: Member Chooses a Fee Tier for the Upcoming Year

The system SHALL allow each member to choose, before the voting deadline, one published fee tier for the upcoming calendar year. The choice SHALL be an explicit action by the member. A member MAY change their choice any number of times until the voting deadline. After the voting deadline the choice is locked and cannot be changed by the member.

#### Scenario: Member chooses a tier

- **WHEN** a member selects one of the published tiers for the upcoming year before the voting deadline
- **THEN** the member becomes assigned to that tier for that year

#### Scenario: Member changes the choice before the deadline

- **WHEN** a member who already chose a tier selects a different published tier before the voting deadline
- **THEN** the member's assignment moves to the newly selected tier

#### Scenario: Member cannot change the choice after the deadline

- **WHEN** a member attempts to change their chosen tier after the voting deadline
- **THEN** the system rejects the request as the voting is closed

#### Scenario: Previous year's tier is offered as a default

- **WHEN** a member opens the choice form for the upcoming year and chose a tier in the previous year that is also published for the upcoming year
- **THEN** the form pre-fills the previous year's tier as a non-binding default
- **AND** the choice is recorded only after the member explicitly confirms it

### Requirement: Emergency Assignment by Administrator

The system SHALL allow a membership administrator to assign or change a member's fee tier for a year, including after the voting deadline. This emergency assignment overrides the locked state of a member's choice.

#### Scenario: Administrator assigns a tier after the deadline

- **WHEN** a membership administrator assigns a published tier to a member after the voting deadline
- **THEN** the member becomes assigned to that tier for that year

#### Scenario: Administrator changes a member's tier after the deadline

- **WHEN** a membership administrator changes a member's already assigned tier after the voting deadline
- **THEN** the member's assignment moves to the newly selected tier

### Requirement: Sanction for a Missing Choice

When a member has not chosen a fee tier by the voting deadline, the system SHALL prevent that member from registering for new events and SHALL deregister that member from all events whose registrations are still open. The sanction takes effect the day after the voting deadline.

#### Scenario: Member who missed the choice is blocked and deregistered

- **WHEN** the voting deadline has passed and a member did not choose a tier
- **THEN** the member is prevented from registering for new events
- **AND** the member is deregistered from all events that still have open registrations

#### Scenario: Member who chose a tier is not sanctioned

- **WHEN** the voting deadline has passed and a member did choose a tier before it
- **THEN** the member is not blocked and remains registered for their events

### Requirement: Lifting the Sanction by Emergency Assignment

When a membership administrator performs an emergency assignment for a member who was sanctioned for a missing choice, the system SHALL lift the registration block for that member. Previously deregistered registrations SHALL NOT be restored automatically.

#### Scenario: Emergency assignment unblocks future registrations

- **WHEN** a membership administrator assigns a tier to a member who was blocked for a missing choice
- **THEN** the member is allowed to register for events again

#### Scenario: Deregistered registrations are not restored automatically

- **WHEN** a membership administrator assigns a tier to a member who was deregistered for a missing choice
- **THEN** the previously cancelled registrations remain cancelled
- **AND** restoring them requires a manual re-registration

### Requirement: Editing a Published Tier Until First Surcharge

The system SHALL allow a membership administrator to edit the yearly fee and membership payment rules of a published tier only until the first surcharge has been calculated on the basis of that published tier. Once the first surcharge has been calculated, the published tier's snapshot is frozen and SHALL NOT be edited; corrections require publishing a new tier.

#### Scenario: Administrator edits a published tier before any surcharge

- **WHEN** a membership administrator edits the rules of a published tier for which no surcharge has yet been calculated
- **THEN** the published tier's snapshot reflects the changes

#### Scenario: Editing is rejected after the first surcharge

- **WHEN** a membership administrator attempts to edit a published tier for which at least one surcharge has already been calculated
- **THEN** the system rejects the request because the snapshot is frozen

### Requirement: Generating the Yearly Membership Fee

The system SHALL post the yearly membership fee to each member's financial account based on the tier the member is assigned to for the year. The yearly fee SHALL be posted the day after the voting deadline. The yearly fee for a given member and year SHALL be posted at most once.

#### Scenario: Yearly fee is posted the day after the deadline

- **WHEN** the day after the voting deadline arrives
- **THEN** each member assigned to a published tier for that year has the tier's yearly fee posted to their financial account

#### Scenario: Yearly fee is not posted twice

- **WHEN** the yearly fee generation runs again for a member and year that already received the yearly fee
- **THEN** no additional fee is posted for that member and year

#### Scenario: Member without an assigned tier receives no yearly fee

- **WHEN** the yearly fee generation runs and a member has no assigned tier for that year
- **THEN** no yearly fee is posted for that member

### Requirement: Member Sees Their Current Fee Tier

The system SHALL allow a member to see their currently assigned fee tier for the relevant year. A widget on the member's profile SHALL display the member's current tier.

#### Scenario: Member sees the assigned tier on the profile

- **WHEN** a member opens their profile
- **THEN** the profile shows the member's currently assigned fee tier

#### Scenario: Member with no assigned tier sees a prompt to choose

- **WHEN** a member who has not chosen a tier for the upcoming year opens the profile while voting is open
- **THEN** the profile prompts the member to choose a tier

### Requirement: Audit Trail of Tier Assignments

The system SHALL retain the history of which tier each member was assigned to in each year for accounting purposes.

#### Scenario: Past assignments remain available

- **WHEN** a membership administrator reviews a member's fee tier assignment for a past year
- **THEN** the system shows the tier the member was assigned to in that year
