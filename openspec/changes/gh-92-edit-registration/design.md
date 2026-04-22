## Context

Today a member can only create or cancel an event registration. The `EventRegistration` value object in the `events` bounded context is immutable — there is no update path. A member who wants to change a value currently has to `unregister` + `register`, which:

- resets `registeredAt` (undesirable once first-come-first-served capacity logic lands),
- produces a `MemberUnregisteredFromEventEvent` followed by a `MemberRegisteredForEventEvent` (noisy audit and misleading downstream signal — e.g., future ORIS sync would treat it as "left and came back"),
- is racy with capacity caps: another member can slip into the slot between the two calls.

`EventRegistration` is currently modelled as a `@ValueObject` held inside the `Event` aggregate (`Event.registrations`). Business rules for opening/closing the registration window already live on `Event` (`assertRegistrationsOpen()` — checks `ACTIVE` status, `registrationDeadline`, and `eventDate`), and the same guard applies to register + unregister today.

Scope: self-edit only. Admin override is out of scope (tracked separately — `#32`, `#33`). Guest members (`hostující`) use the same path as club members — they already share the registration flow today.

## Goals / Non-Goals

**Goals:**
- Add an `Edit Registration` requirement to the `event-registrations` spec with scenarios for editing SI card number, category, and (conditionally, after `#102` lands) supplementary-service selections.
- Introduce an `editRegistration` command on the `Event` aggregate that:
  - preserves the original `registeredAt`,
  - applies the same open-window guard as register/unregister (`ACTIVE` status AND before `eventDate` AND before `registrationDeadline`),
  - re-validates category against the event's category list,
  - re-validates SI card number,
  - emits a single `RegistrationEditedEvent` with old + new values.
- Expose the edit operation through a new `PUT /api/events/{eventId}/registrations/{memberId}` endpoint, authorized so that the acting user must equal `{memberId}`. A HAL-FORMS affordance is attached to the own-registration representation when editing is still allowed.
- Expose the same affordance on the `row for me` inside the event detail's registrations list (conditional on being the acting member and the window being open).

**Non-Goals:**
- Changing who can edit — no admin path, no delegation.
- Editing out-of-competition flag. Proposal `#87` (out-of-competition) is not a dependency of this change; if `#87` lands later, `editRegistration` may be extended then.
- Audit/history of edits. MVP stores the current state only; a future audit-log feature can cover change history.
- Capacity-aware category enforcement. Capacities do not exist yet; when they do (separate proposal), that proposal tightens the edit rules.
- Changing editing windows. Uses the exact same guard as register/unregister; no new "extended until start" window.
- Multi-deadline events. "I mezi termíny" from the issue is read as "anytime before the single deadline".

## Decisions

### D1 — `editRegistration` as a method on the `Event` aggregate, not on `EventRegistration`

`EventRegistration` is an immutable value object. The open-window guard lives on `Event` and needs `Event.status`, `Event.eventDate`, `Event.registrationDeadline`, plus `Event.categories` for category re-validation. Keeping the mutation on `Event` keeps all invariants in one place.

Implementation shape:
```java
// Event.java
public void editRegistration(MemberId memberId, EditRegistrationCommand command) {
    assertRegistrationsOpen();
    EventRegistration current = findRegistration(memberId)
            .orElseThrow(() -> new RegistrationNotFoundException(memberId, this.id));

    String resolvedCategory = resolveCategory(command.category());
    EventRegistration updated = current.withChanges(
            command.siCardNumber(), resolvedCategory);

    registrations.remove(current);
    registrations.add(updated);

    registerEvent(RegistrationEditedEvent.fromAggregate(
            this, memberId, current, updated));
}
```

`EventRegistration.withChanges(SiCardNumber, String)` returns a new value object copying `id` and `registeredAt` from the source — i.e., identity and timestamp are preserved; only mutable fields change.

**Alternatives considered:**
- *Expose mutation on `EventRegistration` directly.* Rejected — the value object has no visibility into `Event`-level invariants (status, deadline, category list) and would duplicate them.
- *Delete + re-add with new id.* Rejected — changes `id` (which the REST URL addresses today implicitly via `/mine`, but the id is still the stable key in the aggregate), confuses downstream listeners, and forces a second event.

### D2 — Single `RegistrationEditedEvent(memberId, eventId, oldSnapshot, newSnapshot)`

Emit one edit event rather than an `Unregistered` + `Registered` pair.

**Why:** Downstream handlers that care about the delta (future ORIS sync, notification on category move, audit) need to recognise an edit as distinct from a cancellation. Pairing events loses that signal — a listener receiving `Unregistered` alone cannot tell whether the member is really leaving.

**Payload:** both old and new snapshots (`SiCardNumber`, `category`, supplementary services once `#102` lands). This keeps listeners from having to re-query the aggregate to compute the delta.

**Alternatives considered:**
- *Unregistered + Registered pair.* Rejected — misleading semantics; noisy.
- *No event (silent update).* Rejected — breaks any future listener (ORIS sync, notification) that needs to react to registration changes.
- *`RegistrationEditedEvent` with only new values.* Rejected — forces every listener to re-query the aggregate to see what changed.

### D3 — Same open-window rule as register/unregister

Editing is gated by the same `assertRegistrationsOpen()` helper used by register and unregister. No deviations, no "allowed until event start" extension.

**Why:** Consistency is a strong default. A member who cannot unregister also cannot edit — otherwise they could change their category after the deadline, defeating the deadline's purpose. If product later wants more liberal rules for edit, it's a follow-up.

### D4 — Preserve `registeredAt`

`registeredAt` represents the member's place in the first-come-first-served queue. Editing a field should not penalise the member in that queue.

**Why:** Otherwise a member is incentivised to unregister+re-register only if they want to keep their slot by not editing — perverse incentive. Also, a planned capacity feature would be broken by an editing flow that moves members to the end of the queue.

### D5 — REST shape: `PUT /api/events/{eventId}/registrations/{memberId}` with full representation, owner authorization

Full `PUT` (not `PATCH`), because edits in the UI submit the whole form anyway (SI card + category together). The URL carries an explicit `{memberId}`. Authorization rule:

> The acting user MUST resolve to the same `MemberId` as `{memberId}`. Otherwise → `403 Forbidden`.

Implementation reuses the existing ownership pattern from `common.security.fieldsecurity`:

```java
@PutMapping("/{memberId}")
@OwnerVisible
public ResponseEntity<Void> editRegistration(
        @PathVariable EventId eventId,
        @OwnerId @PathVariable MemberId memberId,
        @Valid @RequestBody EditRegistrationRequest body) {
    // delegate to EventRegistrationPort.editRegistration(eventId, memberId, command)
}
```

`@OwnerVisible` + `@OwnerId` on the `{memberId}` path variable delegates to `OwnershipResolver`, which compares against the current user's `MemberId` claim from the JWT. No custom `@PreAuthorize` expression needed.

Not `/mine` — explicit `{memberId}` in the URL keeps the resource content-addressable, avoids a hidden "acting user" lookup, and leaves room for `#32`/`#33` (admin override) to reuse the same endpoint. Admin override simply adds `@HasAuthority(...)` alongside `@OwnerVisible` — the ownership/authority check combines with OR semantics (already supported by the framework) — no URL contract change.

Affordance is attached to the `own-registration` representation under a link rel like `edit`, and is only present when both conditions hold: the edit window is open AND the viewer is the registered member. This mirrors the existing `unregister` affordance semantics.

The `row for me` in the registrations list carries the same affordance (same HAL link embedded on the row), so the frontend can render an "Upravit" trigger on the row without a second round-trip.

**Alternatives considered:**
- *`PUT /api/events/{eventId}/registrations/mine` (implicit acting user).* Rejected — hides the subject of the action in the session, makes future admin override a URL change rather than an authorization change, and diverges from REST convention that `PUT` targets an identified resource.
- *`PATCH` with JSON Patch.* Rejected — overkill for 2–3 editable fields; HAL-FORMS template is easier to describe and validate.
- *Attach affordance only to the dedicated own-registration view.* Rejected — UX decision was "both places".

### D6 — Field validation reuses existing primitives

SI card number validation stays in `SiCardNumber` (already enforces 4–8 digits). Category re-validation reuses `Event.resolveCategory()`. No new validation code.

## Risks / Trade-offs

- **Race between two concurrent edits by the same member.** → Mitigation: the `Event` aggregate is persisted as a whole; the second save will either overwrite the first or fail on optimistic lock. Last-writer-wins is acceptable for self-edit (the member sees the final state in the response).
- **Frontend stale data: member opens edit form, deadline passes while form is open, submit fails.** → Mitigation: server returns the same "registration deadline has passed" error as register/unregister; frontend shows the error inline. No new UX affordance required.
- **Listeners that subscribe to `MemberRegisteredForEventEvent` for side effects (e.g., "welcome" email) will not fire on edits.** → This is the intended behaviour — an edit is not a new registration. Listeners that need to react to edits subscribe to `RegistrationEditedEvent` explicitly.
- **Memento / persistence format changes.** `EventRegistrationMemento` already covers the current shape; editing in place just persists the updated value. No schema migration required.
- **Scope creep from `#87` and `#102`.** The design intentionally does not take a hard dependency on either. When `#102` (supplementary services) lands, extending `editRegistration` to cover service selections is a small follow-up on the same command shape (add a field, extend the event payload). When `#87` (out-of-competition) lands, whether to include it in edit is a deliberate decision at that time.

## Migration Plan

Not applicable — additive change with no data migration. The `EventRegistration` value object gains a copy-with method; no persistence schema change.

Deployment: standard. Rollback is a code revert — no data written in a format the old code can't read.

## Open Questions

None — answered during /openspec-continue-change question round:

1. Editable fields: SI card, category, supplementary services (conditional on `#102`). Out-of-competition flag is NOT included.
2. Time window: same as unregister (before event date AND before registration deadline).
3. "I mezi termíny": interpreted as "anytime before the registration deadline" — no multi-deadline semantics.
4. Registration timestamp: preserved.
5. Domain event: single `RegistrationEditedEvent` with old + new values.
6. Audit history: out of scope for MVP.
7. UI affordance: on both the dedicated own-registration view and the `row for me` in the registrations list.
8. Capacity-aware category enforcement: out of scope (no capacity feature yet).
9. Guest members: same rules as club members.
10. Admin override: out of scope (tracked in `#32`, `#33`).
