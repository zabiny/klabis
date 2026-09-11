---
name: event-aggregate-signature-growth
description: How to grow Event / EventRegistration factory signatures without touching ~60 call sites
metadata:
  type: project
---

`Event.reconstruct(...)` and `EventRegistration.reconstruct(...)` are called positionally from ~14 and ~40 sites respectively (production mementos, `EventTestDataBuilder`, calendar module tests, controller tests). `event.registerMember(memberId, siCard, categoryId)` from ~8 sites. `EventsDataBootstrap` builds `new Event.CreateEvent(...)` positionally at 27 sites.

**Why:** these signatures cannot be extended in place without a mechanical sweep of every caller.

**How to apply:** when adding fields to `Event`/`EventRegistration` factories, keep the old-arity method/constructor as a **delegating overload** with sensible defaults for the new params, and add the new full-arity one alongside. `CreateEvent`/`UpdateEvent` use `@RecordBuilder` so the builder is additive, but positional `new Event.CreateEvent(...)` in `EventsDataBootstrap` needs a non-canonical delegating constructor on the record. This is how `event-shared-transport-accommodation` Section 2 added the four boolean flags with zero call-site churn outside the touched files.

Position of new fields in `reconstruct`: after `baseEntryFee`, before `List<EventRegistration> registrations, AuditMetadata auditMetadata` (the trailing structural params). In `EventRegistration.reconstruct`: after `registeredAt`.
