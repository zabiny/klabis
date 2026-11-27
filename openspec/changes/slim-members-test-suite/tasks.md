Every slice ends with: backend tests of the touched classes and the affected frontend
tests pass, the coverage comparison of design D4 shows no newly missed line or branch
(or only lines of removed production code), code review, one commit. Each removal names
the test that keeps the assertion (design D1, D2). Paths: `M/` =
`backend/src/test/java/com/klabis/members/`, `R/` = `M/infrastructure/restapi/`,
`FE/` = `frontend/src/`.

## 1. Safety net

- [x] 1.1 Record the coverage baseline: JaCoCo per-class missed lines/branches for `com.klabis.members`, Vitest coverage for `FE/pages/members` and `FE/components/members` (design D4); store the summary in `/mnt/ramdisk/klabis` for the later comparisons
- [x] 1.2 Make `MemberTestDataBuilder` default to an adult with a fixed date of birth; build minors relative to `now()`; fix tests that relied on the minor default to say so explicitly, incl. `MemberTest.updateClearingEmailOnCompleteMemberShouldFail` (design D8); separate commit
- [x] 1.3 Replace the fixed 2015 minor dates in `RegistrationServiceTest.minorCommand` and `RegisterMemberWithGuardiansIntegrationTest` with dates relative to `now()`
- [x] 1.4 Fix the duplicate UUID of `ADULT` and `CHILD_C` in `LegalGuardianGroupServiceTest`
- [x] 1.5 Add the five precondition assertions of design D3 (sort order, full-field memento builder, partial-address mapper test, completeness port assertion, Lifecycle step 8 `$.email`)
- [x] 1.6 Add the testing rules to `.claude/skills/backend-patterns/references/testing-guide.md` and the test-levels section of `frontend/CLAUDE.md`: what is not tested; relationship rules in `*RelationshipSource` tests, one slice test per grant shape; matrices (400 per field, 401 per endpoint, 403 per authority) parameterized and asserting field keys, not localized texts; test data independent of the date; a new Spring context only with a reason and `@DataJdbcTest` always with the standard `includeFilters`; page tests mock only the API; one E2E scenario per journey and removed spec scenarios listed under "Not automated"

## 2. Trivial and dead tests

- [x] 2.1 Delete `M/domain/ValueObjectTestBase.java` and its overrides in `AddressTest`, `BankAccountNumberTest`, `BirthNumberTest`, `EmailAddressTest`, `PhoneNumberTest`
- [x] 2.2 Delete equality/immutability/toString tests of records (`NationalityTest`, `PersonNameTest`, `PersonalInformationTest.shouldBeImmutable`, `ExpiringDocumentTest`); keep them only for hand-written `equals` (`PersonalInformation`, `RegistrationNumber`)
- [x] 2.3 Delete tests that re-run validation delegated to another value object: name null/blank in `PersonalInformationTest`, country cases in `AddressTest.CountryValidation` and `NationalityTest` except null country (kept by `PersonNameTest`, `CountryTest`)
- [x] 2.4 Check that `RegistrationNumber` is never serialized by Jackson in any response, then remove `getClubCode()`, `getBirthYear()`, `getSequenceNumber()` and `PersonName.initials()`, `formalName()` with their tests (design D7)
- [x] 2.5 Delete empty, always-true and `@Disabled` tests: `MemberMementoTest.shouldHandleClearDomainEventsWithNullMemberReference`, empty nested `MemberRepositoryTest.UpdateOperations`, `@Disabled` tests in `MemberControllerApiTest` and `MemberMappingTests`
- [x] 2.6 Reduce `R/MemberMappingTests` to about 5 tests (MapStruct null guards and single-document variants covered by the all-fields tests); fix its stale Javadoc
- [x] 2.7 Delete `MemberCreatedEventTest` (kept by `MemberTest`), `scheduler/MinorAgeOutJobTest`, `MembersOrisDiscoveryPropertiesTest.appliesOverride`, `MemberLegalGuardianGroupLinkProcessorTest` (kept by `LegalGuardianGroupLinksTests`)
- [x] 2.8 `CurrentUserIntegrationTest`: drop the media-type parameter
- [x] 2.9 Optional parameterization of null/blank/too-long cases in `AddressTest`, `BirthNumberTest`, `PhoneNumberTest`, `EmailAddressTest`, `RegistrationNumberTest`, `PhoneNumberNormalizerTest`
- [x] 2.10 Coverage comparison, code review, commit

## 3. Profile editing and access

- [x] 3.1 Delete `R/GroupOwnerEditsMemberApiTest` (kept by `GuardianEditsMinorApiTest`, `groups/GroupDelegationIntegrationTest`, `FreeGroupRelationshipSourceTest`)
- [x] 3.2 Delete tautological minor/18 slice tests: `UpdateMemberApiTest.shouldRejectMinorUpdatingOwnProfile`, `shouldAllowMemberTurned18TodayToUpdateOwnProfile`, `AdminEditMinorTests.shouldAllowAdminToUpdateMinor`; `MemberControllerApiTest.minorOwnProfileShouldNotReturnUpdateAffordance`, `justTurnedAdultOwnProfileShouldReturnUpdateAffordance`, `adminViewingMinorShouldReturnUpdateAffordance` (kept by `SelfRelationshipSourceTest`)
- [x] 3.3 Replace the per-field reserved-field tests with one parameterized PATCH test in `UpdateMemberApiTest` over first name, last name, date of birth, gender, nationality, birth number; delete `shouldRejectMemberUpdatingOwnGender/OwnNationality`, `MemberControllerApiTest.SelfUpdateSecurityTests.selfUpdateShouldOnlyPassAllowedFieldsToService`, `UpdateMemberOwnershipTests` duplicates, `GuardianEditsMinorApiTest.guardianCannotChangeReservedData`
- [x] 3.4 Merge `ownProfileShouldReturnUpdateAffordance`, `selfEditTemplateShouldMarkReservedFieldsReadOnly`, `holderShouldSeeAllDataAndEditTemplate` into one EDIT_PROFILE holder test
- [x] 3.5 `UpdateMemberApiTest`: move the 11 admin mapping tests into a new unit test of `UpdateMemberRequestMapper` reduced to "all fields map" and "undefined keeps the baseline, explicit null clears"; delete self-edit mapping duplicates (keep `shouldAllowMemberToUpdateOwnEmail`), `HateoasTests`, `shouldReturn409WhenConcurrentUpdate`, `shouldReturn404WhenMemberNotFound`, the duplicate unknown-country test; merge the three stranger-403 tests; parameterize the 10 length/format 400s; remove `andDo(print())`
- [x] 3.6 `GuardianEditsMinorApiTest`: keep 4 tests (design D2); move the `MemberViewAccess` checks into `BirthNumberAuditMatchesSerializationApiTest`, extended with `canEdit` and a minor row
- [x] 3.7 Merge `NonMemberGuardianEditsChildIntegrationTest` into `SelfAccessIntegrationTest` on `AccessTestFixture`: keep one detail-and-edit happy path verified through GET and the birth number audit chain; delete `guardianCannotChangeReservedData`, `guardianCannotEditUnrelatedMinor`, `removedGuardianLosesPermission`, `SelfAccessIntegrationTest.guardianDeniedOtherProfile`, `minorSeesOwnDataWithoutEditAction`
- [x] 3.8 Delete `R/BirthNumberAuditControllerTest`; add one `updatedBy` assertion to the kept PATCH test and one `registeredBy` assertion to the kept registration test
- [x] 3.9 Parameterize the single-authority cases in `SelfRelationshipSourceTest`; keep boundary and `TestSnapshots` parity tests
- [x] 3.10 `MemberTest`: collapse the owner-fields and admin-fields nested classes into two update tests; delete `shouldRejectClearingEmailWithoutGuardianCoverage` and `shouldAllowUnrelatedUpdateOnMinorAlreadyMissingGuardian` twins; parameterize the two "clearing" triples
- [x] 3.11 `ManagementServiceTest`: collapse `MemberUpdateTests` into one test that asserts the changed state and the guardian contacts passed to the domain; delete the identical view-access pairs
- [x] 3.12 FE `MemberDetailPage.test.tsx`: delete tests covered by `DisplayContactText.test`, `BirthNumberConditionalField.test`, `LegalGuardiansSection.test`; delete in-file duplicates (legacy compatibility, calendar feed, holder block, gender, edit-mode, CSS layout); `it.each` for badge, missing data, birth number, deactivation and the "button iff link/template" table; replace `querySelector('[name=…]')` with role/label queries
- [x] 3.13 E2E: remove "Authorized user views active member detail", "Member cannot change their own nationality", "Member cannot edit another member's profile"; move "country outside the common list" down (covered by `HalFormsCountry.test`); list them under "Not automated"
- [x] 3.14 Coverage comparison, code review, commit

## 4. Registration

- [x] 4.1 Delete `RegisterMemberAutoProvisioningTest` and `ImportMemberRegistrationNumberConflictTest`; add a unit test for the `DataIntegrityViolationException` branch of the registration-number conflict
- [x] 4.2 `RegisterMemberWithGuardiansIntegrationTest`: delete `rejectsMinorWithoutGuardian`, `rejectsUnknownGuardian`, `rejectedTakeOverLeavesLoginUnchanged`; keep the rollback tests and `rejectsTakeOverOfMember`; add `@CleanupTestData` (design D5)
- [x] 4.3 Move `ImportMemberUsualConsequencesTest` to the module root as `@CrossModuleTest` and drop its finance assertions (kept by `MemberAccountCreationIntegrationTest`)
- [x] 4.4 `RegistrationServiceTest`: `RegisterMemberMethod` 9 → 2; delete `RegisterAdult.shouldRequireOwnEmail/OwnPhone` and `shouldRegisterMinorWithNewGuardian`; use the real `RegistrationNumberGenerator` with a stubbed repository; drop `Strictness.LENIENT` and the stubs it hid
- [x] 4.5 `MemberTest`: delete the validation tests delegated to `PersonalInformation` and the contact tests twinned in `MemberErrorTypesTest` / `ContactInformationCrossValidation`
- [x] 4.6 Merge `domain/MemberErrorTypesTest` and root `MembersErrorTypesTest`; construct exceptions directly instead of re-running domain flows
- [x] 4.7 `MemberControllerApiTest` registration: merge the 7 identical-payload tests into one (201, Location, full command); parameterize the 7 bean-validation 400s asserting field keys; merge the two unknown-nationality tests; merge the three register template tests and replace fixed array positions
- [x] 4.8 FE `MemberRegistration.integration.test.tsx`: delete the generic form-validation block and duplicate guardian tests; move the e-mail format test to `HalFormsForm.test`
- [x] 4.9 E2E: remove "Minor registered without guardians is rejected" and `member-accounts` "Account is created together with a new member"; list them under "Not automated"
- [x] 4.10 Coverage comparison, code review, commit

## 5. Suspension and resume

- [ ] 5.1 `ManagementServiceTest`: delete `shouldHandleMissingUserAccountGracefully` ×2, `shouldProceedWhen…` default-stub tests, `shouldSuspendActiveMemberWithoutNote`, `shouldPublishMemberSuspendedEventOnSuccessfulSuspension`, `AlreadySuspendedTests`, `shouldHandleConcurrentSuspensionAttempts`, duplicate not-found tests, `ListMembersTests`; drop `Strictness.LENIENT`
- [ ] 5.2 `MemberTest`: delete `activeMemberShouldNotHaveSuspensionDetails`, `shouldAllowSuspensionWithAllReasonTypes`; merge `shouldRecordSuspensionTimestamp`
- [ ] 5.3 `MemberControllerApiTest` suspend/resume: merge the happy-path and authorization-pass tests; delete `shouldReturn409WithBothBlockersWhenBothApply`, `shouldExposeGroupLinkForEveryGroupType`, `shouldReturn403ForCascadeWithoutManageAuthority`
- [ ] 5.4 `MemberSuspensionGuardianInvariantIntegrationTest`: delete `cascadeKeepsSoleGuardianLink`; replace Mockito mocks in `LegalGuardianGroupSuspensionBlockersAdapterTest` and `LegalGuardianCascadeAdapterTest` with `InMemoryLegalGuardianGroupRepository`
- [ ] 5.5 `MemberLifecycleCrossModuleTest`: remove steps 3, 6.5 and 12
- [ ] 5.6 FE: keep one 409 flow in `MemberDetailPage.groups` and one wiring test in `MembersPage.groups`; delete the other 409 tests in both groups files and both page tests; extract the fake-mutate harness and copied mocks into shared fixtures under `FE/__mocks__/`; trim `SuspensionWarningDialog.test` duplicates
- [ ] 5.7 E2E: remove "Suspended member detail shows suspension info", the blocked-by variants (last owner, negative balance, both, minor's negative balance), "successor owner of a free group", "free group can only be dissolved"; fold the suspension-note check into "Admin suspends an active member"; remove `legal-guardians` "Suspended guardian is removed from a group with another guardian"; list them under "Not automated"
- [ ] 5.8 Coverage comparison, code review, commit

## 6. Member list and filters

- [ ] 6.1 Delete `MemberFilterCrossModuleTest` (precondition 1.5 sort assertion in place)
- [ ] 6.2 `MemberControllerApiTest` list: parameterize the status-filter group (MANAGE / non-MANAGE), merge the four self-link tests, merge default sort into default pagination, delete custom-pagination test, merge list field visibility into two tests, delete summary-item template duplicates and `shouldReturnEmptyArrayWhenNoActiveMembers`
- [ ] 6.3 `MemberRepositoryTest`: one table-driven test for fulltext, status, incomplete-only and bornOn filters; delete `shouldWithStatusPreserveFulltextQuery`, duplicate empty-page test, `shouldFindMemberByEmail` (after the case-insensitive variant asserts the e-mail)
- [ ] 6.4 FE `MembersPage.test.tsx` and `MembersFilterBar.test.tsx`: delete duplicates of filter bar and `membersQuery` tests, the removed PiggyBank test; `it.each` for row icons, column headers, toolbar buttons, filter clicks; extract `manageResource` fixture
- [ ] 6.5 E2E: remove "User can search members by last name"; list it under "Not automated"
- [ ] 6.6 Coverage comparison, code review, commit

## 7. Member persistence

- [ ] 7.1 `MemberRepositoryTest`: delete the 8 save-only tests; merge the 10 round-trip tests into two (full and minimal member, `findById`, recursive comparison without audit fields); delete version and auditing duplicates; use `aMember()` defaults instead of copied builder chains
- [ ] 7.2 `MemberMementoTest`: delete the 7 tests covered by `shouldCreateMementoFromMemberWithAllFields`; merge the null-member event tests
- [ ] 7.3 `BirthNumberAuditServiceTest`: standard `includeFilters`, 3 tests → 1, rename after the repository it tests
- [ ] 7.4 Delete `UpdateMemberPersistenceTest` (precondition Lifecycle `$.email` in place)
- [ ] 7.5 Coverage comparison, code review, commit

## 8. ORIS sync

- [ ] 8.1 Merge the four ORIS integration classes into `MemberOrisSyncScenarioIntegrationTest` with 3 tests (discovery incl. PENDING_ACTIVATION and no e-mail, later ORIS change, chip number conflict); one id sequence
- [ ] 8.2 Delete `noClubKeyMeansNoOrisCallAndNothingEnrolled`, `losingPhoneInOris…`, `orisSupplyingMissingBirthNumber…`, `klabisOwnedFieldSurvivesSubsequentSyncPass`, `MemberOrisImportIncompleteMemberIntegrationTest` (preconditions 1.5 in place)
- [ ] 8.3 `ManagementServiceTest`: delete `SyncMemberFromOrisTests` not-found variant
- [ ] 8.4 Parameterize `MemberProjectionMapperTest` dropped-and-logged cases and gender/SI variants
- [ ] 8.5 `MemberSyncAdapterTest`: delete `declaresEntityTypeAndSystem`, `readLocal.throwsWhenMemberNotFound`, `doesNotTouchTrainerLicence`; replace `Mockito.mock(Member.class)` with the builder
- [ ] 8.6 Delete `MemberOrisImportEnabledApiTest.runningTwiceInvokesTheJobTwice`
- [ ] 8.7 Coverage comparison, code review, commit

## 9. Permissions, legal guardian groups and finance journeys

- [ ] 9.1 FE `PermissionsDialog.test.tsx`: one `it.each` instead of the 13 per-authority tests; merge saving-state tests; delete error-text duplicates of `usePermissionsEditor.test`
- [ ] 9.2 FE `MembersPage.test.tsx`: stop stubbing `PermissionsDialog` and `usePermissionsEditor`; mock the API instead
- [ ] 9.3 Fold `LegalGuardianGroupDetailHalParityTest` into `LegalGuardianGroupControllerTest.GetGroup`; parameterize `LegalGuardianControllerTest` validation and stranger cases; delete `LegalGuardianGroupServiceTest.Queries.lists`
- [ ] 9.4 `MemberControllerApiTest`: delete the legal guardian group link duplicates of `LegalGuardianGroupLinksTests`, replace `Mockito.mock` of aggregates in the remaining training-group test; parameterize all 401s of members endpoints into one test and 403s by required authority; then split the class per nested group
- [ ] 9.5 E2E `member-accounts.feature`: remove "Member cannot view another member's account", the two "cannot record any transaction" scenarios, "Member guardian opens Finance and sees their own account first", "Non-member guardian switches to the other child's account", the two non-positive amount scenarios, "Overdraft limit does not block a finance-manager charge"; move "Member without minor children sees no account switcher" down; list them under "Not automated"
- [ ] 9.6 E2E steps: replace the `'1–1 z 1'` pagination assertion, the 20 s `toPass` poll and module-level mutable state in `members.steps.ts` where the removals leave them unused or obsolete
- [ ] 9.7 Coverage comparison, code review, commit

## 10. Missing tests

A failing new test is not committed and not fixed here; file an issue in milestone `review-6` with the test as the reproduction (design D6).

- [ ] 10.1 Domain test: `Member.register` and `Member.update` publish the birth number modification audit event when `registeredBy` / `updatedBy` is set and, for update, only when the number changes
- [ ] 10.2 `MemberRepositoryTest`: birth number is stored encrypted at rest (raw column differs from the plain value, round-trip decrypts), following `SyncRecordJdbcRepositoryTest.EncryptionAtRest`
- [ ] 10.3 FE: saving the permissions dialog sends the update and closes the dialog
- [ ] 10.4 `MemberSyncAdapterTest`: import and sync of an ORIS member with unknown nationality and with unknown gender
- [ ] 10.5 Final comparison against the 1.1 baseline: report removed test methods, lines, Spring contexts, E2E scenarios and any accepted coverage difference in the PR description
