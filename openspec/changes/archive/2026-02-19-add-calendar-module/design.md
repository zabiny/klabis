## Context

The application currently has an Events bounded context for managing orienteering events but lacks a calendar view. The frontend CalendarPage expects a `/api/calendar-items` endpoint that returns items with start/end dates and descriptions. Events exist as aggregates with `eventDate` (single date) and fire `EventPublishedEvent` and `EventCancelledEvent` domain events. There is no `EventUpdatedEvent`.

**Current state:**
- Events module: Fully functional with create/update/publish/cancel lifecycle
- Frontend: CalendarPage exists with expected interface
- Missing: Calendar bounded context, calendar items, and API

**Constraints:**
- Must follow Spring Modulith architecture (separate bounded contexts)
- Event-driven sync required (no direct module dependencies)
- HATEOAS compliance mandatory for all API responses
- Frontend consumes via HAL+FORMS
- GDPR compliance required
- Target: 10+ concurrent users, <500ms API response

## Goals / Non-Goals

**Goals:**
- Display Events on calendar view automatically
- Allow authorized users to add custom calendar items (reminders, deadlines, club activities)
- Keep Event-linked calendar items synchronized with Event changes
- Provide read access to all logged-in members
- Restrict write access to users with CALENDAR:MANAGE authority
- Support multi-day calendar items for manual entries

**Non-Goals:**
- Personal/private calendars (single shared calendar for now)
- Time precision (date-only, no hours/minutes)
- Recurring calendar items
- Calendar item categories/tags
- Integration with external calendar systems (Google Calendar, etc.)
- Calendar event reminders/notifications

## Decisions

### 1. Separate Calendar Bounded Context

**Decision**: Create new `calendar` module as separate bounded context.

**Rationale**:
- Calendar will eventually support more than just Events (trainings, deadlines, meetings)
- Clear separation of concerns (calendar display vs. event management)
- Follows DDD bounded context principle
- Easier to extend later without modifying Events module

**Alternatives considered**:
- **Add to Events module**: Simpler now, but couples calendar display to Events concept
- **Shared module**: Violates bounded context principles

### 2. Event-Driven Synchronization via Domain Events

**Decision**: Use Spring Modulith event handlers to sync Events → CalendarItems.

**Rationale**:
- Decoupled architecture (no direct Calendar → Events dependency)
- Automatic sync via outbox pattern (reliable, handles failures)
- Follows existing pattern (EventPublishedEvent, EventCancelledEvent already in use)
- Calendar module only consumes events, doesn't know about Event lifecycle

**Event flow**:
```
Event.update() → EventUpdatedEvent
    ↓ (Spring Modulith outbox)
EventUpdatedEventHandler (calendar module)
    ↓
Find CalendarItem by eventId → Update with new Event data
```

**Alternatives considered**:
- **Direct repository call**: Calendar service queries Events repo directly - violates dependency rule
- **Polling**: Calendar periodically queries for updates - inefficient, delayed

### 3. Event-Linked Items Are Read-Only

**Decision**: CalendarItems with `eventId != null` are read-only. Only manual items (`eventId == null`) can be edited by CALENDAR:MANAGE users.

**Rationale**:
- Prevents data divergence (manual edits overwritten on Event update)
- Clear separation: Events are source of truth for event-linked items
- Simplifies logic (no conflict resolution needed)

**Alternatives considered**:
- **Allow edits with "manual override" flag**: More flexible but complex, user confusion
- **Allow all edits with overwrite warning**: Defeats purpose of auto-sync

### 4. Store Event ID Without Validation

**Decision**: CalendarItem stores `eventId` as nullable field, no validation that Event exists.

**Rationale**:
- Simple and resilient
- CalendarItem remains functional even if Event is deleted
- YAGNI - orphaned items unlikely due to outbox reliability
- Follows "store ID only" guidance

**Alternatives considered**:
- **Validate on save**: Query Events to check existence - unnecessary coupling
- **Auto-delete orphaned items**: Scheduled cleanup - adds complexity, rare case

### 5. CalendarItem Description Format (Option C)

**Decision**: Auto-generated description includes location, organizer, and website URL if present.

**Format**:
```
event.websiteUrl != null
  ? event.location + " - " + event.organizer + "\n" + event.websiteUrl.value()
  : event.location + " - " + event.organizer
```

**Rationale**:
- Provides useful context in calendar view
- Website URL allows quick access to event details
- Newline for readability
- Matches frontend expectation of "description" field

**Alternatives considered**:
- **Option A (simple)**: `location + " - " + organizer` - lacks website
- **Option B (detailed)**: "(more info)" placeholder - less useful

### 6. Date Range Query for Calendar View

**Decision**: Repository query finds items that intersect with requested month range.

**SQL**:
```sql
SELECT * FROM calendar_items
WHERE (start_date <= :endMonth AND end_date >= :startMonth)
ORDER BY start_date, end_date
```

**Rationale**:
- Supports multi-day items that span month boundaries
- Single query for entire month view
- Indexed on (start_date, end_date) for performance

**Alternatives considered**:
- **Exact date match**: Misses items spanning months
- **Multiple queries**: Start date in month OR end date in month - duplicates items

### 7. Add EventUpdatedEvent to Events Module

**Decision**: Add `EventUpdatedEvent` domain event to Event.update() method.

**Payload**:
```java
public record EventUpdatedEvent(
    EventId eventId,
    String name,
    LocalDate eventDate,
    String location,
    String organizer,
    WebsiteUrl websiteUrl
)
```

**Rationale**:
- Complete event data needed for CalendarItem updates
- Prevents Calendar handler from querying Events repo (decoupled)
- Follows principle of event-driven communication

**Alternatives considered**:
- **Only EventId**: Handler queries Events repo - creates dependency

### 8. Event Handlers Named After Events

**Decision**: Event handlers in Calendar module named after events they handle, not the aggregate they update.

**Naming**:
- `EventPublishedEventHandler` (not CalendarItemCreatedEventHandler)
- `EventUpdatedEventHandler` (not CalendarItemUpdatedEventHandler)
- `EventCancelledEventHandler` (not CalendarItemDeletedEventHandler)

**Rationale**:
- Follows domain event naming convention
- Clearer intent: "handle EventUpdatedEvent" vs "update CalendarItem"
- Consistent with Spring Modulith event handler naming

### 9. CALENDAR:MANAGE Authority

**Decision**: Add new authority `CALENDAR:MANAGE` to Spring Security Authority enum.

**Permissions**:
- ANY MEMBER: View calendar items (GET /api/calendar-items)
- CALENDAR:MANAGE: Add/edit/delete manual items (POST/PUT/DELETE /api/calendar-items)

**Rationale**:
- Explicit permission for calendar management
- Follows existing pattern (EVENTS:MANAGE, MEMBERS:MANAGE)
- Separate from general admin/organizer roles (flexible authorization)

**Alternatives considered**:
- **ROLE_ORGANIZER**: Too broad, may not manage calendar
- **ROLE_ADMIN**: Over-privileged for calendar management

### 10. Delete Event-Linked Items on Event Cancellation

**Decision**: EventCancelledEventHandler deletes CalendarItem when Event is cancelled.

**Rationale**:
- Cancelled events shouldn't appear on calendar
- Consistency: Event status (CANCELLED) not shown, so CalendarItem shouldn't be shown
- User expectation: Cancelled event disappears from calendar

**EventFinishedEvent**: Ignored (events remain visible as historical items)

### 11. Idempotent Event Handlers

**Decision**: Event handlers silently ignore edge cases with warning logs.

**Cases**:
- EventUpdatedEventHandler: CalendarItem already deleted → ignore, log warning
- EventCancelledEventHandler: CalendarItem already deleted → ignore, log warning

**Rationale**:
- Handlers should be idempotent (safe to retry)
- No user-facing errors for transient states
- Outbox may deliver events out of order

**Alternatives considered**:
- **Throw exception**: Could cause outbox retry loops
- **Validate before operation**: Additional complexity, no benefit

## Risks / Trade-offs

### Risk 1: Event-Linked Items Data Loss

**Risk**: If Event is deleted but CalendarItem remains, users see orphaned items without context.

**Mitigation**:
- Unlikely due to outbox reliability
- CalendarItem still functional (just has event ID reference)
- Future: Could add "Event deleted" badge in UI

### Risk 2: Event-Linked Items Read-Only Limitation

**Risk**: Users may want to customize event-linked items (e.g., add notes).

**Trade-off**:
- Simpler implementation
- Prevents data divergence
- Alternative: Users can create separate manual items for notes

### Risk 3: Event Date Changes Confusion

**Risk**: If Event date changes multiple times quickly, CalendarItem updates may lag.

**Mitigation**:
- Outbox ensures eventual consistency
- Calendar handler overwrites with latest Event data
- Acceptable for calendar use case (not real-time)

### Risk 4: Performance of Date Range Query

**Risk**: Large number of calendar items could slow month view query.

**Mitigation**:
- Indexed on (start_date, end_date) - efficient range queries
- Cache calendar items for month view (future optimization)
- Reasonable scale: 10+ concurrent users, club calendar (not public social calendar)

## Migration Plan

1. **Add EventUpdatedEvent to Events module**
   - Create domain event record with full Event data
   - Fire in Event.update() method
   - Add unit tests

2. **Create Calendar module structure**
   - Set up bounded context package structure
   - Add Spring Modulith configuration

3. **Implement Calendar domain**
   - CalendarItem aggregate root
   - CalendarItemId value object
   - CalendarRepository interface
   - Validation rules (endDate >= startDate)

4. **Implement persistence layer**
   - Create calendar_items table with migration script
   - JDBC repository adapter
   - CalendarMemento for Spring Data JDBC

5. **Implement event handlers**
   - EventPublishedEventHandler: Create CalendarItem
   - EventUpdatedEventHandler: Update CalendarItem
   - EventCancelledEventHandler: Delete CalendarItem

6. **Add CALENDAR:MANAGE authority**
   - Update Authority enum
   - Update Spring Security configuration

7. **Implement REST API**
   - CalendarController with CRUD endpoints
   - HATEOAS links (_links)
   - HAL+FORMS templates for create/edit
   - Authorization (@HasAuthority)

8. **Frontend integration**
   - Update CalendarPage to use new API
   - Verify calendar items display correctly
   - Test CRUD operations with CALENDAR:MANAGE

9. **Rollback strategy**
   - Drop calendar_items table
   - Remove CALENDAR:MANAGE authority
   - Remove EventUpdatedEvent from Events module
   - Disable Calendar module (comment out in package scan)

## Open Questions

None. All design decisions have been made based on exploration phase.
