# Spec Delta

## MODIFIED Requirements

### Requirement: Discipline Catalog Management Requires EVENTS:MANAGE

The discipline catalog is an event-management tool, not a general-purpose reference list. The system SHALL only offer navigation to the discipline catalog, and only allow reading it, to users with EVENTS:MANAGE authority. The catalog is offered as the tab "Disciplíny" of the page "Nastavení akcí".

#### Scenario: Manager sees the Disciplines entry in the main menu

- **WHEN** a user with EVENTS:MANAGE authority opens "Nastavení akcí" from the main menu
- **THEN** a "Disciplíny" tab is shown

#### Scenario: User without EVENTS:MANAGE does not see the Disciplines entry

- **WHEN** a user without EVENTS:MANAGE authority views the main menu
- **THEN** no "Nastavení akcí" entry is shown
- **AND** the discipline catalog is not reachable
