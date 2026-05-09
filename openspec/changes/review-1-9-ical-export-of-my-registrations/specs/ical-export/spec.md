## ADDED Requirements

### Requirement: Personal Calendar Feed for User Registrations

The system SHALL provide each authenticated user with a personal calendar feed in iCalendar format containing the events they are registered to. The feed is reachable through a stable URL that the user adds to their external calendar application (Google Calendar, Apple Calendar, Outlook). External calendars subscribed to this URL update automatically as the user registers, unregisters, or events get cancelled.

The feed URL SHALL be authenticated by a personal access token unique to the user, embedded in the URL as a query parameter, since calendar applications cannot present OAuth2 credentials.

The feed SHALL contain one entry per event the user is registered to, with the event date, name, location, organizer, and a link back to the event detail in Klabis. Cancelled events SHALL be marked as cancelled in the feed so the external calendar can visually mark or remove them. Events the user is no longer registered to SHALL disappear from the feed on the next refresh.

#### Scenario: User generates the calendar feed token for the first time

- **GIVEN** an authenticated member visits their profile and has not generated a calendar token yet
- **WHEN** the member triggers the "Vytvořit kalendářový feed" action
- **THEN** the system generates a new token, stores it cryptographically hashed, and displays the full subscribe URL once
- **AND** the URL includes the raw token as a query parameter

#### Scenario: User adds the feed URL to Google Calendar

- **GIVEN** the user has copied their feed URL
- **WHEN** the user adds it as a "subscribed calendar" in Google Calendar
- **THEN** Google Calendar fetches the iCalendar feed and displays the user's registered events on their calendar dates

#### Scenario: External calendar reflects a new registration

- **GIVEN** a user has the feed subscribed in their external calendar
- **WHEN** the user registers for a new event in Klabis
- **THEN** at the next refresh by the external calendar, the new event appears

#### Scenario: External calendar reflects an unregistration

- **GIVEN** a user has the feed subscribed and an event "X" is on their calendar
- **WHEN** the user unregisters from event "X"
- **THEN** at the next refresh, event "X" disappears from the calendar

#### Scenario: External calendar reflects a cancelled event

- **GIVEN** a user is registered to event "Y" and that event is in the user's calendar
- **WHEN** the event organizer cancels event "Y" in Klabis
- **THEN** at the next refresh, the calendar entry for "Y" is marked as cancelled (struck through or moved to a cancelled status, depending on the calendar client)

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

#### Scenario: Empty feed for a user with no registrations

- **GIVEN** a user has a feed token but is currently registered to no events
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
