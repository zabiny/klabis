## ADDED Requirements

### Requirement: Filter Events by Year

The events list filter bar SHALL include a year selector that allows the user to restrict the list to a single calendar year. The selector exposes the past ten years and the next two years relative to the current year, plus an explicit "no year" option.

When a year is selected, the list shows only events whose date falls within that year. Selecting a year SHALL implicitly switch the time-window selector to "Vše" (so events from past or future years remain visible regardless of the previous time-window setting).

Selecting "no year" (clearing the selection) SHALL restore the previous time-window behavior.

#### Scenario: User filters events by a specific year

- **WHEN** a user selects "2024" in the year selector
- **THEN** the list shows only events with date in 2024
- **AND** the time-window selector switches to "Vše" automatically

#### Scenario: User clears the year filter

- **GIVEN** the year filter is set to "2024"
- **WHEN** the user changes it to "no year"
- **THEN** the year-based date constraint is removed
- **AND** the time-window selector returns to its previous value

#### Scenario: Year filter persists in the URL

- **WHEN** the user has selected a year and reloads the page
- **THEN** the same year remains selected
- **AND** the list shows only that year's events

### Requirement: Event Coordinator Label Reads as "Vedoucí"

In every part of the user interface that exposes the event coordinator (table column header, filter label, detail section heading, form field label), the label SHALL read "Vedoucí" (or "Vedoucí akce" where context demands the longer form), not "Koordinátor". This is a localization preference; the underlying data field name in the API and the domain remains unchanged (the field continues to be referred to as the event coordinator in API contracts, OpenAPI documentation, and developer documentation).

#### Scenario: Events table header

- **WHEN** a user views the events list
- **THEN** the column that shows the event coordinator is headed "Vedoucí" (not "Koordinátor")

#### Scenario: Filter bar label

- **WHEN** a user opens the events list filter bar
- **THEN** the filter that targets the coordinator is labelled "Vedoucí" (not "Koordinátor")

#### Scenario: Event detail section

- **WHEN** a user views an event detail
- **THEN** the section displaying the coordinator's name reads "Vedoucí" (not "Koordinátor")

#### Scenario: Event create / update form field

- **WHEN** an event manager opens the create or update form for an event
- **THEN** the field for assigning a coordinator is labelled "Vedoucí" (not "Koordinátor")
