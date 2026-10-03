# Relationship Authorization Specification

## Purpose

Defines how permissions can apply to a specific person or object (for example one member) rather than to the whole system, and how groups hand such permissions to their owners.

## ADDED Requirements

### Requirement: Permissions Over A Specific Target

The system SHALL support permissions that apply to a specific target (for example one member) in addition to global permissions. An action that accepts either a global permission or a permission over its target SHALL be allowed when the user holds the global permission, or holds the permission over that target.

#### Scenario: Global permission allows the action on any target

- **GIVEN** a user with MEMBERS:MANAGE authority
- **WHEN** the user performs an action that accepts MEMBERS:MANAGE or MEMBER:EDIT_DETAILS over the member
- **THEN** the action is allowed for any member

#### Scenario: Permission over a target allows the action only on that target

- **GIVEN** a user who holds MEMBER:EDIT_DETAILS over member A but not over member B and has no MEMBERS:MANAGE
- **WHEN** the user performs the action on member A
- **THEN** the action is allowed
- **AND** performing it on member B is refused with a permission denied error

### Requirement: Groups Delegate Permissions To Their Owners

A group SHALL be able to define a set of delegated permissions. The owners of such a group SHALL hold each delegated permission over every member of the group. Only permissions that are not global administrator permissions MAY be delegated. Owners lose a delegated permission over a member as soon as the member leaves the group, and lose all delegated permissions when they stop being owners.

#### Scenario: Owner holds the delegated permission over group members

- **GIVEN** a group that delegates MEMBER:EDIT_DETAILS and has members A and B
- **WHEN** an owner of the group is checked for MEMBER:EDIT_DETAILS over member A or member B
- **THEN** the owner holds the permission over both

#### Scenario: Owner does not hold the permission over non-members

- **GIVEN** a group that delegates MEMBER:EDIT_DETAILS
- **WHEN** an owner of the group is checked for MEMBER:EDIT_DETAILS over a person who is not a member of the group
- **THEN** the owner does not hold the permission

#### Scenario: Group without delegated permissions grants nothing

- **GIVEN** a group that delegates no permissions
- **WHEN** an owner of the group is checked for any permission over a member of the group
- **THEN** the owner holds no permission because of the group

#### Scenario: Ordinary members of the group do not receive the permissions

- **GIVEN** a group that delegates MEMBER:EDIT_DETAILS
- **WHEN** a member who is not an owner is checked for the permission over another member
- **THEN** the member does not hold the permission

#### Scenario: Administrator permissions cannot be delegated

- **WHEN** a group is defined with a global administrator permission such as MEMBERS:PERMISSIONS as a delegated permission
- **THEN** the system rejects the group definition

### Requirement: Offered Actions Reflect Permissions

The system SHALL offer an action (a form or a link) only to users who could currently perform it, including permissions held through a group. The offer and the actual permission check SHALL always agree.

#### Scenario: Action offered because of a delegated permission

- **GIVEN** a user who holds MEMBER:EDIT_DETAILS over a member through a group
- **WHEN** the user views that member's detail
- **THEN** the "Upravit" action is displayed

#### Scenario: Action not offered without any permission

- **GIVEN** a user who holds neither MEMBERS:MANAGE nor a delegated MEMBER:EDIT_DETAILS over a member
- **WHEN** the user views that member's detail
- **THEN** the edit action is not displayed

#### Scenario: Lists show the action per row

- **GIVEN** a user who holds MEMBER:EDIT_DETAILS over only some of the members in a list
- **WHEN** the user views the list
- **THEN** the edit action is offered only for those members
