## Why

The current events list has two filters — the time-window selector (Budoucí / Proběhlé / Vše) and the year selector — that override each other. Selecting a year forces the time window to "Vše", and switching the time window clears the year. This makes natural combinations impossible to express: a user cannot ask "which events happened earlier this year" or "what is still upcoming this year" without doing the date math in their head. Letting the two filters combine with AND semantics is the obvious mental model and removes hidden state changes that surprise users.

## What Changes

- The time-window selector and the year selector on the events list combine with **AND** semantics instead of overriding each other.
- Selecting a year no longer forces the time-window selector to "Vše".
- Changing the time-window selector no longer clears the year.
- When the **current year** is selected, all three time-window options (Budoucí / Proběhlé / Vše) are available.
- When a **non-current year** is selected, only "Vše" makes sense (the other two would either always be empty for past years or always equal "Vše" for past years where no event lies in the future). "Budoucí" and "Proběhlé" SHALL be disabled in the UI, and the active selection SHALL be coerced to "Vše".
- The **default value of the year selector** is the current year (instead of "no year"). The events list opens by default scoped to the current calendar year combined with the default time window.
- **BREAKING (UI behaviour only)**: the implicit "year clears the time window" and "time window clears the year" rules are removed. The default landing view of the events list changes from "all upcoming events" to "upcoming events in the current year". URL filter state semantics change accordingly.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `events`: Replace the "Filter Events by Year" requirement so that the year filter combines with the time-window filter via AND, and define how the time-window options behave when a non-current year is selected.

## Impact

- **Frontend**: events list page filter bar — wiring between year selector and time-window selector; disabled-state handling for time-window options; URL state serialisation.
- **Backend**: none expected. The existing events query API already accepts time window and date-range / year filters independently; the change is in how the frontend composes them.
- **Specs**: `openspec/specs/events/spec.md` — modify the "Filter Events by Year" requirement and its scenarios; verify the "View Events List" section stays consistent.
- **Users**: deep links / shared URLs that relied on the old override behaviour will resolve to a different (more intuitive) result set.
