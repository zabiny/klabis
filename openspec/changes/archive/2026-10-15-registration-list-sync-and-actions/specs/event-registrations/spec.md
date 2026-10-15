## MODIFIED Requirements

### Requirement: Synchronisation State Is Reachable From A Registration

For a registration matched with ORIS, the registration detail and the registrations list SHALL offer a way to open its synchronisation state, as for any synchronised entity: anyone who can see the registration sees whether it is kept in step with ORIS, how it stands and when it was last synchronised; only users with SYNC:MANAGE see the details and the actions available for any synchronised record. The list marks only registrations matched with ORIS.

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

#### Scenario: Registrations list shows the synchronisation state of matched registrations

- **GIVEN** an event with several registrations, some matched with ORIS entries and some created in Klabis only
- **WHEN** a user views the registrations list on the event detail page
- **THEN** each row of a registration matched with ORIS shows an indicator of its synchronisation state
- **AND** rows of registrations created in Klabis only show no such indicator

#### Scenario: Member sees only the headline state of a registration in the list

- **GIVEN** a member without SYNC:MANAGE viewing the registrations list
- **WHEN** they open the synchronisation indicator of a row
- **THEN** they see that the registration is kept in step with ORIS, its current state and when it was last synchronised
- **AND** they are not shown the data held on either side nor any synchronisation action

#### Scenario: Synchronisation manager acts on a registration from the list

- **GIVEN** a registration in the list that needs a decision
- **WHEN** a user with SYNC:MANAGE opens its synchronisation indicator
- **THEN** they see the values held in Klabis and in ORIS and can resolve the difference

## ADDED Requirements

### Requirement: Registration Edit Dialog Offers Unregistering

When a member edits a registration that they are allowed to cancel, the edit dialog SHALL offer an "Odhlásit" action. Choosing it SHALL lead to the same confirmation of unregistering as elsewhere in the application; the registration is cancelled only after confirmation. The action is not offered when unregistering is not allowed.

#### Scenario: Member unregisters from the edit dialog

- **GIVEN** a member is registered for an event for which unregistering is still allowed
- **WHEN** the member opens the edit dialog of their registration and chooses "Odhlásit"
- **THEN** the edit dialog closes and the member is asked to confirm unregistering
- **AND** after confirming, the registration is cancelled and the list they came from shows the event as not registered

#### Scenario: Member declines to unregister

- **WHEN** the member chooses "Odhlásit" in the edit dialog and then dismisses the confirmation
- **THEN** the registration remains unchanged

#### Scenario: Unregister action is hidden when unregistering is not allowed

- **GIVEN** the registration deadline of the event has passed
- **WHEN** the member opens the edit dialog of their registration
- **THEN** no "Odhlásit" action is shown
