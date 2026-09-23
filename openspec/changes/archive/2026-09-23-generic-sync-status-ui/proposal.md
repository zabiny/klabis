## Why

The `data-synchronization` spec (`openspec/specs/data-synchronization/spec.md`, requirement "Synchronisation State Is Visible Per Entity") already fully describes how sync state must be shown to users — headline state for any signed-in user, full detail and actions for users holding `SYNC:MANAGE` — and the backend API (`sync.yaml`) already implements and field-trims exactly that. No frontend surfaces it yet: there is no generic way to see an entity's sync state or act on it, only a bespoke, entity-specific "sync now" button on `EventDetailPage`. This proposal closes that implementation gap with a single reusable component, instead of building another one-off per entity.

## What Changes

- Add a generic `SyncStatusIndicator` component driven purely by the presence of a `_links.sync` relation on any HAL resource (works today for events and disciplines, and for any future synced entity without changes).
- Two display modes: `icon` — icon only, last-successful-sync date shown as a tooltip on hover — used in list rows (`EventsPage`); `icon+date` — icon and last-successful-sync date both shown inline — used on entity detail pages (`EventDetailPage`, `DisciplineDetailPage`).
- Component fetches the sync sub-resource itself on mount (one GET per instance) so the icon's color reflects the actual status immediately, not just on hover.
- Map the 6 `SyncStatus` values (`NEW`, `IN_SYNC`, `RETRYING`, `CONFLICT`, `FAILED`, `RETIRED`) onto the existing `Badge` color vocabulary (`info`/`success`/`warning`/`error`/`default`) with distinct icons per state (`CONFLICT` and `FAILED` share the `error` color but use different icons, since they require different actions).
- Clicking the icon opens an overlay only when the response carries HAL-FORMS templates (i.e., caller has `SYNC:MANAGE`); the overlay renders those templates (`synchronizeNow`, `acknowledgeSyncConflict`, `resolveSyncConflict`, `resetSyncRecord`) via the existing HAL-FORMS subresource-provider mechanism, styled for this context rather than as a raw generic form.
- Remove the bespoke manual-sync button on `EventDetailPage` (`syncEventFromOris`), replaced by this component's overlay.
- Add the `SYNC` OAuth2 scope to the frontend's scope configuration (`frontend/.env`, and local-dev variants) — it's currently missing, so the sync endpoints are unreachable from the SPA regardless of the signed-in user's authorities.
- Fix a stale comment on the `sync` link in `docs/openapi/spec/events.yaml` ("Present when ... the caller has SYNC:MANAGE") that no longer matches the actual/spec'd visibility rule (the link/headline state is visible to any signed-in user; only the detail fields and action templates inside the sync sub-resource are gated on `SYNC:MANAGE`).

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/data-synchronization/spec.md` — the requirement "Synchronisation State Is Visible Per Entity" already specifies both the headline-state-for-everyone and full-detail-plus-actions-for-`SYNC:MANAGE` tiers, including the "no actions offered without the permission" rule. This proposal implements that requirement as written; it does not add, modify, or remove any scenario in this spec.
- `openspec/specs/events/spec.md` — event detail/list behavior is unaffected; only a UI-level affordance (the old manual sync button) is swapped for the new generic component. No event lifecycle or API contract changes.

**Why no spec update is needed:**
The user-visible sync-state behavior this component surfaces was already fully specified before this proposal — the API (`sync.yaml`) already returns the right data, trimmed per authority, for exactly this UI to render. What's missing today is only the frontend implementation, not the specification of the behavior. This proposal is the first UI consumer of an already-approved requirement, not a new or changed one; nothing in `openspec/specs/` needs to change as a result. The two side-fixes (OAuth2 scope config, a stale API-doc comment) are configuration/documentation corrections, not behavior changes.

## Impact

- **Frontend**: new generic component (and its overlay/modal), integration into `EventsPage` (table rows) and `EventDetailPage`/`DisciplineDetailPage` (detail view), removal of the old `syncEventFromOris` button, `frontend/.env` (and local-dev env example) scope change.
- **Backend**: none, beyond verifying/registering the `SYNC` scope on the existing OAuth2 client registrations (`klabis-web`, `klabis-web-local`) if not already enabled — configuration, not code.
- **Docs**: one stale comment fixed in `docs/openapi/spec/events.yaml`.
- **Developer workflow**: none.
