## 1. Preparation

- [x] 1.1 Record baseline per module (`events`, `finance`, `membershipfees`, `groups`, `calendar`, `sync`, `oris`, `common`): test counts, Spring contexts and test time using the spring-test-profiler skill with `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true`
- [x] 1.2 Re-verify the findings in design D4 in code (which REST adapters inject non-primary beans, which mocks exist) and correct the table if needed
- [x] 1.3 Inventory Optional / profile-gated beans injected in REST adapters of the modules above (e.g. `OrisEventImportPort`) and decide per bean: feature flag (local mock) or required dependency
- [x] 1.4 Resolve open questions from design (`OrisClubKeyPort` primary port name, `AccommodationListCsvRenderer` loading, `common` test classification)

## 2. Module `groups`

- [x] 2.1 Add `@GroupsWebMvcTest` (`@ModuleSlicing` STANDALONE, composes `@GroupsWebMvcMockitoBeans` and `@CommonWebMvcMockitoBeans`); migrate `FreeGroupControllerTest` and `TrainingGroupControllerTest`, merge related link-processor/postprocessor tests as `@Nested` groups
- [x] 2.2 Write 403 tests so that port returns are stubbed (controller body must not fail with NPE if the authorization annotation were missing; design D7)
- [x] 2.3 Run `groups` tests, review, commit

## 3. Module `calendar`

- [x] 3.1 Add `@CalendarWebMvcTest`; migrate `CalendarControllerTest` and merge `CalendarRootPostprocessorTest`, `IcalTokenMemberDetailLinkProcessorTest` as controller-level expectations; add unit tests for link-processor branches unreachable from a response
- [x] 3.2 Resolve the package-private `@Import(CalendarInfrastructureConfiguration)` coupling in `CalendarWebMvcMockitoBeans`
- [x] 3.3 Run `calendar` tests, review, commit

## 4. Module `sync`

- [ ] 4.1 Add a primary port over `SyncProjectionFieldReader`; `SynchronizationController` and `SyncStateResponseConverter` use it (test first, no behavior change)
- [ ] 4.2 Update `SyncWebMvcMockitoBeans` and add `@SyncWebMvcTest`; migrate `SynchronizationControllerTest` and merge postprocessor tests
- [ ] 4.3 Run `sync` tests plus `members` tests (they compose the sync mocks), review, commit

## 5. Module `finance`

- [ ] 5.1 Extend the finance primary port with what `MemberAccountController` reads from `MemberAccountRepository` (unit test first); controller no longer injects the repository
- [ ] 5.2 Resolve `FinanceAccountLinkSupport` (real helper in the slice or primary port); remove its mock
- [ ] 5.3 Add `@FinanceWebMvcTest` and `FinanceWebMvcMockitoBeans`; migrate `MemberAccountControllerTest`, merge `AccountMemberDetailLinkProcessorTest`, `AccountRootLinkProcessorTest`, `AccountMemberSummaryLinkProcessorTest`
- [ ] 5.4 Run `finance` tests, review, commit

## 6. Module `membershipfees`

- [ ] 6.1 Provide primary ports for `EventTypeOptionsPort` and `RankingOptionsPort` consumed by `MembershipFeeTierController` (unit test first); remove their mocks from `WithPostprocessors`
- [ ] 6.2 Add `@MembershipFeesWebMvcTest`; migrate `FeeSelectionCampaignControllerTest`, `MemberFeeSummaryControllerTest`, `MemberFeeChoiceControllerTest`, `MembershipFeeGroupControllerTest`, `MembershipFeeTierControllerTest`; merge `MemberFeeSummaryLinkProcessorTest`
- [ ] 6.3 Run `membershipfees` tests, review, commit

## 7. Module `events`

- [ ] 7.1 Resolve `AccommodationListCsvRenderer` mock; update `EventsWebMvcMockitoBeans` (no `OrisEventImportPort`)
- [ ] 7.2 Add `@EventsWebMvcTest`; migrate `EventControllerTest`, `EventRegistrationControllerTest`, `EventTypeControllerTest`, `CategoryPresetControllerTest`, `DisciplineControllerTest`; merge `EventTypesRootPostprocessorTest`, `RegistrationRecordTransactionLinkProcessorTest`, `DashboardUpcomingRegistrationsLinkProcessorTest`
- [ ] 7.3 `OrisEventController` / `OrisEventControllerTest`: local `OrisEventImportPort` mock in a dedicated class, with the "ORIS off" variant in the shared context
- [ ] 7.4 Run `events` tests, review, commit

## 8. Module `oris`

- [ ] 8.1 Add a dedicated primary port over `OrisClubKeyPort` (unit test first, `JMoleculesArchitectureTest` stays green); update `CommonWebMvcMockitoBeans`
- [ ] 8.2 Migrate `OrisClubKeyControllerTest` and `OrisControllerTest` (profile `oris`, local mocks of profile-gated beans, `OrisApiClient` kept as documented exception)
- [ ] 8.3 Run `oris` tests, review, commit

## 9. Module `common`

- [ ] 9.1 Classify the 16 `@WebMvcTest` classes in `common` (controller tests migrate; filter/advice/infrastructure tests keep a dedicated setup) and migrate the controller tests (`PermissionControllerTest`, `PasswordSetupControllerTest`, `DashboardControllerTest`, `RootControllerTest`, and others identified)
- [ ] 9.2 Merge `RootProfileLinkProcessorTest`, `OrisClubKeyRootLinkProcessorTest` and similar postprocessor tests into the controller tests where applicable
- [ ] 9.3 Run `common` tests, review, commit

## 10. Members follow-ups

- [ ] 10.1 Unit test for the `addsNoLinkWithoutContext` branch of the members link processor
- [ ] 10.2 Verify `POST /api/members` returning 201 is covered end-to-end elsewhere; add a test if not
- [ ] 10.3 Run `members` tests, review, commit

## 11. Remove legacy form

- [ ] 11.1 Confirm no test uses `@WithPostprocessors` or `@WebMvcTest(controllers = ...)`, then delete `WithPostprocessors`
- [ ] 11.2 Update `backend-patterns` skill (testing-guide, checklists): module-sliced form is the only form; remove references to the legacy form

## 12. Final verification

- [ ] 12.1 Run the full backend suite with `SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true --rerun-tasks`; confirm only the known failures (`ModularEventsTest`, `EventLoggingTests`) and skipped counts from XML results
- [ ] 12.2 Compare per-module test counts, contexts and test time with the baseline from 1.1; explain every difference
- [ ] 12.3 One-time mutation check of all migrated 403 tests (design D7): rename the `x-klabis-authority` section (open and close tag) in `api.mustache`, run all REST adapter tests of the migrated modules with `SKIP_OPTIMIZATIONS=true --rerun-tasks`, confirm every 403 test fails on the status assertion (not NPE/ServletException), fix tests that do not, document those enforced by another mechanism (ownership, field-level), restore the template with `git checkout` and confirm via `git status`
- [ ] 12.4 Run `ModuleStructureVerificationTest`, `JMoleculesArchitectureTest`, `LayerArchitectureTest`, `SecurityArchitectureTest`, `AffordanceRoutingArchitectureTest`
