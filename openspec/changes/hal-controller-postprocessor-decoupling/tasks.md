## 1. Baseline and guardrails

- [ ] 1.1 Run the full backend test suite on the starting revision and record the passing baseline.
- [ ] 1.2 Add/extend parity tests that capture the exact `_links` and `_templates` for the affected responses: group detail `owners`/`members`/`pendingInvitations`/`parents`/`trainers`, member list import affordance, member/discipline/event `sync` link, event `_embedded.registrations`, fee-group `_embedded.members`, membership-fee tier list `activeCampaign`/`pastCampaigns`, rule and campaign templates. These tests assert behaviour and must pass unchanged at the end.

## 2. Unify the controller→postprocessor side channel (D2)

- [ ] 2.1 Replace `MembershipFeeTierListPostprocessor`'s `ACTIVE_CAMPAIGN_ATTR` static setter/reader with `HalResponseContext`; update `MembershipFeeTierController.listTiers`.
- [ ] 2.2 Replace `MembershipFeeTierListRulesPostprocessor.OPTIONS_ATTR` static setter/reader with `HalResponseContext`; update `MembershipFeeTierController.listRules`.
- [ ] 2.3 Replace `FeeSelectionCampaignListPostprocessor.LEVEL_OPTIONS_ATTR` static setter/reader with `HalResponseContext`; update `FeeSelectionCampaignController.listPublications`.
- [ ] 2.4 Confirm no production code reads raw `RequestContextHolder` request attributes for controller→postprocessor handoff; run affected `@WebMvcTest` slices.

## 3. Deferred providers in `HalResponseContext` (D1, D6)

- [ ] 3.1 Add supplier-backed context API (`setDeferredContext(Class<T>, Supplier<T>)`, `embedDeferred(Supplier<Collection<?>>, Class<?>)`) with lazy, memoized, request-scoped resolution and `clear()` discarding unresolved suppliers; unit-test eager, lazy, memoized, ambiguous and cleared cases (extend `HalResponseContextTest`).
- [ ] 3.2 Convert `MemberController.listMembers` `ClubKeyHeld` and `MemberOrisImportAffordancePostprocessor` to deferred resolution (`MemberDiscoveryPort` / `OrisClubKeyPort` as `Optional`, feature-flag safe).
- [ ] 3.3 Convert `MemberController.getMember` `EnrolledMemberIds`, `DisciplineController` `EnrolledDisciplineIds`, and `EventController` `EnrolledEventIds` (list + detail) to deferred providers; assert the per-page sync lookup still runs at most once per request.
- [ ] 3.4 Convert `EventController.getEvent` `_embedded` registrations and `MembershipFeeGroupController.getFeeGroup` `_embedded` members to `embedDeferred`; assert the registration/member port is not called when no HAL response is rendered.
- [ ] 3.5 Keep `AccommodationListContext` / `RegistrationsCollectionContext` as cheap deferred path-variable passthroughs (do not revert to URI-template parsing); verify accommodation and registration list links unchanged.
- [ ] 3.6 Replace the Root/Dashboard placeholder domain with an explicit marker-wrap signal; ensure `RootAdminLinkProcessor`, root module link processors and `DashboardSelfLinkProcessor` still run when HAL is selected, and that a plain-JSON request does no wrapping.

## 4. Postprocessor-owned option data (D3)

- [ ] 4.1 Inject `RankingOptionsPort` / `EventTypeOptionsPort` (already mocked in `WithPostprocessors`) into `MembershipFeeTierListRulesPostprocessor`; delete `setOptions` and the controller-side option build in `listRules`.
- [ ] 4.2 Inject `FeeSelectionCampaignManagementPort` / `ObjectProvider` into `MembershipFeeTierListPostprocessor`; delete `setActiveCampaign` and the controller-side `findActiveCampaign` + `isAdmin` gate in `listTiers`; add any new mock to `WithPostprocessors`.
- [ ] 4.3 Inject `FeeSelectionCampaignManagementPort` / `MembershipFeeTierManagementPort` into `FeeSelectionCampaignListPostprocessor`; delete `setLevelOptions` and the controller-side level-option build in `listPublications`.
- [ ] 4.4 Remove `groupOptions` from `MemberFeeSummaryController.FeeSummaryView` and resolve group options inside `MemberFeeSummaryDetailsPostprocessor` via the port.
- [ ] 4.5 Run the membership-fees and events slice tests; confirm tier/rule/campaign/choose-tier templates unchanged.

## 5. Nested group-item postprocessing (D4, D5, D7)

- [ ] 5.1 Extend `HalResponseBodyAdvice` (or add a dedicated nested-model postprocessor it invokes) to walk `RepresentationModel` values nested in the payload and invoke the existing `RepresentationModelProcessorInvoker` once per item; unit-test with a nested `List<EntityModel<X>>` payload.
- [ ] 5.2 Add item postprocessors for the group item DTOs (`OwnerResponse`, `ParentResponse`, `TrainerResponse`, `FreeGroupMembershipResponse`, `FamilyGroupMembershipResponse`, `GroupMembershipResponse`, `PendingInvitationResponse`) reading the owning group from a deferred context value (D5).
- [ ] 5.3 Migrate `FreeGroupController`: delete `buildOwnerModel`, `buildMemberModel`, `buildPendingInvitationModel` and `InvitationModelBuilder`; confirm `getGroup` links/affordances parity.
- [ ] 5.4 Migrate `FamilyGroupController`: delete `toFamilyGroupResponse` item building and `buildChildModel`; confirm parity.
- [ ] 5.5 Migrate `TrainingGroupController`: delete the trainer/member item building in `toTrainingGroupResponse` and `buildLimitedGroupResponse`; confirm parity (including the limited-response shape for members/trainers).
- [ ] 5.6 Remove the unused `EntityLinks` field/constructor parameter from `EventRegistrationController`.

## 6. Cleanup and verification

- [ ] 6.1 Delete any `HalResponseContext` members and helper code left unused by phases 2–5; update `WithPostprocessors` with the final mock set.
- [ ] 6.2 Confirm no production controller under `*/infrastructure/restapi/` imports `klabisLinkTo`, `klabisAfford`, `EntityModel.of` or builds `_links`/`_templates` (excluding `Location` headers and `methodOn` path building).
- [ ] 6.3 Confirm no production controller eagerly calls a port solely to populate `HalResponseContext`.
- [ ] 6.4 Run the full backend test suite; all parity tests from 1.2 pass unchanged and no existing assertions required modification.
- [ ] 6.5 Manual HAL parity spot-check on port 3000 (admin + club member): root/dashboard, member list/detail, free/family/training group detail, membership-fees tier list and rules, event detail `_embedded`, calendar navigation.
- [ ] 6.6 If any step could not reach response parity, stop and convert this change to the `spec-driven` schema.
