# Implementation Tasks

Organized as **vertical end-to-end slices**: each slice cuts through domain → persistence → API → frontend → tests and is independently committable and testable (per project convention: prefer vertical slices, each phase independently committable).

Slice 1 is a walking skeleton — it delivers the PRICING facet end-to-end **and** establishes the shared facet mechanism (sealed `EventFacet`, the facets map on `Event`, the Memento composition rule, the facet sub-resource controller pattern, the frontend `FacetSection`). Slices 2–3 then plug additional facets into that mechanism. Slices 4–6 layer type-driven pre-fill, ORIS, and cleanup on top.

Each slice follows TDD (Red-Green-Refactor) and runs tests via the `test-runner` agent before commit.

## 1. Slice: Pricing facet end-to-end (walking skeleton + shared mechanism)

- [ ] 1.1 Domain: introduce `FacetType` enum (PRICING) and `sealed interface EventFacet` with `type()` and `validateForActivation()`; add `PricingFacet` (holds base entry fee; may be empty/partial; construction-time validation rejects only internally contradictory data)
- [ ] 1.2 Domain: add `facets: Map<FacetType, EventFacet>` to `Event` with operations to activate, deactivate, edit a facet, and a derived active-set (`facets.keySet()`); enforce facets editable only in DRAFT/ACTIVE
- [ ] 1.3 Domain: enforce facet completeness on publish — `DRAFT → ACTIVE` calls `validateForActivation()` on every active facet and blocks the transition (with which-facet feedback) if any is incomplete
- [ ] 1.4 Domain tests: facet activation/deactivation, empty-in-draft allowed, publish blocked by incomplete pricing facet, publish succeeds when complete and when no facets
- [ ] 1.5 Persistence: add `event_pricing_facet` child table (event_id PK/FK, amount, currency) to `V001`; remove `base_entry_fee_*` columns from `events`
- [ ] 1.6 Persistence: `EventMemento` maps the pricing child memento (nullable); `toEvent()` adds `PricingFacet` when the child row is present; `from(Event)` writes/deletes the child row from the facets map (presence = active marker)
- [ ] 1.7 Persistence tests: round-trip an event with/without pricing facet, deactivation deletes the row, empty pricing facet persists
- [ ] 1.8 API: facet sub-resource controller pattern — `GET/PATCH /api/events/{id}/facets/pricing`, `POST` (activate), `DELETE` (deactivate); each operation loads/mutates/saves the whole Event aggregate; per-facet HAL+FORMS template (`updatePricingFacet`)
- [ ] 1.9 API: structured `EventDto` carries a nested `facets` object (pricing only for now) and `_links` to each active facet (`facet:pricing`); inactive facets omitted; activation affordance for inactive facets
- [ ] 1.10 API tests: activate/deactivate/edit pricing facet, facet link present only when active, activate/deactivate require EVENTS:MANAGE, editing facet data requires EVENTS:MANAGE or being a coordinator of that event (and not a coordinator of another event), publish-blocked error surfaced
- [ ] 1.11 Frontend: `FacetSection` wrapper that renders one facet's `HalFormDisplay` from its `facet:*` link, plus activate/deactivate affordances; wire a pricing section into `EventDetailPage`. `FacetSection` MUST be agnostic to the facet's template shape — fully delegate field rendering to `HalFormDisplay`/the field factory, not hardcode scalar fields — so a composite-field facet (e.g. ranking in Slice 2) plugs in without rework
- [ ] 1.12 Frontend: regenerate OpenAPI types; add localization labels for the pricing facet/section and activate/deactivate actions
- [ ] 1.13 Frontend tests: pricing section shown only when facet active, manager can activate/deactivate/edit, controls hidden without EVENTS:MANAGE
- [ ] 1.14 Run backend + frontend tests (test-runner), refresh backend static resources, code review, commit

## 2. Slice: Ranking facet end-to-end

- [ ] 2.1 Domain: add RANKING to `FacetType` and `RankingFacet` to the sealed hierarchy (holds `EventRanking`; partial allowed, `validateForActivation()` requires a ranking value)
- [ ] 2.2 Domain tests: ranking facet activation, empty-in-draft, publish completeness
- [ ] 2.3 Persistence: add `event_ranking_facet` child table (level_id, level_short_name, level_name) to `V001`; remove `level_*` columns from `events`; extend `EventMemento`
- [ ] 2.4 Persistence tests: ranking facet round-trip and deactivation
- [ ] 2.5 API: `…/facets/ranking` operations + `updateRankingFacet` template; add `ranking` to the structured `EventDto.facets` and `facet:ranking` link
- [ ] 2.6 API tests: ranking facet activate/deactivate/edit
- [ ] 2.7 Frontend: ranking section in `EventDetailPage` via `FacetSection`; reuse the ranking composite field in the field factory; labels; tests
- [ ] 2.8 Run tests, refresh static resources, code review, commit

## 3. Slice: Categories facet end-to-end

- [ ] 3.1 Domain: add CATEGORIES to `FacetType` and `CategoriesFacet` (holds the `List<EventCategory>` (stable id, optional `orisId`, name, optional fee override, unique names); empty list allowed even when active; `validateForActivation()` permits an empty list per current category semantics)
- [ ] 3.2 Domain: move `categories` off `Event`'s base fields into `CategoriesFacet`; update base-field references
- [ ] 3.3 Domain: adapt registration to the new "Categories facet not active" state — `resolveCategory()` requires a category only when the Categories facet is active AND its list is non-empty; a non-active facet and an active-but-empty facet both mean "category not required" (preserves today's empty-list behaviour). Covers `registerMember()` and `editRegistration()`
- [ ] 3.3a Domain: refuse to deactivate the Categories facet while any registration of the event holds a category from it, reporting how many registrations prevent it (registrations are looked up through the existing registration port, not by reaching into the registration aggregate)
- [ ] 3.4 Domain tests: categories facet activation/edit, empty list allowed; **regression** for registration — register/edit on an event without the Categories facet (no category required), on an active-but-empty facet (no category required), and on a non-empty facet (category required and validated)
- [ ] 3.5 Persistence: add the marker table `event_categories_facet` (event_id PK/FK) to `V001` and make the existing `event_categories` rows reference it; extend `EventMemento` so the facet is present iff the marker row exists and carries the category rows
- [ ] 3.6 Persistence tests: categories facet round-trip with ids, `orisId` and fee override preserved, active-but-empty facet (marker row without category rows), deactivation removes marker and category rows
- [ ] 3.7 API: `…/facets/categories` operations + `updateCategoriesFacet` template; add `categories` to structured `EventDto.facets` and `facet:categories` link
- [ ] 3.8 API: registration affordance offers a `category` option list only when the Categories facet is active with a non-empty list; otherwise no category field is offered (replaces the unconditional `Map.of("category", event.getCategories())` in registration/edit affordances)
- [ ] 3.9 API tests: categories facet activate/deactivate/edit, deactivation refused (with the count) while registrations hold a category; registration affordance has no category field when the facet is inactive or empty, and has it when non-empty
- [ ] 3.10 Frontend: categories section in `EventDetailPage` via `FacetSection`; keep the category preset picker wired to the categories facet field; registration form shows the category field only when the affordance offers it; labels; tests
- [ ] 3.11 Run tests, refresh static resources, code review, commit

## 4. Slice: Event type default facets + pre-fill on create

- [ ] 4.1 Domain: add `defaultFacets: Set<FacetType>` to `EventType`; on Create Event, pre-fill the event's facets from the selected type's default facets; clearing/changing the type does not alter already-active facets
- [ ] 4.2 Domain tests: pre-fill from type, manager adjusts facets afterwards, type change/clear leaves facets intact
- [ ] 4.3 Persistence: add `default_facets` to `event_types` (in `V001`); map in the EventType memento; update `BootstrapDataLoader` seed types ("Závod" → CATEGORIES+RANKING+PRICING, "Trénink" → CATEGORIES)
- [ ] 4.4 API: expose and edit `defaultFacets` in the event types catalog (create/update); include default-facet info so Create Event can pre-fill
- [ ] 4.5 API tests: create event with a type pre-fills its facets; catalog set/edit of default facets
- [ ] 4.6 Frontend: event types admin page edits default facets; create-event flow reflects pre-filled facets; labels; tests
- [ ] 4.7 Run tests, refresh static resources, code review, commit

## 5. Slice: ORIS import and synchronisation target facets

- [ ] 5.1 Sync adapter: `OrisEventProjectionMapper` / `OrisEventDetailsReader` / `OrisEventSyncAdapter` activate and fill CATEGORIES (from classes), RANKING (from level), PRICING (max class fee) facets when `pullAndEnroll` creates the event and on every later pass; absent ORIS data → facet not active. `OrisEventImportService` stays the unchanged `pullAndEnroll` entry point
- [ ] 5.2 Sync adapter: the projection used for change detection reads the facets, so activating, editing or deactivating RANKING or PRICING on an ORIS-imported event is detected as a manager's edit to an ORIS-owned field and waits for a decision instead of being overwritten; categories keep matching by `orisId`, manual categories and fee overrides never cause a decision; category removal in ORIS preserves existing registrations and logs a warning
- [ ] 5.3 Tests: import with/without ranking, with/without usable fee, with/without classes; sync refresh; category removal with registrations; facet deactivated by a manager then synchronised → conflict, not silent restore; extend the existing sync scenario/integration tests rather than adding a parallel set
- [ ] 5.4 Verify import/sync round-trips through persistence and the structured EventDto; adjust API tests as needed
- [ ] 5.5 Run tests, code review, commit

## 6. Slice: Cleanup and finalization

- [ ] 6.1 Remove the old flat ranking/fee/categories fields and their request/response handling from `EventDto`, `CreateEventRequest`, `UpdateEventRequest` and mappers (now facet-scoped)
- [ ] 6.2 Update `BootstrapDataLoader` sample events to create facets where relevant; verify H2 startup and seed data
- [ ] 6.3 Update affected docs (developer manual / event-driven architecture notes if they reference flat event fields)
- [ ] 6.4 Full backend + frontend test run (test-runner), refresh static resources, final code review
- [ ] 6.5 Update `tasks.md` completion state; run `openspec validate modularize-event-facets --strict`; commit
