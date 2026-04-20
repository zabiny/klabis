## Why

GitHub issue #64 ("Chci mít možnost zneviditelnit/smazat akci", milestone `core`, labels `Events`, `přihlašovatel`, `question`) asks for the ability to *hide* or *delete* an event outside of the current cancel flow.

Today the `events` spec supports only the following lifecycle transitions:
- DRAFT → ACTIVE (publish)
- DRAFT → CANCELLED (cancel a draft)
- ACTIVE → CANCELLED (cancel an active event; registrations kept for records)
- ACTIVE → FINISHED (automatic once event date passes)

There is **no hard delete** and **no "hide" state**. DRAFT events are already hidden from regular members (`List Events`: *"Regular user does not see DRAFT events"*), so managers can keep an event in DRAFT forever to effectively hide it. But:

- A DRAFT event created in error (wrong organizer code, duplicate from ORIS import, test data) stays in the database forever and clutters the manager's list.
- A cancelled event cannot be removed — it stays in `Events Table Display` as a struck-through/cancelled row indefinitely.
- There is no "archive" state — over years of operation the list will grow with cancelled/finished events with no way to tidy up.

The issue is labelled `question` because the exact semantics are not decided: is it a true delete (row disappears), an archival state (row filtered out unless explicitly requested), or just visibility?

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: extend `Event Status Lifecycle` (new transition paths), `List Events` (visibility rules for the new state or behavior for deleted events), `Get Event Detail` (404 when deleted or hidden from regular users), `Events Table Display` (how the affordances render).

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — depending on the decision, either new scenarios for a hard delete action (which events can be deleted, who can delete, what happens to related data — registrations, calendar items), or new scenarios for an "archive/hidden" status and transitions to/from it.

**Affected code (backend, events module):** the `Event` aggregate gains a delete method or a new status value. The events repository / JDBC adapter gains a hard-delete path or a query filter for the new status. Cascade semantics for registrations and calendar items must be defined.

**Affected code (frontend):** the event detail / events list page exposes a new action (e.g., "Smazat" or "Archivovat") via HAL-Forms affordance, available only to managers.

**APIs (REST):** either a new `DELETE /api/events/{id}` endpoint (hard delete) or a new action endpoint (e.g., `POST /api/events/{id}/archive`).

**Dependencies:** none.

**Data:** potentially a new status value, or a soft-delete column, or hard-delete cascade across the `events`, `event_registrations`, `calendar_items`, and `event_categories` tables.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Hard delete vs. soft delete / archive.**
   - Option A: **Hard delete DRAFT only.** Managers can permanently delete DRAFT events (the ones that never went public). ACTIVE, FINISHED, CANCELLED cannot be deleted — only cancel + archive. Minimal blast radius; ORIS-imported DRAFTs can be cleaned up easily.
   - Option B: **Hard delete any event.** Cascades delete registrations, calendar items, categories. Dangerous — loses audit trail. Probably too destructive for FINISHED events (historical record).
   - Option C: **New ARCHIVED status.** ARCHIVED is a terminal status reachable from FINISHED or CANCELLED; archived events are hidden from the default list but queryable by opting into "include archived". Preserves data; solves clutter.
   - Option D: **Both.** Hard delete DRAFT, archive FINISHED/CANCELLED.

   Recommend: Option D.

2. **Who can delete / archive?** `EVENTS:MANAGE` is the obvious answer. Confirm, or do we want a separate `EVENTS:DELETE` permission?

3. **What happens to registrations on a deleted DRAFT?** DRAFT events cannot have registrations today (`Registration for a non-active event not allowed`), so this is a non-issue for DRAFT. For ARCHIVED events that had registrations, registrations stay as historical records.

4. **What happens to event-linked calendar items on a deleted DRAFT?** DRAFT events do not have calendar items today (calendar sync only fires on publish). For ARCHIVED events — the calendar items may already have been removed on cancellation. Confirm no cascade worries.

5. **What happens to ORIS import provenance on hard delete?** If an ORIS-imported event is deleted, can the same event be re-imported later? Today the unique key is event id / ORIS id — deletion should not prevent re-import. Confirm.

6. **Should the ARCHIVED state surface on the `Automatic Event Completion` path?** I.e., auto-archive events N days after FINISHED? Probably out of scope — keep archive manual for now.

7. **UI affordances.**
   - Where does "Smazat" appear? Only on DRAFT events (detail + list row).
   - Where does "Archivovat" appear? On FINISHED and CANCELLED events.
   - Is there an "Obnovit" (restore) for archived events back to FINISHED/CANCELLED?

8. **How does the archived state interact with the events list filters (status filter, etc.)?** Archived events are excluded by default. A new filter checkbox "Zobrazit archivované" is visible to managers. Regular members never see archived events. Confirm.

9. **Dedup with the `cancel` action.** Is "archive a cancelled event" allowed, or is CANCELLED already permanently hidden from the list for regular members (if we implement #66 "viditelně škrtnout" differently)? Coordinate with the `add-event-cancellation-reason` proposal (#66).

10. **Spec location.** All changes sit inside `events` spec. Confirm no cross-spec deltas except what is listed under Impact.
