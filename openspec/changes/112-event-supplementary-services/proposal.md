## Why

GitHub issue #112 ("Chci mít možnost přepsat informace v importovaných akcích (vcetne doplnkovych sluzeb)", milestone `MVP`, labels `přihlašovatel`, `question`) has two parts:

1. **Overwrite any ORIS-imported event information locally** — a manager should be able to edit fields of an ORIS-imported event (location, description, deadline, etc.) and have the local version take precedence over subsequent ORIS syncs.
2. **Supplementary services** (*doplňkové služby*) — the event should support a list of extra services (accommodation, transport, dinner, T-shirt, …) that members can pick up during registration. These come from ORIS in some form but need to be representable and editable locally.

Current spec coverage:

- `events` `Update Event` — managers with `EVENTS:MANAGE` can edit DRAFT/ACTIVE events, which **implicitly includes ORIS-imported ones**. The spec does not distinguish. That covers part (1) at a basic level, BUT there is no rule about what happens on the next ORIS sync — will the manual edits be overwritten? Per `Row-Level Management Actions in Events Table`, managers can click "Synchronizovat" to re-sync from ORIS; behavior on conflict is underspecified.
- Supplementary services are **not modelled at all** today. No field on the event, no UI, no spec scenario. Related issues #102 (member picks services at registration), #103 (accommodation capacity), #104 (shared accommodation for weekend multi-events), #105 (list of accommodated members), #106 (ID cards for accommodation) all depend on this.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: extend `Update Event`, `Row-Level Management Actions in Events Table`, and `Event Detail Page` with:
  - explicit rules for how manual edits to ORIS-imported events interact with subsequent ORIS sync,
  - a supplementary-services list on the event aggregate.
- Likely also `event-registrations`: once services exist on the event, registrations need to carry service selections (prepares the ground for #102).

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — new or extended scenarios on:
  - `Update Event`: "Manager overrides ORIS-imported field". Each editable field either becomes "locally overridden" (ORIS re-sync skips it) or "not overridden" (ORIS re-sync refreshes it). Policy choice, see Open Questions.
  - `Row-Level Management Actions in Events Table` / ORIS sync: what happens on sync when there are local overrides.
  - New "Supplementary Services" requirement: a list of services (name, optional description, optional price, optional capacity) per event. Create/edit scenarios. Display scenarios on event detail.

**Affected code (backend, events module):**
- Per-field "locally modified" flag on the event aggregate (or a separate `event_overrides` table).
- A `SupplementaryService` value object (or entity) with id, name, description, price, capacity.
- An `updateEvent` path that marks edited fields as locally modified.
- An ORIS sync path that respects the locally-modified flag and refreshes only unmodified fields.

**Affected code (frontend):**
- The event edit form already exists — extend with a supplementary-services editor (add/remove/reorder rows with name/description/price/capacity).
- Event detail page shows the services list.
- Possibly a visual marker for "locally overridden" fields when viewing an ORIS-imported event.

**APIs (REST):** additive — event representation and update command grow.

**Dependencies:** none.

**Data:** new `event_supplementary_services` table; possibly per-field override tracking.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Override granularity.**
   - Option A: field-level override. The system tracks which individual fields have been manually edited; ORIS sync refreshes only the untouched fields.
   - Option B: event-level override. Once any field is manually edited, the ORIS sync no longer overwrites anything on that event; re-sync is a no-op until the manager explicitly resets.
   - Option C: every sync overwrites everything (current, undesired behavior).

   Recommend: Option A. The UI shows a visual marker on overridden fields; a per-field "reset to ORIS" action is available.

2. **Should the override also block auto-import (issue #113)?** If so, auto-import must share the same override-respecting logic. Recommend: yes.

3. **Supplementary services — what fields?**
   - Name (required, e.g., "Ubytování ze soboty na neděli")
   - Description (optional)
   - Price (optional, Czk amount)
   - Capacity (optional, integer — for services with limited supply like accommodation)
   - Deadline (optional; can differ from event deadline — e.g., T-shirt orders close earlier)

   Recommend: all five, all except name are optional.

4. **Are services ORIS-sourced, manual, or both?**
   - ORIS exposes some service-like fields (entry fees, services) in its XML. Should klabis auto-import them?
   - Recommend: Manual-only for now. ORIS import of services is out of scope.

5. **Service ordering / sorting.** Manual order preserved? Alphabetical? Price ascending? Recommend: manual order (drag-to-reorder).

6. **Registrations carry service selections — in this proposal or a follow-up?** Issue #102 is the dedicated one for the registration side. Recommend: this proposal only adds the services to the event; `event-registrations` spec does not change here. Issue #102 will layer on the selection flow.

7. **Pricing and currency.** Single currency (CZK)? Multi-currency? Recommend: CZK only; price stored as integer haléře (or BigDecimal) — existing finance patterns in the project, if any, take precedence.

8. **Deletion of a service that has existing selections.** If #102 lands and a member has selected "Ubytování", can the manager still delete that service? Recommend: reject with error "služba je již vybraná uživateli X". Deferred until #102. For this proposal: simple delete allowed.

9. **ORIS conflict indicator UX.** When the manager views an ORIS-imported event and sees locally-overridden fields, what does the UI show? Recommend: a small "upraveno" badge next to each overridden field, with a tooltip "hodnota z ORIS: ...".

10. **Scope split.** Should we split this into two proposals (one for override semantics, one for supplementary services)? The two are logically independent. Recommend: split — but for now this single proposal covers both for discussion. Confirm the split or keep combined.
