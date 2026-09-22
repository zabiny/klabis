# Tasks

## 1. Upgrade the ORIS client

- [x] 1.1 Bump `com.dpolach.api:oris-client` from `0db715d9-SNAPSHOT` to `1.2.0` in `backend/build.gradle.kts` (this release carries `getClubUserList`); verify the project compiles and the existing ORIS tests (`OrisEventSyncAdapterTest`, `DisciplineSyncAdapterTest`) still pass unchanged — the upgrade also moves off a snapshot onto a release, so watch for any behaviour drift in the existing endpoints

## 2. Club key storage, hidden by construction

- [x] 2.1 Write a failing unit test for `OrisClubKeyPort`'s in-memory adapter asserting `isSet()` is false initially, true after `store(...)`, false again after `clear()`, and that **the port exposes no way to read the stored value back** (a compile-level guarantee — assert by the absence of a getter in the interface, not at runtime); then implement `OrisClubKeyPort` and its in-memory adapter in `members.infrastructure.orissync`
- [x] 2.2 Write a failing unit test asserting `store("")` and `store("   ")` are refused and leave any previously held key in force, then implement the validation
- [x] 2.3 Write a failing unit test for `OrisClubMembers.listClubMembers()` asserting it throws `ClubKeyNotSetException` when no key is held and **never calls `OrisApiClient`**, and that it passes the held key through to `getClubUserList` when one is set; then implement `OrisClubMembers`, its implementation and `ClubKeyNotSetException` (design.md D9)

## 3. Club key REST surface

- [x] 3.1 Author the club-key resource in `docs/openapi/spec/` per the project's spec-first workflow: `GET`/`PUT`/`DELETE` on `/api/oris/club-key`, the response carrying `isSet` only, all three gated on `SYNC:MANAGE` via the field-security extension; regenerate the API interfaces
- [x] 3.2 Write a failing `@WebMvcTest` asserting `GET` returns `isSet: false` with only a `set` affordance, `isSet: true` with only a `clear` affordance (never both, design.md D10), and that **no response ever contains the key value**; then implement the controller and its postprocessor to pass
- [x] 3.3 Write a failing test asserting the club-key link is absent from the API root for a user without `SYNC:MANAGE` and present with it, then implement the root postprocessor on `EntityModel<RootModel>` (mirroring `RootAdminLinkProcessor`)
- [x] 3.4 Write a failing test asserting `PUT` with a blank body is refused and `DELETE` on an unheld key succeeds without error (specs/oris-club-key), then wire the controller to the port to pass

## 4. Member domain gains a synchronisation command

- [x] 4.1 Write a failing unit test for `Member.syncFromOris(SyncFromOris)` asserting it updates every ORIS-owned field **and leaves `identityCard`, `drivingLicenseGroup`, `medicalCourse`, `trainerLicense`, `refereeLicense`, `dietaryRestrictions`, `guardian`, `bankAccountNumber` and the whole suspension block untouched**; then implement the `SyncFromOris` command record and the method (design.md D3)
- [x] 4.2 Write a failing unit test asserting `syncFromOris` publishes **no** `BirthNumberAccessedEvent` even when the birth number changes (unlike `Member.update`, which needs a `UserId` a synchronisation does not have — design.md D3); verify it passes
- [x] 4.3 Add `ManagementPort.syncMemberFromOris(memberId, SyncFromOris)` delegating to the aggregate; write a service test asserting it loads, applies and saves, and throws `MemberNotFoundException` for an unknown id

## 5. Member registration accepts a number issued elsewhere

- [x] 5.1 Write a failing unit test for `RegistrationPort.importMember(ImportMember)` asserting the member carries the **given** registration number and that `RegistrationNumberGenerator` is never called; then implement `ImportMember` (composing `RegisterNewMember`, design.md D4) and `importMember` in `RegistrationService`, converging both entry points onto one private path
- [x] 5.2 Write a failing test asserting `registerMember` still generates a number exactly as before (regression guard on the shared path) and that `RegisterNewMember`'s shape is unchanged; verify it passes
- [x] 5.3 Write a failing test asserting `importMember` with a registration number another member already carries is refused and creates nothing (specs/members "A registration number already in use is refused"); verify the `UNIQUE` constraint surfaces as a domain-meaningful failure rather than a raw SQL error
- [x] 5.4 Write a test asserting an imported member gets a `User`, a financial account and age-based training-group assignment identically to a hand-registered one (specs/member-synchronization "Registering brings its usual consequences")

## 6. Member projection and field mapping

- [x] 6.1 Add `SyncEntityType.MEMBER("members")` and `SyncEntityTypeParam.MEMBERS`; verify existing `SyncEntityType`-based tests still pass unchanged
- [x] 6.2 Write a failing unit test for `MemberProjection` asserting it carries **only** the ORIS-owned fields (design.md D2) and that no Klabis-owned field is expressible; then implement the record
- [x] 6.3 Write failing mapper tests covering every transformation in design.md D6 — `gender` `"M"`/`"F"`/other, `si == 0` → `null`, blank strings → `null`, birth number `RRMMDD/XXXX` passthrough, postal code with an inner space, nationality upper-casing — then implement `MemberProjectionMapper` for both directions (from ORIS `ClubMember`, from local `Member`)
- [x] 6.4 Write a failing test for telephone normalisation: a bare national number gains the country's dialling prefix, a number already starting `+` is untouched, and one that cannot be normalised confidently becomes `null` rather than a guess (design.md D6); implement to pass
- [x] 6.5 Write a test asserting the projection hashes identically for two reads of unchanged data (no phantom differences from `si`/blank-string handling)

## 7. Member synchronisation adapter

- [x] 7.1 Write a failing `MemberSyncAdapterTest` asserting `capabilities()` is `pullOnlyCreating()` **with `containsSensitiveData = true`** (design.md D13) and that `applyToExternal` throws `UnsupportedOperationException`; then implement the adapter skeleton in `members.infrastructure.orissync`
- [x] 7.2 Write a failing test for `createLocal` asserting it calls `importMember` with the ORIS registration number and returns the new member's id; implement to pass
- [x] 7.3 Write a failing test for `applyToLocal` asserting it builds a `SyncFromOris` from the projection and calls `syncMemberFromOris` — and that a member's trainer licence and guardian survive the call untouched (the regression this design exists to prevent); implement to pass
- [x] 7.4 Write a failing test for `readExternal`/`readLocal` asserting both produce a `MemberProjection` in the same shape, and that `readExternal` for an id absent from the club list fails meaningfully; implement to pass

## 8. Discovery job

- [ ] 8.1 Write a failing `MemberDiscoveryJobTest` asserting members already paired (per `SynchronizationPort.findByExternalReferences`) are skipped and only new ids reach `pullAndEnroll`; then implement the job (design.md D7)
- [ ] 8.2 Write a failing test asserting members whose ORIS membership has lapsed are never enrolled, and that a lapsed member **already** in Klabis is left entirely alone (specs/member-synchronization, design.md D5); implement to pass
- [ ] 8.3 Write a failing test asserting the job does nothing and makes **no ORIS call** when no club key is held, logging rather than surfacing a failure; implement to pass
- [ ] 8.4 Write a failing test asserting one member's failure does not stop the rest of the run and is recorded; implement the per-member try/catch to pass
- [ ] 8.5 Schedule the job on its own `klabis.members.oris-discovery-cron` property (independent of `klabis.sync.scan-cron`), externalized via an env var, gated on the `oris` profile via `@OrisIntegrationComponent`; verify it is registered via a Spring context test

## 9. Manual import trigger

- [ ] 9.1 Author `POST /api/members/oris-import` in `docs/openapi/spec/`, gated on `SYNC:MANAGE`; regenerate the API interfaces
- [ ] 9.2 Write a failing `@WebMvcTest` asserting the `importFromOris` affordance is present on `GET /api/members` only when the caller holds `SYNC:MANAGE` **and** a club key is held, and absent otherwise (specs/member-synchronization, design.md D11); implement the postprocessor to pass
- [ ] 9.3 Write a failing test asserting the endpoint runs the same discovery work as the scheduled job and brings nobody in twice when run again; implement the controller to pass

## 10. Member exposes its synchronisation state

- [ ] 10.1 Write a failing test asserting a member brought in from ORIS carries a `sync` link and a hand-registered member carries **none** (specs/member-synchronization "A member not linked to ORIS", design.md D12); implement via `synchronizationPort.findByTarget`, mirroring `EventController`
- [ ] 10.2 Write a test asserting the linked sync-state resource shows the member's differing fields to a `SYNC:MANAGE` holder and nothing to a user without it (the engine's existing rules, verified end-to-end for members)

## 11. Sample data stands down when ORIS is active

- [ ] 11.1 Change `MembersDataBootstrap`, `TrainingGroupDataBootstrap`, `EventsDataBootstrap` and `MembershipFeeTiersDataBootstrap` from `@Profile("example-data")` to `@Profile({"example-data", "!oris"})` (design.md D14); write a context test asserting the initialisers are absent when both profiles are active and present with `example-data` alone
- [ ] 11.2 Update the profile table and the "Clean database (ORIS sync scenario)" note in `backend/CLAUDE.md` to state that demo data no longer loads while `oris` is active — including that the documented default profile set now yields an empty database

## 12. End-to-end verification

- [ ] 12.1 Write an integration test (mirroring `OrisEventSyncScenarioIntegrationTest`) proving the full path: a club key is supplied, discovery brings in a valid ORIS member, the member appears with the ORIS registration number, and a later ORIS change reaches the member record
- [ ] 12.2 Write an integration test proving a Klabis-only field edited by an administrator survives a subsequent sync pass untouched, and that a chip number entered in Klabis while ORIS holds none becomes a conflict rather than being cleared (design.md D6)
- [ ] 12.3 Run the full backend test suite via the `test-runner` agent and confirm no regression in `members`, `events` or `sync`
- [ ] 12.4 Verify the club-key admin screen and the member list's import action against a running instance per root `CLAUDE.md` (frontend on `http://localhost:3000`), confirming the affordances appear and disappear with the key's state
