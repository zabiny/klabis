## Why

GitHub issue #113 asks for a club-internal registration deadline set ahead of the ORIS one ("termin prihlasek -1D"). The club's registrar collects members' registrations in klabis and then forwards them to ORIS as a single entry. If klabis accepted registrations right up to the ORIS deadline, the registrar would have no time to do that — a member registering minutes before the upstream cut-off would silently miss the event.

The fix is a buffer: klabis closes registrations a fixed period before ORIS does.

**This is expected to be temporary.** Once automatic forwarding of registrations to ORIS is implemented and has proven reliable, the buffer loses its purpose and the internal deadline should collapse back onto the ORIS one. The design must therefore favour the smallest thing that works and must be cheap to remove — a single club-level value, not a per-group or per-event mechanism. Anything that spreads the offset across the domain will be expensive to unwind later.

Split out of `gh-113-oris-auto-sync` because it is independent of how an event arrives: an event imported by hand needs the same buffer as one imported by a schedule.

## What Changes

- **A single club-level offset.** One value, configured once, applied to every ORIS-imported event: the internal registration deadline is the ORIS deadline minus that offset.
- **Members see and are bound by the internal deadline.** Where a registration deadline is shown to a member, and where the system decides whether registration is still open, it is the internal deadline that counts.
- **The ORIS deadline remains visible** to managers, so the relationship between the two is not a mystery when a member asks why registration closed early.
- **Events not imported from ORIS are unaffected** — their deadline is whatever the manager set, with no offset applied.

Explicitly **not** in this change: per-group offsets (an earlier variant of the idea — dropped), per-event manual offsets, a manager's ability to reopen registration after the internal deadline (issue #99, "koho mam dohlasit rucne"), and automatic forwarding of registrations to ORIS — which is the thing that will eventually make this change obsolete.

## Capabilities

### Modified Capabilities

- `events`: an ORIS-imported event's effective registration deadline is derived from the upstream deadline and the club-level offset; both are visible to a manager.
- `event-registrations`: whether registration is open is decided against the internal deadline.

## Impact

**Affected code (backend):**

- `RegistrationDeadlines` (`com.klabis.events.domain`) — the value object already holds up to three deadlines (`deadline1..3`) and already answers `registrationsOpen(today)` and `nextRelevant(today)`. This is the single place where the offset has to take effect, which is what keeps the change small and reversible.
- Club-level configuration holding the offset.
- The synchronisation projection for ORIS events (`OrisEventProjection`) carries the upstream deadlines. The offset must **not** be baked into the projection or the stored ORIS-owned value — otherwise the synchronisation engine would compare a derived value against the upstream one and report a permanent false conflict on every pass. The offset is applied when the deadline is *used*, not when it is *stored*. This is the main implementation risk of this change.

**APIs (REST):** the deadline a member sees becomes the internal one; the upstream deadline is exposed alongside it for managers. Additive for the configuration itself.

**Data:** one configuration value. No change to how event deadlines are stored.

**Dependencies:** none new.

**Frontend:** where a registration deadline is displayed, it is the internal one; the event detail shows the ORIS deadline to managers for context.

## Open Questions

1. **Where does the offset live?** Application configuration (a property) is the cheapest and most honest expression of "this is temporary", but changing it needs a redeploy. A club-level setting on a settings page is friendlier but more to build and more to remove later. Given the intent to delete this feature, configuration seems right — to confirm.

2. **Is "-1D" a whole day or a time of day?** "Minus 24 hours" and "the day before at 23:59" differ when the ORIS deadline has a time component. Note that `RegistrationDeadlines` stores `LocalDate`, not an instant, which suggests day-granularity is the natural fit — to confirm against how the deadline is enforced today.

3. **Does the offset apply to all three deadlines or only the first?** An ORIS event can carry up to three successive deadlines. Applying it to each keeps the relationship uniform; applying it only to the last (the real cut-off) is less disruptive. To decide.

4. **What if the offset pushes the deadline into the past?** For an event imported shortly before its ORIS deadline, the internal deadline may already have passed at import time. Is the event imported with registration closed, or is the offset skipped in that case?

5. **Do existing registrations survive a shortened deadline?** Applying the offset to already-imported events may retroactively close a registration window that is currently open. Existing registrations must stay valid; the question is whether the change applies to existing events at all, or only to newly imported ones.
