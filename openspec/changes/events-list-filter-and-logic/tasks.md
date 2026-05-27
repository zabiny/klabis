## 1. Frontend — AND-combine year and time-window filters

- [x] 1.1 Write failing UI test: with year = current year and time window = "Budoucí", events list shows only events from current year with date today-or-later.
- [x] 1.2 Remove the side-effect where selecting a year forces the time-window selector to "Vše".
- [x] 1.3 Remove the side-effect where changing the time-window selector clears the year.
- [x] 1.4 Ensure both filter values are sent independently to the events query (year AND time window).
- [x] 1.5 Make the failing test pass; verify all existing events-list filter tests still pass.

## 2. Frontend — Disable Budoucí / Proběhlé for non-current years

- [ ] 2.1 Write failing UI test: selecting a past year disables "Budoucí" and "Proběhlé" and coerces the active time window to "Vše".
- [ ] 2.2 Write failing UI test: selecting a future year (non-current) disables "Budoucí" and "Proběhlé" and coerces the time window to "Vše".
- [ ] 2.3 Implement enabled/disabled state for time-window options based on whether the selected year equals the current year or is "no year".
- [ ] 2.4 Implement coercion of the active time-window value to "Vše" at the moment a non-current year is selected.
- [ ] 2.5 Add a tooltip / aria-disabled affordance on disabled options explaining why they are unavailable.
- [ ] 2.6 Make the failing tests pass.

## 3. Frontend — Switch-back and "no year" behaviour

- [ ] 3.1 Write failing UI test: switching from a non-current year back to the current year re-enables "Budoucí" and "Proběhlé" and leaves the active time window at "Vše".
- [ ] 3.2 Write failing UI test: switching from a non-current year to "no year" re-enables all options and leaves the time window at "Vše".
- [ ] 3.3 Implement re-enabling logic; verify no "previous value restore" logic is added.
- [ ] 3.4 Make the failing tests pass.

## 4. Frontend — Default year value

- [ ] 4.0a Write failing UI test: opening the events list with no URL filters renders with the year selector set to the current year and the list scoped to that year.
- [ ] 4.0b Change the default year-selector value from "no year" to the current year.
- [ ] 4.0c Verify that loading an explicit `?year=` value (including a deliberate "no year" sentinel) still overrides the default.
- [ ] 4.0d Make the failing test pass.

## 5. Frontend — URL state

- [ ] 5.1 Write failing test: loading the page with `?year=<past>&when=upcoming` normalises the URL/state so that the time window resolves to "Vše" on first render and the resulting list contains all events from that year.
- [ ] 5.2 Write failing test: reloading a URL with `?year=<current>&when=upcoming` keeps both filters active and shows the AND-combined list.
- [ ] 5.3 Update URL serialisation / deserialisation to treat year and time window as independent values.
- [ ] 5.4 Make the failing tests pass.

## 6. Verification

- [ ] 6.1 Run the full frontend test suite via developer:test-runner-skill.
- [ ] 6.2 Manual QA in the browser at http://localhost:3000 — walk through each scenario in the modified spec (default landing view, current-year + each time window, past year, future year, switch-back, clear year, deep link).
- [ ] 6.3 Confirm `openspec validate events-list-filter-and-logic --strict` passes.
