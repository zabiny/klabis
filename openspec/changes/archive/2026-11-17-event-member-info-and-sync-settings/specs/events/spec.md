## ADDED Requirements

### Requirement: Information For Members On An Event

An event SHALL carry an optional free text "Informace pro členy" in which the event leader tells club members practical details, such as when and where they meet. The text is plain multi-line text; its line breaks are preserved when it is shown. Whoever may edit the event (a user with EVENTS:MANAGE or a coordinator of that event) SHALL be able to set, change and clear it.

#### Scenario: Manager adds information for members when creating an event

- **WHEN** a user with EVENTS:MANAGE fills in "Informace pro členy" in the event creation form and submits
- **THEN** the event is created with that text

#### Scenario: Coordinator writes information for members

- **GIVEN** a member without EVENTS:MANAGE who is a coordinator of a DRAFT or ACTIVE event
- **WHEN** the coordinator edits the event, writes "Sraz v 7:30 u klubovny" into "Informace pro členy" and saves
- **THEN** the event detail page shows "Sraz v 7:30 u klubovny" under "Informace pro členy"

#### Scenario: Members read the information on the event detail

- **GIVEN** an ACTIVE event whose "Informace pro členy" spans several lines
- **WHEN** a club member opens the event detail page
- **THEN** they see the "Informace pro členy" text with its line breaks kept

#### Scenario: No information section when the text is empty

- **WHEN** a user views the detail page of an event without any "Informace pro členy"
- **THEN** no "Informace pro členy" section is shown

#### Scenario: Clearing the information removes it from the detail

- **GIVEN** an event with "Informace pro členy" set
- **WHEN** a user allowed to edit the event clears the text and saves
- **THEN** the event detail page no longer shows the "Informace pro členy" section

#### Scenario: Users who cannot edit the event cannot change the information

- **WHEN** a member without EVENTS:MANAGE who is not a coordinator of the event views it
- **THEN** they can read "Informace pro členy" but are offered no way to change it

#### Scenario: Information of a finished event cannot be changed

- **WHEN** a user attempts to change "Informace pro členy" of a FINISHED or CANCELLED event
- **THEN** the system shows an error that the event cannot be modified, as for any other event field

#### Scenario: Too long information is refused

- **WHEN** a user allowed to edit the event enters "Informace pro členy" longer than 2000 characters and saves
- **THEN** the form shows an error that the text is too long
- **AND** the previously saved text stays in force

### Requirement: ORIS Synchronisation Leaves Information For Members Alone

"Informace pro členy" SHALL belong to Klabis only. Importing or synchronising an event from ORIS SHALL NOT set, change or clear it, and a change to it SHALL NOT be treated as a difference against ORIS.

#### Scenario: Synchronisation keeps the information

- **GIVEN** an ORIS-imported event with "Informace pro členy" written by its coordinator
- **WHEN** the event is synchronised with ORIS
- **THEN** the "Informace pro členy" text is unchanged

#### Scenario: Editing the information does not stop synchronisation

- **GIVEN** an ORIS-imported event that is kept in step with ORIS
- **WHEN** its coordinator changes "Informace pro členy" and saves
- **THEN** the event keeps being synchronised
- **AND** it is not marked as needing a decision about a difference against ORIS
