# Tasks

Prerequisites: `rebac-1-groups-owners-not-members` and `rebac-2-target-authorization` are implemented.

## 1. Self-edit through the new permission (behavior preserved)

- [x] 1.1 Add `MEMBERS_EDIT_PROFILE` (MEMBER, {SPECIFIC}) with the label "Úprava údajů člena"; verify the `Authority` tests and that the permissions dialog options are unchanged
- [x] 1.2 Write failing tests for `SelfProfileRelationshipSource`: adult gets `EDIT_PROFILE` over self, minor gets nothing, member turning 18 today gets it; verify they fail, implement, verify they pass
- [x] 1.3 Switch `updateMember` and the member detail field rules in `members.yaml` to D4 (`[MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]`, reserved request fields with `x-klabis-read-authority`); remove `OwnProfileEditRule`; verify existing members `@WebMvcTest`s (adult self-edit, minor refused, admin edit) pass unchanged
- [x] 1.4 Verify the self-edit template now carries reserved fields as read-only (`@WebMvcTest` on the member detail template) and the frontend edit form renders them read-only including gender and date of birth and omits them from the PATCH body; verify with component tests

## 2. Legal guardians edit their minors (vertical slice)

- [x] 2.1 Add `delegatedAuthorities()` to `MemberGroup` with `LegalGuardianGroup` = {EDIT_PROFILE} and `TrainingGroup` = ∅; verify domain tests
- [x] 2.2 Write failing tests for `LegalGuardianGroupRelationshipSource` (guardian over each minor, not over other minors, removed guardian loses it, minor who left loses it); implement; verify they pass
- [x] 2.3 Write failing `@WebMvcTest`s: guardian sees the minor's full detail including birth number and guardians section, gets the edit template with reserved fields read-only, saves a phone change; guardian of another child is refused; verify they fail, then pass after wiring
- [x] 2.4 Route suspended-member visibility and the birth-number access audit in `ManagementService.getMemberAndRecordView` through the evaluator (D4); write tests that a guardian sees a suspended child and that their birth-number view is audited; verify they pass
- [x] 2.5 Frontend: holder layout (edit button only, no "Členské příspěvky"/"Oprávnění"/"Ukončit členství"), guardians section without "Upravit zástupce"; verify component tests and manually on http://localhost:3000 with a guardian from example data (add one if missing in bootstrap)
- [x] 2.6 Verify a non-member EXT guardian can open and edit their child's profile (integration test)

## 3. Free group delegation (vertical slice)

- [x] 3.1 Write failing domain tests: `FreeGroup.create` with an empty and with `{EDIT_PROFILE}` set; `{MEMBERS_MANAGE}` rejected; set immutable; verify they fail, implement (including persistence in the group memento), verify they pass
- [x] 3.2 Update `groups.yaml` (D6) — `createGroup.delegatedAuthorities` with options, read-only `delegatedAuthorities` in free group detail and pending invitations; verify the bundle validates
- [x] 3.3 Write failing tests for `FreeGroupRelationshipSource` (owner over members, not over co-owners, member who left drops out, removed owner loses all, union with guardian grants); implement; verify they pass
- [x] 3.4 `@WebMvcTest`s: create with delegation, detail and invitation show the delegated set, owner gets the edit template on a member, a member does not get it on another member; verify they pass
- [x] 3.5 Frontend: opt-in checkbox in "Create group" dialog (unchecked by default), delegated permissions line on pending invitations and on the free group detail, read-only in the edit form; verify component tests and manually on http://localhost:3000

## 4. Training groups delegate nothing

- [x] 4.1 Write a test that a trainer without `MEMBERS:MANAGE` does not get the edit action on a trainee; verify it passes
- [x] 4.2 Verify training group create/edit forms offer no delegation (`@WebMvcTest` on templates)

## 5. Documentation and integration

- [x] 5.1 Update `docs/openapi/spec/README.md` examples if needed and the developer manual section on relationship sources (via `developer-manual-maintainer`); verify they mention the three sources
- [ ] 5.2 Run full backend and frontend test suites; verify all green
- [ ] 5.3 E2E on http://localhost:3000: guardian edits child's phone; minor sees own data without edit; admin removes guardian → guardian loses edit after reload; founder creates a free group with delegation, invitee sees the notice, accepts, owner edits the member, member leaves, owner loses edit
