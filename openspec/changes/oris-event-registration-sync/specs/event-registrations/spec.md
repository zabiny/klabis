## ADDED Requirements

### Requirement: Registrations Entered In ORIS Appear In Klabis

For every ACTIVE event kept in step with ORIS, the system SHALL regularly bring in the ORIS entries of club members who exist in Klabis and are linked to their ORIS club membership. Entries of anybody else SHALL be ignored. A brought-in registration takes its category, SI card number, SI rental, ORIS note and registration time from ORIS, regardless of the Klabis registration deadline and the event's SI policy.

#### Scenario: A member entered in ORIS appears in the registrations list

- **GIVEN** an ACTIVE event imported from ORIS and a club member known in Klabis who is not registered for it in Klabis
- **WHEN** the club's registrar enters the member into the event in ORIS
- **THEN** after the next synchronisation run the member appears in the event's registrations list in Klabis
- **AND** the registration shows the category, SI card number, SI rental request and ORIS note entered in ORIS

#### Scenario: Registration time comes from ORIS

- **WHEN** a registration is brought in from ORIS
- **THEN** its registration time is the time the entry was created in ORIS
- **AND** it takes the matching place in the first-come-first-served order of the list

#### Scenario: Entry made after the Klabis registration deadline is still brought in

- **GIVEN** an ACTIVE event whose Klabis registration deadline has passed
- **WHEN** a club member is entered into the event in ORIS
- **THEN** the registration still appears in Klabis

#### Scenario: Entry that does not fit the event's SI policy is still brought in

- **GIVEN** an event that requires an SI card and does not allow rental
- **WHEN** a club member is entered into the event in ORIS without an SI card number
- **THEN** the registration appears in Klabis without an SI card number

#### Scenario: Entry of a sanctioned member is brought in

- **GIVEN** a club member who is blocked from registering in Klabis by a sanction
- **WHEN** the member is entered into an ACTIVE ORIS event in ORIS
- **THEN** the registration appears in Klabis like any other

#### Scenario: Entries of people unknown to Klabis are ignored

- **WHEN** ORIS lists an entry of a club member who does not exist in Klabis or is not linked to their ORIS club membership
- **THEN** no registration is created for that entry

#### Scenario: Draft events are not searched for entries

- **GIVEN** an event imported from ORIS that is still in DRAFT status
- **WHEN** a synchronisation run takes place
- **THEN** no registrations are brought in for it
- **AND** they are brought in by the first run after the event is published

#### Scenario: Entry in a category Klabis does not know yet

- **GIVEN** an ORIS entry in a category that the Klabis event does not have yet
- **WHEN** the synchronisation run tries to bring it in
- **THEN** the registration is not created in that run
- **AND** it is brought in by a later run once the event's categories have been synchronised from ORIS

### Requirement: A Klabis Registration Is Matched With The Member's ORIS Entry

When a member registered in Klabis is also entered into the same event in ORIS, the system SHALL match the two instead of creating a second registration. The registration SHALL take the category, SI card number, SI rental and ORIS note from ORIS, and SHALL keep its Klabis registration time, internal note and shared-service choices.

#### Scenario: Registrar enters a member who registered in Klabis

- **GIVEN** a member registered for an ORIS-imported ACTIVE event in Klabis
- **WHEN** the registrar enters the member into the event in ORIS with a different category
- **THEN** after the next synchronisation run the member still has a single registration for the event
- **AND** the registration shows the category entered in ORIS
- **AND** the registration time, internal note and shared transport and accommodation choices are unchanged

### Requirement: ORIS Changes To A Registration Reach Klabis

Once a registration is matched with an ORIS entry, the system SHALL keep it in step with ORIS automatically: changes to the category, SI card number, SI rental or ORIS note made in ORIS SHALL reach the registration without anyone asking for it. The system SHALL stop keeping the registrations of an event in step once the event is finished or cancelled.

#### Scenario: Category changed in ORIS reaches Klabis

- **GIVEN** a registration matched with an ORIS entry and not changed in Klabis since
- **WHEN** the registrar changes the member's category in ORIS
- **THEN** the registration in Klabis shows the new category after the next synchronisation run

#### Scenario: Registrations of a finished event stop being kept in step

- **WHEN** an event becomes finished or is cancelled
- **THEN** its registrations are no longer synchronised with ORIS
- **AND** they keep the values they had

### Requirement: A Registration Removed From ORIS Is Kept And Marked

When the ORIS entry matched with a registration disappears from ORIS, the system SHALL NOT delete the registration in Klabis. It SHALL mark the registration as "V ORISu nenalezena". The mark SHALL be shown on the registration detail and, to the event coordinator and users with EVENTS:REGISTRATIONS, in the registrations list. When the member is entered into the event in ORIS again, the mark SHALL disappear and the registration SHALL take the ORIS values.

#### Scenario: Entry deleted in ORIS stays in Klabis with a mark

- **GIVEN** a registration matched with an ORIS entry
- **WHEN** the registrar deletes the entry in ORIS
- **THEN** after the next synchronisation run the registration is still listed in Klabis
- **AND** the coordinator and users with EVENTS:REGISTRATIONS see it marked "V ORISu nenalezena"
- **AND** the member sees the mark on the detail of their registration

#### Scenario: Other members do not see the mark in the list

- **WHEN** a member without EVENTS:REGISTRATIONS who is not the coordinator views the registrations list
- **THEN** no "V ORISu nenalezena" mark is shown on other members' rows

#### Scenario: Entry re-created in ORIS removes the mark

- **GIVEN** a registration marked "V ORISu nenalezena"
- **WHEN** the registrar enters the member into the event in ORIS again
- **THEN** after the next synchronisation run the mark is gone
- **AND** the registration shows the values entered in ORIS

### Requirement: A Klabis Change To A Synchronised Registration Asks For A Decision

ORIS does not yet accept registration changes from Klabis. A change made in Klabis to the category, SI card number, SI rental or ORIS note of a registration matched with ORIS, or unregistering while the ORIS entry still exists, SHALL NOT be discarded by the next synchronisation; the registration SHALL instead wait for a decision by a user with SYNC:MANAGE, as described for any synchronised record. Changing the internal note or the shared-service choices SHALL never cause a decision.

#### Scenario: Member changes their category in Klabis

- **GIVEN** a member's registration matched with an ORIS entry
- **WHEN** the member changes their category in Klabis
- **AND** the registration is synchronised afterwards
- **THEN** the member's new category is kept
- **AND** the registration is marked as needing a decision about the difference against ORIS

#### Scenario: Member changes their ORIS note in Klabis

- **GIVEN** a member's registration matched with an ORIS entry
- **WHEN** the member changes the "Poznámka do ORIS" in Klabis and the registration is synchronised afterwards
- **THEN** the new note is kept
- **AND** the registration is marked as needing a decision

#### Scenario: Member unregisters in Klabis while still entered in ORIS

- **GIVEN** a member's registration matched with an ORIS entry
- **WHEN** the member unregisters from the event in Klabis
- **AND** the registration is synchronised afterwards
- **THEN** the member stays unregistered in Klabis
- **AND** a decision about the difference against ORIS is requested

#### Scenario: Decision resolves itself once ORIS agrees

- **GIVEN** a member unregistered in Klabis and the difference against ORIS awaits a decision
- **WHEN** the registrar deletes the member's entry in ORIS
- **THEN** after the next synchronisation run no decision is needed any more
- **AND** the member stays unregistered without a "V ORISu nenalezena" mark

#### Scenario: Internal note and shared-service choices never cause a decision

- **GIVEN** a member's registration matched with an ORIS entry
- **WHEN** the member changes the internal note or the shared transport or accommodation choice and the registration is synchronised afterwards
- **THEN** the changes are kept
- **AND** no decision is asked of anyone

#### Scenario: A kept difference asks again when ORIS changes

- **GIVEN** a user with SYNC:MANAGE decided to keep the difference between a registration and ORIS
- **WHEN** the registrar later changes the entry in ORIS
- **THEN** after the next synchronisation run a new decision is requested

#### Scenario: Taking the ORIS values after an unregistration restores the registration

- **GIVEN** a member unregistered in Klabis while still entered in ORIS and a decision is requested
- **WHEN** a user with SYNC:MANAGE takes the ORIS values
- **THEN** the member is registered again with the category, SI card, SI rental, ORIS note and registration time from ORIS
- **AND** without shared transport and accommodation choices and without an internal note

#### Scenario: Sending Klabis values to ORIS is not offered

- **WHEN** a user with SYNC:MANAGE resolves a difference on a registration
- **THEN** taking the ORIS values and keeping the difference are offered
- **AND** sending the Klabis values to ORIS is not offered

### Requirement: Registrations Can Be Loaded From ORIS On Demand

A user with SYNC:MANAGE SHALL be able to bring in the ORIS entries of an ACTIVE ORIS-synchronised event immediately from the event detail page, using the action "Načíst přihlášky z ORIS", instead of waiting for the scheduled run. The action brings in entries not yet in Klabis; registrations already matched with ORIS are kept in step by the regular synchronisation. The action SHALL NOT be offered to other users or for events that are not ACTIVE or not kept in step with ORIS.

#### Scenario: Synchronisation manager loads registrations from ORIS

- **GIVEN** an ACTIVE event kept in step with ORIS
- **WHEN** a user with SYNC:MANAGE clicks "Načíst přihlášky z ORIS" on the event detail page
- **THEN** the club members' ORIS entries not yet in Klabis are brought in immediately
- **AND** the registrations list shows them

#### Scenario: Action is not offered without the permission

- **WHEN** a user without SYNC:MANAGE opens the detail page of an ORIS-synchronised event
- **THEN** the "Načíst přihlášky z ORIS" action is not displayed

#### Scenario: Action is not offered for events not kept in step with ORIS

- **WHEN** a user with SYNC:MANAGE opens the detail of an event created in Klabis, or of a DRAFT, FINISHED or CANCELLED event
- **THEN** the "Načíst přihlášky z ORIS" action is not displayed

### Requirement: Synchronisation State Is Reachable From A Registration

For a registration matched with ORIS, the registration detail SHALL offer a way to open its synchronisation state, as for any synchronised entity: anyone who can see the registration detail sees whether it is kept in step with ORIS, how it stands and when it was last synchronised; only users with SYNC:MANAGE see the details and the actions available for any synchronised record. The registrations list SHALL NOT show the synchronisation state.

#### Scenario: Synchronisation manager opens a registration's synchronisation state

- **GIVEN** a registration matched with an ORIS entry that needs a decision
- **WHEN** a user with SYNC:MANAGE opens the registration detail and follows the synchronisation state link
- **THEN** they see the values held in Klabis and in ORIS and which side changed
- **AND** they can confirm the difference and resolve it

#### Scenario: Member sees the headline synchronisation state of their registration

- **GIVEN** a member's registration matched with an ORIS entry
- **WHEN** the member opens the detail of their registration and follows the synchronisation state link
- **THEN** they see that it is kept in step with ORIS, its current state and when it was last synchronised
- **AND** they are not shown the data held on either side nor any synchronisation action

#### Scenario: Registration created in Klabis only has no synchronisation state

- **WHEN** a user opens the detail of a registration for an event that is not kept in step with ORIS
- **THEN** no synchronisation state link is offered
