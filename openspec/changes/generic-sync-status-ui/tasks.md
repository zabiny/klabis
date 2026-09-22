## 1. Build the generic status icon component (both modes, all states)

- [ ] 1.1 Write component tests for `SyncStatusIndicator` covering: no `syncLink` prop → renders nothing; each of the 6 `SyncStatus` values → correct `Badge` variant + icon (per design.md D4); loading state; fetch error state — all against a mocked sync sub-resource response, no live backend needed
- [ ] 1.2 Write component tests for the two modes specifically: `mode="icon"` renders only the icon, with the formatted `lastSuccessfulSyncAt` appearing as a tooltip on hover (and a graceful fallback when the entity has never successfully synced); `mode="icon+date"` renders the icon and the formatted date together inline, always visible, with the same fallback when absent
- [ ] 1.3 Implement `SyncStatusIndicator` per design.md D1/D2/D4: accepts a `syncLink` prop and a `mode: 'icon' | 'icon+date'` prop, wraps content in `HalRouteProvider routeLink={syncLink}`, reads `SyncStateResponse` via `useHalRoute()`, renders the mapped icon/color and the date per the mode's placement rule above

## 2. Wire the icon+date mode into the detail page

- [ ] 2.1 Add `<SyncStatusIndicator syncLink={resourceData._links?.sync} mode="icon+date" />` to `EventDetailPage` next to the existing status badges
- [ ] 2.2 Add/adjust `EventDetailPage.test.tsx` coverage for the new indicator's presence (mocked sync sub-resource), without requiring a live backend

## 3. Wire the icon mode into the list rows

- [ ] 3.1 Add a `sync` column to `EventsPage`'s table rendering `<SyncStatusIndicator syncLink={value?.sync} mode="icon" />` per row, reading each row's own `_links.sync` (already present on `EventSummaryDto` per `EventController.java:595-598` — no backend change needed)
- [ ] 3.2 Add/adjust `EventsPage.test.tsx` coverage: enrolled rows render the indicator (mocked), unenrolled rows render nothing in that column

## 4. Add the manager overlay with actions

- [ ] 4.1 Write component tests for the overlay: not offered (no click affordance) when `_templates` absent on the fetched sync sub-resource; offered when present; each of `synchronizeNow`/`acknowledgeSyncConflict`/`resolveSyncConflict`/`resetSyncRecord` renders only when its template is present (delegated to `HalFormButton`'s existing behavior, but assert the overlay wires it up correctly per state)
- [ ] 4.2 Implement the overlay/modal component per design.md D5: status header (state, last successful sync, direction/external id/next-attempt/failure-count for `SYNC:MANAGE` callers), diverged-fields/local-external-baseline comparison when `CONFLICT`, and the relevant `HalFormButton`s — all inside the same `HalRouteProvider` context established by `SyncStatusIndicator`
- [ ] 4.3 Make `SyncStatusIndicator` clickable (opens the overlay) only when the fetched sync sub-resource carries `_templates`; otherwise keep it non-interactive

## 5. Retire the old manual-sync button

- [ ] 5.1 Remove `<HalFormButton name="syncEventFromOris" .../>` from `EventDetailPage` (design.md D6 — same underlying engine action, now reachable through the overlay's `synchronizeNow`)
- [ ] 5.2 Update/remove any now-obsolete test coverage in `EventDetailPage.test.tsx` that asserted the old button's presence/behavior
- [ ] 5.3 Confirm no other frontend call site still references the `syncEventFromOris` affordance
- [ ] 5.4 Run the full frontend test suite and `npm run lint`/`npm run build` (type-check) to confirm no regressions

## 6. Make the sync endpoint reachable and correctly documented

Deliberately last: nothing above needs this to be buildable or testable (all tests use a mocked sync sub-resource), so it doesn't gate any of the UI work — it only needs to land before real end-to-end use.

- [ ] 6.1 Add `SYNC` to `frontend/.env`'s `VITE_OAUTH_SCOPE` (and `.env.development.local.example` if it repeats the scope list there)
- [ ] 6.2 Verify the `klabis-web` and `klabis-web-local` OAuth2 client registrations (`authorizationserver` module bootstrap) already allow the `SYNC` scope; if not, add it there too
- [ ] 6.3 Fix the stale `sync` link description in `docs/openapi/spec/events.yaml` (currently "Present when the event is enrolled in synchronisation and the caller has SYNC:MANAGE") to describe the actual/spec'd rule: present when enrolled, visible to any signed-in user; detail fields and action templates inside the sync sub-resource are what's gated on `SYNC:MANAGE`
- [ ] 6.4 Regenerate the FE OpenAPI bundle/types (`npm run openapi`) so the doc fix and any type impact land in `klabisApi.d.ts`/`halTypes.ts`

## 7. Final verification

- [ ] 7.1 Manually verify on localhost: sign in, confirm the access token's `scope` claim includes `SYNC`, and that `GET /api/events/{id}/sync` no longer 401/403s purely for lack of scope
- [ ] 7.2 Full manual pass on localhost per `frontend/CLAUDE.md`'s testing guidance, as both `ZBM9000` (admin/manager) and `ZBM9500` (plain member): `EventDetailPage` shows icon+date; `EventsPage` rows show icon with hover tooltip; manager can open the overlay and trigger `synchronizeNow`/conflict actions where applicable; non-manager sees the icon only, with no way to open the overlay
- [ ] 7.3 Run `npm run refresh-backend-server-resources` so the built frontend is available on `:8443` for a final check outside the Vite dev server
- [ ] 7.4 Update this tasks.md's checkboxes to reflect final state before requesting review
