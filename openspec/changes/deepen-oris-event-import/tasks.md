## 1. Module skeleton and the presence rule

- [x] 1.1 Red: test `ImportOrisEventsButton` — a resource with no import template renders nothing; a resource with only `importEvent` renders the button; a resource with `importEventsBatch` renders the button
- [x] 1.2 Green: create `components/events/ImportOrisEventsButton.tsx` with the `{resource}` interface, template derivation via `halResource` (batch first), and the trigger button
- [x] 1.3 Green: `GLOSSARY.md` gains the **ORIS event import** entry
- [x] 1.4 Verify: module slice tests green

## 2. Dialog flow

- [ ] 2.1 Red: opening the dialog loads the ORIS feed for the default region and renders loading, error, empty and populated states
- [ ] 2.2 Red: switching region refetches the feed and clears the selection
- [ ] 2.3 Red: the selection limit comes from the affordance's multi property `max`, falls back to 50; toggling cannot exceed it; select-all selects at most the limit and deselects when the limit is reached
- [ ] 2.4 Red: submitting uses the template's `target`, its `method` (POST when absent) and the multi property's name as the body key (`orisIds` when absent); lowercase statuses are normalised to uppercase; the per-event result screen renders; a failed submit shows the failure text
- [ ] 2.5 Red: reopening the dialog clears region, selection, submit error and the previous result
- [ ] 2.6 Green: implement the internal state function and the two dialog screens in the module's file, with no props relayed between them
- [ ] 2.7 Red: a completed batch import invalidates the `['authorized']` queries (events list refetches) with no `onImported` callback
- [ ] 2.8 Verify: module slice tests green

## 3. Page migration

- [ ] 3.1 `EventsPage` renders `ImportOrisEventsButton` once and drops `isImportModalOpen`, the three template variables and the trigger button
- [ ] 3.2 `EventsPage.test.tsx` drops the `useOrisEventImport` and `ImportOrisEventModal` mocks; the real module is rendered and the page test asserts clicking the button opens the dialog
- [ ] 3.3 Delete `hooks/useOrisEventImport.ts`, `hooks/useOrisEventImport.test.ts`, `components/events/ImportOrisEventModal.tsx`, `components/events/ImportOrisEventModal.test.tsx`
- [ ] 3.4 Verify: page and component slice tests green

## 4. Verification

- [ ] 4.1 Full frontend unit suite green
- [ ] 4.2 `npm run build` (type check) and `npm run lint` green
- [ ] 4.3 E2E smoke of `events` and `application-navigation` against the local instance
- [ ] 4.4 `npm run refresh-backend-server-resources` so the backend serves the rebuilt SPA
