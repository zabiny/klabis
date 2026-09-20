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
