## MODIFIED Requirements

### Requirement: Filter Events by Year

The events list filter bar SHALL include a year selector that allows the user to restrict the list to a single calendar year. The selector exposes the past ten years and the next two years relative to the current year, plus an explicit "no year" option. The default value of the year selector is the **current year**.

The year selector and the time-window selector (Budoucí / Proběhlé / Vše) SHALL combine with AND semantics. When both are set, the list shows only events whose date falls within the selected year AND matches the selected time window. Selecting or changing one selector SHALL NOT modify the other.

The availability of the time-window options SHALL depend on the selected year:

- When **"no year"** or the **current year** is selected, all three time-window options (Budoucí, Proběhlé, Vše) are available.
- When a **year other than the current year** is selected, only "Vše" is available; "Budoucí" and "Proběhlé" SHALL be disabled in the UI. If the time-window selector was previously set to "Budoucí" or "Proběhlé", it SHALL be coerced to "Vše" at the moment the non-current year is selected.

When a non-current year is later replaced by "no year" or the current year, the disabled time-window options become available again. The coerced "Vše" value remains in effect until the user changes it; the previous time-window value is not restored.

#### Scenario: Default year selector value is the current year

- **GIVEN** the current year is 2026
- **WHEN** a user opens the events list page without any explicit filter
- **THEN** the year selector shows "2026" as the active value
- **AND** the list shows only events whose date is in 2026 combined with the default time-window selector value

#### Scenario: User filters by current year and "Budoucí"

- **GIVEN** the current year is 2026
- **WHEN** a user selects "2026" in the year selector and "Budoucí" in the time-window selector
- **THEN** the list shows only events whose date is in 2026 AND is today or later
- **AND** all three time-window options remain enabled

#### Scenario: User filters by current year and "Proběhlé"

- **GIVEN** the current year is 2026
- **WHEN** a user selects "2026" in the year selector and "Proběhlé" in the time-window selector
- **THEN** the list shows only events whose date is in 2026 AND is before today
- **AND** all three time-window options remain enabled

#### Scenario: User selects a past year

- **GIVEN** the current year is 2026 and the time-window selector is set to "Budoucí"
- **WHEN** the user selects "2024" in the year selector
- **THEN** the list shows only events whose date is in 2024
- **AND** the time-window selector is coerced to "Vše"
- **AND** the "Budoucí" and "Proběhlé" options are disabled

#### Scenario: User selects a future year

- **GIVEN** the current year is 2026 and the time-window selector is set to "Proběhlé"
- **WHEN** the user selects "2027" in the year selector
- **THEN** the list shows only events whose date is in 2027
- **AND** the time-window selector is coerced to "Vše"
- **AND** the "Budoucí" and "Proběhlé" options are disabled

#### Scenario: User changes time-window selector while a year is selected

- **GIVEN** the current year is 2026, the year selector is set to "2026", and the time-window selector is "Vše"
- **WHEN** the user changes the time-window selector to "Budoucí"
- **THEN** the year selector remains set to "2026"
- **AND** the list shows only events whose date is in 2026 AND is today or later

#### Scenario: User clears the year filter

- **GIVEN** the year filter is set to "2024" and the time-window selector is "Vše" (disabled options "Budoucí" and "Proběhlé")
- **WHEN** the user changes the year selector to "no year"
- **THEN** the year-based date constraint is removed
- **AND** all three time-window options become enabled
- **AND** the time-window selector remains set to "Vše"

#### Scenario: User switches from past year back to current year

- **GIVEN** the current year is 2026, the year selector is set to "2024", and the time-window selector is "Vše"
- **WHEN** the user changes the year selector to "2026"
- **THEN** the "Budoucí" and "Proběhlé" options become enabled
- **AND** the time-window selector remains set to "Vše" until the user changes it

#### Scenario: Year filter persists in the URL

- **WHEN** the user has selected a year and a time window and reloads the page
- **THEN** the same year and time window remain in effect
- **AND** the list shows only events matching both filters
