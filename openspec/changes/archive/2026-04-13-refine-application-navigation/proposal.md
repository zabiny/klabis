## Why

Two navigation-level issues show up consistently across the application:

- **Menu clutter.** The main navigation currently mixes everyday user destinations (Kalendář, Akce, Členové, Skupiny) with administrative ones (Tréninkové skupiny, Šablony kategorií, Rodinné skupiny) in a single flat list. For a regular club member most of the admin items are not even clickable (the backend withholds the corresponding HAL links), but managers get a long unstructured column that does not communicate which items are day-to-day vs. which are administrative.
- **"Create something" forms dump the user back on the list.** When a manager creates a new group, the current form submit flow closes the modal or clears the form params and leaves the user on the same list page. The natural next action is almost always "configure the thing I just created" — but to do that the user has to find the row in the list and click through. Worse, for most `POST` endpoints the backend already returns a `Location` header that points at the newly-created resource, and the frontend simply throws it away because `useAuthorizedMutation` only reads the response body.

## What Changes

- **Main menu gains a secondary "Administrace" section** on the desktop sidebar. The section contains every administrative destination the current user is authorized to see. Today that means `training-groups`, `category-presets`, and `family-groups`; additional administrative items added later automatically land in the same section.
- **Administrace section renders only when it contains at least one visible item.** A regular member without any admin authorizations sees no section header, no empty state, no "Administrace" heading.
- **Section assignment lives in the frontend navigation layer**, not in the backend HAL response. The `useRootNavigation` hook maps each HAL rel to a section using a small whitelist, so the backend continues to emit a flat `_links` map and adding a new item is a one-line frontend change.
- **Mobile bottom navigation ignores the administrative section entirely.** On mobile the bottom nav still shows everyday destinations only — there is not enough horizontal room to also render an admin row, and the dashboard is not being extended as part of this change. This is an explicit trade-off: admin tasks on mobile are out of reach until a later iteration.
- **Create-style form submissions navigate to the newly created resource automatically.** `useAuthorizedMutation` captures the `Location` response header alongside the JSON body and exposes it to callers. `HalFormDisplay` uses it: on a successful `POST` that returns a `Location` header, the form layout navigates the user to the resource path extracted from that header. Edit (`PUT`/`PATCH`) and delete (`DELETE`) endpoints return no `Location` and behave exactly as today.
- **Dead `createFamilyGroup` modal wiring is removed from `MembersPage.tsx`.** The page currently contains a button, modal, and state machine that are never reachable because the backend does not emit the `createFamilyGroup` template on `/api/members`. With family groups staying on their own page inside the Administrace section, this dead code has no purpose and is deleted.

## Capabilities

### New Capabilities
- `application-navigation`: a single capability that owns the rules for how the main menu is structured, how sections are rendered, how the frontend decides section membership, how mobile vs. desktop navigation differ, and how the application navigates the user after successful create-style form submissions.

### Modified Capabilities
<!-- none -->

## Impact

**Frontend:**
- `frontend/src/hooks/useRootNavigation.ts` — `NavigationItem` gains a `section: 'main' | 'admin'` field. The hook applies a hard-coded whitelist that maps `training-groups`, `category-presets`, and `family-groups` to `'admin'`; everything else defaults to `'main'`. Items keep their original order within each section.
- `frontend/src/pages/Layout.tsx` — the desktop sidebar renders two groups: the main navigation (existing heading "NAVIGACE") and a new administrative group (heading label from `labels.ui.navAdminSection`). The admin group is only rendered when the filtered admin item list is non-empty. The mobile bottom-nav continues to render the flat list of items from the main section only; admin items are explicitly excluded on mobile.
- `frontend/src/localization/labels.ts` — new key `ui.navAdminSection: 'Administrace'`.
- `frontend/src/hooks/useAuthorizedFetch.ts` — `useAuthorizedMutation`'s `mutationFn` returns `{ data, location }` instead of just the parsed JSON. `location` is the raw `Location` response header or `null`. Every existing caller that reads the `onSuccess` argument now reads `.data` instead of the bare value.
- `frontend/src/components/HalNavigator2/HalFormDisplay.tsx` — `handleSubmit` inspects the mutation result and, when the HTTP method is `POST` and a `Location` header is present, navigates to `extractNavigationPath(location)` using `useNavigate()` after cache invalidation and toast. If no `Location` is present (e.g. `PUT`, `PATCH`, `DELETE`), behavior is unchanged.
- `frontend/src/components/HalNavigator2/HalFormsPageLayout.tsx` — no change beyond verifying the success path still triggers `onSubmitSuccess` correctly after the `HalFormDisplay` navigation.
- `frontend/src/pages/members/MembersPage.tsx` — delete the `createFamilyGroupModal` state, the "Create family group" `Button`, and the associated modal block (lines ~45, ~171–179, ~233–249). The "Members" page no longer tries to render a create-family-group action.
- `frontend/src/pages/members/MemberRegistrationPage.tsx` — remove the manual `handleSubmitSuccess` fallback that parses `responseData._links.self`; the new generic `HalFormDisplay` behavior supersedes it. The page should still navigate to `/members` on `onClose` (cancel), unchanged.
- Tests: `useRootNavigation.test.ts(x)` (new or extended) covers the section whitelist; `Layout.test.tsx` covers the "admin section only when non-empty" behavior; `HalFormDisplay.test.tsx` covers the post-create navigation based on a mocked `Location` header; `useAuthorizedFetch.test.tsx` covers the new `{data, location}` return shape; `MembersPage.test.tsx` is updated to remove assertions about the deleted dead code.

**Backend:**
- No changes. The backend already:
  - emits a flat `_links` map from `GET /api`,
  - gates each navigation link behind the relevant authority via `klabisLinkTo`,
  - returns `Location` headers from every `POST` create endpoint (`ResponseEntity.created(...)`),
  - withholds affordances that the current user is not authorized for.
- All four poznámky are satisfied by frontend-only changes plus the dead-code cleanup in `MembersPage.tsx`.

**Specs:**
- New `openspec/specs/application-navigation/spec.md` defines the main menu structure, section rendering rules, mobile vs. desktop differences, and the "navigate to newly created resource" interaction pattern.

**Out of scope:**
- Extending the dashboard with admin shortcut buttons. Mobile users will not be able to reach training-groups, category-presets, or family-groups until a follow-up change addresses this.
- Any URL restructuring of existing admin routes. `training-groups`, `category-presets`, and `family-groups` keep their top-level paths.
- Any change to what the backend returns from `GET /api` or from any resource endpoint.
- Any change to the register-member flow beyond deleting its now-redundant manual navigation fallback.
- A redirect for the removed "Rodinné skupiny" top-level menu entry — there is none; family-groups stays under Administrace with the same URL as today.
