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
