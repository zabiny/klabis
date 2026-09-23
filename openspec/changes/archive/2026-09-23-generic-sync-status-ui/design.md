## Context

The `sync` module already exposes, per linked entity, a `sync` sub-resource (`GET /api/{entityType}/{id}/sync`, `SyncApi.getSyncState`) returning a `SyncStateResponse` with `status` (`NEW`/`IN_SYNC`/`RETRYING`/`CONFLICT`/`FAILED`/`RETIRED`), `lastSuccessfulSyncAt`, and — only for callers with `SYNC:MANAGE` (`x-klabis-authority` field trimming) — direction, external id, next-attempt time, failure count, diverged fields/sides, and HAL-FORMS templates (`synchronizeNow`, `acknowledgeSyncConflict`, `resolveSyncConflict`, `resetSyncRecord`). The `sync` link is already attached to every enrolled `Event` resource, at both detail (`EventDetailsPostprocessor`) and list-row level (`EventSummaryPostprocessor`, both delegating to the shared `EventAffordanceSupport.addManagementAffordances`/sync-link block in `EventController.java:595-598`) — so every row in `EventsPage`'s table already carries `_links.sync` today, unused by the frontend.

No frontend code renders any of this. The only existing UI ("manual sync") is `EventDetailPage`'s `HalFormButton name="syncEventFromOris"` (line 233), backed by `EventController`'s `OrisEventsApi.syncEventFromOris` affordance. Tracing it (`OrisEventImportService.syncEventFromOris`, `backend/src/main/java/com/klabis/events/application/OrisEventImportService.java:84-97`) shows it does nothing but guard against `CONFLICT`/`FAILED` and then call `synchronizationPort.synchronizeNow(record.getId(), null)` — the exact same engine operation the sync sub-resource's own `synchronizeNow` template triggers. The two affordances are two doors onto the same action; today's door throws a 409 (`EventSyncNeedsResolutionException`) when status is `CONFLICT`/`FAILED`, where the sync sub-resource's own template is simply absent in that state and the conflict/failure is instead surfaced properly via `acknowledgeSyncConflict`/`resolveSyncConflict`/`resetSyncRecord`.

The reusable HAL-FORMS toolkit (`hal-navigator-patterns` skill) already covers everything needed except one thing: `HalSubresourceProvider` resolves its target link from the *ambient* `HalRouteContext` (`useHalRoute()` inside `HalSubresourceProvider`, `frontend/src/contexts/HalRouteContext.tsx:149-161`) — i.e. from the current *page's* route resource. A table row's own resource (an item inside `_embedded`) is never the ambient route resource, so `HalSubresourceProvider` cannot be used as-is to fetch a row's own `sync` sub-resource.

Frontend does not yet have a discipline list/detail page (only a `Layout.tsx` nav reference) — no such page exists to integrate the new component into today.

## Goals / Non-Goals

**Goals:**
- One generic, reusable component that renders an entity's sync status as an icon, usable anywhere a HAL resource carries `_links.sync` — starting with `EventDetailPage` and `EventsPage`'s table rows.
- Icon reflects the real `status` immediately (not just on hover), with two display modes: `icon` (icon only, last-successful-sync date shown as a tooltip on hover — used in list rows) and `icon+date` (icon and last-successful-sync date both shown inline — used on detail pages).
- Click opens an overlay with full detail and available actions, but only when the sync sub-resource response carries HAL-FORMS templates (i.e., the caller has `SYNC:MANAGE`) — for everyone else, the icon is informational only, per the `data-synchronization` spec's "no actions offered without the permission" rule.
- Replace `EventDetailPage`'s `syncEventFromOris` button with the same capability, now exposed through the overlay's `synchronizeNow` action.
- Make the `SYNC` OAuth2 scope actually reach the frontend, since none of this is reachable without it.

**Non-Goals:**
- No backend/API changes to the `sync` module or its response shape — the data this UI needs already exists.
- No change to `EventSummaryDto`/list response schema — no bulk-embedding of sync state into list payloads; the component fetches its own state per instance.
- No removal of the backend `syncEventFromOris` endpoint or its HAL-FORMS affordance — only its frontend usage (the button) is dropped. Removing the now-redundant backend endpoint is a separate, later cleanup if ever done.
- No discipline-page integration in this change — no discipline list/detail page exists on the frontend yet. The component is built generic so that page can adopt it later without modification.
- No spec changes (this is the spec-free schema; see proposal.md's justification).

## Decisions

### D1 — Component takes an explicit `syncLink` prop, not ambient context

`SyncStatusIndicator` receives the `Link` (or `HalResourceLinks`) value read by its caller from `resourceData._links.sync`, rather than trying to discover it from ambient `HalRouteContext`. This is what makes the component usable identically in a detail page (where `_links.sync` is on the page's own route resource) and inside a table row (where it's on an `_embedded` item that is never the ambient route resource).

```tsx
// Detail page
<SyncStatusIndicator syncLink={resourceData._links?.sync} mode="icon+date" />

// Table cell, per row
<TableCell column="_links" dataRender={({value}) => (
  <SyncStatusIndicator syncLink={value?.sync} mode="icon" />
)}>Sync</TableCell>
```

If `syncLink` is absent, the component renders nothing (not every event is enrolled).

`mode` is not just a size variant — the two modes differ in *where the date lives*: `icon` shows only the icon, with the last-successful-sync date surfaced as a tooltip on hover (used in list rows, where table width is scarce); `icon+date` shows the icon and the date together inline, always visible (used on detail pages, where there's room and the date is worth showing without an interaction).

### D2 — Fetch via `HalRouteProvider` directly, not `HalSubresourceProvider`

Internally, `SyncStatusIndicator` wraps its fetching logic in `<HalRouteProvider routeLink={syncLink}>` (the same provider `HalSubresourceProvider` itself delegates to — see Context) instead of using `HalSubresourceProvider`. `HalRouteProvider` already accepts an explicit `routeLink` and does not depend on any ambient parent resource, which is exactly what a per-row instance needs. This keeps the fetch on the existing React-Query-backed HAL fetching path (caching, dedup) without inventing a new one.

```tsx
const SyncStatusIndicator = ({syncLink, mode}: Props) => {
  if (!syncLink) return null;
  return (
    <HalRouteProvider routeLink={syncLink}>
      <SyncStatusIndicatorContent mode={mode} />
    </HalRouteProvider>
  );
};
```

`SyncStatusIndicatorContent` then reads the fetched `SyncStateResponse` via `useHalRoute()` exactly as any other HAL page does — same hooks, same conventions, nothing new to learn.

### D3 — Eager fetch, one request per instance

Each `SyncStatusIndicator` instance fetches on mount (via `HalRouteProvider`'s built-in `useAuthorizedQuery`), so a paginated `EventsPage` page renders one sync-state request per visible row. This is an accepted, deliberate trade-off (confirmed with the user): the icon's color must reflect the true state immediately, so there's no cheaper option that preserves at-a-glance correctness. React Query's existing caching (`staleTime`/`gcTime`, already 5 minutes on the underlying `HalRouteProvider`) keeps repeat mounts (e.g. re-render, pagination back-and-forth) from re-fetching needlessly. Lists are paginated (dozens of rows, not hundreds), keeping the burst bounded.

### D4 — State → icon/color mapping

Reuses the existing `Badge` variant vocabulary (`frontend/src/components/UI/Badge.tsx`) and `lucide-react` icon set already used elsewhere in the app, rather than inventing a new visual language:

| `SyncStatus` | Badge variant | Icon (lucide-react) | Meaning |
|---|---|---|---|
| `NEW` | `info` | `CircleDot` | Not yet synced |
| `IN_SYNC` | `success` | `Check` | In step |
| `RETRYING` | `warning` | `RotateCw` | Trying again after a transient failure |
| `CONFLICT` | `error` | `AlertTriangle` | Needs a decision (acknowledge/resolve) |
| `FAILED` | `error` | `XCircle` | Stopped, needs a reset |
| `RETIRED` | `default` | `Archive` | No longer synchronised |

`CONFLICT` and `FAILED` intentionally share the `error` color (both mean "needs a human") but get distinct icons, since they require different actions and it's worth being able to tell them apart at a glance.

### D5 — Overlay reuses `HalFormButton`, not a hand-rolled `HalFormsForm` call

Because the overlay's content sits inside the same `HalRouteProvider` (D2), the sync sub-resource *is* the ambient HAL context for everything inside the overlay — so the overlay can use the ordinary page-level pattern, `<HalFormButton name="synchronizeNow" />` etc., exactly as any other page does. `HalFormButton` already no-ops when its named template is absent, which is precisely how state-dependent action availability (`synchronizeNow` only in `NEW`/`IN_SYNC`/`RETRYING`, etc.) is expressed server-side — no client-side status-to-action mapping needs to be duplicated.

The overlay is "UI-customized" in the sense that its layout is purpose-built (a status header, last-sync timestamp, and — for `SYNC:MANAGE` callers — the diverged-fields/local-external-baseline comparison when in `CONFLICT`) with the relevant `HalFormButton`s placed where they make sense, rather than an auto-generated sequential form over all `SyncStateResponse` fields.

### D6 — `syncEventFromOris` button removal is safe

Since `OrisEventImportService.syncEventFromOris` and the sync sub-resource's `synchronizeNow` both terminate in the same `synchronizationPort.synchronizeNow(record.getId(), null)` call (see Context), dropping the `HalFormButton name="syncEventFromOris"` from `EventDetailPage` in favour of the overlay's `synchronizeNow` changes no backend behavior — it's a strict UX improvement, since the overlay naturally hides the action instead of the old path's 409 error toast when the record is `CONFLICT`/`FAILED`.

## Risks / Trade-offs

- **[Risk]** N sync-state requests per `EventsPage` page load → **[Mitigation]** accepted trade-off (D3); bounded by pagination; React Query caching avoids re-fetch storms on re-render.
- **[Risk]** Adding `SYNC` to the frontend's OAuth2 scope changes what an already-issued token can request; if the client registration in `authorizationserver` doesn't list `SYNC` as an allowed scope, token requests could fail scope validation → **[Mitigation]** verify/update the `klabis-web`/`klabis-web-local` registered-client scopes as part of this change's tasks; no production environment exists yet, so this is a dev/CI-only risk today.
- **[Risk]** Testing a component with 6 distinct visual states plus a permission-gated overlay needs deliberate fixture coverage → **[Mitigation]** cover all 6 states plus the with/without-`SYNC:MANAGE` (templates present/absent) branch explicitly in component tests, using MSW-style mocked `sync` sub-resource responses per the project's existing test conventions.

## Open Questions

None outstanding — all prior open questions (scope config, stale API-doc comment, list vs. detail placement, fetch strategy, state→icon mapping, overlay content shape) were resolved during exploration (see proposal.md and this design's Decisions).
