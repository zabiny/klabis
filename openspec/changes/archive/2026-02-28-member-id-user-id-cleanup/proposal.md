# Domain Identity Types Cleanup (MemberId, UserId, EventId)

## Why

The codebase currently mixes raw `UUID` types with domain-specific value objects (`UserId`, `MemberId`, `EventId`), creating ambiguity about which ID type is being used. This reduces type safety, makes the code harder to understand, and increases the risk of passing wrong IDs to methods.

Existing issues:
- `Member.getId()` returns `UserId` instead of `MemberId` (conceptual mismatch)
- Service methods use `UUID memberId` / `UUID eventId` parameters instead of type-safe value objects
- Domain events use `UUID` instead of typed IDs
- Event-related services consistently use `UUID eventId` despite `EventId` value object existing

This refactoring establishes clear type boundaries:
- **Member aggregates use `MemberId`**
- **User aggregates use `UserId`**
- **Event aggregates use `EventId`**

This makes the code self-documenting and prevents accidental passing of wrong ID types between aggregates.

## What Changes

- **BREAKING**: `Member.getId()` returns `MemberId` instead of `UserId`
- **BREAKING**: `MemberId` becomes the primary identifier for Member aggregates
- **BREAKING**: Event-related services use `EventId` instead of `UUID eventId` parameters
- **BREAKING**: Domain events (`UserCreatedEvent`, `MemberCreatedEvent`, `MemberTerminatedEvent`) use typed IDs instead of raw `UUID`
- Service layer methods use typed IDs (`MemberId`, `UserId`, `EventId`) instead of `UUID` parameters
- Exception classes (`DuplicateRegistrationException`, `RegistrationNotFoundException`, `EventNotFoundException`) use typed IDs
- `MemberDto`, `EventDto` and response DTOs continue using `UUID` (external API contract unchanged)
- Memento classes continue using `UUID` (persistence layer unchanged)

## Capabilities

### Modified Capabilities

- `members`: Introduces type-safe member identification to prevent confusion with user/event identifiers; maintains 1:1 member-user identifier relationship with explicit conversion only
- `users`: Adds type-safe user identification to prevent confusion with member/event identifiers; updates `UserCreatedEvent` to use type-safe user identifier
- `events`: Introduces type-safe event identification across all event operations; prevents accidental use of member/user identifiers for event operations

## Impact

**Affected code areas:**
- `Member` aggregate and all related domain classes
- `MemberId` value object (removal of `toUserId()` convenience method)
- Service interfaces: `ManagementService`, `RegistrationService`, `EventRegistrationService`, `EventManagementService`
- Controller methods accepting `UUID eventId` / `UUID memberId` as path variables
- Domain events: `UserCreatedEvent`, `MemberCreatedEvent`, `MemberTerminatedEvent`
- Exception classes: `DuplicateRegistrationException`, `RegistrationNotFoundException`, `EventNotFoundException`
- Calendar module: `CalendarMemento`, `CalendarJdbcRepository`, related DTOs
- All test files referencing Member/User/Event IDs

**No impact on:**
- External API contracts (DTOs keep `UUID` in responses/requests)
- Database schema (mementos keep `UUID` for persistence)
- Frontend (no changes to HAL+FORMS responses)

**Migration risk:** Medium - requires careful updates across members, users, events, and calendar modules, but persistence and API layers remain stable.
