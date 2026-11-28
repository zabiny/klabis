## Context

The proposal gives the motivation. This document records where each behaviour of the
members module stays tested after the cleanup, what must be in place before anything is
deleted, and how loss of coverage is detected. It is based on a static review of all
test classes of the module (backend domain, application, REST, persistence and ORIS
sync, frontend members pages and components, E2E features `members`, `legal-guardians`
and `member-accounts`) on commit `aa9b5d2`.

Today the suite has three recurring problems:

- **Higher levels repeat lower levels.** The same rule is asserted in domain tests,
  application tests, WebMvc slices, `@SpringBootTest` integration tests, frontend page
  tests and E2E scenarios.
- **WebMvc slices emulate relationships.** In `@MembersWebMvcTest`,
  `@WithKlabisMockUser(targetGrants = EDIT_PROFILE …)` looks the same for a guardian, a
  group owner and an adult editing themself. Neither the controller nor the
  postprocessors read the date of birth. Slice tests named after minors, guardians or
  group owners therefore pass because of the annotation, not because of production code.
  The real rules live in `SelfRelationshipSource`, `LegalGuardianGroupRelationshipSource`
  and `FreeGroupRelationshipSource`, and their own tests cover them.
- **Trivial tests.** Generated `equals`/`hashCode`/`toString` of records (25 executions
  through `ValueObjectTestBase` alone), MapStruct null guards, Spring property binding,
  one-line delegations, tests of methods that no production code calls, empty and
  `@Disabled` tests.

## Goals / Non-Goals

**Goals:**
- Remove or merge about 330 backend test methods, 120 frontend tests and 24 E2E
  scenarios with no loss of coverage, or a loss that is explicitly accepted per item.
- Remove three Spring test contexts used only by members tests.
- Put the rules that would have prevented the duplication into the testing guides.
- Add the tests for the behaviours that today have none.

**Non-Goals:**
- Changing production behaviour. Resolving the spec/code disagreements in
  [#455](https://github.com/zabiny/klabis/issues/455) and
  [#456](https://github.com/zabiny/klabis/issues/456).
- Fixing bugs that a new test reveals (D6).
- Cleaning up other modules. The rules added to the guides apply to them, the cleanup
  does not.
- Introducing a line-coverage target.

## Decisions

### D1. Yardstick

`testing-guide.md` is binding: each behaviour is tested once, at the lowest level that
can verify it; mock only boundaries; integration tests verify cooperation between
modules on the happy path; E2E verifies journeys. A test is removed when another test at
the same or a lower level asserts the same behaviour, and that other test is named in the
task or commit. A higher-level test is kept only when it verifies something no lower
level can (real transaction rollback, event delivery after commit, wiring of the ORIS
sync engine, a user journey through UI and API).

### D2. Where each behaviour stays

| Behaviour | Stays in | Removed from |
| --- | --- | --- |
| Profile edit by guardian, group owner, self | `*RelationshipSource` tests (who gets which grant), `GuardianEditsMinorApiTest` (4 tests: holder view and template, holder saves, VIEW_PROFILE-only, unrelated minor), one integration class `SelfAccessIntegrationTest` on `AccessTestFixture` incl. the birth number audit chain, one FE holder test | `GroupOwnerEditsMemberApiTest`, tautological minor/18 tests in slices, most of `NonMemberGuardianEditsChildIntegrationTest` (merged into `SelfAccessIntegrationTest`), FE holder block duplicates |
| Reserved profile fields are read-only without MEMBERS:MANAGE | one parameterized test over all six fields in `UpdateMemberApiTest` (write), template `readOnly` assertion in the merged EDIT_PROFILE holder test (read), FE readOnly test | per-field tests in `MemberControllerApiTest.SelfUpdateSecurityTests` / `UpdateMemberOwnershipTests`, guardian classes, E2E "own nationality" |
| Detail field visibility per viewer | `BirthNumberAuditMatchesSerializationApiTest` (one helper, one row per viewer kind, extended with `canEdit` and a minor row) | `MemberViewAccess` checks spread over guardian classes |
| Suspension blockers (409) and cascade | `ManagementServiceTest`, one controller test per 409 branch, FE `useSuspendMemberAction.test` and `SuspensionWarningDialog.test`, one wiring test per page, E2E "successor trainer" and "whole guardian group" journeys | 409 variants in page tests, `MemberControllerApiTest.shouldReturn409WithBothBlockersWhenBothApply`, 7 E2E blocker scenarios |
| Registration incl. guardians and takeover | `MemberTest`, `RegistrationServiceTest`, rollback tests in `RegisterMemberWithGuardiansIntegrationTest`, one controller test (201, Location, command), E2E adult and minor-with-guardian journeys | `RegisterMemberAutoProvisioningTest`, `ImportMemberRegistrationNumberConflictTest`, non-rollback cases in the integration class, 7 identical-payload controller tests, generic form validation in FE page test, E2E "minor without guardian" |
| User account provisioning on registration | `RegistrationServiceTest`, `MemberLifecycleCrossModuleTest` | `RegisterMemberAutoProvisioningTest`, finance assertions in `ImportMemberUsualConsequencesTest`, E2E "account created with member" |
| Member list, filters, search | `MemberRepositoryTest` (SQL incl. sort), controller tests (parameter pass-through, forced ACTIVE), `MembersFilterBar.test`, `membersQuery.test` | `MemberFilterCrossModuleTest`, duplicates in `MembersPage.test`, E2E search |
| Persistence round-trip | two round-trip tests in `MemberRepositoryTest` (full and minimal member, re-read from the database), `MemberMementoTest.shouldCreateMementoFromMemberWithAllFields` | save-only repository tests, 7 memento tests |
| ORIS sync wiring | `MemberOrisSyncScenarioIntegrationTest` with 3 tests (discovery incl. PENDING_ACTIVATION and no e-mail, later ORIS change, chip number conflict), unit tests of mapper, projection, adapter, job | 3 other ORIS integration classes |
| `MemberCreatedEvent` reaching training-group auto-assignment | `ImportMemberUsualConsequencesTest`, moved to the module root as `@CrossModuleTest` | — |

### D3. Preconditions before deleting

These assertions make a later deletion safe. Each is added in slice 1, before the test
that currently carries the coverage is removed:

| Add | Makes safe |
| --- | --- |
| `MemberRepositoryTest.shouldSortMembersByLastName` asserts the full lastName, firstName order (today only `isNotNull()`) | deleting `MemberFilterCrossModuleTest` |
| `MemberMementoTest.createMemberWithAllFields()` sets referee licence, suspension fields and `dataIncomplete` | merging repository round-trips and deleting memento duplicates |
| `MemberProjectionMapperTest` covers partial-address normalisation (`isPartialAddress`: postal code and country cleared) | deleting `MemberOrisImportIncompleteMemberIntegrationTest` |
| `ManagementServiceTest.shouldLoadApplyAndSaveOnSuccessfulSync` asserts that missing data is recorded through `memberCompletenessPort` | deleting `losingPhoneInOris…` and `orisSupplyingMissingBirthNumber…` |
| `MemberLifecycleCrossModuleTest` step 8 asserts `$.email`, not only `$.id` | deleting `UpdateMemberPersistenceTest` |

### D4. Detecting loss of coverage

The review was static. During implementation the guard is a measured comparison:

- Before slice 1, record a JaCoCo report of the backend test run (it already runs as
  `finalizedBy(jacocoTestReport)`) and a Vitest coverage report (`npm run test:coverage`)
  as the baseline. Keep the per-class missed lines and branches for
  `com.klabis.members` and for `src/pages/members`, `src/components/members`.
- After every slice, compare. Each line or branch that becomes missed is covered back
  before the slice is committed, unless it falls into a category the testing guide says
  not to test: code generated by MapStruct or RecordBuilder, a debugging `toString`, a
  one-line delegation or a scheduler trigger that only delegates. Such accepted lines are
  listed in the commit message. There is no percentage tolerance.
- E2E coverage is not measured. Every removed scenario is added to the feature file's
  "Not automated" header with the test that covers it, as `frontend/e2e/README.md`
  requires.

### D5. Spring contexts

Removing `MemberFilterCrossModuleTest` and `RegisterMemberAutoProvisioningTest` and
aligning `BirthNumberAuditServiceTest` with the standard
`@DataJdbcTest(includeFilters = @Filter(Repository.class))` removes three contexts.
`RegisterMemberWithGuardiansIntegrationTest` is **not** converted to
`@KlabisModuleTest`: it shares the default `@SpringBootTest` context today and a module
test would add one. It gets `@CleanupTestData`, because it commits data and never cleans
it up. `CurrentUserIntegrationTest` drops its media-type parameter (4 runs fewer).

### D6. New tests that fail

Slice 10 adds tests for behaviours that have none. If such a test fails, it has found a
bug. The bug is not fixed here, and the failing test is not committed (red or disabled).
Instead a GitHub issue in milestone `review-6` is filed with the test as the reproduction,
and the slice continues with the next test. The ORIS import of a member with unknown
nationality or gender is expected to fail this way: the mapper turns unknown values into
`null`, but `MemberSyncAdapter.buildImportMember` calls `Nationality.of(null)` and
`PersonalInformation.of` requires both values.

### D7. Dead production code

`PersonName.initials()`, `PersonName.formalName()`, `RegistrationNumber.getClubCode()`,
`getBirthYear()` and `getSequenceNumber()` have no caller in `src/main`. They are removed
together with their tests. Before removing the getters of `RegistrationNumber` the task
checks that no response is built by serializing a `RegistrationNumber` with Jackson; a
serialized getter would make the removal a change of the API.

### D8. Test data independent of the date

`MemberTestDataBuilder` defaults to an adult with a fixed date of birth. Minors and
members at an age boundary are built relative to `LocalDate.now()` (or a fixed clock
where the code takes one). The same applies to the 2015 minors in
`RegistrationServiceTest.minorCommand` and `RegisterMemberWithGuardiansIntegrationTest`.
Tests that relied on the minor default are fixed to say so explicitly; this may reveal
tests whose name does not match their data (for example
`MemberTest.updateClearingEmailOnCompleteMemberShouldFail` builds a minor without a
guardian, which is incomplete).

### D9. Slices are behaviours, not layers

Slices 3 to 9 each take one behaviour through backend, frontend and E2E at once, so the
decision in D2 is made in one place and committed together. Slice 2 is the exception: it
removes trivial tests (records, dead code, empty and disabled tests) across the module,
because they carry no behaviour to place.

## Risks / Trade-offs

- **A deleted test was the only one asserting a detail** → D4 compares coverage per class
  after each slice; a newly missed line blocks the commit.
- **Coverage stays but an assertion is lost** (a line is executed by another test that
  does not check its result) → every deletion names the test that keeps the assertion;
  the code review before each commit checks the named test really asserts it.
- **Fewer E2E scenarios catch fewer integration problems between frontend and backend**
  → the kept 16 journeys still exercise each page and each main endpoint; removed
  scenarios were checks of validation, authorization or rendering that unit tests cover.
- **Parameterized tests are harder to read when they fail** → each parameter row carries
  a readable name (`@ParameterizedTest(name = …)`, `it.each` with a title template).
- **Rewriting the test data builder touches many tests at once** → it is done in slice 1
  as its own commit, before any deletion, so failures it causes are not mixed with
  removals.

## Glossary

No new domain terms. "Slice" means one behaviour of the members module handled across
all test levels in one commit.
