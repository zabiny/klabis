# Tasks

Prerequisite: `import-incomplete-members` is archived (its deltas are in the main specs) before this change is archived.

## 1. Refactor: family group parents referenced by UserId (no behaviour change)

- [ ] 1.1 Change `FamilyGroup` parents to `UserId` (children stay `MemberId`), either by giving `FamilyGroup` its own participant sets or by generalising `MemberGroup` over the owner id type (design D4). Update `FamilyGroupMemento`/schema in V001 and the family group REST mapping. Verify all existing family, training and free group tests pass unchanged and `JMoleculesArchitectureTest` + Modulith verification pass.
- [ ] 1.2 Relax exclusivity to children only: write failing domain/application tests that a person who is a parent of one family group can be designated parent of another (create and add-parent), while a member who is a child of a family group is still refused. Implement; update the family group create/add-parent UI error handling if messages change. Verify with `FamilyGroup*Test` and the family group controller tests.

## 2. Guardians as linked people — registration of a minor with one member guardian (vertical slice)

- [ ] 2.1 Write failing `Member` domain tests: `GuardianLink` list replaces `GuardianInformation`; registering a minor requires ≥1 guardian, an adult with guardians is refused, a member cannot be their own guardian, duplicates are refused; `missingData()` checks own e-mail/phone for adults only and `GUARDIAN` for minors without guardians (design D1, D7). Implement `GuardianLink`, `GuardianRelationship`, remove `GuardianInformation`, update `MemberMemento` + V001 (`members.member_guardians` table). Verify 100 % coverage of the new domain logic.
- [ ] 2.2 Update `docs/openapi/spec/members.yaml`: `GuardianInput` with `memberId` variant, `guardians` array on register request and detail response, drop `guardian` from PATCH (design API chapter). Regenerate the API and implement mapping for the `memberId` variant, including the check that the picked member has own e-mail and phone. Verify with `@WebMvcTest` for `registerMember`/`getMember` (422 for each validation rule, guardians in response with `member` link).
- [ ] 2.3 Frontend: registration form switches sections by date of birth (guardians section for minors, own contacts required for adults), repeatable guardian entry with "existing member" picker; member detail shows guardians list with link to member profile; edit form no longer shows guardian fields. Verify with component tests and a Playwright check on http://localhost:3000 registering a minor with a member guardian.

## 3. Non-member guardians with their own account

- [ ] 3.1 Write failing tests for `GuardianProfile` (name, e-mail, phone required) and for the registration application service: a `newGuardian` input creates a `GuardianProfile` and a `User` with username = normalised e-mail in PENDING_ACTIVATION and sends no e-mail; a second input with the same e-mail reuses the existing guardian (design D1, D2). Implement, add `members.guardian_profiles` to V001. Verify with unit + JDBC repository tests.
- [ ] 3.2 Extend the OpenAPI `GuardianInput.newGuardian` variant and the member detail guardian item for non-member guardians (no `member` link). Frontend: "new guardian" entry in the guardians section; when the e-mail matches an existing guardian, show that guardian's data. Verify with `@WebMvcTest` and component tests.
- [ ] 3.3 Write a failing integration test that a non-member guardian can log in with their e-mail address once activated, while a member cannot log in with their e-mail. Implement any missing wiring (username normalisation on lookup). Add the login-page hint that guardians log in with their e-mail. Verify via the integration test and a login component test.
- [ ] 3.4 Admin edits a non-member guardian's contact details (`updateGuardianProfile`): write failing tests that the change is visible on every linked minor, that changing e-mail changes the login name via a `common.users` port, and that an e-mail used by another guardian account is refused. Implement endpoint, OpenAPI entry and the edit dialog on the guardians section. Verify with unit, `@WebMvcTest` and component tests.

## 4. Activation rules: guardians self-service, minors only via "Založit účet"

- [ ] 4.1 Write failing tests for `MemberActivationContactVerifier`: minors never match; adults match only their own e-mail; guardian e-mails no longer match (design D3). Implement. Verify with the verifier unit tests and the existing `POST /api/auth/password-setup/request` integration test updated to the new rules.
- [ ] 4.2 Write failing tests that `requestNewToken` with an e-mail login name sends the link to a pending non-member guardian, applies the rate limit per login name and returns the uniform response otherwise. Rename `registrationNumber` to `loginName` in `TokenRequestRequest` (OpenAPI `common.yaml`). Update the frontend request page to accept registration number or guardian e-mail. Verify with service tests, integration test and component test.
- [ ] 4.3 Write failing tests for `startMemberAccount`: sends an activation link to the member's own e-mail when the user is pending and the member has an own e-mail; refused otherwise; never sends to a guardian e-mail; rate limited. Implement endpoint, `startAccount` template on member detail and the "Založit účet" button. Verify with application, `@WebMvcTest` (template presence conditions) and component tests.

## 5. Guardian authorisation and guardian management

- [ ] 5.1 Write failing tests for the "caller is a guardian of the member" predicate: a guardian (member or non-member) can open the minor's detail, gets the self-edit template and the `startAccount`/guardian templates; other users do not (design D6). Implement next to `@OwnerVisible`. Verify with `@WebMvcTest` security tests and an integration test logging in as a non-member guardian.
- [ ] 5.2 Write failing domain and application tests for `addMemberGuardian` / `removeMemberGuardian`: admin and guardian may add/remove; the last guardian of a minor cannot be removed; an adult's last guardian can. Implement endpoints, OpenAPI entries, `addGuardian`/`removeGuardian` templates and the add/remove UI on the guardians section. Verify with unit, `@WebMvcTest` and component tests.
- [ ] 5.3 Write a failing test that a member who turned 18 without own e-mail/phone reports `EMAIL`/`PHONE` missing while keeping guardians, and that filling own contacts completes them. Implement (should follow from 2.1). Verify with `Member` unit tests using a fixed `Clock`.

## 6. Family group follows guardians

- [ ] 6.1 Write failing tests for `MemberGuardiansChanged` publication on registration of a minor and on add/remove guardian (design D5). Implement in `members`. Verify with Modulith `Scenario`/`PublishedEvents` tests.
- [ ] 6.2 Write failing tests for the `groups` listener covering each branch of D5: create group named after the child's last name; join the single existing group of a guardian; create new group when guardians are parents of several groups; add a guardian as parent to the child's existing group; remove a parent only when they guard no other child of the group. Implement listener and the members primary port for "guardians of other children". Verify with unit tests and a Modulith integration test.
- [ ] 6.3 Family group detail shows non-member parents by name (members port resolving a `UserId` to a display name), without a member link; non-member parents can open their family group. Update OpenAPI `groups.yaml` parent item and the `{parentId}` path variable. Verify with `@WebMvcTest`, component test and a Playwright check on http://localhost:3000.

## 7. Notifications to all guardians

- [ ] 7.1 Write a failing test that e-mails addressed to a minor's guardian are sent to every guardian (resolve recipients through a members port returning all guardian e-mails). Implement for every existing sender that addresses guardians (check in-flight notification changes, e.g. `gh-89-deadline-reminder-notifications`, and align them). Verify with the e-mail service tests.

## 8. Integration and wrap-up

- [ ] 8.1 End-to-end Playwright check on http://localhost:3000: register a minor with a member guardian and a new non-member guardian; family group is created with both parents; non-member guardian activates their account via the request page and logs in with e-mail; guardian clicks "Založit účet" after adding the minor's e-mail. Verify all steps pass.
- [ ] 8.2 Run full backend (`./gradlew test`) and frontend (`npm test`, `npm run lint`, type check) suites and verify they pass; run code review agent before the final commit.
- [ ] 8.3 Add label `BackendCompleted` to GitHub issues #332, #333 and #334 once backend work is merged.
