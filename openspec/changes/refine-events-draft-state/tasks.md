## 1. Backend: Hide Registrations Link for DRAFT Events

- [x] 1.1 Write failing test: event detail API does not return registrations HAL link for DRAFT event
- [x] 1.2 Condition the registrations HAL link on event status (omit for DRAFT)
- [x] 1.3 Write test: event detail API returns registrations HAL link for ACTIVE event
- [x] 1.4 Write test: event detail API returns registrations HAL link for FINISHED event

## 2. Frontend: Hide Registrations Section for DRAFT Events

- [x] 2.1 Verify frontend hides registrations section when HAL link for registrations is absent (HAL-driven — verify existing behavior covers this, add test if needed)
- [x] 2.2 Write test: registrations section is not rendered on event detail page when event is in DRAFT status

## 3. Frontend: Category Preset Picker in Event Form

- [x] 3.1 Fetch available Category Presets when event create/edit form is opened (via HAL link)
- [x] 3.2 Render "Select from templates" button next to categories field only when presets are available
- [x] 3.3 Implement preset picker dialog/dropdown showing available presets by name
- [x] 3.4 On preset selection, populate the categories field with the preset's categories
- [x] 3.5 Allow manual editing of categories after preset is applied
- [x] 3.6 Write tests for preset picker: button shown when presets exist, hidden when none exist, categories populated on selection
