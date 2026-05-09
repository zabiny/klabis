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

### Iteration 3 — REST API + HAL Forms for deadlines

**Scope:** New request/response DTOs for deadlines; legacy `registrationDeadline` field dropped from responses. Cancel reason already done in Iter 1.

**Key files changed:**
- `CreateEventRequest.java` (NEW) — request record with `List<LocalDate> deadlines`, `@Size(min=1, max=3)`, `@AssertTrue isDeadlinesOrdered()`, `toRegistrationDeadlines()` helper
- `UpdateEventRequest.java` (NEW) — same structure as CreateEventRequest for update endpoint
- `EventController.java` — `createEvent` and `updateEvent` now accept `CreateEventRequest`/`UpdateEventRequest` instead of domain command records; mapping to domain commands done in controller
- `EventDto.java` — replaced `registrationDeadline: LocalDate` with `deadlines: List<LocalDate>` (READ_ONLY HAL-Forms)
- `EventSummaryDto.java` — same replacement
- `EventDtoMapper.java` — `toDeadlineList()` helper converts `RegistrationDeadlines` VO to chronological `List<LocalDate>` (null when empty)

**New tests (EventControllerTest):**
- Create with 1 deadline → service called with correct deadline1 (verify arg)
- Create with 3 deadlines → service called with all three deadlines mapped
- Create with out-of-order deadlines → 400 Bad Request
- Create with >3 deadlines → 400 Bad Request
- Update with 2 deadlines → service called with correct deadlines
- Update with out-of-order deadlines → 400 Bad Request
- GET detail: single deadline exposed as `deadlines[0]`
- GET detail: all three deadlines exposed as `deadlines[0..2]`
- updateEvent HAL-Forms template: `deadlines` property has `multi: true`, `type: date`, `min: 1`, `max: 3`

**BC note:** `registrationDeadline` field name dropped from both `EventDto` and `EventSummaryDto`; replaced by `deadlines` array. Frontend is the only client and will be updated in Iter 4.

**Test status:** 243/244 passed; 1 pre-existing failure (`EventManagementE2ETest.shouldCreateEventWithoutLocation` — same as Iter 2, `$.location` isEmpty assertion fails on null field with NON_NULL serialization)

### Iteration 4 — Frontend (form, cancel dialog, detail, table, labels)

**Scope:** All frontend changes for N3 and N6.

**Key files changed:**
- `src/api/types.ts` — added `minLength?`/`maxLength?` to `HalFormsProperty` (for string-length constraints from Spring `@Size`)
- `src/localization/labels.ts` — added `fields.deadlines`, `fields.cancellationReason`, `sections.deadlines`, `sections.eventCancelled`
- `src/components/HalNavigator2/halforms/fields/HalFormsTextArea.tsx` — added char counter when `prop.maxLength` is set; applies to cancel dialog `cancellationReason` field
- `src/pages/events/EventDetailPage.tsx` — updated `EventDetail` interface (`deadlines: string[]`, `cancellationReason?: string`); added "UZÁVĚRKY PŘIHLÁŠEK" card and "AKCE BYLA ZRUŠENA" card; removed old `registrationDeadline` display; `deadlines` edit via HAL-Forms template (HalFormsCollectionField auto-renders)
- `src/pages/events/EventsPage.tsx` — updated `EventListData` type; replaced `registrationDeadline` column with `deadlines` column showing primary deadline + badge with tooltip for extras; added cancellation reason as tooltip on status cell

**Notes on 6.1:** No form code changes needed — `halFormsFieldsFactory` already routes `multiple: true` + no options to `HalFormsCollectionField`, and `date` type renders via existing `HalFormsInput`. Sequential validation deferred to backend (backend returns 400 if out-of-order).

**New tests:**
- `HalFormsTextArea.test.tsx` (NEW) — 5 tests: label, no counter without maxLength, counter shown, counter updates on type, counter with initial value
- `EventDetailPage.test.tsx` — 9 new tests: deadlines section (5), cancellation section (4)
- `EventsPage.test.tsx` — 3 new tests: deadlines column header, single deadline, multiple deadlines badge

**Test status:** 1317/1317 passed
