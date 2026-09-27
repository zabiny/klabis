## MODIFIED Requirements

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
