# Calendar Items Specification

## Purpose

Covers the calendar view where members see upcoming club activities. Calendar items are either created automatically from published events or manually by calendar managers. Defines how members browse the calendar, view item details, and how managers create, edit, and delete manual items.
## Requirements
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

### Requirement: Get Calendar Item Detail

The system SHALL allow authenticated members to view detailed information about a specific calendar item.

#### Scenario: Member views calendar item detail

- **WHEN** authenticated member opens a calendar item's detail
- **THEN** all available fields are displayed: name, start date, end date
- **AND** the description is displayed only when the item has one

#### Scenario: Member views a calendar item without a description

- **WHEN** authenticated member opens the detail of a calendar item that has no description
- **THEN** the detail view shows the item without any description section or placeholder text

#### Scenario: Event-linked calendar item shows link to event

- **WHEN** user views a calendar item that originates from an event
- **THEN** a link to the related event is shown
- **AND** no edit or delete options are available (item is read-only)

#### Scenario: Manual calendar item shows edit and delete for authorized user

- **WHEN** user with CALENDAR:MANAGE permission views a manually created calendar item
- **THEN** edit and delete actions are available

#### Scenario: Manual calendar item shows no edit/delete without permission

- **WHEN** user without CALENDAR:MANAGE permission views a manually created calendar item
- **THEN** no edit or delete actions are available

### Requirement: Create Manual Calendar Item

The system SHALL allow users with CALENDAR:MANAGE permission to create manual calendar items. Name, start date, and end date are required. Description is optional; when provided, it SHALL NOT exceed 1000 characters. An empty or whitespace-only description submitted from the form SHALL be treated as no description.

#### Scenario: Manager creates a calendar item with a description

- **WHEN** user with CALENDAR:MANAGE permission submits the calendar item creation form with name, description, start date, and end date
- **THEN** the calendar item is created with the given description and appears in the calendar

#### Scenario: Manager creates a calendar item without a description

- **WHEN** user with CALENDAR:MANAGE permission submits the calendar item creation form with name, start date, and end date and leaves the description empty
- **THEN** the calendar item is created without a description and appears in the calendar

#### Scenario: Multi-day calendar item appears across all its dates

- **WHEN** user creates a calendar item spanning multiple days
- **THEN** the item appears in the calendar for all days in its range

#### Scenario: Create button not shown without permission

- **WHEN** user without CALENDAR:MANAGE permission views the calendar
- **THEN** no create calendar item button is shown

#### Scenario: Form shows validation errors for missing required fields

- **WHEN** user submits the calendar item form with a missing name, missing start date, missing end date, or invalid dates
- **THEN** the form shows inline validation errors

#### Scenario: Description exceeding length limit shows error

- **WHEN** user submits the calendar item form with a description longer than 1000 characters
- **THEN** the form shows an error that the description must not exceed 1000 characters

#### Scenario: End date before start date shows error

- **WHEN** user sets an end date before the start date
- **THEN** the form shows an error that the end date must be on or after the start date

### Requirement: Update Manual Calendar Item

The system SHALL allow users with CALENDAR:MANAGE permission to update manually created calendar items. Description remains optional on update; users may clear an existing description by submitting an empty value, and an empty or whitespace-only value SHALL be treated as no description. Event-linked calendar items cannot be manually edited.

#### Scenario: Manager updates a manual calendar item

- **WHEN** user with CALENDAR:MANAGE permission edits and saves a manual calendar item
- **THEN** the calendar item is updated with the new values

#### Scenario: Manager clears the description of a manual calendar item

- **WHEN** user with CALENDAR:MANAGE permission edits a manual calendar item and submits it with the description field cleared
- **THEN** the calendar item is saved without a description

#### Scenario: Event-linked calendar item cannot be manually edited

- **WHEN** user with CALENDAR:MANAGE permission attempts to edit an event-linked calendar item
- **THEN** the system shows an error that event-linked items cannot be manually edited

#### Scenario: Edit action not shown without permission

- **WHEN** user without CALENDAR:MANAGE permission views a calendar item
- **THEN** no edit action is available

### Requirement: Delete Manual Calendar Item

The system SHALL allow users with CALENDAR:MANAGE permission to delete manually created calendar items. Event-linked calendar items cannot be manually deleted.

#### Scenario: Manager deletes a manual calendar item

- **WHEN** user with CALENDAR:MANAGE permission confirms deletion of a manual calendar item
- **THEN** the item is removed from the calendar

#### Scenario: Event-linked calendar item cannot be manually deleted

- **WHEN** user with CALENDAR:MANAGE permission attempts to delete an event-linked calendar item
- **THEN** the system shows an error that event-linked items cannot be manually deleted

#### Scenario: Delete action not shown without permission

- **WHEN** user without CALENDAR:MANAGE permission views a calendar item
- **THEN** no delete action is available

### Requirement: Automatic Synchronization from Events

The system SHALL automatically maintain event-linked calendar items so that they always reflect the current state of published events. For each published event there SHALL always be one calendar item representing the event date. When the event has a registration deadline set, there SHALL additionally be one calendar item representing the registration deadline, labelled "Přihlášky - {event name}" and dated on the deadline day. When the originating event has no location, the event-date item is created without location information — its description is assembled only from the values that are actually available (location, organizer, website), and is left empty when none of these are present. The registration-deadline item has no description. Whenever the event is updated, the set of event-linked calendar items SHALL be updated to match the current event state: items that should exist are created or updated, and items that should no longer exist are removed. When an event is cancelled, every calendar item linked to that event SHALL be removed.

#### Scenario: Event-date calendar item created when event is published

- **WHEN** an event is published (transitions from DRAFT to ACTIVE)
- **THEN** a calendar item is automatically created with the event's name and date
- **AND** the calendar item's description is assembled from the event's location (when set), organizer, and optional website
- **AND** the calendar item is read-only (cannot be manually edited or deleted)

#### Scenario: Event-date calendar item created from event without a location

- **WHEN** an event is published that has no location
- **THEN** a calendar item is automatically created with the event's name and date
- **AND** the calendar item's description contains the organizer (and website when present) without any location prefix

#### Scenario: Registration-deadline calendar item created when a published event has a deadline

- **WHEN** an event with a registration deadline set is published
- **THEN** in addition to the event-date item, a second calendar item is automatically created
- **AND** the second item is labelled "Přihlášky - {event name}"
- **AND** the second item is dated on the registration deadline
- **AND** the second item has no description
- **AND** clicking the second item navigates to the source event's detail page

#### Scenario: No registration-deadline item created when the event has no deadline

- **WHEN** an event with no registration deadline is published
- **THEN** only the event-date calendar item is created
- **AND** no second calendar item appears for that event

#### Scenario: Event-date and registration-deadline items both appear on the same day when they coincide

- **WHEN** an event is published whose registration deadline equals the event date
- **THEN** two separate calendar items appear on that day
- **AND** one is the event-date item labelled with the event's name
- **AND** the other is the registration-deadline item labelled "Přihlášky - {event name}"

#### Scenario: Calendar items are updated when the event is updated

- **WHEN** an event's name, date, location, organizer, website, or registration deadline is updated
- **THEN** every event-linked calendar item for that event is reconciled with the new event state
- **AND** the event-date item reflects the current event date, name, and assembled description
- **AND** the registration-deadline item reflects the current deadline date and event name in its label

#### Scenario: Registration-deadline item appears when a deadline is added to an event

- **GIVEN** a published event with no registration deadline and no deadline calendar item
- **WHEN** the event is updated to add a registration deadline
- **THEN** a registration-deadline calendar item is created on the new deadline date
- **AND** the event-date calendar item remains unchanged apart from the update propagation

#### Scenario: Registration-deadline item disappears when a deadline is cleared

- **GIVEN** a published event with a registration deadline and an existing deadline calendar item
- **WHEN** the event is updated to clear the registration deadline
- **THEN** the registration-deadline calendar item is removed
- **AND** the event-date calendar item remains

#### Scenario: Event-linked calendar items are created on event update when missing

- **GIVEN** a published event whose event-linked calendar items do not currently exist (for example because of a prior failure)
- **WHEN** the event is updated
- **THEN** the expected event-linked calendar items for the current event state are created
- **AND** no stale items are left behind

#### Scenario: All calendar items deleted when event is cancelled

- **WHEN** an event is cancelled
- **THEN** every event-linked calendar item for that event is removed from the calendar
- **AND** this includes both the event-date item and the registration-deadline item when present

#### Scenario: Finishing an event does not affect calendar items

- **WHEN** an event transitions from ACTIVE to FINISHED
- **THEN** every event-linked calendar item for that event remains visible on the calendar

### Requirement: Personal Calendar Feed for "Můj rozvrh"

The system SHALL provide each authenticated user with a personal calendar feed in iCalendar format containing the events on their personal schedule — that is, every event in which the user either has an active registration or holds the event-coordinator role. The same union of memberships drives the "Můj rozvrh" filter in the in-app calendar; the feed and the in-app filter SHALL stay consistent in scope.

The feed is reachable through a stable URL that the user adds to their external calendar application (Google Calendar, Apple Calendar, Outlook). External calendars subscribed to this URL update automatically as the user registers, unregisters, gains or loses the coordinator role, or events get cancelled.

The feed URL SHALL be authenticated by a personal access token unique to the user, embedded in the URL as a query parameter, since calendar applications cannot present OAuth2 credentials.

The feed SHALL contain one entry per event on the user's schedule, with the event date, name, location, organizer, the user's role on the event when they coordinate it, and a link back to the event detail in Klabis. Cancelled events SHALL be marked as cancelled in the feed so the external calendar can visually mark or remove them. Events that leave the user's schedule (registration cancelled and not coordinator, or coordinator role removed and not registered) SHALL disappear from the feed on the next refresh.

#### Scenario: User generates the calendar feed token for the first time

- **GIVEN** an authenticated member visits their profile and has not generated a calendar token yet
- **WHEN** the member triggers the "Vytvořit kalendářový feed" action
- **THEN** the system generates a new token, stores it cryptographically hashed, and displays the full subscribe URL once
- **AND** the URL includes the raw token as a query parameter

#### Scenario: User adds the feed URL to Google Calendar

- **GIVEN** the user has copied their feed URL
- **WHEN** the user adds it as a "subscribed calendar" in Google Calendar
- **THEN** Google Calendar fetches the iCalendar feed and displays the user's scheduled events on their calendar dates

#### Scenario: External calendar reflects a new registration

- **GIVEN** a user has the feed subscribed in their external calendar
- **WHEN** the user registers for a new event in Klabis
- **THEN** at the next refresh by the external calendar, the new event appears

#### Scenario: External calendar reflects an unregistration

- **GIVEN** a user has the feed subscribed and an event "X" is on their calendar
- **AND** the user is not the coordinator of event "X"
- **WHEN** the user unregisters from event "X"
- **THEN** at the next refresh, event "X" disappears from the calendar

#### Scenario: External calendar reflects newly assigned coordinator role

- **GIVEN** a user has the feed subscribed
- **AND** an event "Y" exists for which the user is not registered and not the coordinator
- **WHEN** the event organizer assigns the user as the event coordinator of "Y"
- **THEN** at the next refresh, event "Y" appears in the calendar
- **AND** the entry indicates the user's coordinator role (in the description or another visible field)

#### Scenario: External calendar reflects removed coordinator role

- **GIVEN** a user has the feed subscribed and an event "Z" is on their calendar only because they are its coordinator
- **AND** the user is not registered as a participant on event "Z"
- **WHEN** the user is removed from the coordinator role on event "Z"
- **THEN** at the next refresh, event "Z" disappears from the calendar

#### Scenario: Event with both participant and coordinator role appears exactly once

- **GIVEN** a user is registered as a participant on event "W" and is also its coordinator
- **WHEN** the feed URL is fetched
- **THEN** event "W" appears exactly once in the feed
- **AND** the entry indicates the user's coordinator role

#### Scenario: External calendar reflects a cancelled event

- **GIVEN** a user has event "Q" on their schedule (registered or coordinating) and it is in the user's external calendar
- **WHEN** the event organizer cancels event "Q" in Klabis
- **THEN** at the next refresh, the calendar entry for "Q" is marked as cancelled (struck through or moved to a cancelled status, depending on the calendar client)

#### Scenario: User regenerates the calendar token after suspecting a leak

- **GIVEN** a user has a feed token and worries that the URL was leaked
- **WHEN** the user triggers the "Vygenerovat nový token" action with confirmation
- **THEN** the system replaces the stored hash with a new token
- **AND** the previous URL stops returning the feed
- **AND** the user must update their calendar subscription with the new URL

#### Scenario: Feed URL with an invalid or unknown token is rejected

- **WHEN** a request reaches the feed URL with a token that does not match any user
- **THEN** the response is an authentication error
- **AND** no calendar data is returned

#### Scenario: Empty feed for a user with no schedule

- **GIVEN** a user has a feed token but is currently neither registered to any event nor a coordinator of any event in the feed's date window
- **WHEN** the feed URL is fetched
- **THEN** the response is a valid iCalendar document with no events listed
- **AND** the calendar client treats it as an empty calendar (no error)

#### Scenario: Calendar entry includes link back to Klabis

- **GIVEN** an event "Z" is in the user's feed
- **WHEN** the calendar client renders the entry
- **THEN** the entry exposes a URL that opens the event detail page in Klabis

#### Scenario: User profile shows when the token was last set

- **GIVEN** a user has a feed token
- **WHEN** the user opens the calendar feed section of their profile
- **THEN** the profile displays a label indicating when the token was last generated (so the user can recognise if regeneration is needed after a leak)

