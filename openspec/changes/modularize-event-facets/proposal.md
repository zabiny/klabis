## Why

Today every Event carries all possible fields (categories, ranking, base entry fee) regardless of its kind — a training has no use for ranking or entry fee, yet the fields exist on it anyway. As the club grows it needs different event variants and, in the future, optional add-ons such as transport, accommodation and services. A flat "god record" cannot express "this event has categories and entry fee, that one only categories" and would keep accumulating sparse, always-present fields with every new variant.

This change makes the Event modular: a small base plus independently toggleable **facets** (atomic field groups). It lays the structural foundation now so future facets (transport, accommodation, services) drop into the same pattern without reworking the Event.

## What Changes

- Introduce **EventFacet** — an atomic, toggleable group of event fields modeled as a value object inside the Event aggregate (`sealed interface EventFacet`). Three facets in this change:
  - `CATEGORIES` — race/training categories (kept as the current string list)
  - `RANKING` — ranking/series (the existing `EventRanking`)
  - `PRICING` — base entry fee (the existing `Money`)
- Each Event holds a set of **active facets** (the single source of truth for what is enabled) plus the facet data. Facets are fully independent — the admin enables any combination; no inter-facet dependencies.
- **BREAKING (internal model & API):** `categories`, `ranking` and `baseEntryFee` are no longer always-present fields of Event. They exist only when the corresponding facet is active. They move out of the flat Event payload into per-facet sub-resources.
- A facet may be **active but empty** while the event is in `DRAFT`; facet invariants are enforced when the event transitions `DRAFT → ACTIVE`.
- **EventType** gains `defaultFacets` — selecting a type pre-fills an event's facets (e.g. "Závod" → CATEGORIES + RANKING + PRICING, "Trénink" → CATEGORIES). The pre-filled set can be freely changed per event; the type only seeds a default.
- **API (HAL+FORMS):** each active facet is exposed as a sub-resource with its own template and HAL links:
  - `GET/PATCH /api/events/{id}/facets/{type}` — read/edit a facet
  - `POST /api/events/{id}/facets/{type}` — activate a facet
  - `DELETE /api/events/{id}/facets/{type}` — deactivate a facet
  - the Event response carries `_links` to each active facet (`facet:categories`, …) and a structured `facets` object
- **ORIS import** activates and fills the relevant facets (CATEGORIES, RANKING, PRICING) instead of writing flat Event fields.
- **Frontend:** the event detail renders a section per active facet, driven by the facet `_links`, reusing the existing `HalFormDisplay` + field factory.
- **Registration adapts to the new category state (no price change):** because categories become facet-scoped, registration must handle the new "Categories facet not active" state. Registration logic stays behaviorally unchanged — a category is required only when the Categories facet is active with a non-empty list; both a non-active facet and an active-but-empty facet mean "no category required" (matching today's empty-list behaviour). This is one new guard, not a redesign of registration.
- **Out of scope (deferred to a follow-up change):** computing event price from a member's selections at registration time. The facet interface is designed to allow it later (facets already hold pricing data), but no price calculation is done here.

## Capabilities

### New Capabilities
- `event-facets`: defines what an event facet is, how facets are activated/deactivated, how active facets are the source of truth, empty-facet-in-DRAFT rules, invariant enforcement on activation/publication, and the facet sub-resource API (read/edit/activate/deactivate).

### Modified Capabilities
- `events`: ranking and base entry fee become facet-scoped (exist only when RANKING/PRICING facet active); Create/Update Event and the event detail page account for facets; event-type assignment pre-fills default facets; ORIS import for ranking and fee targets facets.
- `event-types`: the event types catalog gains `defaultFacets` per type.
- `event-categories`: categories become facet-scoped (exist only when the CATEGORIES facet is active); ORIS import and ORIS sync activate/fill the CATEGORIES facet. Categories stay a plain string list in this change — turning them into rich objects with per-category pricing is a separate future change.
- `event-registrations`: registering for an event requires a category only when the Categories facet is active with a non-empty list; a non-active facet and an active-but-empty facet both mean no category is required (behaviorally identical to today's empty-list case). No other registration behavior changes.

## Impact

- **Domain** (`com.klabis.events.domain`): new `EventFacet` sealed hierarchy (`CategoriesFacet`, `RankingFacet`, `PricingFacet`), `FacetType`; `Event` gains active-facets + facet map and loses always-present `categories`/`ranking`/`baseEntryFee` as direct fields; `EventType` gains `defaultFacets`; facet invariant validation hooked into the `DRAFT → ACTIVE` transition.
- **Persistence** (`com.klabis.events.infrastructure.jdbc`): one child table per facet (`event_categories_facet`, `event_ranking_facet`, `event_pricing_facet`); the presence of a child row marks a facet active, so the per-facet columns are removed from `events` and no `active_facets` column is added. `EventMemento` composes facets from the present child rows. Schema is rewritten in place in `V001` — **no data migration** (development runs on in-memory H2 only, no production data).
- **API** (`com.klabis.events.infrastructure.restapi`): new facet sub-resource controller(s) and per-facet HAL+FORMS templates; `EventDto` becomes structured (nested `facets`, facet `_links`); `OrisEventImportService` activates/fills facets.
- **Frontend** (`frontend/src/pages/events`): event detail renders a facet section per `facet:*` link with activate/deactivate affordances; reuses `HalFormDisplay`, `eventFormFieldsFactory`, localization labels.
- **Registration** (`com.klabis.events`): `Event.resolveCategory()` and the registration affordances gain one guard for the "Categories facet not active" state; registration is otherwise behaviorally unchanged.
- **Unchanged:** registration deadlines, location/organizer/website and other base fields, event status lifecycle (only extended with facet validation), events list/filter/table, the rest of `event-registrations` (unregister, list, accommodation, transactions), `category-presets` (now fills the CATEGORIES facet).
