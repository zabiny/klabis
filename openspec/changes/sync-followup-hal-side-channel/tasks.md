## 1. Add the typed context map to HalResponseContext

- [x] 1.1 Store the context as a map keyed by the value's runtime class, under a single new request attribute. Follow the scoping the existing three slots use.
- [x] 1.2 Add `setContext(Object)` — stores under `value.getClass()`. A second value of the *same* type replaces the first; values of different types coexist.
- [x] 1.3 Add `<T> Optional<T> findContext(Class<T> type)` resolving by assignability, so a value can be read through an interface or supertype it implements.
- [x] 1.4 Add `<T> T getContext(Class<T> type)` — same lookup, throws when nothing matches.
- [x] 1.5 Throw on an ambiguous lookup: two stored values both assignable to the requested type. Picking one silently makes the result depend on map iteration order. Name both matched types in the message.
- [x] 1.6 **Reads must not consume.** `getContext` and `findContext` leave the value in the map, unlike the existing `take*` methods. Javadoc this explicitly and say why: `AccommodationListItemPostprocessor` reads once per item.
- [x] 1.7 Distinct exception messages for "nothing stored for this type" and "ambiguous match", naming the requested type in both. A message that cannot tell them apart makes this harder to debug than the string key it replaces.
- [x] 1.8 Extend `clear()` to drop the context attribute. Omitting this leaks values into the next request served by the same thread; it compiles and passes every single-request test.
- [x] 1.9 Keep the addition purely additive — `setDomain`, `setDomainList`, `embed` and their `take*` counterparts behave exactly as before.

## 2. Test the new API

- [x] 2.1 `getContext` returns the stored value for an exact type match.
- [x] 2.2 `getContext` returns the value when requested through an interface or supertype it is assignable to.
- [x] 2.3 `getContext` throws when nothing is stored for that type, while a value of an unrelated type *is* stored — proving the lookup discriminates rather than returning whatever is present.
- [x] 2.4 `findContext` returns empty in the same situation.
- [x] 2.5 Two values of different types coexist and each is retrievable. Verify this test fails against a single-slot implementation — it is the assertion that pins the map behaviour.
- [x] 2.6 A second `setContext` of the same type replaces the first.
- [x] 2.7 An ambiguous lookup throws. Construct it with two stored types sharing a supertype, then request the supertype.
- [x] 2.8 **Repeated reads return the value every time.** Read the same type twice and assert both succeed. Verify this test fails if the read is made consuming — without that check the test proves nothing about the property it exists for.
- [x] 2.9 The no-request-attributes-bound case (no `RequestContextHolder` binding) behaves as absence, matching how the existing slots treat it.
- [x] 2.10 `clear()` drops the context — a subsequent read finds nothing.

## 3. Move the event enrolment flag

- [x] 3.1 Introduce a record carrying the enrolment state rather than publishing a bare `boolean`, so a later field costs no API change. Place it where both `EventController` and its nested postprocessor can see it.
- [x] 3.2 Publish it from `EventController#getEvent` via `setContext`, replacing the `RequestContextHolder.currentRequestAttributes().setAttribute(...)` call.
- [x] 3.3 Read it in `EventDetailsPostprocessor` via `findContext` — **not** `getContext`. That postprocessor runs on responses `getEvent` did not produce, where absence means "not enrolled". `getContext` here turns a normal path into an exception.
- [x] 3.4 Confirm the absent-value branch still yields "no sync link", identical to today's `Boolean.TRUE.equals(null)`.
- [x] 3.5 Delete `EVENT_SYNC_ENROLLED_ATTR` and the private `isEventSyncEnrolled()` helper.
- [x] 3.6 Confirm `EventDetailsPostprocessor` still does not depend on `SynchronizationPort`, so `@WebMvcTest` slices need not mock it. This is the whole reason the indirection exists.

## 4. Move the accommodation-list eventId

- [x] 4.1 Introduce a record carrying the accommodation-list `eventId`. Do not reuse the enrolment record — they are published by different endpoints and share nothing but a name.
- [x] 4.2 Publish it from `EventController#getAccommodationList`, which already holds `eventId` as a `@PathVariable` and already calls `HalResponseContext.setDomainList(...)`.
- [x] 4.3 Read it in `AccommodationListPostprocessor` via `findContext`, keeping an absence-means-no-link branch equivalent to today's `Optional.empty()`.
- [x] 4.4 Read it in `AccommodationListItemPostprocessor` the same way. This one runs per item — confirm every row still gets its `self` link, using a fixture with **at least two** registrations. A single-item fixture cannot detect a consuming read.
- [x] 4.5 Delete the `AccommodationListSupport` class entirely.
- [x] 4.6 Check `getAccommodationListAsCsv`: it renders CSV, runs no postprocessor, and must not need the context. Confirm it is unaffected rather than assuming it.
- [x] 4.7 Update the class javadoc on both postprocessors — both currently state the eventId "comes from the URI template", which stops being true.

## 5. Move the registrations-collection eventId

- [ ] 5.1 Introduce a record carrying the registrations-collection `eventId`, published from `EventRegistrationController#listRegistrations`.
- [ ] 5.2 Read it in `RegistrationsCollectionPostprocessor` via `findContext`, keeping the absence-means-no-link branch.
- [ ] 5.3 Delete `EventRegistrationController.currentEventId()`.
- [ ] 5.4 Check whether `EventController#getEvent` also produces `RegistrationSummaryDto` content through `HalResponseContext.embed(...)` and whether that path reaches this postprocessor. If it does, decide deliberately whether the `event` relation should appear there — today it does not, because no URI variable named `eventId` is bound on that request. Do not change that behaviour by accident.

## 6. Cleanup and verification

- [ ] 6.1 Remove `RequestContextHolder`, `RequestAttributes` and `HandlerMapping` imports that become unused in both controllers. Verify per file rather than globally — check nothing else in each file still uses them.
- [ ] 6.2 Confirm no postprocessor in `events` reads `HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE` any more.
- [ ] 6.3 Confirm the event detail response is unchanged: the `sync` link appears for the same events and users. Check an enrolled event, a non-enrolled event, and a response from a different endpoint whose postprocessor runs with no context set.
- [ ] 6.4 Confirm the accommodation list response is unchanged: the collection-level `event` relation and a `self` link on every row.
- [ ] 6.5 Confirm the registrations collection response is unchanged: the `event` relation still present.
- [ ] 6.6 Run the full backend test suite; all tests compile and pass.
- [ ] 6.7 Code review, with attention to `HalResponseContext` being shared across modules — the three existing slots and `clear()` semantics must be verified unchanged for their other callers, not just for the new ones. Review the non-consuming read specifically; it is the one property whose violation is silent.
- [ ] 6.8 Commit.
