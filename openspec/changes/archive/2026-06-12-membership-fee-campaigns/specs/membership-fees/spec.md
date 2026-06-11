## RENAMED Requirements

### Requirement: Publishing Fee Levels for a Calendar Year
FROM: Publishing Fee Levels for a Calendar Year
TO: Starting a Fee Selection Campaign

### Requirement: Year cannot be published twice
FROM: Year cannot be published twice
TO: Only one active campaign at a time

## MODIFIED Requirements

### Requirement: Starting a Fee Selection Campaign

The system SHALL allow a membership administrator to start a fee selection campaign for a specific calendar year by selecting a set of fee levels and setting a voting deadline. The deadline SHALL be a future date. A new campaign MAY only be started when no other campaign is currently active. Starting a campaign SHALL create, for each selected level, a frozen-on-demand snapshot independent of the catalog.

#### Scenario: Administrator starts a fee selection campaign

- **WHEN** a membership administrator starts a campaign for a calendar year with a voting deadline in the future and a set of catalog levels
- **THEN** each selected level becomes a published level for that year holding a complete snapshot of its yearly fee and rules
- **AND** members can choose among the published levels for that year

#### Scenario: Voting deadline must be in the future

- **WHEN** a membership administrator attempts to start a campaign with a voting deadline set to today or a past date
- **THEN** the system rejects the request as invalid

#### Scenario: Only one active campaign at a time

- **WHEN** a membership administrator attempts to start a new campaign while another campaign is still active
- **THEN** the system rejects the request
- **AND** the administrator must wait until the active campaign closes before starting a new one

#### Scenario: Voting deadline applies to the whole campaign

- **WHEN** a membership administrator sets the voting deadline while starting a campaign
- **THEN** the single deadline governs the choice window for every level published in that campaign

### Requirement: Changing the Voting Deadline of an Active Campaign

The system SHALL allow a membership administrator to change the voting deadline of a currently active campaign. The new deadline SHALL NOT be set to a past date.

#### Scenario: Administrator extends the voting deadline

- **WHEN** a membership administrator changes the voting deadline of an active campaign to a future date
- **THEN** the campaign's deadline is updated and the new deadline governs the choice window

#### Scenario: Administrator sets deadline to today

- **WHEN** a membership administrator changes the voting deadline of an active campaign to today's date
- **THEN** the campaign's deadline is updated to today

#### Scenario: Deadline cannot be moved to the past

- **WHEN** a membership administrator attempts to change the voting deadline of an active campaign to a date in the past
- **THEN** the system rejects the request as invalid

#### Scenario: Closed campaign deadline cannot be changed

- **WHEN** a membership administrator attempts to change the voting deadline of a campaign that is already closed
- **THEN** the system rejects the request
