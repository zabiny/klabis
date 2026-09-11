## Why

Scheduled work is spread across four modules with no shared mechanism. The
application currently has six `@Scheduled` methods:

| Job | Schedule | Configurable |
|---|---|---|
| `TokenCleanupJob` | `0 0 0 * * *` | no — hardcoded in the annotation |
| `EventCompletionScheduler` | `0 0 2 * * *` | no — hardcoded in the annotation |
| `FeeSelectionDeadlineScheduler` | `0 0 3 * * *` | no — hardcoded in the annotation |
| `SyncHistoryRetentionJob` | `0 0 3 * * *` | no — hardcoded in the annotation |
| `SyncScheduler.runFullPass` | `${klabis.sync.scan-cron}` | yes |
| `SyncScheduler.runDueScan` | `${klabis.sync.due-scan-interval}` | yes |

Four of the six carry their cron expression inline, so changing when they run needs a
rebuild. There is no shared way to see what is scheduled, when a job last ran, whether
it failed, or to run one by hand. Each module solved the problem for itself.

Three things make this worth addressing now rather than later:

- **More scheduled work is coming.** `gh-89-deadline-reminder-notifications` describes
  *"a scheduled job scans events whose registrationDeadline is within the next N
  days"*, and the notification capability behind issues #27 and #28 will add more.
  Every new job repeats the pattern and inherits the same gaps.
- **Notifications want a different scheduling shape.** Sending a reminder 24 hours
  before a specific event's deadline is naturally "run once, at this computed
  instant". Spring's `@Scheduled` only does fixed cadences, which forces a polling
  design — `gh-89` proposes exactly that, plus a `sent_deadline_reminders` table to
  suppress duplicates. A durable scheduler can hold a one-shot trigger instead.
- **A second backend instance would run every job twice.** Nothing today prevents it.
  This is not an active problem — one instance is deployed — but it is a property that
  gets harder to retrofit as the number of jobs grows.

## What Changes

- **A Quartz `Scheduler` bean** with a clustered JDBC `JobStore`, so triggers are
  durable and, if a second instance is ever deployed, each job fires once across the
  cluster.
- **The `QRTZ_*` tables** as a database migration.
- **The four plain periodic jobs move onto it** — `TokenCleanupJob`,
  `EventCompletionScheduler`, `FeeSelectionDeadlineScheduler`,
  `SyncHistoryRetentionJob`. Each becomes a Quartz `Job` with its schedule supplied by
  configuration instead of an inline annotation. The work each one performs is
  untouched; only what invokes it changes.
- **`SyncScheduler`'s two cadences move onto it as ordinary cron/interval jobs.** They
  keep calling `SyncRecordRepository.findDueForScan` exactly as they do today.
- **A documented way to add a job**, so the next one does not invent its own approach.

Explicitly **not** in this change:

- **`sync`'s per-record scheduling stays as it is.** The `sync.sync_schedule` table,
  `ScheduleEffect`, `SyncSchedule`, the claim mechanism and the D15 transaction
  boundary in `SyncOutcomeWriter` are all untouched. Turning each `sync_record` into
  its own Quartz job is a separate, much riskier question — see
  `consider-migration-of-sync-to-quartz`, which recommends against it.
- **No admin UI** for inspecting or triggering jobs. Quartz makes that possible later;
  this change only puts the schedules in one durable place.
- **No new jobs.** `gh-89` and the notification work are the motivation, not the scope.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/non-functional-requirements/spec.md` — the one spec that constrains
  scheduling, and it constrains only `sync`:
  - *"Synchronisation Operational Configuration"* requires the scan cadences to be
    configurable, *"follows the new values after a restart"*, with documented defaults
    (every 15 minutes; full comparison nightly). Quartz satisfies this identically —
    the values keep coming from `SyncProperties`, and a restart is still what applies
    them. **This requirement must not regress**: whatever supplies the cron to the
    Quartz trigger has to keep reading the same configuration keys and the same
    defaults.
  - *"One Synchronisation At A Time Per Record"* is satisfied by the claim mechanism
    (`claimed_at` / `claimLease`), which this change does not touch.
  - *"Synchronisation history removed automatically after a configurable retention
    period"* — `SyncHistoryRetentionJob` keeps doing exactly this; only its trigger
    changes.
- `openspec/specs/data-synchronization/spec.md` — unaffected. It describes scheduling
  only as *"scheduled synchronisation runs"*, never a mechanism or a cadence. Both
  cadences continue to run and continue to call the same query.
- `openspec/specs/events/spec.md` — unaffected. `EventCompletionScheduler` calls
  `EventManagementPort.finishExpiredActiveEvents(date)`; the port, the date it is
  given and the resulting state changes are unchanged.
- `openspec/specs/membership-fees/spec.md` — unaffected. `FeeSelectionDeadlineScheduler`
  calls `CampaignEndProcessingPort.processCampaignEnd(today)`, unchanged.
- `openspec/specs/users-authentication/spec.md` — unaffected. `TokenCleanupJob` deletes
  expired password-setup tokens; expiry semantics are unchanged.

**Why no spec update is needed:**

This substitutes the mechanism that invokes already-specified work. Every job runs at
the same time, does the same thing and produces the same outcome; only the thing that
calls it changes. No requirement names Spring `@Scheduled`, and no requirement
constrains the four non-sync jobs' cadence at all.

**Two boundaries to watch during implementation** — cross either and this change must
be re-raised under the `spec-driven` schema:

1. **Making the four non-sync schedules configurable is an added capability, not a
   preserved one.** No spec requires it today. That is a widening rather than a
   behaviour change and is a natural part of the migration, but if it grows into
   operator-facing configuration with its own documented defaults — as
   *"Synchronisation Operational Configuration"* is for `sync` — it deserves a
   requirement of its own.
2. **Misfire handling is new observable behaviour.** Today a job missed during a
   restart is simply skipped until its next occurrence. Quartz's default misfire
   policy may run it immediately on startup instead. Each job needs a deliberate
   misfire policy chosen to match today's behaviour, otherwise `processCampaignEnd`
   or `finishExpiredActiveEvents` could run at a time they never would have before.

## Impact

**Affected code (backend):**

- New Quartz configuration and a small registration mechanism for jobs.
- `com.klabis.common.users.infrastructure.TokenCleanupJob`,
  `com.klabis.events.infrastructure.scheduler.EventCompletionScheduler`,
  `com.klabis.membershipfees.infrastructure.scheduler.FeeSelectionDeadlineScheduler`,
  `com.klabis.sync.application.SyncHistoryRetentionJob`,
  `com.klabis.sync.application.SyncScheduler` — each loses `@Scheduled` and gains a
  Quartz `Job` wrapper. The methods doing the work keep their current signatures;
  several already expose a parameterised variant (`completeExpiredEvents(LocalDate)`)
  that tests call directly, and that stays true.
- `KlabisApplication` — `@EnableScheduling` can go once no `@Scheduled` remains.

**Data:** the `QRTZ_*` tables. No change to any existing table.

**Dependencies:** `spring-boot-starter-quartz`.

**APIs (REST):** none.

**Frontend:** none.

**Build and test workflow:**

- Quartz ships database-specific DDL. Tests run on H2 in `MODE=PostgreSQL`, so
  `StdJDBCDelegate`'s behaviour there needs verifying early. This is the same class of
  problem that already forced a two-statement `UPDATE`-then-`INSERT` upsert in
  `SyncScheduleJdbcRepository`, because H2 in that mode rejects `ON CONFLICT`.
- Tests must not start a live scheduler. Quartz should be in `standby` for the test
  profile, with jobs invoked directly — which is how the existing scheduler tests
  already work, since they call the inner methods rather than waiting for a cron.
- `Clock` is already injected across `sync` (archived change
  `2026-09-06-sync-followup-clock-injection`), so the jobs' own time handling is
  already testable without waiting on a scheduler.

## Open Questions

1. **Clustered `JobStore` now, or a simple one until a second instance exists?** The
   clustered store is the reason clustering works at all, but it adds row-locking and
   requires every instance to have a synchronised clock. Enabling it from the start
   avoids a later migration; deferring it keeps the first step smaller.

2. **Where do job schedules live in configuration?** A flat namespace
   (`klabis.scheduling.<job>.cron`) is uniform, but `sync`'s two cadences already have
   established keys under `klabis.sync.*` that the NFR spec's requirement is written
   against. Keep those where they are and add the rest alongside, or unify?

3. **What is the misfire policy per job?** See the boundary noted above. Most likely
   "do nothing, wait for the next occurrence" to match today, but each job should be
   decided on rather than inheriting a default.

4. **Does `SyncScheduler` keep its per-record try/catch and circuit-breaker check?**
   It currently stops a scan when the breaker opens and swallows per-record failures so
   one bad record cannot abort the run. Both should survive the move unchanged — to
   confirm no Quartz-level retry or failure handling is layered on top and changes the
   outcome.

5. **Is a scheduled-jobs admin view wanted later?** Out of scope here, but if it is
   likely, it influences how much metadata each job carries (a display name, a
   description, whether it is safe to trigger by hand).
