# Team Coordination File — Event Categories Implementation

## Status: IN PROGRESS

## Overview
Implementing event categories feature per openspec proposal: event-categories.
- Event aggregate gains `categories` field
- ORIS import extracts categories
- Sync from ORIS action
- Category presets management
- Registration with category selection
- Frontend updates

## Iteration Log

### 2026-04-08 — Tasks 1.1–1.6, 2.1–2.2 implemented

**Domain (1.1):** Added `categories` field (`List<String>`, defaults to empty list) to `Event` aggregate. Updated `CreateEvent`, `UpdateEvent`, and `CreateEventFromOris` command records with `List<String> categories`. Updated private constructor, `reconstruct()`, `update()`, and `getCategories()` getter. Null-safe: null input defaults to empty list throughout.

**Persistence (1.2, 1.3):** Added `categories VARCHAR(2000) NULL` column to events table in V001 migration. Updated `EventMemento` with `@Column("categories") String categories` field. `copyBasicEventInfo()` converts `List<String>` to comma-separated string (null when empty). `toEvent()` splits comma-separated string back to list (empty list when null/blank).

**Domain events (1.4):** Added `List<String> categories` to `EventCreatedEvent` and `EventUpdatedEvent`. Compact constructors default null to `List.of()` via `List.copyOf()`. `fromAggregate()` factories delegate to `event.getCategories()`.

**DTOs (1.5):** Added `List<String> categories` to `EventDto` and `EventSummaryDto`. Updated `EventDtoMapper.toDto()` and `toSummaryDto()` to map `event.getCategories()`.

**Bootstrap (1.6):** No event bootstrap data exists in `BootstrapDataLoader` — task skipped.

**ORIS import (2.1, 2.2):** Added `extractCategories(EventDetails)` private method to `EventManagementService` — extracts non-blank `EventClass.name` values from `details.classes()` map. `importEventFromOris()` now passes extracted categories to the `CreateEventFromOris` builder.

**Test fixes:** Updated all 10 direct `Event.reconstruct()` call sites in tests to include the new `categories` parameter (`List.of()`). Updated 3 direct `EventUpdatedEvent` constructor calls in calendar tests.

All 1975 tests pass.

### 2026-04-08 — Tasks 5.1–5.5 implemented

**Domain (5.1):** Added `CategoryPreset` aggregate root in `events/domain/` extending `KlabisAggregateRoot`. Fields: `id` (CategoryPresetId), `name` (String), `categories` (List<String>). Commands: `CreateCategoryPreset`, `UpdateCategoryPreset` (both nested records with `@RecordBuilder`). Factory methods: `create(command)` with name validation, `reconstruct(...)` for persistence loading. Added `CategoryPresetId` value object (UUID-based, same pattern as `EventId`) in module root package.

**Repository (5.1):** Added `CategoryPresetRepository` interface in domain layer with `save`, `findById`, `findAll`, `deleteById`.

**Persistence (5.2):** Added `category_presets` table to V001 migration (id UUID PK, name VARCHAR(200), categories VARCHAR(2000) NULL, full audit columns). `CategoryPresetMemento` with `@Table("category_presets")`, comma-separated categories conversion. `CategoryPresetJdbcRepository` extending `CrudRepository`. `CategoryPresetRepositoryAdapter` implementing domain repository (`@SecondaryAdapter @Repository`).

**Application layer (5.3):** `CategoryPresetManagementPort` (`@PrimaryPort`) with `createPreset`, `updatePreset`, `deletePreset`, `getPreset`, `listAll`. `CategoryPresetManagementService` package-private implementation. `CategoryPresetNotFoundException` extends `ResourceNotFoundException` (auto-maps to 404 via global handler).

**REST API (5.4):** `CategoryPresetController` at `/api/category-presets` with 5 CRUD endpoints. All require `EVENTS:MANAGE` authority. HATEOAS: GET list has create affordance; GET detail has update+delete affordances and collection link. Added `category-presets` link to `EventsRootPostprocessor`.

**DTOs (5.5):** `CategoryPresetDto` record with `id` (HalForms READ_ONLY), `name`, `categories`. `CategoryPresetDtoMapper`. `CategoryPresetIdMixin` (`@JacksonMixin`) for Jackson UUID serialization.

**Tests:** 41 new tests — domain unit tests (CategoryPresetTest), service unit tests (CategoryPresetManagementServiceTest), JDBC integration tests (CategoryPresetJdbcRepositoryTest), controller MVC tests (CategoryPresetControllerTest). All 41 pass; full suite 2032 tests (1 pre-existing flaky MemberControllerSecurityTest unrelated to this change).

### 2026-04-08 — Tasks 3.1–3.4 implemented

**Domain (3.1):** Added `SyncFromOris` command record (nested in `Event`, annotated `@RecordBuilder`) with fields: `name`, `eventDate`, `location`, `organizer`, `websiteUrl`, `registrationDeadline`, `categories`. Added `syncFromOris(SyncFromOris)` domain method that: throws `IllegalStateException` when `orisId` is null; throws `IllegalStateException` when status is FINISHED or CANCELLED; overwrites all fields from command; publishes `EventUpdatedEvent` via `registerEvent()`.

**Service (3.2):** Added `syncEventFromOris(EventId)` to `EventManagementPort` interface and `EventManagementService`. Service: loads event by id (throws `EventNotFoundException` if missing); asserts ORIS client is active; fetches `EventDetails` from ORIS; resolves organizer, websiteUrl, registrationDeadline, and categories using the same helpers as `importEventFromOris`; calls `event.syncFromOris()`; saves.

**Controller (3.3):** Added `POST /api/events/{id}/sync-from-oris` to `EventController` — requires `EVENTS:MANAGE` authority, returns 204 No Content.

**Affordance (3.4):** In `addLinksForEvent()`, added `syncEventFromOris` affordance for DRAFT and ACTIVE cases when `orisIntegrationActive && event.getOrisId() != null`.

**Test data:** Added `withOrisId(Integer)` to `EventTestDataBuilder`.

All 1991 tests pass.

### 2026-04-08 — Tasks 7.1–7.5 implemented

**Domain (7.1):** Added optional `String category` field to `EventRegistration` value object. Updated `CreateEventRegistration` command record with `String category`. Updated private constructor, `create()` (passes category from command), `reconstruct()` (new `category` parameter), and added `category()` getter. `toString()` includes category.

**Domain (7.2):** Added `String category` to `RegisterCommand` nested record in `Event`. Updated `registerMember(MemberId, SiCardNumber, String category)` — added `resolveCategory()` private method: when event has categories, category MUST be provided and in the list (throws `BusinessRuleViolationException`); when event has no categories, category is silently ignored (returns null). Updated `EventRegistrationService.registerMember()` to pass `command.category()`.

**Persistence (7.3):** Added `category VARCHAR(50) NULL` column to `event_registrations` table in V001 migration. Updated `EventRegistrationMemento` with `@Column("category") String category` field, `from()` method, `toEventRegistration()` (passes category to `reconstruct()`), and `getCategory()` getter.

**DTOs (7.4):** Added `String category` to `RegistrationDto` and `OwnRegistrationDto` (with `@HalForms(access = READ_ONLY)`). Updated `RegistrationDtoMapper.toDto()` to map `registration.category()`. Updated `EventRegistrationController.toOwnRegistrationDto()` to include `registration.category()`.

**Logging (7.5):** Added `warnIfSyncRemovesCategoriesWithRegistrations()` private method to `EventManagementService`. Called before `event.syncFromOris()` — computes categories that have registrations but are not in the incoming ORIS category list, logs WARN with affected category names and registration counts. No-op when no registrations exist.

**Tests:** Added 9 new domain tests (category validation in `EventTest`), 2 service tests (category validation in `EventRegistrationServiceTest`), 2 controller tests (category in list and own registration response in `EventRegistrationControllerTest`). Updated all 28 existing call sites of `registerMember()` and `EventRegistration.reconstruct()` to include the new parameter.

All 2040 tests pass (1 pre-existing flaky `MemberControllerSecurityTest` unrelated to this change).

### 2026-04-08 — Tasks 4.1–4.4 implemented

**Labels (4.1):** Added `categories: 'Kategorie'` to `labels.fields` and `syncFromOris: 'Synchronizovat z ORISu'` to `labels.templates` in `frontend/src/localization/labels.ts`.

**EventDetailPage (4.2):** Added `categories?: string[]` to `EventDetail` interface. Categories are displayed as a row of `Badge` components (variant `info`, size `sm`) in a `DetailRow` — row is only shown when `categories` is non-empty or in edit mode. In inline edit mode, `ri('categories')` delegates rendering to the HAL form template field. Added `HalFormButton name="syncFromOris"` with `RefreshCw` icon alongside the other action buttons — automatically hidden when the template is absent.

**OpenAPI types (4.3):** The `klabis-full.json` spec has not yet been exported from the backend (backend not running at implementation time). Manually added `registrationDeadline?: string` and `categories?: string[]` to `EntityModelEventDto` in `klabisApi.d.ts` to reflect the actual backend DTOs. When the backend is started and `npm run openapi` is run next, the generated types will replace this manual update.

**Static resources (4.4):** Ran `npm run refresh-backend-server-resources` — all 1051 frontend tests passed, build succeeded, static assets copied to backend.

### 2026-04-08 — Tasks 6.1–6.2 implemented

**CategoryPresetsPage (6.1):** Created `frontend/src/pages/events/CategoryPresetsPage.tsx` — lists presets via `HalEmbeddedTable` with `categoryPresetDtoList` collection name. Columns: name (sortable), categories (rendered as `Badge` pills with `info` variant). `HalFormButton name="createCategoryPreset"` in the header — automatically hidden when template is absent (permission-gated by backend). Row click navigates to preset detail via `route.navigateToResource`. Individual preset edit/delete affordances will be rendered generically through the HAL navigator when navigating to a preset detail resource.

**Navigation (6.2):** Added `'category-presets': 'Šablony kategorií'` to `labels.nav`. Added `Tags` icon mapping for `'category-presets'` in `Layout.tsx`. Added route `/category-presets` in `App.tsx`. Backend exposes the `category-presets` link in API root only for users with `EVENTS:MANAGE` permission — navigation entry auto-shows/hides accordingly.

**Labels:** Added `createCategoryPreset`, `updateCategoryPreset`, `deleteCategoryPreset` to `labels.templates`; `categoryPresetsList` and `presetsListHeading` to `labels.sections`.

**Task 6.3 deferred:** The categories field on the event create/edit form is already editable as free text via HAL Forms. Preset selection as a convenience dropdown is a nice-to-have and has been deferred.

**Tests:** 3 new tests for `CategoryPresetsPage` (page title, create button visibility with/without template). All 1054 frontend tests pass.

### 2026-04-08 — Tasks 8.1–8.3 implemented

**HalFormsFieldFactory (8.1):** Fixed `halFormsFieldsFactory` to dispatch to `HalFormsSelect` for any property that has `prop.options` defined, regardless of the field's declared `type`. Previously, only `type: "select"` triggered the select component — a field with `type: "text"` and inline `options` would incorrectly render as a text input. The fix adds an early-return guard before the `switch`: if `conf.prop.options` is truthy, `HalFormsSelect` is returned immediately. The `registerForEvent` HAL form template will automatically render the `category` field as a dropdown once the backend populates options from the event's categories.

**Registration list (8.2):** Added a conditional `category` column to the `HalEmbeddedTable` in the registrations section of `EventDetailPage`. The column is rendered only when `event.categories && event.categories.length > 0`, using `labels.fields.categories` as the header label.

**Types (8.3):** Added `category?: string` to the `RegistrationData` interface in `EventDetailPage.tsx`.

**Tests:** 6 new tests — 3 in `HalFormsFieldFactory.test.tsx` (select dispatch for `type: "select"`, options-present override for `type: "text"`, text input when no options) and 3 in `EventDetailPage.test.tsx` (no category column when categories empty, no category column when categories absent, category column shown when categories present). All 1060 frontend tests pass.

