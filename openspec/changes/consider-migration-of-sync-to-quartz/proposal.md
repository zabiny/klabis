## Status: UNDER CONSIDERATION — investigation only, not approved for implementation

**This proposal is a record of an investigation, not a decision**, and it is scoped to
one specific question:

> Should each individual `sync_record` be scheduled by its own Quartz trigger, instead
> of by the `sync.sync_schedule` table that a polling scan reads?

Adopting Quartz as the application's scheduler is a **separate and healthier** change,
proposed on its own in `introduce-quartz-scheduler`. That one moves the six existing
`@Scheduled` jobs — including `sync`'s two scan cadences — onto a shared durable
scheduler, and deliberately leaves `sync`'s per-record scheduling alone. **Nothing here
blocks it, and it is not a prerequisite for it.**

What remains for this proposal is the last step only: dissolving `sync_schedule` into
Quartz triggers, one per record. **The current recommendation is not to do it** — see
"Recommendation". This document exists so the analysis does not have to be redone, and
so the reasons are on file if someone proposes it again.

Do not create `design.md` or `tasks.md` for this change unless someone explicitly
decides the per-record migration is wanted.

## Why

Once `introduce-quartz-scheduler` is in place, a Quartz `Scheduler` exists and `sync`'s
two scan cadences already run on it. The natural next question is whether the polling
scan should go away entirely: rather than waking every 15 minutes to ask *which records
are due*, each record could carry its own trigger that fires when that record is due.

That is a real and conventional design. It also turns out to be the point where the
comparison with the current mechanism stops being favourable, for reasons that are not
obvious until the consumers of `sync_schedule` are traced:

- **The only functional gap it would close is already closed.** Per-record triggers
  buy nothing for clustering — that is solved as soon as the two scan cadences are
  Quartz jobs, which `introduce-quartz-scheduler` does.
- **Timing precision is not a motivation.** `next_attempt_due_at` is a *lower bound*
  ("not before T"), consulted by a 15-minute poll — not a fire time. The retry backoff
  starts at 15 minutes and grows to a 24-hour cap, so the delay polling introduces is
  within the noise of the backoff itself.
- **`sync_schedule` is not just scheduling state.** One of its two columns feeds a
  correctness decision in `runPass`, which is the finding that makes this more than a
  mechanical substitution (see "The D3 short-circuit" below).

## What Changes

**Assumes `introduce-quartz-scheduler` is already done** — a Quartz `Scheduler`, the
`QRTZ_*` tables and the dependency all come from there. This change adds only what is
specific to scheduling records individually.

**Added:**

- `SyncRecordJob implements Job` — `@DisallowConcurrentExecution`, carrying
  `syncRecordId` in its `JobDataMap`, delegating to the existing
  `SynchronizationService.runScheduledPass(id)`. The job must resolve the record
  itself; a Quartz job is instantiated outside any transaction.
- Trigger lifecycle management at the five points where a record's schedule is
  created, changed or ended (see Impact).

**Removed:**

- The due-scan cadence itself — with each record carrying its own trigger there is
  nothing left to scan for. The nightly full pass stays, as the only way an external
  change that announces itself nowhere still gets noticed.
- `SyncRecordJdbcRepository.findDueForScan` and its `LEFT JOIN` onto `sync_schedule`.
- **The `sync.sync_schedule` table in its entirety**, together with `SyncSchedule`,
  `SyncScheduleRepository`, `SyncScheduleRepositoryAdapter`, `SyncScheduleJdbcRepository`
  and `SyncScheduleMemento`. Both of its columns are replaced by trigger state — see
  below.
- `ScheduleEffect` — with no schedule table left to apply an effect to, the type has
  nothing to carry. The domain methods that return one today instead return nothing,
  and the application layer calls the scheduler directly.

**Why `dirty_since` does not survive as a table (revised finding):**

An earlier draft of this proposal claimed `dirty_since` had to stay, because its
`COALESCE(dirty_since, :dirtySince)` write collapses a burst of edits into one marker
and a Quartz trigger has no equivalent. **That was wrong, and checking the consumers
disproves it.**

`dirty_since` is never read as a point in time. Every consumer tests it for
null/non-null only:

- `SynchronizationService:344` — `record.getDirtySince() == null`
- `SyncRecordJdbcRepository:85` — `ss.dirty_since IS NOT NULL`
- the tests — `isNotNull()`

`ScheduleEffect`'s own javadoc already records this: the two schedules
*"only ever get read as null/non-null (never for its actual age)"*. The `COALESCE` is
therefore not preserving an age — it is only avoiding a pointless overwrite of a flag
that is already set. Its Quartz equivalent is the ordinary "schedule unless already
scheduled" idiom:

```java
if (scheduler.checkExists(triggerKey)) return;  // already due, nothing to do
scheduler.scheduleJob(triggerBuilder.startNow().build());
```

A second edit in a burst finds the trigger present and does nothing, collapsing the
burst exactly as `COALESCE` does today.

**What genuinely has to be carried over** is narrower: `runPass` consults
`getDirtySince() == null` as one half of the version-token short-circuit (design.md
D3) — the expensive full read is skipped only when the external token is unchanged
*and* no local edit has been observed. So the pass must still be able to tell *why* it
was woken. That is one bit of information, and it can ride on the trigger that fired
(a flag in the `JobDataMap`, or separate `JobKey`s for the nightly pass and the
dirty pass) rather than in a table of its own.

This makes the migration cleaner than first assessed — but it sharpens the main risk
rather than reducing it. See "Main risk" below.

**Probably retained:**

- **The claim mechanism.** `@DisallowConcurrentExecution` plus clustered `QRTZ_LOCKS`
  covers scheduled-vs-scheduled overlap, but today's `claimed_at` / `claimLease` also
  guards a *manual* trigger against a scheduled pass, and Quartz locks only its own
  jobs. The likely outcome is *both* mechanisms coexisting — worse than the single
  mechanism in place today.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/data-synchronization/spec.md` — unaffected. The spec describes
  scheduling only in terms a user can observe, never a mechanism or a cadence:
  - *"Records Are Kept In Step Automatically"* — says the system keeps sides in step
    on its own, with no statement about how often or by what means.
  - *"Failed Synchronisation Is Retried, Then Stops And Waits"* — *"tried again
    later, with a growing delay"*. The growth curve is configuration
    (`SyncProperties.retryDelay`), and the migration keeps `RetryScheduler`'s
    derivation of it from the attempt history untouched.
  - *"The user triggers a synchronisation themselves"* — *"immediately rather than
    waiting for the next scheduled run"*. The manual path does not go through the
    scheduler in either design.
  - *"Finished Entities Stop Being Synchronised"* — *"no longer included in scheduled
    synchronisation runs"*. The outcome is identical; only the means changes, from a
    `WHERE retired_at IS NULL` predicate to an explicit `deleteJob` (see Impact).
  - *"Every Synchronisation Attempt Is Recorded"* — the attempt history is written by
    `SyncOutcomeWriter`, which this change does not touch except for the transaction
    concern noted below.
- `openspec/specs/events/spec.md` — unaffected. The ORIS import path
  (`OrisEventImportService.importEventFromOris` → `synchronizationPort.enroll`) is
  unchanged; only what `enroll` does internally with the schedule changes.

**Why no spec update is needed:**

This is an infrastructure substitution. Every requirement is written from the user's
perspective and is satisfied identically before and after: records are still
synchronised on their own, failures are still retried with a growing delay, retired
records are still left alone, and manual triggers still bypass the schedule. The
observable difference is limited to *when within a 15-minute window* a retry fires —
which no requirement constrains, and which the retry backoff dwarfs.

**Caveat:** the transaction concern in Impact is an internal-correctness risk, not a
behaviour change — but if it were resolved by *weakening* the atomicity of the attempt
write, that would put the *"Every Synchronisation Attempt Is Recorded"* requirement at
risk, and this change would then need the `spec-driven` schema instead.

## Impact

### Main risk: all scheduling state leaves our transaction (design.md D15)

`SyncOutcomeWriter.persist()` writes `sync_record`, `sync_attempt` and `sync_schedule`
in **one** `@Transactional` method. D15 requires exactly this: *"every attempt must
appear in the history, so a crash between two separately-committed writes must never
happen"*.

Quartz's `JobStore` manages its own connections. A `scheduleJob` / `rescheduleJob`
call inside that transaction is **not** part of the same commit unless Quartz is
deliberately wired to the application's `DataSource` via `JobStoreCMT` — possible, but
conditional and fragile. The alternative is a weaker guarantee: record and attempt
atomic, trigger best-effort.

**Dropping `sync_schedule` entirely makes this worse, not better.** Today `markDirty`
is an ordinary `UPDATE` in an ordinary transaction: it either commits with everything
else, or it does not happen. Once the dirty marker becomes a Quartz trigger, *every*
trace of "this record is waiting to be synchronised" lives in a store outside our
transaction boundary. A crash between the domain commit and the trigger write leaves a
record that was changed locally and that nothing will ever wake — silently, with no row
anywhere to notice it by. The current design cannot produce that state.

So the cleaner shape (no schedule table at all) and the safer shape (scheduling state
committed with the domain) pull in opposite directions. Resolving that tension is the
decisive question of this migration — not the DDL, and not the trigger bookkeeping.
It touches the exact invariant that `sync-followup-outcome-writer-transactions`
recently tightened.

### Trigger lifecycle becomes explicit

Today "stop scheduling this record" is a side effect of a query predicate. With Quartz
it becomes an operation that must not be forgotten anywhere:

| Today | With Quartz |
|---|---|
| `SynchronizationService:80` `syncScheduleRepository.createFor(id)` | `scheduler.scheduleJob(...)` plus the nightly trigger |
| `markDirty` → `UPDATE dirty_since = COALESCE(...)` | `checkExists(triggerKey)`, else `scheduleJob(...startNow())` |
| `ScheduleEffect.DUE_AT` → `UPDATE next_attempt_due_at` | `rescheduleJob` with a new `startAt` |
| `ScheduleEffect.CLEAR` / `CLEAR_DUE_AT` | `unscheduleJob`, or fall back to the nightly trigger |
| `retire(id)` → nothing; the record drops out of `WHERE` | `deleteJob` — a missed call leaves a trigger firing at a dead record |

### The D3 short-circuit needs its input from somewhere else

`runPass` skips the expensive pair of reads when the external version token is
unchanged **and** `getDirtySince() == null` (`SynchronizationService:344`). With the
schedule table gone, that second half has to arrive with the trigger — a flag in the
`JobDataMap`, or distinct `JobKey`s for the nightly pass and the dirty pass.

Worth noting that this is a behaviour-preserving substitution only if the flag is as
reliable as the column. A trigger that fires after a crash, having lost its
`JobDataMap` flag, would be read as "not dirty" and could skip a read it should have
made — turning a scheduling concern into a correctness one.

### Affected code

- `com.klabis.sync.application` — `SyncScheduler` deleted; `SyncOutcomeWriter` and
  `SynchronizationService` lose their scheduling writes and call the scheduler
  instead; new `SyncRecordJob`. `SyncRecordClaimer` likely survives (see above).
- `com.klabis.sync.domain` — `SyncSchedule` and `ScheduleEffect` deleted;
  `SyncScheduleRepository` deleted; every domain method on `SyncRecord` that returns a
  `ScheduleEffect` today changes signature.
- `com.klabis.sync.infrastructure.jdbc` — `SyncScheduleRepositoryAdapter`,
  `SyncScheduleJdbcRepository` and `SyncScheduleMemento` deleted;
  `SyncRecordJdbcRepository` loses `findDueForScan`.
- Database migration: `sync.sync_schedule` dropped. The `QRTZ_*` tables already
  exist from `introduce-quartz-scheduler`.

Roughly 15–20 files, seven of them deleted. Small in lines; the risk is concentrated
in the transaction boundary, not the volume. Note that the deletions are the *easy*
part — `ScheduleEffect` exists precisely because a dropped scheduling write compiles
cleanly and fails silently (see its javadoc, and task 4.9's "structural guard against
the dropped-effect trap"). Replacing it with scheduler calls gives that guard up.

### Build and test workflow

- The H2 / `StdJDBCDelegate` question is settled by `introduce-quartz-scheduler`; by
  the time this change is considered, Quartz already runs against the test database.
- `SyncSchedulerTest` is replaced by tests asserting trigger creation and rescheduling.
- The bigger cost is elsewhere: tests that manipulate schedule rows via `JdbcTemplate`
  today would have to drive Quartz instead (`standby` mode plus
  `scheduler.triggerJob()`). Synchronisation tests become slower and more prone to
  non-determinism, and a scheduling assertion moves from "select the row and check the
  column" to inspecting trigger state.
- `sync-followup-clock-injection` — injecting the existing `Clock` bean at the 13 sites
  still calling `Instant.now()` — would make those tests deterministic *without*
  Quartz, and is worth doing regardless of what happens here.

## Recommendation

**Do not migrate `sync`'s per-record scheduling onto Quartz.**

Every benefit originally imagined for it is delivered by `introduce-quartz-scheduler`
instead, at a fraction of the risk:

| Benefit | Delivered by |
|---|---|
| One durable, standard scheduler for the application | `introduce-quartz-scheduler` |
| Configurable schedules, no hardcoded crons | `introduce-quartz-scheduler` |
| Clustering — each job fires once across instances | `introduce-quartz-scheduler` |
| Scheduling at a computed instant (for notifications) | `introduce-quartz-scheduler` |
| Sub-15-minute precision for a sync retry | irrelevant — backoff is 15 min to 24 h |

What is left for this change is cost without a matching benefit:

- **Scheduling state leaves the domain transaction.** Today `markDirty` is an ordinary
  `UPDATE` that either commits with everything else or does not happen. With per-record
  triggers, every trace of "this record is waiting" lives outside our transaction, and
  a crash between the two writes leaves a locally-changed record that nothing will ever
  wake — silently, with no row to notice it by. The current design cannot produce that
  state.
- **A trigger lifecycle maintained by hand** where `WHERE retired_at IS NULL` does the
  job today. A forgotten `deleteJob` leaves a trigger firing at a dead record.
- **`ScheduleEffect`'s compile-time guard is given up.** It exists precisely because a
  dropped scheduling write compiles cleanly and fails silently.
- **The D3 short-circuit's input becomes less reliable** — a correctness input moving
  from a committed column to a `JobDataMap` flag.
- **Probably two overlapping concurrency mechanisms** instead of one, since Quartz
  locks only its own jobs and the claim also guards the manual trigger.
- **Slower, less deterministic tests.**

**Revisit only if** per-record scheduling acquires a requirement that polling genuinely
cannot satisfy — for example a sync that must react within seconds rather than within
the scan interval. Clustering alone is not such a requirement, and neither is
tidiness: after `introduce-quartz-scheduler`, `sync_schedule` is not an inconsistency
in the scheduling approach but an ordinary piece of domain state that happens to be
read by a scheduled job.
