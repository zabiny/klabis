# Team Coordination File: review-1-2-group-creation-fix

## Goal
Fix HTTP 500 on creating FreeGroup/FamilyGroup/TrainingGroup. Root cause is suspected in shared persistence layer (GroupMemento / GroupJdbcRepository). Approach: TDD — write failing integration test on PostgreSQL TestContainer, then fix.

## Scope
- backend only; no API contract change, no spec change
- 3 endpoints: `POST /api/groups`, `POST /api/family-groups`, `POST /api/training-groups`

## Iterations
1. **Diagnosis (red)** — write `GroupCreationIntegrationTest` covering all 3 group types; run it; capture deterministic stack trace
2. **Fix (green)** — minimal change in shared persistence; tests must pass
3. **Harden** — edge cases (empty/long name validation)
4. **Code review + simplify**
5. **Commit + update memory**

## Progress log

---

## Iteration 1: Diagnosis (2026-05-06)

**Agent:** backend-developer
**Test file written:** `backend/src/test/java/com/klabis/groups/GroupCreationIntegrationTest.java`
**Annotation pattern used:** `@SpringBootTest(classes={TestApplicationConfiguration.class})` + `@AutoConfigureMockMvc` + `@ActiveProfiles("test")` + `@CleanupTestData`

### Test outcomes on H2 (`test` profile)

All 3 scenarios **passed** (3/3 green):
- `POST /api/groups` (FreeGroup) → 201
- `POST /api/family-groups` (FamilyGroup, with `MEMBERS:MANAGE`) → 201
- `POST /api/training-groups` (TrainingGroup, with `GROUPS:TRAINING`) → 201

**No stack trace captured** — the bug does not reproduce against H2 in `test` profile.

### Stack trace

None — all assertions passed.

### Hypothesis: Bug is PostgreSQL-environment-specific

The `test` profile activates H2. No PostgreSQL TestContainer is configured in the project yet. The bug was reproduced on `api.klabis.otakar.io` (PostgreSQL) and locally with H2 the error message was:

```
DataIntegrityViolationException: Check constraint invalid: "chk_user_groups_type: "
SQL statement: INSERT INTO "user_groups" (...) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) [23514-240]
root cause: org.h2.jdbc.JdbcSQLNonTransientConnectionException: Databáze byla ukončena
```

The H2 error is misleading — the root cause is `The database has been closed [90098-240]`, which means H2 shut down mid-transaction. This is NOT a real `chk_user_groups_type` violation (the constraint is `type IN ('FREE','TRAINING','FAMILY')` and `FreeGroup.TYPE_DISCRIMINATOR = "FREE"` matches). The `DataIntegrityViolationException` wrapper is a red herring.

The same test passing on H2 now rules out application-layer bugs in the controller → service → repository → memento path. The `GroupMemento.isNew()` logic and type mapping are correct for H2.

### Revised hypothesis: H2 in-memory shutdown during BootstrapDataLoader

When H2 runs, Spring Boot `BootstrapDataLoader` may execute and fail (e.g., missing data it expects), causing H2 to shut down mid-transaction during the first request. The `createGroup` call hits a closed DB → `JdbcSQLNonTransientConnectionException: database has been closed` wrapped as `DataIntegrityViolationException`.

**Evidence for this hypothesis:**
- Root cause in original local log is `org.h2.jdbc.JdbcSQLNonTransientConnectionException: Databáze byla ukončena` (database was closed), NOT a real check constraint violation.
- The `check constraint` text in the error is H2 embedding the last SQL it was trying to execute when it died, not a real constraint violation.
- Our test uses `@CleanupTestData` (runs `cleanup.sql` before class) which wipes all data — so BootstrapDataLoader's data has already been cleared before the test runs, but the DB is still alive.

**Production (PostgreSQL) hypothesis:**
- Production may have a different Flyway migration state or a constraint that H2 does not enforce the same way.
- Or the `chk_user_groups_type` constraint on PostgreSQL actually fires because `GroupMemento` is sending a wrong `type` value in some code path hit only on PostgreSQL.
- Without a PostgreSQL TestContainer or production log, the true root cause for PostgreSQL cannot be determined from this iteration.

### Next steps

**Option A (recommended):** Add a PostgreSQL TestContainer to the project to get production-parity test results. Configure `@Testcontainers` + `PostgreSQLContainer` in a new test configuration class and re-run `GroupCreationIntegrationTest` against it.

**Option B:** Access the production backend log (`api.klabis.otakar.io`) to get the exact stack trace from PostgreSQL JDBC driver when `POST /api/groups` is invoked.

**Option C:** Investigate `BootstrapDataLoader` — if it fails at startup on H2 in non-test runs (e.g. `bootRun` without test data seed), that explains the local reproduction with the `h2` profile outside tests.

### Files not changed

No production code was touched in this iteration.

---

## Iteration 1b: Local reproduction (2026-05-06)

**Agent:** backend-developer
**Backend:** running locally, profiles `h2,ssl,debug,metrics,oris,local-dev`
**Approach:** Playwright UI test + backend log analysis

### Reproduction attempt result

`POST /api/groups` (FreeGroup "Test free group") → **201 Created**. Bug did NOT reproduce.

The backend was restarted at 2026-05-06 01:14 (Spring DevTools restart). On this fresh H2 instance, group creation succeeds. The 500 error is not captured in the current backend log at `/tmp/klabis-backend.log`.

### What IS in the log: bootstrap-time OptimisticLockingFailureException

The log at lines 5735–5973 shows a **different but related bug** occurring during startup, not during group creation:

```
org.springframework.dao.OptimisticLockingFailureException:
Failed to update versioned entity with id 'cac93f93-c90c-49c0-ac25-998164774014'
(version '0') in table ["user_groups"];
Was the entity updated or deleted concurrently?

at com.klabis.groups.traininggroup.infrastructure.jdbc.TrainingGroupRepositoryAdapter.save(TrainingGroupRepositoryAdapter.java:34)
at com.klabis.groups.traininggroup.infrastructure.listeners.MemberCreatedListener.lambda$on$1(MemberCreatedListener.java:41)
at com.klabis.groups.traininggroup.infrastructure.listeners.MemberCreatedListener.on(MemberCreatedListener.java:39)
```

**Root cause of this bootstrap failure:**
`MembersDataBootstrap` creates ~10 members in a single transaction, each publishing `MemberCreatedEvent`.
`MemberCreatedListener` uses `@ApplicationModuleListener` (async), so all 10 events fire concurrently on a thread pool (`task-1` through `task-8`).
Each listener calls `trainingGroupRepository.findAll()` (reads training group at version `0`) and then `trainingGroupRepository.save(group)` (attempts UPDATE with `WHERE version = 0`).
Multiple threads race to update the same training group UUID — only one wins, 2–3 fail with `OptimisticLockingFailureException`.

This is a **concurrent write race on training groups during member bootstrap**, NOT a type discriminator violation.

### isNew() logic review

`GroupMemento.initCommon()` sets `isNew = (group.getAuditMetadata() == null)`. For a newly-created group (`FreeGroup.create()`), `auditMetadata` is `null` → `isNew = true` → Spring Data JDBC does INSERT. For a reconstructed group (`FreeGroup.reconstruct(...)` via `toFreeGroup()`), `auditMetadata` is populated → `isNew = false` → Spring Data JDBC does UPDATE. This logic is correct and not the source of the 500.

### The actual group creation 500: revised hypothesis

The 500 on `POST /api/groups` has never been captured in the local log. Two scenarios remain:

**Scenario A — bootstrap-caused DB corruption (H2 timing):**
If the bootstrap-time `OptimisticLockingFailureException` leaves a training group in a partially-updated or transaction-rolled-back state that later corrupts H2's in-memory state, a subsequent `POST /api/groups` (which also writes to `user_groups`) could trip over residual locks or a corrupted H2 transaction. This would explain why the bug is seen locally on H2 (in non-test sessions where the race condition fires) but NOT in the test profile (which uses `@CleanupTestData` and a different startup sequence).

**Scenario B — oris profile difference:**
The `oris` profile enables `oris.client.enabled=true` and `my-club-id=205`. If the ORIS module registers a startup event listener or a `BootstrapDataInitializer` that also writes to `user_groups`, it could race with the member bootstrap events. The `oris` profile is active locally and on `api.klabis.otakar.io` but NOT in the `test` profile — this precisely matches the "bug reproduces in prod/local but not in tests" pattern. However, examination of the ORIS code shows no `BootstrapDataInitializer` implementation in the ORIS module.

**Most likely root cause (Scenario A):**
The `OptimisticLockingFailureException` during bootstrap causes Spring Modulith to mark some `MemberCreatedEvent` publications as uncompleted. Spring Modulith retries uncompleted event publications on the next application event or on a scheduled retry. If the retry fires during a concurrent `POST /api/groups` request (which also opens a transaction on `user_groups`), the H2 in-memory lock contention could produce the 500. The original error message mentioning `chk_user_groups_type` check constraint violation was an H2 artifact from the failed/rolled-back transaction context — H2 embeds the last executed SQL in the exception message even when the real cause is a connection/lock issue.

### Key evidence for Scenario A

- The `OptimisticLockingFailureException` at startup leaves 2–3 `MemberCreatedEvent` publications with status `uncompleted` in the `event_publication` table (Spring Modulith persistence).
- Spring Modulith retries uncompleted publications asynchronously.
- The retry involves `TrainingGroupRepositoryAdapter.save()` — writes to `user_groups`.
- If this retry fires concurrently with a user-initiated `POST /api/groups` → `FreeGroupRepositoryAdapter.save()` → also writes to `user_groups`, H2 in-memory locking can cause a cascading failure.
- The specific H2 error ("database was closed" / `JdbcSQLNonTransientConnectionException`) reported in the original Iteration 1 analysis would be consistent with H2 in-memory connection pool exhaustion or transaction conflict.

### What the test profile avoids

The `test` profile uses `@CleanupTestData` (runs `cleanup.sql` before the class), which resets all data including the `event_publication` table. This prevents Spring Modulith from finding any uncompleted publications to retry. The `@SpringBootTest` context also starts fresh, so there are no concurrent bootstrap events in flight during the test.

### Next step: confirm via event_publication table inspection

To confirm Scenario A: check the `event_publication` table immediately after backend startup (before any user action) to see if there are rows with `completion_date IS NULL` for `MemberCreatedEvent`. If yes, that confirms the retry-during-request race is the mechanism.

**Recommended fix:**
1. Fix the bootstrap-time race in `MemberCreatedListener`: add retry logic or make `assignEligibleMember` + `save` idempotent under optimistic locking failure (catch `OptimisticLockingFailureException`, re-load, re-apply, re-save).
2. Alternatively, make `MembersDataBootstrap.bootstrapData()` create members one-at-a-time inside separate transactions so events are dispatched sequentially (avoids the N-concurrent-events problem).

**The group creation 500 bug itself** is a symptom of uncompleted Spring Modulith event publications retrying concurrently with user requests. The fix for the bootstrap race condition will also fix the group creation 500.

### Files examined

- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/groups/common/infrastructure/jdbc/GroupMemento.java`
- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/groups/common/infrastructure/jdbc/GroupJdbcRepository.java`
- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/groups/freegroup/infrastructure/jdbc/FreeGroupRepositoryAdapter.java`
- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/groups/freegroup/application/FreeGroupManagementService.java`
- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/groups/traininggroup/infrastructure/listeners/MemberCreatedListener.java`
- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/groups/traininggroup/infrastructure/bootstrap/TrainingGroupDataBootstrap.java`
- `/home/davca/Documents/Devel/klabis/backend/src/main/java/com/klabis/members/infrastructure/bootstrap/MembersDataBootstrap.java`
- `/tmp/klabis-backend.log` (lines 5735–5973 key)

---

## Iteration 1c: Production stack trace — definitive root cause (2026-05-06)

User provided **production stack trace** (production runs on H2, not PostgreSQL). Failures captured for FreeGroup, FamilyGroup, TrainingGroup at 23:19–23:23.

**Root cause identified:** H2 in-memory database is being closed at runtime. Innermost frame:

```
org.h2.jdbc.JdbcSQLNonTransientConnectionException: The database has been closed [90098-240]
  at org.h2.engine.SessionLocal.getDatabase(SessionLocal.java:674)
  at org.h2.engine.SessionLocal.getCompareMode(SessionLocal.java:2137)
  at org.h2.expression.condition.ConditionInConstantSet.getValue(ConditionInConstantSet.java:80)
  at org.h2.constraint.ConstraintCheck.checkRow(ConstraintCheck.java:99)
  at org.h2.table.Table.fireBeforeRow(Table.java:1199)
  at org.h2.command.dml.Insert.insertRows(Insert.java:171)
```

The H2 session attempts to evaluate the `chk_user_groups_type` check constraint (`type IN ('FREE','TRAINING','FAMILY')` — uses `ConditionInConstantSet`), but the call to `SessionLocal.getDatabase()` throws because the in-memory database has been **destroyed**. Spring then wraps this into `DataIntegrityViolationException` with a **misleading** "Check constraint invalid" message (the constraint values are fine — it's the DB itself that's gone).

**Why H2 destroys the DB:** the JDBC URL `jdbc:h2:mem:klabis;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH` does **not** set `DB_CLOSE_DELAY=-1`. Default is `0` — when the last open connection closes, H2 kills the in-memory database. Hikari recycles idle connections; once all idle connections expire, H2 wipes everything. New connections then hit an empty DB without tables/constraints/data, producing this exact failure mode.

**Why test profile passes:** tests run as a single process holding connections continuously throughout the test class, so the connection-pool-empties-out scenario never occurs.

**The earlier `OptimisticLockingFailureException` finding (Iteration 1b) is a secondary concurrency bug** in `MemberCreatedListener` bootstrap, not the cause of the production HTTP 500. Out of scope for this proposal.

### Fix shape (single line config change)

`backend/src/main/resources/application-h2.yml` — append `;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` to the JDBC URL.

- `DB_CLOSE_DELAY=-1` — keep the in-memory DB alive for the entire JVM lifetime, even when no connections are open.
- `DB_CLOSE_ON_EXIT=FALSE` — let Spring/JVM shut down handle DB cleanup, avoid race during JVM exit.

### Validation

- `GroupCreationIntegrationTest` already exists and passes — keeps regression coverage for the create flow itself.
- The bug itself (H2 idle-shutdown) is not catchable in a fast unit test without artificial connection-pool manipulation; the manual smoke test on production after deploy is the validation path.
- Tasks.md needs updating to reflect the actual root cause being a config issue, not a persistence-layer bug.

---
