## MODIFIED Requirements

### Requirement: View Calendar Items

The system SHALL allow authenticated members to view calendar items for a specified date range. The default view is the current month. The maximum range is 366 days.

The calendar view SHALL offer an optional "Můj rozvrh" filter that, when active, narrows the displayed items to those linked to events where the current user is either a registered participant or the event coordinator. The filter is off by default. While the filter is active, deadline calendar items and manual calendar items are not shown.

The current state of the "Můj rozvrh" filter SHALL be visible in the calendar header at all times, so the user can recognise that the filter is active even when the resulting view is empty.

#### Scenario: Member views calendar for a specific month

- **WHEN** authenticated member navigates to the calendar view for a specific month
- **THEN** all calendar items that intersect with that month are displayed
- **AND** items are sorted by start date ascending by default

#### Scenario: Calendar defaults to current month

- **WHEN** authenticated member opens the calendar without specifying a date range
- **THEN** the calendar shows items for the current month

#### Scenario: Date range exceeding one year shows error

- **WHEN** user requests a calendar view with a date range exceeding 366 days
- **THEN** the system shows an error that the date range must not exceed 366 days

#### Scenario: Calendar can be sorted

- **WHEN** user changes the sort order (e.g., by name descending)
- **THEN** the calendar items are reordered accordingly

#### Scenario: Invalid sort field shows error

- **WHEN** user attempts to sort by an unsupported field
- **THEN** the system shows an error listing the allowed sort fields

#### Scenario: Multi-day events span across month boundaries

- **WHEN** a calendar item starts in May and ends in June
- **AND** user views the June calendar
- **THEN** the item is included in the June view

#### Scenario: Calendar navigation shows next and previous month links

- **WHEN** user views the June 2026 calendar
- **THEN** a "next month" link navigates to July 2026
- **AND** a "previous month" link navigates to May 2026
- **AND** sort settings are preserved when navigating

#### Scenario: "Můj rozvrh" filter is off by default

- **WHEN** authenticated member opens the calendar for the first time in a session
- **THEN** the "Můj rozvrh" filter is off
- **AND** every calendar item that intersects the selected month is displayed

#### Scenario: "Můj rozvrh" filter includes event-date items where member is a participant

- **GIVEN** an authenticated member who has an active registration for an event in the selected month
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** the event-date calendar item linked to that event is displayed

#### Scenario: "Můj rozvrh" filter includes event-date items where member is the coordinator

- **GIVEN** an authenticated member who is the coordinator of an event in the selected month
- **AND** the member is not registered as a participant on that event
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** the event-date calendar item linked to that event is displayed

#### Scenario: "Můj rozvrh" filter excludes events where the member has no involvement

- **GIVEN** an authenticated member with no registration and no coordinator role on a published event in the selected month
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** the event-date calendar item linked to that event is not displayed

#### Scenario: "Můj rozvrh" filter hides deadline calendar items

- **GIVEN** an event in the selected month that has a registration deadline
- **AND** a "Přihlášky - {event name}" deadline calendar item exists for that event
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** the deadline calendar item is not displayed, regardless of whether the member is registered for the event

#### Scenario: "Můj rozvrh" filter hides manual calendar items

- **GIVEN** a manual calendar item in the selected month created by a calendar manager
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** the manual calendar item is not displayed

#### Scenario: "Můj rozvrh" filter applies to past months too

- **GIVEN** an authenticated member who had an active registration for an event last month
- **WHEN** the member navigates to last month's calendar with the "Můj rozvrh" filter on
- **THEN** the event-date calendar item linked to that past event is displayed

#### Scenario: Active "Můj rozvrh" filter is visible even when the result is empty

- **GIVEN** an authenticated member who has no registrations and no coordinator role in the selected month
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** the calendar grid renders for the month with no item chips in any day cell
- **AND** the active state of the "Můj rozvrh" filter remains visible in the calendar header
- **AND** the member can turn the filter off directly from the header without any additional navigation

#### Scenario: Member with no registrations and no coordinator role sees an empty result

- **GIVEN** an authenticated member who has neither registrations nor coordinator roles in the selected month
- **WHEN** the member turns the "Můj rozvrh" filter on
- **THEN** no calendar items are displayed

#### Scenario: Cancelled event does not appear under "Můj rozvrh"

- **GIVEN** an authenticated member who had an active registration for an event that has since been cancelled
- **WHEN** the member turns the "Můj rozvrh" filter on for the month containing that event
- **THEN** the cancelled event's calendar items are not displayed

#### Scenario: Filter state lives in the URL and resets across sessions

- **GIVEN** an authenticated member who has the "Můj rozvrh" filter turned on
- **WHEN** the member shares the current calendar URL with another (equally entitled) user, or bookmarks it
- **THEN** opening that URL restores the same filter state
- **AND** opening the calendar from the main navigation in a fresh session shows the filter turned off
