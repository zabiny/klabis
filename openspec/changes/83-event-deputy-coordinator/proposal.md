## Why

GitHub issue #83 ("Chci definovat vedoucího akce (a jeho zástupce)", milestone `core`, labels `Events`, `organizátor akce`, `přihlašovatel`, `question`) asks for the ability to define a deputy ("zástupce") event coordinator in addition to the primary coordinator.

Today the `events` spec supports exactly one coordinator per event (`Create Event`: *"coordinator (přiřaditelný člen klubu)"*, `Event Detail Page`, `Events Table Display`: *"coordinator shown as clickable link"*, filtered-by-coordinator in `List Events`). A single coordinator is a bottleneck:

- If the primary coordinator becomes unavailable (injury, last-minute schedule clash), there is no co-owner on the event. Other managers still need `EVENTS:MANAGE` to step in.
- Members wanting to reach the coordinator have a single point of contact — if they do not answer, the next escalation path is unclear.
- For bigger events it is common for two people to share coordinator duties (registrations vs. logistics, for example).

The issue is tagged `question` because the exact shape (single deputy? multiple deputies? full co-coordinator with equal privileges?) is not decided.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: extend `Create Event`, `Update Event`, `Event Detail Page`, `Events Table Display`, and `List Events` (filtering) to cover a deputy coordinator field.

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — new scenarios on the five requirements listed above covering: optional deputy on create, editable deputy on update, deputy displayed on detail and list pages, filter by deputy (or "any coordinator"), and any permission/authority granted by being a deputy.

**Affected code (backend, events module):** the `Event` aggregate and its commands (`CreateEvent`, `UpdateEvent`, ORIS-variants) gain a deputy coordinator field (nullable member reference). The HAL representation gains a deputy link and the list query may gain a deputy-aware filter. Coordinator authority checks (if they exist anywhere — TBD during design) need to include the deputy.

**Affected code (frontend):** event create/edit form, detail page, and list table render the deputy alongside the primary coordinator.

**APIs (REST):** additive — new optional field in the event DTO on create/update/get.

**Dependencies:** none.

**Data:** new nullable column on the events table.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Single deputy vs. list of deputies.**
   - Option A: exactly one deputy (`deputyCoordinator: MemberId?`). Simplest, matches the issue wording ("zástupce" singular).
   - Option B: a list of deputies with no upper bound. Allows multi-person coordination (logistics person + registration person + course setter).
   - Option C: a list of coordinators where the first is designated "primary" and the rest are "deputies", all stored in one collection.

2. **Does the deputy have any implicit permissions, or is it purely informational?**
   - Option A: informational only. Deputy is a member reference displayed on the event; no event management permissions implied. Managers with `EVENTS:MANAGE` still do all edits.
   - Option B: deputy gets implicit edit rights for that event (regardless of `EVENTS:MANAGE`). Requires a new authority check plumbed through the `events` module.
   - Option C: deputy gets a subset — can edit the event coordinator-level fields (registrations list access, contact) but not core fields (date, status transitions).

3. **Does the deputy show up in "Moje akce" views or in filters?** The `List Events` requirement today supports *"filter by coordinator"* — should the same filter match events where I am the primary OR deputy coordinator? Recommend: yes, merge behaviors into "any coordinator role".

4. **How does the existing `coordinator` column behave when migrating?** Not a production concern (H2 in-memory per project policy), so a simple schema addition is enough. Confirm.

5. **Interaction with `add-event-coordinator-contact` (issue #67/#37).** If deputies exist, the "contact coordinator" affordance should probably list all coordinators (primary + deputies). Should this proposal just add the field, or should it also extend the contact scenarios introduced by the other proposal? Recommend: add the field here; the contact proposal references deputies once they exist.

6. **ORIS import:** ORIS events have an event organizer contact. Does the ORIS import populate deputy fields in any way? Recommend: no, ORIS import ignores deputies (they are a klabis-internal concept). Confirm.

7. **Can the same member be both primary coordinator and deputy?** Clearly should be rejected. Confirm the expected error wording.

8. **Visual treatment on the events list table.** Does the deputy show as a second name in the coordinator column (comma-separated), as a tooltip, or only on detail? Recommend: detail-only to keep the list compact. Confirm.
