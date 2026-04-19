## Why

GitHub issue #92 ("Chci mít možnost kdykoli před termínem (i mezi termíny) změnit příhlášku", milestone `MVP`, labels `clen klubu`, `hostujici clen`, `question`) asks for the ability to change an existing registration — not only unregister + re-register, which is today's workaround.

Current state of the `event-registrations` spec:
- `Register for Event` — create a registration with SI card + (required when event has categories) a category selection.
- `Unregister from Event` — cancel before event date and before registration deadline.
- `View Own Registration` — read-only view of one's registration.

There is **no "edit registration" requirement**. The member can indirectly change their category or SI card number by unregistering and re-registering, but that has real drawbacks:
- It loses the original registration timestamp (which can affect "first come first served" ordering when capacities are set in the future).
- It generates duplicate domain events, which makes audit/logging noisier.
- If there is a "registered members count" trigger (e.g., cap reached), the sequence unregister → re-register could lose the member's slot if another member registers in between.
- Between deadlines: the issue mentions "i mezi termíny" — likely referring to multi-event weekend registrations where some events share a common deadline. Edge case worth clarifying.

## Capabilities

### New Capabilities

<!-- None. -->

### Modified Capabilities

- `event-registrations`: add `Edit Registration` as a new requirement with scenarios for editing SI card, category, and (conditional on proposal #87 landing) the out-of-competition flag and (conditional on #102) the supplementary-service selections.

## Impact

**Affected specs:**
- `openspec/specs/event-registrations/spec.md` — new `Edit Registration` requirement. Scenarios cover: which fields are editable, when editing is allowed (before event date AND before registration deadline, same guards as unregister), field validation, error cases (editing a non-existent registration, editing after deadline).

**Affected code (backend, event-registrations module):**
- New command `editRegistration(eventId, newSiCard, newCategory, …)`.
- The registration aggregate/entity exposes an update method that preserves the `registrationTimestamp` and only changes editable fields.
- The REST endpoint: `PUT /api/events/{eventId}/registrations/mine` (or `PATCH`).

**Affected code (frontend):**
- The "Moje přihláška" view (from `View Own Registration`) gets an "Upravit" button that opens a modal with pre-filled fields (today the view is read-only + unregister).

**APIs (REST):** additive — new endpoint / affordance on the own-registration representation.

**Dependencies:**
- None required for MVP.
- Picks up new fields from #87 (out-of-competition flag) and #102 (service selections) when those land.

**Data:** none new — registration record is updated in place.

## Open Questions

All questions below MUST be answered before the next OpenSpec artifacts (specs, design, tasks) are created for this change.

1. **Which fields are editable?**
   - (a) SI card number — yes, useful if the member borrows a different chip on the day.
   - (b) Category — yes, primary use case.
   - (c) Out-of-competition flag (pending #87) — yes, once the field exists.
   - (d) Supplementary-service selections (pending #102) — yes, same logic as above.

   Recommend: all of the above, conditional on upstream proposals.

2. **Time window.** Same as `Unregister from Event` (before event date AND before registration deadline)? Recommend: yes — consistent rule. Confirm.

3. **"I mezi termíny" — what does that mean?** The phrase is from the issue title. Candidates:
   - Between multiple registration deadlines on a multi-stage event — but events today have a single deadline field, so irrelevant until multi-deadline events exist.
   - Between an early deadline and the final deadline — same as above.
   - Simply "anytime before the final deadline" — the natural reading.

   Recommend: interpret as "anytime before the registration deadline" — no new semantics needed. Confirm.

4. **Does editing preserve the registration timestamp, or update it?** Recommend: preserve. Changing fields should not punish the member in a first-come-first-served queue. Confirm.

5. **Does editing re-fire any domain events?**
   - Option A: emit a single `RegistrationEdited` event with old + new values.
   - Option B: emit `Unregistered` + `Registered` (simulate the indirect path).
   - Option C: no event — just silent update.

   Recommend: Option A. Downstream (ORIS export, notifications) can decide how to handle it.

6. **Audit.** Do we keep a history of registration edits? Recommend: for MVP, no — single current state only. A later audit-log feature can cover this.

7. **UI affordance.** Should the edit affordance also appear on the event detail page (row in the registrations section for "me") or only on the dedicated own-registration view? Recommend: both — a click on one's own row in the registrations list opens the edit modal.

8. **Category change when the chosen category is full (future capacity feature).** If capacities are added later (outside this proposal), editing from a non-full category into a full one must be rejected. Out of scope for now. Confirm.

9. **Guest members / hostující.** Can guests edit their registrations the same way? Recommend: yes, same rules. Confirm.

10. **Admin override — can an event coordinator / manager edit someone else's registration?** That is a separate issue (#32 absence recording, #33 manual registration). Recommend: this proposal covers self-edit only. Confirm.
