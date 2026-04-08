## Context

The member detail page today renders group membership information in two places at once: as HAL links on `member._links` (which nobody on the UI side reads directly) and as full embedded `Section` blocks that re-fetch each group via `HalSubresourceProvider` and display its name, owners, and related data inline. The embedded sections are a holdover from an early pass at the page, when the group detail pages did not yet exist; now that they do, the inline copy is pure duplication.

The member detail page also carries two mockup-era buttons for a finance / transactions feature that does not exist — "Vložit / Vybrat" in the admin view and "Členské příspěvky" in the self-profile view. Neither has a backend endpoint, neither is wired to anything, and both have been sitting in the code (and in the spec) since the initial layout work. They are the most visible piece of cruft on the page.

The backend already emits `trainingGroup` and `familyGroup` HAL links on the member detail response when applicable. It does so unconditionally — any reader who can load the member can also see the two links. This matches the project's current stance that group membership is not sensitive information.

See `proposal.md` for the motivation and `specs/members/spec.md` for the requirement-level delta.

## Goals / Non-Goals

**Goals:**
- Replace the embedded group information sections with compact navigation buttons that jump to the existing group detail pages.
- Remove the dead "Vložit / Vybrat" button from the code so the action bar only shows things that actually work.
- Make the new buttons strictly HAL-link-driven — visible whenever the link is present, hidden otherwise, in every view.
- Keep the specification honest about intent: the finance-related buttons remain described in the spec as a forward-looking marker so that a future finance proposal lands where it was always planned.

**Non-Goals:**
- Implementing any finance / transactions functionality.
- Reworking how the backend decides which HAL links to emit on the member detail. The current "emit for any reader" behavior is retained.
- Removing the `HalSubresourceProvider` infrastructure. It is used by other pages and stays.
- Introducing a new spec capability or a new requirement block. The change is a single scenario added to the existing `Member Detail Page Layout` requirement.
- Visual redesign of the member detail page beyond the button changes described in this proposal.

## Decisions

### Decision 1: Buttons are gated by HAL links, not by templates or roles

**Choice:** Each new button checks exactly one thing: is the corresponding HAL link (`trainingGroup` / `familyGroup`) present on the member detail response? If yes, render the button. If no, don't. No template check, no role check, no current-user comparison.

**Why:** The backend already owns the decision "does this member belong to a group?". By reading it back from the HAL response, the frontend stays free of duplicated business logic and automatically adapts if the backend's rules change. It also matches the pattern used elsewhere in the app where action visibility is driven by server-emitted metadata.

**Alternatives considered:**
- **Check a template** (e.g. `_templates.viewTrainingGroup`). Rejected — there is no such template, and adding one would be overkill: the action is a plain GET navigation, not a form submission.
- **Check `member.trainingGroupId` / `member.familyGroupId`** fields on the response body. Rejected — those fields would carry half of the information (an ID but no URL), and the frontend would have to reconstruct the navigation path by hand. HAL links give us the full URL directly.
- **Always render the button and disable it when the link is missing.** Rejected — a disabled button communicates "this action exists but is temporarily unavailable", which is not true. If the member isn't in a group, the action simply doesn't apply.

### Decision 2: Both buttons are available in every view (admin, self, other)

**Choice:** The button-rendering condition is only "is the HAL link present". There is no branching on whether the current user is an admin, the member themselves, or somebody else. If the backend emits the link, the button shows up.

**Why:** Group membership is not sensitive data, and a member should be able to jump to "my training group" from their own profile page just as easily as an admin can jump from another member's profile. Splitting the behavior per view would create three places to maintain the same rule and invite drift.

The backend already emits the links uniformly for any reader of the member detail, so no backend change is needed to support this.

**Alternatives considered:**
- **Admin-only buttons.** Rejected — a regular member looking at their own profile has a legitimate reason to jump to their training group (see trainer contact info, fellow trainees). Blocking self-navigation would be unhelpful.
- **Different buttons for admin vs. self.** Rejected — same destination, same affordance, no reason to differ.

### Decision 3: New `labels.links` namespace for link-following button labels

**Choice:** Introduce a new top-level namespace `labels.links` in `frontend/src/localization/labels.ts` with two keys:

```ts
links: {
    trainingGroup: 'Tréninková skupina',
    familyGroup: 'Rodina',
}
```

The button labels come from this namespace.

**Why:** The existing label namespaces each have a clear semantic:
- `buttons` — generic action verbs (submit, save, cancel, edit, close).
- `templates` — HAL-Forms template titles (updateMember, deleteGroup, suspendMember).
- `fields` — form field labels.
- `sections` — page-section headings.

A button that **follows a HAL link** to another page fits none of those cleanly. Putting the labels in `buttons` would overload the namespace with destination-specific strings; putting them in `templates` would be wrong because there is no HAL-Forms template involved. A new `links` namespace draws a clean line and leaves room for future link-following buttons (for example, a "Permissions" button would move here during a later cleanup).

"Rodina" is intentionally shorter and more human than "Rodinná skupina" — it matches the review note verbatim and reads better on a button.

**Alternatives considered:**
- **Reuse `labels.sections.trainingGroup` / `labels.sections.familyGroup`.** Rejected — those are in all-caps (`"TRÉNINKOVÁ SKUPINA"`, `"RODINNÁ SKUPINA"`) because they are used as section headings. Unsuitable for buttons.
- **Put the labels in `buttons`.** Rejected — bloats a namespace whose scope is generic verbs.

### Decision 4: Delete dead code, keep dead spec text

**Choice:** In `MemberDetailPage.tsx`, delete the "Vložit / Vybrat" button, its `Banknote` icon import, the two helper components (`TrainingGroupInfo`, `FamilyGroupInfo`), and their local types. In `openspec/specs/members/spec.md`, leave the scenarios that mention "Vložit / Vybrat" (admin view) and "Členské příspěvky" (self-profile view) completely untouched.

**Why:** Dead code and dead spec text have opposite roles. Dead code rots, misleads reviewers, and produces confusing test failures when somebody refactors near it. Dead spec text is free to keep around as long as it's intentional — and in this case it is: it serves as a checklist for the finance-module proposal that will eventually land. When the finance module is built, its proposal will re-enable the buttons and connect them to real endpoints, and the spec will already describe where they belong.

This creates a deliberate, documented gap between the spec and the code. The gap is explicitly called out in `proposal.md` so that anyone reviewing the difference understands it is not an oversight.

**Alternatives considered:**
- **Delete the dead spec scenarios too.** Rejected — loses the roadmap marker and risks the finance-module proposal rediscovering the layout from scratch.
- **Keep the dead button in the code behind a feature flag.** Rejected — feature flags for UI that has no implementation are an anti-pattern. The button would still do nothing whether the flag is on or off.
- **Replace the button with a placeholder "coming soon" state.** Rejected — users don't need to be reminded about a feature that has no ETA, and it clutters the action bar with non-actionable UI.

## Risks / Trade-offs

- **Risk:** A future reviewer sees the spec describe "Vložit / Vybrat" and assumes the absence of the button in the UI is a regression. **Mitigation:** The `proposal.md` for this change explicitly documents the intentional gap, and the spec scenarios themselves can be followed back to the archived change proposal. Future code reviewers should read the most recent archived member-related change before concluding that a button is missing.

- **Risk:** The `trainingGroup` / `familyGroup` HAL links are emitted for every reader of the member detail, including readers who may not have permission to see the destination group in full detail. **Mitigation:** This is the current backend behavior and matches the product decision that group membership is public within the club. If the group detail page itself enforces further access rules, the user may land on a 403 after clicking — this is an acceptable outcome because no sensitive data from the member page is leaking.

- **Risk:** The new `labels.links` namespace might feel over-engineered for just two entries. **Mitigation:** The cost of one more namespace is negligible, and establishing it now means the next "link-following button" (e.g. a permissions link, a club-level navigation shortcut) has an obvious home and does not trigger another naming discussion.

- **Trade-off:** Removing dead code while leaving dead spec text is non-obvious and needs to be explicitly justified every time. Accepted — the roadmap value of the spec text outweighs the small cognitive overhead, and the discipline of calling it out in proposals keeps the intent visible.
