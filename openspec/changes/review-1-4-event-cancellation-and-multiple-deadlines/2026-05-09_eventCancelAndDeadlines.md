# TCF — review-1-4 Event cancellation reason & multiple deadlines

## Proposal summary
- N3: optional `cancellationReason` (max 500 chars) when cancelling event; shown in detail + as tooltip in list
- N6: support up to 3 sequential `RegistrationDeadlines` (replacing single deadline); ORIS imports all three; FE form/detail/list updates

## Iteration plan

Vertical slices, each independently functional & committable:

1. **Domain + persistence + REST for cancellation reason (N3 only)** — backend smaller risk first
2. **Domain `RegistrationDeadlines` VO + Event aggregate update + persistence + ORIS import** — backend N6
3. **REST API + HAL forms for deadlines + cancel** — exposes new fields
4. **Frontend** — cancel dialog + event form (deadlines) + detail + table column + labels
5. **Cleanup** — code review fixes, docs sync, archival prep (archival itself out of scope of implementation)

After each iteration: tests must pass, then commit.

## Status log

(subagents append here)

### Iteration 0 — kickoff (team leader)
- TCF created
- Proposal & spec read; design decisions confirmed (in-place V001 update, RegistrationDeadlines VO, Optional<String> reason, fail-loud ORIS validation)
- No blocking questions for user — design.md resolves all open questions

### Iteration 1 — N3 cancellation reason (domain + persistence + REST)

**Scope:** cancellation reason only; no deadline changes.

**Key files changed:**
- `V001__initial_schema.sql` — added `cancellation_reason VARCHAR(500) NULL` to events table
- `Event.java` — added `CancelEvent` command record (with 500-char domain guard), `cancellationReason` field, overloaded `cancel(CancelEvent)` + zero-arg convenience overload, `getCancellationReason()` getter; updated private constructor and `reconstruct()` signature (added `cancellationReason` param)
- `EventMemento.java` — added `cancellation_reason` column mapping; `from()` and `toEvent()` updated
- `EventManagementPort.java` / `EventManagementService.java` — `cancelEvent` signature updated to `cancelEvent(EventId, Event.CancelEvent)`
- `EventController.java` — `cancelEvent` endpoint accepts optional `CancelEventRequest` body; affordance calls updated
- `EventDto.java` / `EventSummaryDto.java` — added `cancellationReason` field (read-only, NON_NULL)
- `EventDtoMapper.java` — maps `getCancellationReason().orElse(null)` into both DTOs
- All `Event.reconstruct(...)` call sites updated (added `cancellationReason` parameter): `EventTestDataBuilder`, `EventTest`, `EventRegistrationServiceTest`, `CalendarEventSyncIntegrationTest`, `EventsEventListenerTest`
- `EventManagementServiceTest` — `cancelEvent` calls updated to pass `Event.CancelEvent.withoutReason()`

**New tests:**
- `EventTest` — 4 new tests: cancel with reason, cancel without reason, 501-char rejection, 500-char acceptance
- `EventJdbcRepositoryTest` — 2 new persistence tests: reason persisted and reloaded, no-reason case
- `EventControllerTest` — 4 new tests: cancel with reason (service called with correct command), 501-char → 400, detail exposes `cancellationReason`

**Test status:** 249/249 passed (218 events module + 31 calendar module)

### Iteration 2 — N6 RegistrationDeadlines VO + Event aggregate + persistence + ORIS import

**Scope:** Backend only; no REST DTO or HAL forms changes (deferred to Iter 3). `EventDtoMapper` bridges via `deadline1().orElse(null)` for backward compat.

**Key files changed:**
- `RegistrationDeadlines.java` (NEW) — `@ValueObject` record with `Optional<LocalDate>` deadline1/2/3; invariants (d2 requires d1, d3 requires d2, non-decreasing order); factory methods `none()`, `single(d1)`, `of(d1,d2,d3)` (nullable inputs); helpers `last()`, `nextRelevant(today)`, `registrationsOpen(today)`, `isEmpty()`
- `Event.java` — replaced `registrationDeadline: LocalDate` field with `registrationDeadlines: RegistrationDeadlines`; all command records updated (CreateEvent, UpdateEvent, CreateEventFromOris, SyncFromOris); `validateDeadlinesAgainstEventDate()` uses `deadlines.last()` to check ≤ event date; null-safe constructor defaults to `RegistrationDeadlines.none()`; cross-module `EventData` still provides `LocalDate registrationDeadline` via `last().orElse(null)`
- `V001__initial_schema.sql` — added `registration_deadline_2 DATE NULL` and `registration_deadline_3 DATE NULL` columns
- `EventMemento.java` — added deadline_2/3 column fields; `copyBasicEventInfo()` unpacks VO; `toEvent()` reconstructs via `RegistrationDeadlines.of(d1, d2, d3)`
- `OrisEventImportService.java` — reads `entryDate2()` / `entryDate3()` from ORIS `EventDetails`; `buildRegistrationDeadlines()` converts ZonedDateTime→LocalDate and throws `BusinessRuleViolationException` on invalid order
- `EventsDataBootstrap.java` — all `LocalDate` deadline args wrapped with `RegistrationDeadlines.single(...)`; null args stay null (null-safe constructor handles them)
- `EventDataProviderImpl.java` — calendar module compat: `getRegistrationDeadlines().last().orElse(null)`
- `EventDtoMapper.java` — temporary bridge: `deadline1().orElse(null)` (Iter 3 will expose all deadlines)
- All test call sites updated: `EventTestDataBuilder`, `EventTest`, `EventRegistrationServiceTest`, `EventControllerTest`, `CalendarEventSyncIntegrationTest`, `OrisEventImportServiceTest`

**New tests:**
- `RegistrationDeadlinesTest` (NEW) — construction (empty/single/two/three/same-day/factory), invariant violations (d2NoD1, d3NoD2, d2BeforeD1, d3BeforeD2), `last()`, `nextRelevant()`, `registrationsOpen()`
- `EventTest` — 3 new tests: multiple deadlines last in future → open, all passed → closed, deadline > eventDate → rejected
- `EventJdbcRepositoryTest` — 3 new persistence tests: three deadlines round-trip, single deadline round-trip, no deadlines round-trip
- `OrisEventImportServiceTest` — 3 new ORIS mapping tests: single deadline, three deadlines, d1+d3 missing d2 → BusinessRuleViolationException

**Test status:** 2408/2409 passed; 1 pre-existing failure unrelated to Iter 2 (`EventManagementE2ETest.shouldCreateEventWithoutLocation` — `$.location` isEmpty assertion)
