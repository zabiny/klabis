## 1. Domain layer takes the instant as an argument

- [ ] 1.1 Change `SyncAttempt` factory methods to accept the `Instant` instead of calling `Instant.now()`.
- [ ] 1.2 Change `SyncRecord` methods that stamp a time (claim, release, dirty marking, `nextAttemptDueAt`, failure handling) to accept the `Instant` from their caller.
- [ ] 1.3 Change `SyncConflictDetected` and `SyncTerminallyFailed` to carry the instant passed by the publisher.
- [ ] 1.4 Verify no `Instant.now()` remains under `sync/domain/` or `sync/*.java`.

## 2. Application layer injects the Clock

- [ ] 2.1 Inject `Clock` into `SyncRecordClaimer` and pass `clock.instant()` into the claim/lease calls.
- [ ] 2.2 Inject `Clock` into `SyncScheduler`; use it for due-record selection and pass it onward.
- [ ] 2.3 Inject `Clock` into `SyncHistoryRetentionJob`; compute the retention cut-off from it.
- [ ] 2.4 Inject `Clock` into `SynchronizationService`; pass the instant into every domain call that now expects one.
- [ ] 2.5 Verify no `Instant.now()` remains under `sync/application/`.

## 3. Tests control time instead of storage

- [ ] 3.1 Rewrite `SyncHistoryRetentionJobTest` to advance a fixed `Clock` rather than rewriting `started_at` through `JdbcTemplate`.
- [ ] 3.2 Rewrite `SyncSchedulerTest` to advance a fixed `Clock` rather than rewriting `next_attempt_due_at` through `JdbcTemplate`.
- [ ] 3.3 Confirm each rewritten test still asserts the same behaviour as before — the assertions must not weaken when the mechanism changes.
- [ ] 3.4 Supply a `Clock` bean to any sync test configuration that now needs one.

## 4. Verification

- [ ] 4.1 Run the full backend test suite; all tests compile and pass.
- [ ] 4.2 Code review, focused on whether any timing assertion silently became weaker.
- [ ] 4.3 Commit.
