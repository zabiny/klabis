## Why

GitHub issue #109 ("Chci mit moznost filtrovat v kalendari podle ruznych kriterii", milestone `MVP`, labels `clen klubu`, `hostujici clen`, `question`) asks for user-driven filtering of the calendar by various criteria — deliberately broad, because the exact set of useful filters is up for discussion (hence `question`).

Today the `calendar-items` spec `View Calendar Items` supports:
- Date-range selection (default current month, max 366 days).
- Sorting (by name, by date).
- Month navigation.
- One filter, "Můj rozvrh" (query parameter `mySchedule`): narrows the calendar to event-date items of events where the user is a registered participant or a coordinator, hides deadline and manual items, is off by default, is shown in the calendar header at all times and lives in the URL (shipped as `calendar-my-schedule-filter`). The same union drives the personal iCalendar feed.

Nothing else can be filtered. Without "Můj rozvrh" every authenticated member sees every calendar item in the selected range. The backend repository criterion `CalendarFilter` already carries an item-type set and an event-id set, but the REST endpoint exposes only `mySchedule`.

This proposal complements issue #110 (filter by my groups, which adds a single group-relevance filter). Issue #109 is broader — it is the umbrella "let the user narrow down the calendar" feature, and "Můj rozvrh" is the first filter of it, which also sets the pattern the remaining ones should follow (URL state, visible in the header, off by default).

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `calendar-items`: extend `View Calendar Items` with a set of user-driven filters. The exact filter set is an open question.

## Impact

**Affected specs:**
- `openspec/specs/calendar-items/spec.md` — `View Calendar Items` gains scenarios for each filter added. `Get Calendar Item Detail` is unaffected.

**Affected code (backend, calendar module):**
- The list query accepts additional optional filter parameters.
- The HAL-Forms affordance for the calendar list may expose the filter controls.

**Affected code (frontend):**
- Calendar page gains a filter bar with the confirmed filter controls.

**APIs (REST):** additive — new optional query parameters on `GET /api/calendar`.

**Dependencies:**
- Filter by group depends on issue #110 and on event-to-group tagging (`gh-113-event-training-group-tagging`).
- Filter by "only events I'm registered to" is satisfied by the shipped "Můj rozvrh".

**Data:** none.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change. Because the issue scope is deliberately open, the main job of the proposal is to pick which filters land and which are deferred.

1. **Which filters belong in the MVP cut?** Candidate filters:
   - (a) **Item type** — event-date items, registration-deadline items, manual calendar items. Simple toggle group.
   - (b) **Group relevance** (*moje skupiny* vs. vše) — see proposal #110.
   - (c) **Registration status** — only events I'm registered to. Largely covered by "Můj rozvrh" (participant OR coordinator); only a narrower "just my registrations" variant would be new.
   - (d) **Competition level / event category** — national / regional / club-only. Requires event classification, currently unspecified.
   - (e) **Organizer** — same as the events-list organizer filter.
   - (f) **Full-text search** on item name/description.
   - (g) **Location / distance** — home range within X km. Requires geocoding, heavy; probably out of scope.
   - (h) **Date range** — already exists.

   Recommend for MVP: (a) + (b) + (f). (c) is already delivered as "Můj rozvrh". Defer (d), (e), (g).

2. **Relationship with #110.** Issue #110 is essentially one of the filters covered here. Options:
   - Merge: #110 becomes part of this proposal (closed as covered).
   - Keep separate: #110 lands the group-filter feature first (because it has a specific acceptance criterion); this proposal adds the rest.

   Recommend: keep separate — #110 is concrete, #109 is the umbrella. Confirm.

3. **Filter persistence.** Decided by "Můj rozvrh": the state lives in the URL and resets in a fresh session (`Filter state lives in the URL and resets across sessions`). Keep the same for every new filter.

4. **Multi-select vs. single-select per filter.** Recommend: multi-select for type, single-select for group (defaulting to "my groups").

5. **Full-text search scope.** Name only, or name + description? Recommend: name only for MVP.

6. **Are filters combinable (AND) or independent?** Recommend: AND across filter dimensions, OR inside a single dimension's multi-select.

7. **Filter affordance.** "Můj rozvrh" is a boolean query parameter on `GET /api/calendar` that the link processor preserves on next/previous month links. Mirror that for the new parameters; the events list filters (type, year) use URL-held filters in the frontend as well.

8. **UI placement.** Is there room for a filter bar above the calendar grid, or should filters live in a dropdown? Recommend: collapsible filter bar, same pattern as the events list (if it exists).

9. **Empty-state UX.** What does the calendar show when the filter yields zero items? A dedicated empty-state message. Recommend: yes, a cheerful empty-state ("Žádné položky").

10. **Default filter values per persona.** Should regular members default to "my groups" while managers default to "vše"? Depends on answer to #110's question 6. Recommend: same defaults — members see "my groups"; managers also see "my groups" by default but the toggle is there.

11. **Can a filter reference a single Training Group id (e.g., a trainer wants to see only their group)?** Recommend: yes, add a secondary "konkrétní skupina" picker inside the group filter. Low cost on top of (b).

12. **Scope confirmation.** Confirm this proposal stays within `calendar-items` spec and does not add filters to `View Calendar Item Detail` or other requirements.
