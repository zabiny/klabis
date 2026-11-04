## 1. Narrow `useHalPageData`

- [x] 1.1 Narrow `hooks/useHalPageData.ts` to the D1 interface:
  - delete the 7 unused helpers;
  - flatten `route.*` and drop `queryState`, `isAdmin` and `actions`;
  - keep `pathname` for now (it is removed in group 2).

  Migrate `GenericHalPage` (local `isCollection`, `navigateToResource({href})`), `HalLinksSection` callers, `MemberDetailPage` (typed `resourceData._links?.x` instead of `hasLink`) and the 3 `isAdmin` callers (`useIsAdmin()`). Delete `hooks/useHalActions.ts` if it has no caller left.
- [x] 1.2 Update `__mocks__/halGroupPageTest.tsx`, `__mocks__/halDialogTest.tsx` and every inline `useHalPageData` mock to the new shape, keeping assertions. Tests that depend on `isAdmin` mock `useIsAdmin` (D6). Run the frontend test suite.

## 2. `HalFormDisplay` without `pathname`

- [x] 2.1 Add `HalFormDisplay` tests:
  - with no `pathname` prop, the submit URL falls back to the resource's self link, then to the current route;
  - an explicit `resourceUrl` wins;
  - the target-data fetch is skipped when the target equals the current-resource URL.

  Confirm the tests fail.
- [x] 2.2 Implement D2 in `HalFormDisplay` and `useHalFormData`. Remove the `pathname` prop and its passing from `useTemplateDialog`, `HalFormsPageLayout`, `HalFormPanel` and the inline-edit pages:
  - `EventDetailPage`, `MemberDetailPage`, `GroupDetailPage`, `TrainingGroupDetailPage`;
  - `MembershipFeeGroupDetailPage`, `MembershipFeeTierDetailPage`.

  Check `EventRegistrationDialog` for a render outside `HalRouteProvider`, and pass `resourceUrl` there if needed.
- [x] 2.3 `EventDetailPage` links to the accommodation list through `resourceData._links['accommodation-list']`. Remove `pathname` from `useHalPageData`. Update the affected tests mechanically and run the frontend test suite.

## 3. Row relation extensions in the tooling

- [x] 3.1 Add `validate.test.mjs` cases for `x-hal-item-links` / `x-hal-item-templates`:
  - accepted on an array-payload response, on an `x-hal-embedded` response and next to `x-hal-entity-items: true`;
  - rejected elsewhere;
  - `operation:` checked.

  Add `haltypes.test.mjs` cases for both placements (D4), including the entity-items property override and the unchanged output when no item rels exist. Confirm the tests fail.
- [x] 3.2 Implement D3 in `lib/validate.mjs` and D4 in `lib/haltypes.mjs`. Run the `tools/openapi-bundle` tests.

## 4. Spec content and generated row types

- [ ] 4.1 Members and events:
  - write `x-hal-item-links` / `x-hal-item-templates` for `listMembers` (moving `updateMember` there), `listEvents`, event types, category presets and the `getEvent` registrations, derived from the backend postprocessors (D5);
  - run `npm run openapi`;
  - replace the hand-written row types in `MembersPage`, `EventsPage`, `EventTypesPage`, `CategoryPresetsPage` and `EventDetailPage` (`RegistrationData`) with the generated ones.

  Run the backend build to confirm that the generated Java is unchanged, then `npm run build` and the tests of these pages.
- [ ] 4.2 Finance and membership fees: write the extensions for payment rules, transactions and fee-group members. Replace the row types in `RulesTable` and `FinancesPage`, and switch `MemberFeeChoicePage` to `GetFeeSummaryResource`. Run `npm run build` and the tests of these modules.
- [ ] 4.3 Groups:
  - write the extensions on the `x-hal-entity-items` arrays of free groups (owners, members, pending invitations) and training groups (members, trainers);
  - replace `pages/groups/types.ts` and `pages/training-groups/types.ts` with the generated resource types, deleting the files if they become empty.

  Run `npm run build` and the tests of the group pages.

## 5. Docs and verification

- [ ] 5.1 Document the new extensions in `docs/openapi/spec/README.md` and `.claude/skills/klabis-api-spec/references/hypermedia.md`. Update the `useHalPageData` description in `.claude/skills/hal-navigator-patterns/references/component-api.md` and `components/HalNavigator2/COMPONENTS.md`, removing the stale `route.refetch()`-after-submit advice.
- [ ] 5.2 Run the backend build (spec bundle and codegen), `npm run openapi`, `npm run build`, eslint and the full frontend test suite.
- [ ] 5.3 Run the E2E suite against a local instance. Only the 2 pre-existing unregister failures in `event-registrations.feature` are allowed.
