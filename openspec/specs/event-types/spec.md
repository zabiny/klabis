# Event Types Specification

## Purpose

Defines the catalog of event types used to categorize club events (e.g. "Trénink", "Pohárový závod", "Mistrovství"). Managers with EVENTS:MANAGE authority maintain the catalog; the types are referenced from individual events for display, filtering, and ORIS import auto-mapping.

## Requirements

### Requirement: Manage Event Types Catalog

The system SHALL provide a catalog of event types that can be assigned to club events. Each event type has a unique name (case-insensitive), an optional display color (hex), and a sort order that determines its position in lists and filters. Users with the EVENTS:MANAGE authority SHALL be able to create, rename, recolor, reorder, and delete event types.

An event type that is currently assigned to one or more events SHALL NOT be deletable; the user must first reassign or clear the type on the affected events. The system SHALL inform the user in this case which events block the deletion.

#### Scenario: Manager creates a new event type

- **GIVEN** an authenticated user with EVENTS:MANAGE authority is on the "Typy akcí" administration page
- **WHEN** the user opens the create dialog, fills in name "Pohárový závod" and color #ffaa00, and submits
- **THEN** the new event type appears in the list
- **AND** the type is available in event create/update forms

#### Scenario: Manager renames an event type

- **GIVEN** an event type "Pohárový závod" is in use by several events
- **WHEN** the manager renames it to "Pohárový závod (KP)"
- **THEN** the event type's name changes
- **AND** all events keep their type assignment with the new name

#### Scenario: Manager attempts to delete an event type that is in use

- **GIVEN** an event type is assigned to one or more events
- **WHEN** the manager attempts to delete the type
- **THEN** the system rejects the deletion with an error indicating that the type is in use
- **AND** the error lists at least one event that uses the type

#### Scenario: Manager deletes an event type that is not in use

- **GIVEN** an event type that is not assigned to any event
- **WHEN** the manager confirms deletion
- **THEN** the type is removed from the catalog
- **AND** it no longer appears in event create/update form dropdowns

#### Scenario: Names are unique case-insensitively

- **GIVEN** an event type "Trénink" exists
- **WHEN** a manager attempts to create a new type with name "trénink" or "TRÉNINK"
- **THEN** the system rejects the create with an error that the name already exists

#### Scenario: Member without EVENTS:MANAGE cannot manage the catalog

- **GIVEN** an authenticated member without EVENTS:MANAGE authority
- **WHEN** the member opens the application
- **THEN** the "Typy akcí" administration item is not in the navigation
- **AND** direct attempts to access the catalog management endpoints are rejected with an authorization error
