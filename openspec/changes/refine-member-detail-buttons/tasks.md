## 1. Localization — new `links` namespace

- [ ] 1.1 Open `frontend/src/localization/labels.ts`
- [ ] 1.2 Add a new top-level namespace `links` with two keys: `trainingGroup: 'Tréninková skupina'` and `familyGroup: 'Rodina'`
- [ ] 1.3 Make sure the TypeScript type for the labels object picks up the new namespace (no manual type annotation should be needed if the file uses `as const` / inferred types)

## 2. Member detail page — new group navigation buttons (TDD)

- [ ] 2.1 Add failing test to `MemberDetailPage.test.tsx` (or `MemberDetailPage.groups.test.tsx`, whichever already exercises group-related scenarios): given a mocked member detail response with `_links.trainingGroup.href = '/api/training-groups/<uuid>'`, the page renders a button labelled "Tréninková skupina" with a `Dumbbell` icon
- [ ] 2.2 Add failing test: clicking that button calls `navigateToResource` with the `trainingGroup` link (or equivalently navigates the user to `/training-groups/<uuid>`)
- [ ] 2.3 Add failing test: given a mocked response with `_links.familyGroup.href = '/api/family-groups/<uuid>'`, the page renders a button labelled "Rodina" with a `Heart` icon
- [ ] 2.4 Add failing test: clicking the "Rodina" button navigates to `/family-groups/<uuid>`
- [ ] 2.5 Add failing test: given a response with neither link, neither button is rendered
- [ ] 2.6 Add failing test: both buttons appear in **admin view** (full PATCH template present) and in **self-profile view** (self-edit PATCH template present) — parameterize the test so it runs in both view modes and asserts the same rendering rule
- [ ] 2.7 Import `Dumbbell` and `Heart` from `lucide-react` in `MemberDetailPage.tsx`
- [ ] 2.8 In the action bar of `MemberDetailContent` (the `!isEditing && hasEditTemplate` block around the existing "Upravit profil" / "Oprávnění" / "Ukončit členství" buttons), add a new "Tréninková skupina" `Button` that reads `resourceData._links?.trainingGroup` and calls `route.navigateToResource(link)` when clicked. Use `variant="secondary"` and `startIcon={<Dumbbell className="w-4 h-4"/>}`
- [ ] 2.9 Add a new "Rodina" `Button` next to it that reads `resourceData._links?.familyGroup` and navigates the same way. Use `variant="secondary"` and `startIcon={<Heart className="w-4 h-4"/>}`
- [ ] 2.10 Both buttons must be rendered independently of whether the current PATCH template is admin-view or self-edit-view — the only condition is the presence of the corresponding HAL link
- [ ] 2.11 Verify tests 2.1–2.6 pass

## 3. Member detail page — remove embedded group sections

- [ ] 3.1 Delete the `GroupOwner` and `GroupData` type aliases near the top of `MemberDetailPage.tsx`
- [ ] 3.2 Delete the `TrainingGroupInfo` function component
- [ ] 3.3 Delete the `FamilyGroupInfo` function component
- [ ] 3.4 Delete the two `<Section>` blocks in the render path that wrapped these components (the blocks that checked `member._links?.trainingGroup` and `member._links?.familyGroup` and rendered the section headings)
- [ ] 3.5 Remove the `HalSubresourceProvider`, `HalRouteProvider`, `useHalRoute`, and `MemberNameWithRegNumber` imports if they are no longer referenced anywhere in the file
- [ ] 3.6 Run `tsc --noEmit` (or the project's equivalent) and confirm there are no unused-import warnings in the file

## 4. Member detail page — remove dead "Vložit / Vybrat" button

- [ ] 4.1 Delete the "Vložit / Vybrat" `Button` block in the action bar of `MemberDetailContent` (roughly lines 346–351 in the current file)
- [ ] 4.2 Remove the `Banknote` import from `lucide-react` if no longer referenced
- [ ] 4.3 Run `tsc --noEmit` and confirm the file still compiles

## 5. Tests — remove obsolete assertions

- [ ] 5.1 In `MemberDetailPage.test.tsx` and `MemberDetailPage.groups.test.tsx`, remove any assertions about the old inline "Tréninková skupina" or "Rodinná skupina" sections (e.g. assertions that check for a section heading followed by group data)
- [ ] 5.2 Remove any assertions referencing the "Vložit / Vybrat" button
- [ ] 5.3 Keep the assertions about "Upravit profil", "Oprávnění", "Ukončit členství", and other unchanged actions
- [ ] 5.4 Run the full frontend test suite via the `test-runner` agent — all tests must pass

## 6. Clean up the interim task queue file

- [ ] 6.1 Delete `tasks/member-detail-group-buttons-and-cleanup.md` — this proposal fully supersedes it, and leaving the task file in place would produce two competing sources of truth for the same work

## 7. Manual QA walkthrough

- [ ] 7.1 Start the app with `./runLocalEnvironment.sh`
- [ ] 7.2 Log in as admin (`ZBM9000`). Open the detail of a member who is a trainee of a training group — confirm the "Tréninková skupina" button appears in the action bar. Click it and confirm navigation to the correct training group detail page
- [ ] 7.3 As the same admin, open a member who is a parent or child of a family group — confirm the "Rodina" button appears and navigates to the correct family group detail page
- [ ] 7.4 Open a member in both a training group and a family group — confirm both buttons appear side by side and each navigates correctly
- [ ] 7.5 Open a member in neither group — confirm no group navigation button appears, and the rest of the action bar is unchanged
- [ ] 7.6 Confirm there is no "Vložit / Vybrat" button on any member detail view (admin, self, other)
- [ ] 7.7 Log out and log in as a regular member (`ZBM9500`). Open **your own profile** — if you belong to a training group or family group, confirm the corresponding button appears in the action bar and navigates correctly. This verifies the "both views" rule from the spec scenario
- [ ] 7.8 Confirm the embedded "Tréninková skupina" and "Rodinná skupina" sections no longer appear anywhere below the two-column main content
