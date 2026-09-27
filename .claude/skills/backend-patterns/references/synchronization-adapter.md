# Adding Synchronisation for an Entity

How to plug a new entity type into the `sync` engine (`com.klabis.sync`), which owns change detection, conflict handling, retry and audit generically. Background: ADR-005 in `docs/design-decisions.md`, `openspec/changes/add-bidirectional-sync-engine/design.md`.

The reference implementation is `OrisEventSyncAdapter` (`com.klabis.events.infrastructure.orissync`) — read it alongside this page. Snippets naming `Member` are illustrative only: no member adapter exists.

## Before starting

Read `com.klabis.sync.domain` in full — `SynchronizationAdapter`, `SyncCapabilities`, `SyncProjection`, `SyncEntityType`, `SyncRecord`. `@NamedInterface("sync.domain")` exports the whole package deliberately (ADR-005); depend on these types directly.

## 1. Add the entity type

Add a value to the closed `SyncEntityType` enum with an explicit REST path segment equal to the entity's own resource root, so `/api/{entityType}/{id}/sync…` reads as a sub-resource of `/api/{entityType}/{id}`:

```java
public enum SyncEntityType {
    EVENT("events"),
    MEMBER("members");   // new value
}
```

Add the same wire value to `SyncEntityTypeParam` in `docs/openapi/spec/sync.yaml` and regenerate — never a second hand-written enum.

## 2. Write the projection — only fields the external system owns

A `SyncProjection` is a record serialised by `SyncProjectionCodec`, not a DTO of the aggregate. It holds exactly the fields the *external* system owns:

```java
public record MemberOrisProjection(
        String firstName,
        String lastName,
        String birthNumber,
        String orisClubMembershipId
        // NOT: membership fee tier, internal notes, roles — Klabis owns those
) implements SyncProjection {
    @Override public SyncEntityType entityType() { return SyncEntityType.MEMBER; }
}
```

- **Omission is the mechanism.** The engine hashes and diffs the projection, never the entity — a field absent from the projection can never raise a conflict (design D3).
- **Plain JSON-friendly types only** (`String`, `LocalDate`, `BigDecimal`), as in `OrisEventProjection`. `SyncProjectionCodec` has no wiring for domain value objects; reusing them breaks serialisation silently.
- **`@JsonIgnore` for write-time-only values.** A value computed during `readExternal` and needed by `applyToLocal` travels on the same projection instance as an `@JsonIgnore` component (e.g. `OrisEventProjection.resolvedEventTypeId`) — excluded from hash, persisted column and diffs. Never use it for a Klabis-owned business field, and never carry such values in thread-local state.
- **Canonicalise for hashing.** `BigDecimal` `100` and `100.00` must hash identically — this is not automatic (see step 7).

## 3. Write the adapter

Place it in the module that OWNS the entity, under `infrastructure` (e.g. `events.infrastructure.orissync`) — never in `sync`, and in an integration package only when no Klabis module owns the entity. The adapter then reaches the owning module's repository and aggregate directly; `sync` imports nothing from owning modules, so no cycle arises.

```java
@OrisIntegrationComponent   // or the equivalent module-scoped component annotation
@Application                // not @SecondaryAdapter — see "jMolecules classification"
class MemberOrisSyncAdapter implements SynchronizationAdapter {

    private static final SyncCapabilities CAPABILITIES =
            new SyncCapabilities(
                    true, true,     // readsLocal, readsExternal
                    true, true,     // writesLocal, writesExternal
                    false, false,   // createsLocal, createsExternal
                    true);          // containsSensitiveData — birth number, name

    @Override public SyncEntityType entityType() { return SyncEntityType.MEMBER; }
    @Override public ExternalSystem system() { return ExternalSystem.ORIS; }
    @Override public SyncCapabilities capabilities() { return CAPABILITIES; }
    @Override public Class<? extends SyncProjection> projectionType() { return MemberOrisProjection.class; }

    @Override public SyncProjection readLocal(String entityId) { /* aggregate -> projection */ }
    @Override public SyncProjection readExternal(String externalId) { /* external payload -> projection */ }
    @Override public Optional<ExternalVersionToken> externalVersion(String externalId) { /* see below */ }
    @Override public void applyToLocal(String entityId, SyncProjection projection) { /* aggregate command */ }
    @Override public void applyToExternal(String externalId, SyncProjection projection) { /* idempotent write */ }
}
```

- **Declare `SyncCapabilities` honestly** — the engine decides which directions to attempt from them. A wrong `writesExternal` silently drops a write path or triggers a call the adapter cannot perform.
- **`containsSensitiveData = true` for any personal data.** Projection columns are encrypted regardless (D13), but the flag keeps the access-audit obligation visible. Event data is public, so `OrisEventSyncAdapter` declares `false`.
- **External version token.** Return a cheap per-record version when the external API has one; the engine then skips the full read while it is unchanged and the record is not dirty. Verify availability against the client library JAR, not the design doc. When none exists, return `Optional.empty()` with a comment stating why (see `OrisEventSyncAdapter`) — a bare `empty()` reads as unfinished.

### Trap: jMolecules classification

The adapter is driven by `sync` (secondary role) and calls the owning module's `@PrimaryPort` (primary role). `@PrimaryAdapter` and `@SecondaryAdapter` are mutually exclusive, and a `@SecondaryAdapter` may not reach a primary port — classify it `@Application`. This is the correct classification, not a workaround.

### Trap: the bean-construction cycle

When the owning module's service also needs `SynchronizationPort` (e.g. `syncMemberFromOris` delegating to the engine), wiring forms a loop: service → `SynchronizationPort` → `SynchronizationAdapterRegistry` → adapter → service's port.

Never break it with `@Lazy` — that trades fail-fast startup for a failure on the first sync. Split the port along its two roles (precedent: the `sync-followup-oris-import-cycle` change):

```java
@PrimaryPort
interface MemberOrisFieldsGateway {              // what the adapter calls — no SynchronizationPort dependency
    MemberOrisFields readMemberFields(int orisId);
    Member applyMemberSync(MemberId memberId, MemberOrisFields fields);
}

@PrimaryPort
interface MemberOrisImportPort {                 // orchestration — may depend on SynchronizationPort
    Member importMemberFromOris(int orisId);
    void syncMemberFromOris(MemberId memberId);
}
```

The adapter injects only the gateway, so wiring stays eager and acyclic.

## 4. Enrol and retire

- **Enrol on the import path**, not on a generic "created" event — `OrisEventImportService` calls `SynchronizationPort.enroll(target, externalReference)` when importing. Manually created entities are never enrolled.
- **Retire from a self-listener in the owning module** reacting to its own lifecycle events (`EventsSyncListener` on `EventFinishedEvent`/`EventCancelledEvent`). Look the record up first — unimported entities have none:

```java
@ApplicationModuleListener
void handle(MemberDeactivatedEvent event) {
    synchronizationPort.findByTarget(targetFor(event.memberId()))
            .map(SyncRecord::getId)
            .ifPresent(synchronizationPort::retire);
}
```

## 5. Mark dirty on the entity's own update event

Call `synchronizationPort.markDirty(target)` from a listener on the owning module's update event (`EventUpdatedEvent` for events), so a burst of edits collapses into one pass (D9). If the module publishes no such event yet, add it as part of the integration. `markDirty` is only a scheduling signal — the engine's re-read-before-write rules handle ordering.

## 6. Expose it over REST

The `sync` controller already serves every entity type at `/api/{entityType}/{id}/sync…` — no new endpoint code.

- Add the wire value to `SyncEntityTypeParam` in `sync.yaml` (step 1).
- Add a `sync` link on the entity's own resource when enrolled: the owning module's postprocessor asks `SynchronizationPort` about enrolment (see `EventController`'s postprocessor).
- Keep response mappers with sync-specific dependencies out of the `Converter` bean pool — `SyncStateResponseConverter` is a plain class for this reason (`dto-mapping.md`).

## 7. Test it

1. **Projection hashing** — independently built equal projections hash equally, including `BigDecimal` scale differences. Tests *this* projection's serialisation, not the engine.
2. **Adapter mapping** — agreeing local and external data map to the same projection.
3. **Capabilities honesty** — an operation declared unavailable throws instead of silently doing nothing.
4. **Conflict path** — a local edit to a projected field, run through a full pass, raises a conflict (or resolves per capabilities) instead of being overwritten. This is the guarantee the engine exists for.
5. **Version-token fallback** (if a token exists) — full read skipped when unchanged, performed when changed or absent.

### Trap: `cleanup.sql` must clear the sync tables

Integration tests relying on `src/test/resources/db/cleanup.sql` need `sync.sync_attempt` and then `sync.sync_record` cleared there. Otherwise records leak between test classes and `runFullPass` (which iterates every active record) processes rows the current test never enrolled.
