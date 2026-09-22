# Spec Delta

## Purpose

Provides club managers with an up-to-date, locally available catalog of orienteering disciplines sourced from ORIS, so that assigning a discipline to an event type never depends on ORIS being reachable at that moment.

## ADDED Requirements

### Requirement: Local Discipline Catalog Kept In Step With ORIS

The system SHALL maintain a catalog of orienteering disciplines, sourced from ORIS, that is kept up to date automatically without any user action. A discipline newly published in ORIS SHALL appear in the catalog on its own, and a discipline's name in the catalog SHALL follow ORIS when it changes there.

#### Scenario: A new ORIS discipline appears in the catalog automatically

- **WHEN** ORIS publishes a discipline that is not yet in the Klabis catalog
- **THEN** the discipline appears in the Klabis catalog without anyone requesting it
- **AND** it is available for a manager to assign to an event type

#### Scenario: A discipline's name changes in ORIS

- **WHEN** ORIS changes the name of a discipline already in the Klabis catalog
- **THEN** the catalog's entry is updated to the new name

### Requirement: Event Type Discipline Assignment Uses The Local Catalog

Users with the EVENTS:MANAGE authority mapping ORIS disciplines to an event type SHALL choose from the local discipline catalog. This choice SHALL remain available even when ORIS cannot currently be reached.

#### Scenario: Manager assigns a discipline while ORIS is unavailable

- **WHEN** a manager with EVENTS:MANAGE authority opens the event type form to assign ORIS disciplines and ORIS cannot be reached at that moment
- **THEN** the manager still sees the full list of known disciplines by name
- **AND** the manager can assign one to the event type

### Requirement: Manager Can Maintain A Manually Added Discipline By Hand

Users with the EVENTS:MANAGE authority SHALL be able to create a discipline directly in Klabis, ahead of ORIS discovering it, and SHALL be able to correct such a manually added discipline's name. A discipline that came from ORIS SHALL NOT be editable in Klabis — ORIS remains its sole source of truth for `code` and `name`.

#### Scenario: Manager adds a discipline ORIS has not published yet

- **WHEN** a manager with EVENTS:MANAGE authority creates a discipline with a code and a name
- **THEN** the discipline appears in the catalog immediately
- **AND** it is available for a manager to assign to an event type

#### Scenario: Manager corrects a manually added discipline's name

- **WHEN** a manager with EVENTS:MANAGE authority changes the name of a discipline that was created directly in Klabis, not sourced from ORIS
- **THEN** the catalog shows the corrected name

#### Scenario: Manager cannot edit a discipline sourced from ORIS

- **WHEN** a manager with EVENTS:MANAGE authority attempts to change the name of a discipline that came from ORIS
- **THEN** the attempt is refused
- **AND** the discipline's name is unchanged

### Requirement: Removing A Discipline Never Breaks An Event Type That Uses It

Users with the EVENTS:MANAGE authority SHALL be able to remove a discipline from active use. Removing it SHALL NOT delete it and SHALL NOT affect any event type that already has it assigned — only new assignments are prevented. A removed discipline SHALL be restorable to active use at any time.

#### Scenario: Manager removes a discipline that is still assigned to an event type

- **WHEN** a manager with EVENTS:MANAGE authority removes a discipline that one or more event types already have assigned
- **THEN** the removal succeeds
- **AND** those event types keep showing that discipline as assigned, unchanged
- **AND** the discipline no longer appears as a choice when assigning disciplines to an event type

#### Scenario: Manager restores a removed discipline

- **WHEN** a manager with EVENTS:MANAGE authority restores a previously removed discipline
- **THEN** the discipline is available again as a choice when assigning disciplines to an event type
