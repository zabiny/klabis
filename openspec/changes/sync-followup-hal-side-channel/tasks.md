## 1. Add the side-channel to HalResponseContext

- [ ] 1.1 Design a typed key so producer and consumer are linked by the compiler, not by a shared string constant.
- [ ] 1.2 Add the put/take pair to `HalResponseContext`, matching the request-attribute scoping and cleanup of the existing slots.
- [ ] 1.3 Keep the addition purely additive — `setDomain`, `setDomainList` and `embed` behave exactly as before.
- [ ] 1.4 Unit-test the new slot, including the absent-value case (no request attributes bound).

## 2. Move the event enrolment flag onto it

- [ ] 2.1 Publish the flag from `EventController` through `HalResponseContext`.
- [ ] 2.2 Read it in `EventDetailsPostprocessor` through the same mechanism.
- [ ] 2.3 Delete `EVENT_SYNC_ENROLLED_ATTR` and the direct `RequestContextHolder` use from `EventController`.
- [ ] 2.4 Confirm the postprocessor still does not depend on `SynchronizationPort`, so `@WebMvcTest` slices need not mock it.

## 3. Fold in the pre-existing instance

- [ ] 3.1 Review the direct `RequestContextHolder` use in `EventRegistrationController` and move it to the new mechanism if it fits the same shape.
- [ ] 3.2 If it does not fit, record why in the change rather than forcing it.

## 4. Verification

- [ ] 4.1 Confirm the event detail response is unchanged: the `sync` link appears for the same events and the same users as before.
- [ ] 4.2 Run the full backend test suite; all tests compile and pass.
- [ ] 4.3 Code review, with attention to the shared `HalResponseContext` being used by other modules.
- [ ] 4.4 Commit.
