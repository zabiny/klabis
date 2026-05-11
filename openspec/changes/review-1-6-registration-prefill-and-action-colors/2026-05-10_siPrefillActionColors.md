# Team Coordination File — review-1-6 (SI prefill + action colors)

Started: 2026-05-10
Proposal: review-1-6-registration-prefill-and-action-colors

## Scope
- N2: prefill `siCardNumber` v registračním formuláři z `useCurrentMember`
- K2: sémantické barevné varianty action tlačítek v Events tabulce dle link relation name

## Plan
- Iter 1 (N2): zjistit jestli current-member endpoint vrací siCardNumber; pokud ano, prefill ve formuláři + testy. Pokud ne, backend rozšíření DTO + frontend.
- Iter 2 (K2): centralizovaný `actionVariants.ts` mapping + variant prop na Button v action sloupci tabulky + testy.
- Iter 3: simplify review, code review fixes, all tests pass, commit per iteration.

## Investigation needed (Iter 0)
- najít EventRegistrationForm a jak inicializuje SI field
- najít `useCurrentMember` hook a jeho payload (obsahuje `siCardNumber`?)
- najít kde se v `KlabisTable` renderují action affordance buttony a jak vypadá Button component (existují variants?)
- vyjmenovat aktuálně použité affordance link relations v events list

## Log
- 2026-05-10 — TCF created.
- 2026-05-10 — Explore findings:

### N2 (SI prefill)
- Form rendered via `HalFormDisplay` (frontend/src/components/HalNavigator2/halforms/HalFormsForm.tsx:17-31) — `getInitialValues()` maps template properties → Formik initial values.
- `useCurrentMember` hook DOES NOT EXIST. Current user available only via `useAuth()` from `frontend/src/contexts/AuthContext2.tsx`; `AuthUserDetails` (lines 15-21): firstName, lastName, id, userName, memberId — NO siCardNumber (comes from JWT claims).
- Backend: no `/api/members/me` endpoint. `MemberDetailsResponse` does NOT include `siCardNumber` (member has `chipNumber` — separate concept). `siCardNumber` is field on `EventRegistration` only.
- `RegisterCommand` TS type (frontend/src/api/klabisApi.d.ts:1042) has `siCardNumber: string`.
- Implication: needs backend exposing siCardNumber for current user OR server-side HAL template `value` prefill on `registerForEvent` template.

### K2 (action button variants)
- `frontend/src/pages/events/EventsPage.tsx:52-59` — `ROW_ACTION_BUTTONS` hardcoded mapping (templateName → icon). `renderActionsCell` (lines 117-133) renders buttons by iterating this array and checking `templates?.[name]`.
- Affordance template names in events list: `registerForEvent`, `unregisterFromEvent`, `publishEvent`, `cancelEvent`, `syncEventFromOris`, `updateEvent`.
- `Button` (frontend/src/components/UI/Button.tsx:5-13) variants today: `primary | secondary | danger | ghost | danger-ghost`. No `warning` variant. `danger` uses `bg-error`.
- Tailwind tokens (frontend/tailwind.config.ts:34-67, src/index.css): `primary`, `secondary`, `accent`, `success`, `warning`, `error`, `info` — sufficient.
- Tests: Vitest + RTL; example `frontend/src/components/UI/Button.test.tsx`, `frontend/src/pages/events/EventsPage.test.tsx`.

### Decision (resolved 2026-05-10)
User zvolil: rozšířit GET registration endpoint o query param `new=true`, který vrátí defaults pro novou registraci (siCardNumber předvyplněné z profilu). Affordance pro novou registraci použije new=true.

Authorization: `{memberId}` se NEIGNORUJE. Server ověří, že principal smí zakládat registraci pro tento memberId (default: sám pro sebe → 403 jinak).

Design.md, proposal.md, tasks.md aktualizovány na tento přístup. Specs delta (event-registrations) zůstává — píše user-facing scénáře bez ohledu na implementaci.

## Iterations
- Iter 1: N2 backend (`?new=true` GET registration → defaults + authorization + affordance)
- Iter 2: N2 frontend ověření (zda affordance volá `?new=true` a generic form prefill funguje); pokud potřeba malá frontend úprava na affordance URL handling, doplnit.
- Iter 3: K2 frontend (actionVariants util, Button warning variant, KlabisTable/EventsPage action column variant prop, tests).
- Iter 4: simplify + code review + fixes + commit.

## Log — Iter 1 backend refactor: separate link rel → renamed link rel (2026-05-11)

**Files changed:**
- `events/.../EventController.java` — `EventDetailsPostprocessor` and `EventSummaryPostprocessor`: renamed link rel from `new-registration` to `newRegistration` (camelCase, consistent with affordance template naming convention). No other logic changed — eligibility check, URL construction, and `?newRegistration=true` query param all stay identical.
- `events/.../EventControllerTest.java` — added 3 new tests asserting `$._links.newRegistration.href` (event detail: present for eligible unregistered member, absent when already registered, absent with no member profile); added 1 new test for list item (`$._embedded.eventSummaryDtoList[0]._links.newRegistration.href`).

**Design constraint discovered:** Spring HATEOAS 2.5.1 `HalFormsTemplateBuilder` hard-filters out affordances whose HTTP method is GET (`lambda$findTemplates$3` predicate: `!model.hasHttpMethod(GET)`). A `klabisAfford(methodOn(...).getRegistration(..., true))` affordance therefore NEVER appears in `_templates`. The "separate link rel → HAL+FORMS GET template" swap stated in Design Decision 1 is not achievable without forking Spring HATEOAS or adding a custom serializer. Chosen approach: keep the `_links.newRegistration` navigation link (structurally a link, not a template), rename from `new-registration` to `newRegistration` to align with template naming conventions. Frontend must continue reading it from `_links` rather than `_templates`. **127/127 backend tests pass.**

## Log — Iter 2 frontend verification (2026-05-11)

**Files changed:**
- `frontend/src/pages/events/EventsPage.tsx` — added `newRegistrationUrl` state and `useAuthorizedQuery` for it; modified `renderActionsCell` to show "Přihlásit se" button when `event._links['new-registration']` exists (using the new flow) while falling back to `registerForEvent` template for rows without that link; added modal that renders `HalFormDisplay` using the fetched `editRegistration` template + resource data.
- `frontend/src/pages/events/EventsPage.test.tsx` — added `new-registration prefill flow` test suite (4 tests): button visible, prefilled siCardNumber, empty SI field, submit with overwritten value.

**Flow implemented:**
1. User clicks "Přihlásit se" → `newRegistrationUrl` set to `event._links['new-registration'].href`
2. `useAuthorizedQuery` fetches that URL (GET with `?newRegistration=true`) — returns defaults resource with `editRegistration` PUT template whose `siCardNumber.value` is prefilled from member's chip number
3. Modal opens with `HalFormDisplay`; `pathname` is set to the template's target path so `useHalFormData`'s `shouldFetchTargetData` returns false and uses the already-fetched `resourceData` directly (no double-fetch)
4. Formik picks up `siCardNumber` from `resourceData.siCardNumber` (or from `prop.value` as fallback) — field is editable; submit sends PUT with overwritten value

**Surprise:** The generic `HalFormDisplay` + `useHalFormData` OPTIONS-probe flow would have re-fetched the PUT target URL (without `?newRegistration=true`), losing the prefill. Fixed by passing `pathname = editTemplatePath` to suppress the re-fetch — the already-fetched defaults resource is used directly.

**Tests:** 1342/1342 frontend tests pass (25/25 EventsPage tests, 4 new).

**Note from leader:** The generic form flow would work without code change if the `editRegistration` affordance's target URL included `?newRegistration=true`. Currently backend uses a separate `new-registration` link for the GET and a plain PUT URL as template target — hence the explicit page-level fetch.

## Log — Iter 1 implementation (2026-05-11)

**Files changed:**
- `members/MemberDto.java` — added `chipNumber` field (7th component); added backward-compatible 6-arg ctor delegate.
- `members/application/MembersImpl.java` — `fromMember()` now maps `member.getChipNumber()` to `chipNumber`.
- `events/.../EventRegistrationController.java` — `getRegistration` extended with `boolean newRegistration` query param; when `true`: checks `principalMemberId == memberId` (else 403), loads `Member.chipNumber` via `members.findById()`, returns defaults `RegistrationDto`; all `methodOn` call sites updated to 3-arg form.
- `events/.../EventController.java` — `EventDetailsPostprocessor` and `EventSummaryPostprocessor`: when user is eligible but not registered, adds `new-registration` GET link pointing to `?newRegistration=true`.
- `events/.../EventRegistrationControllerTest.java` — added `NewRegistrationDefaultsTests` nested class with 5 controller slice tests.

**Authorization rule applied:** `principalMemberId == memberId` (self-only for now); 403 for mismatched memberId or null principal, 401 for unauthenticated (handled by security filter before controller).

**siCardNumber source:** `MemberDto.chipNumber()` populated from `Member.getChipNumber()` (members domain field). Cross-module boundary respected via existing `Members.findById()` interface.

**Surprises:** `MemberDto` had no `chipNumber` field — added it as new last component with all existing constructors preserved as delegates to canonical 7-arg form.

## Log — Iter 2 frontend fixup: link rel rename + TS cast (2026-05-11)

**Files changed:**
- `frontend/src/pages/events/EventsPage.tsx` — renamed link rel lookup from `links?.['new-registration']` to `links?.['newRegistration']` (line 128) and updated the React `key` prop from `"new-registration"` to `"newRegistration"` to match the backend refactoring that aligned all link/template names to camelCase.
- `frontend/src/pages/events/EventsPage.test.tsx` — renamed mock `_links` key from `'new-registration'` to `'newRegistration'` in `buildEventWithNewRegistrationLink`; added `as any` cast to the two `useAuthorizedQuery.mockReturnValue(...)` calls in `renderWithEvents` and `renderWithCancelableEvent` (TS error: partial mock object not assignable to `UseQueryResult<unknown>` — same pattern already used in `renderWithEventHavingNewRegistrationLink`).

**Tests:** 25/25 EventsPage tests pass; 0 new TS errors.
