## Context

Four REST call sites ask the sync module whether an entity is paired and then build a link to its synchronisation state:

| Call site | Row (list) | Detail |
|---|---|---|
| `EventController` + `EventSummaryPostprocessor` / `EventDetailsPostprocessor` | batch `findActiveByTargets` → `EnrolledEventIds` | `findByTarget().isPresent()` |
| `DisciplineController` + `DisciplineDetailsPostprocessor` | batch → `EnrolledDisciplineIds` | `findByTarget().isPresent()` |
| `MemberController` + `MemberSummaryPostprocessor` / `MemberDetailsPostprocessor` | batch → `EnrolledMemberIds` | `findByTarget().isPresent()` |
| `RegistrationSyncLinks` | per row `findByTarget` | per row `findByTarget` |

`findActiveByTargets` excludes RETIRED records and `findByTarget` includes them. Records retire when an event finishes or is cancelled (`EventsSyncListener`) and when a discipline is archived (`DisciplineSyncListener`). `DisciplineManagementService.update` refuses edits on "any record". `EventDetailsPostprocessor.offersOrisRegistrationDiscovery` filters `RETIRED` and `ExternalSystem.ORIS` itself.

`SyncPairingLookup` exists separately from `SynchronizationPort` so that adapters (e.g. `OrisEventRegistrationSyncAdapter`) can query pairings without depending on the engine. The engine depends on every adapter through `SynchronizationAdapterRegistry`, so anything behind `SyncPairingLookup` must not reach the registry.

## Goals / Non-Goals

**Goals:**
- One question, `pairingOf(target)`, answers "how is this entity paired". Its three outcomes are named, so a caller cannot mix two meanings by accident.
- One module builds the `sync` link. Callers no longer know `SyncApi`, `SyncEntityTypeParam`, the rel name, `SyncRecord`, `SyncStatus` or `ExternalSystem`.
- Rules that depend on pairing live in the module that owns them (event row status in `events`, discipline lock in `events`), stated in terms of `SyncPairing`.

**Non-Goals:**
- Callers that need the full record (`DisciplineManagementService.restore`, the `retire` listeners, `OrisEventRegistrationDiscoveryJob`, which needs the external id). They keep `findByTarget` / `findActiveByTargets`.
- The `sync` module's own REST resources (`SynchronizationController`, `SyncStatePostprocessor`, `SyncEntityLinks`).
- Batch loading. It is removed, not redesigned.

## Decisions

1. **`SyncPairing` enum with `NONE`, `ACTIVE`, `RETIRED`**, returned by `SyncPairingLookup.pairingOf(SyncTarget)` and placed in `sync.domain`, next to `SyncRecordRepository`.
   The query matches on `entity_type` + `entity_id` across all external systems and returns:
   - `ACTIVE` if any record is not retired;
   - otherwise `RETIRED` if any record exists;
   - otherwise `NONE`.

   It is backed by a new `SyncRecordRepository.pairingOf(SyncTarget)` that reads only `retired_at`, not the aggregate with its schedule and projections.
   - *Alternative:* resolve the sole external system first, as `SynchronizationService.findByTarget` does. Rejected because it needs `SynchronizationAdapterRegistry` and would close the bean cycle described in Context.
   - *Alternative:* a boolean `isPaired` plus `isActivelyPaired`. Rejected because the enum keeps one query and makes "which meaning?" explicit at every call site.

2. **`SyncLinks` is a `@MvcComponent` bean in `sync.infrastructure.restapi`** (already exported as `@NamedInterface("sync.rest")`). Its one method is `addTo(RepresentationModel<?>, SyncTarget)`. It calls `pairingOf`, and when the result is not `NONE` it adds `klabisLinkTo(methodOn(SyncApi.class).getSyncState(SyncEntityTypeResolver.toParam(type), id))` with rel `sync`. Being an `@MvcComponent`, it is part of every `@WebMvcTest` slice.
   - *Alternative:* a static `addTo(model, target, pairing)`. It saves a query on the two detail pages that also evaluate a rule, but every caller would have to call `pairingOf` first. Rejected for the larger interface.

3. **The link is offered for any pairing; rules choose their own outcome.**

   | Use | Condition | Location |
   |---|---|---|
   | `sync` link on details and rows | `!= NONE` (inside `SyncLinks`) | `sync` |
   | event row link | event status DRAFT or ACTIVE, then `SyncLinks.addTo` | `EventSummaryPostprocessor` |
   | discipline edit lock | `!= NONE` | `DisciplineManagementService.update`, `DisciplineDetailsPostprocessor` |
   | ORIS registration discovery | `== ACTIVE` | `EventDetailsPostprocessor` |

   The link survives retirement, so the history of a finished event or an archived discipline stays reachable. The event row hides it by event status, which is the rule the row actually follows. Today that rule holds only as a side effect of retirement.
   - *Alternative:* a "rows: active only" flag on `SyncLinks`. Rejected because it widens the interface and keeps the indirect rule.

4. **No `ExternalSystem` check for discovery.** Events have exactly one adapter (ORIS), so `ACTIVE` means "actively paired with ORIS". `SyncPairing` carries no system.

5. **Batch lookups are removed** (KISS, expected load). Rows issue one small `pairingOf` query each. A page of 20–50 rows means 20–50 indexed single-row reads. If this ever shows up as slow, batching can be added inside `SyncLinks` without changing its interface.

6. **Leftover API is removed when it becomes unused.** `SynchronizationPort.findActiveByTargets` loses its last callers (the three controllers) and is deleted together with its `SynchronizationService` delegate. `SyncPairingLookup.findActiveByTargets` stays for `OrisEventRegistrationDiscoveryJob`.

7. **Naming follows the glossary.** New code says *pairing*, not *enrolled* (`GLOSSARY.md`, *Sync pairing*).

## Risks / Trade-offs

- [Discovery would accept an active pairing with any system if events ever gain a second integration] → hypothetical today. Whoever adds the second adapter must revisit decision 4; record it in the `backend-patterns` sync adapter checklist.
- [More queries per list page] → bounded by page size and club size, and all of them are indexed lookups on `(entity_type, entity_id)`.
- [Archived ORIS discipline rows change shape (no `updateDiscipline`, new `sync` link)] → a regression test pins the missing affordance and must fail on the current code before the fix. The frontend renders the indicator from the link alone, as it already does on the detail.
- [Controller tests that stubbed `findActiveByTargets` / `findByTarget` lose their meaning silently] → retarget each stub to `pairingOf`. A test that only asserted the link should still fail when the stub returns `NONE`.
