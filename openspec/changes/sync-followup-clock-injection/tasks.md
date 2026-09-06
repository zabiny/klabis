## 1. Domain layer takes the instant as an argument

- [x] 1.1 Change `SyncAttempt` factory methods to accept the `Instant` instead of calling `Instant.now()`.
- [x] 1.2 Change `SyncRecord` methods that stamp a time (claim, release, dirty marking, `nextAttemptDueAt`, failure handling) to accept the `Instant` from their caller.
- [x] 1.3 Change `SyncConflictDetected` and `SyncTerminallyFailed` to carry the instant passed by the publisher.
- [x] 1.4 Verify no `Instant.now()` remains under `sync/domain/` or `sync/*.java`.

## 2. Application layer injects the Clock

- [x] 2.1 Inject `Clock` into `SyncRecordClaimer` and pass `clock.instant()` into the claim/lease calls.
- [x] 2.2 Inject `Clock` into `SyncScheduler`; use it for due-record selection and pass it onward.
- [x] 2.3 Inject `Clock` into `SyncHistoryRetentionJob`; compute the retention cut-off from it.
- [x] 2.4 Inject `Clock` into `SynchronizationService`; pass the instant into every domain call that now expects one.
- [x] 2.5 Verify no `Instant.now()` remains under `sync/application/`.

## 3. Tests control time instead of storage

- [x] 3.1 Rewrite `SyncHistoryRetentionJobTest` to advance a fixed `Clock` rather than rewriting `started_at` through `JdbcTemplate`.
- [x] 3.2 Rewrite `SyncSchedulerTest` to advance a fixed `Clock` rather than rewriting `next_attempt_due_at` through `JdbcTemplate`.
- [x] 3.3 Confirm each rewritten test still asserts the same behaviour as before — the assertions must not weaken when the mechanism changes.
- [x] 3.4 Supply a `Clock` bean to any sync test configuration that now needs one.

## 4. SyncCapabilities named factories

- [ ] 4.1 Read `docs/design-decisions.md` ADR-005 / design D3 and confirm which flag combinations are actually in use before naming anything — the factory names must describe real integration shapes, not invented ones. Today there are two distinct combinations across ten call sites.
- [ ] 4.2 Add static factory methods to `SyncCapabilities` covering those combinations. Name them for what the integration *is* (e.g. what it may read, write and create), not for the flag positions.
- [ ] 4.3 Keep the canonical constructor usable for a combination no factory covers; do not add a factory per hypothetical shape.
- [ ] 4.4 Move `OrisEventSyncAdapter` (line ~60) onto a factory.
- [ ] 4.5 Move the nine test call sites onto factories: `TestSynchronizationAdapter`, `SyncSchedulerTest`, `SyncRecordDirectionResolutionTest` (2), `SynchronizationServiceIntegrationTest` (3), `SynchronizationServiceMarkDirtyIntegrationTest`, `SynchronizationServiceFailureHandlingIntegrationTest`.
- [ ] 4.6 Verify each replacement produces the identical flag combination it replaced — compare component by component, not by eye. A transposition here is exactly the defect this task exists to prevent, and it would compile.
- [ ] 4.7 Confirm no `new SyncCapabilities(` with a positional boolean list remains outside the factories themselves.

## 5. Verification

- [ ] 5.1 Run the full backend test suite; all tests compile and pass.
- [ ] 5.2 Code review, focused on whether any timing assertion silently became weaker, and on task 4.6 — that every factory call yields the flags its call site previously passed.
- [ ] 5.3 Commit.
