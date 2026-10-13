# Tasks

## 1. Coordinator privileges as grants over the event (refactoring, no user-visible change)

- [ ] 1.1 Add `EVENTS_EDIT` and `EVENTS_REGISTRATIONS_READ` (EVENT, SPECIFIC) and re-type `EVENTS_REGISTRATIONS` to EVENT in `Authority` test-first; verify `AuthorityTest`, that neither is in `assignableAuthorities()` nor in `FreeGroup`'s delegatable set
- [ ] 1.2 Add `EventCoordinatorRelationshipSource` (both authorities over every event the user coordinates, via `EventFilter.withCoordinator`) test-first; verify unit test and an integration test that a coordinator's snapshot holds both grants over their event and none over another event
- [ ] 1.3 Make `updateEvent` declarative (`x-klabis-authority: [EVENTS_MANAGE, EVENTS_EDIT]`, `x-klabis-target-id: EVENT` on the shared `EventIdParam`), drop the imperative check; regenerate; verify `openapiBundle -PopenapiCheck`, `@WebMvcTest` for EVENTS:MANAGE, coordinator grant and refused other member, and that `publishEvent` / `cancelEvent` still refuse a coordinator
- [ ] 1.4 Replace `isCoordinatorOrHas` in management affordances, shared-services summary, accommodation list and registrations sort with `isAllowed(…, TargetRef.event(id))`; delete `isCoordinatorOrHas`; verify the existing coordinator `@WebMvcTest`s pass unchanged (rewired to `@WithKlabisMockUser(targetGrants = …)`)
- [ ] 1.5 Re-target `RegistrationSummaryDto.registrationTime` to a hidden `eventId` (`x-klabis-target-id: EVENT`, authorities `EVENTS_REGISTRATIONS`, `EVENTS_REGISTRATIONS_READ`) and remove the hidden `coordinators`; regenerate; verify `@WebMvcTest` that coordinator and EVENTS:REGISTRATIONS see registration time and other members do not
- [ ] 1.6 Update `backend-patterns` `references/authorization.md`, ADR-010 in `docs/design-decisions.md`, `backend/CLAUDE.md` and the `updateEvent` comment in `events.yaml` to describe event-targeted grants instead of the coordinator special case; verify no remaining reference to `isCoordinatorOrHas`
- [ ] 1.7 Run the events module test suite and a browser smoke test as coordinator (edit event, accommodation list, registration-time sort); verify all pass and behaviour is unchanged

## 2. Member fills in notes when registering and sees them on "Moje přihláška"

- [ ] 2.1 Add `RegistrationNote` value object (trim, blank → none, max 255) test-first; verify unit tests for trimming, blank and over-length input pass
- [ ] 2.2 Extend `EventRegistration`, `CreateEventRegistration` and `Event.registerMember` with `internalNote` / `orisNote` test-first; verify `Event` domain tests cover registering with and without notes
- [ ] 2.3 Add `internal_note` / `oris_note` columns to `events.event_registrations` in V001 and map them in `EventRegistrationMemento`; verify repository round-trip test persists and reloads both notes
- [ ] 2.4 Add `internalNote` / `orisNote` to `RegisterEventRequest` and `RegistrationDto` in `docs/openapi/spec/events.yaml`, regenerate bundle and frontend types; verify `./gradlew openapiBundle -PopenapiCheck` and `npm run openapi` succeed
- [ ] 2.5 Map the notes in the register and get-registration path; verify `@WebMvcTest` registers with notes and reads them back for the owner, and a >255 note is rejected with a validation error
- [ ] 2.6 Render both notes in the registration form and registration detail (textarea, Czech labels in `labels.ts`); verify component tests show the fields and submit their values, and `npm run build` passes

## 3. Member and EVENTS:REGISTRATIONS edit notes

- [ ] 3.1 Extend `EditRegistrationCommand` / `Event.editRegistration` with both notes test-first; verify domain tests: change, clear, unchanged registration time, refused after deadline
- [ ] 3.2 Add both notes to `EditRegistrationRequest`, regenerate; map in the edit path; verify `@WebMvcTest` for owner and for an EVENTS:REGISTRATIONS user editing another member's notes
- [ ] 3.3 Verify in the frontend edit form (component test) that the notes are prefilled and saved, and that no edit action is offered after the deadline

## 4. Coordinator reads a registration read-only

- [ ] 4.1 Allow `EVENTS_REGISTRATIONS_READ` over the event on `getRegistration` (imperative `isAllowed` before the declarative owner/EVENTS:REGISTRATIONS check); verify `@WebMvcTest`: coordinator grant gets the SI card number and notes but no `editRegistration` / `unregisterFromEvent` template, a member without the grant is refused
- [ ] 4.2 Expose the registration link on registrations-list rows for holders of `EVENTS_REGISTRATIONS_READ` over the event; verify `@WebMvcTest` that coordinator rows carry it and other members' rows do not
- [ ] 4.3 Add a read-only "detail" row action in the registrations list for the coordinator opening the registration dialog without a submit action; verify component test and that the list itself shows no notes

## 5. Integration

- [ ] 5.1 QA the flows in the browser (member registers with notes, edits them, EVENTS:REGISTRATIONS edits, coordinator reads read-only, list shows no notes); verify all pass
- [ ] 5.2 Add label `BackendCompleted` to GitHub issue #331 once backend tasks are merged; verify the label is present on the issue
