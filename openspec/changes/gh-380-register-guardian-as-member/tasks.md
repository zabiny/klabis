# Tasks

## 1. Takeover section always visible on the registration form

- [x] 1.1 Verify a backend test covers "takeover combined with a minor's date of birth is rejected with a field error on `legalGuardianUserId` and nothing is created" (RegistrationService / registration controller test); add it test-first if missing and verify it passes
- [x] 1.2 Write failing `RegistrationGuardianSections` tests: takeover section is shown with an empty date of birth and with a minor's date of birth; the chosen guardian is not cleared when the date of birth turns minor; "Zákonní zástupci" section still follows the date of birth — verify they fail
- [x] 1.3 Change `RegistrationGuardianSections` per design D5 (render takeover section whenever the template has `legalGuardianUserId`, stop clearing it for minors, enable the profile pre-fill whenever a guardian is chosen) and verify the 1.2 tests pass
- [x] 1.4 Verify the backend's `minor-cannot-take-over-guardian` error is shown next to the takeover picker after submit (frontend test of the registration form with a mocked error response)

## 2. "Registrovat jako člena" on the guardian profile

- [x] 2.1 Add the `registerAsMember` link (operation `registerMember`) to `getLegalGuardian` `x-hal-links` in `docs/openapi/spec/members.yaml`, regenerate the backend and frontend artifacts (`npm run openapi`) and verify both builds compile
- [x] 2.2 Write failing `@WebMvcTest` for the guardian profile: `registerAsMember` link present for a MEMBERS:MANAGE user, absent for the guardian viewing their own profile — verify they fail
- [x] 2.3 Add the link in `LegalGuardianDetailsPostprocessor` via `klabisLinkTo(methodOn(RegistrationApi.class).registerMember(null, null))` (design D2) and verify the 2.2 tests pass
- [x] 2.4 Add optional `onCancel` to `HalFormRequest` and call it from `HalFormsPageLayout` on "Zrušit" (design D4); verify with a `HalFormsPageLayout` test that the callback fires on cancel and is not required otherwise
- [x] 2.5 Write failing tests: `LegalGuardianDetailPage` shows "Registrovat jako člena" only when the `registerAsMember` link is present and navigates to the member list route with `{takeOverLegalGuardianId, returnTo}` state; `MembersPage` opened with that state opens the registration form with the guardian pre-selected and pre-filled, and "Zrušit" navigates to `returnTo` — verify they fail
- [x] 2.6 Implement the button (label in `localization/labels.ts`), the state handling in `MembersPage` and the initial takeover id in `RegistrationGuardianSections` (design D3) and verify the 2.5 tests pass
- [x] 2.7 Verify the regular "Registrovat člena" flow is unchanged (existing `MembersPage` / registration tests pass) and `npm run build` succeeds

## 3. Taken-over guardian gets a registration number; EXT stays as altUsername

- [ ] 3.1 Write failing `User` domain tests: `replaceUsername` moves the current login to the altUsername and sets the new username; it is refused when an altUsername already exists (design D6) — verify they fail, implement, verify they pass
- [ ] 3.2 Add `alt_user_name VARCHAR(7) UNIQUE` (nullable) to `common.users` in V001, map it in the user memento, replace `findByUsername` with `findByAnyUsername` (username or altUsername) in repository/service and all callers (design D7); verify with a repository integration test that both logins find the same user and that a duplicate altUsername is rejected
- [ ] 3.3 Write failing authorization-server test: a user with an altUsername logs in with either login and the issued tokens carry the username (`sub`, `user_name`, `preferred_username`) — verify it fails, make it pass via `KlabisUserDetailsService` using `findUserByAnyUsername`
- [ ] 3.4 Write failing test for the activation-link request with the altUsername of a pending account (link sent to the account holder's own e-mail, same response as for the username) — verify it fails, then make it pass
- [ ] 3.5 Write failing `RegistrationService` test: taking over a guardian makes the generated registration number the user's username and EXTnnnn the altUsername; a failed registration (e.g. minor takeover) leaves the guardian's login unchanged — verify it fails, implement design D8, verify it passes
- [ ] 3.6 Update the `backend-patterns` skill / backend `CLAUDE.md` if they describe `username` as the only login (common module change), and verify no stale "username = registration number only" statement remains

## 4. Integration

- [ ] 4.1 Add an E2E scenario (playwright-bdd, `e2e-tester` agent) for registering a non-member guardian from their profile, landing on the new member's detail with a new registration number, then logging in as that member with the former EXT login number; verify it passes against `./runLocalEnvironment.sh`
- [ ] 4.2 QA in the browser on http://localhost:3000 (admin ZBM9000): takeover section visible on an empty form, minor + takeover rejected, profile button → pre-filled form → cancel returns to profile, submit lands on the member detail with a registration number; the taken-over member logs in with both the registration number and the EXT login number and is shown with the registration number; guardian (EXT login) sees no button on "Můj profil"
- [ ] 4.3 Run the full backend and frontend test suites and `npm run refresh-backend-server-resources`; verify all pass
- [ ] 4.4 Add label `BackendCompleted` to GitHub issue #380 once the backend part is merged and verify the label is shown on the issue

## Workflow follow-up

- Remove stale "MEMBERS:CREATE" mentions from `members` / `legal-guardians` specs in a separate change (out of scope here).
- Archive the change after review.
