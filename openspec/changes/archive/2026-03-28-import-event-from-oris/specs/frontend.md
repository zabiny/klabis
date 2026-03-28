# frontend — delta spec: Import eventu z ORIS

## Requirement: Import button on events list page

The system SHALL display an "Import" button on the events list page for users with `EVENTS:MANAGE` permission when ORIS integration is active.

### Scenario: Import button visible when affordance present

- **WHEN** user with `EVENTS:MANAGE` permission views the events list page
- **AND** the events list response includes `importFromOris` HATEOAS affordance
- **THEN** an "Importovat z ORIS" button is displayed on the page

### Scenario: Import button absent when affordance missing

- **WHEN** the events list response does NOT include `importFromOris` HATEOAS affordance
- **THEN** the "Importovat z ORIS" button is not displayed

## Requirement: Import overlay

The system SHALL present a modal overlay when the user clicks "Importovat z ORIS".

### Scenario: Overlay opens and loads ORIS events

- **WHEN** user clicks "Importovat z ORIS"
- **THEN** a modal overlay opens
- **AND** the system fetches `GET /api/oris/events`
- **AND** a loading indicator is shown while fetching
- **AND** after loading, a selectbox is displayed with all returned ORIS events
- **AND** each option shows event date and event name (e.g. "2026-05-10 — Jihomoravský žebříček")
- **AND** no option is pre-selected

### Scenario: Empty list from ORIS

- **WHEN** `GET /api/oris/events` returns an empty list
- **THEN** selectbox is replaced with a message "Žádné závody k importu"
- **AND** submit button is disabled

### Scenario: ORIS events fetch fails

- **WHEN** `GET /api/oris/events` returns an error
- **THEN** an error message is displayed inside the overlay
- **AND** submit button is disabled

### Scenario: Cancel overlay

- **WHEN** user clicks "Zrušit" or closes the overlay
- **THEN** overlay closes without sending any import request

## Requirement: Submit import

The system SHALL submit the import request when the user selects an event and confirms.

### Scenario: Successful import

- **WHEN** user selects an event from the selectbox
- **AND** clicks "Importovat"
- **THEN** the system sends `POST /api/events/import` with `{ "orisId": <selected id> }`
- **AND** a loading indicator is shown on the submit button
- **AND** on HTTP 201 response, the overlay closes
- **AND** the user is redirected to the event detail page from the `Location` response header

### Scenario: Submit button disabled without selection

- **WHEN** no event is selected in the selectbox
- **THEN** the "Importovat" button is disabled

### Scenario: Duplicate import (409 Conflict)

- **WHEN** `POST /api/events/import` returns 409 Conflict
- **THEN** overlay remains open
- **AND** an error message is displayed: "Tento závod již byl importován"

### Scenario: Other import error

- **WHEN** `POST /api/events/import` returns any other error
- **THEN** overlay remains open
- **AND** a generic error message is displayed inside the overlay
