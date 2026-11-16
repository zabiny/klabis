# Tasks

## 1. Country value object (behaviour-neutral refactoring)

- [x] 1.1 Add the Country value object with today's two-letter format rule and upper-casing (TDD: unit tests for valid, lower-case, blank, malformed codes) and verify the tests pass
- [x] 1.2 Make Nationality wrap Country (remove its own format check, keep "is Czech") and verify the Nationality and MemberCompleteness tests pass
- [x] 1.3 Make Address hold a Country and create personal information from a Nationality instead of strings; adapt REST converters/mappers, JDBC memento, ORIS projection mapper and sync adapter, `MembersImpl`, data bootstrap and tests; verify `./gradlew test` for the members module passes with no behaviour change
- [x] 1.4 Commit the refactoring on its own (no API or UI change)

## 2. Only existing country codes are accepted

- [x] 2.1 TDD: Country accepts only codes from the platform country list (`QQ` refused, `CZ`/`SK`/`FR` accepted) and verify the unit tests pass
- [x] 2.2 TDD: registering a member with nationality `QQ` and updating an address with country `QQ` are refused with no change saved (controller/integration tests), covering spec "Country Code Validation"
- [x] 2.3 TDD: the ORIS projection mapper drops an unknown nationality or address country with a WARN log (ORIS id + field name only); an unknown address country drops the whole address; verify the mapper tests pass
- [x] 2.4 Update `AddressRequest.country` description in `docs/openapi/spec/members.yaml`, regenerate bundle and frontend types (`npm run openapi`) and verify the backend build passes

## 3. Nationality reserved for administrators (#360)

- [x] 3.1 Add `x-klabis-authority: MEMBERS_MANAGE` and `x-klabis-read-authority: [MEMBERS_MANAGE, MEMBERS_EDIT_PROFILE]` to `UpdateMemberRequest.nationality`, regenerate, and verify the generated field carries the authority check
- [x] 3.2 TDD (`@WebMvcTest`): for an adult member editing themself and for a legal guardian, the `updateMember` template omits `nationality` and a submitted nationality is ignored; for an administrator it is offered and saved; verify the tests pass
- [x] 3.3 TDD (frontend): the member edit page shows nationality read-only when the template lacks it (self-edit, guardian) and the birth number field stays read-only; verify the Vitest tests pass
- [x] 3.4 E2E: scenario that an adult member sees their nationality read-only in their own edit form; verify it passes against the local environment

## 4. Country picker for nationality

- [x] 4.1 TDD: country-name helper returning the Czech name for a code and nothing for an unknown code; verify unit tests (CZ → Česko, FR → Francie, QQ → none)
- [x] 4.2 TDD: country picker component (presets in order CZ, SK, AT, PL, DE, SE, NO, DK; "Jiný stát…" reveals a code input with name or unknown-code hint; a stored non-preset value opens in "Jiný stát…" mode); verify component tests pass
- [x] 4.3 Add `x-hal-input-type: Country` to `nationality` in `RegisterMemberRequest` and `UpdateMemberRequest`, render the picker for input type `Country` in `KlabisFieldsFactory`, regenerate types; verify factory tests and that registration and admin edit show the picker
- [x] 4.4 TDD: registration form starts with Česko in nationality and the birth number field is shown; verify the registration integration test passes
- [x] 4.5 Update E2E member steps that type nationality (`frontend/e2e/steps/members.steps.ts`) to use the picker and verify the member E2E scenarios pass

## 5. Country picker for address country

- [x] 5.1 TDD: the address composite field renders its country sub-field with the country picker in registration, admin edit and self-edit; verify the Vitest tests pass
- [x] 5.2 TDD: registration form starts with Česko in address country; verify the registration integration test passes
- [x] 5.3 Update E2E member steps that type address country to use the picker, add a scenario entering a country via "Jiný stát…", and verify the member E2E scenarios pass

## 6. Czech country names on member detail

- [ ] 6.1 TDD: member detail page shows nationality and address country by Czech name (falls back to the code if unknown); verify the MemberDetailPage tests pass

## 7. Integration

- [ ] 7.1 Run the full backend test suite and verify it passes (apart from known pre-existing failures)
- [ ] 7.2 Run `npm run build` and the full Vitest suite in `frontend/` and verify both pass
- [ ] 7.3 Run the E2E suite against the local environment and verify the member scenarios pass
- [ ] 7.4 Run `npm run refresh-backend-server-resources` and verify the backend static assets are updated
- [ ] 7.5 Add label `BackendCompleted` to GitHub issues #360 and #335 and verify the labels are shown

## Workflow follow-up

- Code review before committing code changes.
- Archive the change after review and verify the archived specs.
