## Context

Under ADR-002 controllers return plain DTOs; `HalResponseBodyAdvice` re-wraps them and runs `RepresentationModelProcessor` postprocessors. The advice only wraps when the request's selected content type is HAL, so a non-HAL response passes the payload through unmodified. ADR-003 fixes link generation behind `klabisLinkTo`/`klabisAfford`; ADR-004 keeps envelope structure in the bundler.

The controller→postprocessor boundary is currently fed by four mechanisms:

1. `HalResponseContext.setDomain` / `setDomainList` — the aggregate(s) behind the payload.
2. `HalResponseContext.setContext(Object)` — a typed request-scoped map for flags/hints.
3. `HalResponseContext.embed(Collection, Class)` — a nested collection for `_embedded`, built by the controller.
4. Postprocessor-specific static setters writing raw request attributes directly (`MembershipFeeTierListPostprocessor.setActiveCampaign`, `MembershipFeeTierListRulesPostprocessor.setOptions`, `FeeSelectionCampaignListPostprocessor.setLevelOptions`).

Audit of the current state:

- **Controllers that build HAL directly:** `FreeGroupController` (`toGroupResponse`, `buildOwnerModel`, `buildMemberModel`), `FamilyGroupController` (`toFamilyGroupResponse`, `buildChildModel`), `TrainingGroupController` (`toTrainingGroupResponse`, `buildLimitedGroupResponse`, `buildMemberModel`), and `InvitationModelBuilder`. Root cause: their payload records carry `List<EntityModel<X>>` (`x-hal-entity-items: true`) and the advice only invokes processors on the top-level model, so nested items are hand-wrapped.
- **Eager postprocessor-only lookups:** `MemberController` (`ClubKeyHeld`, `EnrolledMemberIds`), `DisciplineController` (`EnrolledDisciplineIds`), `EventController` (`EnrolledEventIds`, `AccommodationListContext`), `EventRegistrationController` (`RegistrationsCollectionContext`), plus the sync enrolment queries behind them. `EventController.getEvent` also builds `RegistrationSummaryDto`s and `MembershipFeeGroupController.getFeeGroup` builds `MemberInGroupResponse`s solely for `embed`.
- **Controller-computed wrapper/option data:** `MemberFeeSummaryController.FeeSummaryView.groupOptions`, `MembershipFeeTierController.PaymentRuleDomain`, `EventRegistrationController.RegistrationView.prefill`, `MemberAccountController.AccountTransaction`, plus the option lists passed through the static setters.
- **Dead dependency:** `EventRegistrationController` injects `EntityLinks` and never uses it.
- **Placeholder hack:** `RootController` / `DashboardController` publish a fake domain string so the advice wraps a marker model.

The slice constraint behind the current split: `@MvcComponent` postprocessors are discovered by every `@WebMvcTest`, so a required constructor dependency must be mocked centrally in `common/WithPostprocessors`. Feature-flag ports are injected as `Optional<T>`/`ObjectProvider<T>` instead (a mock would activate the feature). Option ports (`RankingOptionsPort`, `EventTypeOptionsPort`) are already in `WithPostprocessors`.

```
current:   controller ──(eager lookup)──▶ setContext / setDomain / embed ──▶ advice ──▶ postprocessor
                  └──(groups) builds EntityModel links/templates by hand

target:    controller ──▶ payload DTO + already-loaded domain ──▶ advice ──▶ postprocessor
                                                                      └──▶ resolves ports / providers itself
```

## Goals / Non-Goals

**Goals:**
- No controller computes anything that is only read by a postprocessor. If no HAL envelope is rendered, the request performs no extra work.
- All HAL `_links` / `_templates` construction happens in `RepresentationModelProcessor` postprocessors, including nested items.
- Exactly one controller→postprocessor side channel (`HalResponseContext`), with deferred resolution.

**Non-Goals:**
- Changing any response shape, link, template, `_embedded` key, status code or authorization outcome.
- `Location` headers: they are HTTP, not HAL, and stay built in the controller (`createEvent`, `resumeMember`, etc.). Note: a few controllers currently use `klabisLinkTo` there, which drops the header when the method itself is unauthorized; normalizing that to plain `linkTo` per ADR-003 is **out of scope** (it would be observable and therefore spec-driven).
- The generated OpenAPI specs, frontend, and `x-hal-embedded` / `x-hal-entity-items` markers — unchanged.
- Replacing `setDomain` / `setDomainList`: registering an aggregate the controller already loaded to build the DTO is not extra work.

## Decisions

**D1. Deferred providers in `HalResponseContext`, not eager values.**
Add supplier-backed entries: `setDeferredContext(Class<T>, Supplier<T>)` and `embedDeferred(Supplier<Collection<?>>, Class<?>)`. `findContext` / `getContext` / the advice resolve the supplier on first read and memoize per request; `clear()` discards unresolved suppliers. This directly satisfies "no dead preparation": if the advice passes the body through, the supplier never runs.
- *Alternative (postprocessors inject the ports directly):* cleaner ownership, but per-row list postprocessors would need request-scoped memoization to preserve today's one-batched-query-per-page behaviour (e.g. `EnrolledEventIds`), and every new non-optional port needs a `WithPostprocessors` entry. Use this only where the port is already slice-safe (see D3).
- *Alternative (parse path/query from the request in the postprocessor):* rejected — the prior `sync-followup-hal-side-channel` change deliberately moved away from `URI_TEMPLATE_VARIABLES_ATTRIBUTE` because a renamed variable silently dropped links. Cheap path-variable passthroughs stay as deferred providers.

**D2. Fold the static-setter channel into `HalResponseContext`.**
`MembershipFeeTierListPostprocessor.setActiveCampaign`, `MembershipFeeTierListRulesPostprocessor.setOptions`, and `FeeSelectionCampaignListPostprocessor.setLevelOptions` stop writing raw attributes; they use the typed context (or, per D3, no context at all). This removes three bespoke mechanisms and their `currentXxx()` readers.

**D3. Move option lookups into the postprocessors that use them.**
`RankingOptionsPort`, `EventTypeOptionsPort` (already mocked in `WithPostprocessors`), `ObjectProvider<EventTypeManagementPort>` (already used by `EventTypeDetailsPostprocessor`), and `FeeSelectionCampaignManagementPort` move into `MembershipFeeTierListRulesPostprocessor`, `MembershipFeeTierListPostprocessor`, `EventTypeListPostprocessor`, and `FeeSelectionCampaignListPostprocessor`. Any newly-required port is registered in `common/WithPostprocessors`; feature-flag ports stay `Optional`/`ObjectProvider`. This deletes the controller-side option building and the wrapper fields that carried it.

**D4. Nested-item postprocessing for groups.**
Guarantee `EntityModel` items nested inside a returned payload are postprocessed. Simplest robust approach: after the existing top-level processing, walk the body's record components / collection elements for `RepresentationModel` values and invoke the `RepresentationModelProcessorInvoker` on each (depth-first), so per-item postprocessors can be registered by item DTO type. Add postprocessors for `OwnerResponse`, `ParentResponse`, `TrainerResponse`, `FreeGroupMembershipResponse`, `FamilyGroupMembershipResponse`, `GroupMembershipResponse`, `PendingInvitationResponse`; delete `InvitationModelBuilder` and the controller `build*Model` helpers. The enclosing `FreeGroupDetailsPostprocessor` / `FamilyGroupDetailsPostprocessor` / `TrainingGroupDetailsPostprocessor` keep the aggregate-level self/collection links.
- *Alternative (make each nested property a `CollectionModel`/`PagedModel` of `EntityModel<Item>`):* Spring HATEOAS would recurse for free, but it changes the serialized shape of the `_links` on those properties and their Java types, so rejected as observably different.

**D5. Nested-item postprocessors receive their owning aggregate via the typed context, not the item DTO.**
Item postprocessors need the owning group (for ownership checks and the group id); publish it once per request as a deferred context value. This is cheap (already loaded) and keeps the per-item processor stateless.

**D6. Replace the Root/Dashboard placeholder with an explicit wrap request.**
`HalResponseContext.setDomain(PLACEHOLDER)` exists only to trip the advice's "wrap when a domain is present" check. Add an explicit no-domain wrap signal (e.g. `HalResponseContext.markHalResource()`), so no fake domain object is needed and postprocessors on `EntityModel<RootModel>` / `EntityModel<DashboardModel>` still run.

**D7. Remove the unused `EntityLinks` from `EventRegistrationController`.**

## Risks / Trade-offs

- **Response parity for nested items** → the highest-risk step. Mitigation: dedicated parity tests asserting exact `_links`/`_templates` on `owners`/`members`/`pendingInvitations`/`parents`/`trainers` before and after; keep the change of one controller at a time.
- **Per-row query regressions** → deferred suppliers must memoize the page-wide lookup once per request. Mitigation: assert one port call per request in the affected slice tests.
- **Slice breakage** → a postprocessor gaining a required port breaks unrelated `@WebMvcTest` slices. Mitigation: use `Optional`/`ObjectProvider` for feature flags; centralise mocks in `WithPostprocessors`; run the full backend test suite after each phase.
- **Recursive nested processing double-invokes processors** → walk only payload containers, never re-invoke on the already-processed top-level model; guard against cycles.
- **Scope creep into observable changes** → if any phase cannot reach parity, stop and convert the change to `spec-driven`.

## Migration Plan

Phases are independently committable and testable; each ends with the full backend suite green and no modified existing assertions.

1. **Side-channel unification (D2).** Fold the three static setters into `HalResponseContext`. No controller change.
2. **Deferred providers (D1, D6).** Add the API + tests; convert `setContext` flags, `embed`, and Root/Dashboard to it. Controllers now register providers instead of computing.
3. **Postprocessor-owned options (D3).** Move option/port lookups into postprocessors; delete wrapper option fields.
4. **Nested group items (D4, D5, D7).** Add nested postprocessing; migrate `FreeGroup` → `FamilyGroup` → `TrainingGroup` one at a time; delete `InvitationModelBuilder`; drop `EntityLinks`.
5. **Cleanup.** Remove `HalResponseContext` members left unused; update `WithPostprocessors`; final full-suite + manual HAL parity check.

Rollback: each phase is a separate commit; reverting one restores the prior side channel without touching the others.

## Open Questions

- Should deferred suppliers be exposed as a public `HalResponseContext` API, or kept package-private to `common/ui` with a narrower `DeferredContext` helper?
- Is `FeeSelectionCampaignManagementPort` acceptable as a required postprocessor dependency (one new `WithPostprocessors` mock), or should the active-campaign lookup stay a deferred context value supplied by the controller?
- Should the nested-model walk live in `HalResponseBodyAdvice` (works for every payload) or in a dedicated `NestedModelPostprocessor` invoked by the advice?
