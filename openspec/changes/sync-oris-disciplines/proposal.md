# Proposal

## Why

Event types map to ORIS disciplines by raw numeric ID only (`events.event_type_oris_disciplines.discipline_id`) — there is no local record of what a discipline actually is. To let a manager pick a discipline by name, `EventTypeManagementService.listDisciplineOptions()` calls the live ORIS discipline API on every request, coupling a routine admin screen to ORIS availability and duplicating ORIS-integration plumbing the club already solved once for events via the generic synchronisation engine (`com.klabis.sync`, ADR-005). Moving disciplines into a locally synced catalog removes that live dependency and gives the sync engine a second, simpler adapter to prove its design against.

## What Changes

- Introduce a local `Discipline` catalog (id, code, name) in the `events` module. Per ADR-005, it carries no `orisId` column — the ORIS correlation is tracked exclusively by the synchronisation engine's own records, the same as `Event`.
- Add `DisciplineSyncAdapter`, a pull-only, local-creating `SynchronizationAdapter` (mirrors `OrisEventSyncAdapter`) that reads ORIS's discipline list and keeps the local catalog's `code`/`name` in step.
- Add a scheduled discovery step that automatically enrols every ORIS discipline not yet known to Klabis. This is new: today the engine only brings in an external record when a user explicitly names one (events); disciplines are reference data with no natural per-record admin action, so the system discovers and brings them in on its own.
- **BREAKING**: `events.event_type_oris_disciplines.discipline_id` changes from a raw ORIS integer to a foreign key referencing the new local `disciplines` table. Since there is no production environment yet (backend `CLAUDE.md`), this is a straight edit to `V001__initial_schema.sql`, not a new migration.
- `EventTypeManagementService.listDisciplineOptions()` stops calling `OrisApiClient.listDisciplines()` and instead reads the local `Discipline` catalog; its `Optional<OrisApiClient>` dependency is removed.
- Add a full CRUD REST API for `Discipline` (list, create, get, update), with HAL links and HAL-FORMS affordances following the same shape as `EventTypeController`.
- Deleting a discipline is a soft delete (archive): it stops being offered for new event-type assignments but is never removed, so every existing `EventType` reference to it keeps working. An archived discipline can be restored.

## Capabilities

### New Capabilities
- `disciplines`: the local, ORIS-sourced catalog of orienteering disciplines that event types can be mapped to, kept in step automatically, with manager CRUD and soft-delete (archive/restore).

### Modified Capabilities
- `data-synchronization`: adds that some kinds of entity are reference data with no meaningful "a user names one to bring in" action — for those, the system discovers and brings in every external record on its own, rather than only in response to a user action.

## Impact

- **Backend domain**: new `Discipline` aggregate/repository in `com.klabis.events.domain`; `EventType`'s `orisDisciplineIds: Set<Integer>` becomes `disciplineIds: Set<DisciplineId>`.
- **Backend sync**: new `SyncEntityType.DISCIPLINE`, new `DisciplineSyncAdapter` and discovery job in `com.klabis.events.infrastructure.orissync`.
- **Database**: `V001__initial_schema.sql` — new `events.disciplines` table; `events.event_type_oris_disciplines.discipline_id` becomes a FK.
- **API**: `EventTypeManagementService.listDisciplineOptions()` (backing the discipline picklist affordance) now reads local data; no request/response shape change for existing consumers. New `/api/disciplines` CRUD resource, spec-first in `docs/openapi/spec/events.yaml`.
- **Removed**: the direct `orisApiClient.get().listDisciplines()` call from `events.application` — ORIS discipline data is only ever read by the sync adapter now.
