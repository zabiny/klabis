# Tasks

Every behavior task is TDD. It starts with a **RED** step: write the failing tests and confirm they fail for the expected reason, not because of a compile error elsewhere or a wrong fixture. It is followed by a **GREEN** step: write the minimal implementation that makes them pass, then refactor with the tests green. Pure renames have no RED step; the existing tests are their safety net.

Run tests through the test-runner, never in parallel. Write E2E scenarios with the e2e-tester agent. After every spec edit, regenerate the OpenAPI bundle and the frontend types.

## 1. Multi-target rules and the coordinator overview rename (design D1, D3)

- [x] 1.1 RED: add `AuthorizationEvaluatorTest` / `InvocationRulesTest` cases and confirm the new cases fail. The cases:
  - MEMBER-kind authority granted over the member target of a two-target method,
  - EVENT-kind authority granted over the event target of the same method,
  - an authority whose kind has no identified target passes only when held over everything,
  - two `@TargetId` parameters of the same kind are rejected when the rules are built.
- [x] 1.2 GREEN: `InvocationRules` keeps one identified target per target kind, and `isAllowedOver` checks each authority over the target of its own `TargetType`. Adapt the `targetOf` / `targetParameterOf` callers (request-body field security, link building) to ask for a kind. Verify: the 1.1 tests and all existing `common` authorization tests pass.
- [x] 1.3 Rename `EVENTS_REGISTRATIONS_READ` → `EVENTS_REGISTRATIONS_OVERVIEW` (`EVENTS:REGISTRATIONS_OVERVIEW`) in `Authority`, `EventCoordinatorRelationshipSource`, `AuthorityToDtoConverter`, `events.yaml` descriptions and comments, and the `common.yaml` comment. Verify:
  - events module tests and `AuthorityToDtoConverter` tests pass,
  - `grep -r REGISTRATIONS_READ backend/src docs/openapi frontend/src` finds nothing.

## 2. Registration management through `EVENTS:REGISTER_MEMBER` over members (design D2, D3, D4; spec: event-registrations)

- [x] 2.1 Rename `EVENTS_REGISTRATIONS` → `EVENTS_REGISTER_MEMBER` (value `EVENTS:REGISTER_MEMBER`) in:
  - backend code and tests,
  - `V001__initial_schema.sql` and the example data,
  - the `common.yaml` wire enum and the `events.yaml` descriptions,
  - frontend `labels.ts`, `PermissionsDialog` and their tests.

  Verify:
  - backend and frontend tests pass,
  - `npm run build` passes,
  - `grep -rnE "EVENTS[:_]REGISTRATIONS([^_]|$)" backend/src docs/openapi frontend/src` finds nothing.
- [x] 2.2 RED: `AuthorityTest` asserts `EVENTS_REGISTER_MEMBER` is MEMBER-kind and in both `assignableAuthorities()` and `holdableOverTargets()`. `SelfRelationshipSource` unit tests assert `EVENTS:REGISTER_MEMBER` over self for an adult member and for a minor member, and no grant for a non-member user. Confirm both fail.
- [x] 2.3 GREEN: re-target `EVENTS_REGISTER_MEMBER` to `TargetType.MEMBER` with `GrantForm.ALL, GrantForm.SPECIFIC`. Rename `SelfProfileRelationshipSource` → `SelfRelationshipSource` and add the member self-grant. Verify: the 2.2 tests pass.
- [x] 2.4 RED: integration tests for edit, unregister, get and get-defaults (`newRegistration=true`), and confirm the new expectations fail:
  - the member themself is allowed,
  - another plain member is denied,
  - the registrations manager is allowed,
  - the event coordinator may get, but not edit or unregister,
  - get-defaults for another member is allowed for the registrations manager and returns that member's SI card,
  - the accommodation list and `registrationTime` stay coordinator- and manager-only.
- [x] 2.5 GREEN: in `events.yaml`:
  - remove `x-klabis-owner-visible` from `editRegistration` / `unregisterFromEvent`,
  - add `x-klabis-target-id: EVENT` to `RegEventIdParam`,
  - declare `getRegistration` with `x-klabis-authority: [EVENTS_REGISTER_MEMBER, EVENTS_REGISTRATIONS_OVERVIEW]`,
  - update comments and descriptions.

  In the controller:
  - delete `RegistrationReadAccess` and replace its link-producer uses with `klabisLinkTo`,
  - make the accommodation-list and sort checks an inline `isAllowed` over the event,
  - drop the "caller must be the member" check for `newRegistration=true`.

  Verify: the 2.4 tests and the events module tests pass.
- [x] 2.6 Verify the existing E2E registration scenarios (`frontend/e2e/features/event-registrations.feature`) still pass with `npm run test:e2e`.

## 3. Registering names the member (design D5)

- [x] 3.1 RED: integration tests for `POST /api/events/{eventId}/registrations/{memberId}`, and confirm they fail:
  - a member registers themself and `Location` points to their registration,
  - a plain member is denied registering another member,
  - the registrations manager registers another member,
  - a sanctioned member is still blocked,
  - the event detail `registerForEvent` affordance targets the caller's member path and is absent for a user without a member profile.
- [x] 3.2 GREEN: move `registerForEvent` in `events.yaml` to the member path:
  - `x-klabis-authority: EVENTS_REGISTER_MEMBER`, with the member path parameter as MEMBER target,
  - remove `@ActingMember`,
  - build the affordances (event detail, summary, registration defaults) with the caller's member id,
  - use the path member for `Location`.

  Verify: the 3.1 tests and the events module tests pass.
- [x] 3.3 Frontend: regenerate types; adjust test fixtures holding the old register URL first (RED where a test asserts the URL), then confirm `EventRegistrationDialog` and `EventsPage` post to the template target. Verify:
  - vitest passes,
  - `npm run build` passes,
  - the E2E "Member registers for an open event" scenario passes.

## 4. Legal guardians manage their minors' registrations (design D3; spec: legal-guardians, event-registrations)

- [x] 4.1 RED: a `LegalGuardianGroup` domain test asserts `EVENTS_REGISTER_MEMBER` is delegated. A `LegalGuardianGroupRelationshipSource` test asserts a guardian holds it over each minor and loses it once the minor leaves the group. Integration tests for a member guardian and for a non-member (EXT) guardian:
  - they get, edit, unregister and register their minor,
  - in the registrations list, the edit action is present on the child's row only,
  - they get no accommodation list and no `registrationTime`,
  - an unrelated minor is denied.

  Confirm the tests fail.
- [x] 4.2 GREEN: add `EVENTS_REGISTER_MEMBER` to `LegalGuardianGroup.delegatedAuthorities()`. Verify: the 4.1 tests pass.
- [x] 4.3 Frontend label: a RED label test for the wording "Správa přihlášek člena" of `'EVENTS:REGISTER_MEMBER'` in `labels.ts`, then GREEN. Verify: label tests and `npm run build` pass.
- [x] 4.4 E2E with the e2e-tester, using `EXT0001` and the example-data minors. Write the scenarios first and confirm they fail before 4.2 is in the running build, or record that they were written after it. The scenarios:
  - "guardian edits their child's registration from the registrations list",
  - "guardian unregisters their child".

  Verify: `npm run test:e2e` passes them. (Written before 4.2 was in the running build; both failed on the missing edit action on the child's row, then passed after 4.2.)

## 5. Viewing member data through `MEMBERS:VIEW_PROFILE` (design D2, D3, D4)

- [x] 5.1 RED: unit and integration tests, and confirm they fail:
  - `AuthorityTest`: `MEMBERS_VIEW_PROFILE` is not grantable over everything and is holdable over targets.
  - `AuthorityToDtoConverter` throws for it.
  - `SelfRelationshipSource`:
    - `VIEW_PROFILE` for an adult member, a minor member and a non-member guardian,
    - `EDIT_PROFILE` for the adult member and the non-member guardian, not for the minor.
  - Member detail and guardian profile:
    - a minor sees all their own data and has no edit action,
    - another member does not see the sensitive fields,
    - the EXT guardian reads and updates their own guardian profile,
    - a member is denied another guardian's profile.
- [x] 5.2 GREEN:
  - add `Authority.MEMBERS_VIEW_PROFILE("MEMBERS:VIEW_PROFILE", MEMBER, SPECIFIC)` and its converter mapping,
  - extend `SelfRelationshipSource`,
  - in `members.yaml`, set the 12 `MemberDetailsResponse` fields to `[MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE, MEMBERS_VIEW_PROFILE]`, `getLegalGuardian` to `[MEMBERS_MANAGE, MEMBERS_VIEW_PROFILE]` and `updateLegalGuardian` to `[MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]`, removing `x-klabis-owner-visible`.

  Verify: the 5.1 tests and the members module tests pass.
- [x] 5.3 Refactor `GuardianListAccess` to `MEMBERS_MANAGE` or `[MEMBERS_EDIT_PROFILE, MEMBERS_VIEW_PROFILE]` over any minor of the group, with the existing guardian-list tests as the safety net. Add a "minor sees their own guardians" test first if one is missing. Verify: the guardian-list tests pass.

## 6. Membership fee choice through `MEMBERS:SELECT_MEMBERSHIP_FEE_TYPE` (design D2, D3, D4; spec: membership-fees, legal-guardians)

- [x] 6.1 RED: unit and integration tests, and confirm they fail:
  - `AuthorityTest` and the converter test for `MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE`.
  - `SelfRelationshipSource`: granted to an adult member; not to a minor or a non-member.
  - The `LegalGuardianGroup` delegation includes it; `FreeGroup.delegatableAuthorities()` does not.
  - Fee page:
    - an adult member reads and chooses,
    - a minor is denied both, and their detail has no fee-page link,
    - a member guardian and an EXT guardian read and choose for their minor,
    - the administrator reads but cannot choose,
    - a free group owner delegating "Úprava údajů člena" is denied both.
- [x] 6.2 GREEN:
  - add `Authority.MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE("MEMBERS:SELECT_MEMBERSHIP_FEE_TYPE", MEMBER, SPECIFIC)` and its converter mapping,
  - add the adult self-grant and the legal guardian delegation,
  - in `membershipfees.yaml`, set `getChoice` / `getFeeSummary` / `getFeeHistory` to `[MEMBERS_MANAGE, MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE]` and `chooseFeeType` / `removeChoice` to `[MEMBERS_SELECT_MEMBERSHIP_FEE_TYPE]`, removing `x-klabis-owner-visible`.

  Verify: the 6.1 tests and the membershipfees, members and groups module tests pass.
- [x] 6.3 Frontend labels: RED label tests for `'MEMBERS:SELECT_MEMBERSHIP_FEE_TYPE'` "Volba členského příspěvku" and `'MEMBERS:VIEW_PROFILE'` "Zobrazení údajů člena" (with descriptions), then GREEN in `labels.ts`. Verify: label tests and `npm run build` pass.
- [x] 6.4 E2E with the e2e-tester, written before checking against the running build. The scenarios:
  - "minor does not see the Členské příspěvky button on their own profile",
  - "guardian chooses a fee type for their child".

  Verify: `npm run test:e2e` passes them. (The minor scenario is not automated: no minor can log in with the example data, so it stays documented in `membership-fee-choice.feature` and is covered by `MemberFeeSelfAccessIntegrationTest`. This was the user's decision. The guardian scenario already passed before 6.2 through the delegated profile-edit permission, and it still passes after 6.2.)

## 7. Remove the owner-visible mechanism (design D6)

- [ ] 7.1 RED: change the `common` security tests that assert owner-visible behavior so they assert the self-grant behavior instead, or delete them where the case no longer exists. The tests are `FieldLevelAuthorizationTest`, `AffordanceAuthorizationTest`, `InterfaceMethodSecurityAnnotationsTest` and `AuthorizationEvaluatorTest`. Confirm the build fails only on references to the removed API.
- [ ] 7.2 GREEN: delete `OwnerVisible` and remove:
  - `ownerVisible` from `InvocationRules`, `FieldRules` and `RequirementDescription`,
  - the `ownerVisible` parameter of `AuthorizationEvaluator.isAllowed`, plus the owner branch of `isAllowedOver`,
  - `RecordTargets.discoverOwnerTarget` and the multi-id collection branch,
  - the `x-klabis-owner-visible` branches in `api.mustache` / `pojo.mustache`, the comments in `pathParams.mustache`, and any validator rule for the extension in `tools/openapi-bundle`.

  Verify:
  - `grep -rn "OwnerVisible\|owner-visible" backend/src docs/openapi tools` finds nothing,
  - the `common` module tests pass.
- [ ] 7.3 Update the documentation: owner access is a self-grant, and operations may identify several targets of different kinds.
  - `docs/openapi/spec/README.md` (extension table and pairing rules),
  - `docs/developerManual/06-security.html`,
  - `backend/CLAUDE.md`,
  - the field-security references of the `backend-patterns` and `klabis-api-spec` skills,
  - an ADR note in `docs/design-decisions.md`.

  Verify: `grep -rn "OwnerVisible\|owner-visible" docs backend/CLAUDE.md .claude/skills` finds nothing.

## 8. Integration checks

- [ ] 8.1 Run the full backend suite (with modulith optimizations skipped) through the test-runner. Verify: all tests pass.
- [ ] 8.2 Run frontend vitest and `npm run build`. Verify: both pass.
- [ ] 8.3 Run the full E2E suite against the local environment. Verify: all scenarios pass, or report failures known on `main`.
- [ ] 8.4 Manual QA in the browser:
  - minor (own profile: data visible, no edit, no fee button),
  - `EXT0001` (child's registration edit and unregister; child's fee choice; own guardian profile edit),
  - event coordinator (registration detail read-only, accommodation list).

  Verify: every check behaves as the delta specs say.
