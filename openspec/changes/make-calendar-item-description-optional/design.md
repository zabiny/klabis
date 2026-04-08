## Context

The `calendar-items` capability currently treats description as mandatory on both create and update of manual calendar items. The only real decision to make when relaxing the rule is how to handle the mismatch between how HTML forms represent "empty" (an empty string) and how we want to store it (null). Everything else is a mechanical removal of a validation constraint.

See `proposal.md` for motivation and `specs/calendar-items/spec.md` in this change for the updated requirements.

## Goals / Non-Goals

**Goals:**
- Make description on manual calendar item create and update optional.
- Keep the existing 1000-character upper bound when a description is provided.
- Ensure the database and domain never distinguish between "empty string" and "absent description" — there is one canonical representation for "no description".

**Non-Goals:**
- Any change to event-linked calendar item synchronization (description is auto-generated from event data; handled separately by the Events proposal that relaxes event `location`).
- Changing any other field (name, start date, end date remain required).
- Introducing a custom validator framework. Standard Bean Validation annotations are sufficient.

## Decisions

### Decision 1: Normalize blank description to `null` at the API boundary

**Choice:** When the inbound create/update request contains a description that is `null`, empty, or whitespace-only, the controller (or a DTO mapper layer) converts it to `null` before calling the domain command. The domain stores it as `null`. The response representation also emits `null` (or omits the field) for items without a description.

**Why:** HTML forms submitted from the frontend typically send `""` for cleared text inputs. If we accepted both `""` and `null` into the database, callers would have to write `description != null && !description.isBlank()` everywhere on the read side. One canonical shape ("empty description" ⇔ `null`) keeps queries, tests, and the detail view simple.

**Alternatives considered:**
- **Accept `""` and `null` as-is** — rejected: leaks the frontend's transport quirk into the domain, produces two equivalent rows with different bytes, complicates read-side checks.
- **Reject `""` with a validation error** — rejected: forces the frontend to special-case clearing the field (send `null` instead of `""`), which fights the HAL-Forms framework's default behavior and provides no user value.

### Decision 2: No doménová validační metoda pro description

**Choice:** Remove `validateDescription()` from the `CalendarItem` aggregate. Length is enforced purely via `@Size(max = 1000)` on the command record fields, consistent with how `name` is handled today (mix of `@NotBlank` and `@Size`). No domain method for description validation remains.

**Why:** Once `@NotBlank` is gone, `validateDescription()` would have nothing meaningful to do (length is already validated by the framework at the command boundary). Keeping a one-line wrapper just to match a pattern is noise.

**Alternatives considered:**
- **Keep `validateDescription()` as a no-op placeholder** — rejected: dead code.
- **Keep domain-level length check in addition to `@Size`** — rejected: duplicates enforcement already done at the application boundary, gives two different error messages.

### Decision 3: Update migration script V001 in place

**Choice:** Change the `description TEXT NOT NULL` column definition in `backend/src/main/resources/db/migration/V001__initial_schema.sql` directly, rather than adding a new migration.

**Why:** Klabis runs against in-memory H2 in development and has no production deployment yet (per `backend/CLAUDE.md` migration policy: *"do not add new migration scripts — update best fitting script"*). The schema is rebuilt from V001 on every restart, so there is no data-migration concern.

**Alternatives considered:**
- **Add `V004__calendar_item_description_nullable.sql`** — rejected: violates the project's explicit migration policy and would accumulate historical noise that has no operational meaning while the project has no production database.

## Risks / Trade-offs

- **Risk:** Frontend detail view currently assumes description is always present and may render an empty label or `null` literal when the field is absent. → **Mitigation:** Covered by the specs delta (see `Get Calendar Item Detail` scenarios) and the implementation tasks include a frontend check.
- **Risk:** Existing tests that construct `CreateCalendarItem` / `UpdateCalendarItem` with a non-null description may mask the regression if the new "null description" path is never exercised. → **Mitigation:** Tasks.md explicitly requires new test cases covering the "no description" path on both create and update, plus the normalization behavior.
- **Trade-off:** Accepting `""` from the transport and quietly normalizing it means the API is tolerant rather than strict. In exchange, the frontend does not need to special-case empty fields. For a single-tenant internal tool this is the right balance.
