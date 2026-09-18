## Why

GitHub issue #113 asks, among other things, for events to be tagged with the training groups they are relevant to ("tagovat pro ktere skupiny"). A club runs several training groups with different ages and ambitions, and only a fraction of the events in the calendar concern any given group. Today a member sees one undifferentiated list and has to know from elsewhere which events are meant for them; a trainer has no way to point their group at the right ones.

This was originally bundled with the ORIS auto-import (`gh-113-oris-auto-sync`), on the assumption that tagging would be inferred automatically at import time. That assumption has been dropped — **tagging is manual** — which makes it independent of where the event came from: a manually created event needs tagging just as much as an imported one. Hence its own change.

## What Changes

- **An event can be tagged with training groups.** Zero, one or several. A manager sets them on the event; there is no automatic inference from age category, competition level or distance.
- **The tags are visible on the event**, so a member can see at a glance whether an event concerns their group.
- **Events can be filtered by training group** in the events list, so a member or trainer can narrow the calendar to what is relevant to them.
- Tagging is available for every event regardless of origin — manually created, manually imported from ORIS, or automatically imported.

Explicitly **not** in this change: automatic inference of the right groups, per-group registration deadlines (see `gh-113-oris-internal-deadline-offset`), notifications to group members about a newly tagged event, and any change to who may register for an event — a tag is informational and does **not** restrict registration.

## Capabilities

### Modified Capabilities

- `events`: an event carries an optional set of training groups it is relevant to; a manager maintains that set; the events list can be filtered by it.
- `user-groups`: a training group's detail shows the upcoming events tagged for it — **only if** the relation is to be navigable from the group side. This is an open question below; if the answer is no, this capability is untouched.

## Impact

**Affected code (backend):**

- `com.klabis.events.domain.Event` — a collection of training group references, with commands to set them. A reference only: the tag names a group, it does not copy anything from it.
- `com.klabis.groups.traininggroup` — read access to validate that a tagged group exists. The training group aggregate itself is unchanged; the events module must not reach into it beyond an existing port.
- Events listing — an additional filter parameter.

The relation is deliberately held on the event side. A training group is a long-lived aggregate whose membership changes for reasons unrelated to the calendar, and nothing about a group changes when an event is tagged.

**APIs (REST):** additive — the event representation gains its training groups, the update affordance gains the field, and the events collection gains a filter parameter. Following the project's HAL+FORMS conventions, the tags are rendered as links to the groups rather than as bare identifiers.

**Data:** a join table between events and training groups. Removing a training group must not leave dangling tags — see the open question on deletion.

**Dependencies:** none new. Both aggregates already exist.

**Frontend:** group badges on the event detail and in the events list; the tag editor in the event form; the group filter on the events list.

## Open Questions

1. **Who may tag an event?** `EVENTS:MANAGE` keeps the permissions surface small and matches the rest of event editing. But a trainer who is not an event manager arguably knows best which events suit their group. Is tagging restricted to event managers, or may a group's trainer tag events for their own group?

2. **Is the relation navigable from the training group?** Showing "upcoming events for this group" on the group detail page is the more useful half of the feature for a member, but it pulls the `user-groups` capability into this change. In scope, or events-side only for now?

3. **What happens to tags when a training group is deleted or renamed?** A rename is harmless if the tag is a reference. A deletion must either be blocked while tags exist, or silently drop the tags. To decide.

4. **Does the filter belong in the members' calendar too?** The events list is a manager-facing view; the calendar (`calendar-items`) is what a member actually looks at. If the filter is only useful in the calendar, this change touches that capability instead.

5. **Should a member's own groups pre-filter their view?** Defaulting the calendar to "events tagged for my groups" would make the feature pay off without any interaction, but hides everything else unless the member notices the filter. Default to all events, or to the member's own groups?
