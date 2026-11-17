## 1. Domain: category changes and resolution (TDD)

- [x] 1.1 Red: `EventTest` resolution cases — a change with an existing id keeps the category's id and `orisId` and adopts the new name and fee; an unknown id is rejected with `UnknownEventCategoryException`; a change without an id gets a fresh id; a current category whose id has no change is dropped; duplicate names are still rejected after resolution; empty changes clear all categories
- [x] 1.2 Red: error order — `update` on a FINISHED event whose command names a foreign category id reports the unknown category, as the mapper did before the service was called
- [x] 1.3 Green: add `EventCategoryUpdate` in `com.klabis.events`; change `UpdateEvent.categories` to `categoryChanges`; build id-bearing changes in `from(Event)` from a copied list; resolve the changes at the top of `update`; move `UnknownEventCategoryException` into `com.klabis.events` and make it public
- [x] 1.4 Refactor: keep the resolution in one private method and run `validateCategories` on its result
- [x] 1.5 Verify: `events` domain slice green (`EventTest`, `SiPolicyTest`)

## 2. REST adapter: translation only

- [x] 2.1 Rewrite the three id-semantics tests in `EventControllerTest` to assert the command's `categoryChanges` — id present keeps the id, id absent yields a null id, an absent `categories` field keeps the baseline changes
- [x] 2.2 Green: `UpdateEventRequestMapper` maps request rows to `EventCategoryUpdate` and keeps `PatchOverlay` for every field; delete `mergeCategories` and its javadoc paragraph
- [x] 2.3 Verify: `events` restapi slice green (`EventControllerTest`, `EventRegistrationControllerTest`)

## 3. Remaining producers and tests

- [x] 3.1 Migrate `UpdateEvent` construction that sets categories to `EventCategoryUpdate` — `EventJdbcRepositoryTest`, `OrisEventSyncScenarioIntegrationTest`, and any `EventTest` call site still passing `EventCategory`
- [x] 3.2 Confirm `EventManagementCrossModuleTest` PATCH bodies (no `categories` field) still pass unchanged, and that one PATCH that does set categories round-trips
- [x] 3.3 Verify: affected `events` slices green

## 4. Verification

- [ ] 4.1 `com.klabis.events` module slice green
- [ ] 4.2 Full backend suite green (`SPRING_MODULITH_TEST_SKIP_OPTIMIZATIONS=true --rerun-tasks`)
- [ ] 4.3 `openspec validate deepen-event-category-updates --strict` passes
