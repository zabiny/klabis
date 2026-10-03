## ADDED Requirements

### Requirement: Context-Specific Authority MEMBER:EDIT_DETAILS

The system SHALL know the authority MEMBER:EDIT_DETAILS, which permits editing a specific member's details. It SHALL only be obtained through a group that delegates it to its owners, and SHALL NOT be assignable to a user as a standalone permission.

#### Scenario: Authority is not offered in the permissions dialog

- **WHEN** an admin with MEMBERS:PERMISSIONS authority opens the permissions dialog of a member
- **THEN** MEMBER:EDIT_DETAILS is not among the assignable permissions

#### Scenario: Assigning the authority directly is rejected

- **WHEN** an admin attempts to assign MEMBER:EDIT_DETAILS to a user as a direct permission
- **THEN** the system shows an error listing the valid authorities
