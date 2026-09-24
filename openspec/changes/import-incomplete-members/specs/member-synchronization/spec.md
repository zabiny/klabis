## ADDED Requirements

### Requirement: Members With Incomplete Details Are Still Brought In

ORIS often lacks details Klabis would require of a member registered by hand, and it never holds a legal guardian. The system SHALL bring such members in anyway, as incomplete members (see the `members` capability, "Incomplete Member Data"), rather than leaving them out of Klabis.

#### Scenario: A member without a telephone number is brought in

- **WHEN** the club has a current member in ORIS with no telephone number
- **THEN** that member is registered in Klabis
- **AND** they are marked as incomplete for administrators

#### Scenario: A minor is brought in without a guardian

- **WHEN** the club has a current member in ORIS who is under 18
- **THEN** that member is registered in Klabis without a legal guardian
- **AND** administrators see that the guardian is missing

#### Scenario: A member without any e-mail address is brought in

- **WHEN** the club has a current member in ORIS with no e-mail address
- **THEN** that member is registered in Klabis
- **AND** a user account awaiting activation is created for them

### Requirement: Details Klabis Cannot Accept Are Left Out

A detail ORIS holds may be one Klabis cannot accept, such as a birth number in an invalid format or a birth number for someone who is not a Czech national. The system SHALL leave such a detail out of the member's record and still bring the member in, rather than refusing the whole member. A detail left out this way SHALL be treated exactly as if ORIS held none.

#### Scenario: A malformed birth number is left out

- **WHEN** a current member in ORIS has a birth number in an invalid format
- **THEN** the member is registered in Klabis without a birth number
- **AND** if the member is a Czech national they are marked as incomplete

#### Scenario: A birth number for a non-Czech national is left out

- **WHEN** a current member in ORIS who is not a Czech national has a birth number
- **THEN** the member is registered in Klabis without a birth number
- **AND** the member is not marked as incomplete on that account

### Requirement: Synchronisation May Leave A Member Incomplete

For the details ORIS owns, ORIS is the authority. When ORIS no longer holds such a detail, the system SHALL apply the change even if it leaves the member incomplete. It SHALL NOT refuse the change, and SHALL NOT hold on to the old value.

#### Scenario: ORIS drops a member's telephone number

- **GIVEN** a complete member kept in step with ORIS whose only telephone number is their own
- **WHEN** their telephone number is removed in ORIS
- **THEN** the telephone number is removed from their record in Klabis
- **AND** the member is marked as incomplete for administrators

#### Scenario: ORIS supplies a missing detail

- **GIVEN** a member kept in step with ORIS who is incomplete only because their birth number is missing
- **WHEN** a valid birth number is entered for them in ORIS
- **THEN** their record in Klabis gains the birth number
- **AND** the member is no longer marked as incomplete

## MODIFIED Requirements

### Requirement: The Import Can Be Started By Hand

A club will want to watch as its whole membership is brought in at once. The system SHALL therefore let a user with the synchronisation permission start the import themselves, from the member list, rather than only waiting for it to run on its own. The action SHALL be offered only when it could actually do something. Bringing members in SHALL NOT send them any e-mail.

#### Scenario: The administrator starts the import from the member list

- **WHEN** a user with the synchronisation permission starts the import from the member list
- **THEN** the club's members are brought in from ORIS
- **AND** the members that were brought in appear in the list

#### Scenario: Bringing members in sends no e-mail

- **WHEN** members are brought in from ORIS, whether started by hand or on the system's own schedule
- **THEN** no e-mail is sent to any of them
- **AND** each of them can activate their account themselves

#### Scenario: The action is not offered without the club key

- **WHEN** a user with the synchronisation permission views the member list while no club key is held
- **THEN** the action to start the import is not offered

#### Scenario: The action is not offered without the permission

- **WHEN** a user without the synchronisation permission views the member list
- **THEN** the action to start the import is not offered

#### Scenario: Starting the import by hand does not bring anyone in twice

- **GIVEN** the club's members have already been brought in
- **WHEN** a user with the synchronisation permission starts the import again
- **THEN** no member is registered a second time
- **AND** only members not yet brought in are added
