## ADDED Requirements

### Requirement: Registrations List Not Accessible for DRAFT Events

The system SHALL NOT provide access to the registrations list for events in DRAFT status. The registrations list link SHALL only be available for events in ACTIVE or FINISHED status.

#### Scenario: Registrations list link is absent for DRAFT event

- **WHEN** a user views the detail page of an event in DRAFT status
- **THEN** no link to the registrations list is present in the page
- **AND** the registrations section is not shown

#### Scenario: Registrations list is accessible for ACTIVE event

- **WHEN** a user navigates to the registrations list of an event in ACTIVE status
- **THEN** the registrations list is displayed normally
