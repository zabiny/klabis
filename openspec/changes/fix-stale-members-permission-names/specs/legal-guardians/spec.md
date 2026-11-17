## MODIFIED Requirements

### Requirement: Legal Guardian Candidates

The system SHALL offer as legal guardian candidates every non-member legal guardian and every active club member aged 18 or older. Candidates SHALL be searchable by name, and each candidate SHALL be shown with enough detail to tell namesakes apart: the registration number for members and the e-mail for non-members. Candidates are available only to users with MEMBERS:MANAGE permission.

#### Scenario: Admin searches for a guardian

- **WHEN** an admin types part of a name into the guardian picker
- **THEN** matching non-member guardians and adult active members are offered
- **AND** each member shows their registration number and each non-member shows their e-mail

#### Scenario: Minors and suspended members are not offered

- **WHEN** an admin opens the guardian picker
- **THEN** members younger than 18 and members whose membership has ended are not offered
