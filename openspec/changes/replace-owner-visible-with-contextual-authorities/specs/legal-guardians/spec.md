# Spec Delta

## ADDED Requirements

### Requirement: Legal Guardians Choose Their Minors' Membership Fee Type

A legal guardian group SHALL always delegate "Volba členského příspěvku" to its guardians over its minors. This cannot be changed. A legal guardian therefore opens the fee page of each of their minor children and chooses, changes or withdraws the child's membership fee type. The guardian loses this permission over a child when the child leaves the group after turning 18, or when the guardian stops being the child's legal guardian.

#### Scenario: Guardian opens the fee page of their child

- **GIVEN** a member is the legal guardian of a minor
- **WHEN** the guardian opens the minor's detail page
- **THEN** the "Členské příspěvky" button is displayed

#### Scenario: Non-member guardian opens the fee page of their child

- **GIVEN** a non-member legal guardian logged in with their EXT login number
- **WHEN** the guardian opens their minor child's detail page
- **THEN** the "Členské příspěvky" button is displayed

#### Scenario: Guardian loses the permission when the child turns 18

- **GIVEN** a legal guardian of a minor
- **WHEN** the minor turns 18 and the daily run has removed them from the legal guardian group
- **THEN** the former guardian no longer sees the "Členské příspěvky" button on the member's detail
- **AND** the member opens their own fee page and chooses their type themself

#### Scenario: Removed guardian loses the permission

- **GIVEN** a legal guardian of a minor
- **WHEN** an administrator changes the minor's legal guardians so that this person is no longer among them
- **THEN** that person no longer sees the "Členské příspěvky" button on the minor's detail

### Requirement: Legal Guardians Manage Their Minors' Event Registrations

A legal guardian group SHALL always delegate "Správa přihlášek člena" to its guardians over its minors. This cannot be changed. A legal guardian, member or not, therefore views, edits and unregisters the event registrations of each of their minor children under the same timing rules that apply to the child. The guardian loses this permission over a child when the child leaves the group after turning 18, or when the guardian stops being the child's legal guardian.

#### Scenario: Guardian edits their child's registration from the registrations list

- **GIVEN** a minor is registered for an active event with an open registration window
- **WHEN** the minor's legal guardian opens the event's registrations list
- **THEN** the child's row offers the edit action
- **AND** the guardian can change the child's category and save it

#### Scenario: Guardian views their child's registration

- **WHEN** the minor's legal guardian opens the child's registration from the event's registrations list
- **THEN** the registration details are shown including the child's SI card number

#### Scenario: Guardian unregisters their child

- **GIVEN** a minor is registered for an event before its date and registration deadline
- **WHEN** the minor's legal guardian chooses "Odhlásit" in the child's registration edit dialog and confirms
- **THEN** the child's registration is cancelled

#### Scenario: Guardian cannot manage the registration of another member

- **WHEN** a legal guardian without the registrations-manager permission opens the registrations list of an event
- **THEN** no edit action is offered on the rows of members who are not their minors

#### Scenario: Guardian loses the permission when the child turns 18

- **GIVEN** a legal guardian of a minor who is registered for an event
- **WHEN** the minor turns 18 and the daily run has removed them from the legal guardian group
- **THEN** the former guardian is no longer offered the edit action on the member's registration
