## 1. SI card prefill via server-side defaults (N2)

- [x] 1.1 Backend: rozšířit GET registration endpoint (`GET /api/events/{eventId}/registrations/{memberId}`) o volitelný query parametr `new: boolean` (default false). Při `new=false` zachovat stávající chování. Při `new=true`: NEIGNOROVAT `{memberId}` — ověřit oprávnění principálu zakládat registraci pro tento memberId (typicky `principalMemberId == memberId`; jinak 403). Pak místo lookupu existující registrace načíst `Member.siCardNumber` cílového `{memberId}` a vrátit "defaults" payload (siCardNumber v template property `value`, ostatní pole prázdná/default).
- [x] 1.2 Backend: na detailu eventu publikovat affordance `registerForEvent` (nebo navigační link na nový registration form) tak, aby cílila na URL s `?new=true`. Ověřit že odkaz se zobrazuje pouze pro auth uživatele, který ještě není přihlášen na daný event.
- [x] 1.3 Backend test (controller): `new=true` pro `{memberId}` == principal vrátí 200 OK s siCardNumber předvyplněným z profilu; `new=false` na neexistující registraci vrátí 404; `new=true` pro cizí `{memberId}` (bez oprávnění) vrátí 403; nepřihlášený → 401.
- [x] 1.4 Backend test (integrační): user bez siCardNumber v profilu → `new=true` vrátí prázdnou hodnotu (žádná chyba).
- [x] 1.5 Frontend ověření: registrační form přes affordance `registerForEvent` zobrazí SI číslo předvyplněné. Hodnotu lze přepsat a submit pošle přepsanou hodnotu.
- [x] 1.6 Run backend tests via test-runner. (2420/2421 pass; 1 unrelated pre-existing failure in EventManagementE2ETest.)

## 2. Action button color variants (K2)

- [x] 2.1 Add `actionVariants.ts` utility under `frontend/src/utils/` with a record mapping link relation names to variant tokens (`primary`, `destructive`, `warning`, `neutral`); include explicit fallback to `neutral` for unknown relations
- [x] 2.2 Audit existing affordance link relations used on the events list — list them and assign variants per the spec (`register-for-event` → primary, `publish-event` → primary, `unregister-from-event` → warning, `cancel-event` → destructive, `update-event` → neutral, `sync-from-oris` → neutral, etc.)
- [x] 2.3 Update the action button renderer in `KlabisTable` (or the events list table) to look up the variant from the mapping and pass it as a prop to the underlying `Button` component
- [x] 2.4 Ensure the `Button` component supports the four variants via theme tokens (Tailwind classes like `bg-primary`, `bg-destructive`, `bg-warning`, `bg-neutral`); add the missing variants if any are not defined
- [x] 2.5 Frontend tests: render an events row with each action and assert the button has the expected variant class
- [x] 2.6 Visual / Storybook test: render each variant in isolation to verify color tokens

## 3. End-to-end verification

- [ ] 3.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 3.2 Browser test (member with SI in profile): open events list, click "Přihlásit se" on an open event — expect SI card field prefilled with profile value, expect button visual is primary
- [ ] 3.3 Browser test (member without SI in profile): open events list, click "Přihlásit se" — expect SI card field empty
- [ ] 3.4 Browser test (admin): scan the events list rows — confirm "Zrušit akci" stands out red, "Odhlásit se z akce" stands out warning, "Přihlásit se" / "Publikovat" stand out primary, "Upravit" / "Synchronizovat" are neutral
- [ ] 3.5 Spot-check accessibility: button color contrast ratios for primary / destructive / warning meet WCAG AA on the application theme (manual or via Lighthouse)

## 4. Documentation

- [ ] 4.1 Update `actionVariants.ts` with a comment listing the policy from the spec (so future developers adding affordances know how to map them)
- [ ] 4.2 Sync spec changes into `openspec/specs/event-registrations/spec.md` and `openspec/specs/events/spec.md` after archiving
