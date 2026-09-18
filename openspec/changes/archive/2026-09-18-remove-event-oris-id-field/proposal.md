## Why

`Event.orisId` predates the synchronisation engine and duplicates identity that `sync_record` already owns (`entity_type=EVENT`, `system=ORIS`, `external_id=<orisId>`). Keeping the same pairing in two places invites drift and forces every reader to know which copy is authoritative. Now that all ORIS event flows (`pullAndEnroll`, `syncEventFromOris`, bulk sync) go through the sync engine, `Event.orisId` is a legacy copy that can be removed in favor of the engine's own record.

## What Changes

- Remove `Event.orisId` field, its getter, and the `canSyncFromOris()` invariant from the domain model (`Event.java`).
- Drop `orisId` from `EventCreateEventFromOrisBuilder` / `EventSyncFromOrisBuilder` command builders (after confirming no other caller needs it — distinct from `EventCategory.orisId`, which is a different field used for category matching during sync merge and is unaffected).
- Remove the `oris_id` column from `events.events` (update the domain DDL in the `V001` migration script per project convention — no new migration script).
- Remove `EventMemento.orisId` and its mapping.
- Delete dead code: `EventRepository.existsByOrisId` / `EventJdbcRepository.existsByOrisId` / `EventRepositoryAdapter.existsByOrisId` (unused — `pullAndEnroll` already handles duplicate imports idempotently).
- Replace the "already imported" filter backing `OrisController.listOrisEvents` (`ImportedOrisEventsService.findImportedOrisIds`, currently `SELECT oris_id FROM events.events WHERE oris_id IN (:ids)`) with an equivalent lookup against `sync_record` (`entity_type=EVENT`, `system=ORIS`, `external_id IN (:ids)`), including `RETIRED` records — matching today's behavior where any past import keeps an ORIS id off the candidate list regardless of the event's current status. This requires a new batch-lookup query on `SyncRecordRepository` (and a corresponding `SynchronizationPort` method), since no existing method supports checking membership for a set of external ids at once.
- Replace the affordance gate in `EventController.addManagementAffordances` (`orisIntegrationActive && event.getOrisId() != null`) with the `isEnrolled` flag already computed via `synchronizationPort.findByTarget(...)` for `EventSyncEnrolment` (`EventController.java:143-145`) — threaded into `addManagementAffordances` instead of recomputed.

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/events/spec.md` — "Already imported events are not offered" (already-imported ORIS events stay off the import candidate list) and "Bulk Synchronize ORIS-Imported Upcoming Events" (which events are eligible for bulk sync) are the two scenarios that could plausibly depend on `Event.orisId`. Both are unaffected:
  - The import-candidate filter is re-pointed at `sync_record` with equivalent semantics (any prior import, any status including `RETIRED`, keeps an ORIS id excluded) — same observable result, different internal source.
  - Bulk sync (`OrisBulkSyncService.syncAllUpcoming`) already iterates `synchronizationPort.findActiveByEntityType(EVENT)` and never reads `Event.orisId` — no change needed there.
- `openspec/specs/data-synchronization/spec.md` — describes the sync engine's own behavior (pairing, conflict handling, retry), which is unchanged; this proposal makes `Event` a consumer of that existing source of truth rather than a second copy of it.

**Why no spec update is needed:** This is a pure internal refactor — no API shape, HAL affordance visibility, authorization rule, or UI-observable outcome changes. Only the internal source of truth for "is this event paired with an ORIS event" moves from a column on `Event` to the existing `sync_record` table.

## Impact

- **Backend modules:** `events` (domain, application, infrastructure/jdbc, infrastructure/restapi, infrastructure/orissync), `sync` (new repository/port query).
- **Database:** `events.events.oris_id` column dropped; no new table.
- **Tests:** Unit/integration tests referencing `Event.orisId`, `canSyncFromOris()`, `existsByOrisId`, or `findImportedOrisIds`'s SQL need updating to the new sync-engine-backed behavior.
- **No frontend changes** — API responses and HAL affordances are unchanged in shape and condition.
