## Context

The club currently has no system support for event management. Events are organized manually, registrations are tracked
in spreadsheets or external tools. The members module exists and provides member data including chip numbers (SI card).
The users module handles authentication and authorization with a permission-based system.

This change introduces a new `events` Spring Modulith module that integrates with existing modules while maintaining
proper bounded context separation.

## Goals / Non-Goals

**Goals:**

- Provide CRUD operations for club events with lifecycle management
- Enable member self-registration for events
- Support automatic event completion after event date passes
- Follow existing architectural patterns (Spring Modulith, HAL+FORMS, Clean Architecture)
- Maintain proper module boundaries and dependencies

**Non-Goals:**

- Event capacity limits (future enhancement)
- Registration deadlines separate from event date (future enhancement)
- Admin registration on behalf of members (future enhancement)
- Multi-day events with date ranges (future enhancement)
- Event categories or tags (future enhancement)
- Payment integration for events (future enhancement)

## Decisions

### Decision 1: Events as Separate Spring Modulith Module

**Choice:** Create `com.klabis.events` as a new Spring Modulith module.

**Rationale:** Events are a distinct bounded context with their own lifecycle and business rules. Keeping them separate
from members maintains clean module boundaries and allows independent evolution.

**Alternatives considered:**

- Adding events to members module: Rejected because events have different lifecycle, different access patterns, and
  would bloat the members module.

### Decision 2: Event Aggregate Contains Registrations

**Choice:** EventRegistration is part of the Event aggregate, not a separate aggregate.

**Rationale:**

- Registrations don't exist without events
- Business rules for registration (status must be ACTIVE) are enforced by Event aggregate
- Registration count and list are natural properties of an event
- Simplifies consistency guarantees

**Alternatives considered:**

- Separate EventRegistration aggregate: Would require eventual consistency between Event and Registration states, adding
  complexity without clear benefit.

### Decision 3: UserId Reference for Members

**Choice:** Store `UserId` (not MemberId) to reference members in Event and EventRegistration.

**Rationale:**

- Consistent with existing pattern where Member and User share UserId
- Avoids introducing new cross-module reference types
- UserId is already exposed as a public interface in users module

**Alternatives considered:**

- Create MemberId type: Would duplicate UserId semantically and add confusion.

### Decision 4: Value Objects for Type Safety

**Choice:** Create value objects: EventId, SiCardNumber, WebsiteUrl

**Rationale:**

- Type safety prevents mixing up IDs and values
- Validation encapsulated in value object constructors
- Consistent with existing patterns (UserId, EmailAddress, PhoneNumber)

**Value object specifications:**

- `EventId`: Wraps UUID, immutable, not null
- `SiCardNumber`: 4-8 digits only, not blank
- `WebsiteUrl`: Valid http/https URL, optional (nullable)

### Decision 5: Scheduled Job for Auto-Completion

**Choice:** Use Spring `@Scheduled` job to transition ACTIVE events to FINISHED after event date.

**Rationale:**

- Simple implementation using existing Spring infrastructure
- Daily execution (e.g., 2:00 AM) is sufficient granularity
- Idempotent operation - safe to run multiple times

**Alternatives considered:**

- Lazy evaluation on GET: Would cause side effects in read operations, violates CQS.
- Event-driven with delayed messages: More complex, overkill for this use case.

### Decision 6: Authentication Principal and Username Resolution

**Choice:** Use User UUID (not username) as authentication principal in SecurityContext for event registration.

**Rationale:**

- UUID allows direct mapping to UserId and MemberId without additional lookups
- Consistent with existing pattern where Member.id = UserId
- Username field (`User.userName`) is kept only for display/UI purposes, not for authentication
- Simplifies member resolution: authentication principal → UUID → MemberId lookup

**Implementation details:**

- `SecurityContextHolder.getContext().getAuthentication().getName()` returns User UUID string
- `UserId.fromString(username)` in EventRegistrationService parses UUID from authentication
- Members module resolves Member by MemberId (which equals UserId)
- Users without associated member records get 400 Bad Request when attempting to register

**Constraints:**

- Not every user has a member record
- Event registration requires valid member (existing Member entity)
- System must return 400 if authenticated user has no member profile

**Alternatives considered:**

- Use username as principal: Would require Member lookup by username field, adds database query complexity
- Use hybrid approach (username for members, UUID for non-members): Confusing and inconsistent

### Decision 7: Authentication Principal and Username Resolution

**Choice:** Use User UUID (not username) as authentication principal in SecurityContext for event registration.

**Rationale:**

- UUID allows direct mapping to UserId and MemberId without additional lookups
- Consistent with existing pattern where Member.id = UserId
- Username field (`User.userName`) is kept only for display/UI purposes, not for authentication
- Simplifies member resolution: authentication principal → UUID → MemberId lookup

**Implementation details:**

- `SecurityContextHolder.getContext().getAuthentication().getName()` returns User UUID string
- `UserId.fromString(username)` in EventRegistrationService parses UUID from authentication
- Members module resolves Member by MemberId (which equals UserId)
- Users without associated member records get 400 Bad Request when attempting to register

**Constraints:**

- Not every user has a member record
- Event registration requires valid member (existing Member entity)
- System must return 400 if authenticated user has no member profile

**Alternatives considered:**

- Use username as principal: Would require Member lookup by username field, adds database query complexity
- Use hybrid approach (username for members, UUID for non-members): Confusing and inconsistent

### Decision 8: HAL+FORMS API with State-Dependent Links

**Choice:** Include HATEOAS links based on event status and user permissions.

**Rationale:**

- Consistent with existing members API design
- Enables frontend to discover available actions
- Links change based on state (e.g., no "edit" link for FINISHED events)

**Link patterns:**

- DRAFT: self, edit, publish, cancel, registrations
- ACTIVE: self, edit, cancel, finish, registrations, register (for members)
- FINISHED/CANCELLED: self, registrations (read-only)

### Decision 9: Event Module Package Structure

**Choice:** Follow existing hybrid package structure pattern.

**Choice:** Follow existing hybrid package structure pattern.

**Note:** Domain value objects are placed at module root (not in `model/` subpackage) following KISS principle for
improved discoverability (updated 2026-01-31).

```
com.klabis.events/
├── Event.java                    (aggregate root)
├── Events.java                   (public query API - read-only)
├── EventId.java                  (value object)
├── EventStatus.java              (enum)
├── EventRegistration.java        (entity within aggregate)
├── SiCardNumber.java             (value object)
├── WebsiteUrl.java               (value object)
├── management/                   (feature package)
│   ├── CreateEventCommand.java
│   ├── UpdateEventCommand.java
│   ├── EventManagementService.java
│   ├── EventController.java
│   └── EventDto.java
├── registration/                 (feature package)
│   ├── RegisterForEventCommand.java
│   ├── EventRegistrationService.java
│   ├── EventRegistrationController.java
│   └── RegistrationDto.java
├── completion/                   (feature package)
│   └── EventCompletionScheduler.java
└── persistence/                  (infrastructure - internal)
    ├── EventRepository.java      (repository interface - internal)
    └── jdbc/
        ├── EventJdbcRepository.java
        ├── EventRepositoryAdapter.java
        ├── EventMemento.java
        └── EventRegistrationMemento.java
```

### Decision 8: Registration Privacy

**Choice:** List registrations show only firstName and lastName. SI card number visible only to the registered member.

**Rationale:**

- Privacy: SI card numbers shouldn't be publicly visible
- Sufficient information for coordinators to see attendance
- Member can see their own full registration via /registrations/me endpoint

## Risks / Trade-offs

**[Risk] Module dependency on Users module**
→ Mitigation: Depend only on UserId value object (shared kernel). Avoid runtime coupling to user services.

**[Risk] Orphaned registrations if member is deleted**
→ Mitigation: Members cannot be deleted, only deactivated. Registrations remain for historical record.

**[Risk] Event coordinator reference to non-existent member**
→ Mitigation: Validate eventCoordinator exists at creation/update time. If member later deactivated, coordinator
reference remains valid for display purposes.

**[Risk] Scheduled job failure leaves events in ACTIVE state**
→ Mitigation: Job is idempotent and runs daily. If missed, next run will catch up. Add monitoring/alerting for job
failures.

**[Risk] Race condition on duplicate registration check**
→ Mitigation: Database unique constraint on (event_id, member_id). Application-level check provides better error
message; DB constraint is safety net.

**[Trade-off] Registrations within Event aggregate limits scalability**
→ For expected club size (hundreds of events, dozens of registrations per event), this is acceptable. If events need
thousands of registrations, consider extracting to separate aggregate.
