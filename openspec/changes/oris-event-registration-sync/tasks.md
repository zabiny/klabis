# Tasks

## 1. A member entered in ORIS appears in Klabis

- [ ] 1.1 Add `Event.syncRegistrationFromOris` (create from ORIS values, ORIS registration time, no window / SI-policy check, category by ORIS class id, `OrisCategoryNotYetSynchronisedException` retryable) test-first; verify `Event` domain tests
- [ ] 1.2 Add `UpdateOrigin` to `MemberRegisteredForEventEvent`, `RegistrationEditedEvent`, `MemberUnregisteredFromEventEvent`; verify existing listeners and tests still pass
- [ ] 1.3 Add `SyncEntityType.EVENT_REGISTRATION` (`event-registrations`) and the value to `SyncEntityTypeParam` in `sync.yaml`; regenerate; verify `openapiBundle -PopenapiCheck` and a `@WebMvcTest` on `/api/event-registrations/{uuid}:{uuid}/sync` binding the colon id
- [ ] 1.4 Add `klabis.oris.clubid` property (fail fast under `oris` profile) and `OrisEventEntries` facade with per-run cache test-first; verify unit test that N reads of one event call ORIS once
- [ ] 1.5 Implement `OrisEventRegistrationProjection` and `OrisEventRegistrationSyncAdapter` (`readLocal`, `readExternal`, `createLocal`, `applyToLocal` for present values) test-first; verify adapter unit tests and an engine integration test that `pullAndEnroll` creates the registration
- [ ] 1.6 Implement `OrisEventRegistrationDiscoveryJob` (ACTIVE ORIS events, MEMBER-paired `ClubUserID` only, subtract paired, per-item try/catch) with its cron property test-first; verify unit tests for ignored unknown members, DRAFT events skipped, failures isolated
- [ ] 1.7 Verify end-to-end with an integration test (stubbed ORIS client): discovery run brings in a club member's entry and the registrations list shows it with category, SI, rent and ORIS note

## 2. Matching an existing Klabis registration

- [ ] 2.1 Extend `syncRegistrationFromOris` / `createLocal` to adopt an existing registration (ORIS values win, registration time, internal note and shared-service choices kept) test-first; verify domain and adapter tests
- [ ] 2.2 Verify with an integration test that a member registered in Klabis and entered in ORIS ends with one registration showing the ORIS category

## 3. Kept in step, removal marked, Klabis edits ask for a decision

- [ ] 3.1 Add `EventRegistration.missingInOris` (`missing_in_oris` column in V001, memento) and absent handling in `syncRegistrationFromOris` (mark, never delete; clear on return) test-first; verify domain and repository tests
- [ ] 3.2 Map tombstones in the adapter (local absent when no registration or marked; external absent when no entry) test-first; verify adapter tests that two absent sides compare equal
- [ ] 3.3 Add `EventRegistrationsSyncListener` (markDirty on MANUAL-origin registration events, ignore SYNCHRONISATION, retire all on finish / cancel) test-first; verify listener tests
- [ ] 3.4 Verify with engine integration tests every row of design D2: ORIS change pulled, ORIS deletion marks, re-creation clears, Klabis category / ORIS note edit and Klabis unregistration conflict, conflict self-resolves after ORIS deletion, internal note edit causes nothing, OUTWARD resolution not offered
- [ ] 3.5 Add `missingInOris` to `RegistrationDto` and `RegistrationSummaryDto` (field security as `registrationTime`); regenerate; verify `@WebMvcTest` visibility for owner, coordinator, EVENTS:REGISTRATIONS and other members
- [ ] 3.6 Show "V ORISu nenalezena" on the registration detail and in the list for coordinator / EVENTS:REGISTRATIONS; verify component tests and `npm run build`

## 4. On-demand load and sync state from the registration

- [ ] 4.1 Add `POST /api/events/{eventId}/oris-registrations/discovery` (SYNC:MANAGE) running discovery for one event, and the `discoverOrisRegistrations` template on `getEvent` only for ACTIVE ORIS-paired events; verify `@WebMvcTest` for permission and event-state gating
- [ ] 4.2 Add the `syncState` link to `getRegistration` for SYNC:MANAGE when the registration is enrolled; verify `@WebMvcTest`
- [ ] 4.3 Add the "Načíst přihlášky z ORIS" action on the event detail and the sync-state link on the registration detail (reusing the existing sync state screen); verify component tests and `npm run build`

## 5. Integration

- [ ] 5.1 Document `klabis.oris.clubid` and the discovery cron in `backend/CLAUDE.md` (sync section) and add the adapter to the `backend-patterns` synchronisation-adapter reference; verify the docs name the new properties
- [ ] 5.2 QA against ORIS test data in the browser: load registrations on demand, adoption, ORIS deletion mark, Klabis edit conflict and its resolution; verify all pass
