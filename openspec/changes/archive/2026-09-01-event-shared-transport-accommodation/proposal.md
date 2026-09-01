## Why

Clubs frequently arrange shared transport and shared accommodation for an event, and organisers need to know how many members want to use each before they book seats or beds. Today there is no way to offer these two options on an event or to collect member choices. This is a deliberately narrow first step; the broader, configurable "supplementary services" model lives in `gh-112-event-supplementary-services` and is out of scope here.

## What Changes

- **Event gains two independent boolean flags**: "shared transport enabled" and "shared accommodation enabled". A manager (or coordinator) sets them when creating an event and can toggle them later via the event form and inline edit, in DRAFT or ACTIVE status.
- **Registration gains two boolean choices**: "wants shared transport" and "wants shared accommodation". Each choice field appears in the registration form and the edit-own-registration form **only when the event has the corresponding flag enabled**. Both default to unchecked.
- **Event Detail Page shows a count summary** for each enabled flag (e.g. "Společná doprava: 12 členů"), visible only to the event coordinator and users with `EVENTS:REGISTRATIONS`, only for ACTIVE events. The row is shown even when the count is 0. No per-member breakdown; the registrations list is unchanged.
- **Disabling a flag that already has selections is allowed.** Existing member choices are retained in storage but not shown anywhere; re-enabling the flag surfaces them again.
- **BREAKING — accommodation list is filtered to members who chose shared accommodation.** The existing "Seznam pro ubytování" action (today lists *every* registered member) now lists only members whose registration has "wants shared accommodation" set. The action is available only when the event has the shared-accommodation flag enabled; otherwise it is not exposed and the API rejects direct access. The old "all registered members" semantics is removed.

No breaking API changes — event representation, the event create/update command, and the registration command all grow additively. The accommodation list contents change is a behavioural break of an existing feature.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `events`: `Create Event` and `Update Event` gain the two optional boolean flags; `Event Detail Page` gains the coordinator-only count summary and the inline-edit toggles for the flags.
- `event-registrations`: `Register for Event` and `Edit Own Registration` gain the two conditional boolean choice fields, present only when the event enables the matching flag; `Generate Accommodation List for Event Registrations` is scoped down to members who chose shared accommodation and is gated on the event's shared-accommodation flag.

## Impact

**Affected specs:**
- `openspec/specs/events/spec.md`
- `openspec/specs/event-registrations/spec.md`

**Affected code (backend, events module):**
- Event aggregate: two boolean fields with defaults `false`; create/update paths accept them.
- Event registration aggregate: two boolean fields with defaults `false`; register/edit paths accept them, ignoring (but retaining) a choice whose flag is currently disabled.
- Read model for the event detail: two counts derived from registrations where the choice is `true`.
- JDBC persistence: new columns on the event and event-registration tables; DB migration.

**Affected code (frontend):**
- Event create/edit form and inline edit: two checkboxes.
- Registration form and edit-own-registration form: two checkboxes rendered from HAL-FORMS metadata, present only when the event enables the flag.
- Event detail page: count summary section, gated on the viewer's authority and event status.

**APIs (REST):** additive fields on the event resource, the event create/update template, the registration resource, and the registration template. All responses keep full `_links`.

**Dependencies:** none. Independent of `gh-112-event-supplementary-services`.

**Data:** new boolean columns (`shared_transport_enabled`, `shared_accommodation_enabled` on events; `wants_shared_transport`, `wants_shared_accommodation` on registrations), all defaulting to `false`.
