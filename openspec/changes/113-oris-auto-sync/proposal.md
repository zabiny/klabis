## Why

GitHub issue #113 ("Automaticky vytvaret a synchronizovat (a tagovat pro ktere skupiny, termin prihlasek -1D,..) akce z ORIS k diskuzi jak a co implementovat", milestone `MVP`, labels `ORIS`, `přihlašovatel`, `question`) asks for automatic mass synchronization of ORIS events into klabis, including:
- automatic periodic import of newly-published ORIS events,
- automatic refresh when existing ORIS events change upstream,
- tagging imported events with the training groups they are relevant to,
- per-group adjustments such as "registration deadline minus one day" (so the club's internal registration closes one day before the ORIS upstream deadline, leaving a buffer for the organizer to forward the list).

Today the `events` spec supports **manual, one-event-at-a-time** import from ORIS:
- `ORIS Import Includes Registration Deadline` — imports a single event including its primary entry deadline.
- `ORIS Import Tolerates Missing Location` — imports even if upstream location is missing.
- `Row-Level Management Actions in Events Table` — managers can trigger a "Synchronizovat" action on individual ORIS-imported events.

There is no:
- automatic bulk import schedule,
- notification when new ORIS events appear,
- group-tagging mechanism linking ORIS events to training groups,
- internal-deadline offset vs. the ORIS upstream deadline,
- change detection / diff view on ORIS refresh.

The issue is labelled `question` because the scope is broad. This proposal covers the full set; open questions at the end capture the decision points.

## Capabilities

### New Capabilities

<!-- None required at the capability level — the ORIS integration lives inside the `events` capability today. An open question asks whether a dedicated `oris-integration` capability is warranted. -->

### Modified Capabilities

- `events`: extend the existing ORIS import requirements with scheduled auto-import, auto-refresh on change, event→group tagging, and the deadline-offset feature.
- `calendar-items`: indirectly affected — when an ORIS event updates (e.g., deadline shifts), the already-synced calendar items must reconcile (the automatic-sync requirement already covers this, but we confirm it handles the ORIS-triggered update path).
- Possibly `user-groups`: if group-tagging is bi-directional (i.e., a training group's detail page lists relevant upcoming events), that spec needs scenarios too.

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — new requirements (or extensions) covering: "Automatic ORIS Bulk Import" (scheduled), "ORIS Change Detection on Re-Import" (what happens to fields that were manually overridden — see #112), "Event-to-Group Tagging" (optional per-event training-group association, manual or auto), "Registration Deadline Offset" (per-event or per-group).
- Possibly new `openspec/specs/oris-integration/spec.md` if the team prefers a dedicated capability.

**Affected code (backend, events module):**
- A new scheduled job that queries ORIS for events matching the club's organizer code / region / date range and imports/refreshes them.
- Change detection logic comparing fresh ORIS data with the stored event; decides update vs. conflict (coordinated with #112).
- A new `tags` / `relevantGroups` collection on the event aggregate, plus rules for auto-assignment (by distance, by competition level, by age-range overlap with training groups — all open).
- A new "internal registration deadline" field distinct from the ORIS-upstream deadline (or a global club-level offset).

**Affected code (frontend):**
- Possibly a new settings page for ORIS auto-import rules (region, date range, offset, etc.).
- Events list: visual marker for ORIS-auto-imported events; affordance to "ignore" an imported event.
- Event detail: group-tag badges and editable tag list.

**APIs (REST):** additive — endpoints for the new settings, tag management, and possibly a manual "trigger auto-import now" affordance for debugging.

**Dependencies:** ORIS client library / API access already exists (for single-event import). Scheduling infrastructure already exists (for `Automatic Event Completion`).

**Data:** new tables or columns for auto-import rules, event tags, internal deadline offset.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change. This is a broad change with many forks; the team should decide scope before investing in design.

1. **Scope for this proposal.** The issue bundles four related features: scheduled import, change detection, group tagging, deadline offset. Options:
   - (a) Tackle all four in this proposal.
   - (b) Split into four proposals (scheduled import / change detection / group tagging / deadline offset) — each can be decided independently.
   - (c) Do only scheduled import now (biggest value, unblocks everything else) and split the rest out.

   Recommend: Option (c).

2. **Auto-import trigger and frequency.**
   - Periodic schedule (daily? nightly?).
   - On-demand only (user clicks a "refresh from ORIS" button on the events list — partially already there at the single-event level; at the bulk level, new).
   - Both (scheduled + manual trigger).

3. **Auto-import scope — which ORIS events are imported?**
   - Events matching the club's organizer code (where the club itself is organizer).
   - Events within a configured region.
   - Events within a configured date range (next N months).
   - Events matching a configured competition-level (ČR / Morava / regional).
   - Combination — configurable on a settings page.

4. **Change detection on re-import vs. manual overrides.** Coordinate with proposal #112 (add-event-supplementary-services / edit-overrides). If a manager manually edited a field, should an ORIS change overwrite it, skip it, or flag a conflict for review?

5. **Group tagging — manual or automatic?**
   - Manual: event managers pick which training groups the event is relevant to.
   - Automatic: system infers from age category overlap, distance to location, competition level.
   - Both (auto as default, manual override).

6. **"Registration deadline -1D".** What does "-1D" mean?
   - Option A: a global club-level offset — every ORIS event's internal deadline is stored as ORIS deadline minus 24 hours.
   - Option B: per-group offset (larger kids can register later than younger ones).
   - Option C: per-event manual offset set at import time.

   Recommend: Option A for MVP.

7. **Internal vs. upstream deadline semantics.** If the klabis internal deadline has passed but the ORIS upstream has not, can members still register "post-deadline" through an admin? Tie-in with issue #99 ("koho mam dohlasit rucne"). Confirm this is out of scope here.

8. **Notification on new ORIS events (#91).** Related issue #91 ("Chci být informován o nových akcích z ORISu") depends on the auto-import existing. Scope: does this proposal also deliver the notification, or just the import?

9. **Idempotency / dedup.** If an ORIS event is re-imported, the system must match it to the existing record by ORIS id, not by name/date. Is the ORIS id already stored per event? Confirm.

10. **New capability vs. extension.** Is this large enough to warrant a new `oris-integration` capability, or should it stay inside `events`? Recommend: extension of `events` for now; promote to its own capability only if ORIS integration grows further (CUS sync, etc.).

11. **Where does the "ORIS auto-import settings" UI live?** Admin area, next to member permissions management. Confirm.

12. **Permissions.** Who can configure auto-import rules? `EVENTS:MANAGE` or a new dedicated permission? Recommend: `EVENTS:MANAGE` to keep permissions surface small.
