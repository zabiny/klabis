# Application Navigation Refinement - QA Testing

## Scenarios

### Desktop Sidebar — Admin Section
- [x] **NAV-1**: Admin (ZBM9000) sees "Administrace" section in sidebar with 3 items: Šablony kategorií, Rodinné skupiny, Tréninkové skupiny
- [x] **NAV-2**: Regular member (ZBM9500) sidebar has NO "Administrace" section

### Mobile Bottom Nav
- [x] **NAV-3a**: Admin (ZBM9000) on 375px viewport — bottom nav shows only main items (Kalendář, Akce, Členové, Skupiny), no admin items
- [x] **NAV-3b**: Regular member (ZBM9500) on 375px viewport — bottom nav shows only main items

### POST 201 Navigation
- [x] **NAV-4**: Create family group → redirects to detail page of new group + toast visible
- [x] **NAV-5**: Create training group → redirects to detail page of new group
- [x] **NAV-6**: Create free group → redirects to detail page of new group

### No Navigation After Edit/Delete
- [x] **NAV-7**: Edit existing event → stays on detail page after save (no unwanted navigation)
- [x] **NAV-8**: Delete category preset → stays on list page (no unwanted navigation)

### Dead Code Removed
- [x] **NAV-9**: Members page has NO "Create family group" button

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | Admin sidebar shows Navigace + Administrace sections correctly |
| NAV-2 | FAIL | ZBM9500 sees "Administrace" section with "Rodinné skupiny" — backend returns `family-groups` link for all users because `listFamilyGroups` is missing `@HasAuthority(MEMBERS_MANAGE)` annotation |
| NAV-3a | PASS | Admin mobile bottom nav shows only main items, no admin items |
| NAV-3b | SKIP | Blocked by NAV-2 issue — ZBM9500 gets family-groups link |
| NAV-4 | FAIL | POST to `/api/family-groups` returns 400 when `parent: null` — `CreateFamilyGroup` requires parent but HAL-Forms template doesn't mark it required; Jackson fails to deserialize `null` into `MemberId` |
| NAV-5 | PASS | After creating training group, redirected to detail page |
| NAV-6 | PASS | After creating free group, redirected to detail page + toast visible |
| NAV-7 | PASS | After editing event, stays on detail page |
| NAV-8 | PASS | After deleting category preset, stays on list page |
| NAV-9 | PASS | No "Create family group" button on Members page |

## Issues Found

### Iteration 2 (after fixes)
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | Admin sidebar correct |
| NAV-2 | PASS | ZBM9500 has no Administrace section after adding @HasAuthority(MEMBERS_MANAGE) to listFamilyGroups |
| NAV-3a | PASS | Admin mobile bottom nav correct |
| NAV-3b | PASS | ZBM9500 mobile bottom nav shows only main items |
| NAV-4 | PASS | Family group creation redirects to detail + toast; parent field now required (*) |
| NAV-5 | PASS | Training group creation redirects to detail |
| NAV-6 | PASS | Free group creation redirects to detail + toast |
| NAV-7 | PASS | Event edit stays on detail page |
| NAV-8 | PASS | Category preset delete stays on list page |
| NAV-9 | PASS | No create family group button on members page |

**All 9 scenarios PASS in iteration 2.**

### Iteration 3 (re-run 2026-04-13)
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | Admin sidebar shows Navigace + Administrace sections with correct items |
| NAV-2 | PASS | ZBM9500 sidebar shows only Navigace section, no Administrace |
| NAV-3 | PASS | Admin at 375px — bottom nav shows only main items (Domů, Kalendář, Akce, Členové, Skupiny), no admin items |
| NAV-4 | PASS | Family group created with ZBM9500 as parent → navigated to /family-groups/{id} detail page |
| NAV-5 | PASS | Training group created (age range 26-34) → navigated to /training-groups/{id} detail page |
| NAV-7 | PASS | Event inline edit → "Uložit změny" keeps user on /events/{id} detail page (no redirect) |
| NAV-9 | PASS | Members page has no "Create family group" button; only "Registrovat člena" is present |

**All 7 tested scenarios PASS in iteration 3.**

## Fixes Applied

### Fix 1: Add `@HasAuthority(Authority.MEMBERS_MANAGE)` to `listFamilyGroups`
- **File**: `backend/src/main/java/com/klabis/members/familygroup/infrastructure/restapi/FamilyGroupController.java`
- Added `@HasAuthority(Authority.MEMBERS_MANAGE)` annotation on `listFamilyGroups` method so `klabisLinkTo` withholds the `family-groups` link from users without that authority

### Fix 2: Introduce `CreateFamilyGroupRequest` DTO with `@NotNull UUID parent`
- **New file**: `backend/src/main/java/com/klabis/members/familygroup/infrastructure/restapi/CreateFamilyGroupRequest.java`
- **File**: `FamilyGroupController.java` — `createFamilyGroup` now accepts `CreateFamilyGroupRequest` (with `@NotNull UUID parent`) instead of the domain command directly
- HAL-Forms template now marks `parent` as required (`parent*`), preventing null submissions

---

### ISSUE-1: `FamilyGroupController.listFamilyGroups` missing `@HasAuthority` annotation
- **Component**: Backend
- **Symptom**: Regular member (ZBM9500) sees "Administrace" section with "Rodinné skupiny" in sidebar
- **Root cause**: `GET /api/family-groups` lacks `@HasAuthority(Authority.MEMBERS_MANAGE)` — `klabisLinkTo` only gates links based on `@HasAuthority`; runtime `requireMembersManageAuthority()` runs too late. All other admin endpoints (`listTrainingGroups`, etc.) have the annotation.
- **Fix**: Add `@HasAuthority(Authority.MEMBERS_MANAGE)` to `listFamilyGroups` in `FamilyGroupController.java`

### ISSUE-2: `CreateFamilyGroup` POST fails with 400 when parent is null
- **Component**: Backend
- **Symptom**: Creating a family group without selecting a parent results in 400 "Failed to read request"
- **Root cause**: `FamilyGroup.CreateFamilyGroup` requires `parent` (Assert.notNull) but the HAL-Forms template has no `required: true`. Frontend sends `parent: null` when unselected; Jackson's `MemberIdMixin.create(UUID)` receives null and fails deserialization.
- **Fix**: Add `@NotNull` to `parent` field in `CreateFamilyGroup` record so (a) Bean Validation rejects null early with a proper message, and (b) HAL-Forms template gets `required: true`
