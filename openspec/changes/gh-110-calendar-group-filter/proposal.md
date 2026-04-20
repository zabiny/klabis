## Why

GitHub issue #110 ("Chci kalendar kde uvidim terminy prihlasek, akce skupin jejichz jsem clenem", milestone `MVP`, labels `clen klubu`, `hostujici clen`, `question`) asks for the calendar to be filtered by the current user's group memberships — so that the member sees events/deadlines relevant to their groups, not every event the club is involved in.

Today the `calendar-items` spec delivers half of this:
- `View Calendar Items` — members see the full calendar for the selected range.
- `Automatic Synchronization from Events` — each published event produces an event-date calendar item plus (when the event has a deadline) a "Přihlášky - {event name}" item on the deadline day. So the deadlines part of the acceptance criterion is already covered.

What is missing is the **filter**:
- No notion of "events my groups are attending" or "events tagged for my groups".
- Today, a child in the U12 training group sees calendar items for every club event, including CR-level races for the adult group — noise that obscures the few events their group actually attends.

This proposal depends on issue #113 / #113-oris-auto-sync (event-to-group tagging): for events to be filterable by group, events must first carry group tags. Without that, the only "group membership" we can filter by is trivial (all events apply to all members).

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `calendar-items`: extend `View Calendar Items` with a group-relevance filter — items are included when the originating event is tagged for one of the current user's groups (or carries no tags at all, falling back to "relevant for everyone").
- `events`: prerequisite — event-to-group tagging from issue #113.

## Impact

**Affected specs:**
- `openspec/specs/calendar-items/spec.md` — `View Calendar Items` gains a filter dimension or a default filter based on the user's group memberships. Open decision: is the group filter always-on (implicit) or opt-in (explicit toggle)?
- `openspec/specs/events/spec.md` (prereq, see proposal #113) — event gains a "relevant groups" collection.

**Affected code (backend, calendar module):**
- Calendar query joins against events and filters by event-tagged groups ∩ user's groups.
- Alternatively, the event→calendar-item sync carries group tags onto the calendar item itself, so the calendar query does not need to join back to events.

**Affected code (frontend):**
- Calendar page: optional filter control "Pouze moje skupiny" (or always-on, depending on answer to Q1).

**APIs (REST):** additive — optional query parameter on `GET /api/calendar`.

**Dependencies:** event-to-group tagging (proposal `113-oris-auto-sync`). This proposal does NOT ship until that is defined.

**Data:** if the calendar stores its own copy of the group tags, a new table/column. Otherwise, none.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Default filter — on or off?**
   - Option A: group filter is **always on** for regular members. They see only events tagged for their groups plus events with no tags (interpreted as "applies to everyone"). Opt-out is possible via a "Zobrazit vše" toggle.
   - Option B: group filter is **off** by default. Members see the full calendar and opt in via a "Pouze moje skupiny" toggle.

   Recommend: Option A — this matches the issue framing ("*akce skupin jejichž jsem členem*"). Confirm.

2. **Which group types count for relevance?**
   - Training group (primary use case).
   - Family group (rarely relevant for event filtering, but acts as a quick "add my family" view).
   - Free groups (user-created, could be relevant for e.g. "ladies' training group"; depends on whether free groups can be tagged on events, which is a separate design question).

   Recommend: training groups only, to start. Family / free groups can be added later.

3. **"No tags" interpretation.** If an event has no group tags, does the filter include it (treat as "everyone") or exclude it (treat as "no group declared, hidden")? Recommend: include — this is the backward-compatible default.

4. **Interaction with the general user-visible calendar filter.** `View Calendar Items` today has no filter — so adding one is additive. This proposal should not change `sort`, `navigate`, etc. Confirm no cross-cutting change.

5. **Deadline calendar items.** The "Přihlášky - {event name}" items inherit their group relevance from the source event (same tag set). Confirm that is what members expect.

6. **Admin / manager view.** Should managers with `EVENTS:MANAGE` see the full calendar (no implicit group filter) by default? Recommend: yes — they need the overview. The toggle stays available if they want it.

7. **Cached / denormalized calendar vs. live join.** If group membership changes (a member moves between training groups per the age-based reassignment), should old calendar items reflect the new membership immediately? Recommend: yes, live join — simpler invariants.

8. **Dependency ordering.** This proposal explicitly depends on event-to-group tagging from `113-oris-auto-sync`. Recommend landing #113 first (or splitting the tagging part of #113 out as a prerequisite). Confirm ordering.

9. **Guest / hostující members.** Hosting members may belong to a training group (if assigned via the automatic age-based rule). The filter logic is the same. Confirm.

10. **Manual calendar items (CALENDAR:MANAGE):** can manual items also carry group tags, or is tagging only meaningful for event-linked items? Recommend: out of scope — manual items are always visible to everyone. Confirm.
