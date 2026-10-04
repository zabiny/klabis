# Spec Delta

## ADDED Requirements

### Requirement: Permissions Over Everything Or Over Specific Targets

The system SHALL let a user hold a permission either over everything or only over specific targets (for example selected members). A permission held over everything also covers targets created later. An action that concerns a target SHALL be allowed when the user holds one of the permissions the action accepts, either over everything or over that target.

#### Scenario: Permission over everything covers a newly created target

- **GIVEN** a user holds a permission over all members
- **WHEN** a new member is registered
- **THEN** the user can perform the actions covered by that permission on the new member

#### Scenario: Permission over specific targets is limited to them

- **GIVEN** a user holds a permission only over member A
- **WHEN** the user views member A and member B
- **THEN** the actions covered by that permission are offered on member A
- **AND** they are not offered on member B
- **AND** an attempt to perform them on member B is refused

#### Scenario: Any one of the accepted permissions is enough

- **GIVEN** an action accepts permission X over everything or permission Y over its target
- **WHEN** a user holding only Y over the target performs the action
- **THEN** the action succeeds

### Requirement: Permissions From Relationships Add Up

The system SHALL derive permissions over specific targets from relationships between the user and the targets (for example ownership of a group). The system SHALL support adding new kinds of relationships, and the permissions coming from all relationships SHALL be combined. A permission held only because of a relationship SHALL end as soon as the relationship ends.

#### Scenario: Two relationships to the same target

- **GIVEN** a user is related to member A through two different relationships, each granting a different permission
- **WHEN** the user views member A
- **THEN** the user can use both permissions on member A

#### Scenario: Ended relationship removes the permission

- **GIVEN** a user holds a permission over member A only through one relationship
- **WHEN** that relationship ends
- **THEN** on the user's next action the permission over member A is no longer available

### Requirement: Permissions Are Current For Every Request

The system SHALL evaluate each request against the user's permissions as they are at the time of that request, not as they were at login. All authorization decisions made while handling one request SHALL be based on the same set of permissions.

#### Scenario: Revoked permission stops working without re-login

- **GIVEN** a logged-in user holds a permission
- **WHEN** an administrator revokes that permission
- **THEN** the user's next request is evaluated without it
- **AND** the user does not need to log out and in again

#### Scenario: One request uses one set of permissions

- **WHEN** the system answers a single request
- **THEN** the actions it offers, the fields it shows and the checks it enforces are all based on the same set of the user's permissions

### Requirement: Offered Actions Match Enforced Checks

The system SHALL offer an action (a form, button or link) to a user only when the user is allowed to perform it, and SHALL allow every action it offers. A form SHALL contain only fields the user may change; fields the user may see but not change SHALL be shown as read-only.

#### Scenario: Offered action can be performed

- **WHEN** the system offers an action to a user
- **AND** the user performs it with valid input
- **THEN** the action is not refused for lack of permission

#### Scenario: Action not offered is refused

- **WHEN** a user attempts an action the system did not offer them for lack of permission
- **THEN** the system refuses the action

#### Scenario: Field visible but not changeable is read-only in the form

- **GIVEN** a user may see a field of a record but not change it
- **WHEN** the user opens the edit form for that record
- **THEN** the field is shown with its current value as read-only
