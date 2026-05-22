## ADDED Requirements

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
