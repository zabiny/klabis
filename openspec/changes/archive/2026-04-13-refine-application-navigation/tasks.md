## 1. Navigation hook — section whitelist (TDD)

- [x] 1.1 Add failing test to `useRootNavigation.test.ts(x)` (create if missing): a response with `_links.events`, `_links.training-groups`, `_links.category-presets`, `_links.family-groups` returns navigation items where events has `section: 'main'` and the other three have `section: 'admin'`
- [x] 1.2 Add failing test: a response with only main-section links returns items that are all `section: 'main'`, and no `admin`-tagged items
- [x] 1.3 Add failing test: a response with only admin-section links returns items that are all `section: 'admin'`
- [x] 1.4 Extend the `NavigationItem` interface in `useRootNavigation.ts` with a `section: 'main' | 'admin'` field
- [x] 1.5 Add a module-level constant `ADMIN_RELS = new Set(['training-groups', 'category-presets', 'family-groups'])`
- [x] 1.6 Update `convertItems(...)` to assign `section = ADMIN_RELS.has(rel) ? 'admin' : 'main'` on every item
- [x] 1.7 Verify tests 1.1–1.3 pass

## 2. Layout — desktop sidebar two-section rendering (TDD)

- [x] 2.1 Add failing test to `Layout.test.tsx`: with a mocked `useRootNavigation` returning items in both sections, the sidebar renders two headings — the existing main heading and a new "Administrace" heading — and groups the items correctly
- [x] 2.2 Add failing test: with a mocked `useRootNavigation` returning only main-section items, the sidebar renders only the main heading and no "Administrace" heading (not even an empty placeholder)
- [x] 2.3 Add failing test: with a mocked `useRootNavigation` returning only admin-section items, the sidebar still renders the main-section heading (empty-menu fallback) and the "Administrace" heading with the admin items
- [x] 2.4 Add failing test: items within each section appear in the order they came from the hook
- [x] 2.5 Add new key `ui.navAdminSection: 'Administrace'` to `frontend/src/localization/labels.ts`
- [x] 2.6 Update the desktop sidebar block in `Layout.tsx` to filter `menuItems` into `mainItems` and `adminItems` before rendering
- [x] 2.7 Render the existing main heading + `mainItems` list as today
- [x] 2.8 When `adminItems.length > 0`, render a second heading (using `labels.ui.navAdminSection`) followed by the `adminItems` list; otherwise render nothing for the admin section
- [x] 2.9 Verify tests 2.1–2.4 pass

## 3. Layout — mobile bottom nav stays flat but main-only (TDD)

- [x] 3.1 Add failing test to `Layout.test.tsx`: at a mobile viewport width, with admin items present in the hook output, the bottom nav does NOT render any admin item
- [x] 3.2 Add failing test: at a mobile viewport width, with only main items present, the bottom nav renders the main items exactly as today
- [x] 3.3 Update the mobile bottom-nav block in `Layout.tsx` to iterate only the filtered `mainItems` instead of the full `menuItems`
- [x] 3.4 Verify tests 3.1 and 3.2 pass

## 4. Mutation hook — return `{ data, location }` (TDD)

- [x] 4.1 Add failing test to `useAuthorizedFetch.test.tsx`: a successful `POST` to a URL that responds `201` with `Location: /api/family-groups/abc` returns `{ data: null, location: '/api/family-groups/abc' }` from the `onSuccess` callback
- [x] 4.2 Add failing test: a successful `PUT` that responds `204` with no body and no `Location` returns `{ data: null, location: null }`
- [x] 4.3 Add failing test: a successful `POST` that responds `201` with a JSON body and a `Location` header returns `{ data: <parsed body>, location: <header> }`
- [x] 4.4 Update `UseAuthorizedMutationVariables` / return type signature so `mutationFn` returns `{ data: unknown; location: string | null }` instead of bare `unknown`
- [x] 4.5 Update `mutationFn` to (a) read `response.headers.get('Location')` before consuming the body, (b) attempt `response.json()` inside a try/catch (existing behavior), (c) return `{ data, location }`
- [x] 4.6 Propagate the type change into `useMutation`'s generic parameters so callers type-check correctly
- [x] 4.7 Verify tests 4.1–4.3 pass

## 5. Mutation hook — fix every existing caller

- [x] 5.1 Grep for `useAuthorizedMutation` usages across `frontend/src` and list each call site
- [x] 5.2 For each call site, update the `onSuccess` / inline success handler to read `.data` from the new result shape instead of the bare value. Call sites that used to access properties of the old return value now destructure `{ data, location }` and use `data`
- [x] 5.3 Run `tsc --noEmit` (or the project's equivalent) to confirm there are no remaining type errors after the caller updates
- [x] 5.4 Run the full frontend test suite via the `test-runner` agent — all existing tests must remain green

## 6. HalFormDisplay — navigate on Location after POST (TDD)

- [x] 6.1 Add failing test to `HalFormDisplay.test.tsx`: a template with method `POST` that mocks a successful submission with `location: '/api/family-groups/xyz'` calls `navigate('/family-groups/xyz')` after cache invalidation
- [x] 6.2 Add failing test: a template with method `PUT` that mocks a successful submission with `location: null` does NOT call `navigate`
- [x] 6.3 Add failing test: a template with method `DELETE` that mocks a successful submission does NOT call `navigate`
- [x] 6.4 Add failing test: a template with method `POST` but `location: null` does NOT call `navigate` (defensive: the backend forgot to set the header)
- [x] 6.5 Add failing test: when navigation happens, it happens AFTER `invalidateAllCaches()`, `route.refetch()`, and `addToast(...)`, so the user sees the success toast before the route change
- [x] 6.6 Import `useNavigate` from `react-router-dom` in `HalFormDisplay.tsx` and import the existing `extractNavigationPath` helper
- [x] 6.7 In `handleSubmit`, change the `onSuccess` callback to destructure `{ data, location }` from the mutation result
- [x] 6.8 Keep the existing order: `invalidateAllCaches` → `route.refetch` → toast → `onSubmitSuccess(data)` → (new) conditional `navigate(extractNavigationPath(location))` → `onClose`
- [x] 6.9 The `navigate` call happens only when `template.method?.toUpperCase() === 'POST'` AND `location != null`
- [x] 6.10 Verify tests 6.1–6.5 pass

## 7. Clean up `MemberRegistrationPage` manual fallback

- [x] 7.1 Remove the `handleSubmitSuccess` callback in `frontend/src/pages/members/MemberRegistrationPage.tsx` that tries to read `responseData._links.self` — the new `HalFormDisplay` behavior supersedes it
- [x] 7.2 Remove the `onSubmitSuccess={handleSubmitSuccess}` prop from the `<HalFormDisplay>` usage in the same file; the default post-POST navigation now handles the happy path
- [x] 7.3 Remove the `toHref` import if it is no longer used anywhere in the file
- [x] 7.4 Update `MemberRegistrationPage.test.tsx` to stop asserting the old `_links.self` behavior; assert that `HalFormDisplay` is called without `onSubmitSuccess` prop
- [x] 7.5 Verify the updated test passes

## 8. Clean up `MembersPage` dead `createFamilyGroup` wiring

- [x] 8.1 Open `frontend/src/pages/members/MembersPage.tsx`
- [x] 8.2 Delete the `createFamilyGroupModal` state hook (around line 45)
- [x] 8.3 Delete the "Create family group" `Button` block (around lines 171–179)
- [x] 8.4 Delete the `createFamilyGroupModal` `<Modal>` block (around lines 233–249) and the associated `HalFormDisplay`
- [x] 8.5 Remove the `Users` icon import if no longer referenced anywhere in the file
- [x] 8.6 Run `tsc --noEmit` and confirm no unused-import warnings remain
- [x] 8.7 Update `MembersPage.test.tsx` and `MembersPage.groups.test.tsx` — remove any assertions that reference the deleted button, state, or modal
- [x] 8.8 Run the tests via `test-runner` and confirm they pass

## 9. Manual QA walkthrough

- [x] 9.1 Start the app with `./runLocalEnvironment.sh`. Log in as admin (`ZBM9000`) on desktop
- [x] 9.2 Confirm the sidebar shows two sections: "NAVIGACE" with everyday items (Kalendář, Akce, Členové, Skupiny) and a new "Administrace" heading below with Šablony kategorií, Rodinné skupiny, Tréninkové skupiny
- [x] 9.3 Click each admin item and confirm navigation works
- [x] 9.4 Log out. Log in as a regular member (`ZBM9500`) on desktop
- [x] 9.5 Confirm the sidebar shows only "NAVIGACE" with main items and no "Administrace" heading at all
- [x] 9.6 Resize the browser to mobile width (or open in a mobile viewport)
- [x] 9.7 Confirm the bottom nav shows only main items and no admin items, regardless of which user is logged in
- [x] 9.8 Back on desktop as admin, open the Rodinné skupiny page and create a new family group
- [x] 9.9 Confirm the user is navigated to the detail page of the newly created family group immediately after submit, and a success toast is visible
- [x] 9.10 Go to Tréninkové skupiny and create a new training group. Confirm the user lands on its detail page
- [x] 9.11 Go to Skupiny (free groups) and create a new free group. Confirm the user lands on its detail page
- [x] 9.12 Edit an existing event via the inline edit on the detail page — confirm the user stays on the detail page after save (no unwanted navigation)
- [x] 9.13 Delete a category preset via its row action — confirm the user stays on the list page
- [x] 9.14 Go to Členové and confirm there is no "Create family group" button on the members list — the dead wiring is gone
- [x] 9.15 Run the full backend and frontend test suites via `test-runner` and confirm all green
