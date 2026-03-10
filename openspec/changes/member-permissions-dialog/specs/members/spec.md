## MODIFIED Requirements

### Requirement: Member detail includes permissions management access

The system SHALL provide access to permissions management from the member detail view.

#### Scenario: Permissions management accessible via HATEOAS link

- **WHEN** authenticated user with MEMBERS:PERMISSIONS authority views member detail
- **THEN** response includes `permissions` HATEOAS link pointing to `/api/users/{id}/permissions`
- **AND** the permissions management action is accessible directly from the member detail context without navigating to a separate page
