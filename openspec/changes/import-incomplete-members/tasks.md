# Tasks

## 1. Close the activation-link takeover (independently releasable)

- [x] 1.1 Write a failing `PasswordSetupServiceImpl` unit test: `requestNewToken` with an e-mail that is **not** an activation contact of that registration number sends no e-mail and generates no token, but still completes normally with the same result as a matching request (design D1). Then declare the `ActivationContactVerifier` port in `common.users` and call it from `requestNewToken`.
- [x] 1.2 Write a failing test that a matching e-mail (trimmed, case-insensitive) still sends the link to the entered address, and that the rate-limit and "already active" outcomes are unchanged. Implement to pass.
- [x] 1.3 Write failing tests for the `members` adapter implementing `ActivationContactVerifier`: it matches the member's own e-mail, matches the guardian's e-mail, rejects any other address, and returns `false` for an unknown registration number or a member without any e-mail (design D2). Implement it in `members.infrastructure`.
- [x] 1.4 Write an integration test through `POST /api/auth/password-setup/request` proving a foreign e-mail gets the same response body as a matching one and receives no mail (captured via the logging e-mail service). Verify `JMoleculesArchitectureTest` and the Modulith verification still pass (no `common` → `members` dependency).

## 2. Accounts without an activation e-mail on creation

- [x] 2.1 Write a failing `RegistrationService` test: registering a minor whose only e-mail is the guardian's succeeds and creates a `PENDING_ACTIVATION` user (the NPE regression). Then make `RegistrationService` create users through the e-mail-less `User.createdUser(username)` and drop the `email` parameter from `UserService.createUser` (design D7).
- [x] 2.2 Write a failing test that registering a member sends no password-setup e-mail. Remove `User.createdUserWithEmail`, the e-mail field of `UserCreatedEvent` and the e-mail-sending branch of `UserCreatedEventHandler`, and update or delete the tests that asserted the old welcome e-mail.
- [x] 2.3 Add an "Aktivovat účet" link on the frontend `LoginPage` leading to `/password-setup/request`. Write a component test for the link, and adjust the request-page copy so it tells the user a link arrives only if the e-mail matches the one held by the club.

## 3. Derived completeness in the domain

- [ ] 3.1 Write failing unit tests for `Member.missingData()` covering each `MissingDataItem`: `EMAIL`/`PHONE` satisfied by either member or guardian, `BIRTH_NUMBER` only for CZ nationals, `GUARDIAN` only for minors (computed against today, so an 18-year-old without a guardian is complete). Then implement `MissingDataItem`, `missingData()` and `isComplete()` (design D3).
- [ ] 3.2 Refactor `Member.register` to enforce completeness through `missingData()`. The existing `Member` registration tests must still pass unchanged, with the same exception types and messages for each missing item.
- [ ] 3.3 Write failing tests for the never-worsen rule in `Member.update`: a complete member cannot lose any required detail (same errors as today); an incomplete member can be saved with unrelated changes, or with only some missing items filled in; an incomplete member cannot lose an additional item (design D5). Implement to pass, and confirm the existing `update` validation tests pass unchanged.

## 4. Materialised `data_incomplete` flag and list filter

- [ ] 4.1 Add `data_incomplete BOOLEAN NOT NULL DEFAULT FALSE` to `members.members` in V001. Write a failing JDBC repository test that saving an incomplete member stores `true`, and that saving it again after completion stores `false`. Implement it in `MemberMemento` as write-only (design D4).
- [ ] 4.2 Write a failing repository test for `MemberFilter.incompleteOnly` combined with the fulltext and status filters (AND semantics). Implement the query condition.

## 5. Import members with incomplete or unacceptable ORIS data

- [ ] 5.1 Write a failing unit test that `Member.importFromOris` creates members missing any combination of items, while still refusing a birth number for a non-CZ nationality (consistency rule). Implement the factory and route `RegistrationPort.importMember` to it; hand registration keeps using `register` (design D5).
- [ ] 5.2 Write failing `MemberProjectionMapper` tests: a malformed ORIS birth number maps to `null`, and a birth number for a non-CZ nationality maps to `null`, each with a WARN log naming the ORIS id and the field but **not** the value (design D6). Implement to pass.
- [ ] 5.3 Write a failing `MemberSyncAdapter`/import integration test (mirroring `MemberOrisSyncScenarioIntegrationTest`) that an ORIS minor without a phone or birth number is brought in, is paired for synchronisation, and is reported incomplete with `GUARDIAN`, `PHONE` and `BIRTH_NUMBER` missing. Implement any remaining wiring.
- [ ] 5.4 Write a test that importing sends no e-mail and creates a `PENDING_ACTIVATION` user, including for a member without any e-mail (specs/member-synchronization "Bringing members in sends no e-mail").

## 6. Synchronisation may leave a member incomplete

- [ ] 6.1 Write a failing unit test that `Member.syncFromOris` accepts removing the member's only phone or birth number (the member becomes incomplete), while still refusing a birth number for a non-CZ nationality. Drop the completeness rules from `syncFromOris` (design D5).
- [ ] 6.2 Write an integration test: a complete member kept in step with ORIS loses their phone in ORIS, and after a synchronisation pass the record has no phone, `data_incomplete` is `true` and the sync record is `IN_SYNC` (not `FAILED`). Also test the reverse: ORIS supplies the missing birth number and the member becomes complete.

## 7. REST API (spec-first)

- [ ] 7.1 In `docs/openapi/spec/members.yaml`, add the `incomplete` query parameter on `GET /api/members`, `dataIncomplete` on `MemberSummaryResponse` and `missingData` (enum `MissingDataItem`) on `MemberDetailsResponse`, both with `x-klabis-authority: MEMBERS_MANAGE` and not owner-visible (design "API Changes"). Regenerate the backend interfaces, the OpenAPI bundle and the frontend types.
- [ ] 7.2 Write a failing `@WebMvcTest`: a MEMBERS:MANAGE caller sees `dataIncomplete` in the list and `missingData` in the detail; a caller without it, including the member viewing their own profile, sees neither. Implement the converters.
- [ ] 7.3 Write a failing `@WebMvcTest`: `incomplete=true` filters the list for MEMBERS:MANAGE callers, is ignored for other callers, and is preserved in the collection's paging links. Implement the parameter-to-`MemberFilter` mapping.

## 8. Frontend

- [ ] 8.1 Member list: show a "Neúplné údaje" badge on rows where `dataIncomplete` is true, and add a "Jen neúplní" filter toggle (rendered only when the field or filter is available to the caller) that sets `incomplete=true`. Component tests cover both.
- [ ] 8.2 Member detail: when `missingData` is non-empty, show a warning "Chybí: …" with Czech labels for each item (add them to `localization/labels.ts`). Component tests cover a complete member, an incomplete member and a caller without the field.

## 9. Verification

- [ ] 9.1 Run the local-only `MemberOrisImportApplicationModuleTest` (not committed; it depends on a fixture with personal data) against `.do-not-commit/zbm-members.json` and confirm all current members are imported (273/273).
- [ ] 9.2 Run the full backend and frontend test suites via the test-runner agent, and `npm run build`, and confirm no regressions.
- [ ] 9.3 Verify manually on `http://localhost:3000` as `admin` with the `oris` profile: after the import, incomplete members show the badge, the filter works, the detail lists missing items, completing a member's data clears the badge, and the login page link leads to activation-link requests that send mail only to matching addresses.
