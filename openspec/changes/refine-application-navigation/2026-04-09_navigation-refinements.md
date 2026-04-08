# TCF: refine-application-navigation

**Created:** 2026-04-09
**Proposal:** `openspec/changes/refine-application-navigation/`
**Coordinator:** team-leader (main conversation)

## Purpose

Synchronization file for subagents working on implementing the `refine-application-navigation` proposal. Every subagent MUST read this file before starting and append a concise summary of their changes/issues when done.

## Plan (vertical slices)

The proposal has 9 task groups. We split work into these iterations (each committable + app stays functional):

- **Iteration A:** Tasks 4 + 5 — mutation hook returns `{data, location}` and update all callers. Foundational refactor; all callers keep working after update.
- **Iteration B:** Tasks 6 + 7 — `HalFormDisplay` auto-navigates on POST+Location; remove dead fallback in `MemberRegistrationPage`. Delivers "navigate to newly created resource" feature end-to-end.
- **Iteration C:** Tasks 1 + 2 + 3 — `useRootNavigation` section whitelist + `Layout.tsx` desktop two-section rendering + mobile bottom-nav stays flat main-only. Delivers "Administrace section" end-to-end.
- **Iteration D:** Task 8 — remove dead `createFamilyGroup` wiring from `MembersPage.tsx`. Pure cleanup.
- **Iteration E (final):** simplify → code review → fix high-priority findings → run all tests → commit.

Task 9 (manual QA walkthrough) is handed back to the user after implementation is merged.

## Status

- [x] TCF created
- [x] Iteration A (tasks 4+5)
- [x] Iteration B (tasks 6+7)
- [x] Iteration C (tasks 1+2+3)
- [x] Iteration D (task 8)
- [x] Iteration E (simplify + code review + test run + commit)

## Subagent notes

### Iteration A — 2026-04-09

**Changes made:**

- `frontend/src/hooks/useAuthorizedFetch.ts`: Added `MutationResult { data: unknown; location: string | null }` interface. Updated `mutationFn` to read `response.headers.get('Location')` before consuming the body, then return `{ data, location }`. Updated `UseAuthorizedMutationOptions.onSuccess` and `onSettled` types accordingly. Changed `useMutation` generic from `unknown` to `MutationResult`.

- `frontend/src/hooks/useAuthorizedFetch.test.ts`: Added 3 new tests for Location header behaviour (tasks 4.1–4.3). Updated all existing `result.current.data` and `onSuccess`/`onSettled` assertions to match the new `{ data, location }` shape. Added `createMockResponseWithHeaders` helper (local to test file — `createMockResponse` in `__mocks__/mockFetch.ts` was not modified since it is shared and its `Headers` default covers the no-Location case).

- `frontend/src/components/HalNavigator2/HalFormDisplay.tsx`: Updated `onSuccess` handler to destructure `{ data: responseData }` from the mutation result.

- `GroupsPage.tsx` and `PermissionsDialog.tsx` `onSuccess` callbacks did not access the return value, so no functional change was needed — the TypeScript type change was sufficient and `tsc --noEmit` confirmed no errors.

**No issues.** All 1080 frontend tests pass; `tsc --noEmit` exits 0.

### Iteration B — 2026-04-09

**Changes made:**

- `frontend/src/components/HalNavigator2/HalFormDisplay.test.tsx`: Added `vi.mock('react-router-dom', ...)` with `mockNavigate` spy and a `createMockResponseWithLocation` helper. Added 5 new tests under `describe('auto-navigation on POST+Location', ...)` covering: POST+Location navigates, PUT+location does not navigate, DELETE does not navigate, POST+null location does not navigate, and order guarantee (invalidate → refetch → navigate).

- `frontend/src/components/HalNavigator2/HalFormDisplay.tsx`: Imported `useNavigate` from `react-router-dom` and `extractNavigationPath` from `../../utils/navigationPath.ts`. Added `const navigate = useNavigate()`. In `onSuccess`, destructured `location` alongside `data: responseData`. Added conditional `navigate(extractNavigationPath(location))` after `onSubmitSuccess?.(responseData)` — fires only when `template.method?.toUpperCase() === 'POST' && location != null`. `onClose()` still runs unconditionally after.

- `frontend/src/pages/members/MemberRegistrationPage.tsx`: Removed `handleSubmitSuccess` callback (which read `responseData._links.self`), removed `onSubmitSuccess={handleSubmitSuccess}` prop from `<HalFormDisplay>`, removed `toHref` import (`useNavigate` retained for `onClose`).

- `frontend/src/pages/members/MemberRegistrationPage.test.tsx`: Replaced 3 old "navigation after success" tests (which drove `capturedOnSubmitSuccess`) with 1 test asserting `HalFormDisplay` is called without `onSubmitSuccess` prop.

**No issues.** All 1083 frontend tests pass; `tsc --noEmit` exits 0.

### Iteration C — 2026-04-09

**Changes made:**

- `frontend/src/hooks/useRootNavigation.ts`: Extended `NavigationItem` interface with `section: 'main' | 'admin'`. Added module-level `ADMIN_RELS = new Set(['training-groups', 'category-presets', 'family-groups'])`. Updated `convertItems` to assign `section` based on rel membership in the set.

- `frontend/src/hooks/useRootNavigation.test.ts`: Created new test file with 3 tests covering mixed sections, main-only, and admin-only scenarios.

- `frontend/src/localization/labels.ts`: Added `ui.navAdminSection: 'Administrace'`.

- `frontend/src/pages/Layout.tsx`: Derived `mainItems` and `adminItems` from `menuItems` via filter. Desktop sidebar now renders `mainItems` first, then conditionally renders the "Administrace" heading + `adminItems` when `adminItems.length > 0`. Mobile bottom nav iterates `mainItems` only.

- `frontend/src/pages/Layout.test.tsx`: Updated default mock to include `section` field. Added 6 new tests across two describe blocks: "Desktop sidebar — two-section rendering" (tasks 2.1–2.4) and "Mobile bottom nav — main-only filtering" (tasks 3.1–3.2).

**No issues.** All 1092 frontend tests pass; `tsc --noEmit` exits 0.

### Iteration D — 2026-04-09

**Changes made:**

- `frontend/src/pages/members/MembersPage.tsx`: Removed `createFamilyGroupModal` useState hook. Removed the `createFamilyGroup` conditional `<Button>` block. Removed the `createFamilyGroup` conditional `<Modal>` + `<HalFormDisplay>` block. Removed `Users` from the `lucide-react` import (was only used by the deleted button).

- `frontend/src/pages/members/MembersPage.groups.test.tsx`: Deleted the entire `describe('MembersPage — family group creation (task 5.4)')` block (3 tests). Removed the now-unused `fireEvent` import.

- `frontend/src/pages/members/MembersPage.test.tsx`: No changes needed — this file had no assertions referencing the deleted code.

**No issues.** All 1089 frontend tests pass; `tsc --noEmit` exits 0.

### Iteration E — simplify pass — 2026-04-09

**Simplifications applied (2):**

1. **Consolidated test `Response` mock helpers into shared `createMockResponse`.**
   - `createMockResponseWithHeaders` in `useAuthorizedFetch.test.ts` (22 lines) was a full duplicate of the shared `createMockResponse` factory in `__mocks__/mockFetch.ts`, differing only in accepting a `headers` map.
   - Extended `createMockResponse` signature with an optional `headers: Record<string, string> = {}` third parameter, making it the single source for `Response` mock construction.
   - Removed the local helper from `useAuthorizedFetch.test.ts`; all 3 call-sites now use `createMockResponse` directly.
   - `createMockResponseWithLocation` in `HalFormDisplay.test.tsx` (18 lines) reduced to a 2-line thin wrapper over `createMockResponse`.

2. **Removed redundant `.mockClear()` calls in `HalFormDisplay.test.tsx`.**
   - `mockInvalidateAllCaches.mockClear()` and `mockNavigate.mockClear()` were called after `vi.clearAllMocks()` in `beforeEach`. `vi.clearAllMocks()` already clears all mocks — the two extra calls were no-ops.

**Left alone by design:**
- `ADMIN_RELS` whitelist in `useRootNavigation.ts` — per design decision 1 this is an explicit opt-in set, not derivable from link structure.
- NavLink className inline functions duplicated between `mainItems` and `adminItems` in `Layout.tsx` — identical logic, but extracting to a named function would change a file not in scope without clear benefit; left for a future refactoring pass.

**Result:** `tsc --noEmit` exits 0. All 1089 frontend tests pass.

### Iteration E — tsc -b fixes — 2026-04-09

**Four type errors masked by `tsc --noEmit` but caught by `tsc -b`:**

1. `HalFormDisplay.test.tsx` — `renderPostForm(method: string)` parameter typed as `string` was incompatible with `mockHalFormsTemplate`'s `method?: HalFormsTemplateMethod`. Fixed by importing `HalFormsTemplateMethod` and narrowing the parameter type.

2. `useIsAdmin.test.tsx` — Five mock `NavigationItem` literals were missing the `section` field added in Iteration C. Fixed by adding `section: 'main'` to the three `main` items (`members`, `events`) and `section: 'admin'` to the `admin` item.

3. `MemberRegistrationPage.test.tsx` — `capturedOnSubmitSuccess` was a module-level `let` that was assigned in the mock but never read in any assertion (left behind from the Iteration B test rewrite). Removed the declaration, the assignment in the mock, and the `undefined` reset in `beforeEach`. Removed `onSubmitSuccess` from the mock destructuring.

4. `MembersPage.groups.test.tsx` — `renderPage` removal in Iteration D left the entire mock scaffold orphaned (`MemoryRouter`, `QueryClient`, `QueryClientProvider`, `useHalPageData`, `MembersPage`, `mockHalFormsTemplate`, `createMockPageData`, and all `vi.mock(...)` calls). Rewrote the file to contain only the imports actually used by the remaining test (`FetchError`, `vi`).

**Result:** `tsc -b` exits 0. All 1090 tests pass. `npm run refresh-backend-server-resources` completes cleanly.

### Iteration E — code review fixes — 2026-04-09

**Blocking fix — `onClose` skipped on POST+Location navigation:**

- `HalFormDisplay.tsx` `handleSubmit` `onSuccess`: replaced unconditional `onClose()` after `navigate(...)` with a `willNavigate` guard. When `method === 'POST' && location != null`, only `navigate(...)` is called; `onClose()` is skipped. For all other methods (PUT, DELETE, POST without Location), `onClose()` is called as before. This eliminates the double-navigation regression on `MemberRegistrationPage` (which passes `onClose={() => navigate('/members')}`).

**Warning fix 1 — call-order test instruments `addToast`:**

- `HalFormDisplay.test.tsx`: Added `mockAddToast` spy and `vi.mock('../../contexts/ToastContext', ...)` to intercept `useToast`. In the ordering test, `mockAddToast` now pushes `'toast'` into `callOrder`. Expected sequence updated from `['invalidate', 'refetch', 'navigate']` to `['invalidate', 'refetch', 'toast', 'navigate']`.

**Warning fix 2 — `onSettled` dropped from `UseAuthorizedMutationOptions`:**

- `useAuthorizedFetch.ts`: Removed `onSettled` from `UseAuthorizedMutationOptions` interface and from the `useMutation` call (no live callers existed).
- `useAuthorizedFetch.test.ts`: Removed the `onSettled` callback test.

**New regression-guard tests added to `HalFormDisplay.test.tsx`:**

- `does NOT call onClose after POST+Location (navigation supersedes close)` — asserts `mockOnClose` is never called when POST+Location fires.
- `calls onClose after PUT success (no navigation)` — asserts `mockOnClose` is called and `mockNavigate` is not called on PUT.

**Result:** `tsc --noEmit` exits 0. All 1090 frontend tests pass.
