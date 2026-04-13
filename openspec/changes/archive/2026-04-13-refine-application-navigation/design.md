## Context

The application has a single flat main menu driven by HAL links that come back from `GET /api`. Each module contributes a `RepresentationModelProcessor<EntityModel<RootModel>>` that adds its own link to the root response, and `klabisLinkTo` automatically withholds the link from users who lack the target endpoint's authority. The frontend reads `response._links`, skips system rels (`self`, `curies`), and turns every remaining entry into a flat `NavigationItem`. The desktop sidebar and the mobile bottom-nav both iterate that flat list.

On the "post-create navigation" side, every create endpoint already returns `ResponseEntity.created(...)` with a `Location` header pointing at the new resource. The `useAuthorizedMutation` hook, however, only parses and returns the JSON body, so the header is lost before any caller can react to it. One page (`MemberRegistrationPage.tsx`) tries to compensate by reading `responseData._links.self`, but the backend never puts a body on create responses, so that fallback silently fails and the user ends up on `/members` anyway.

See `proposal.md` for motivation and `specs/application-navigation/spec.md` for the resulting requirements.

## Goals / Non-Goals

**Goals:**
- Give the desktop sidebar a two-section structure (main + administrative) without changing the backend HAL contract.
- Keep the rule "section appears only when it has at least one visible item" completely driven by the HAL links the current user is authorized to receive.
- Turn "after creating something, open its detail" into a uniform behavior that works for every `POST` endpoint without per-page code.
- Remove dead code in `MembersPage.tsx` that no longer has a purpose after this change.

**Non-Goals:**
- Reworking mobile navigation. Mobile keeps a flat main-only bottom-nav. Admin tasks on mobile remain an explicit gap until a later change extends the dashboard or introduces an overflow menu.
- Moving any of the existing admin pages to new URLs or to subroutes.
- Changing what the backend sends. The link hierarchy stays flat on the wire; "sections" are a pure frontend presentation concern.
- Auto-navigating after non-POST submissions. `PUT`, `PATCH`, and `DELETE` keep their current "invalidate and stay" behavior.
- Supporting dynamic section membership (e.g. "the user picks which section an item goes into"). The whitelist is static and lives in code.

## Decisions

### Decision 1: Section membership is a frontend whitelist, not a backend annotation

**Choice:** Add a `section: 'main' | 'admin'` field to `NavigationItem` and compute it inside `useRootNavigation.ts` via a static mapping:

```ts
const ADMIN_RELS = new Set(['training-groups', 'category-presets', 'family-groups'])
```

Anything not in the set is `'main'`. The backend continues to return a flat `_links` object from `GET /api`.

**Why:** The decision "which items belong to the admin section" is a pure UI composition choice, not a property of the resource. Teaching the backend about UI sections would either require a custom link relation registry or a non-standard property on every link, both of which would couple presentation concerns to API shape and drag every other HAL consumer (tests, API docs, potential third-party clients) into the decision. A three-line frontend set is cheaper and easier to change.

**Alternatives considered:**
- **Add a `section` attribute to each HAL link** via Spring HATEOAS customizer. Rejected — non-standard, complicates link serialization, and reverses the direction of flow (API dictating UI).
- **Split `GET /api` into `/api` and `/api/admin`** with two separate `RootModel`s. Rejected — invents a new URL for a cosmetic UI concern and adds a second navigation fetch.
- **Derive section from URL prefix** (e.g. anything starting with `/api/admin/...` is admin). Rejected — forces a URL migration that would touch every admin controller and every existing test.

### Decision 2: Admin section is rendered only when it has at least one item

**Choice:** The `Layout.tsx` desktop sidebar filters the navigation items into `mainItems` and `adminItems` and renders the `Administrace` heading + list **only** when `adminItems.length > 0`. No placeholder text, no greyed-out heading, no empty state.

**Why:** A regular member with no admin authorities already receives no admin links from the backend. Rendering an empty "Administrace" heading would be noise at best and a false positive ("I should be able to click something here") at worst. The rule mirrors how the main section already behaves — if the backend returns no links at all, the current code shows a "no menu available" message, not an empty heading.

**Alternatives considered:**
- **Always render the heading, hide individual items.** Rejected — leaks the concept "admin section exists" to users who have no business seeing it.
- **Show disabled items with a tooltip explaining why.** Rejected — admin operations are authority-gated and users cannot request upgrade in this UI; a tooltip would just be clutter.

### Decision 3: Mobile bottom-nav is deliberately main-only

**Choice:** The mobile bottom-nav iterates `mainItems` (same filter as the desktop sidebar's main section) and renders nothing else. Admin items are reachable only via the desktop sidebar. The mobile dashboard is **not** extended in this change.

**Why:** The bottom-nav has physical room for four or five items and already hosts the everyday destinations (home, calendar, events, members, groups). Adding a second tier would either mean squeezing things down to unusable sizes or hiding items behind an overflow affordance — both of which exceed the scope of this proposal. Not rendering admin items on mobile is a conscious trade-off we are making explicit in the spec rather than pretending it is a bug for later.

**Alternatives considered:**
- **Bottom-nav "more" overflow button.** Rejected for this change — requires a new UI pattern (overflow menu, keyboard and a11y handling, etc.) that is worth its own design conversation. The spec leaves the door open to adding it later.
- **Extend the dashboard with admin quick-actions.** Also rejected for this change. The user explicitly ruled it out and asked that the dashboard remain untouched while the mobile gap is acknowledged.
- **Bottom-nav shows everything (status quo).** Rejected — the whole point of the change is to separate everyday from administrative destinations, and doing so only on desktop while leaving mobile flat sends mixed signals.

### Decision 4: `useAuthorizedMutation` returns `{ data, location }`, callers destructure

**Choice:** Change the `mutationFn` return type from `unknown` to `{ data: unknown, location: string | null }`. `location` is the raw `Location` response header, or `null` when the backend did not send one. Every existing `onSuccess` callback that previously read the bare `data` value now reads `.data` from the result. `HalFormDisplay` is updated to read both.

**Why:** The cleanest contract for "tell me what the server said about this request" is a typed object. The alternative of squirreling the header into a React context or a ref would make the data flow implicit, and extending the existing `mutate` options with an extra callback would give us two places where success logic lives. A single destructured return value lets each caller decide for itself whether it cares about the header, and the TypeScript compiler catches missed call sites.

This is a breaking change for the hook's internal consumers, but the consumer count is small and every call site is in the same repository.

**Alternatives considered:**
- **Return just `Response` from `mutationFn`** and let each caller parse it. Rejected — every existing caller would need to add boilerplate to parse JSON and handle empty bodies. The hook's whole purpose is to hide that.
- **Add an `onSuccessWithHeaders` callback option.** Rejected — two success callbacks is one too many, and the new option would be easy to miss when writing new code.
- **Store the last `Location` header on a context/ref.** Rejected — implicit data flow between a hook and unrelated consumer components is exactly the thing that bites later.

### Decision 5: Auto-navigate after `POST` with a `Location` header, in `HalFormDisplay`

**Choice:** `HalFormDisplay.handleSubmit` reads `result.location` from the mutation. If the template's HTTP method is `POST` and `location` is non-null, after cache invalidation and the toast, it calls `navigate(extractNavigationPath(location))`. Non-`POST` methods ignore the location entirely and behave as today.

**Why:** The behavior is natural for create forms and a no-op for update/delete forms because the backend never sets `Location` on those responses. Anchoring the rule on "HTTP method is POST AND Location is present" means the frontend does not need to know which templates are "create" templates — the backend already tells us by filling in (or omitting) the header. New create endpoints get the behavior for free. `extractNavigationPath` already exists and handles both absolute URLs and `/api`-prefixed paths.

The decision deliberately centralizes the navigation in `HalFormDisplay` rather than each caller because:
- The alternative (each page wiring up navigation by hand) is what `MemberRegistrationPage.tsx` tried and failed at, and what currently produces the "stays on the list" behavior on every other page.
- Pages that genuinely want a different post-submit destination (rare — likely zero today) can still override via the existing `onSubmitSuccess` prop.

**Alternatives considered:**
- **Navigate based on the template name prefix** (`create*`). Rejected — couples navigation to a naming convention and breaks when a POST template is named differently.
- **Navigate based on a new `navigateOnSuccess` prop.** Rejected — callers would have to opt in everywhere, which re-creates the current "every page forgets to opt in" problem.
- **Navigate inside `useAuthorizedMutation` itself.** Rejected — the hook should not know anything about routing.

### Decision 6: Delete `createFamilyGroup` wiring from `MembersPage.tsx`

**Choice:** Remove the `createFamilyGroupModal` state, the button, and the modal block from `MembersPage.tsx`. The family-group create flow lives on the family-groups list page inside the Administrace section, and `GET /api/members` does not (and will not) emit a `createFamilyGroup` template.

**Why:** The code is unreachable today. Keeping dead code around "just in case we flip it back on" pollutes the file, invites misleading test cases, and creates a false impression in reviewers that this is a supported flow. The `familyGroups` capability decides where family groups are created, and the answer is "from the family groups page" — not "from the members list".

## Risks / Trade-offs

- **Risk:** Making `useAuthorizedMutation` return a tuple-shaped object is a breaking change for every existing call site. **Mitigation:** The hook is a thin internal wrapper; call sites are all in the same repo and caught at compile time by TypeScript. The PR will touch each one explicitly.

- **Risk:** Auto-navigating after any `POST` with a `Location` could surprise a caller that intended to stay on the list. **Mitigation:** `onSubmitSuccess` remains an escape hatch — a caller that wants the old behavior can opt out in its own wrapper component. No current caller appears to need this, but the escape hatch is free.

- **Risk:** Deleting the `createFamilyGroup` wiring from `MembersPage` means if someone later wants it back, they have to reintroduce the state, modal, and backend template emission together. **Mitigation:** Accepted — restoring a dead feature from git history is a well-understood operation, and leaving half-built UI in the tree is worse than a small round trip through version control.

- **Trade-off:** Mobile users have no path to admin pages at all after this change. Accepted, documented in the spec as an explicit gap, and called out in the proposal. A later iteration can add a mobile-specific affordance without having to revisit this proposal's decisions.

- **Trade-off:** Section membership is hard-coded in the frontend. Adding a new admin module will require a one-line edit to `ADMIN_RELS`. Accepted — this is shorter, clearer, and more discoverable than any alternative that tries to make the set configurable.
