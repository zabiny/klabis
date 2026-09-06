## Why

`EventController` hands a boolean to its own postprocessor through a hand-rolled request
attribute: the controller computes whether the event is enrolled for synchronisation,
stores it under a `static final String EVENT_SYNC_ENROLLED_ATTR` key via
`RequestContextHolder`, and `EventDetailsPostprocessor` reads it back out to decide
whether to add the `sync` link.

The mechanism itself is not new or wrong — `HalResponseContext` (`common/ui`) is built on
exactly the same technique, storing values under request attributes keyed by class name,
and the same controller method calls `HalResponseContext.setDomain(...)` and
`.embed(...)` two lines above. The problem is that `HalResponseContext` exposes slots
only for a domain object, a domain list and an embedded collection — there is no slot for
an arbitrary hint, so this one had to be hand-rolled next to it.

The cost is a coupling the compiler does not check. Two classes agree on a string key
across a controller/postprocessor boundary. A typo in either one compiles, and the failure
mode is silent: the flag reads absent and the `sync` link simply does not appear.

The same gap has a second shape in the same module. Three postprocessors need the
`eventId` of the request they are decorating, and none of them can get it from the payload
— an event with no registrations yields an empty collection, and `EventRegistration`
carries no reference back to its event. So they reach into `RequestContextHolder` for
`HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE` and re-parse the URI template, through
two near-identical private helpers (`AccommodationListSupport.currentEventId()` and
`EventRegistrationController.currentEventId()`).

That is the postprocessor recovering a value the controller was handed directly. Every
producing method already has the id as a `@PathVariable`. Re-deriving it from the URI
couples the postprocessor to the path template — rename the variable from `eventId` and
the link vanishes silently, with nothing failing to compile — and makes the postprocessor
untestable without a bound request carrying the right URI attributes.

Raised by the quality review of the bidirectional sync engine (archived change
`2026-09-04-add-bidirectional-sync-engine`) and deferred from it as an API change outside
that proposal's scope.

## What Changes

- Add a typed context map to `HalResponseContext`, keyed by the value's runtime class:
  - `setContext(Object)` — stores the value under `value.getClass()`.
  - `<T> T getContext(Class<T> type)` — returns the stored value cast to `type`. Throws if
    nothing is stored for that type.
  - `<T> Optional<T> findContext(Class<T> type)` — empty when nothing is stored for that
    type.

  Keying by class is what makes producer and consumer compiler-linked: the type token
  *is* the key, so there is no string to mistype and no way to read a value as the wrong
  type. A lookup resolves by assignability, so reading through an interface or supertype
  works; an ambiguous match (two stored values both assignable to the requested type) is a
  programming error and throws rather than picking one.

  Several independent values can coexist, because the three call sites below publish
  unrelated records and one of them must not force the others into a single aggregate.
  Storing a second value of the *same* type replaces the first.

- **Reads must not consume.** Unlike the existing `take*` methods, `getContext` and
  `findContext` leave the value in place. `AccommodationListItemPostprocessor` runs once
  per item and reads the context on every row; a consuming read would decorate the first
  row and silently skip the rest — a defect that compiles and passes any single-item test.

- Move the event enrolment flag onto it. `EventController` publishes a record rather than
  a bare `boolean`, so a later field costs no API change. `EventDetailsPostprocessor`
  reads it through `findContext`, because that postprocessor legitimately runs on
  responses `getEvent` did not produce, where absence means "not enrolled", not "bug".
  `EVENT_SYNC_ENROLLED_ATTR` and its two `RequestContextHolder` call sites disappear.

- Move the three URI-template readers onto it. The controller publishes the `eventId` it
  already holds; the postprocessors read it from the context instead of re-parsing the
  path:
  - `EventController#getAccommodationList` → `AccommodationListPostprocessor` and
    `AccommodationListItemPostprocessor`
  - `EventRegistrationController#listRegistrations` → `RegistrationsCollectionPostprocessor`

  Both `currentEventId()` helpers and the `AccommodationListSupport` class are deleted.

- Keep the reason the sync flag exists at all: the postprocessor must not hold
  `SynchronizationPort`, so that unrelated `@WebMvcTest` slices need not mock it. Any
  design that makes the postprocessor inject the port is out of scope here.

## No Behavior Change Justification

**Specs reviewed:**

- `openspec/specs/events/spec.md` — unaffected. "Synchronisation State Is Reachable From
  The Event" requires the event to expose its synchronisation state to authorised users;
  the `sync` link keeps appearing under exactly the same condition, computed by the same
  controller from the same port. Nothing in the spec describes how the `event` relation on
  a registration or accommodation collection is derived.
- `openspec/specs/data-synchronization/spec.md` — unaffected. No requirement describes how
  a controller communicates with a postprocessor.
- `openspec/specs/application-navigation/spec.md` — reviewed because it governs HAL link
  exposure; the set of links and the authority gating them are unchanged.

**Why no spec update is needed:**

The response bodies are byte-identical: the same `sync` link, the same `event` relation,
the same per-row `self` links, on the same responses, for the same users. Only the
internal plumbing carrying those values from the controller to the postprocessor changes.

Each moved value resolves to the same result it does today. The `eventId` published by a
controller is the `@PathVariable` Spring bound from the very URI template the postprocessor
currently re-parses, so the two sources cannot disagree. The absent-context branch in
`EventDetailsPostprocessor` reproduces today's `Boolean.TRUE.equals(null) == false`
exactly, and the accommodation and registration postprocessors keep an
absence-means-no-link branch matching today's `Optional.empty()`.

`getContext`'s throwing behaviour reaches no production path: every consumer added by this
change uses `findContext`. `getContext` exists for a caller that declares the value
mandatory, and this change introduces no such caller.

## Impact

- **Modules:** `common/ui` (new API surface on `HalResponseContext`), `events`
  (`EventController` with its nested postprocessors, `EventRegistrationController`).
- **Code:** removal of one string constant, one utility class (`AccommodationListSupport`),
  two `currentEventId()` helpers and four `RequestContextHolder` call sites; a purely
  additive change to a shared class used across modules, plus small records for the
  published values.
- **Risk:** `HalResponseContext` is shared infrastructure — a change there touches every
  module that renders HAL. The addition must be purely additive; the three existing slots
  keep their behaviour, and `clear()` must drop the whole context map or values leak into
  the next request served by the same thread.
- **Behavioural risk concentrated in one place:** the non-consuming read. Every other part
  of this change fails loudly if wrong; a consuming read fails quietly on multi-item
  collections only.
- **Follow-on value:** the next controller that needs to hand a value to a postprocessor
  gets a supported, compiler-checked way to do it instead of copying either trick a third
  time.
