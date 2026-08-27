---
name: hal-collection-type-blindness
description: HalResponseBodyAdvice.wrapCollection/wrapPage must pass an explicit element ResolvableType or every module's collection-level postprocessor leaks into every list endpoint
metadata:
  type: project
---

`HalResponseBodyAdvice` (com.klabis.common.ui) wraps a controller's plain `List<Dto>` / `Page<Dto>`
into a `CollectionModel` / `PagedModel` and runs it through a fresh
`RepresentationModelProcessorInvoker`.

**The bug (fixed 2026-08-27):** `CollectionModel.of(items)` with no fallback type, plus the 1-arg
`invokeProcessorsFor(model)`, makes the invoker resolve the collection's element DTO type to
`Object`. Then every collection-level `RepresentationModelProcessor<CollectionModel<EntityModel<?>>>`
in the whole app context matches — e.g. `GET /api/family-groups` came back carrying calendar's
`_links.next`/`_links.prev` and a `createCalendarItem` `_template`, plus `createEventType`,
`createGroup`, etc.

**Why it hides:** the leak only appears once some *earlier* `CollectionModel` response of a
*different* DTO type has run on the same request-handling thread (Spring `ResolvableType` state).
A single isolated `GET /api/family-groups` in a fresh `@SpringBootTest` does NOT reproduce it —
you must first hit another list endpoint (e.g. `/api/calendar-items`) in the same test method.
A `@WebMvcTest` slice can never reproduce it (only registers the controller-under-test's own
postprocessors).

**The fix:** derive the concrete DTO type from the controller method's declared return type
(`ResponseEntity<List<X>>` -> `X`) via `ResolvableType.forMethodParameter`, then:
- `CollectionModel.of(items).withFallbackType(ResolvableType.forClassWithGenerics(EntityModel.class, X))`
  (and the same `.withFallbackType` on the `PagedModel` — `PagedModel` preserves its `PageMetadata`
  across that call, `CollectionModel.withFallbackType` would not)
- call the 2-arg `invokeProcessorsFor(model, ResolvableType for CollectionModel<EntityModel<X>>)`.

The fallback type also covers the empty-collection case, where `CollectionModel` cannot derive the
element type from content and the invoker's empty-branch matches on `getResolvableType()`.

**Regression guard:** `HalCollectionCrossModuleLeakIntegrationTest` — full `@SpringBootTest`,
calls `/api/calendar-items` then asserts `/api/family-groups` carries only `createFamilyGroup`.
