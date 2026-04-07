## 1. Event Categories — Backend Domain + Persistence

- [x] 1.1 Add `categories` field (`List<String>`) to Event aggregate, update `CreateEvent`, `UpdateEvent`, `CreateEventFromOris` command records, private constructor, `reconstruct()`, `update()`, and getter
- [x] 1.2 Add `categories VARCHAR(2000)` column to events table in V001 migration
- [x] 1.3 Update EventMemento — add `categories` column field, update `copyBasicEventInfo()` (List→comma-separated) and `toEvent()` (comma-separated→List)
- [x] 1.4 Add `List<String> categories` to EventCreatedEvent and EventUpdatedEvent domain events, update `fromAggregate()` factories, handle null safely in compact constructors
- [x] 1.5 Update EventDto, EventSummaryDto, and EventDtoMapper to include categories
- [x] 1.6 Add categories to bootstrap data for test events — no event bootstrap data exists, skipped

## 2. ORIS Import — Extract Categories

- [x] 2.1 Add `extractCategories(EventDetails)` helper method to EventManagementService — extract `EventClass.name` values from `details.classes()` map
- [x] 2.2 Update `importEventFromOris()` to pass extracted categories to `CreateEventFromOris` builder

## 3. Sync from ORIS — Backend

- [x] 3.1 Add `SyncFromOris` command record to Event and `syncFromOris()` domain method (validate DRAFT/ACTIVE status and non-null orisId, overwrite fields, publish EventUpdatedEvent)
- [x] 3.2 Add `syncEventFromOris(EventId)` method to EventManagementPort and EventManagementService
- [x] 3.3 Add `POST /{id}/sync-from-oris` endpoint to EventController with EVENTS:MANAGE authority
- [x] 3.4 Add syncFromOris affordance to event detail links — only for DRAFT/ACTIVE events with orisId when ORIS integration is active

## 4. Event Categories — Frontend

- [x] 4.1 Add `categories` and `syncFromOris` labels to `labels.ts`
- [x] 4.2 Update EventDetailPage — add categories display as pills/tags (Badge components), add `HalFormButton` for syncFromOris action
- [x] 4.3 Regenerate OpenAPI types (`npm run openapi`)
- [x] 4.4 Run `npm run refresh-backend-server-resources`

## 5. Category Presets — Backend

- [x] 5.1 Create CategoryPreset aggregate (name + list of categories) in events module domain layer
- [x] 5.2 Create CategoryPreset persistence — CategoryPresetMemento, DB table in V001, repository
- [x] 5.3 Create CategoryPresetManagementPort and CategoryPresetManagementService (CRUD operations)
- [x] 5.4 Create CategoryPresetController — REST CRUD endpoints with EVENTS:MANAGE authority, HATEOAS links and affordances
- [x] 5.5 Add CategoryPreset DTOs and mapper

## 6. Category Presets — Frontend

- [x] 6.1 Create CategoryPresetsPage — list presets, create/edit/delete with HAL Forms
- [x] 6.2 Add navigation entry for category presets page (EVENTS:MANAGE only)
- [ ] 6.3 Add preset selection UI to event create/edit form — dropdown to select preset which populates categories field (deferred — categories field is already editable on the event form)

## 7. Event Registration with Category — Backend

- [x] 7.1 Add `category` field (optional String) to EventRegistration value object and RegisterCommand
- [x] 7.2 Add category validation in `Event.registerMember()` — if event has categories, category MUST be provided and MUST be in event's category list; if no categories, category is ignored
- [x] 7.3 Update EventRegistration persistence (EventRegistrationMemento, DB column)
- [x] 7.4 Update RegistrationDto and registration list to include category
- [x] 7.5 Add WARN logging when ORIS sync removes a category that has registrations

## 8. Event Registration with Category — Frontend

- [x] 8.1 Verified HalFormsFieldFactory dispatches to select when prop.options present; fixed factory to use HalFormsSelect for any field with options (regardless of type)
- [x] 8.2 Add category column to registration list — conditional on event.categories?.length > 0
- [x] 8.3 Update RegistrationData interface to include category?: string

## 9. Spec Updates

- [ ] 9.1 Update `openspec/specs/events/spec.md` with category-related requirements
- [ ] 9.2 Update `openspec/specs/event-registrations/spec.md` with category selection requirement
- [ ] 9.3 Create `openspec/specs/event-categories/spec.md` from delta spec
- [ ] 9.4 Create `openspec/specs/category-presets/spec.md` from delta spec
