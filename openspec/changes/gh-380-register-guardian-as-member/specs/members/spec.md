## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a user account and a member profile in a single flow. Whether the new member is a minor (younger than 18 today) or an adult is determined from the date of birth, and the registration form switches its sections accordingly. A minor SHALL be registered together with at least one legal guardian; each guardian is either chosen from the legal guardian candidates or entered as a new non-member guardian in the same step. An adult SHALL be registered without legal guardians. A member registered by hand SHALL be complete; registration does not send any e-mail. If any part of the registration fails, nothing is created.

#### Scenario: Admin registers a new adult member

- **WHEN** admin with MEMBERS:CREATE permission navigates to the registration page
- **AND** submits the registration form for an adult with valid data
- **THEN** the new member appears in the member list
- **AND** a user account awaiting activation is created for the member
- **AND** no e-mail is sent to the member

#### Scenario: Admin registers a minor with an existing guardian

- **WHEN** admin fills in a date of birth of a person younger than 18
- **AND** chooses an adult member as the legal guardian and submits the form
- **THEN** the minor appears in the member list
- **AND** the chosen member is listed as the minor's legal guardian

#### Scenario: Admin registers a minor with a new non-member guardian

- **WHEN** admin registers a minor and enters a new guardian with first name, last name, e-mail and telephone
- **THEN** the minor is registered with that guardian
- **AND** the guardian gets a user account awaiting activation with a login number from the series EXT0001, EXT0002, …
- **AND** no e-mail is sent to anyone

#### Scenario: Admin registers a minor with two guardians

- **WHEN** admin registers a minor with one existing member and one new non-member as guardians
- **THEN** both are listed as the minor's legal guardians

#### Scenario: Minor registered without guardians is rejected

- **WHEN** admin submits the registration form for a minor without any legal guardian
- **THEN** the form shows an error that a legal guardian is required for minors
- **AND** no member is created

#### Scenario: Adult registered with guardians is rejected

- **WHEN** a registration of an adult is submitted together with legal guardians
- **THEN** the registration is rejected with an error that adults cannot have legal guardians
- **AND** no member is created

#### Scenario: Registration form switches sections by date of birth

- **WHEN** admin enters a date of birth of a person younger than 18
- **THEN** the form shows the "Zákonní zástupci" section and marks the member's own e-mail and telephone as optional
- **WHEN** admin changes the date of birth to a person aged 18 or older
- **THEN** the "Zákonní zástupci" section is hidden and the member's own e-mail and telephone are required

#### Scenario: Guardian takeover is offered before a date of birth is entered

- **WHEN** admin opens the registration form
- **THEN** the "Převzetí zákonného zástupce" section is shown before any date of birth is entered
- **AND** it stays shown whatever date of birth the admin enters

#### Scenario: Taking over a guardian as a minor is rejected

- **GIVEN** a non-member legal guardian "Eva Svobodová"
- **WHEN** admin chooses to take over "Eva Svobodová", enters a date of birth of a person younger than 18 and submits the form
- **THEN** the form shows an error that a minor cannot take over the role of a legal guardian
- **AND** no member is created
- **AND** "Eva Svobodová" stays a non-member legal guardian

#### Scenario: Failed guardian creation leaves nothing behind

- **WHEN** admin registers a minor with a new guardian whose e-mail is already in use
- **THEN** the form shows an error that the e-mail is already in use
- **AND** neither the member nor any guardian is created

#### Scenario: Adult registration takes over an existing non-member guardian

- **GIVEN** a non-member legal guardian "Eva Svobodová"
- **WHEN** admin registers an adult member and chooses to take over "Eva Svobodová"
- **THEN** the form is pre-filled with her name, e-mail and telephone
- **AND** after submission she becomes a club member and remains the legal guardian of her minors
- **AND** she gets a registration number by the usual rules, shown as her registration number everywhere in the application
- **AND** she can log in with either her new registration number or her former login number EXTnnnn

#### Scenario: Registration button not shown without permission

- **WHEN** user without MEMBERS:CREATE permission views the member list
- **THEN** the "Registrovat člena" button is not displayed

#### Scenario: Registration form shows validation errors

- **WHEN** admin submits the registration form with invalid or missing data
- **THEN** the form shows inline error messages for each invalid field
- **AND** no member is created

### Requirement: Member Registration Page Layout

The member registration page SHALL use a two-column layout.

#### Scenario: Registration page layout

- **WHEN** admin navigates to member registration page
- **THEN** the left column contains: personal data section, contact section, guardian takeover section, legal guardians section (only for a minor), address section
- **AND** the right column contains supplementary information section only (no documents or licenses)
- **AND** "Zrušit" and "Registrovat člena" (user-plus icon) buttons appear at the bottom of the form

#### Scenario: Legal guardians section lets admin choose or create guardians

- **WHEN** admin works with the legal guardians section of the registration form
- **THEN** they can add any number of guardians, each either chosen from the guardian candidates or entered as a new person with first name, last name, e-mail and telephone
- **AND** they can remove a guardian from the list before submitting
