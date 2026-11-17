## Context

A HAL response already says which actions a resource offers: `_links` carries navigable relations and `_templates` carries HAL-FORMS templates, and the server omits both for callers that may not use them (`src/api/halTypes.ts:1-8`). The frontend, however, resolves that fact in three independent ways and restates per-action texts at every call site.

Today:

- `useTemplateDialog` looks a template up by name (`useTemplateDialog.tsx:51-62`) and is the modal engine for 27 call sites in 13 files.
- `HalFormButton` repeats the same lookup (`HalFormButton.tsx:106`).
- Pages read `_templates` / `_links` directly — 139 and 69 occurrences outside tests.
- `HalFormDisplay` assembles the submit URL from `template.target ?? resourceUrl ?? selfHref ?? route` (`HalFormDisplay.tsx:100,110`), while `templateTarget` holds the same rule for two other callers.
- `toHref`, `asLinkArray` and `templateTarget` have no tests; `templated` filtering exists only inside `HalRouteContext.getLink`.

## Goals / Non-Goals

**Goals:**
- One answer per action: `halResource(resource).template(name)` and `.link(rel)` return everything a call site needs, or `undefined`.
- HAL stays the single source of truth — the UI never holds a register of which resource offers which action.
- Texts resolved by naming convention with fallbacks, so call sites stop restating labels, dialog titles and success messages.
- The submit `href` is computed in one place.

**Non-Goals:**
- A per-resource action catalogue or any other declarative list of actions.
- Presentation policy: modal vs. inline, size, icons, layout stay at the call site.
- Client-side permission logic: authority and server state remain expressed only by the presence/absence of links and templates.
- Collection navigation (`HalLinksSection`, `GenericHalPage`), a shared row-action renderer, query keys, cache invalidation.

## Decisions

**D1 — No catalogue; the resource answers.** A catalogue would duplicate in the UI the knowledge of which resource offers which link, which contradicts HAL: the server already encodes both authorization and state by omitting relations and templates. The module therefore takes the action name from the call site (a literal, as today) and answers from the payload.

**D2 — A pure factory, not a hook.** `halResource(resource)` returns an object with two methods and needs no React context: `href` derives from the resource, so it works inside a `map` over table rows as well as in a page.

**D3 — Two methods, one parameter each.** `template(name)` and `link(rel)` — the same names HAL uses. Each returns `undefined` when the resource carries no such entry; callers gate rendering on that, exactly as `.available` does today.

**D4 — The action carries every fact the catalogue would have.** A template action returns `{name, template, href, label, dialogTitle, successMessage}`; a link action returns `{rel, link, href, label}`. Returning raw `_templates`/`_links` entries would leave the 27 call sites resolving texts and `href` themselves.

**D5 — Texts come from naming convention, with never-breaking fallbacks.**

| Field | Rule |
|---|---|
| `label` | `getTemplateLabel(name) ?? template.title ?? name` |
| `dialogTitle` | `getDialogTitleLabel(name) ?? label` |
| `successMessage` | today's rule: `template.title ? "<title> — úspěšně uloženo" : labels.ui.savedSuccessfully` |
| link `label` | `getLinkLabel(rel) ?? link.title ?? rel` |

`labels.links` exists but is typed and only partly populated (`trainingGroup`, `legalGuardians`); `getLinkLabel(rel)` reads it as `Record<string, string>` and falls back. The success-message rule is reproduced verbatim, not replaced by a generic text, because the toast is user-visible and must not change.

**D6 — `href` is resolved inside the module.** Template `href = template.target ?? selfHref(resource)`; link `href = link.href`. `templateTarget` is deleted and `HalFormDisplay` accepts the resolved `href` instead of rebuilding the chain.

**D7 — `link(rel)` returns the first non-`templated` link.** A relation that carries several links is either a templated link and a concrete one, or a collection; an action needs the concrete target. This is the rule `HalRouteContext.getLink` already implements and `getResourceLink` now delegates to it.

**D8 — Client-state conjunctions stay at call sites.** Three places combine server truth with page state (`status === 'EDITABLE' && template present`, `votingOpen`, `currentGroup`). Without a catalogue there is nowhere to declare them, and they are page policy, not resource facts.

**D9 — `useTemplateDialog` accepts the resolved action.** `open(action, overrides)` replaces `(resource, name)`; `TemplateDialogOptions.resource`, `templateOf`, `available`/`availableOn` leave the hook (call sites use `halResource(...).template(...)`). Portal, `HalFormDisplay` wiring, success/error handling and the existing per-open overrides stay. `size`, `note`, `submitLabel`, `fieldsFactory`, `initialData`, `onSubmitError` remain call-site concerns. Because the action cannot carry the source resource, `TemplateDialogOverrides` also accepts `initialData`: row and late-bound dialogs previously passed the resource that way, and without it their templates — which have no `target` — would open empty.

**D10 — `HalFormButton` resolves through the module.** It keeps its props (`modal`, `label`, `icon`, `variant`, `dialogTitle`, `navigateOnSuccess`, `hideLabel`, `fieldsFactory`); its internal `!resourceData?._templates?.[name]` check becomes an action lookup.

**D11 — `getResourceLink` delegates.** `HalRouteContext.getLink` keeps its shape but resolves through `halResource().link()`, so `templated` filtering has a single implementation and `getResourceLink` keeps behaving identically.

**D12 — The module owns normalisation; the old helpers stay exported.** `toHref` (29 call sites) and `asLinkArray` (13) are used by code unrelated to actions, so they remain exported and gain tests; `templateTarget` has only action-related callers and is deleted. The module's own use of them is internal.

**D13 — Phasing inside one change.** Phase 1: module, `useTemplateDialog`, `HalFormButton`, `HalFormDisplay`, `getResourceLink`, all 27 dialog call sites, seam tests. Phase 2: the six row-action columns. Both share this design; two separate changes would duplicate the proposal and the reasoning.

**D14 — Tests move to the seam.** The new module test covers presence/absence, `href` resolution (`target` present, `target` absent), label/dialog-title/success-message fallbacks, link arrays with `templated` filtering, and `undefined` for a missing resource. `useTemplateDialog`, `HalFormButton` and `HalFormDisplay` tests keep their behaviour assertions and drop template-lookup ones; page tests keep "the action appears when HAL offers it".

## Risks / Trade-offs

- **Text drift during migration.** Every migrated call site must render the identical label, dialog title and toast; the module reproduces the fallback chain, but a call site that passed an explicit string must keep passing it as an override. Each migration task names the text it must preserve.
- **Wide signature change at once.** `useTemplateDialog.open(action)` touches 27 call sites in 13 files; they migrate in one phase so no intermediate state has two shapes.
- **`labels.links` growth.** Migrating row actions adds rel labels to a currently typed map; `getLinkLabel` reads it dynamically, so the map's static typing must be widened without losing key checks for existing entries.
- **`undefined` instead of `available`.** Call sites that used `.available` must switch to a truthiness check on the action; a forgotten site fails tests rather than rendering a dead button, because the old `available` API disappears.

## Glossary

| Term | Meaning |
|---|---|
| HAL action | One thing a resource offers: a `_templates` entry (form) or a `_links` relation (navigation), resolved together with its `href` and texts. |
| Template action | An action backed by a HAL-FORMS template; carries template, target href, label, dialog title and success message. |
| Link action | An action backed by a link relation; carries the link, its href and its label. |
| Action name | The `_templates` key or the `_links` relation the call site asks for. |
| Resolved href | Where the action is submitted or navigated: `template.target ?? self`, or `link.href`. |

## Domain and API impact

None. No domain concept changes and no REST API changes: the SPA reads the same responses and renders the same affordances.
