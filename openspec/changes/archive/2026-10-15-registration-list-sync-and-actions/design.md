# Design

## Context

Per-event registration rows come from `listRegistrations`; the sync link (`sync`) is added only by the detail postprocessor, with one `findByTarget` lookup. Rows in the events list already carry `editRegistration` and `unregisterFromEvent` templates for a registered user (event self link affordances); the frontend renders `unregisterFromEvent` but ignores `editRegistration`. `EventRegistrationDialog` loads the registration with GET on a link and reads `_templates`, `_links.sync`, `missingInOris` from it. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- Sync indicator in the registrations list with the same visibility rules as the registration detail, without N+1 lookups.
- "Odhlásit" in the edit dialog, "Upravit přihlášku" in the events list, with no new HAL links or templates.

**Non-Goals:**
- Changing `registerForEvent` / `newRegistration` flow.
- Changing authorization of synchronisation state (the sync endpoint already restricts detail to SYNC:MANAGE).

## Decisions

**D1 – Sync link in the list is added per row, without batch loading.** `RegistrationSummaryPostprocessor` does the same `findByTarget` lookup as `RegistrationDetailsPostprocessor` and adds the same `sync` link for enrolled rows (the shared logic is extracted so list and detail use one method). With 200–300 users and tens of events per year a registrations list has at most tens of rows, so the extra indexed lookups are negligible (KISS, see CLAUDE.md design principles). Alternative – batch `findActiveByTargets` passed through `HalResponseContext` or `RegistrationView` – rejected as premature optimisation.

**D2 – Frontend list renders `SyncStatusIndicator mode="icon"`** in the actions cell of `RegistrationsTable` when the row has `_links.sync`, as the events list does. Visibility rules need no frontend logic: the indicator opens the sync state endpoint, which already differentiates headline state from details.

**D3 – "Upravit přihlášku" from `editRegistration` template of the events list row.** The template `target` is the registration resource URL (same URL for PUT submit and GET), so the frontend opens `EventRegistrationDialog` with a link whose `href` is the normalized template target. No backend change; avoids introducing a `registration` link that duplicates the template target. The dialog still loads the registration itself (values, `sync`, `missingInOris`) and the event for context.

**D4 – The edit dialog handles unregistering itself.** In edit mode, when the loaded registration has the `unregisterFromEvent` template, the dialog shows "Odhlásit"; clicking it switches the dialog content to the confirmation of that template (the same generic template-form component the events list uses for its unregister confirmation). After success the dialog closes and calls its existing change callback (`onRegistered`, renamed `onChanged`), through which hosts already refresh. Hosts need no new wiring. Alternative – an `onUnregister` callback delegating to a host-owned confirmation modal – rejected: it adds an API to the dialog and wiring in every host.

## Risks / Trade-offs

- [Renaming `onRegistered` to `onChanged` touches all dialog hosts] → mechanical rename, verified by `npm run build` and existing tests.
- [Edit/unregister template present for rows the user may only see, e.g. EVENTS:REGISTRATIONS managers viewing others' rows] → the button is driven purely by template presence on the loaded registration; the unregister template's target is that row's resource, so behaviour is consistent with the detail page.

## REST API

No new endpoints, parameters, templates or bodies.

- `GET /api/events/{eventId}/registrations` – each item in `_embedded.registrationDtoList` gains `_links.sync` (as in the registration detail) when the registration is enrolled for synchronisation.
- No new HAL+FORMS affordances; `editRegistration` and `unregisterFromEvent` already exist on event list rows and registration detail.

## Domain

No domain changes.

## Glossary

- **Zapojená přihláška (enrolled registration):** a registration matched with an ORIS entry and tracked by synchronisation.
- **Indikátor stavu synchronizace:** icon in a list row that opens the synchronisation state of the record.
