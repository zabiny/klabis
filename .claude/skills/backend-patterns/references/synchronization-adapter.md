# Adding Synchronisation for an Entity

How to plug a new entity type into the `sync` engine (`com.klabis.sync`), which owns change detection, conflict handling, retry and audit generically (`docs/design-decisions.md` ADR-005, `openspec/changes/add-bidirectional-sync-engine/design.md`).

The only adapter that exists today is `OrisEventSyncAdapter` in `com.klabis.oris.eventsync` — read it alongside this page. This page also walks through **`Member`** as a worked example throughout, to make the steps concrete for an entity with sensitive data and a two-way external system. **No member adapter exists** — member synchronisation is an explicit non-goal of the proposal that built this engine. Treat every `Member`/ORIS-person reference below as illustrative, not as code you will find in the repo.

## Before you start

Read `com.klabis.sync.domain` in full — `SynchronizationAdapter`, `SyncCapabilities`, `SyncProjection`, `SyncEntityType`, `SyncRecord`. It is small and every later step assumes you already know its shapes. `@NamedInterface("sync.domain")` exports the whole package deliberately (ADR-005) — you are meant to depend on these types directly.

## 1. Add the entity type

`SyncEntityType` is a closed enum; each value declares its own REST path segment explicitly, not derived from the name:

```java
public enum SyncEntityType {
    EVENT("events"),
    MEMBER("members");   // hypothetical addition
    // ...
}
```

The path segment must match the entity's own resource root, because `/api/{entityType}/{id}/sync…` is meant to read as a sub-resource of `/api/{entityType}/{id}`.

Also add the corresponding value to the generated `SyncEntityTypeParam` — it comes from `docs/openapi/spec/sync.yaml` (step 6), so this is really one change made in the spec and regenerated, not a second hand-written enum.

## 2. Write the projection: only fields the external system owns

A `SyncProjection` is a plain data carrier — a record implementing `SyncProjection`, serialised directly by `SyncProjectionCodec`. It is **not** a DTO of the aggregate. It contains exactly the fields the *external* system owns, in one shape shared by both sides:

```java
public record MemberOrisProjection(
        String firstName,
        String lastName,
        String birthNumber,
        String orisClubMembershipId
        // NOT: membership fee tier, internal notes, roles — Klabis owns those
) implements SyncProjection {

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.MEMBER;
    }
}
```

Follow `OrisEventProjection`'s actual shape: plain JSON-friendly types (`String`, `LocalDate`, `BigDecimal`), not the aggregate's own value objects — `SyncProjectionCodec` has no bespoke Jackson wiring for `Optional`-backed value objects, and reusing the domain value object type here would silently break serialisation.

**Why omission is the mechanism, not a filter.** The engine hashes and diffs the projection, never the entity. A field that is absent from the projection cannot produce a hash difference, so editing it is invisible to synchronisation by construction (`design.md` D3) — this is what keeps a membership-fee-tier edit or a manually added category from ever raising a conflict, with no special-casing anywhere in the engine.

**The `@JsonIgnore` escape hatch.** Sometimes you need a value at write time that must never be compared. The real example is `OrisEventProjection.resolvedEventTypeId`: the event type is Klabis-owned and must stay out of hashing, but `OrisEventSyncAdapter.readExternal` still needs to carry the ORIS discipline resolution forward to `applyToLocal` *on the same projection instance* — carrying it as thread-local state keyed on call order was tried first and rejected as fragile (task 8.10's follow-up). The field is a normal record component, annotated `@JsonIgnore`:

```java
public record OrisEventProjection(
        String name,
        // ...
        @JsonIgnore EventTypeId resolvedEventTypeId
) implements SyncProjection { /* ... */ }
```

`SyncProjectionCodec` never serialises it, so it never reaches the hash, the persisted column, or `divergedFields()`/`changedSides()`. Use this whenever you need to smuggle a write-time-only value through the projection rather than through fragile out-of-band state — but keep it to values genuinely computed during the read, never a Klabis-owned business field that happens to be convenient to carry.

**Hash canonicalisation.** Write a projection test that two projections built from equal data hash equally — in particular, monetary amounts of differing `BigDecimal` scale (`100` and `100.00`) must hash identically. This is not automatic; see step 7.

## 3. Write the adapter

Implement `SynchronizationAdapter` in the integration's own module (not in `sync`, and not in the owning module's own package — ORIS's adapter lives in `com.klabis.oris.eventsync`, a sibling of `com.klabis.oris`, precisely so it does not read as belonging inside the `sync` module itself).

```java
@OrisIntegrationComponent   // or the equivalent module-scoped component annotation
@Application                // see the classification trap below — not @SecondaryAdapter
class MemberOrisSyncAdapter implements SynchronizationAdapter {

    private static final SyncCapabilities CAPABILITIES =
            new SyncCapabilities(
                    true, true,     // readsLocal, readsExternal
                    true, true,     // writesLocal, writesExternal — ORIS persons ARE two-way
                    false, false,   // createsLocal, createsExternal
                    true);          // containsSensitiveData — birth number, name

    private final MemberManagementPort memberManagementPort;
    private final OrisPersonPort orisPersonPort;

    // ...

    @Override public SyncEntityType entityType() { return SyncEntityType.MEMBER; }
    @Override public ExternalSystem system() { return ExternalSystem.ORIS; }
    @Override public SyncCapabilities capabilities() { return CAPABILITIES; }
    @Override public Class<? extends SyncProjection> projectionType() { return MemberOrisProjection.class; }

    @Override
    public SyncProjection readLocal(String entityId) { /* map Member -> MemberOrisProjection */ }

    @Override
    public SyncProjection readExternal(String externalId) { /* map ORIS person -> MemberOrisProjection */ }

    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty(); // see the version-token trap below
    }

    @Override
    public void applyToLocal(String entityId, SyncProjection projection) { /* Member.syncFromOris(...) */ }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) { /* ORIS write, idempotent */ }
}
```

Declare `SyncCapabilities` honestly — every boolean is a fact the engine trusts to decide which directions are even attempted. Getting `writesExternal` wrong either silently drops a write path that should exist, or makes the engine attempt a call the adapter cannot actually perform.

**`containsSensitiveData` matters — this is not a formality for a member adapter.** A birth number and a name are exactly the personal data D13 built projection encryption-at-rest for. Event data is public, so `OrisEventSyncAdapter` correctly declares `false`; a member (or any personal-data) adapter must declare `true`. The projection columns are already encrypted regardless of this flag (D13 encrypts unconditionally), but the flag is what keeps the access-audit obligation visible for the next person who reads this adapter (`design.md` Risks table — deferred until the first sensitive adapter ships, which yours would be).

### The external version token, and the full-read fallback

`externalVersion` lets the engine skip a full external read when nothing changed. `OrisEventSyncAdapter` returns `Optional.empty()` unconditionally — verified against the real `oris-client` JAR (not assumed from the design doc, which originally described a `getEventListVersions` method that turns out not to exist): `getEventList` carries no version field, and `EventDetails.version()` is only reachable after the very full read the token exists to avoid. Document *why* a token is absent, the same way, if your adapter also has none — a bare `Optional.empty()` with no comment reads as an unfinished implementation to the next person, not a considered fact about the external API.

If your external system does offer a cheap per-record version (ORIS persons might, unlike ORIS events — verify against the client library, don't assume), return it and the engine will short-circuit the full read whenever it is unchanged and the record is not dirty.

### Trap: jMolecules classification

An adapter that both implements `sync`'s `SynchronizationAdapter` (a driven/secondary role, called by the engine) **and** calls another module's `@PrimaryPort` (a driving/primary role, e.g. `MemberManagementPort`) holds two hexagonal roles on one class. jMolecules cannot express both — `@PrimaryAdapter` and `@SecondaryAdapter` are mutually exclusive in the library's own layer predicates, and a `@SecondaryAdapter` is never permitted to reach a primary port. Classify the adapter `@Application` instead. This is not a workaround for a modelling mistake — it is the correct classification for a class that is genuinely both an adapter into `sync` and a driving caller into the owning module.

### Trap: the bean-construction cycle

If the owning module's own service needs `SynchronizationPort` — for example, a `MemberOrisImportService.syncMemberFromOris` delegating to the engine, the same move `OrisEventImportService` made — you get a cycle: `MemberOrisImportService` → `SynchronizationPort` → `SynchronizationAdapterRegistry` → `MemberOrisSyncAdapter` → the owning module's import port, back to `MemberOrisImportService`. Break it with `@Lazy` on the adapter's dependency on that import port, exactly as `OrisEventSyncAdapter.orisEventImportPort` does:

```java
MemberOrisSyncAdapter(MemberManagementPort memberManagementPort, @Lazy MemberOrisImportPort memberOrisImportPort) {
    // memberManagementPort does not participate in the cycle — stays eager
}
```

The cost is explicit: a broken import-port bean now surfaces on the first synchronisation attempt rather than at application startup. Accept that trade — it is the price of the module boundary in ADR-001, not a workaround to design away.

## 4. Enrol and retire

**Enrolment belongs on the import path, not on a listener reacting to a generic "created" event.** An entity becomes synchronisable when it is actually imported from the external system — `OrisEventImportService`'s import flow calls `SynchronizationPort.enroll(target, externalReference)` at that point, not on `EventCreatedEvent` in general (a manually created event, never imported, is never enrolled and stays outside synchronisation entirely). A hypothetical `MemberOrisImportService` would do the same: enrol when a Klabis member is matched or created from an ORIS import, not on every `MemberRegisteredEvent`.

**Retirement happens on a self-listener inside the owning module**, reacting to the module's own lifecycle events — `EventsSyncListener` (`events.infrastructure.listeners`) listens to `EventFinishedEvent`/`EventCancelledEvent` and calls `SynchronizationPort.retire`. Not every entity carries an enrolled record (unimported ones don't), so the listener does a `findByTarget` lookup rather than assuming one exists:

```java
@OrisIntegrationComponent
@PrimaryAdapter
class MembersSyncListener {
    private final SynchronizationPort synchronizationPort;
    // constructor...

    @ApplicationModuleListener
    void handle(MemberDeactivatedEvent event) {
        synchronizationPort.findByTarget(targetFor(event.memberId()))
                .map(SyncRecord::getId)
                .ifPresent(synchronizationPort::retire);
    }
}
```

A `@PrimaryAdapter` event listener calling a foreign module's primary port is an established pattern in this codebase (`EventsSyncListener`'s javadoc cites `calendar.infrastructure.listeners.EventsEventListener` as the precedent) — it is not special to `sync`.

## 5. Mark dirty on the entity's own update event

The engine needs to know when the local side changed, so a burst of edits collapses into one pass instead of running once per edit (`design.md` D9). This requires the owning module to already publish a domain event on ordinary mutation — `events` has `EventUpdatedEvent`; a `Member` adapter would need an equivalent (`MemberUpdatedEvent` or similar) if one does not already exist. If the module publishes no such event today, adding it is part of the change that adds the integration — it is not something the `sync` module can substitute for.

```java
@ApplicationModuleListener
void handle(MemberUpdatedEvent event) {
    synchronizationPort.markDirty(targetFor(event.memberId()));
}
```

`markDirty` is purely a scheduling signal, never consulted to decide whether a write is safe (D9) — you do not need to reason about ordering races here; the engine's re-read-before-write rules handle that internally.

## 6. Expose it over REST

The `sync` module's one controller already serves every entity type uniformly at `/api/{entityType}/{id}/sync…` — you add no new endpoint code. Two things to do:

- **Add the entity type's wire value to `docs/openapi/spec/sync.yaml`** (the `SyncEntityTypeParam` enum) — this is what step 1's `SyncEntityType` addition is regenerated from.
- **Add a `sync` link on the entity's own resource**, the way `GET /api/events/{id}` gains one when an event is enrolled: the owning module's controller/postprocessor calls `SynchronizationPort` (a primary port, per ADR-001) to check enrolment and, if enrolled, adds a link to the sync sub-resource. See `EventController`'s postprocessor for the exact wiring.

### Trap: never register a mapping `Converter` bean with module-specific dependencies

`SyncStateResponseConverter` (mapping `SyncRecord` → the response DTO) is **deliberately not** a Spring `Converter<S,T>` bean, even though that is the usual mapping pattern elsewhere in this codebase (`rest-adapter.md`). Its dependencies (`SyncProjectionFieldReader`, `SynchronizationPort`) are sync-module-specific — but any class implementing `org.springframework.core.convert.converter.Converter` and annotated as a Spring bean is picked up by Boot's MVC auto-configuration into the **global** `mvcConversionService`, which every `@WebMvcTest` slice in the whole application constructs. A converter with module-specific constructor dependencies then breaks every unrelated slice test that doesn't have those beans available.

`MemberIdToUuidConverter` and `RegisterNewMemberConverter` get away with being `Converter` beans because they take **no constructor dependencies at all**. `StringToSyncEntityTypeParamConverter` (the path-variable binder from step 1/6) is safe for the same reason — deliberately stateless. The moment a mapper needs dependencies, either keep it a plain class constructed directly by its controller (as `SyncStateResponseConverter` is), or verify it truly has zero constructor dependencies before making it a `Converter` bean.

## 7. Test it

Cover, in roughly this order:

1. **Projection hashing** — equal projections (built independently from local-shaped and external-shaped source data) hash equally; in particular a monetary field of differing `BigDecimal` scale (`100` vs `100.00`) must not produce a hash difference. This is the canonicalisation D3 calls out by name as the trap to name explicitly — write the test even when it feels redundant with the generic engine tests, because it is testing *your* projection's serialisation, not the engine.
2. **Adapter mapping** — a real local entity and a real (or stubbed) external payload both map to the same projection shape when their data agrees.
3. **Capabilities-honesty** — an operation the adapter declares unavailable (e.g. `applyToExternal` on a pull-only adapter) throws rather than silently doing nothing, so a capabilities bug fails loudly instead of masquerading as a no-op success.
4. **The conflict path** — a local edit to a field the projection covers, run through a full pass, must raise a conflict (or, if the adapter is two-way, resolve via whichever direction the capabilities allow) rather than being silently overwritten. This is the behavioural guarantee the whole engine exists for; skipping this test leaves the one property that matters unverified.
5. **Version-token fallback**, if you have one — engine skips the full read when the token is unchanged; falls back correctly when it changed or is absent.

### Trap: `cleanup.sql` must clear the sync tables

If your integration test setup relies on `src/test/resources/db/cleanup.sql` to reset state between test classes, verify `sync.sync_attempt` and `sync.sync_record` are cleared there (attempt history first, it references the record). If they are missing, records from one test class' entities survive into the next, and `runFullPass` — which by design iterates every active record — starts finding rows that were never enrolled by the test currently running. This bit slice 8 of the sync engine's own implementation; it will bite any integration whose tests enrol records without noticing the same gap.
