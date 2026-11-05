## Context

`useHalPageData<T>()` has 22 callers. It returns:
- `resourceData`, `isLoading`, `error`, `isAdmin`;
- the `route` group: `pathname`, `navigateToResource`, `refetch`, `queryState`, `getResourceLink`;
- the `actions` group: `handleNavigateToItem`;
- 9 helper functions.

Usage on the base branch (non-test files):

| Member | Callers |
|---|---|
| `getLinks`, `getTemplates`, `hasEmbedded`, `getEmbeddedItems`, `hasForms`, `getPageMetadata`, `hasTemplate` | 0 |
| `isCollection` | `GenericHalPage` |
| `hasLink` | `MemberDetailPage` |
| `isAdmin` | 3 |
| `actions.handleNavigateToItem` | 3 (`GenericHalPage`, `HalLinksSection` via prop) |
| `route.queryState` | 0 (only `HalRouteContext` itself) |
| `route.refetch` | 6 (sync-completion and `onChanged` callbacks; these do not go through `useAuthorizedMutation`) |
| `route.getResourceLink` | 5 |
| `route.pathname` | 9 (6 × `pathname` prop of `HalFormDisplay`, `HalFormsPageLayout`, `useTemplateDialog`, `EventDetailPage` accommodation link) |

Page resources are typed by `halTypes.ts`, which `tools/openapi-bundle/lib/haltypes.mjs` generates from response-level `x-hal-links` / `x-hal-templates` (for example `GetEventResource`). Rows are not typed, for three reasons:
- list rows in `_embedded` carry relations the spec never lists. `listMembers` even puts the row template `updateMember` into the collection's `x-hal-templates`;
- rows of an `x-hal-embedded` collection (event registrations) are in the same situation;
- items of `x-hal-entity-items: true` arrays (group owners, members, pending invitations, trainers) become `EntityModel<X>` without typed relations, so `pages/training-groups/types.ts` rebuilds the resource with `Omit`.

The backend never reads response-level relation extensions. `KlabisSpringCodegen` consumes only `x-hal-entity-items` and `x-hal-input-type`. `validate.mjs` rejects unknown `x-hal-*` keys through `KNOWN_HAL_EXTENSIONS`.

## Goals / Non-Goals

**Goals:**
- `useHalPageData` keeps only members that have callers and do more than rename a context value.
- Pages stop passing `pathname` across the seam into the HAL-FORMS display.
- Every row with relations gets a generated type, and the hand-written row types disappear.

**Non-Goals:**
- Replacing `vi.mock(useHalPageData)` in tests with HAL fixtures. That is left for a later candidate, which rewrites the group pages anyway.
- A runtime HAL view (`res.link()` / `res.template()`). It was rejected because typed property access already exists, and a view would only relocate `toHref` / `asLinkArray`.
- `useHalRoute` / `HalRouteContext` itself, apart from what it hands to `useHalPageData`.
- Generating presence guarantees. Relations stay optional, as for responses.

## Decisions

### D1: Narrow interface

```ts
useHalPageData<T>(): {
  resourceData: T | null;
  isLoading: boolean;
  error: Error | null;
  navigateToResource: (resource: HalResponse | Link, options?) => void;
  getResourceLink: (rel?: string) => Link | null;
  refetch: () => Promise<void>;
}
```

- `handleNavigateToItem(href)` callers call `navigateToResource({href})`.
- `isAdmin` callers call `useIsAdmin()` directly.
- `GenericHalPage` computes `isCollection` locally.
- `useHalActions` is deleted once it has no caller.

Alternative: keep the `route` grouping. Rejected because every remaining member is equally common.

### D2: `HalFormDisplay` resolves its own current-resource URL

The `pathname` prop is removed. `HalFormDisplay` computes the current-resource URL in one place:
1. the `resourceUrl` prop, if given;
2. otherwise the `self` link of `resourceData`;
3. otherwise the current route from `HalRouteContext`.

It uses that URL for the submit fallback (after `template.target`) and for `useHalFormData`'s decision to fetch target data, which replaces the `currentPathname` argument.

`useTemplateDialog`, `HalFormsPageLayout`, `HalFormPanel` and the 6 inline-edit pages stop passing it. If a caller renders `HalFormDisplay` outside a `HalRouteProvider` (check `EventRegistrationDialog` on `HomePage`), it passes `resourceUrl` explicitly.

Precedence is unchanged for every current caller: today they pass the page route as `pathname`, and the page resource's self link is the same URL.

### D3: Row relation extensions, two placements

The two new keys `x-hal-item-links` and `x-hal-item-templates` use the same map format as `x-hal-links` / `x-hal-templates` (rel → `{operation?, description}`).

1. **On a response.** They describe each row in that response's `_embedded`. This covers list responses (`listMembers`, `listEvents`, …) and responses with `x-hal-embedded` (`getEvent` registrations, `getFeeGroup` members), because the rows of both sit in `_embedded`.
2. **On an array property marked `x-hal-entity-items: true`.** They describe each item of that array (`owners`, `members`, `pendingInvitations`, `trainers`).

Q13 asked for three placements. A separate key inside the `x-hal-embedded` value is not needed, because placement 1 already reaches those rows.

`validate.mjs`:
- adds both keys to `KNOWN_HAL_EXTENSIONS`;
- rejects them anywhere else;
- rejects them on a response that has no array payload and no `x-hal-embedded`;
- checks `operation:` against existing `operationId`s, like the response-level keys.

### D4: Generated row types

`haltypes.mjs` emits, per placement:

```ts
// placement 1
export interface ListMembersItemHal { _links?: {...}; _templates?: {...} }
export type ListMembersItemResource =
  components['schemas']['EntityModelMemberSummaryResponse'] & ListMembersItemHal;
export const ListMembersItemRels = {links: [...], templates: [...]} as const;

// placement 2: the parent resource type overrides the array property
export type GetTrainingGroupResource =
  Omit<components['schemas']['EntityModelTrainingGroupResponse'], 'members' | 'trainers'>
  & {members?: GetTrainingGroupMembersItemResource[]; trainers?: GetTrainingGroupTrainersItemResource[]}
  & GetTrainingGroupHal;
```

- The row schema is the `EntityModel` of the array's `items` payload, which openapi-typescript already generates.
- Type names follow the operation, plus `Item` (response) or the PascalCase property name plus `Item` (property).
- For a property schema used by several operations, the override is emitted on each resource that uses it.

`haltypes.test.mjs` covers both placements, the override, and the "no item rels → unchanged output" case.

### D5: Spec content

For each row type the frontend hand-writes today, the implementer reads the backend postprocessor or assembler that adds the row's links and affordances, and writes them into the spec with their authorisation conditions in `description`. Sources:
- members, events, event types, category presets, payment rules and transactions lists;
- event registrations, fee-group members;
- group owners, members and pending invitations; training-group members and trainers.

`updateMember` moves from `listMembers`' `x-hal-templates` to `x-hal-item-templates`, because the backend adds it per row.

`MemberFeeChoicePage` already has `x-hal-templates` on `getFeeSummary`, so it switches to `GetFeeSummaryResource` without spec work.

### D6: Tests

- Update `__mocks__/halGroupPageTest.tsx`, `__mocks__/halDialogTest.tsx` and the inline `vi.mocked(useHalPageData).mockReturnValue({...})` objects to the D1 shape, keeping every assertion.
- Tests that assert `isAdmin`-dependent rendering mock `useIsAdmin` instead.
- Typed row fixtures may need the generated types. Where a fixture used `as unknown as`, keep the cast.

## Risks / Trade-offs

- **The spec lists a relation the backend no longer emits, or misses one.** That only weakens types: an unknown rel fails the build and leads to a fix, and a stale rel is harmless at runtime. Mitigation: derive the list from backend code (D5), not from the frontend.
- **Generated overrides drift from openapi-typescript's schema names.** Mitigation: `haltypes.test.mjs` plus `npm run build` after `npm run openapi`.
- **About 31 test files change mechanically.** Mitigation: the shared helpers absorb most of it, and the changes are done page by page together with the code they cover.
- **`HalFormDisplay` outside a route provider.** Mitigation: an explicit `resourceUrl` (D2), and a test for that case.
