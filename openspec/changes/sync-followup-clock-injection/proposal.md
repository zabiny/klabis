## Why

The synchronisation engine reads the current time through `Instant.now()` at 13 call
sites instead of the project's `Clock` bean (`com.klabis.common.ClockConfiguration`),
which `membershipfees` already injects consistently (`MemberFeeHistoryService`,
`FeeSelectionCampaignManagementService`, `MemberChoiceService`,
`FeeSelectionCampaignController`).

Sync is the most time-dependent module in the codebase — claim leases, retry backoff,
`nextAttemptDueAt`, scan cadence and history retention are all time arithmetic — yet it
is the one module that cannot have time controlled in a test. The cost is already
visible: `SyncSchedulerTest` and `SyncHistoryRetentionJobTest` have to reach past the
domain and rewrite `started_at` / `next_attempt_due_at` columns through `JdbcTemplate`
to simulate the passage of time, because there is no seam to advance. Tests that
manipulate storage to exercise domain timing are brittle and assert less than they
appear to.

Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`) and deferred from it, because rewriting the
engine's time axis is not cleanup and carries its own risk.

## What Changes

- Inject the existing `Clock` bean into the application-layer components that read the
  clock: `SyncScheduler`, `SyncRecordClaimer`, `SyncHistoryRetentionJob`,
  `SynchronizationService`.
- Pass the resulting `Instant` into the domain rather than letting the domain read the
  clock itself: `SyncRecord`, `SyncAttempt`, `SyncConflictDetected` and
  `SyncTerminallyFailed` take the timestamp as an argument. The domain layer must not
  depend on a Spring bean, so the seam belongs at the application boundary.
- Rewrite `SyncSchedulerTest` and `SyncHistoryRetentionJobTest` to advance a fixed
  `Clock` instead of rewriting rows through `JdbcTemplate`.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — unaffected. Its requirements
  constrain *what* happens over time (a record stops after N consecutive failures, a
  retry waits, a claim is held for a bounded period), never how the current instant is
  obtained.
- `openspec/specs/non-functional-requirements/spec.md` — unaffected. "Synchronisation
  Limits Are Configurable" fixes the defaults (5 failures, 15-minute first retry,
  doubling, 24-hour cap, 5-minute lease, 15-minute scan, nightly full pass, 30-day
  retention); those values and their effects are unchanged. The scenarios say an
  operator changes a value in configuration and synchronisation follows it — which
  remains true.
- `openspec/specs/events/spec.md` — unaffected. No requirement mentions timing beyond
  "kept in step automatically".

**Why no spec update is needed:**

`Clock.systemDefaultZone()` is what `Instant.now()` already uses, so in production every
call site resolves to the same instant it does today. This is a dependency-injection
seam, not a change in scheduling, ordering, or duration. The only observable difference
is in tests, which gain the ability to control time.

## Impact

- **Modules:** `sync` (application and domain layers), no other module.
- **Code:** 13 call sites across 8 files; constructor signatures of four application
  components and several domain factory methods gain a parameter.
- **Tests:** `SyncSchedulerTest` and `SyncHistoryRetentionJobTest` lose their
  `JdbcTemplate` time manipulation. Other sync tests may need a `Clock` in their
  configuration.
- **Developer workflow:** future time-dependent sync tests become writable without
  touching storage.
- **Risk:** touching the engine's time axis is the reason this was not folded into the
  cleanup pass. Every timing assertion must be re-verified, not just re-run.
