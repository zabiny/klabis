---
name: oris-sync-migration
description: Multi-phase migration of ORIS event import/resync onto the generic com.klabis.common.sync engine; Phases 1 and 2+3 complete
metadata:
  type: project
---

Plan: `/home/davca/.claude/plans/there-was-created-synchronization-twinkly-jellyfish.md` (5 phases).

**Why:** `com.klabis.common.sync` engine had no real consumer. ORIS event import is the first;
member sync is intended second (`SyncType.MEMBER`, out of scope).

**How to apply:** When continuing this work, read the plan. Phase 1 (committed on
`feature/sync-engine` as `feat(sync): add ORIS event SyncSource adapters and SyncLine wiring`)
delivered, in `com.klabis.events.infrastructure.sync`:
- `EventSyncData` (the single `SyncData` payload for both `SyncLine` sides — NOT `Event.SyncFromOris`,
  which lacks orisId/EventDetails/getSyncId and is a domain type)
- `OrisEventMappingSupport` — business mapping helpers were MOVED here (variant b: `OrisEventImportService`
  injects it and delegates; no duplicated copies). Public constructor so unit tests can `new` it with a
  mock `EventTypeRepository`.
- `LocalEventSyncSource`, `OrisEventSyncSource` (@Profile oris, read-only save throws
  `OrisEventSaveNotSupportedException`), `IdentityEventConverter`, `EventSyncConfiguration` (SyncLine bean, @Profile oris)
- `OrisEventDetailsMapper` — MapStruct, only trivial fields → `TrivialEventFields` record
- `EventRepository.findByOrisId(int)` added (derived query on `EventJdbcRepository`).

Phases 2+3 (DONE, committed on `feature/sync-engine` as `refactor(sync): route single-event ORIS
import and resync through DataSync`, hash `13d248e6`): `OrisEventImportService` is now a thin
delegate — deps reduced to `DataSync` + `EventRepository`.
- `importEventFromOris(orisId)` → `dataSync.sync(SyncId.externalId(EVENT, ""+orisId), PULL)`, then
  `eventRepository.findById(eventIdOf(record.localId()))`.
- `syncEventFromOris(eventId)` → load event, guard `getOrisId()==null` (throws anon
  `BusinessRuleViolationException`), then `dataSync.sync(SyncId.localId(EVENT, uuid), PULL)`.
- `translate(int orisId, String failureCause)` private shim maps `failureCause` substrings
  (case-insensitive) back to typed exceptions so REST statuses hold:
  `constraint|duplicate|unique` → `DuplicateOrisImportException` (409);
  `not found|no sync record|no sync line` → `EventNotFoundException` (404);
  `registration deadline|invalid` → anon `BusinessRuleViolationException` (422);
  else → anon `BusinessRuleViolationException("ORIS sync failed: ...")`.
  FRAGILE (couples to `DataSyncImpl` / `SyncLine` / `OrisEventMappingSupport` message text) —
  follow-up in plan is a result-body contract like the bulk endpoints.
- `OrisEventImportServiceTest` rewritten against a mock `DataSync`. `OrisEventTypeAutoMappingTest`
  deleted; "does not overwrite existing eventTypeId on update" moved to `LocalEventSyncSourceTest`.

Phase 4 (NOT done): rewire bulk services (`OrisBulkSyncService`, `OrisEventBulkImportService`) to
`dataSync.sync(...)` per item, translating `SyncRecord.result` into their result DTOs.
Phase 5 (NOT done): final dead-code sweep + full `events` + `common.sync` + Modulith test run.

**Gotcha hit:** building a Mockito mock inside a `when(...).thenReturn(...)` argument (e.g.
`when(details.classes()).thenReturn(Map.of("x", mockClass(...)))` where `mockClass` itself stubs)
throws `UnfinishedStubbingException`. Build inner mocks into locals first.
