## MODIFIED Requirements

### Requirement: Multi-Event ORIS Import

The system SHALL allow an event manager to select multiple ORIS events at once in the "Import from ORIS" dialog and import them in a single batch. The dialog SHALL only offer ORIS events that are not yet present in the application, so the manager never selects an event that is already imported. The import processes each selected event independently: a failure to import one event SHALL NOT prevent the others from being imported (no all-or-nothing). The dialog SHALL allow at most a fixed maximum number of events to be selected in a single batch and SHALL make this limit clear to the manager before submitting. After the batch completes, the manager SHALL see a per-event result indicating success or failure for each selected event, where successfully imported events are clearly marked as successful and failed events as failed. The number of currently selected events SHALL be shown to the manager without redundant duplication. Selecting a single event remains a valid case of this batch flow.

#### Scenario: Manager selects multiple events to import

- **WHEN** an event manager opens the "Import from ORIS" dialog
- **THEN** each available ORIS event is presented with a selection control allowing more than one event to be selected at the same time
- **AND** the manager can select several events before confirming

#### Scenario: Already imported events are not offered

- **WHEN** an event manager opens the "Import from ORIS" dialog
- **THEN** ORIS events that have already been imported into the application are not shown in the list of events available to import
- **AND** only events that can still be imported are presented for selection

#### Scenario: No events remain to import

- **WHEN** every ORIS event for the selected region has already been imported
- **THEN** the dialog shows an empty state indicating there are no events left to import

#### Scenario: Manager imports the selected events in one action

- **WHEN** an event manager has selected several ORIS events and confirms the import
- **THEN** all selected events are imported in a single operation
- **AND** the manager is not required to repeat the import for each event individually

#### Scenario: Selection is limited to the maximum batch size

- **WHEN** an event manager has selected the maximum allowed number of ORIS events in the dialog
- **THEN** the manager cannot add further events to the selection
- **AND** the dialog makes the maximum limit clear to the manager

#### Scenario: One event fails but the rest are imported

- **WHEN** an event manager imports several ORIS events and one of them cannot be imported
- **THEN** the remaining selected events are still imported successfully
- **AND** the failing event does not cancel the whole batch

#### Scenario: Manager sees a per-event result summary

- **WHEN** a batch import finishes
- **THEN** the manager sees a summary listing each selected event with its date and name
- **AND** each successfully imported event is clearly marked as imported successfully
- **AND** each failed event is clearly marked as failed and shows the reason it could not be imported

#### Scenario: Selected count is shown once

- **WHEN** an event manager has selected one or more ORIS events in the dialog
- **THEN** the number of selected events is presented to the manager a single time, without being repeated elsewhere in the same dialog area

#### Scenario: Importing a single selected event

- **WHEN** an event manager selects exactly one ORIS event and confirms the import
- **THEN** that event is imported
- **AND** the result summary lists the single event with its outcome

#### Scenario: Confirm action is unavailable when nothing is selected

- **WHEN** an event manager has not selected any ORIS event in the dialog
- **THEN** the import cannot be confirmed
