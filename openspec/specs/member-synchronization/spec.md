# Member Synchronization Specification

## Purpose

Keeps the club membership in Klabis aligned with ORIS, the source of truth for Czech orienteering: members the club has in ORIS are brought into Klabis automatically, and the personal data ORIS owns is kept in step, while data only Klabis knows is left untouched.

## Requirements

### Requirement: Club Members Are Brought In From ORIS On Their Own

The club's membership is maintained in ORIS, so the system SHALL find members the club has in ORIS but not yet in Klabis and bring them in by itself, without anyone naming them one by one. A member brought in this way SHALL be registered exactly as though an administrator had registered them by hand, so that everything that normally follows a registration follows here too.

#### Scenario: A member the club does not have yet is brought in

- **WHEN** the club has a member in ORIS who does not exist in Klabis
- **THEN** that member is registered in Klabis with the details held in ORIS
- **AND** they appear in the member list like any other member
- **AND** they are kept in step with ORIS from then on

#### Scenario: Registering brings its usual consequences

- **WHEN** a member is brought in from ORIS
- **THEN** everything that normally follows registering a member by hand follows here too, unchanged
- **AND** nothing is skipped or altered on account of the member having come from ORIS

#### Scenario: Members already brought in are not brought in twice

- **WHEN** the system looks for new members and a member has already been brought in
- **THEN** that member is not registered again
- **AND** they continue to be kept in step

### Requirement: Members Whose Club Membership Has Lapsed Are Not Brought In

ORIS records whether someone's club membership currently stands. The system SHALL bring in only members whose membership stands, so that people who left the club years ago are not registered as current members.

#### Scenario: A lapsed member is skipped

- **WHEN** the club has a member in ORIS whose membership no longer stands
- **THEN** that member is not registered in Klabis
- **AND** the system continues with the remaining members

#### Scenario: A lapsed member who is already in Klabis is left alone

- **GIVEN** a member who is kept in step with ORIS
- **WHEN** their membership in ORIS stops standing
- **THEN** their membership in Klabis is not changed on that account
- **AND** ending their membership in Klabis remains something a person decides

### Requirement: The Registration Number Comes From ORIS

ORIS issues the registration number that identifies a member across the whole sport, so for a member brought in from ORIS the system SHALL adopt the number ORIS holds rather than issuing one of its own.

#### Scenario: The ORIS registration number is adopted

- **WHEN** a member is brought in from ORIS
- **THEN** the member carries the registration number ORIS holds for them
- **AND** no new registration number is issued

#### Scenario: A member registered by hand still gets an issued number

- **WHEN** an administrator registers a member by hand without giving a registration number
- **THEN** the system issues one as it always has

#### Scenario: The registration number changes in ORIS

- **GIVEN** a member kept in step with ORIS
- **WHEN** their registration number changes in ORIS and nobody has changed it in Klabis
- **THEN** the member's registration number in Klabis is updated to match
- **AND** the member remains linked to the same ORIS record

### Requirement: Changes Made In ORIS Reach The Member Record

Once a member is kept in step with ORIS, the system SHALL apply changes made in ORIS to the member's record in Klabis, exactly as though an administrator had edited the member.

#### Scenario: A member's contact details change in ORIS

- **WHEN** a member's address, e-mail, telephone or SI chip number changes in ORIS
- **THEN** the member's record in Klabis is updated to match
- **AND** the change is visible to anyone who may view the member

#### Scenario: A detail ORIS does not hold is not cleared

- **GIVEN** a member kept in step with ORIS who has an SI chip number in Klabis
- **WHEN** ORIS holds no SI chip number for them
- **THEN** the chip number in Klabis is left as it is
- **AND** the difference is treated the same way as any other difference between the two sides

### Requirement: Only The Details ORIS Owns Are Kept In Step

Klabis records a great deal about a member that ORIS knows nothing about. The system SHALL keep in step only the details ORIS holds — registration number, name, date of birth, gender, nationality, birth number, e-mail, telephone, address and SI chip number — and SHALL leave everything else exactly as Klabis holds it.

#### Scenario: Details Klabis alone keeps are never disturbed

- **GIVEN** a member kept in step with ORIS whose record holds a driving licence, a trainer licence, a guardian, a bank account and dietary requirements
- **WHEN** the member is synchronised with ORIS
- **THEN** none of those details are changed or cleared
- **AND** only the details ORIS holds are considered

#### Scenario: A detail Klabis alone keeps is edited

- **WHEN** an administrator edits a detail that ORIS does not hold
- **THEN** the edit stands
- **AND** it is not reported as a difference against ORIS

### Requirement: A Member's Synchronisation Can Be Followed

Members are kept in step through the same machinery as every other linked record, so everything the synchronisation capability already guarantees SHALL apply to members: what state a member's synchronisation is in, when it last succeeded, what differs when a decision is needed, and the history of attempts. This state SHALL be reachable both from the member list and from the member's detail page.

#### Scenario: Reaching a member's synchronisation from the member detail page

- **WHEN** a user views a member's detail page for a member who is kept in step with ORIS
- **THEN** they can get from there to how the member stands against ORIS

#### Scenario: Reaching a member's synchronisation from the member list

- **WHEN** a user with the synchronisation permission views the member list
- **THEN** a member kept in step with ORIS shows a synchronisation status indicator in its row
- **AND** opening it shows how the member stands against ORIS

#### Scenario: Following one member's synchronisation

- **WHEN** a user with the synchronisation permission opens the synchronisation state of a member kept in step with ORIS
- **THEN** they see its current state and when it last succeeded
- **AND** they can see the details held on each side

#### Scenario: A member's birth number is part of what is compared

- **WHEN** a user with the synchronisation permission views a member whose birth number differs between Klabis and ORIS
- **THEN** the difference is shown to them
- **AND** a user without that permission is shown none of it

#### Scenario: A member not linked to ORIS

- **WHEN** a user views a member who was never brought in from ORIS
- **THEN** no way to reach a synchronisation state is offered for that member, neither in the list nor on the detail page
- **AND** the member is presented exactly as members were before ORIS synchronisation existed

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

### Requirement: One Member's Failure Does Not Stop The Rest

Bringing in members runs over the whole club at once. A member the system cannot bring in SHALL NOT prevent the remaining members from being brought in.

#### Scenario: One member cannot be brought in

- **WHEN** a member cannot be registered while the club's membership is being brought in
- **THEN** the remaining members are still brought in
- **AND** the member that could not be brought in is recorded so it can be looked into

#### Scenario: ORIS cannot be reached

- **WHEN** ORIS cannot be reached while looking for new members
- **THEN** no member is registered
- **AND** the attempt is made again later without anyone intervening

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

#### Scenario: A member with an incomplete address is brought in

- **WHEN** the club has a current member in ORIS whose address lacks a street, city, postal code or country
- **THEN** that member is registered in Klabis without an address
- **AND** administrators see that the address is missing

#### Scenario: A member without any e-mail address is brought in

- **WHEN** the club has a current member in ORIS with no e-mail address
- **THEN** that member is registered in Klabis
- **AND** a user account awaiting activation is created for them

### Requirement: Details Klabis Cannot Accept Are Left Out

A detail ORIS holds may be one Klabis cannot accept, such as a birth number, telephone number or e-mail address in an invalid format, or a birth number for someone who is not a Czech national. The system SHALL leave such a detail out of the member's record and still bring the member in, rather than refusing the whole member. A detail left out this way SHALL be treated exactly as if ORIS held none.

#### Scenario: A malformed birth number is left out

- **WHEN** a current member in ORIS has a birth number in an invalid format
- **THEN** the member is registered in Klabis without a birth number
- **AND** if the member is a Czech national they are marked as incomplete

#### Scenario: An invalid telephone number or e-mail address is left out

- **WHEN** a current member in ORIS has a telephone number or e-mail address in a format Klabis does not accept
- **THEN** the member is registered in Klabis without that detail
- **AND** if no other telephone number or e-mail address is available they are marked as incomplete

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
