# Tasks

## 1. Manager sets the SI policy of an event

- [ ] 1.1 Add `SiUsage` / `SiPolicy` (`forOrisImport()` = REQUIRED with rental, rental forced off for NOT_USED) test-first; verify unit tests pass
- [ ] 1.2 Carry `SiPolicy` through `Event` create (required) / update / `createFromOris` (`forOrisImport()`), refusing changes on FINISHED / CANCELLED events, and keep it out of the ORIS event sync projection, test-first; verify `Event` domain tests and an ORIS event sync test that a changed policy survives synchronisation
- [ ] 1.3 Add `si_usage` / `si_rental_allowed` to `events.events` in V001 and the memento; verify repository round-trip test
- [ ] 1.4 Add `siUsage` / `siRentalAllowed` to `CreateEventRequest`, `UpdateEventRequest`, `EventDto` in `events.yaml`; regenerate bundle and frontend types; verify `openapiBundle -PopenapiCheck` and `npm run openapi` succeed
- [ ] 1.5 Map the policy in create / update / detail; verify `@WebMvcTest` for manager and coordinator, that create without `siUsage` is rejected, and that the create / update templates expose both properties
- [ ] 1.6 Show the SI policy in the event form, inline edit and event detail (Czech labels); verify component tests and `npm run build`

## 2. Registration follows the SI policy

- [ ] 2.1 Make `EventRegistration.siCardNumber` optional and add `wantsToRentSi`; enforce `SiPolicy.validate` in `registerMember` / `editRegistration` test-first; verify domain tests for every row of the D2 table
- [ ] 2.2 Make `si_card_number` nullable, add `wants_to_rent_si` in V001 and the memento; review every `siCardNumber()` reader for absence; verify repository test and compile
- [ ] 2.3 Update `RegisterEventRequest`, `EditRegistrationRequest`, `RegistrationDto` (optional SI, `wantsToRentSi`); regenerate; verify bundle check and `npm run openapi`
- [ ] 2.4 Shape `registerForEvent` / `editRegistration` templates by the policy (D3); verify `@WebMvcTest` for NOT_USED, OPTIONAL, REQUIRED with and without rental
- [ ] 2.5 Render the rental tick box and optional SI field in the registration dialog and rental request on the detail; verify component tests and `npm run build`

## 3. Integration

- [ ] 3.1 QA in the browser: manager changes the policy, member registers under each policy, rental flow, policy change keeps recorded numbers; verify all pass
