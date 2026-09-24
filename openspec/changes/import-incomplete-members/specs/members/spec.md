## ADDED Requirements

### Requirement: Incomplete Member Data

A member brought in from ORIS may lack details that registering by hand requires. The system SHALL treat a member as incomplete when any of the following is true:

- neither the member nor their guardian has an e-mail address;
- neither the member nor their guardian has a telephone number;
- the member is a Czech national without a birth number;
- the member is a minor without a legal guardian.

Completeness SHALL always follow from the member's current details and SHALL never be set or cleared by hand. Only users with MEMBERS:MANAGE authority SHALL see whether a member is incomplete and what is missing.

#### Scenario: Admin sees which members are incomplete

- **WHEN** user with MEMBERS:MANAGE authority views the member list
- **THEN** each incomplete member's row is marked "Neúplné údaje"
- **AND** complete members carry no such mark

#### Scenario: Admin filters the list to incomplete members

- **WHEN** user with MEMBERS:MANAGE authority turns on the "Jen neúplní" filter in the member list
- **THEN** only incomplete members are shown
- **AND** the filter combines with the other list filters

#### Scenario: Admin sees what is missing on the member detail

- **WHEN** user with MEMBERS:MANAGE authority opens the detail of an incomplete member
- **THEN** a warning lists each missing item, for example "Chybí: rodné číslo, zákonný zástupce"
- **AND** the "Upravit profil" button lets them fill the details in

#### Scenario: Regular user sees nothing about completeness

- **WHEN** user without MEMBERS:MANAGE authority views the member list or a member's detail
- **THEN** no incompleteness mark, filter or warning is shown

#### Scenario: Filling in the missing details makes the member complete

- **GIVEN** an incomplete member whose only missing item is a legal guardian
- **WHEN** an admin adds a guardian with an e-mail address and telephone number and saves
- **THEN** the member is no longer marked incomplete
- **AND** the member no longer appears under the "Jen neúplní" filter

#### Scenario: A minor without a guardian becomes complete on turning 18

- **GIVEN** a minor who is incomplete only because they have no legal guardian
- **WHEN** they turn 18
- **THEN** their detail no longer lists a missing guardian

### Requirement: Edits Never Leave A Member Less Complete

Editing a member in Klabis SHALL NOT remove a detail whose absence would make the member incomplete. An incomplete member MAY be saved while still incomplete, as long as the edit does not add a new missing item. This keeps members who are complete today complete, while letting administrators fill in imported members step by step.

#### Scenario: Complete member cannot lose a required detail

- **GIVEN** a complete Czech member
- **WHEN** an admin clears the member's birth number and saves
- **THEN** the form shows an error that the birth number is required
- **AND** no changes are saved

#### Scenario: Incomplete member can be edited without completing everything

- **GIVEN** a member missing both a birth number and a legal guardian
- **WHEN** an admin fills in only the birth number and saves
- **THEN** the change is saved
- **AND** the member remains incomplete with only the guardian missing

#### Scenario: Unrelated edit on an incomplete member is saved

- **GIVEN** a member missing a telephone number
- **WHEN** an admin changes only the member's address and saves
- **THEN** the change is saved
- **AND** the member remains incomplete

#### Scenario: Incomplete member cannot lose another required detail

- **GIVEN** a member missing a telephone number but holding an e-mail address
- **WHEN** an admin removes the e-mail address and saves
- **THEN** the form shows an error that contact information is required
- **AND** no changes are saved

## MODIFIED Requirements

### Requirement: Member Registration Flow

The system SHALL process member registration by creating a user account and a member profile in a single flow. A member registered by hand SHALL be complete; registration does not send any e-mail.

#### Scenario: Admin registers a new member

- **WHEN** admin with MEMBERS:CREATE permission navigates to the registration page
- **AND** submits the registration form with valid data
- **THEN** the new member appears in the member list
- **AND** a user account awaiting activation is created for the member
- **AND** no e-mail is sent to the member

#### Scenario: Minor with only a guardian's e-mail is registered

- **WHEN** admin registers a minor whose only e-mail address is their guardian's
- **THEN** the member is registered successfully
- **AND** a user account awaiting activation is created for the member

#### Scenario: Registration button not shown without permission

- **WHEN** user without MEMBERS:CREATE permission views the member list
- **THEN** the "Registrovat člena" button is not displayed

#### Scenario: Registration form shows validation errors

- **WHEN** admin submits the registration form with invalid or missing data
- **THEN** the form shows inline error messages for each invalid field
- **AND** no member is created

#### Scenario: Registration by hand cannot create an incomplete member

- **WHEN** admin submits the registration form for a minor without a legal guardian
- **THEN** the form shows an error that a legal guardian is required for minors
- **AND** no member is created

## REMOVED Requirements

### Requirement: Welcome Email on Registration

**Reason**: Registration, whether by hand or from ORIS, no longer sends any e-mail. Importing the whole club from ORIS would otherwise send hundreds of activation links at once, many of which would expire unused, and members imported without an e-mail could not receive one anyway.

**Migration**: Members activate their account themselves by requesting an activation link with their registration number and e-mail address (see the `users` capability, "Password Setup Token Reissuance"). Clubs should tell members to do so.
