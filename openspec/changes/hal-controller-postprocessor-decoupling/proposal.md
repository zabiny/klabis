## Why

HAL hypermedia is produced in two places today: the `RepresentationModelProcessor` postprocessors, and the MVC controllers that feed them. Controllers build links/affordances by hand (the three group controllers), eagerly compute data (sync-enrolment sets, ORIS club-key state, embedded collections, HAL-FORMS inline options) that only a postprocessor reads, and use three different request-attribute side channels to hand it over. None of that work is needed when no HAL envelope is rendered, and it leaves "which link/template appears" split across controller and postprocessor, so the rules drift and slices must be mocked piecemeal.

## What Changes

- **One side channel:** fold the three ad-hoc request-attribute mechanisms (`MembershipFeeTierListPostprocessor.setActiveCampaign`, `MembershipFeeTierListRulesPostprocessor.setOptions`, `FeeSelectionCampaignListPostprocessor.setLevelOptions`) into `HalResponseContext`.
- **No eager postprocessor-only work:** add deferred (supplier-backed) entries to `HalResponseContext` so controllers register *how to obtain* a value instead of computing it; postprocessors resolve it on demand. Convert the sync-enrolment hints (`EnrolledEventIds`, `EnrolledDisciplineIds`, `EnrolledMemberIds`), the ORIS `ClubKeyHeld` hint, and the `embed(...)` collections (`EventController.getEvent`, `MembershipFeeGroupController.getFeeGroup`) to deferred providers.
- **Postprocessors own what they can:** inject the already-slice-safe option/ports (`RankingOptionsPort`, `EventTypeOptionsPort`, `ObjectProvider<EventTypeManagementPort>`, `FeeSelectionCampaignManagementPort`) into the postprocessors that build templates, and delete the controller-side option prep and the wrapper fields that only carried it (`FeeSummaryView.groupOptions`, option params on `setOptions` / `setLevelOptions` / `setActiveCampaign`).
- **All HAL construction in postprocessors:** add nested-item postprocessing so `EntityModel` items nested in group payload records go through processors, then add item postprocessors for `OwnerResponse`, `ParentResponse`, `TrainerResponse`, `FreeGroupMembershipResponse`, `FamilyGroupMembershipResponse`, `GroupMembershipResponse`, `PendingInvitationResponse`; delete `InvitationModelBuilder` and the controller-private `build*Model` / `to*Response` helpers that build links.
- **Cleanup:** drop the unused `EntityLinks` dependency in `EventRegistrationController`; replace the Root/Dashboard "placeholder domain" hack with an explicit marker-wrap path; remove `HalResponseContext` API left unused by the above.

## No Behavior Change Justification

Every response body — links, `_templates`, `_embedded`, status codes and payloads — must be byte-for-byte equivalent after this change. The refactor only moves *where* that content is assembled; it changes no rule about *when* a link/template is present.

**Specs reviewed:**
- `openspec/specs/non-functional-requirements/spec.md` — HAL+FORMS media type, HATEOAS link structure and conditional edit templates unchanged; the same links/templates are still produced, only by a different class.
- `openspec/specs/members/spec.md`, `events/spec.md`, `event-registrations/spec.md`, `disciplines/spec.md`, `event-types/spec.md`, `category-presets/spec.md` — link/template/embedded scenarios unchanged.
- `openspec/specs/membership-fees/spec.md`, `member-accounts/spec.md` — the "Active Campaign Shown Inline" / "Past Campaigns" and account/transaction link scenarios unchanged; the postprocessor still receives the same options and campaign.
- `openspec/specs/user-groups/spec.md` — free/family/training group detail and invitation affordances unchanged; nested item links move from controller to postprocessor with identical output.
- `openspec/specs/calendar-items/spec.md`, `application-navigation/spec.md`, `dashboard/spec.md`, `data-synchronization/spec.md` — navigation links, calendar navigation and sync-state links unchanged.

**Why no spec update is needed:** the change touches no requirement statement, business rule, authorization decision or response shape. It relocates HAL link/template assembly and removes eager computation whose results are unobservable unless a HAL envelope is rendered.

## Impact

- **Modules:** `common/ui` (`HalResponseContext`, `HalResponseBodyAdvice`), `members`, `events`, `membershipfees`, `finance`, `groups/freegroup`, `groups/familygroup`, `groups/traininggroup`, `calendar`, `sync`, `oris`.
- **Code:** the ~14 controllers and ~25 postprocessors listed in `design.md`; removal of `InvitationModelBuilder`.
- **Tests:** existing `@WebMvcTest`/E2E assertions must pass unchanged; `common/WithPostprocessors` may need new mock entries for ports the postprocessors take over.
- **No build/tooling, spec, API-contract or frontend changes.**
