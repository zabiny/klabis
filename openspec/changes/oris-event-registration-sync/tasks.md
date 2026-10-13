# Tasks

## 1. A member entered in ORIS appears in Klabis

- [ ] 1.1 Add `Event.syncRegistrationFromOris` (create from ORIS values, ORIS registration time, no window / SI-policy / sanction check, category by ORIS class id, `OrisCategoryNotYetSynchronisedException` retryable) test-first; verify `Event` domain tests incl. a sanctioned member
- [ ] 1.2 Add `UpdateOrigin` to `MemberRegisteredForEventEvent`, `RegistrationEditedEvent`, `MemberUnregisteredFromEventEvent`; verify existing listeners and tests still pass
- [ ] 1.3 Add `SyncEntityType.EVENT_REGISTRATION` (`event-registrations`) and the value to `SyncEntityTypeParam` in `sync.yaml` (description: not a resource root); add the `SyncPairingLookup` port (D3) with `SynchronizationPort` delegating to it; regenerate; verify `openapiBundle -PopenapiCheck` and a `@WebMvcTest` on `/api/event-registrations/{uuid}:{uuid}/sync` binding the colon id
- [ ] 1.4 Add the `OrisEventEntries` facade (club id from `oris.client.my-club-id`, TTL cache with `klabis.events.oris-entries-cache-ttl`, per-event eviction) test-first; verify unit tests that N reads of one event call ORIS once and that eviction forces a new call
- [ ] 1.5 Implement `OrisEventRegistrationProjection` and `OrisEventRegistrationSyncAdapter` (`readLocal`, `readExternal` with D2 normalisation and `CreatedDateTime` in `OrisClientProperties.timezone`, `createLocal`, `applyToLocal` for present values; `containsSensitiveData`; ORIS non-OK status and `OptimisticLockingFailureException` wrapped as retryable; unknown category marks the EVENT record dirty) test-first; verify adapter unit tests, an engine integration test that `pullAndEnroll` creates the registration and that a second pass ends NOTHING_TO_DO for SI 0, null `RentSI` and an over-long note
- [ ] 1.6 Implement `OrisEventRegistrationDiscoveryJob` (ACTIVE ORIS events, entries without `ClubUserID` skipped, MEMBER-paired `ClubUserID` only, subtract paired, per-item try/catch, no markDirty of paired registrations) with its cron property test-first; verify unit tests for ignored unknown members, null `ClubUserID`, DRAFT events skipped, failures isolated
- [ ] 1.7 Verify end-to-end with an integration test (stubbed ORIS client): discovery run brings in a club member's entry and the registrations list shows it with category, SI, rent and ORIS note

## 2. Matching an existing Klabis registration

- [ ] 2.1 Extend `syncRegistrationFromOris` / `createLocal` to adopt an existing registration (ORIS values win, registration time, internal note and shared-service choices kept) test-first; verify domain and adapter tests
- [ ] 2.2 Verify with an integration test that a member registered in Klabis and entered in ORIS ends with one registration showing the ORIS category

## 3. Kept in step, removal marked, Klabis edits ask for a decision

- [ ] 3.1 Add `EventRegistration.missingInOris` (`missing_in_oris` column in V001, memento) and absent handling in `syncRegistrationFromOris` (mark, never delete; clear on return) test-first; verify domain and repository tests
- [ ] 3.2 Map tombstones in the adapter (local absent when no registration or marked; external absent when no entry) test-first; verify adapter tests that two absent sides compare equal
- [ ] 3.3 Add `EventRegistrationsSyncListener` (markDirty on MANUAL-origin registration events, ignore SYNCHRONISATION, retire on finish / cancel via `findActiveByEntityType` filtered by the `eventId:` prefix) test-first; verify listener tests incl. retiring the record of a member who had unregistered
- [ ] 3.4 Verify with engine integration tests every row of design D2: ORIS change pulled, ORIS deletion marks, re-creation clears, Klabis category / ORIS note edit and Klabis unregistration conflict, conflict self-resolves after ORIS deletion, internal note edit causes nothing, OUTWARD resolution not offered, ACCEPT_DIVERGENCE followed by an ORIS change raises a new conflict, INWARD on an unregistration conflict re-creates the registration
- [ ] 3.5 Add `missingInOris` to `RegistrationDto` and `RegistrationSummaryDto` (field security as `registrationTime` after gh-331: `EVENTS_REGISTRATIONS`, `EVENTS_REGISTRATIONS_READ` over the hidden `eventId`); regenerate; verify `@WebMvcTest` visibility for coordinator, EVENTS:REGISTRATIONS and other members, and on the detail for the owner
- [ ] 3.6 Show "V ORISu nenalezena" on the registration detail and in the list for coordinator / EVENTS:REGISTRATIONS; verify component tests and `npm run build`

## 4. On-demand load and sync state from the registration

- [ ] 4.1 Add `POST /api/events/{eventId}/oris-registrations/discovery` (SYNC:MANAGE) running discovery for one event with the acting user and a cache eviction first, and the `discoverOrisRegistrations` template on `getEvent` only for ACTIVE ORIS-paired events; verify `@WebMvcTest` for permission and event-state gating, and a test that already-paired registrations are not passed
- [ ] 4.2 Add the ungated `sync` link to `getRegistration` when the registration is enrolled (as on events / members); verify `@WebMvcTest` that owner and coordinator get the link and the headline state only, SYNC:MANAGE the details, and a registration that is not enrolled has no link
- [ ] 4.3 Add the "Načíst přihlášky z ORIS" action on the event detail and the sync-state link on the registration detail (reusing the existing sync state screen); verify component tests and `npm run build`

## 5. Integration

- [ ] 5.1 Document the discovery cron and cache TTL in `backend/CLAUDE.md` (sync section) and add the adapter to the `backend-patterns` synchronisation-adapter reference; verify the docs name the new properties
- [ ] 5.2 QA against ORIS test data in the browser: load registrations on demand, adoption, ORIS deletion mark, Klabis edit conflict and its resolution; verify all pass
