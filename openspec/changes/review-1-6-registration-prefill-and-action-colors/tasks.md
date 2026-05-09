## 1. SI card prefill in registration form (N2)

- [ ] 1.1 Verify that `useCurrentMember()` hook (or equivalent) returns the SI card number — if not, extend the current-user response DTO to expose `siCardNumber` (visible only to the owner via `@OwnerVisible`)
- [ ] 1.2 In the event registration form component, read `siCardNumber` from the current member; pass it as Formik `initialValues.siCardNumber` (fallback to empty string if not present)
- [ ] 1.3 Frontend test: when current member has SI card number, form opens with the value prefilled; when not, field is empty
- [ ] 1.4 Frontend test: overwriting the prefilled value and submitting sends the overwritten value to the API; verify by mocking the registration mutation
- [ ] 1.5 Run frontend tests via test-runner

## 2. Action button color variants (K2)

- [ ] 2.1 Add `actionVariants.ts` utility under `frontend/src/utils/` with a record mapping link relation names to variant tokens (`primary`, `destructive`, `warning`, `neutral`); include explicit fallback to `neutral` for unknown relations
- [ ] 2.2 Audit existing affordance link relations used on the events list — list them and assign variants per the spec (`register-for-event` → primary, `publish-event` → primary, `unregister-from-event` → warning, `cancel-event` → destructive, `update-event` → neutral, `sync-from-oris` → neutral, etc.)
- [ ] 2.3 Update the action button renderer in `KlabisTable` (or the events list table) to look up the variant from the mapping and pass it as a prop to the underlying `Button` component
- [ ] 2.4 Ensure the `Button` component supports the four variants via theme tokens (Tailwind classes like `bg-primary`, `bg-destructive`, `bg-warning`, `bg-neutral`); add the missing variants if any are not defined
- [ ] 2.5 Frontend tests: render an events row with each action and assert the button has the expected variant class
- [ ] 2.6 Visual / Storybook test: render each variant in isolation to verify color tokens

## 3. End-to-end verification

- [ ] 3.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 3.2 Browser test (member with SI in profile): open events list, click "Přihlásit se" on an open event — expect SI card field prefilled with profile value, expect button visual is primary
- [ ] 3.3 Browser test (member without SI in profile): open events list, click "Přihlásit se" — expect SI card field empty
- [ ] 3.4 Browser test (admin): scan the events list rows — confirm "Zrušit akci" stands out red, "Odhlásit se z akce" stands out warning, "Přihlásit se" / "Publikovat" stand out primary, "Upravit" / "Synchronizovat" are neutral
- [ ] 3.5 Spot-check accessibility: button color contrast ratios for primary / destructive / warning meet WCAG AA on the application theme (manual or via Lighthouse)

## 4. Documentation

- [ ] 4.1 Update `actionVariants.ts` with a comment listing the policy from the spec (so future developers adding affordances know how to map them)
- [ ] 4.2 Sync spec changes into `openspec/specs/event-registrations/spec.md` and `openspec/specs/events/spec.md` after archiving
