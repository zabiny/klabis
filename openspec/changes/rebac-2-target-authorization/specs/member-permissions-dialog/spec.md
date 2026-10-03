# Spec Delta

## ADDED Requirements

### Requirement: Permission Changes Take Effect Immediately

The system SHALL apply permissions saved in the permissions dialog to the affected user from their next action on, without requiring them to log in again. The dialog SHALL offer only permissions that can be granted over everything; permissions that arise only from relationships SHALL NOT be shown in the dialog.

#### Scenario: Granted permission is available without re-login

- **GIVEN** a member is logged in and does not hold "Správa tréninkových skupin"
- **WHEN** an administrator enables "Správa tréninkových skupin" for the member and saves
- **AND** the member reloads the page
- **THEN** the training groups page is available to the member

#### Scenario: Revoked permission is gone without re-login

- **GIVEN** a member is logged in and holds "Správa tréninkových skupin"
- **WHEN** an administrator disables that permission for the member and saves
- **AND** the member reloads the page
- **THEN** the training groups page is no longer available to the member

#### Scenario: Relationship-only permissions are not offered

- **WHEN** an administrator opens the permissions dialog of a member
- **THEN** the dialog does not contain any permission that can only arise from a relationship
