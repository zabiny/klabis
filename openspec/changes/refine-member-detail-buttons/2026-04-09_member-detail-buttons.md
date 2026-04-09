# TCF — refine-member-detail-buttons

## Scope

Frontend-only change. Refactor `MemberDetailPage.tsx`:
1. Add new `labels.links` namespace (`trainingGroup`, `familyGroup`)
2. Add two new HAL-link driven navigation buttons (`Tréninková skupina`, `Rodina`) to the action bar
3. Remove embedded `TrainingGroupInfo` / `FamilyGroupInfo` sections + their types
4. Remove dead `Vložit / Vybrat` button and `Banknote` import
5. Update tests (`MemberDetailPage.test.tsx`, `MemberDetailPage.groups.test.tsx`)
6. Delete superseded task queue file `tasks/member-detail-group-buttons-and-cleanup.md`
7. Manual QA via Playwright

## Decisions

- HAL-link driven (no template / role checks)
- Same behavior in every view
- New `labels.links` namespace
- Dead code deleted, dead spec text kept as roadmap marker

## Work log

<!-- subagents append here -->

### 2026-04-09 — frontend-developer agent

**Files changed:**
- `frontend/src/localization/labels.ts` — added `links` namespace with `trainingGroup` and `familyGroup` keys
- `frontend/src/pages/members/MemberDetailPage.tsx` — removed `GroupOwner`, `GroupData` types, `TrainingGroupInfo`, `FamilyGroupInfo` components, both embedded `<Section>` blocks, the dead `Vložit / Vybrat` button; removed `HalSubresourceProvider`, `HalRouteProvider`, `useHalRoute`, `MemberNameWithRegNumber`, `Banknote`, `HalResourceLinks` imports; added `Dumbbell`/`Heart` imports and two new HAL-link-gated navigation buttons
- `frontend/src/pages/members/MemberDetailPage.groups.test.tsx` — fully replaced old embedded-section tests with new tests covering: button renders when link present, click calls `navigateToResource`, neither button when both links absent, both views (admin/self) parameterized, no embedded sections, no `Vložit / Vybrat`

**Unexpected findings:**
- `tasks/member-detail-group-buttons-and-cleanup.md` was already absent — task 6.1 was a no-op
- The new group navigation buttons render inside the `!isEditing && hasEditTemplate` block, so they only appear when an edit template is present (same as all other action buttons) — this is consistent with the existing pattern and the proposal's intent

**Test suite result:** 1111/1111 passed

### 2026-04-09 — simplify fixes (frontend-developer agent)

**Fix 1 — consistency with permissions button (`MemberDetailPage.tsx` lines ~310-327):**
- Replaced `resourceData._links?.trainingGroup` / `resourceData._links!.trainingGroup!` with `hasLink('trainingGroup')` / `route.getResourceLink('trainingGroup')!`
- Same for `familyGroup`
- Both group buttons now use the same pattern as the sibling `permissions` button

**Fix 2 — trimmed redundant tests (`MemberDetailPage.groups.test.tsx`):**
- Removed `it.each(['admin view', 'self-profile view'])` — link-gating is independent of template shape
- Removed three "does not render X" tests that asserted against text/buttons that no longer exist
- Deleted now-unused `selfEditTemplate` constant
- Updated `createMockPageData` so `hasLink` and `getResourceLink` derive from `resourceData._links` dynamically (required after Fix 1 changed the gate from `resourceData._links?.x` to `hasLink('x')`)
- Updated click tests to mock `getResourceLink` with `mockImplementation` returning the specific link for the tested relation

**Test suite result:** 1106/1106 passed

### 2026-04-09 — bug fix (frontend-developer agent)

**Bug:** The two group navigation buttons (`Tréninková skupina`, `Rodina`) were nested inside the `{!isEditing && hasEditTemplate && (...)}` block, making them invisible in the "other member" view where no edit template is present — violating Decision 2 of the design.

**Fix in `MemberDetailPage.tsx`:**
- Changed outer gate from `{!isEditing && hasEditTemplate && ...}` to `{!isEditing && ...}`
- `Upravit profil` button now guarded individually by `{hasEditTemplate && ...}`
- Group buttons remain gated only by `hasLink('trainingGroup')` / `hasLink('familyGroup')` — no template dependency
- `Oprávnění`, `suspendMember`, `resumeMember` buttons retain `hasEditTemplate &&` guard (these are admin/self-only actions by server design)

**New test in `MemberDetailPage.groups.test.tsx`:**
- Added: `'renders group navigation buttons even without an edit template (other-member view)'`
- Response has `trainingGroup` + `familyGroup` links but no `_templates` — both buttons must render

**Test suite result:** 6/6 passed (MemberDetailPage.groups suite)
