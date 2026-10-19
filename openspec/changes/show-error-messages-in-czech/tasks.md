# Tasks

Phase 1 is groups 1–5: infrastructure delivered through membership fees. Phase 2 is groups 6–12: one vertical slice per module. Phase 3 is group 13: closing. Every task follows Red-Green-Refactor: write a failing test first, then implement.

## 1. Voting deadline error in Czech at the deadline field (first end-to-end slice)

- [ ] 1.1 Add the error contract (error code defaulting to the simple class name, message arguments) to the four `common/exceptions` bases. Verify with a unit test that a subclass reports its code and arguments and keeps its English `getMessage()`.
- [ ] 1.2 Add `messages.properties` (Czech) and configure `MessageSource` and a fixed `cs` locale resolver. Verify with a `@WebMvcTest` that a request sent with `Accept-Language: en` still gets a Czech `detail`.
- [ ] 1.3 Make `MvcExceptionHandler` resolve `detail` from `error.<code>` with arguments and set the `code` property. Verify with handler tests for a business-rule exception (Czech `detail`, `code` present).
- [ ] 1.4 Give `DeadlineNotInFutureException` separate codes for the start-campaign and change-deadline cases, add their Czech texts, and bind the start-campaign case to `votingDeadline` in `MembershipFeesExceptionHandler` (`fieldErrors`). Verify with a controller test asserting `code`, the Czech `detail` and `fieldErrors.votingDeadline`.
- [ ] 1.5 Frontend: rework `toFormValidationError` to keep `fieldErrors`, `detail`, `errors` and `code` separately, and remove the English `'Form validation errors'` literal. Verify with updated `hateoas.test.ts`.
- [ ] 1.6 Frontend: apply server `fieldErrors` to Formik fields after a failed submit in `HalFormsForm`, keeping the entered values. Show form-level texts and unmatched field errors (labelled by template `prompt`, never by raw key) in `ErrorDisplay`. Verify with `HalFormsForm`/`ErrorDisplay` tests, including the case where the form is already mounted.
- [ ] 1.7 Update the E2E scenario "Voting deadline must be in the future" in `frontend/e2e/features/membership-fees.feature` to expect the Czech message at the voting deadline field. Verify that the scenario passes locally against `./runLocalEnvironment.sh`.

## 2. Duplicate payment rule names the event type and ranking

- [ ] 2.1 Give `DuplicatePaymentRule` a Czech catalogue text with event type name and ranking label arguments. Make `MembershipFeesExceptionHandler` resolve the event type name and ranking label through `MembershipFeeTierOptionsPort`, falling back to a wording without the name, never the identifier. Verify with a controller test (name present, no UUID in `detail`) and a test of the fallback for an unknown event type.
- [ ] 2.2 Update the duplicate-rule E2E scenario to expect the Czech message with the event type name. Verify that the scenario passes locally.

## 3. Generic Czech fallback

- [ ] 3.1 Backend: add the generic catalogue texts (bad request, not found, forbidden, conflict, unexpected). Use them for exceptions without a catalogue entry (with a WARN log naming the code) and for `IllegalArgumentException`/`IllegalStateException` (raw message logged only). Verify with handler tests asserting that no English or raw text appears in `detail`.
- [ ] 3.2 Frontend: add the `describeError(error)` helper (problem `detail`, then a status-based Czech label, then `unexpectedError`) with the status labels in `labels.ts`. Switch the membership-fees pages (`MembershipFeesAdminPage`, `MembershipFeeGroupDetailPage`, `MembershipFeeTierDetailPage`, `MemberFeeChoicePage`, `FeeSelectionCampaignDetailPage`) to it. Verify with helper unit tests and page tests showing a Czech text for a 404.

## 4. Bean validation and framework errors in Czech

- [ ] 4.1 Remove the `[ConstraintCode]` and `param:` prefixes from `errors`/`parameterErrors`, and add Czech catalogue overrides for user-reachable Spring MVC errors (unreadable body, type mismatch, missing parameter). Verify with `MvcExceptionHandler` tests.
- [ ] 4.2 Verify that a generated-DTO constraint on a membership-fees request (for example a blank tier name) returns a Czech Hibernate Validator message in `fieldErrors`. Verify with a controller test sent with `Accept-Language: en`.
- [ ] 4.3 Translate the remaining membership-fees exceptions (`ActiveCampaignExists`, `CampaignClosed`, `CampaignAlreadyProcessed`, `SnapshotFrozen`, `VotingClosed`, `PaymentRuleNotFound` and the `*NotFound` application exceptions) into Czech texts without IDs. Verify with handler tests asserting `code` and Czech `detail`.

## 5. Completeness check and API documentation

- [ ] 5.1 Add the catalogue completeness test (unique codes, an `error.<code>` key for every exception implementing the contract, no stale keys, every key resolves with sample arguments), with an allow-list of the modules not yet migrated (all except membership fees). Verify that the test passes and that removing a membership-fees key makes it fail.
- [ ] 5.2 Document `code`, `fieldErrors`, `errors` and `parameterErrors` in `docs/openapi/spec/_shared/problem.yaml`, then regenerate the bundle (`./gradlew openapiBundle`) and the frontend types (`npm run openapi`). Verify that `npm run build` and the backend build pass.
- [ ] 5.3 Run the backend membership-fees tests, the frontend unit tests and the membership-fees E2E feature. Verify that all pass, then request a code review.

## 6. Members module

- [ ] 6.1 Translate members exceptions and hand-written validation messages (birth number, address, identity card, suspension, legal guardians) into Czech catalogue texts with names instead of IDs, bind field-specific errors to their request fields in `MembersExceptionHandler`, and promote form-reachable `IllegalArgumentException`s found during review to domain exceptions. Verify with handler/controller tests asserting `code` and Czech `detail`/`fieldErrors`.
- [ ] 6.2 Remove members from the completeness allow-list, switch members pages to `describeError`, and update affected E2E texts. Verify that the completeness test, the frontend tests and the members E2E features pass.

## 7. Events module (events, event types, disciplines, registrations)

- [ ] 7.1 Translate exceptions and validation messages in `EventsExceptionHandler`, `EventTypeExceptionHandler` and `DisciplineExceptionHandler` into Czech with names (event name, category, discipline) instead of IDs, and bind field-specific errors. Verify with handler/controller tests.
- [ ] 7.2 Remove events from the allow-list, switch events pages and `EventRegistrationDialog` to `describeError`, and update E2E texts. Verify that the completeness test, the frontend tests and the events E2E features pass.

## 8. Calendar module

- [ ] 8.1 Replace the English `message=` texts on `CalendarItem` and translate the calendar exceptions into Czech. Switch `CalendarPage` to `describeError`, remove calendar from the allow-list, and update E2E texts. Verify with calendar controller tests, the completeness test and the calendar E2E features.

## 9. Finance module

- [ ] 9.1 Rebase `TransactionNotFound`, `MemberAccountNotFound`, `TransactionAlreadyReversed` and `OverdraftLimitExceeded` onto the common bases with Czech texts (formatted amounts, member name). Switch `FinanceTransactionDialog` and `ReverseConfirmModal` to `describeError`. Remove finance from the allow-list. Verify with finance handler tests, the frontend tests and the finance E2E features.

## 10. Groups (free, training, family, user groups)

- [ ] 10.1 Translate the exceptions in `GroupsExceptionHandler`, `FreeGroupExceptionHandler` and `TrainingGroupExceptionHandler` into Czech with group and member names, bind field-specific errors, switch the group pages to `describeError`, and remove groups from the allow-list. Verify with handler tests, the frontend tests and the group E2E features.

## 11. Common/users (permissions, password, authorization)

- [ ] 11.1 Translate the users, permissions and authorization exceptions (`CannotRemoveLastPermissionManager`, `AdminLockout`, `InsufficientAuthority`, `MemberProfileRequired`, the password exceptions) and the local handler in `PermissionController` into Czech, and give rate limiting a Czech text. Verify with handler tests.
- [ ] 11.2 Switch `ChangePasswordDialog` from English substring matching to the error `code`, and switch the remaining pages to `describeError`. Remove common/users from the allow-list. Verify with `ChangePasswordDialog` and `usePermissionsEditor` tests and the related E2E features.

## 12. Sync/ORIS and notifications

- [ ] 12.1 Translate the user-facing exceptions in `SyncExceptionHandler` (for example `ClubKeyNotSet`, `AmbiguousSyncTarget`) and notifications into Czech, switch `SyncOverviewPage` to `describeError`, and remove both from the allow-list. Verify with handler tests and the sync E2E features.

## 13. Closing (integration only)

- [ ] 13.1 Delete the completeness allow-list, and verify that `grep` finds no remaining `{error.message}` or `{(error as Error).message}` alert renders in `frontend/src/pages` and `frontend/src/components`.
- [ ] 13.2 Add an ADR to `docs/design-decisions.md` explaining why the backend owns Czech error texts and why the frontend keys behaviour on `code`. Verify that the ADR follows the existing `## ADR-NNN` numbering after ADR-010.
- [ ] 13.3 Run the full backend test suite (final verification run), `npm run build`, the frontend unit tests, and the full E2E suite. Verify that all pass, apart from failures already known on `main`.

## Workflow follow-up

- Run a code review before each commit, as required by `CLAUDE.md`.
- Archive the change once all phases are merged.
