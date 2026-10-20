## MODIFIED Requirements

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
- **AND** the administrator sees a Czech message saying that the level already has a rule for that event type and ranking, naming the event type by its name and the ranking by its label
- **AND** the message contains no internal identifier of the event type

### Requirement: Starting a Fee Selection Campaign

The system SHALL allow a membership administrator to start a fee selection campaign for a specific calendar year by selecting a set of fee levels and setting a voting deadline. The deadline SHALL be a future date. A new campaign MAY only be started when no other campaign is currently active. Starting a campaign SHALL create, for each selected level, a frozen-on-demand snapshot independent of the catalog.

#### Scenario: Administrator starts a fee selection campaign

- **WHEN** a membership administrator starts a campaign for a calendar year with a voting deadline in the future and a set of catalog levels
- **THEN** each selected level becomes a published level for that year holding a complete snapshot of its yearly fee and rules
- **AND** members can choose among the published levels for that year

#### Scenario: Voting deadline must be in the future

- **WHEN** a membership administrator attempts to start a campaign with a voting deadline set to today or a past date
- **THEN** the system rejects the request as invalid
- **AND** a Czech message saying that the voting deadline must be a future date is shown next to the voting deadline field
- **AND** the start campaign form stays open with the entered values

#### Scenario: Only one active campaign at a time

- **WHEN** a membership administrator attempts to start a new campaign while another campaign is still active
- **THEN** the system rejects the request
- **AND** the administrator must wait until the active campaign closes before starting a new one

#### Scenario: Voting deadline applies to the whole campaign

- **WHEN** a membership administrator sets the voting deadline while starting a campaign
- **THEN** the single deadline governs the choice window for every level published in that campaign
