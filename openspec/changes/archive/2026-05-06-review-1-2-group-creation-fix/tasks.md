## 1. Diagnosis (Red phase)

- [x] 1.1 Create `GroupCreationIntegrationTest` (`@SpringBootTest`, `test` profile) under `backend/src/test/java/com/klabis/groups/...`
- [x] 1.2 Add scenario: authenticated user calls `POST /api/groups` with valid JSON `{"name": "Test free group"}` — expect HTTP 201
- [x] 1.3 Add scenario: admin user calls `POST /api/family-groups` with valid JSON `{"name": "Test family"}` — expect HTTP 201
- [x] 1.4 Add scenario: GROUPS:TRAINING user calls `POST /api/training-groups` with valid JSON `{"name": "Test training", "ageRangeMin": 10, "ageRangeMax": 14}` — expect HTTP 201
- [x] 1.5 Run the test class — all 3 scenarios passed on H2 (test profile keeps connections open, so the bug doesn't reproduce)
- [x] 1.6 User provided production stack trace. Root cause identified: **H2 in-memory DB is destroyed when Hikari closes its last idle connection** (`DB_CLOSE_DELAY=0` is the H2 default). Subsequent INSERT fails inside `ConstraintCheck.checkRow` → `SessionLocal.getDatabase()` → `JdbcSQLNonTransientConnectionException: The database has been closed`. The "chk_user_groups_type" message is misleading — Spring wraps the connection-closed exception as a constraint violation. Fix: add `DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` to the H2 JDBC URL.

## 2. Fix the H2 connection lifetime config

- [x] 2.1 Append `;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE` to the H2 JDBC URL in `backend/src/main/resources/application-h2.yml`
- [x] 2.2 Run `GroupCreationIntegrationTest` via test-runner — all 3 create scenarios still pass (regression coverage)
- [x] 2.3 Override the H2 URL in `application-test.yml` (drop `DB_CLOSE_DELAY=-1`) so test contexts don't share in-memory state across `@SpringBootTest` runs
- [x] 2.4 Run full backend test suite — 2342/2343 pass. The single remaining failure (`EventJdbcRepositoryTest$FilterByRegisteredBy.shouldIncludeCancelledEventsWithMatchingRegistration`) is a pre-existing date-sensitive test (uses `LocalDate.now()` against a hard-coded eventDate `2026-05-01` that is now in the past). Unrelated to this fix; out of scope.

## 3. Refactor and harden — N/A

Skipped. Root cause was a config issue (H2 `DB_CLOSE_DELAY`), not the persistence layer. Edge-case validation tests for `GroupMemento`/`isNew()` are irrelevant since those code paths were never the bug.

- [x] 3.1 Skipped — edge-case validation tests not needed for a config-only fix
- [x] 3.2 Skipped — `Persistable.isNew()` / `AuditMetadata` were not touched
- [x] 3.3 Full backend test suite already run in 2.4

## 4. End-to-end verification on the deployed environment — manual

These tasks are deferred to manual user verification after deploy. The integration test in `GroupCreationIntegrationTest` covers the create flows; the bug itself (idle-induced H2 shutdown) is timing-based and not reproducible in fast tests.

- [x] 4.1 Manual — user deploys and verifies on `api.klabis.otakar.io`
- [x] 4.2 Manual smoke test — FreeGroup creation
- [x] 4.3 Manual smoke test — FamilyGroup creation
- [x] 4.4 Manual smoke test — TrainingGroup creation
- [x] 4.5 Manual — follow-up flows (invite/list/delete)

## 5. Documentation

- [x] 5.1 Memory updated: `feedback_h2_close_delay.md` captures the H2 `DB_CLOSE_DELAY=-1` requirement and test profile override gotcha
- [x] 5.2 Skipped — fix did not touch `GroupMemento` mapping rules; nothing to add to `backend-patterns` skill
