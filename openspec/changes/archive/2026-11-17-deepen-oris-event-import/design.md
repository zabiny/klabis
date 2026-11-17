## Context

`useOrisEventImport` returned 17 fields and `ImportOrisEventModal` declared 16 props that mirror them field for field; `EventsPage` owned the open state, picked the template (`importEventsBatch ?? importEvent`) and connected the two with a spread. The dialog is the only consumer of the hook, and the page is the only consumer of the dialog.

The flow itself has three inputs it does not own: the ORIS feed (`GET /api/oris/events?region=…`, not a HAL link), the import affordance (`target`, `method`, the multi property's name and `max`), and the `['authorized']` cache invalidation that every successful mutation already performs.

## Goals / Non-Goals

**Goals:**
- One interface for "import events from ORIS": the button the user sees.
- The page stops naming templates, keeping open state, and passing 16 props.
- One test file for the flow, all of it through the same interface callers use.

**Non-Goals:**
- Changing the ORIS feed endpoint, its query parameter or its response handling.
- Changing the submission contract derived from the affordance (`target`, `method`, body key, `max`).
- Touching `BulkSyncOrisModal` (a different flow).
- Replacing the hardcoded `/api/oris/events` URL with a HAL link — there is none today.

## Decisions

**D1 — The module owns the trigger, the open state and the dialog.** Interface:

```ts
interface ImportOrisEventsButtonProps {
    resource: HalResponse;   // the events resource the page is rendering
}

export function ImportOrisEventsButton({resource}: ImportOrisEventsButtonProps): ReactElement | null
```

The page renders it once, unconditionally. When the resource carries no import template the module returns `null`, replacing the page's `{activeImportTemplate && …}` gate for both the button and the dialog. Rejected: keeping the button in the page (the open state would stay outside the module and the module would have two callers — the button and the page).

**D2 — The import template is derived inside the module.** `halResource(resource).template('importEventsBatch') ?? halResource(resource).template('importEvent')` — the same "batch first" rule, now in one place. The page stops knowing the two template names. `halResource` is the existing affordance module; the module adds no catalogue of which resource offers which template.

**D3 — One file.** The logic (region, closed selection set, feed query, submit, result normalisation) becomes a non-exported function in `ImportOrisEventsButton.tsx`, and the two dialog screens (selection and per-event result) become non-exported components in the same file receiving one state object. Nothing is exported but the button. Rejected: a separate internal hook file — it would re-create a module boundary with no second consumer and force the dialog to keep a prop mirror.

**D4 — No `onImported` callback.** `useAuthorizedMutation` invalidates the whole `['authorized']` prefix after every successful mutation, so the events list refetches by itself. A test asserts the list query is invalidated after a batch import; if that ever stops holding, the callback comes back as one prop.

**D5 — The feed query is disabled while the dialog is closed**, exactly as today: the URL is `undefined` until the dialog opens, and the dialog is not rendered until then.

**D6 — Reopening resets the flow.** Region, selection, submit error and the previous result are cleared in the open handler instead of a `useEffect` keyed on `isOpen`; the state lives in the button, so the handler can reset it directly and the effect disappears.

**D7 — Tests concentrate at the module's interface.** `ImportOrisEventsButton.test.tsx` merges what the hook test and the modal test verified separately: the button's presence rule, feed loading/error/empty states, region switching, the selection limit (from `orisIds.max`, default 50), select-all, submission (URL, method, body key), status normalisation and the result screen. The two old test files are deleted with their subjects.

**D8 — `EventsPage.test.tsx` renders the real module.** The hook mock and the `ImportOrisEventModal` mock are removed; the existing `useAuthorizedFetch` / `authorizedFetch` mocks (already present) remain the boundary. The page test keeps one assertion: clicking "Importovat z ORIS" opens the dialog. The test asserting the template object reaches the hook disappears with the hook — the rule it guarded is now the module's own test (D2).

**D9 — Behaviour kept verbatim:** the button label and variant, the dialog title, the fetch states, the "select all" indeterminate state, the shown-of counter, the limit hint, the failure text, the POST fallback, the `orisIds` body-key fallback, and the uppercase normalisation of `status`.

## Risks / Trade-offs

- **A larger single file.** Button, logic and both dialog screens in one file is roughly 350 lines. The alternative reintroduces exactly the prop mirror this change removes; the dialog screens are private components, so the file reads as one module.
- **Reliance on cache invalidation.** Dropping `onImported` makes the flow depend on `useAuthorizedMutation` invalidating `['authorized']`. Pinned by a test; the event list is an authorized query, so it is covered today.
- **The batch-first rule is now invisible from the page.** A third import template added later needs the rule revisited inside the module rather than at the call site — the rule already lived in the page, so this is a move, not new coupling.

## Glossary

| Term | Meaning |
|---|---|
| ORIS import | Bringing events from the ORIS feed into Klabis, in one batch, with a per-event result. |
| Import affordance | The HAL-FORMS template on the events resource that the import submits to; its presence is what makes the button appear. |
| Feed query | `GET /api/oris/events?region=…`, the list of ORIS events offered for import. |
| Module-internal | Code in the module's file that is not exported; callers and tests never see it. |

## Domain and API impact

None. No domain concept, REST endpoint, request/response shape or authorization changes. `GLOSSARY.md` gains an **ORIS event import** entry, because the module is now named after a concept the glossary did not define.
