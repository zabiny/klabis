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

## 3. Refactor and harden

- [ ] 3.1 Add edge-case scenarios to `GroupCreationIntegrationTest`: empty name → 400, name longer than 200 chars → 400, missing required field → 400 — for each of the 3 group types
- [ ] 3.2 If the fix touched `Persistable.isNew()` or `AuditMetadata` initialization, add a unit test on `GroupMemento` directly to assert isNew is `true` for a freshly created aggregate before save
- [ ] 3.3 Re-run the full backend test suite via test-runner

## 4. End-to-end verification on the deployed environment

- [ ] 4.1 Deploy the change to `https://api.klabis.otakar.io`
- [ ] 4.2 Browser test (admin user): `/groups` → "Vytvořit skupinu" → name "Smoke test free" → Odeslat → expect group appears in "Moje skupiny", no HTTP 500 dialog
- [ ] 4.3 Browser test (admin user): `/family-groups` → create new family group → expect group appears in list, no HTTP 500 dialog
- [ ] 4.4 Browser test (admin user): `/training-groups` → create new training group with valid age range → expect group appears in list, no HTTP 500 dialog
- [ ] 4.5 Confirm follow-up flows still work: invite a member to the newly created free group, list groups, delete one of the test groups

## 5. Documentation

- [ ] 5.1 Update memory `project_user_groups_persistence.md` with the actual root cause and the fix shape (so future sessions don't waste time on the same hypothesis)
- [ ] 5.2 If the fix changed `GroupMemento` mapping rules, add a one-line note to `backend-patterns` skill describing the gotcha (without expanding the skill into a tutorial)
