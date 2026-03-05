# calendar-items Specification

## Purpose

This specification defines requirements for managing calendar items. Calendar items display date-based events on a calendar view, including automatic synchronization from Events and manual creation for club activities, reminders, and deadlines.

## Requirements

### Requirement: View Calendar Items

The system SHALL allow authenticated members to retrieve a list of calendar items for a specified date range.

#### Business Rules:

- **Maximum Date Range**: Date range must not exceed 366 days (1 year, including leap year)
- **Default Date Range**: If startDate and endDate are not provided, system defaults to current month
- **Sorting**: Results are sortable by id, name, startDate, endDate (default: startDate,asc)
- **No Pagination**: All items within date range are returned (no pagination)

#### Scenario: List calendar items for month view

- **GIVEN** authenticated user with ANY MEMBER role
- **WHEN** user makes GET request to /api/calendar-items?startDate=2026-06-01&endDate=2026-06-30
- **THEN** system returns HTTP 200 OK
- **AND** response includes calendar items that intersect with the specified date range
- **AND** response is sorted by startDate ascending (default)
- **AND** response includes HATEOAS navigation links (self, next, prev)
- **AND** Content-Type is application/prs.hal-forms+json

#### Scenario: List calendar items with default date range

- **GIVEN** authenticated user with ANY MEMBER role
- **WHEN** user makes GET request to /api/calendar-items (no date parameters)
- **THEN** system returns HTTP 200 OK
- **AND** system uses current month as date range (first day to last day)
- **AND** response includes calendar items for current month

#### Scenario: List calendar items exceeds maximum range

- **GIVEN** authenticated user with ANY MEMBER role
- **WHEN** user makes GET request to /api/calendar-items with date range exceeding 366 days
- **THEN** system returns HTTP 400 Bad Request
- **AND** error message indicates "Date range must not exceed 366 days"

#### Scenario: List calendar items includes sorting

- **GIVEN** authenticated user with ANY MEMBER role
- **WHEN** user makes GET request to /api/calendar-items?sort=name,desc
- **THEN** system returns HTTP 200 OK
- **AND** response includes calendar items sorted by name in descending order

#### Scenario: Invalid sort field

- **GIVEN** authenticated user with ANY MEMBER role
- **WHEN** user makes GET request to /api/calendar-items?sort=invalidField,asc
- **THEN** system returns HTTP 400 Bad Request
- **AND** error message indicates "Invalid sort field" and lists allowed fields

#### Scenario: List calendar items includes multi-day items

- **GIVEN** calendar item exists with startDate=2026-05-28 and endDate=2026-06-03
- **WHEN** user makes GET request to /api/calendar-items?startDate=2026-06-01&endDate=2026-06-30
- **THEN** system includes the item in response
- **AND** item spans both May and June

#### Scenario: Calendar response includes month navigation links

- **GIVEN** authenticated user requests calendar items for June 2026
- **WHEN** user makes GET request to /api/calendar-items?startDate=2026-06-01&endDate=2026-06-30
- **THEN** response includes HATEOAS "next" link with startDate=2026-07-01&endDate=2026-07-31
- **AND** response includes HATEOAS "prev" link with startDate=2026-05-01&endDate=2026-05-31
- **AND** links maintain same sort parameter as original request

#### Scenario: Month navigation preserves custom sort

- **GIVEN** authenticated user requests calendar items with custom sort
- **WHEN** user makes GET request to /api/calendar-items?startDate=2026-06-01&endDate=2026-06-30&sort=name,asc
- **THEN** "next" link includes sort=name,asc parameter
- **AND** "prev" link includes sort=name,asc parameter

#### Scenario: Unauthenticated access to calendar items

- **WHEN** unauthenticated user attempts to access /api/calendar-items
- **THEN** HTTP 401 Unauthorized is returned

### Requirement: Get Calendar Item Detail

The system SHALL allow authenticated members to retrieve detailed information about a specific calendar item.

#### Scenario: Get existing calendar item

- **GIVEN** authenticated user with ANY MEMBER role
- **WHEN** user makes GET request to /api/calendar-items/{id}
- **THEN** system returns HTTP 200 OK
- **AND** response includes all calendar item fields (name, description, startDate, endDate, eventId)
- **AND** response includes HATEOAS links based on item type and user permissions
- **AND** Content-Type is application/prs.hal-forms+json

#### Scenario: Get event-linked calendar item

- **GIVEN** calendar item linked to Event (eventId != null)
- **WHEN** user views calendar item detail
- **THEN** response includes link to Event (_links.event.href)
- **AND** edit/delete links are NOT included (read-only)

#### Scenario: Get manual calendar item

- **GIVEN** manual calendar item (eventId == null)
- **WHEN** user with CALENDAR:MANAGE permission views calendar item detail
- **THEN** response includes links: self, edit, delete
- **AND** edit/delete links are NOT included for users without CALENDAR:MANAGE

#### Scenario: Get non-existent calendar item

- **WHEN** authenticated user makes GET request to /api/calendar-items/{id} with non-existent ID
- **THEN** HTTP 404 Not Found is returned
- **AND** response includes error details with problem+json media type

### Requirement: Create Manual Calendar Item

The system SHALL allow authorized users to create manual calendar items.

#### Scenario: Create manual calendar item with all required fields

- **GIVEN** authenticated user with CALENDAR:MANAGE permission
- **WHEN** user submits POST /api/calendar-items with name, description, startDate, endDate
- **THEN** system creates a calendar item with eventId=null
- **AND** returns HTTP 201 Created with Location header
- **AND** response includes HAL+FORMS links (self, edit, delete)

#### Scenario: Create multi-day calendar item

- **GIVEN** authenticated user with CALENDAR:MANAGE permission
- **WHEN** user submits POST /api/calendar-items with startDate=2026-06-01 and endDate=2026-06-05
- **THEN** system creates a calendar item spanning 5 days
- **AND** item appears in calendar for all 5 dates

#### Scenario: Create calendar item without permission

- **WHEN** authenticated user without CALENDAR:MANAGE permission attempts to create a calendar item
- **THEN** HTTP 403 Forbidden is returned
- **AND** response includes error details with problem+json media type

#### Scenario: Create calendar item with invalid data

- **WHEN** user submits calendar item with missing required fields or invalid format
- **THEN** HTTP 400 Bad Request is returned
- **AND** response includes validation errors for each invalid field

#### Scenario: Create calendar item with end before start

- **WHEN** user submits calendar item with startDate=2026-06-10 and endDate=2026-06-05
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates endDate must be after or equal to startDate

### Requirement: Update Manual Calendar Item

The system SHALL allow authorized users to update manual calendar items.

#### Scenario: Update manual calendar item

- **GIVEN** authenticated user with CALENDAR:MANAGE permission
- **AND** manual calendar item exists (eventId == null)
- **WHEN** user submits PUT /api/calendar-items/{id} with updated name, description, dates
- **THEN** system updates the calendar item
- **AND** returns HTTP 200 OK with updated representation
- **AND** response includes HAL+FORMS links

#### Scenario: Attempt to update event-linked calendar item

- **GIVEN** event-linked calendar item exists (eventId != null)
- **WHEN** authenticated user with CALENDAR:MANAGE permission attempts to PUT /api/calendar-items/{id}
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates event-linked items cannot be manually edited

#### Scenario: Update calendar item without permission

- **WHEN** authenticated user without CALENDAR:MANAGE permission attempts to update a calendar item
- **THEN** HTTP 403 Forbidden is returned

### Requirement: Delete Manual Calendar Item

The system SHALL allow authorized users to delete manual calendar items.

#### Scenario: Delete manual calendar item

- **GIVEN** authenticated user with CALENDAR:MANAGE permission
- **AND** manual calendar item exists (eventId == null)
- **WHEN** user submits DELETE /api/calendar-items/{id}
- **THEN** system deletes the calendar item
- **AND** returns HTTP 204 No Content

#### Scenario: Attempt to delete event-linked calendar item

- **GIVEN** event-linked calendar item exists (eventId != null)
- **WHEN** authenticated user with CALENDAR:MANAGE permission attempts to DELETE /api/calendar-items/{id}
- **THEN** HTTP 400 Bad Request is returned
- **AND** error message indicates event-linked items cannot be manually deleted

#### Scenario: Delete calendar item without permission

- **WHEN** authenticated user without CALENDAR:MANAGE permission attempts to delete a calendar item
- **THEN** HTTP 403 Forbidden is returned

#### Scenario: Delete non-existent calendar item

- **WHEN** authenticated user submits DELETE /api/calendar-items/{id} with non-existent ID
- **THEN** HTTP 404 Not Found is returned

### Requirement: Automatic Synchronization from Events

The system SHALL automatically create, update, and delete calendar items when Events are published, updated, or cancelled.

#### Scenario: Create calendar item on Event publish

- **GIVEN** Event exists in DRAFT status
- **WHEN** Event is published (DRAFT → ACTIVE transition)
- **AND** EventPublishedEvent is fired
- **THEN** system creates a calendar item with:
  - name: Event.name
  - description: Event.location + " - " + Event.organizer + [newline + Event.websiteUrl if present]
  - startDate: Event.eventDate
  - endDate: Event.eventDate
  - eventId: Event.id
- **AND** calendar item is read-only (eventId != null)

#### Scenario: Update calendar item on Event update

- **GIVEN** Event exists with linked calendar item
- **WHEN** Event is updated (name, eventDate, location, or organizer changes)
- **AND** EventUpdatedEvent is fired with full Event data
- **THEN** system updates the linked calendar item with new values
- **AND** eventId remains unchanged

#### Scenario: Delete calendar item on Event cancellation

- **GIVEN** Event exists with linked calendar item
- **WHEN** Event is cancelled
- **AND** EventCancelledEvent is fired
- **THEN** system deletes the linked calendar item
- **AND** no calendar item remains for this Event

#### Scenario: Event completion does not affect calendar item

- **GIVEN** Event exists with linked calendar item
- **WHEN** Event is finished (ACTIVE → FINISHED transition)
- **THEN** calendar item remains visible on calendar
- **AND** EventFinishedEvent does NOT trigger calendar item changes

#### Scenario: Ignore missing calendar item on Event update

- **GIVEN** Event exists
- **AND** no linked calendar item exists
- **WHEN** EventUpdatedEvent is fired
- **THEN** system silently ignores the event
- **AND** logs warning about missing calendar item

#### Scenario: Ignore deleted calendar item on Event cancellation

- **GIVEN** Event exists with linked calendar item
- **AND** calendar item was already manually deleted
- **WHEN** EventCancelledEvent is fired
- **THEN** system silently ignores the event
- **AND** logs warning about missing calendar item

### Requirement: CalendarItemId Value Object

The system SHALL use CalendarItemId as a unique identifier for calendar items. The identifier wraps a UUID and provides type safety.

#### Scenario: CalendarItemId wraps UUID value

- **WHEN** a CalendarItemId is created with a valid UUID
- **THEN** identifier is successfully created
- **AND** identifier is immutable
- **AND** identifier provides equality based on the wrapped UUID

#### Scenario: CalendarItemId prevents null UUID

- **WHEN** a CalendarItemId is created with null UUID
- **THEN** validation fails with error indicating UUID cannot be null

### Requirement: Calendar Item Response Format

Calendar API responses SHALL follow HAL+FORMS specification with proper data serialization.

#### Scenario: Calendar dates serialized as ISO-8601

- **WHEN** a calendar item with startDate=2026-06-01 and endDate=2026-06-05 is returned
- **THEN** startDate field is serialized as "2026-06-01"
- **AND** endDate field is serialized as "2026-06-05"

#### Scenario: Calendar item response includes event link

- **WHEN** an event-linked calendar item is returned
- **THEN** response includes _links.event.href pointing to /api/events/{eventId}

#### Scenario: Calendar item response uses HAL+FORMS media type

- **WHEN** any calendar endpoint returns a response
- **THEN** Content-Type is application/prs.hal-forms+json
- **AND** response includes _links object for hypermedia navigation
