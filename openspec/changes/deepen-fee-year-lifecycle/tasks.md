## 1. Close and process the fee year through one port (TDD)

- [ ] 1.1 **Red** — add `FeeYearLifecyclePort` (`@PrimaryPort`) in `membershipfees.application` with `closeCampaign(FeeSelectionCampaignId)` and `processCampaignEnd(LocalDate)`, plus a `FeeYearLifecycleService` whose two methods throw `UnsupportedOperationException`.
- [ ] 1.2 **Red** — move `ManualCampaignClosePortTest` and `CampaignEndProcessingPortTest` onto the new interface, assertions and fixtures unchanged, and run them: both must fail.
- [ ] 1.3 **Red** — add the behaviour tests missing today, each failing for the right reason: closing an already-processed campaign is rejected, a member charged by one close is not charged again, a member without a choice publishes the sanction event, and a failure in one campaign aborts the rest of the batch.
- [ ] 1.4 **Green** — implement `FeeYearLifecycleService`, absorbing `CampaignProcessor` and the bodies of `ManualCampaignClosePortImpl` / `CampaignEndProcessingPortImpl`; keep `@Transactional` per method and the ordering freeze → charge → marker → missed-choice events → `markProcessed`.
- [ ] 1.5 **Refactor** — point `FeeSelectionCampaignController` and `FeeSelectionDeadlineScheduler` at the new port, delete `ManualCampaignClosePort`, `CampaignEndProcessingPort` and their implementations, and update `MembershipFeesWebMvcMockitoBeans` and the REST tests that referenced them.
- [ ] 1.6 Run the `membershipfees` test slice; commit slice 1.

## 2. One placement rule for member choice and emergency assignment (TDD)

- [ ] 2.1 **Red** — add `MemberFeePlacementTest` for the package-private collaborator: a member moving to a different group is removed from the current one with `removeMemberChoice` for a member choice and with unguarded `removeMember` for an administrator, is added with the right `AssignmentSource`, and both groups are saved. It must fail to compile against the missing class, then fail on behaviour.
- [ ] 2.2 **Red** — add `assignLevel(AssignFeeLevel)`, `chooseFeeLevel(ChooseFeeLevel)` and `removeFeeChoice(MemberId, int)` to `FeeYearLifecyclePort` as throwing stubs and move `AdminFeeAssignmentServiceTest` and `MemberChoiceServiceTest` onto them unchanged: the campaign-existence check, the immediate charge plus marker after a closed campaign, the resolve-event publication on assignment only, and the member-choice window guard must fail.
- [ ] 2.3 **Green** — implement the three methods in `FeeYearLifecycleService` through `MemberFeePlacement`.
- [ ] 2.4 **Refactor** — update `MembershipFeeGroupController`, `MemberFeeChoiceController` and `MemberFeeHistoryService` (which uses `getRecommendedLevelForYear`) to the new port; delete `AdminFeeAssignmentPort`, `AdminFeeAssignmentService`, `MemberChoicePort` and `MemberChoiceService`; make mechanical-only updates to `MembershipFeeGroupControllerTest`, `FeeSelectionCampaignControllerTest`, `MembershipFeesErrorTypesTest` and `MembershipFeesWebMvcMockitoBeans` — no assertion may be weakened or dropped in the move.
- [ ] 2.5 Run the `membershipfees` test slice; commit slice 2.

## 3. Cleanup and supporting documents

- [ ] 3.1 Delete the deprecated, uncalled `listPublications()` and `listClosedPublications()` from `FeeSelectionCampaignManagementPort` and its service, then compile to prove nothing referenced them.
- [ ] 3.2 Update the two `CampaignEndProcessingPort` references in `openspec/changes/introduce-quartz-scheduler/proposal.md` to the new port.
- [ ] 3.3 Confirm the **Fee year (příspěvkový rok)** term is present in `GLOSSARY.md` (added during the architecture review).

## 4. Verification

- [ ] 4.1 Run the full backend test suite, including the Modulith module-structure and ArchUnit tests.
- [ ] 4.2 Run a code review of the three slices and address the findings in a follow-up commit.
- [ ] 4.3 Commit slice 3 and verify the change contains no spec delta.
