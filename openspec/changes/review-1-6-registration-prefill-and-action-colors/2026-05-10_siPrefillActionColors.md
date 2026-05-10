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
