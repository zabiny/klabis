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
