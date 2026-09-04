## Why

`SyncOutcomeWriter` defends against one race with four mechanisms stacked on top of each
other:

1. a `@Lazy` self-injected proxy, so `@Transactional` applies to an internally-invoked
   method,
2. `Propagation.REQUIRES_NEW`, because a retry inside the failed transaction would trade
   `OptimisticLockingFailureException` for `UnexpectedRollbackException`,
3. a manual retry that re-reads the stored version and overwrites the record's
   `AuditMetadata` version stamp — which effectively disables optimistic locking for that
   write,
4. and, in `SynchronizationService.markDirty`, a swallowed `OptimisticLockingFailureException`.

Each layer is individually justified, and the javadoc explains all four honestly — this
is not careless code. But the stack exists because two independent things write to the
same `sync_record` row: the outcome of a pass, and the dirty marker raised when an inward
write triggers the module's own `EventUpdatedEvent` listener.

The engine's own design says the dirty marker is not a domain change. `design.md` D9
records that `dirtySince` is a scheduling hint that must never affect correctness. If it
is not a domain change, it should not be taking the aggregate's version — and if it does
not take the version, the race disappears and all four layers become unnecessary.

Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`) and deferred from it: this is a change to how
the module persists, not cleanup.

## What Changes

- Persist the dirty marker as a targeted update that does not bump the aggregate version,
  reflecting D9's own position that marking a record dirty is a scheduling hint rather
  than a domain change.
- With the write conflict gone, remove the layers it forced:
  - the manual optimistic-lock retry and the version-stamp overwrite in `SyncOutcomeWriter`,
  - `Propagation.REQUIRES_NEW`, which existed only to give the retry a fresh transaction,
  - the `@Lazy` self-proxy, which existed only to make that retry transactional,
  - the swallowed `OptimisticLockingFailureException` in `SynchronizationService.markDirty`.
- Keep the property that motivated the class in the first place: the record and its
  attempt row are still written atomically (`design.md` D15 — a crash must never leave an
  attempt unrecorded). `SyncOutcomeWriter` stays a separate bean so the call from
  `SynchronizationService` still crosses a proxy boundary.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — unaffected. "Every Synchronisation
  Attempt Is Recorded" still holds: the record and attempt are written in one
  transaction. "Records Are Kept In Step Automatically" is unchanged; a record marked
  dirty is still picked up by the next scan.
- `openspec/specs/non-functional-requirements/spec.md` — reviewed for "One
  Synchronisation At A Time Per Record". The claim mechanism is untouched; this change
  concerns the version stamp on the outcome write, not the lease.
- `openspec/specs/events/spec.md` — unaffected.

**Why no spec update is needed:**

The outcomes persisted, their contents and their atomicity are identical. What changes is
which writes contend for the aggregate's version — an internal persistence concern with
no scenario describing it. Where today a losing write retries and succeeds, afterwards it
does not have to retry; the committed result is the same either way.

## Impact

- **Modules:** `sync` only (`SyncOutcomeWriter`, `SynchronizationService`, the record
  repository adapter).
- **Code:** a net reduction — one targeted update replaces four defensive layers.
- **Risk:** this is the highest-risk of the four sync follow-ups. It touches transaction
  boundaries and concurrency on the engine's central write path, and the failure mode
  (a lost dirty marker, or an attempt row missing after a crash) is silent and rare.
  `SynchronizationServiceMarkDirtyIntegrationTest` and
  `SynchronizationServiceFailureHandlingIntegrationTest` cover this area and must be
  strengthened, not merely kept passing.
- **Prerequisite:** confirm against `design.md` D9 and D15 in the archived change before
  starting, since the argument for the whole proposal rests on them.
