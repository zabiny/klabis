## Why

`useHalPageData` is a shallow module. Its interface has about 15 members, and most of them only delegate to `useHalRoute`, `useHalActions` and `useIsAdmin`. Seven helpers have no caller (`getLinks`, `getTemplates`, `hasEmbedded`, `getEmbeddedItems`, `hasForms`, `getPageMetadata`, `hasTemplate`). Others have one or two callers each. 31 test files still mock the whole interface with `vi.fn()` stubs.

Page resources already have typed relations, generated into `halTypes.ts` from `x-hal-links` / `x-hal-templates`. Table rows do not. The spec describes relations only for a whole response, so 9 files hand-write row types like `EntityModelMemberSummaryResponse & {_templates?: Record<string, HalFormsTemplate>}`, and a typo in a template name compiles.

This is candidate 2B of the 2026-11-04 frontend architecture review. It is stacked on `extract-api-path-module`.

## What Changes

- **`useHalPageData<T>()` is narrowed** to `{resourceData, isLoading, error, navigateToResource, getResourceLink, refetch}`:
  - the 7 unused helpers are deleted;
  - `hasLink` is replaced by typed `resourceData._links?.x` (`MemberDetailPage`);
  - `isCollection` moves into `GenericHalPage`, its only caller;
  - `isAdmin` is dropped, and its 3 callers use `useIsAdmin()`;
  - `actions.handleNavigateToItem` merges into `navigateToResource` (3 callers), and `useHalActions` is deleted if nothing else uses it;
  - `route.*` is flattened; `queryState` is dropped (no page uses it);
  - `pathname` is dropped (see the next item).
- **`HalFormDisplay` loses its `pathname` prop.** It derives the current-resource URL itself (the resource's self link, then the current route from `HalRouteContext`), for both submit and target-data decisions. The 6 inline-edit pages stop passing `pathname={route.pathname}`. `EventDetailPage` links to the accommodation list through the typed `accommodation-list` link instead of `${route.pathname}/accommodation-list`.
- **New spec extensions `x-hal-item-links` and `x-hal-item-templates`** describe the relations on each row. They use the same map format as `x-hal-links` / `x-hal-templates`, and are allowed:
  - on a list response (rows in `_embedded`);
  - inside an `x-hal-embedded` block (nested collections such as event registrations);
  - next to `x-hal-entity-items: true` on an array property (group owners, members, trainers).
- **`tools/openapi-bundle`:**
  - the validator accepts the new keys and checks where they are placed;
  - `haltypes.mjs` generates a row type (`<Op>ItemResource` and the like) for each placement;
  - for entity-item arrays, the generated resource type overrides the array property with the typed row.
- **Spec content:** the row relations of the affected list endpoints, the event registrations and the group arrays are written into `docs/openapi/spec/*.yaml`. The row-level `updateMember` template moves from `listMembers`' `x-hal-templates` to `x-hal-item-templates`.
- **Hand-written row types are replaced** by generated ones in:
  - `MembersPage`, `EventsPage`, `EventTypesPage`, `CategoryPresetsPage`, `RulesTable`, `FinancesPage`, `EventDetailPage` (`RegistrationData`);
  - `pages/groups/types.ts` and `pages/training-groups/types.ts`.

  `MemberFeeChoicePage` switches to the existing generated `fee-summary` resource type.
- **Tests:** the shared mock helpers (`__mocks__/halGroupPageTest.tsx`, `__mocks__/halDialogTest.tsx`) and the inline `useHalPageData` mocks are updated to the narrow shape. There is no rewrite to HAL fixtures.
- **Docs:** the `klabis-api-spec` skill (`references/hypermedia.md`), `docs/openapi/spec/README.md`, and `hal-navigator-patterns` (`component-api.md`, `useHalPageData` section).

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/members/spec.md`, `events/spec.md`, `event-registrations/spec.md`, `event-types/spec.md`, `category-presets/spec.md`, `membership-fees/spec.md`, `member-accounts/spec.md`, `user-groups/spec.md`: unaffected. Pages show the same data and actions, gated by the same links and templates; only their compile-time types change.
- `openspec/specs/application-navigation/spec.md`: unaffected. Navigation targets are unchanged. The accommodation-list link resolves to the same route `/events/:id/accommodation-list`.
- `openspec/specs/non-functional-requirements/spec.md`: unaffected. No API response changes.

**Why no spec update is needed:**
- The OpenAPI additions are documentation and type-generation metadata. The backend does not read `x-hal-*` relation extensions, so requests, responses and authorization stay byte-for-byte the same.
- The frontend changes remove unused API surface and compile-time types.
- `HalFormDisplay` keeps the same URL precedence (template target, then resource URL, then current route). The only difference is that it resolves the URL itself instead of receiving it from the page.

## Impact

- **OpenAPI spec:** `docs/openapi/spec/` (`members.yaml`, `events.yaml`, `membershipfees.yaml`, `finance.yaml`, `groups.yaml`, `README.md`). No change to the generated backend code.
- **Tooling:** `tools/openapi-bundle/lib/validate.mjs`, `lib/haltypes.mjs` and their tests.
- **Frontend:**
  - `hooks/useHalPageData.ts`, `hooks/useHalActions.ts`;
  - `components/HalNavigator2/` (`HalFormDisplay`, `HalFormsPageLayout`, `HalFormPanel`, `useTemplateDialog`), `components/events/EventRegistrationDialog.tsx`, `components/membership-fees/RulesTable.tsx`;
  - pages in `members/`, `events/`, `finances/`, `groups/`, `training-groups/`, `membership-fees/`, plus `GenericHalPage.tsx`.
- **Tests:** about 31 test files that mock `useHalPageData`, and the shared mock helpers. Mechanical shape updates only.
- **Docs:** the `klabis-api-spec` and `hal-navigator-patterns` skills.
