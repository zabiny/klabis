## MODIFIED Requirements

### Requirement: Valid Authorities

The system SHALL restrict permission updates to a predefined set of valid authorities.

#### Scenario: Valid authorities can be assigned

- **WHEN** admin with MEMBERS:PERMISSIONS authority assigns any combination of valid authorities (MEMBERS:MANAGE, MEMBERS:READ, MEMBERS:PERMISSIONS, EVENTS:MANAGE, EVENTS:REGISTRATIONS, EVENTS:READ, CALENDAR:MANAGE, GROUPS:TRAINING) to a user
- **THEN** the assignment succeeds

#### Scenario: Invalid authority shows error

- **WHEN** admin attempts to assign an authority not in the valid set
- **THEN** the system shows an error listing the valid authorities
