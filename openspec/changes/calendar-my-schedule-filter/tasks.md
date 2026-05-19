## 1. Backend — narrow vertical slice (participant match)

- [x] 1.1 Write failing controller/integration test: `GET /api/calendar?mySchedule=true` returns only event-date items linked to events where the current user has an active registration; deadline and manual items are excluded
- [x] 1.2 Write failing test: when `mySchedule` is absent or `false`, response is unchanged from the existing baseline
- [x] 1.3 Add `mySchedule` optional boolean `@RequestParam` to the calendar list controller (default behaviour unchanged when absent or `false`)
- [x] 1.4 Implement the use-case branch in the calendar application service: when `mySchedule=true`, call the events module with `EventFilter.none().withDateRange(from, to).withRegisteredBy(currentUserId)`, collect the resulting event IDs, and restrict calendar items to `linkedEventId IN (ids) AND itemType = event-date AND date IN [from, to]`
- [x] 1.5 Verify tests from 1.1 and 1.2 pass; refactor for clarity

## 2. Backend — extend the slice to coordinator match (union)

- [x] 2.1 Write failing test: `mySchedule=true` also returns event-date items where the current user is the event coordinator (but is not a participant)
- [x] 2.2 Write failing test: `mySchedule=true` returns the union — an event where the user is both participant and coordinator appears exactly once
- [x] 2.3 Extend the use case to run the second `EventFilter` query (`withCoordinator(currentUserId).withDateRange(from, to)`) and union the resulting event IDs with the participant set
- [x] 2.4 Verify tests from 2.1 and 2.2 pass; refactor for clarity

## 3. Backend — historical view and cancelled events

- [x] 3.1 Write failing test: `mySchedule=true` on a past month includes event-date items where the user was registered or coordinated in that past month
- [x] 3.2 Write failing test: `mySchedule=true` does not include items for cancelled events (because the calendar-items spec already removes their items; verify behaviour holds end-to-end)
- [x] 3.3 Verify tests from 3.1 and 3.2 pass without code changes; if any fail, adjust the use case or fixtures

## 4. Backend — pagination, sorting, and date-range preservation

- [x] 4.1 Write failing test: `mySchedule=true` preserves the existing sort behaviour (start date ascending by default) and respects explicit sort parameters
- [x] 4.2 Write failing test: `mySchedule=true` respects the existing 366-day maximum date range and returns the documented error when exceeded
- [x] 4.3 Verify tests pass; refactor if needed

## 5. Backend — HAL response and `_links`

- [x] 5.1 Write failing test: the self link on `GET /api/calendar?mySchedule=true&...` preserves the `mySchedule` parameter
- [x] 5.2 Write failing test: the `prev`/`next` month navigation links preserve the `mySchedule` parameter
- [x] 5.3 Implement link preservation in the controller's HAL response building
- [x] 5.4 Verify tests pass; refactor

## 6. Frontend — toggle in the calendar header

- [x] 6.1 Write failing component test: the calendar header renders a "Můj rozvrh" toggle control, default off when the URL does not carry `mySchedule=true`
- [x] 6.2 Write failing component test: turning the toggle on appends `mySchedule=true` to the URL; turning it off removes it
- [x] 6.3 Implement the toggle in the calendar page header, wired to URL search params
- [x] 6.4 Verify tests pass; refactor

## 7. Frontend — toggle drives the API call

- [x] 7.1 Write failing test: when the URL carries `mySchedule=true`, the calendar data fetch includes the parameter
- [x] 7.2 Wire the URL search param into the HAL fetch (or follow the self link with the new parameter) for the calendar page
- [x] 7.3 Verify the calendar renders the filtered subset against a mocked API response with only the event-date items in scope

## 8. Frontend — empty grid + active filter visibility

- [x] 8.1 Write failing test: when the API returns no items and `mySchedule=true` is in the URL, the calendar grid renders for the selected month with no item chips, and the active state of the toggle is visible in the header
- [x] 8.2 Confirm no banner, alert, or empty-state text is introduced
- [x] 8.3 Verify tests pass

## 9. Frontend — URL/shareable state and month navigation

- [x] 9.1 Write failing test: navigating to the next/previous month preserves `mySchedule=true` in the URL
- [x] 9.2 Write failing test: opening a deep-linked URL containing `mySchedule=true` renders the calendar with the toggle on
- [x] 9.3 Wire month navigation links to preserve the filter parameter (use the backend-provided `prev`/`next` links from task 5)
- [x] 9.4 Verify tests pass; refactor

## 10. Localisation

- [x] 10.1 Add the "Můj rozvrh" label to `src/localization/labels.ts`
- [x] 10.2 Use the label everywhere the toggle and the active-filter indicator are rendered

## 11. Cross-cutting verification

- [x] 11.1 Run the full backend test suite via the `test-runner` agent and confirm it is green
- [x] 11.2 Run the full frontend test suite via the `test-runner` agent and confirm it is green
- [x] 11.3 Manually exercise the feature end-to-end against `runLocalEnvironment.sh`: log in as the seeded admin (`ZBM9000`) and the seeded member (`ZBM9500`), turn the filter on/off, navigate months, share the URL between sessions
- [x] 11.4 Confirm `lint` / type-check on the frontend is clean

## 12. Frontend bundle refresh

- [x] 12.1 Run `npm run refresh-backend-server-resources` so the frontend bundle served from `https://localhost:8443` reflects the new toggle
- [x] 12.2 Verify the bundle change is staged for the same commit as the source changes
