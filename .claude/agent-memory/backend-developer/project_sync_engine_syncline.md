---
name: sync-engine-syncline
description: Klabis common.sync SyncLine/SyncSource contract and the ORIS event sync mapping-at-the-boundary pattern
metadata:
  type: project
---

Generic data-sync engine lives in `com.klabis.common.sync` (do NOT modify: `SyncLine`, `SyncData`,
`SyncSource`, `SyncItemId`, `DataSyncImpl`).

**`SyncLine<L extends SyncData, E extends SyncData>`** = record of
`(SyncSource<L> localSource, SyncSource<E> externalSource, Converter<L,E> converter, Converter<E,L> reverseConverter)`.
- `pull(externalId)`: `externalSource.fetch` -> `reverseConverter.convert(E)` -> `localSource.save(L)`.
- `push(localId)`: `localSource.fetch` -> `converter.convert(L)` -> `externalSource.save(E)`.
- compact ctor asserts `externalSource.isOppositeOf(localSource)` (same `SyncType`, opposite `SyncParty`).
- `SyncLine.withoutMapping(a,b)` = identity converters, only when L and E are the same type.
- `pull`/`push` throw `IllegalStateException` when `fetch` returns empty ("Local/External item with ID ... not found").
- `DataSyncImpl.sync(...)` catches **all** exceptions from `pull`/`push` into a `SyncRecord` with
  `result()==ERROR` and `failureException()` = the caught `Throwable`. It never rethrows.

**Mapping-at-the-boundary pattern (ORIS event sync, `com.klabis.events.infrastructure.sync`):**
- External payload `OrisEventSyncData(int orisId, EventDetails details)` — raw `com.dpolach.api.orisclient`
  DTO is confined here and never crosses the SyncLine.
- `EventSyncDataConverter implements Converter<OrisEventSyncData, EventSyncData>` is the `reverseConverter`
  and owns ALL ORIS->domain translation (delegates to `OrisEventDetailsMapper` MapStruct +
  `OrisEventMappingSupport`). Annotated `@OrisIntegrationComponent` (profile `oris`) — consistent with
  `OrisEventSyncSource` / `EventSyncConfiguration`; needs the `OrisWebUrls` bean.
- Local payload `EventSyncData` is a flat, already-translated record (domain types only:
  `EventId, WebsiteUrl, RegistrationDeadlines, List<EventCategory>, EventRanking, Money, EventTypeId`).
- `LocalEventSyncSource` (`@Component`, LOCAL) only does create-vs-update resolution + command assembly
  (`EventCreateEventFromOrisBuilder` / `EventSyncFromOrisBuilder` from `com.klabis.events.domain`) +
  `event.applyAutoMappedEventType(...)` (null-safe no-op). Deps: `EventRepository` + `CategoryRegistrationGuard`.
- `CategoryRegistrationGuard` (`@Component`, package-private): pure local `log.warn` guard over the
  `Event` aggregate — no ORIS types, no persistence.
- Forward (PUSH) converter is a lambda that throws `UnsupportedOperationException` — ORIS is read-only.
  `OrisEventSyncSource.save` throws `OrisEventSaveNotSupportedException` ("read-only ... event %d").
- The `EventSyncConfiguration` bean type is `SyncLine<EventSyncData, OrisEventSyncData>` (asymmetric).

**`OrisWebUrls` / `OrisApiClient` bean gating:** provided by `OrisApiClientAutoConfiguration` in the
`oris-client` lib, gated only by `@ConditionalOnProperty("oris.client.enabled", matchIfMissing=true)` —
NOT by the Spring `oris` profile. The `oris` profile (`@OrisIntegrationComponent`) only gates Klabis's
own `com.klabis.oris` + sync wiring classes. `application-oris.yml` sets `oris.client.enabled: true`.
