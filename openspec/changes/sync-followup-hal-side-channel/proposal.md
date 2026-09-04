## Why

`EventController` hands a boolean to its own postprocessor through a hand-rolled request
attribute: the controller computes whether the event is enrolled for synchronisation,
stores it under a `public static final String EVENT_SYNC_ENROLLED_ATTR` key via
`RequestContextHolder`, and `EventDetailsPostprocessor` reads it back out to decide
whether to add the `sync` link.

The mechanism itself is not new or wrong — `HalResponseContext` (`common/ui`) is built on
exactly the same technique, storing values under request attributes keyed by class name,
and the same controller method calls `HalResponseContext.setDomain(...)` and
`.embed(...)` two lines above. The problem is that `HalResponseContext` exposes slots
only for a domain object, a domain list and an embedded collection — there is no slot for
a plain flag, so this one had to be hand-rolled next to it.

The cost is a hidden coupling the compiler does not check. Two classes agree on a string
key across a controller/postprocessor boundary; if the postprocessor ever runs on a
response the controller did not produce, the flag is silently absent and the `sync` link
silently disappears. `EventRegistrationController` reaches for `RequestContextHolder`
directly too, so this is a second instance of the same gap rather than a one-off.

Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`) and deferred from it as an API change outside
that proposal's scope.

## What Changes

- Add a typed side-channel to `HalResponseContext` for values that are neither the domain
  object nor an embedded collection — a keyed put/take pair whose key is a typed token
  rather than a shared string constant, so the compiler links producer and consumer.
- Move the enrolment flag onto it: `EventController` publishes through
  `HalResponseContext`, `EventDetailsPostprocessor` reads through it, and
  `EVENT_SYNC_ENROLLED_ATTR` plus the direct `RequestContextHolder` use disappear from
  `EventController`.
- Review the pre-existing direct `RequestContextHolder` use in
  `EventRegistrationController` and move it to the same mechanism if it fits.
- Keep the reason the flag exists at all: the postprocessor must not hold
  `SynchronizationPort`, so that unrelated `@WebMvcTest` slices need not mock it. Any
  design that makes the postprocessor inject the port is out of scope here.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/events/spec.md` — unaffected. "Synchronisation State Is Reachable From
  The Event" requires the event to expose its synchronisation state to authorised users;
  the `sync` link keeps appearing under exactly the same condition, computed by the same
  controller from the same port.
- `openspec/specs/data-synchronization/spec.md` — unaffected. No requirement describes how
  a controller communicates with a postprocessor.
- `openspec/specs/application-navigation/spec.md` — reviewed because it governs HAL link
  exposure; the set of links and the authority gating them are unchanged.

**Why no spec update is needed:**

The response body is byte-identical: the same `sync` link, on the same responses, for the
same users. Only the internal plumbing that carries one boolean from the controller to the
postprocessor changes.

## Impact

- **Modules:** `common/ui` (new API surface on `HalResponseContext`), `events`
  (`EventController`, `EventDetailsPostprocessor`, possibly `EventRegistrationController`).
- **Code:** removal of one public string constant and two `RequestContextHolder` call
  sites; a small addition to a shared class used across modules.
- **Risk:** `HalResponseContext` is shared infrastructure — a change there touches every
  module that renders HAL. The addition must be purely additive; existing slots keep
  their behaviour.
- **Follow-on value:** the next controller that needs to hand a hint to a postprocessor
  gets a supported way to do it instead of copying the attribute trick a third time.
