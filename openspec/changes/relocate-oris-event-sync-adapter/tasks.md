## 1. Safety net before moving anything

- [x] 1.1 Read `OrisEventProjectionMapperTest` and confirm what it already guarantees: both sides hash equally (`fromEvent` vs `fromOrisFields`), differing data hashes differently, and `resolvedEventTypeId` never affects the hash. These three must keep passing unchanged through the whole change — they are the regression net for design.md's first risk.
- [x] 1.2 Add a test pinning the exact canonical JSON of a fully-populated `OrisEventProjection` (`SyncProjectionCodec.toCanonicalJson`), asserting the literal string. `SyncProjectionCodecTest` only covers a local `TestProjection`, so no test currently fixes this projection's field names or field set. Write it now so it can fail honestly later, rather than after the move when it would just be fitted to whatever the new code emits.
- [x] 1.3 Run the full backend suite and record the baseline. **Baseline measured 2026-09-17 on `claude/klabis-oris-sync-engine-t9sy9q`: 3489/3489 passing, zero failures.** `ModularEventsTest` and `EventLoggingTests` were expected to fail on clean `main` but pass here, so there are no pre-existing failures to discount — any failure in a later phase is a regression of this change.

## 2. Move `@OrisIntegrationComponent` to `com.klabis.common`

- [x] 2.1 Move the annotation to `com.klabis.common`, directly in the package beside `ClockConfiguration` and `ClubProperties` (design.md D5). Content is unchanged: `@Profile("oris")` + `@Component`.
- [x] 2.2 Update the import in all seven applying classes: `OrisEventImportService`, `OrisEventBulkImportService`, `OrisBulkSyncService`, `EventsSyncListener`, `OrisEventController`, `OrisEventFieldsGatewayService`, `OrisEventSyncAdapter`. Also update `OrisControllerTest` and any other test referencing it.
- [x] 2.3 Verify `grep -rn "import com.klabis.oris" backend/src/main/java/com/klabis/events/` returns nothing — the `events → oris` edge should now be gone entirely.
- [x] 2.4 Verified the `@Profile("oris")` gating still works. Done with an `ApplicationContextRunner` test (`backend/src/test/java/com/klabis/common/OrisIntegrationComponentTest.java`) rather than a full app start: it asserts an annotated bean registers with `spring.profiles.active=oris` and does not register without it. The annotation's whole job is that gating, and a broken move fails silently in the "always on" direction.

## 3. Move the adapter into `events.infrastructure.orissync`

- [x] 3.1 Create `com.klabis.events.infrastructure.orissync` and move `OrisEventSyncAdapter`, `OrisEventProjection` and `OrisEventProjectionMapper` into it (design.md D1). Carry `OrisEventProjection`'s record component list over character-for-character, `@JsonIgnore` on `resolvedEventTypeId` included.
- [x] 3.2 Move `OrisEventFieldsToProjectionMapper`'s body into the new package as a package-private helper — the external read keeps going `EventDetails → OrisEventFields → OrisEventProjection` (design.md D4). Do not write a second `EventDetails` mapper.
- [x] 3.3 Move the four tests: `OrisEventSyncAdapterTest`, `OrisEventProjectionMapperTest`, `OrisEventSyncAdapterIntegrationTest`, `OrisEventSyncScenarioIntegrationTest`. Assertions stay as they are; only packages and imports change.
- [x] 3.4 Run the suite. At this point the gateway still exists and the adapter still calls it — behaviour must be identical to 1.3's baseline.

## 4. Expose the `events.application` seam

- [x] 4.1 Seam decided (design.md open question 1): a single public `@Component`, `OrisEventFieldsReader` in `events.application`, exposing `readOrisFields(int orisId)`. The read half turned out to be genuinely shared rather than merely a boundary bridge — `OrisEventImportService` already consumed `readOrisFields` for `Event.createFromOris` — so one named collaborator serves both the import path and the adapter. The write half has no second consumer and folds into `applyToLocal` in phase 5 instead.
- [x] 4.2 Implement it in `events.application`, reusing `OrisEventDetailsMapper` and the existing `resolveEventTypeFromOrisDiscipline` logic verbatim. Keep `@Transactional(readOnly = true)` — it reads `EventTypeRepository`.
- [x] 4.3 Confirm `OrisEventImportService` can use the same seam, so the `EventDetails` mapping still exists exactly once across the import and sync paths.

## 5. Fold the gateway into the adapter

- [ ] 5.1 Move `readOrisFields`'s body into `OrisEventSyncAdapter.readExternal`, via the 4.2 seam. Preserve design.md D12: no transaction may span the external HTTP call.
- [ ] 5.2 Move `applyOrisSync`'s body into `applyToLocal`, building `EventSyncFromOrisBuilder` directly from the `OrisEventProjection` — this is what makes `OrisEventProjectionToFieldsMapper` unnecessary. Keep `@Transactional`; the `EventRepository.save` at the end depends on it.
- [ ] 5.3 Carry `warnIfSyncRemovesCategoriesWithRegistrations` across verbatim. It is the only diagnostic on this path and its loss is invisible until an event silently drops a category that had registrations (design.md D2).
- [ ] 5.4 Reassemble `RegistrationDeadlines`, `Money`, `EventRanking` and `EventCategory` from the projection's flat fields inside `applyToLocal`, matching `OrisEventProjectionToFieldsMapper`'s logic exactly — including `RegistrationDeadlines.of(d1, d2, d3)` and the null guards on ranking and fee.
- [ ] 5.5 Delete `OrisEventFieldsGateway`, `OrisEventFieldsGatewayService` and `OrisEventProjectionToFieldsMapper`.
- [ ] 5.6 Verify `OrisEventFields` still exists and is still used by `OrisEventImportService` for `Event.createFromOris` (design.md D3 — the types are not merged), and that it no longer appears in any port signature.

## 6. Verify nothing moved that should not have

- [ ] 6.1 Run the full suite and compare against 1.3's baseline. The three hash guarantees from 1.1 and the canonical-JSON pin from 1.2 must pass **without modification** — if either needed editing, the projection's shape changed and the change is wrong.
- [ ] 6.2 Start the application context with the `oris` profile and confirm no bean cycle (design.md risk 3). The adapter now holds an `EventRepository` dependency it did not have before. If a cycle appears, use `ObjectProvider` on the adapter registry — do not restore the port.
- [ ] 6.3 Exercise one inward sync end to end against a real ORIS event: import, edit nothing, run a pass, confirm `NOTHING_TO_DO`. Then change a field on the ORIS side and confirm the pass writes it.
- [ ] 6.4 Confirm `/api/oris/events` and the four `OrisEvents` endpoints are unchanged — no controller moved, so the generated `*Api` interfaces should be untouched. A diff of `build/generated/openapi/` against the baseline is the quickest check.

## 7. Promote `com.klabis.oris` to a Modulith module

- [ ] 7.1 Add `com.klabis.oris.package-info` with `@ApplicationModule` (design.md D6). This step is severable — if it proves awkward, the rest of the change stands on its own.
- [ ] 7.2 Run `ModuleStructureVerificationTest`. Confirm the generated `OrisImportApi`, `OrisEventSummary` and `OrisEventSummaryBuilder` stay visible to Spring MVC (they sit in the module's root package, so they should be exposed by default). If the declaration restricts them, fall back to `Type.OPEN`.
- [ ] 7.3 Confirm `oris`'s only remaining edge into `events` is `ImportedOrisEventsPort`, and that the test now fails if a `com.klabis.events.domain` import is reintroduced into `oris` — the point of the promotion is that this boundary is checked, not assumed.
- [ ] 7.4 Update `ModuleStructureVerificationTest`'s javadoc: it lists known boundary violations as technical debt, and this change removes a category of them.

## 8. Close out

- [ ] 8.1 Update `sync-skip-self-inflicted-dirty-marker`'s proposal, which describes the call path `OrisEventSyncAdapter.applyToLocal → OrisEventFieldsGatewayService.applyOrisSync`. No file overlap, but that prose is now wrong.
- [ ] 8.2 Grep the codebase and `openspec/` for lingering references to `com.klabis.oris.eventsync`, `OrisEventFieldsGateway` and the two deleted mappers — javadoc in `sync` and `events` cites them by name.
- [ ] 8.3 Note in design.md that the storage half of the projection risk stays dormant until the Postgres migration, so whoever performs it knows to check the projection shape has not drifted.
