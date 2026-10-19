# Tasks

Phase 1 is groups 1–4: infrastructure delivered through membership fees. Phase 2 is groups 5–11: one vertical slice per module. Phase 3 is group 12: closing. Every task follows Red-Green-Refactor: write a failing test first, then implement.

Every new error type follows the same steps:

- the exception implements `UserFacingError`;
- the handler emits the parameters by name, plus `field` where the error concerns one field;
- the type is added to the `ErrorType` enum in `docs/openapi/spec/_shared/problem.yaml`, followed by `./gradlew openapiBundle` and `npm run openapi`;
- a Czech template is added to `labels.errorTypes`;
- a test in `labels` covers the template.

## 1. Voting deadline error in Czech at the deadline field (first end-to-end slice)

- [ ] 1.1 Add the opt-in `UserFacingError` contract to `common/exceptions` (error type derived from the kebab-cased class name, message parameters). Verify with a unit test of the derivation and an override.
- [ ] 1.2 Make `MvcExceptionHandler` emit the `/errors/{name}` type, the parameters as extension properties, and `field`, for `UserFacingError` exceptions. Keep the English `detail`. Verify with handler tests.
- [ ] 1.3 Add the generic types from design D2: `validation-failed`, `invalid-request`, `not-found`, `conflict`, `concurrent-update` and `unexpected` in `MvcExceptionHandler`, plus `unauthorized` and `forbidden` in the security entry point and access-denied handler, migrated from `https://api.klabis.example.com/errors/*`. Also add `account-inactive` in `AccountStatusValidationFilter`, switched to `application/problem+json`. Collect the generic types in `CommonErrorTypes`. Map the four bases to their status's generic type when an exception does not implement the contract. Send only a fixed generic `detail` for `IllegalArgumentException`/`IllegalStateException` and unhandled exceptions, and log the raw message at WARN. Verify with handler and security tests.
- [ ] 1.4 Add the `ErrorType` enum and the documented extension properties (`field`, `fieldErrors`, `errors`, `parameterErrors`) to `_shared/problem.yaml`, and reference the enum from `ProblemDetail.type`. Map `ErrorType` to `java.lang.String` in the common `schemaMappings` (`OpenApiModule.kt`). Regenerate the bundle and the frontend types. Verify that the backend compiles (no empty enum is generated) and that `npm run build` passes.
- [ ] 1.5 Add `ErrorTypeCatalogueTest`. It checks that every type from `UserFacingError` classes and `CommonErrorTypes` is distinct, that it is in the spec enum, and that every enum value is emitted. Verify that the test passes and fails when an enum entry is removed.
- [ ] 1.6 Give `DeadlineNotInFutureException` the types `voting-deadline-not-in-future` (start) and `voting-deadline-in-past` (change), with a `deadline` parameter. Bind the start case to the `votingDeadline` field in `MembershipFeesExceptionHandler`. Verify with controller tests asserting `type`, `deadline` and `field`.
- [ ] 1.7 Frontend: add `labels.errorTypes: Record<ErrorType, template>` with Czech texts for the generic types and the two deadline types. Add the `describeError` helper (known type → template, otherwise status-based generic text, otherwise `unexpectedError`). Verify with unit tests, and verify that `npm run build` fails when a type has no text.
- [ ] 1.8 Frontend: rework `toFormValidationError` to keep `type`, the parameters, `field`, `fieldErrors` and `errors`, and remove the `'Form validation errors'` literal. Apply field errors with Formik `setErrors`/touched after a failed submit in `HalFormsForm`. Show form-level texts and unmatched field errors (labelled by template `prompt`, never by raw key) in `ErrorDisplay`. Verify with `hateoas`, `HalFormsForm` and `ErrorDisplay` tests, including a form that is already mounted.
- [ ] 1.9 Update the E2E scenario "Voting deadline must be in the future" in `frontend/e2e/features/membership-fees.feature` to expect the Czech message at the voting deadline field. Verify that it passes locally against `./runLocalEnvironment.sh`.

## 2. Duplicate payment rule names the event type and ranking

- [ ] 2.1 Make `DuplicatePaymentRuleException` a `UserFacingError`. In `MembershipFeesExceptionHandler`, resolve the `eventTypeName` and `rankingLabel` parameters through `MembershipFeeTierOptionsPort`, and leave out a parameter that cannot be resolved, never replacing it with the ID. Add the enum value and the Czech template, with a wording for a missing name. Verify with controller tests (names present, no UUID in any parameter) and a `labels` test of both wordings.
- [ ] 2.2 Update the duplicate-rule E2E scenario to expect the Czech message with the event type name. Verify that it passes locally.

## 3. Bean validation in Czech and the remaining membership-fees errors

- [ ] 3.1 Fix the locale resolver to `cs`. Remove the `[ConstraintCode]` and `param:` prefixes from `errors`/`parameterErrors`. Verify with a controller test sent with `Accept-Language: en` that a blank membership-fee tier name gets a Czech `fieldErrors` message and the type `/errors/validation-failed`.
- [ ] 3.2 Migrate the remaining membership-fees exceptions (`ActiveCampaignExists`, `CampaignClosed`, `CampaignAlreadyProcessed`, `SnapshotFrozen`, `VotingClosed`, `PaymentRuleNotFound` and the `*NotFound` application exceptions) to typed errors with parameters by name, enum values and Czech templates. Review the membership-fees `IllegalArgumentException`s and `Assert`s that users can trigger and promote them to typed errors. Verify with handler tests and `labels` tests.
- [ ] 3.3 Switch the membership-fees pages (`MembershipFeesAdminPage`, `MembershipFeeGroupDetailPage`, `MembershipFeeTierDetailPage`, `MemberFeeChoicePage`, `FeeSelectionCampaignDetailPage`) from `error.message` to `describeError`. Verify with page tests showing Czech text for a 404.

## 4. Phase 1 verification

- [ ] 4.1 Run the backend membership-fees and common tests, the frontend unit tests, `npm run build`, and the membership-fees E2E feature. Verify that all pass, then request a code review.

## 5. Members module

- [ ] 5.1 Migrate the members exceptions (birth number, address, identity card, suspension, legal guardians) to typed errors with names instead of IDs and with `field` bindings in `MembersExceptionHandler`. Rewrite the hand-written `message=` texts in Czech. Promote user-reachable checks. Add the enum values and Czech templates. Verify with handler/controller tests and `labels` tests.
- [ ] 5.2 Switch the members pages to `describeError` and update the affected E2E texts. Verify with the frontend tests and the members E2E features.

## 6. Events module (events, event types, disciplines, registrations)

- [ ] 6.1 Migrate the exceptions in `EventsExceptionHandler`, `EventTypeExceptionHandler` and `DisciplineExceptionHandler` to typed errors with event, category and discipline names and with `field` bindings. Add the enum values and Czech templates. Verify with handler/controller tests and `labels` tests.
- [ ] 6.2 Switch the events pages and `EventRegistrationDialog` to `describeError` and update the E2E texts. Verify with the frontend tests and the events E2E features.

## 7. Calendar module

- [ ] 7.1 Rewrite the English `message=` texts on `CalendarItem` in Czech, migrate the calendar exceptions to typed errors with Czech templates, switch `CalendarPage` to `describeError`, and update the E2E texts. Verify with calendar controller tests, `labels` tests and the calendar E2E features.

## 8. Finance module

- [ ] 8.1 Migrate `OVERDRAFT_LIMIT_EXCEEDED`/`TRANSACTION_ALREADY_REVERSED` to `/errors/overdraft-limit-exceeded` and `/errors/transaction-already-reversed`, and update the assertion in `MemberAccountControllerTest`. Rebase `TransactionNotFound`, `MemberAccountNotFound`, `TransactionAlreadyReversed` and `OverdraftLimitExceeded` onto the common bases as typed errors, with the amount and the member name as parameters. Add the enum values and Czech templates. Switch `FinanceTransactionDialog` and `ReverseConfirmModal` to `describeError`. Verify with finance handler tests, `labels` tests and the finance E2E features.

## 9. Groups (free, training, family, user groups)

- [ ] 9.1 Migrate the exceptions in `GroupsExceptionHandler`, `FreeGroupExceptionHandler` and `TrainingGroupExceptionHandler` to typed errors with group and member names and with `field` bindings. Add the enum values and Czech templates, and switch the group pages to `describeError`. Verify with handler tests, `labels` tests and the group E2E features.

## 10. Common/users (permissions, password, authorization)

- [ ] 10.1 Migrate `PermissionController`'s `https://klabis.com/problems/*` types to `/errors/user-not-found`, `/errors/cannot-remove-last-permission-manager`, `/errors/admin-lockout`, `/errors/invalid-authority` and `/errors/invalid-request`, and update the three `PermissionControllerTest` assertions. Migrate `InsufficientAuthority`, `MemberProfileRequired`, the password exceptions (including `/errors/incorrect-current-password`) and rate limiting to typed errors with Czech templates. Verify with handler tests and `labels` tests.
- [ ] 10.2 Switch `ChangePasswordDialog` from English substring matching to the error type, and switch the remaining pages in this area to `describeError`. Verify with `ChangePasswordDialog` and `usePermissionsEditor` tests and the related E2E features.

## 11. Sync/ORIS and notifications

- [ ] 11.1 Migrate the user-facing exceptions in `SyncExceptionHandler` (for example `ClubKeyNotSet`, `AmbiguousSyncTarget`) and in notifications to typed errors with Czech templates, and switch `SyncOverviewPage` to `describeError`. Verify with handler tests, `labels` tests and the sync E2E features.

## 12. Closing (integration only)

- [ ] 12.1 Extend `ErrorTypeCatalogueTest` to require that every subclass of the four common exception bases implements `UserFacingError`. Verify that `grep` finds no remaining `{error.message}` or `{(error as Error).message}` alert renders in `frontend/src/pages` and `frontend/src/components`.
- [ ] 12.2 Add ADR-011 to `docs/design-decisions.md`: errors are identified by RFC 9457 `type` URIs `/errors/{name}` with named parameters; the frontend owns the Czech wording in `labels.ts`; the spec `ErrorType` enum is the shared list, enforced by `ErrorTypeCatalogueTest` and the `Record<ErrorType, …>` type; and bean validation is the exception, kept in Czech by the fixed locale. Verify that the ADR follows the existing `## ADR-NNN` format.
- [ ] 12.3 Run the full backend test suite (final verification run), `npm run build`, the frontend unit tests and the full E2E suite. Verify that all pass, apart from failures already known on `main`.

## Workflow follow-up

- Run a code review before each commit, as required by `CLAUDE.md`.
- Archive the change once all phases are merged.
