# Tasks

## 1. Coordinator privileges as grants over the event (refactoring + coordinators rule)

- [x] 1.1 Add `EVENTS_EDIT` and `EVENTS_REGISTRATIONS_READ` (EVENT, SPECIFIC), re-type `EVENTS_REGISTRATIONS` to EVENT and rename `delegatable()` to `holdableOverTargets()` in `Authority` test-first; verify `AuthorityTest` (updated expectations), that neither new authority is in `assignableAuthorities()` nor in `FreeGroup`'s delegatable set
- [x] 1.2 Add an id-only "events coordinated by member" method to `EventRepository` (backed by `findIdsByCoordinator`) and `EventCoordinatorRelationshipSource` test-first; verify unit test and an integration test that a coordinator's snapshot holds both grants over their event and none over another event
- [x] 1.3 Make `updateEvent` declarative (`x-klabis-authority: [EVENTS_MANAGE, EVENTS_EDIT]`, `x-klabis-target-id: EVENT` on the shared `EventIdParam`), drop the imperative check; regenerate; verify `openapiBundle -PopenapiCheck`, `@WebMvcTest` for EVENTS:MANAGE, coordinator grant and refused other member, and that `publishEvent` / `cancelEvent` still refuse a coordinator
- [x] 1.4 Replace `isCoordinatorOrHas` in management affordances, shared-services summary, accommodation list access, the `accommodation-list` link and the registrations sort with `isAllowed(…, TargetRef.event(id), false)`; delete `isCoordinatorOrHas`; verify the coordinator tests in `EventControllerTest` / `EventRegistrationControllerTest` keep their expectations after rewiring their fixtures from `withCoordinator` to `@WithKlabisMockUser(targetGrants = …)`
- [x] 1.5 Re-target `RegistrationSummaryDto.registrationTime` to a hidden `eventId` (`x-klabis-target-id: EVENT`, authorities `EVENTS_REGISTRATIONS`, `EVENTS_REGISTRATIONS_READ`, owner-visible dropped) and remove the hidden `coordinators`; regenerate bundle and frontend types (`npm run openapi`); verify a positive `@WebMvcTest` that coordinator and EVENTS:REGISTRATIONS see registration time and other members do not
- [x] 1.6 Add `x-klabis-authority: EVENTS_MANAGE` + `x-klabis-read-authority: [EVENTS_READ]` to `UpdateEventRequest.coordinators`; verify `@WebMvcTest` that the coordinator's update template marks coordinators read-only, a coordinator's change of coordinators is refused while other fields save, and EVENTS:MANAGE changes them; verify the frontend edit form renders the field read-only
- [x] 1.7 Update docs and stale texts: `backend-patterns` `references/authorization.md`, ADR-010 in `docs/design-decisions.md`, `backend/CLAUDE.md`, `events.yaml` comments (`updateEvent`, `getAccommodationList`, `RegistrationSummaryDto` / `registrationTime` descriptions), a comment on the `Authority` enum in `common.yaml`; verify no remaining reference to `isCoordinatorOrHas` / `isCoordinatorOrHasRegistrationsAuthority`
- [x] 1.8 Run the events module test suite and a browser smoke test as coordinator (edit event with read-only coordinators, accommodation list, registration-time sort); verify all pass

## 2. Registration detail access and read-only dialog

- [x] 2.1 Remove `x-klabis-authority` / `x-klabis-owner-visible` from `getRegistration` (spec comment pointing to design D3); add `RegistrationReadAccess.canRead` (owner, EVENTS:REGISTRATIONS, or `EVENTS_REGISTRATIONS_READ` over the event) and enforce it in `getRegistration`; verify `@WebMvcTest`: owner, EVENTS:REGISTRATIONS and coordinator grant get the detail (coordinator incl. SI number, without `editRegistration` / `unregisterFromEvent` templates), any other member is refused
- [x] 2.2 Guard every link to `getRegistration` with `canRead` — registrations-list rows, accommodation list items, the event's registration links; verify `@WebMvcTest` that coordinator rows carry the link and other members' rows do not, and that no producer offers it to an unauthorised caller
- [x] 2.3 Add the read-only mode to `EventRegistrationDialog` (existing registration without register / edit template → data without inputs or submit; new registration without register template → error as today) and a "Detail" row action for rows with the registration link but no edit template; verify component tests for coordinator, member after deadline, and the closed-registrations error
- [x] 2.4 QA in the browser: coordinator and member-after-deadline open the read-only detail; verify both pass

## 3. Member fills in notes when registering and sees them on the registration

- [x] 3.1 Add `RegistrationNote` value object (trim, blank → none, max 255) test-first; verify unit tests for trimming, blank and over-length input pass
- [x] 3.2 Extend `EventRegistration`, `CreateEventRegistration`, `Event.RegisterCommand`, `Event.registerMember` and `EventRegistrationService` with `internalNote` / `orisNote` test-first, keeping notes out of `toString()` and domain event payloads; verify `Event` domain tests cover registering with and without notes
- [x] 3.3 Add `internal_note` / `oris_note` columns to `events.event_registrations` in V001 and map them in `EventRegistrationMemento`; verify repository round-trip test persists and reloads both notes
- [x] 3.4 Add `internalNote` / `orisNote` (maxLength 255, `x-hal-input-type: textarea`) to `RegisterEventRequest` and `RegistrationDto` in `docs/openapi/spec/events.yaml`, regenerate bundle and frontend types; verify `./gradlew openapiBundle -PopenapiCheck` and `npm run openapi` succeed
- [x] 3.5 Map the notes in the register and get-registration path; verify `@WebMvcTest` registers with notes and reads them back for the owner, and a >255 note is rejected with a validation error
- [x] 3.6 Add `renderField` calls for both notes to the dialog's `customLayout` (form and read-only view) with Czech labels in `labels.ts`; verify component tests show the fields, submit their values and show them read-only, and `npm run build` passes

## 4. Member and EVENTS:REGISTRATIONS edit notes

- [x] 4.1 Extend `EditRegistrationCommand` / `Event.editRegistration` with both notes test-first; verify domain tests: change, clear, unchanged registration time, refused after deadline
- [x] 4.2 Add both notes to `EditRegistrationRequest`, regenerate; map in the edit path; verify `@WebMvcTest` for owner and for an EVENTS:REGISTRATIONS user editing another member's notes
- [x] 4.3 Verify in the frontend edit form (component test) that the notes are prefilled and saved, and that after the deadline the registration opens read-only with the notes

## 5. Integration

- [x] 5.1 QA the flows in the browser (member registers with notes, edits them, EVENTS:REGISTRATIONS edits, coordinator reads read-only, member reads after deadline, list shows no notes); verify all pass
- [x] 5.2 Add label `BackendCompleted` to GitHub issue #331 once backend tasks are merged; verify the label is present on the issue
