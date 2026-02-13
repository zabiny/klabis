## 1. Events Module Changes

- [ ] 1.1 Create EventUpdatedEvent domain event record with full Event data (eventId, name, eventDate, location, organizer, websiteUrl)
- [ ] 1.2 Add EventUpdatedEvent.publish() static factory method
- [ ] 1.3 Modify Event.update() to fire EventUpdatedEvent with updatable fields
- [ ] 1.4 Write unit tests for EventUpdatedEvent (payload validation, factory method)
- [ ] 1.5 Write unit tests for Event.update() verifying EventUpdatedEvent is published
- [ ] 1.6 Write integration test for event-driven EventUpdatedEvent publication

## 2. Calendar Module Setup

- [ ] 2.1 Create calendar bounded context package structure (com.klabis.calendar)
- [ ] 2.2 Add Spring Modulith @ApplicationModule annotation to calendar package
- [ ] 2.3 Add calendar module to application context (component scan or module configuration)
- [ ] 2.4 Create CalendarItemId value object (UUID wrapper, validation, factory methods)
- [ ] 2.5 Write unit tests for CalendarItemId (null validation, equality, factory methods)

## 3. Calendar Domain Model

- [ ] 3.1 Create CalendarItem aggregate root with fields: id, name, description, startDate, endDate, eventId, auditMetadata
- [ ] 3.2 Implement CalendarItem.create() factory method with validation (endDate >= startDate, name/description not blank)
- [ ] 3.3 Implement CalendarItem.update() method with validation for manual items (eventId == null)
- [ ] 3.4 Implement CalendarItem.delete() method (domain logic if needed)
- [ ] 3.5 Add @AggregateRoot, @Identity, @Association annotations (jMolecules)
- [ ] 3.6 Write unit tests for CalendarItem creation validation
- [ ] 3.7 Write unit tests for CalendarItem.update() with valid and invalid inputs
- [ ] 3.8 Write unit tests for CalendarItem event-linked read-only enforcement

## 4. Calendar Persistence Layer

- [ ] 4.1 Create CalendarRepository interface with methods: save, findById, findByDateRange, delete, findByEventId
- [ ] 4.2 Create database migration script for calendar_items table (id, name, description, start_date, end_date, event_id, audit metadata)
- [ ] 4.3 Add indexes: idx_calendar_items_date_range (start_date, end_date), idx_calendar_items_event_id (event_id)
- [ ] 4.4 Create CalendarMemento with Spring Data JDBC annotations and field mapping
- [ ] 4.5 Implement CalendarMemento.toCalendarItem() reconstruction method
- [ ] 4.6 Implement CalendarMemento.fromCalendarItem() conversion method
- [ ] 4.7 Create CalendarJdbcRepository extending CrudRepository
- [ ] 4.8 Implement CalendarRepositoryAdapter implementing CalendarRepository interface
- [ ] 4.9 Implement findByDateRange query with date range intersection logic
- [ ] 4.10 Implement findByEventId query for event handler lookups
- [ ] 4.11 Write repository unit tests for CalendarRepositoryAdapter
- [ ] 4.12 Write repository integration tests for CalendarJdbcRepository with real database

## 5. Event Handlers (Event-Driven Sync)

- [ ] 5.1 Create EventPublishedEventHandler in calendar module
- [ ] 5.2 Implement EventPublishedEventHandler.handle() to create CalendarItem from Event data
- [ ] 5.3 Build CalendarItem description: location + " - " + organizer + [newline + websiteUrl if present]
- [ ] 5.4 Set startDate and endDate from Event.eventDate (same date for events)
- [ ] 5.5 Set eventId from EventPublishedEvent.eventId
- [ ] 5.6 Check for duplicate CalendarItem (prevent creating if exists)
- [ ] 5.7 Add @ApplicationModuleListener annotation for event subscription
- [ ] 5.8 Write unit tests for EventPublishedEventHandler (happy path, duplicate handling)

- [ ] 5.9 Create EventUpdatedEventHandler in calendar module
- [ ] 5.10 Implement EventUpdatedEventHandler.handle() to update linked CalendarItem
- [ ] 5.11 Find CalendarItem by eventId from EventUpdatedEvent
- [ ] 5.12 Update CalendarItem with new Event data (name, description, dates)
- [ ] 5.13 Log warning and ignore if CalendarItem not found (idempotent)
- [ ] 5.14 Write unit tests for EventUpdatedEventHandler (update, missing item)

- [ ] 5.15 Create EventCancelledEventHandler in calendar module
- [ ] 5.16 Implement EventCancelledEventHandler.handle() to delete linked CalendarItem
- [ ] 5.17 Find CalendarItem by eventId from EventCancelledEvent
- [ ] 5.18 Delete CalendarItem if found
- [ ] 5.19 Log warning and ignore if CalendarItem not found (idempotent)
- [ ] 5.20 Write unit tests for EventCancelledEventHandler (delete, missing item)

- [ ] 5.21 Write integration test for event-driven sync flow (Event → CalendarItem)
- [ ] 5.22 Write integration test for update sync flow (Event.update → CalendarItem.update)
- [ ] 5.23 Write integration test for cancellation flow (Event.cancel → CalendarItem.delete)

## 6. Security Configuration

- [ ] 7.1 Add CALENDAR_MANAGE to Authority enum
- [ ] 7.2 Update Spring Security configuration to recognize CALENDAR:MANAGE authority
- [ ] 7.3 Update Authority enum tests

## 7. Calendar API Layer

- [ ] 6.1 Create CalendarItemDto with fields: name, description, startDate, endDate, eventId
- [ ] 6.2 Create CreateCalendarItemCommand with validation annotations
- [ ] 6.3 Create UpdateCalendarItemCommand with validation annotations
- [ ] 6.4 Create CalendarManagementService application service
- [ ] 6.5 Implement listCalendarItems() method with date range filtering and pagination
- [ ] 6.6 Implement getCalendarItem() method with HATEOAS link building
- [ ] 6.7 Implement createCalendarItem() method with validation and authorization
- [ ] 6.8 Implement updateCalendarItem() method with event-linked read-only check
- [ ] 6.9 Implement deleteCalendarItem() method with event-linked read-only check
- [ ] 6.10 Create CalendarController with @RequestMapping("/api/calendar-items")
- [ ] 6.11 Implement GET /api/calendar-items endpoint with startDate/endDate query parameters
- [ ] 6.12 Implement GET /api/calendar-items/{id} endpoint
- [ ] 6.13 Implement POST /api/calendar-items endpoint with @HasAuthority("CALENDAR:MANAGE")
- [ ] 6.14 Implement PUT /api/calendar-items/{id} endpoint with @HasAuthority("CALENDAR:MANAGE")
- [ ] 6.15 Implement DELETE /api/calendar-items/{id} endpoint with @HasAuthority("CALENDAR:MANAGE")
- [ ] 6.16 Add HATEOAS _links to all responses (self, edit, delete for manual items, event for linked items)
- [ ] 6.17 Add HAL+FORMS _templates for create/update operations
- [ ] 6.18 Handle CalendarNotFoundException for non-existent items
- [ ] 6.19 Handle CalendarItemReadOnlyException for event-linked item edit/delete attempts
- [ ] 6.20 Write controller unit tests with MockMvc
- [ ] 6.21 Write controller integration tests with HATEOAS link verification

## 8. Application Configuration

- [ ] 8.1 Add calendar module to Spring Boot application class (component scan or @Import)
- [ ] 8.2 Configure Spring Modulith to include calendar module
- [ ] 8.3 Add calendar module to package-info.java documentation
- [ ] 8.4 Verify application starts successfully with calendar module

## 9. Frontend Integration

- [ ] 9.1 Verify frontend CalendarPage can consume /api/calendar-items endpoint
- [ ] 9.2 Test calendar items display in month view
- [ ] 9.3 Test event-linked items display with correct description
- [ ] 9.4 Test multi-day manual items display across date boundaries
- [ ] 9.5 Test manual item creation (if CALENDAR:MANAGE available in test environment)
- [ ] 9.6 Verify HATEOAS links work for navigation to Event details

## 10. Documentation

- [ ] 10.1 Update DOMAIN-MODEL.md with Calendar aggregate description
- [ ] 10.2 Update API.md with calendar endpoints documentation
- [ ] 10.3 Add calendar module to ARCHITECTURE.md bounded contexts section
- [ ] 10.4 Update EVENT-DRIVEN-ARCHITECTURE.md with EventUpdatedEvent and calendar sync diagram

## 11. End-to-End Testing

- [ ] 11.1 Write E2E test for Event → CalendarItem automatic creation flow
- [ ] 11.2 Write E2E test for Event.update → CalendarItem.update flow
- [ ] 11.3 Write E2E test for Event.cancel → CalendarItem.delete flow
- [ ] 11.4 Write E2E test for manual calendar item CRUD operations
- [ ] 11.5 Write E2E test for date range query including multi-day items
- [ ] 11.6 Verify all tests pass with >80% code coverage

## 12. Code Quality

- [ ] 12.1 Run all tests (unit, integration, E2E)
- [ ] 12.2 Verify code coverage >80% (100% for domain logic)
- [ ] 12.3 Run linting and fix any issues
- [ ] 12.4 Run type checking and fix any issues
- [ ] 12.5 Review code for consistency with existing patterns
- [ ] 12.6 Ensure all public methods have JavaDoc comments
