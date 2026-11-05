## ADDED Requirements

### Requirement: Event Facets

An event SHALL be composed of a small set of base fields plus zero or more independently toggleable **facets**, where each facet is an atomic group of related fields. The available facets are Categories, Ranking, and Pricing. A facet is either active on an event or not; active facets are the only place the corresponding fields exist. Facets are independent of one another — an event manager MAY activate any combination of facets on any event, in any order, with no facet requiring another.

#### Scenario: Event with only some facets active

- **WHEN** an event manager views an event that has the Categories facet active but not the Ranking or Pricing facets
- **THEN** the event detail shows a categories section
- **AND** shows no ranking section and no entry fee section

#### Scenario: Facets are independent

- **WHEN** an event manager activates the Pricing facet on an event that has no Categories facet
- **THEN** the event has an entry fee section and still has no categories section

### Requirement: Activate and Deactivate Event Facets

Users with EVENTS:MANAGE authority SHALL be able to activate a facet on an event and deactivate an active facet, from the event detail page. Activating a facet adds its section to the event; deactivating a facet removes that section and discards the data it held. The choice of which facets to activate is per event and SHALL NOT be constrained by the event's type.

#### Scenario: Manager activates a facet

- **WHEN** a manager activates the Ranking facet on an event that did not have it
- **THEN** a ranking section appears on the event detail, ready to be filled in

#### Scenario: Manager deactivates a facet

- **GIVEN** an event has the Pricing facet active with an entry fee set
- **WHEN** the manager deactivates the Pricing facet
- **THEN** the entry fee section is removed from the event detail
- **AND** the previously entered entry fee is no longer associated with the event

#### Scenario: Activate/deactivate controls hidden without permission

- **WHEN** a user without EVENTS:MANAGE authority views an event detail, including a coordinator of that event
- **THEN** no controls to activate or deactivate facets are shown

### Requirement: Categories Facet Cannot Be Deactivated While Registrations Use It

The Categories facet SHALL NOT be deactivated while any registration of the event holds a category from it. The system SHALL tell the manager how many registrations prevent the deactivation.

#### Scenario: Categories facet cannot be deactivated while registrations use its categories

- **GIVEN** an ACTIVE event with the Categories facet active and members registered in its categories
- **WHEN** a manager attempts to deactivate the Categories facet
- **THEN** the system prevents the deactivation
- **AND** tells the manager how many registrations hold a category

#### Scenario: Categories facet can be deactivated when no registration holds a category

- **GIVEN** an event with the Categories facet active and no registration holding a category
- **WHEN** a manager deactivates the Categories facet
- **THEN** the categories section is removed from the event detail

### Requirement: Deactivating An ORIS-Owned Facet Is Not Silently Undone

On an event imported from ORIS, activating or deactivating the Ranking or Pricing facet changes a field that ORIS owns. It SHALL be handled as any other manager's edit to an ORIS-owned field (see "A Manager's Edit To An ORIS Field Is Never Silently Overwritten" in the events capability): the next synchronisation SHALL NOT silently undo it.

#### Scenario: Deactivating an ORIS-owned facet on an ORIS-imported event is not silently undone

- **GIVEN** an ORIS-imported event that has the Ranking facet active with the ranking taken from ORIS
- **WHEN** a manager deactivates the Ranking facet
- **AND** the event is synchronised with ORIS afterwards
- **THEN** the event is not silently given the ORIS ranking back
- **AND** the event is marked as needing a decision about the difference against ORIS

### Requirement: Facets May Be Incomplete While the Event Is a Draft

While an event is in DRAFT status, an active facet MAY be empty or only partially filled in, so that a manager can enable a facet now and complete its data later. A facet's data SHALL never be internally contradictory even while incomplete.

#### Scenario: Manager activates a facet and leaves it empty in draft

- **GIVEN** an event in DRAFT status
- **WHEN** a manager activates the Pricing facet but does not enter an entry fee
- **THEN** the event is saved with the Pricing facet active and no entry fee yet

#### Scenario: Manager partially fills a facet in draft

- **GIVEN** an event in DRAFT status with the Categories facet active
- **WHEN** a manager saves with the categories list still empty
- **THEN** the event is saved with the Categories facet active and an empty categories list

### Requirement: Facet Completeness Enforced When Publishing

When an event is published (transitions from DRAFT to ACTIVE), every active facet SHALL be complete. If any active facet is incomplete, the system SHALL prevent publication and inform the manager which facet needs to be completed.

#### Scenario: Publishing is blocked by an incomplete facet

- **GIVEN** a DRAFT event with the Pricing facet active but no entry fee entered
- **WHEN** a manager attempts to publish the event
- **THEN** the system prevents the publication
- **AND** shows an error indicating the entry fee must be completed before publishing

#### Scenario: Publishing succeeds when all active facets are complete

- **GIVEN** a DRAFT event whose every active facet is fully filled in
- **WHEN** a manager publishes the event
- **THEN** the event becomes ACTIVE

#### Scenario: Publishing an event with no facets

- **GIVEN** a DRAFT event with no facets active
- **WHEN** a manager publishes the event
- **THEN** the event becomes ACTIVE

### Requirement: Edit Facet Data

Users with EVENTS:MANAGE authority, and members listed as coordinators of the event, SHALL be able to edit the data of an active facet on that event in DRAFT or ACTIVE status, in line with "Coordinator Implicit Edit Authority" in the events capability. Activating and deactivating facets is reserved for EVENTS:MANAGE. Editing a facet changes only that facet's fields and leaves the event's base fields and other facets unchanged.

#### Scenario: Manager edits one facet without affecting others

- **GIVEN** an event with both the Categories and Pricing facets active
- **WHEN** a manager changes the entry fee in the Pricing facet and saves
- **THEN** the entry fee is updated
- **AND** the categories remain unchanged

#### Scenario: Coordinator edits a facet of their event

- **GIVEN** a member who is a coordinator of a DRAFT or ACTIVE event without EVENTS:MANAGE permission, and the event has the Categories facet active
- **WHEN** the coordinator edits the categories and saves
- **THEN** the categories are updated
- **AND** no EVENTS:MANAGE permission is required

#### Scenario: Coordinator cannot edit a facet of another event

- **GIVEN** a member who is a coordinator of event A but not of event B and has no EVENTS:MANAGE permission
- **WHEN** the member views event B
- **THEN** no control to edit a facet is available

#### Scenario: Facet of a finished event cannot be edited

- **WHEN** a manager attempts to edit a facet of a FINISHED event
- **THEN** the system shows an error that finished events cannot be modified
