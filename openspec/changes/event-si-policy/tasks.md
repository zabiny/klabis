# Tasks

## 1. Manager sets the SI policy of an event

- [x] 1.1 Add `SiUsage` / `SiPolicy` (`default()` = REQUIRED with rental, rental forced off for NOT_USED) test-first; verify unit tests pass
- [x] 1.2 Carry `SiPolicy` through `Event` create (`CreateEvent` defaults to `default()`, so `EventsDataBootstrap` and existing tests need no change) / update / `createFromOris` (applies `default()`, `CreateEventFromOris` unchanged), refusing changes on FINISHED / CANCELLED events, and keep it out of the ORIS event sync projection, test-first; verify `Event` domain tests and an ORIS event sync test that a changed policy survives synchronisation
- [x] 1.3 Add `si_usage` / `si_rental_allowed` to `events.events` in V001 and the memento; verify repository round-trip test
- [x] 1.4 Add `siUsage` (inline enum; `type: [string, 'null']` in `UpdateEventRequest`; converters map the generated enums to `SiUsage`, no `schemaMappings`; `x-hal-input-type: SiUsage`) and `siRentalAllowed` to `CreateEventRequest`, `UpdateEventRequest`, `EventDto` in `events.yaml`; regenerate bundle and frontend types; verify `openapiBundle -PopenapiCheck` and `npm run openapi` succeed
- [x] 1.5 Map the policy in `CreateEventConverter`, `UpdateEvent.from` (explicit `null` keeps the current usage) and detail; verify `@WebMvcTest` for manager and coordinator, that create without `siUsage` is rejected, that a PATCH without `siUsage` keeps it, and that the create / update templates expose both properties
- [x] 1.6 Add `SiUsage` to `ENUM_GROUP_BY_TYPE` with Czech labels; add both fields to the create form field list in `EventsPage`, the edit form, inline edit and event detail; verify component tests and `npm run build`

## 2. Registration follows the SI policy

- [x] 2.1 Make `EventRegistration.siCardNumber` optional and add `wantsToRentSi`; enforce `SiPolicy.validate` in `registerMember` (always; values not offered are not taken) and `editRegistration` (on every edit; values not offered are kept) test-first; drop the dead `\d{6,7}` pattern on `Event.RegisterCommand`; verify domain tests for every row of the D2 table, an edit of the category only on a registration that does not fit the policy being refused, and kept values under NOT_USED / no rental
- [x] 2.2 Make `si_card_number` nullable, add `wants_to_rent_si` in V001 and the memento; handle absence in every `siCardNumber()` reader (memento, `EventRegistrationController`, `EventRegistrationService`); verify repository test and tests for an absent number
- [x] 2.3 Update `RegisterEventRequest`, `EditRegistrationRequest` (optional SI with pattern `^\d{4,8}$`, `wantsToRentSi`), `RegistrationDto` (optional SI, `wantsToRentSi`) and the shared-service property descriptions; regenerate; verify bundle check and `npm run openapi`
- [x] 2.4 Map `wantsToRentSi` in the register / edit / get-registration paths; verify `@WebMvcTest` that the templates carry both SI properties with `siCardNumber` not required, and that the domain's "required" error is returned for REQUIRED without number or rental
- [x] 2.5 Add `siUsage` / `siRentalAllowed` to `RegistrationDialogEventContext`; shape the dialog per the D3 table (hide / optional / required / required-unless-renting via the Yup schema, enforced in edit mode the same as in new mode, conditional marker in `SiCardNumberField`), strip hidden fields in `postprocessPayload`, show the rental request in the read-only detail; verify component tests for each D3 row, that a NOT_USED event does not send the prefilled profile number, and `npm run build`

## 3. Integration

- [x] 3.1 QA in the browser: manager creates an event choosing the SI usage, changes the policy, member registers under each policy, rental flow, edit of a non-SI field on a non-fitting registration, policy change keeps recorded numbers; verify all pass
