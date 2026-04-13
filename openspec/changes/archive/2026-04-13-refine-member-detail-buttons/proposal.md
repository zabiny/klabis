## Why

Two problems on the member detail page are worth fixing together:

- **Embedded group sections are too heavy.** When a member belongs to a training group or a family group, the detail page currently embeds a whole `Section` block per group — fetching the group detail via `HalSubresourceProvider` and rendering its name, owners, and related data inline. This duplicates what the dedicated group detail pages already show, bloats the member detail page, and creates two places where the same group rendering can drift apart. The intent of those sections has always been "let me jump to the related group", not "let me read its data here".
- **Dead "Vložit / Vybrat" button clutters the action bar.** The button was placed on the page as a mockup-era placeholder for a future finance / transactions feature. The backend has no corresponding HAL link or API, and clicking the button does nothing. The same is true of the "Členské příspěvky" button in the self-profile variant. Both are remnants of a planned-but-unbuilt finance module.

This proposal replaces the embedded sections with compact navigation buttons driven by the HAL links the backend already emits, and removes the dead finance-related buttons from the page. Crucially, the **specification keeps mentioning the finance-related buttons** as an intentional roadmap marker: when the finance module is eventually built, the spec will already describe where those buttons belong, and a future proposal can reintroduce them without rediscovering the layout.

## What Changes

- **Embedded "Tréninková skupina" and "Rodinná skupina" sections are removed from the member detail page.** The page no longer fetches group subresources inline via `HalSubresourceProvider`.
- **Two new navigation buttons replace them in the action bar:** "Tréninková skupina" (Dumbbell icon) and "Rodina" (Heart icon). Each button is rendered exclusively when the corresponding HAL link (`trainingGroup` or `familyGroup`) is present on the member detail response. The backend already emits these links unconditionally for readers of the member detail; no backend change is required.
- **Both buttons are available in every member detail view** — admin view, self-profile view, and any other variant — because the gate is the presence of the HAL link, not a client-side role check or template check.
- **Dead "Vložit / Vybrat" button is removed from the code** of `MemberDetailPage.tsx` along with its `Banknote` icon import and the now-unused helper components `TrainingGroupInfo` / `FamilyGroupInfo` and their local types. The dead code is deleted entirely; no conditional fallback is left behind.
- **The specification deliberately keeps describing "Vložit / Vybrat" and "Členské příspěvky" as intended buttons.** The intent is to preserve a visible roadmap marker for the not-yet-implemented finance module. A future proposal that introduces the finance module will implement those buttons and connect them to real backend endpoints; at that point, the spec will already describe them correctly. Until then, the spec and the code are deliberately out of sync, and this proposal documents that gap.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `members`: add a new scenario to `Requirement: Member Detail Page Layout` describing how group-navigation buttons appear on the member detail page based on the presence of HAL links.

## Impact

**Backend:**
- No changes. Both HAL links (`trainingGroup` and `familyGroup`) are already emitted by existing `RepresentationModelProcessor<EntityModel<MemberDetailsResponse>>` beans in the training group and family group modules. The links are emitted for any reader who can load the member detail, consistent with the decision that group membership information is not sensitive and every reader who can see the member should be able to navigate to the group the member belongs to.

**Frontend:**
- `frontend/src/pages/members/MemberDetailPage.tsx`:
  - Delete the `TrainingGroupInfo` and `FamilyGroupInfo` helper components (lines ~27–55).
  - Delete the `GroupOwner` and `GroupData` type aliases (lines ~24–25).
  - Delete the two `<Section>` blocks that wrap those helpers (lines ~398–412).
  - Delete the "Vložit / Vybrat" `Button` block in the action bar (lines ~346–351).
  - Add two new buttons to the action bar, placed alongside "Oprávnění": a "Tréninková skupina" button rendered when `member._links?.trainingGroup` is present, and a "Rodina" button rendered when `member._links?.familyGroup` is present. Both use `variant="secondary"`, the `Dumbbell` and `Heart` icons respectively, and `route.navigateToResource(link)` for navigation.
  - Remove the `Banknote` import. Remove `HalSubresourceProvider`, `HalRouteProvider`, `useHalRoute`, `MemberNameWithRegNumber` imports if they become unused after the section deletion (they are used nowhere else in this file).
  - Add `Dumbbell` and `Heart` to the `lucide-react` imports.
- `frontend/src/localization/labels.ts`:
  - Add a new top-level `links` namespace with two keys: `trainingGroup: 'Tréninková skupina'` and `familyGroup: 'Rodina'`. This namespace is for labels of buttons that follow HAL links (distinct from `templates` which describes HAL-Forms template titles and `buttons` which holds generic action verbs).
- `frontend/src/pages/members/MemberDetailPage.test.tsx` and `MemberDetailPage.groups.test.tsx`:
  - Update/remove assertions about the old inline "Tréninková skupina" and "Rodinná skupina" sections.
  - Add assertions covering: (a) button renders only when the corresponding HAL link is present, (b) clicking navigates via `navigateToResource`, (c) neither button renders when both links are absent, (d) "Vložit / Vybrat" no longer appears anywhere.

**Specs:**
- `openspec/specs/members/spec.md` — add a new scenario to `Requirement: Member Detail Page Layout` named `Group navigation buttons shown when member belongs to a group`. The existing scenarios that mention `"Vložit / Vybrat"` (admin detail view) and `"Členské příspěvky"` (self-profile view) are deliberately left unchanged.

**Out of scope:**
- Implementing the finance module or any `POST /api/.../transactions` endpoints. The dead buttons are removed from the code but their spec mentions remain as a roadmap for a future proposal.
- Any backend change to the emission of `trainingGroup` / `familyGroup` links. The current behavior (emit for any reader of the member detail) is retained deliberately: both links describe non-sensitive membership facts and every reader who can see the member should be able to jump to the group.
- Reintroducing `labels.sections.trainingGroup` / `labels.sections.familyGroup` removal — these labels are still used by `FamilyGroupDetailPage`, `TrainingGroupDetailPage`, and the respective list pages.
- Visual redesign of the action bar beyond inserting the two new buttons and removing the dead one.
