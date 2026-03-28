# events — delta spec: Import eventu z ORIS

## Requirement: Import Event from ORIS

The system SHALL allow authorized users to create a new event by importing data from ORIS orienteering system.

**Precondition:** ORIS integration must be active (profile `oris`). When inactive, the import affordance is not present.

### Scenario: Import affordance present when ORIS active

- **WHEN** authenticated user with `EVENTS:MANAGE` permission views the events list
- **AND** ORIS integration is active
- **THEN** the response includes an `importFromOris` affordance

### Scenario: Import affordance absent when ORIS inactive

- **WHEN** ORIS integration is not active
- **THEN** the events list response does NOT include `importFromOris` affordance

### Scenario: Import event successfully

- **WHEN** authenticated user with `EVENTS:MANAGE` permission submits `POST /api/events/import` with `{ "orisId": 9876 }`
- **THEN** the system fetches event details from ORIS (`getEvent?id=9876`)
- **AND** creates a new event in DRAFT status with mapped fields:
  - `name` ← ORIS `Name`
  - `eventDate` ← ORIS `Date`
  - `location` ← ORIS `Place`
  - `organizer` ← first non-blank of `Org1.Abbr`, `Org2.Abbr`; fallback `"---"`
  - `websiteUrl` ← `https://oris.ceskyorientak.cz/Zavod?id={orisId}`
  - `orisId` ← ORIS `ID` (stored internally, not exposed in API)
- **AND** returns HTTP 201 Created with `Location` header pointing to the new event

### Scenario: Import without permission

- **WHEN** authenticated user without `EVENTS:MANAGE` permission submits `POST /api/events/import`
- **THEN** HTTP 403 Forbidden is returned

### Scenario: Duplicate import rejected

- **WHEN** an event with the given `orisId` already exists (any status)
- **THEN** HTTP 409 Conflict is returned
- **AND** no new event is created

### Scenario: ORIS event not found

- **WHEN** ORIS API returns no data for the given `orisId`
- **THEN** HTTP 404 Not Found is returned

## Requirement: orisId Internal Field

The `Event` aggregate SHALL store the ORIS source identifier as an internal field.

### Scenario: orisId uniqueness enforced

- **WHEN** creating an event with an `orisId`
- **THEN** the database enforces a unique constraint on `oris_id`
- **AND** `orisId` is NOT included in any API response

### Scenario: Manually created events have no orisId

- **WHEN** event is created via `POST /api/events` (manual creation)
- **THEN** `orisId` is null
- **AND** uniqueness constraint allows multiple null values
