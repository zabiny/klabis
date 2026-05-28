## ADDED Requirements

### Requirement: Multi-Event ORIS Import

The system SHALL allow an event manager to select multiple ORIS events at once in the "Import from ORIS" dialog and import them in a single batch. The import processes each selected event independently: a failure to import one event SHALL NOT prevent the others from being imported (no all-or-nothing). After the batch completes, the manager SHALL see a per-event result indicating success or failure for each selected event. Selecting a single event remains a valid case of this batch flow.

#### Scenario: Manager selects multiple events to import

- **WHEN** an event manager opens the "Import from ORIS" dialog
- **THEN** each available ORIS event is presented with a selection control allowing more than one event to be selected at the same time
- **AND** the manager can select several events before confirming

#### Scenario: Manager imports the selected events in one action

- **WHEN** an event manager has selected several ORIS events and confirms the import
- **THEN** all selected events are imported in a single operation
- **AND** the manager is not required to repeat the import for each event individually

#### Scenario: One event fails but the rest are imported

- **WHEN** an event manager imports several ORIS events and one of them cannot be imported (for example it was already imported)
- **THEN** the remaining selected events are still imported successfully
- **AND** the failing event does not cancel the whole batch

#### Scenario: Manager sees a per-event result summary

- **WHEN** a batch import finishes
- **THEN** the manager sees a summary listing each selected event with its date and name and an indicator of whether it was imported successfully or failed
- **AND** failed events show the reason they could not be imported

#### Scenario: Importing a single selected event

- **WHEN** an event manager selects exactly one ORIS event and confirms the import
- **THEN** that event is imported
- **AND** the result summary lists the single event with its outcome

#### Scenario: Confirm action is unavailable when nothing is selected

- **WHEN** an event manager has not selected any ORIS event in the dialog
- **THEN** the import cannot be confirmed
