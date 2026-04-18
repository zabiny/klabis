# Team Coordination File — add-registration-deadline-calendar-items

This file is the synchronization point between subagents working on this openspec proposal. Every subagent MUST:

1. Read this file first to understand the current state.
2. Append a concise summary of what they changed and any issues/decisions at the bottom when finished.
3. Mark completed items in `tasks.md` after finishing their iteration.

## Proposal summary

Extend calendar auto-sync so each published event produces not only an `EVENT_DATE` calendar item, but also an `EVENT_REGISTRATION_DATE` item when the event has a `registrationDeadline`. Unify the three handlers in `CalendarEventSyncService` into a single reconcile path (self-healing). Cancel removes all event-linked items.

Key design decisions:
- One concrete class `EventCalendarItem` carries both kinds via a `kind` field (promoted from persistence to domain).
- `CalendarItemKind` moves from `com.klabis.calendar.infrastructure.jdbc` to `com.klabis.calendar` (package root), stays package-private.
- Single `synchronizeFromEvent(EventData event)` method branches on `this.kind`. `SynchronizeFromEvent` record deleted.
- Factories: `createForEventDate(...)` (renamed from `createForEvent`) and `createForRegistrationDeadline(eventName, eventId, deadlineDate)`.
- `EventData` gains `registrationDeadline` (`LocalDate`).
- No REST API, DTO, or frontend changes.

See `proposal.md`, `design.md`, `tasks.md`, `specs/calendar-items/spec.md` for full details.

## Iteration plan

Because the refactor touches the domain, persistence, and application layers simultaneously, the slice boundaries from the design's implementation order are followed. Each iteration leaves the project compiling and all tests green before moving on.

- **Iter 1** — Events module: add `registrationDeadline` to `EventData` + provider; update direct constructors in tests. (tasks section 1)
- **Iter 2** — Persistence enum: move `CalendarItemKind` to `com.klabis.calendar` package root, add `EVENT_REGISTRATION_DATE` value, update imports. (tasks section 2)
- **Iter 3** — Domain: add `kind` field + factories + unified `synchronizeFromEvent(EventData)`; delete `SynchronizeFromEvent` record. Rename `createForEvent` → `createForEventDate`. (tasks section 3)
- **Iter 4** — Persistence round-trip for new kind (memento updates + tests). (tasks section 4)
- **Iter 5** — Application: rewrite `CalendarEventSyncService` into unified reconcile. Listener delegation. (tasks sections 5 + 6)
- **Iter 6** — Integration tests + controller/DTO verification. (tasks sections 7 + 8)
- **Iter 7** — Final verification: full suite + smoke test + `openspec validate --strict`. (tasks section 9)

After iterations: simplify code review, fix high-priority findings, commit.

## Progress log

(Subagents append their notes below — date, iteration, files touched, outcomes, anything the next subagent needs to know.)

---

### 2026-04-18 — Iteration 1

**Files touched:**
- `backend/src/main/java/com/klabis/events/EventData.java` — added `registrationDeadline` (`LocalDate`) as 6th record component; updated Javadoc
- `backend/src/main/java/com/klabis/events/infrastructure/jdbc/EventDataProviderImpl.java` — populated `registrationDeadline` from `event.getRegistrationDeadline()`
- `backend/src/test/java/com/klabis/calendar/application/CalendarEventSyncServiceTest.java` — updated 7 `new EventData(...)` calls to pass `null` as 6th argument
- `backend/src/test/java/com/klabis/calendar/application/CalendarEventSyncIntegrationTest.java` — updated 2 `new EventData(...)` calls to pass `null` as 6th argument

**Test result:** 2189/2189 passed (full backend suite).

**Notes for Iter 2:** No surprises. `Event.getRegistrationDeadline()` already existed. The `@RecordBuilder` annotation on `EventData` means the generated `EventDataBuilder` now also has a `registrationDeadline(LocalDate)` setter — no manual changes needed there. `CalendarItemKind` is currently in `com.klabis.calendar.infrastructure.jdbc`; Iter 2 moves it to `com.klabis.calendar` package root.

---

### 2026-04-18 — Iteration 2

**Files touched:**
- `backend/src/main/java/com/klabis/calendar/CalendarItemKind.java` — created at package root with `MANUAL`, `EVENT_DATE`, `EVENT_REGISTRATION_DATE`; made `public` (see note below)
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarItemKind.java` — deleted (old location)
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarMemento.java` — added `import com.klabis.calendar.CalendarItemKind`; added placeholder `EVENT_REGISTRATION_DATE` case in switch (throws `UnsupportedOperationException` — to be replaced in Iter 4)
- `backend/src/test/java/com/klabis/calendar/infrastructure/jdbc/CalendarRepositoryAdapterTest.java` — added `import com.klabis.calendar.CalendarItemKind`

**Test result:** 2189/2189 passed.

**Visibility note:** The design says "package-private" but Java package-private does not extend to sub-packages. `CalendarMemento` in `com.klabis.calendar.infrastructure.jdbc` cannot see a package-private type from `com.klabis.calendar`. Making it `public` is correct: Spring Modulith's `@ApplicationModule` boundary on `com.klabis.calendar` prevents the enum from leaking to other modules regardless of Java visibility. The tasks.md wording "expand its access if necessary" in 2.1 explicitly anticipated this.

**Notes for Iter 3:** `CalendarItemKind` is now importable by `com.klabis.calendar.domain.EventCalendarItem`. The placeholder `UnsupportedOperationException` case in `CalendarMemento.toCalendarItem()` must be replaced in Iter 4 once `EventCalendarItem.reconstruct` accepts a `kind` parameter.

---

### 2026-04-18 — Iteration 3

**Files touched:**

Production:
- `backend/src/main/java/com/klabis/calendar/domain/EventCalendarItem.java` — added `private final CalendarItemKind kind` field + `getKind()`; renamed `createForEvent` → `createForEventDate` (still takes `CreateCalendarItemForEvent`, sets `kind = EVENT_DATE`); added `createForRegistrationDeadline(String, EventId, LocalDate)` factory; replaced `synchronizeFromEvent(SynchronizeFromEvent)` with `synchronizeFromEvent(EventData)` branching on `this.kind`; extended `reconstruct(...)` to 8-param signature accepting `kind`; deleted `SynchronizeFromEvent` record.
- `backend/src/main/java/com/klabis/calendar/application/CalendarEventSyncService.java` — updated `handleEventPublished` to call `createForEventDate`; updated `handleEventUpdated` to call `synchronizeFromEvent(eventData)` directly (no `SynchronizeFromEvent` wrapper). Service structure otherwise unchanged (Iter 5 rewrites the reconcile logic).
- `backend/src/main/java/com/klabis/calendar/infrastructure/jdbc/CalendarMemento.java` — `from()` now reads `eventDateItem.getKind()` instead of hard-coding `EVENT_DATE`; `toCalendarItem()` `EVENT_DATE` branch passes `CalendarItemKind.EVENT_DATE` to `reconstruct`; `EVENT_REGISTRATION_DATE` branch still throws `UnsupportedOperationException` (stub for Iter 4).

Tests:
- `backend/src/test/java/com/klabis/calendar/domain/EventCalendarItemTest.java` — rewrote completely; added `CreateForRegistrationDeadlineTests` (3.1) and rewrote `SynchronizeFromEventTests` (3.2); updated `createForEvent` → `createForEventDate`; updated `reconstruct` to 8-param; added `ReconstructTests` covering both kinds.
- `backend/src/test/java/com/klabis/calendar/domain/CalendarItemTest.java` — rewrote `SynchronizeFromEventMethod` to use `EventData` constructor instead of `SynchronizeFromEvent` builder; updated all `EventCalendarItem.reconstruct` calls to 8-param signature.
- `backend/src/test/java/com/klabis/calendar/application/CalendarEventSyncServiceTest.java` — updated all `EventCalendarItem.reconstruct` calls to 8-param; deduplicated import.
- `backend/src/test/java/com/klabis/calendar/infrastructure/jdbc/CalendarRepositoryAdapterTest.java` — updated 3 `EventCalendarItem.reconstruct` calls to 8-param.
- `backend/src/test/java/com/klabis/calendar/infrastructure/jdbc/CalendarJdbcRepositoryTest.java` — added `CalendarItemKind` import; updated 2 `EventCalendarItem.reconstruct` calls to 8-param.
- `backend/src/test/java/com/klabis/calendar/CalendarItemTestDataBuilder.java` — added `CalendarItemKind` import; updated `buildEventLinked` to pass `CalendarItemKind.EVENT_DATE`.

**Test result:** 2195/2195 passed (full backend suite).

**Handoff for Iter 4 (memento):**
- `CalendarMemento.toCalendarItem()` `EVENT_REGISTRATION_DATE` case still throws `UnsupportedOperationException` — Iter 4 must replace it with `EventCalendarItem.reconstruct(..., CalendarItemKind.EVENT_REGISTRATION_DATE, ...)`.
- `CalendarMemento.from()` already reads `eventDateItem.getKind()`, so it will correctly persist `EVENT_REGISTRATION_DATE` once items of that kind are created.
- No schema migration needed — `kind` column is `VARCHAR`, `EVENT_REGISTRATION_DATE` is a new allowed string value.

**Handoff for Iter 5 (CalendarEventSyncService):**
- `handleEventPublished` and `handleEventUpdated` still follow the old single-item logic (find first, skip/warn if missing). The full reconcile rewrite happens in Iter 5.
- `handleEventUpdated` now calls `synchronizeFromEvent(eventData)` — this correctly handles both kinds via the domain branch. The service-level wiring is correct; only the reconcile algorithm needs replacing.
