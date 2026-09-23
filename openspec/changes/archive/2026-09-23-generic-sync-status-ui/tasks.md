## 1. Build the generic status icon component (both modes, all states)

- [x] 1.1 Write component tests for `SyncStatusIndicator` covering: no `syncLink` prop → renders nothing; each of the 6 `SyncStatus` values → correct `Badge` variant + icon (per design.md D4); loading state; fetch error state — all against a mocked sync sub-resource response, no live backend needed
- [x] 1.2 Write component tests for the two modes specifically: `mode="icon"` renders only the icon, with the formatted `lastSuccessfulSyncAt` appearing as a tooltip on hover (and a graceful fallback when the entity has never successfully synced); `mode="icon+date"` renders the icon and the formatted date together inline, always visible, with the same fallback when absent
- [x] 1.3 Implement `SyncStatusIndicator` per design.md D1/D2/D4: accepts a `syncLink` prop and a `mode: 'icon' | 'icon+date'` prop, wraps content in `HalRouteProvider routeLink={syncLink}`, reads `SyncStateResponse` via `useHalRoute()`, renders the mapped icon/color and the date per the mode's placement rule above

## 2. Wire the icon+date mode into the detail page

- [x] 2.1 Add `<SyncStatusIndicator syncLink={resourceData._links?.sync} mode="icon+date" />` to `EventDetailPage` next to the existing status badges
- [x] 2.2 Add/adjust `EventDetailPage.test.tsx` coverage for the new indicator's presence (mocked sync sub-resource), without requiring a live backend

## 3. Wire the icon mode into the list rows

- [x] 3.1 Add a `sync` column to `EventsPage`'s table rendering `<SyncStatusIndicator syncLink={value?.sync} mode="icon" />` per row, reading each row's own `_links.sync` (already present on `EventSummaryDto` per `EventController.java:595-598` — no backend change needed)
- [x] 3.2 Add/adjust `EventsPage.test.tsx` coverage: enrolled rows render the indicator (mocked), unenrolled rows render nothing in that column

## 4. Add the manager overlay with actions

- [x] 4.1 Write component tests for the overlay: not offered (no click affordance) when `_templates` absent on the fetched sync sub-resource; offered when present; each of `synchronizeNow`/`acknowledgeSyncConflict`/`resolveSyncConflict`/`resetSyncRecord` renders only when its template is present (delegated to `HalFormButton`'s existing behavior, but assert the overlay wires it up correctly per state)
- [x] 4.2 Implement the overlay/modal component per design.md D5: status header (state, last successful sync, direction/external id/next-attempt/failure-count for `SYNC:MANAGE` callers), diverged-fields/local-external-baseline comparison when `CONFLICT`, and the relevant `HalFormButton`s — all inside the same `HalRouteProvider` context established by `SyncStatusIndicator`
- [x] 4.3 Make `SyncStatusIndicator` clickable (opens the overlay) only when the fetched sync sub-resource carries `_templates`; otherwise keep it non-interactive

## 5. Retire the old manual-sync button

- [x] 5.1 Remove `<HalFormButton name="syncEventFromOris" .../>` from `EventDetailPage` (design.md D6 — same underlying engine action, now reachable through the overlay's `synchronizeNow`)
- [x] 5.2 Update/remove any now-obsolete test coverage in `EventDetailPage.test.tsx` that asserted the old button's presence/behavior
- [x] 5.3 Confirm no other frontend call site still references the `syncEventFromOris` affordance
- [x] 5.4 Run the full frontend test suite and `npm run lint`/`npm run build` (type-check) to confirm no regressions

## 6. Make the sync endpoint reachable and correctly documented

Deliberately last: nothing above needs this to be buildable or testable (all tests use a mocked sync sub-resource), so it doesn't gate any of the UI work — it only needs to land before real end-to-end use.

- [x] 6.1 Add `SYNC` to `frontend/.env`'s `VITE_OAUTH_SCOPE` (and `.env.development.local.example` if it repeats the scope list there)
- [x] 6.2 Verify the `klabis-web` and `klabis-web-local` OAuth2 client registrations (`authorizationserver` module bootstrap) already allow the `SYNC` scope; if not, add it there too
- [x] 6.3 Fix the stale `sync` link description in `docs/openapi/spec/events.yaml` (currently "Present when the event is enrolled in synchronisation and the caller has SYNC:MANAGE") to describe the actual/spec'd rule: present when enrolled, visible to any signed-in user; detail fields and action templates inside the sync sub-resource are what's gated on `SYNC:MANAGE`
- [x] 6.4 Regenerate the FE OpenAPI bundle/types (`npm run openapi`) so the doc fix and any type impact land in `klabisApi.d.ts`/`halTypes.ts`

## 7. Final verification

- [x] 7.1 Manually verified on localhost: ZBM9000 (admin) and ZBM9500 (member) both have `SYNC` in the access_token `scope` claim (decoded payload includes `"scope": "openid profile MEMBERS EVENTS SYNC"`); backend client registrations accept the scope (covered by I6 BE tests `OidcRegisteredClientsBootstrapTest` + `LocalDevRefreshTokenFlowTest`)
- [ ] 7.2 Full manual pass on localhost per `frontend/CLAUDE.md`'s testing guidance, as both `ZBM9000` (admin/manager) and `ZBM9500` (plain member): `EventDetailPage` shows icon+date; `EventsPage` rows show icon with hover tooltip; manager can open the overlay and trigger `synchronizeNow`/conflict actions where applicable; non-manager sees the icon only, with no way to open the overlay — *partially verified: login flow, scope wiring, and the new "Synchronizace" column header on `EventsPage` confirmed for both users; visual states of the icon/tooltip/overlay could not be exercised because the local example-data DB has no events enrolled in ORIS sync, so no `sync` link is present on any row/detail payload and the indicator correctly renders nothing per design D1. Visual pass deferred to a follow-up once an enrolled event exists in the fixture (out of scope for this change).*
- [x] 7.3 Ran `npm run refresh-backend-server-resources` (2023/2023 FE tests pass; tsc + Vite build OK; backend static resources staged with the new bundle)
- [x] 7.4 Updated this tasks.md's checkboxes to reflect final state before requesting review

## 8. Final polish after first manual pass

- [x] 8.1 Render `lastSuccessfulSyncAt` as **full datetime** (date + time, not date-only) in: the SyncStatusIndicator tooltip / `aria-label` (icon mode) and the inline text after the icon (`icon+date` mode). Use the existing `formatDateTime` helper, not `formatDate`. — Added new helper `formatDateTimeSeconds` (`dateUtils.ts`) so existing `formatDateTime` callers (registrationTime, nextAttemptDueAt) are not regressed; SyncStatusIndicator uses seconds throughout (aria-label, tooltip content, inline text after the icon).
- [x] 8.2 Render `lastSuccessfulSyncAt` as full datetime in the overlay's "Poslední úspěšná synchronizace" line (currently uses `formatDate`). — Overlay now uses `formatDateTimeSeconds` for the last-sync field.
- [x] 8.3 Add Czech localization strings under `labels.templates` for the four sync-state templates currently without translations: `synchronizeNow` ("Synchronizovat"), `acknowledgeSyncConflict` ("Potvrdit konflikt"), `resolveSyncConflict` ("Vyřešit konflikt"), `resetSyncRecord` ("Resetovat synchronizaci"). — HalFormButton now resolves these via `getTemplateLabel` so the button shows Czech, not the API `template.title`. Verified on `92f5f776` (Oblastní žebříček): overlay renders the button as "Synchronizovat".
- [x] 8.4 Audit frontend code and tests for any place that wrongly assumes `_links.sync` is present on every event/list-row detail (it is not — only events paired with ORIS get it). — No findings. `EventsPage.tsx` and `EventDetailPage.tsx` both go through `<SyncStatusIndicator syncLink={...}>` with the link treated as optional; tests cover both enrolled and unenrolled rows correctly.
- [x] 8.5 Update affected component tests for 8.1, 8.2, 8.3 (full datetime strings, Czech button labels). — 6 new test cases added; total 2029 FE tests pass.
- [x] 8.6 Run `npm run lint && npm run test && npm run build` — must stay green. — Lint 0 errors, 2029/2029 tests pass, build OK.

## 9. Fix sync overlay submit + merge sync column into Akce

Two regressions turned up after 8.x: the overlay's HalFormButton clicks registered a form request but never submitted (HalFormsPageLayout sits at page level and only sees the event-list/detail resource, not the sync sub-resource), and the dedicated `_links-sync` column duplicated the row's affordance surface. Both fixed together.

- [x] 9.1 Add `HalFormRequest.resourceContext` (`templates`, `resourceData`, `pathname`, `resourceUrl`) to `frontend/src/contexts/halFormContext.ts`. Captures the resource the button saw at click time.
- [x] 9.2 Have `HalFormButton` (`frontend/src/components/HalNavigator2/HalFormButton.tsx`) snapshot `useHalPageData()` + the self link into `resourceContext` on click — page-level buttons also pass it; the layout just ignores them.
- [x] 9.3 Have `HalFormsPageLayout` (`frontend/src/components/HalNavigator2/HalFormsPageLayout.tsx`) resolve the template via `override.templates ?? pageResource._templates` and pass `override.resourceData`/`pathname`/`resourceUrl` to `HalFormDisplay` (and `HalFormPanel` via new optional `template` prop on `HalFormPanel.tsx` so inline forms inside a nested provider skip the extra collection fetch).
- [x] 9.4 Merge the sync column into the Akce cell on `EventsPage.tsx`: drop the `<TableCell column="_links-sync">` block and append `<SyncStatusIndicator syncLink={syncLink} mode="icon"/>` as the last child of `renderActionsCell`, gated on `links?.sync` existing. Indicator stops being its own column header — the existing `labels.tables.sync` constant is no longer referenced anywhere in the page and can be removed in a follow-up.
- [x] 9.5 Tests + validation — 5 new FE test cases (2 I9-regression in `SyncStatusOverlay.test.tsx` proving the inner form modal opens with the override template, 2 focused override tests in `HalFormsPageLayout.test.tsx`, 1 sync-in-Akce ordering test in `EventsPage.test.tsx`). Existing rendering tests for the four action buttons (`form-template-button-${name}`) untouched. Final: `npm run lint` 0 errors, 2034/2034 tests pass, `npm run build` OK.

