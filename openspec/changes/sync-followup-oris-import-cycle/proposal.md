## Why

`OrisEventSyncAdapter` injects `OrisEventImportPort` with `@Lazy` to break a
bean-construction cycle:

```
OrisEventImportService → SynchronizationPort → SynchronizationAdapterRegistry
                       → OrisEventSyncAdapter → OrisEventImportPort
                       → OrisEventImportService
```

`@Lazy` makes the cycle constructible, but it does not remove it — it trades fail-fast
startup for a failure that surfaces at the first call instead. The javadoc on the adapter
describes the cycle accurately; nothing here is hidden. But the cycle is a symptom: it
appeared only when task 8.3 made `OrisEventImportService` delegate `syncEventFromOris` to
the engine, and `design.md` D2 assumed a one-way dependency from `oris.eventsync` into
`events.application`.

The underlying cause is that one bean holds two roles. `OrisEventImportPort` exposes four
methods that split cleanly along that line:

- `readOrisFields(int)` and `applyOrisSync(EventId, OrisEventFields)` — primitives the
  sync adapter calls; they need no knowledge of the engine.
- `importEventFromOris(int)` and `syncEventFromOris(EventId)` — orchestration of the
  import/sync flow; these are what require `SynchronizationPort`.

Only the second pair participates in the cycle. Splitting the two roles removes it
without `@Lazy` and restores the one-way dependency D2 describes.

Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`) and deferred from it as a module-boundary
change.

## What Changes

- Split `OrisEventImportPort` along the seam above: a gateway interface carrying the ORIS
  field primitives (`readOrisFields`, `applyOrisSync`), and the orchestration port
  keeping `importEventFromOris` and `syncEventFromOris`.
- Point `OrisEventSyncAdapter` at the gateway only, and drop `@Lazy` from its constructor.
- Let the orchestration service keep depending on `SynchronizationPort`; with the adapter
  no longer reaching back into it, the cycle is gone.
- Verify the module-boundary tests still pass — the dependency direction should become
  strictly cleaner, not merely different.
- Optionally reconsider, as part of the same split, whether `OrisEventFields` and
  `OrisEventDetailsMapper` still belong in `events.application`. D2 places ORIS-specific
  mapping in `oris.eventsync`, and three of the four mappers already live there; this
  proposal does not require moving them, but the split is the natural moment to decide.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/events/spec.md` — unaffected. "ORIS-Imported Events Are Kept In Step
  Automatically", "A Manager's Edit To An ORIS Field Is Never Silently Overwritten" and
  the ORIS import requirements all describe outcomes; none constrains which Spring bean
  holds which method.
- `openspec/specs/data-synchronization/spec.md` — unaffected. The adapter contract, the
  engine and the record lifecycle are untouched.

**Why no spec update is needed:**

The same four operations remain available and behave identically; only their distribution
across interfaces and beans changes. No endpoint, payload, authority or outcome is
affected.

## Impact

- **Modules:** `events.application` (interface split), `oris.eventsync` (adapter
  dependency).
- **Code:** one interface becomes two; one implementation is split or one class comes to
  implement both; `@Lazy` disappears from `OrisEventSyncAdapter`.
- **Architecture tests:** `ModuleStructureVerificationTest`, `LayerArchitectureTest` and
  `JMoleculesArchitectureTest` all constrain this area. Note that the sync engine's
  implementation already hit a jMolecules violation here — `@PrimaryAdapter` and
  `@SecondaryAdapter` are mutually exclusive, and `OrisEventSyncAdapter` is annotated
  `@Application` for that reason. Re-run all four architecture tests explicitly; a
  package-filtered test run will not include `com.klabis.JMoleculesArchitectureTest`.
- **Benefit:** startup regains fail-fast wiring for this path, and the dependency
  direction matches what D2 describes.
