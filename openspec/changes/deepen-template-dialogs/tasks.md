## 1. The hook and `HalFormButton`

- [x] 1.1 Write `components/HalNavigator2/useTemplateDialog.test.tsx` with a real `QueryClient`, `MemoryRouter`, a HAL fixture and the `__mocks__/mockFetch.ts` stub, with no mock of `HalFormDisplay` or `Modal`. It covers:
  - `available` / `availableOn` / `label`;
  - `open()` renders the dialog with the default title (design D4), and an explicit `title` wins;
  - the submit URL is `template.target`, otherwise the resource's `self` path, otherwise the route fallback (D2);
  - `open(row, name)` with a late-bound name (D3);
  - cancel closes without a request;
  - a successful submit shows the success toast and closes; with `navigateOnSuccess` and a POST + `Location` it navigates instead;
  - `onSubmitError` returning `true` closes the dialog;
  - `fieldsFactory` is used;
  - the dialog is rendered through a portal into `document.body` (D8).

  Confirm that the tests fail.
- [x] 1.2 Implement `useTemplateDialog` (D1–D4, D8). 1.1 passes.
- [x] 1.3 Rebuild the modal variant of `HalFormButton` on the hook (D7). Remove `resourceContext` from `HalFormRequest`, `HalFormButton` and `HalFormsPageLayout`, and drop the modal branch of `HalFormsPageLayout`. Update `HalFormButton.test.tsx` and `HalFormsPageLayout.test.tsx` mechanically, keeping their assertions.
- [x] 1.4 Add a test: opening `synchronizeNow` from `SyncStatusOverlay` shows the form above the overlay. Then run the frontend test suite.

## 2. Membership fees and legal guardians

- [x] 2.1 Migrate `components/membership-fees/RulesTable.tsx`, `components/membership-fees/CampaignDetail.tsx`, `components/legal-guardians/LegalGuardiansSection.tsx`, and the delete modal of `pages/membership-fees/MembershipFeeTierDetailPage.tsx` and the `assignMember` modal of `pages/membership-fees/MembershipFeeGroupDetailPage.tsx` from `HalFormModal` to the hook. Keep every `title`, `note`, `successMessage` and callback expression (D5).
- [x] 2.2 Rewrite the affected tests: drop the `HalFormModal` / `HalFormDisplay` / `Modal` mocks and assert dialog titles and submit requests through the fixture. Run the tests of these modules.

## 3. Groups and training groups

- [x] 3.1 Migrate every modal in `pages/groups/GroupDetailPage.tsx` to the hook: add/invite member, add/remove owner, remove member, leave, delete and cancel invitation. Member-picker dialogs pass `fieldsFactory: createMemberFilteredFactory(…)` (D6). Row templates use `open(row)` / `availableOn(row)`. Drop `route.refetch()` from `onClose` (D9). The self-removal of an owner keeps its current post-submit behavior.
- [x] 3.2 Migrate every modal in `pages/training-groups/TrainingGroupDetailPage.tsx` in the same way, including the trainee and trainer pickers with their filter.
- [x] 3.3 Delete the `_templates` cast types in `pages/groups/types.ts` and `pages/training-groups/types.ts` that are no longer used.
- [x] 3.4 Rewrite the affected tests, including `TrainingGroupDetailPage.trainers.test.tsx`, without mocking `HalFormDisplay`, `HalFormModal` or `Modal`. Assert that member-picker dialogs hide current members. Run the tests of these pages.

## 4. Members, events, category presets, event types

- [x] 4.1 Migrate the `suspendMember` and `sendMemberAccountActivation` modals of `pages/members/MemberDetailPage.tsx`. Keep the `labels.dialogTitles.*` titles, `size="2xl"`, the confirmation `note`, `successMessage`, `submitLabel` and `onSuspendSubmitError`. Buttons use `available` / `label` where the label is unchanged.
- [x] 4.2 Replace the row `actionModal` state in `pages/members/MembersPage.tsx`, `pages/events/EventsPage.tsx`, `pages/events/CategoryPresetsPage.tsx` and `pages/events/EventTypesPage.tsx` with one late-bound hook each (D3), keeping each page's title expression (D5) and `navigateOnSuccess`.
- [x] 4.3 Rewrite the affected tests without mocking `HalFormDisplay` or `Modal`. Keep the existing assertions on titles, toasts and navigation. Run the tests of these pages.

## 5. Cleanup

- [x] 5.1 Delete `components/HalNavigator2/HalFormModal.tsx`. Confirm there are no remaining imports.
- [x] 5.2 Remove `excludeMemberIds` / `includeOnlyMemberIds` and their branch from `HalFormDisplay` (D6). Confirm that no page outside the out-of-scope inline cases and `EventRegistrationDialog` imports `HalFormDisplay` for a modal.
- [x] 5.3 Update `.claude/skills/hal-navigator-patterns/references/component-api.md` where it describes opening `HalFormDisplay` in a modal, so it points to `useTemplateDialog`.
- [ ] 5.4 Run `npm run build` and the full frontend test suite. Run `npm run refresh-backend-server-resources`.
- [ ] 5.5 Run the E2E scenarios touching groups, members, events and membership fees against a local instance. All pass.
