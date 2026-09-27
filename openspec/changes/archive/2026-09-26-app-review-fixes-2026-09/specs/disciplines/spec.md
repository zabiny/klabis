## ADDED Requirements

### Requirement: Discipline Catalog Management Requires EVENTS:MANAGE

The discipline catalog is an event-management tool, not a general-purpose reference list. The system SHALL only offer navigation to the discipline catalog, and only allow reading it, to users with EVENTS:MANAGE authority.

#### Scenario: Manager sees the Disciplines entry in the main menu

- **WHEN** a user with EVENTS:MANAGE authority views the main menu
- **THEN** a "Disciplíny" entry is shown

#### Scenario: User without EVENTS:MANAGE does not see the Disciplines entry

- **WHEN** a user without EVENTS:MANAGE authority views the main menu
- **THEN** no "Disciplíny" entry is shown
