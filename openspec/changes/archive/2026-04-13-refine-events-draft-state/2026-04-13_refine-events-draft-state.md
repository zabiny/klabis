# Team Coordination File — refine-events-draft-state

**Date:** 2026-04-13  
**Proposal:** refine-events-draft-state  
**Status:** In Progress

## Summary of Work

### Proposal Overview
Two changes:
1. **Backend + Frontend:** Hide registrations section/link for DRAFT events — achieved by conditioning HAL link on event status (backend), frontend is HAL-driven so it hides automatically.
2. **Frontend:** Category Preset picker in event create/edit form — "Select from templates" button populated categories from presets.

### Tasks from tasks.md

#### Group 1: Backend — Hide Registrations Link for DRAFT Events
- [ ] 1.1 Write failing test: event detail API does not return registrations HAL link for DRAFT event
- [ ] 1.2 Condition the registrations HAL link on event status (omit for DRAFT)
- [ ] 1.3 Write test: event detail API returns registrations HAL link for ACTIVE event
- [ ] 1.4 Write test: event detail API returns registrations HAL link for FINISHED event

#### Group 2: Frontend — Hide Registrations Section for DRAFT Events
- [ ] 2.1 Verify frontend hides registrations section when HAL link for registrations is absent
- [ ] 2.2 Write test: registrations section is not rendered on event detail page when event is in DRAFT status

#### Group 3: Frontend — Category Preset Picker in Event Form
- [ ] 3.1 Fetch available Category Presets when event create/edit form is opened (via HAL link)
- [ ] 3.2 Render "Select from templates" button next to categories field only when presets are available
- [ ] 3.3 Implement preset picker dialog/dropdown showing available presets by name
- [ ] 3.4 On preset selection, populate the categories field with the preset's categories
- [ ] 3.5 Allow manual editing of categories after preset is applied
- [ ] 3.6 Write tests for preset picker: button shown when presets exist, hidden when none exist, categories populated on selection

## Iteration Plan

- **Iteration 1:** Backend — Condition HAL link on event status + tests (Tasks 1.1–1.4)
- **Iteration 2:** Frontend — Verify/add tests for hidden registrations section in DRAFT (Tasks 2.1–2.2)
- **Iteration 3:** Frontend — Category Preset picker implementation + tests (Tasks 3.1–3.6)

## Progress Log

### Iteration 1 — Backend: Condition registrations HAL link on event status (DONE)

- Modified `EventController.addLinksForEvent()`: wrapped registrations link in `if (event.getStatus() != EventStatus.DRAFT)` guard
- Updated existing test `shouldNotIncludeRegisterAffordanceForDraftEvent` → renamed to `shouldNotIncludeRegistrationsLinkForDraftEvent`, changed assertion from `exists()` to `doesNotExist()` for `_links.registrations`
- Tasks 1.3 and 1.4 were already covered by existing tests (`shouldIncludeRegisterAffordanceForActiveEvent` checks ACTIVE link exists; `shouldNotIncludeRegisterAffordanceForFinishedEvent` checks FINISHED link exists)
- All 62 EventControllerTest tests pass

### Iteration 2 — Frontend: Hide registrations section for DRAFT events (DONE)

- Frontend was NOT HAL-link driven for the registrations section — it rendered unconditionally
- Modified `EventDetailPage.tsx`: wrapped registrations section in `{resourceData._links?.registrations && (...)}` guard
- Updated existing registrations section tests to include `registrations` link in mock data (required now)
- Added new test: "hides registrations section when registrations link is absent (DRAFT event)"
- All 31 EventDetailPage frontend tests pass

### Iteration 3 — Frontend: Category Preset Picker in Event Form (DONE)

- `CategoryPresetPickerButton.tsx` and `eventFormFieldsFactory.tsx` were already implemented
- `EventsPage.tsx` already wired `eventFormFieldsFactory` for `createEvent` and modal `updateEvent`
- Fixed gap: added `fieldsFactory={eventFormFieldsFactory}` to `HalFormDisplay` in `EventDetailPage.tsx` inline edit block
- Added import of `eventFormFieldsFactory` in `EventDetailPage.tsx`
- Created `CategoryPresetPickerButton.test.tsx` with 8 tests covering: button shown/hidden based on presets, dialog opens with preset names, dialog closes after selection, categories field populated on selection
- All 39 tests pass (8 new + 31 existing EventDetailPage)
