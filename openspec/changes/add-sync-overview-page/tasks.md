# Tasks

GitHub issue: zabiny/klabis#389. Each group is an end-to-end vertical slice; follow Red-Green-Refactor (failing test first) within every task. After any change in `docs/openapi/spec/`, regenerate the bundle (`./gradlew openapiBundle` from `backend/`) and frontend types (`npm run openapi` from `frontend/`).

## 1. Attempt feed reachable from the menu (design D1, D2, D4, D5, D8)

- [x] 1.1 Add `listSyncAttempts` (`GET /api/sync/attempts`, params `includeUnchanged`, `page`, `size`) and `SyncAttemptResponse` (with `entityLabel` = external id for now) to `sync.yaml`, plus `syncOverview` root link path in `klabis.yaml`; verify `./gradlew openapiBundle -PopenapiCheck` passes
- [x] 1.2 Implement `SyncHistoryQuery.listAttempts` with its JDBC adapter (join `sync_attempt` + `sync_record`, newest first, paged, "changed nothing" excluded unless `includeUnchanged`); verify with a repository integration test covering ordering, paging, the unchanged rule (scheduled no-op hidden, manual direction-less success shown)
- [x] 1.3 Implement the controller operation (username via `UserService.findUserById`, `syncState` item link) and a root link processor adding `syncOverview` for `SYNC:MANAGE`; verify with `@WebMvcTest` (HAL-FORMS accept) covering fields, `syncState` link, 403 without `SYNC:MANAGE`, root link present/absent by authority
- [x] 1.4 Frontend: add `syncOverview` to admin rels in `useRootNavigation`, labels in `labels.ts`, and a page for `/sync/attempts` rendering the paged attempt table (`KlabisTable`) with a `SyncStatusIndicator` per row opening `SyncStatusOverlay`; verify with Vitest tests for menu placement, table columns, paging and opening the overlay

## 2. Readable entity names and entity links (design D3)

- [x] 2.1 Add the `SyncEntityDescriber` SPI to the `sync` REST layer and use it in `listSyncAttempts` (label + optional `entity` link, fallback to external id with no link); add `entity` link to `sync.yaml`; verify with a `@WebMvcTest` using stub describers, including the fallback
- [x] 2.2 Implement the members describer ("name (registration number)", link to member) in `members` `infrastructure.restapi`; verify with a unit test incl. missing member → empty, and register it in the module's `*WebMvcMockitoBeans` if needed
- [x] 2.3 Implement the events, disciplines and event-registration describers in `events` `infrastructure.restapi` (registration label "member – event" via `com.klabis.members.Members`, link to the event); verify with unit tests incl. missing entity → empty
- [x] 2.4 Frontend: render `entityLabel` as a link to the entity page when an `entity` link is present, plain text otherwise; verify with Vitest

## 3. Filtering the attempt list (spec: "The Attempt List Can Be Narrowed Down", "Attempts That Changed Nothing…")

- [ ] 3.1 Add `entityType`, `outcome`, `from`, `to` parameters to `listSyncAttempts` in `sync.yaml` and the query adapter; verify with repository integration tests for each filter and a combination
- [ ] 3.2 Pass filters through the controller; verify with `@WebMvcTest` that query parameters reach the query port and invalid enum values answer 400
- [ ] 3.3 Frontend: filter bar (entity kind, outcome, date range, "Zobrazit i pokusy bez změny" toggle) driving query parameters; verify with Vitest that each control changes the request and resets to page 1

## 4. "Vyžaduje pozornost" view (design D6)

- [ ] 4.1 Add `listSyncRecords` (`GET /api/sync/records?status=…`) and `SyncRecordSummaryResponse` to `sync.yaml`, plus the `recordsNeedingAttention` collection link on `listSyncAttempts`; verify `openapiBundle -PopenapiCheck` passes
- [ ] 4.2 Implement `SyncHistoryQuery.listRecordsIn` and the controller operation (describer label/link, `syncState` link, failure count); verify with repository test (CONFLICT/FAILED returned, RETRYING/RETIRED not) and `@WebMvcTest` incl. 403 without `SYNC:MANAGE`
- [ ] 4.3 Frontend: second tab following `recordsNeedingAttention`, rows with `SyncStatusIndicator`, empty state "nothing needs attention", list refreshed after an overlay action; verify with Vitest

## 5. Recent attempts in the synchronisation state view (design D7)

- [ ] 5.1 Add `listEntitySyncAttempts` (`GET /api/{entityType}/{id}/sync/attempts`) and the `attempts` link on `getSyncState` to `sync.yaml`; verify `openapiBundle -PopenapiCheck` passes
- [ ] 5.2 Implement `SyncHistoryQuery.listAttemptsOf` and the controller operation; add the `attempts` link in `SyncStatePostprocessor` only for `SYNC:MANAGE`; verify with repository test and `@WebMvcTest` (link present for manager, absent for plain user, 404 for unlinked entity)
- [ ] 5.3 Frontend: "Historie pokusů" section in `SyncStatusOverlay` following the `attempts` link (10 most recent, "show unchanged" toggle, empty-retention message); verify with Vitest incl. no section when the link is absent

## 6. Integration and verification

- [ ] 6.1 Run backend and frontend test suites via the test-runner agent and verify they pass
- [ ] 6.2 Run `npm run refresh-backend-server-resources` and verify the build succeeds
- [ ] 6.3 QA the page in the browser at http://localhost:3000 as ZBM9000 (overview, filters, attention view, resolve a conflict and restart a failed record from the overview, history in overlay) and as ZBM9500 (no menu entry); verify all spec scenarios behave as written
- [ ] 6.4 Run `openspec validate add-sync-overview-page --strict` and verify it passes
- [ ] 6.5 Add label `BackendCompleted` to GitHub issue #389 once backend groups 1–5 are merged

## Workflow follow-up

- Archive the change after code review and QA are complete.
