## Why

GitHub issue #83 ("Chci definovat vedoucího akce (a jeho zástupce)", milestone `core`, labels `Events`, `organizátor akce`, `přihlašovatel`) asks for the ability to assign multiple coordinators to an event.

Today the `events` spec supports exactly one coordinator per event. A single coordinator is a bottleneck:

- If the coordinator becomes unavailable, there is no co-owner on the event — other managers still need `EVENTS:MANAGE` to step in.
- Members wanting to reach the coordinator have a single point of contact — if they do not answer, the next escalation path is unclear.
- For bigger events it is common for two people to share coordinator duties (registrations vs. logistics, for example).

## Decisions

The following decisions were made before creating specs and design:

1. **Collection of coordinators, symmetric roles.** The single `coordinator` field is replaced by a collection of coordinators (`coordinators: MemberId[]`). All members in the collection have identical rights — there is no "primary" vs. "deputy" distinction at the data level. The issue wording "vedoucí a zástupce" maps naturally to a two-element collection.

2. **Implicit edit rights for all coordinators.** Being in the `coordinators` collection grants implicit edit rights for that event, regardless of `EVENTS:MANAGE` — identical to the current single-coordinator behaviour.

3. **Filter "by coordinator" matches any position in the collection.** The `List Events` coordinator filter returns events where the given member appears anywhere in the coordinators collection.

4. **Persistence: new join table `event_coordinators`.** The existing `coordinator` column is removed and replaced by an `event_coordinators(event_id, member_id)` join table. No production migration concern (H2 in-memory).

5. **UI scope of this proposal.** This proposal changes the data model from a single field to a collection and updates UI accordingly:
   - Read-only: list of coordinator names.
   - Edit form: multi-value field where each entry is a member dropdown.
   - Issue gh-67 will later enrich the read-only display with contact information.

6. **ORIS import does not populate coordinators.** Coordinators are a Klabis-internal concept — ORIS has no equivalent field. After ORIS import the coordinators collection is empty.

7. **Duplicates deduplicated.** A member may appear in the coordinators collection at most once. The collection is a `LinkedHashSet`, so submitting a duplicate is silently deduplicated (merged, first-occurrence order preserved) rather than raising an error.

8. **List table display: first coordinator + "+N more".** The events list table shows the first coordinator name and a "+N" badge when more coordinators exist. Full list is shown on the event detail page.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: replace single `coordinator` field with a `coordinators` collection across `Create Event`, `Update Event`, `Event Detail Page`, `Events Table Display`, and `List Events` (filtering).

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md` — updated scenarios covering: collection on create/update, list display (first + badge), detail display (full list), filter by coordinator (any position), implicit edit authority for all coordinators, duplicate rejection, ORIS import ignores field.

**Affected code (backend, events module):** `Event` aggregate replaces single coordinator with a coordinators collection. `CreateEvent`/`UpdateEvent` commands accept `Set<MemberId>`. HAL representation exposes coordinator links as a collection. Authority check includes any member from the collection. `event_coordinators` join table replaces the `coordinator` column.

**Affected code (frontend):** event create/edit form uses a multi-value member dropdown; detail page renders the full coordinator list; list table shows first + badge.

**APIs (REST):** `coordinator` field in the event DTO is replaced by `coordinators` array. Breaking change — no external consumers known.

**Dependencies:** gh-67 (coordinator contact display) will extend the read-only coordinator list with contact info once this proposal is implemented.

**Data:** new `event_coordinators(event_id, member_id)` join table; `coordinator` column removed.
