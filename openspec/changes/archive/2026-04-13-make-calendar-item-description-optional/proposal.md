## Why

The calendar item creation form currently requires users to fill in a description, but calendar managers often want to add quick reminders where the name alone is self-explanatory (e.g. "Klubová schůze", "Jarní úklid areálu"). Forcing a description adds unnecessary friction and forces users to invent filler text. Making description optional aligns the form with actual usage patterns without losing expressiveness for cases where context matters.

## What Changes

- **Manual calendar item creation**: `description` becomes optional. Users can submit the form with only `name`, `start date`, and `end date`.
- **Manual calendar item update**: `description` becomes optional on edit. Users can clear an existing description by submitting an empty value.
- **Input normalization**: Empty string (`""`, whitespace-only) submitted from the form is normalized to `null` at the API boundary, so the database never stores two representations of "empty".
- **Length limit preserved**: When description is provided, it must not exceed 1000 characters (unchanged).
- **Detail view**: When a calendar item has no description, the detail view omits the description field (or shows nothing) instead of rendering an empty label.
- **Event-linked calendar items are unaffected**: Their description is still auto-generated from event data by the sync logic; this proposal does not change that code path. Any fallout from making event `location` optional is handled by the separate Events proposal.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `calendar-items`: Description is no longer a required field when creating or updating manual calendar items. Validation rules and detail-view rendering scenarios are updated accordingly.

## Impact

**Backend:**
- `calendar/domain/CalendarItem.java` — remove `@NotBlank` from `CreateCalendarItem.description` and `UpdateCalendarItem.description`; remove `validateDescription()` calls from `createManual()` and `update()`; delete the now-unused `validateDescription()` helper. `@Size(max = 1000)` stays.
- `calendar/infrastructure/restapi/CalendarController.java` (or a DTO mapper) — normalize incoming blank description to `null` before passing to the domain command.
- `backend/src/main/resources/db/migration/V001__initial_schema.sql` — change `description TEXT NOT NULL` to `description TEXT NULL` (in-memory H2, reset on restart, no data migration needed per project policy).
- Existing unit/integration tests for create/update calendar item — extend to cover the new "without description" scenarios and the empty-string-to-null normalization.

**Frontend:**
- HAL-Forms automatically pick up the backend's `required: false` metadata, so the form should stop showing description as mandatory with no code changes. The calendar item detail view needs to handle the case where description is `null` / missing.

**Specs:**
- `openspec/specs/calendar-items/spec.md` — update `Requirement: Create Manual Calendar Item` and `Requirement: Update Manual Calendar Item` scenarios to reflect that description is optional; adjust `Requirement: Get Calendar Item Detail` to cover the "no description" case.

**No impact:**
- Automatic Synchronization from Events (event-linked items) — description is generated from event data; code path unchanged by this proposal.
- Other modules, security, authorization.
