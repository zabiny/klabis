## Why

The application needs a calendar view to display events and other date-based items to users. Currently, Events exist but are not displayed on a calendar interface. Authorized users also need the ability to add custom calendar items (reminders, deadlines, club activities) beyond just Events, with proper access control.

## What Changes

- **New Calendar Bounded Context**: Add `calendar` module with `CalendarItem` aggregate
- **Event-Driven Synchronization**: Auto-generate calendar items from Events via domain events
  - `EventPublishedEvent` → Create CalendarItem
  - `EventUpdatedEvent` → Update linked CalendarItem
  - `EventCancelledEvent` → Delete linked CalendarItem
- **REST API**: Provide endpoints for managing calendar items with HATEOAS
- **Authorization**: Add `CALENDAR:MANAGE` authority for managing calendar items
- **Behavior**:
  - Event-linked items: Read-only, auto-sync, deleted on Event cancellation
  - Manual items: Full CRUD, multi-day support, only editable by authorized users

## Capabilities

### New Capabilities
- `calendar-items`: Manage calendar items with automatic synchronization from Events and manual creation capabilities

### Modified Capabilities
- `events`: Add `EventUpdatedEvent` domain event to `Event.update()` method

## Impact

- **New module**: `com.klabis.calendar` bounded context
- **New database table**: `calendar_items`
- **New authority**: `CALENDAR:MANAGE` in Spring Security
- **Modified**: Events module to fire `EventUpdatedEvent` on updates
- **API**: New endpoints `/api/calendar-items` with CRUD operations
- **Frontend**: Calendar page already exists, will consume new API
