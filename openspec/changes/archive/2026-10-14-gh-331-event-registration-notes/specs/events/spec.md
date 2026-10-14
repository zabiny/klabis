## MODIFIED Requirements

### Requirement: Update Event

The system SHALL allow users with EVENTS:MANAGE permission OR any member in the event's coordinators collection to update events in DRAFT or ACTIVE status. Editable fields include categories and the coordinators collection. Only users with EVENTS:MANAGE permission SHALL be able to change the coordinators collection; a coordinator without EVENTS:MANAGE sees the coordinators read-only in the edit form.

#### Scenario: Manager updates a DRAFT event

- **WHEN** user with EVENTS:MANAGE permission edits and saves a DRAFT event
- **THEN** the event is updated with the new values

#### Scenario: Manager updates an ACTIVE event

- **WHEN** user with EVENTS:MANAGE permission edits and saves an ACTIVE event
- **THEN** the event is updated with the new values

#### Scenario: Coordinator edits the event without EVENTS:MANAGE

- **WHEN** a member who is listed as a coordinator of a DRAFT or ACTIVE event edits and saves that event
- **THEN** the event is updated with the new values
- **AND** no EVENTS:MANAGE permission is required

#### Scenario: Finished event cannot be edited

- **WHEN** user attempts to edit a FINISHED event
- **THEN** the system shows an error that finished events cannot be modified

#### Scenario: Cancelled event cannot be edited

- **WHEN** user attempts to edit a CANCELLED event
- **THEN** the system shows an error that cancelled events cannot be modified

#### Scenario: Update action not shown without permission or coordinator role

- **WHEN** user without EVENTS:MANAGE permission views an event
- **AND** the user is not listed as a coordinator of that event
- **THEN** no edit action is available

#### Scenario: Duplicate coordinator in update is deduplicated

- **WHEN** user adds the same member twice to the coordinators field during update and submits
- **THEN** the event is updated with that member listed once

#### Scenario: Coordinator cannot change the coordinators

- **GIVEN** a member who is listed as a coordinator of a DRAFT or ACTIVE event and has no EVENTS:MANAGE permission
- **WHEN** the coordinator opens the edit form of that event
- **THEN** the coordinators are shown read-only
- **AND** the coordinator can still change and save the other event fields

#### Scenario: Manager changes the coordinators

- **WHEN** a user with EVENTS:MANAGE permission adds or removes a coordinator in the edit form of a DRAFT or ACTIVE event and saves
- **THEN** the event is updated with the new coordinators

