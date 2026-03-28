## Backend - DB & Domain

- [x] Add `oris_id INTEGER NULL` column with a `UNIQUE` constraint to the `events` table in `V001__initial_schema.sql`
- [x] Add private nullable `Integer orisId` field to `Event` aggregate with a package-private getter `getOrisId()`
- [x] Add factory method `Event.createFromOris(int orisId, String name, LocalDate eventDate, String location, String organizer, WebsiteUrl websiteUrl)` that sets `orisId` and starts the event in `DRAFT` status; registers `EventCreatedEvent`
- [x] Add nested command record `Event.ImportCommand(int orisId)` used as input type for the import endpoint
- [x] Add unit test verifying that `Event.createFromOris(...)` produces an event in `DRAFT` status with the correct field values and a non-null `orisId`
- [x] Add `DuplicateOrisImportException` (HTTP 409) in package `com.klabis.events.application`

## Backend - Repository

- [x] Add method `boolean existsByOrisId(int orisId)` to `EventRepository` interface in `com.klabis.events.domain`
- [x] Add `@Column("oris_id") Integer orisId` field to `EventMemento`; propagate the value in `EventMemento.from(Event)` (read from `event.getOrisId()`) and in `EventMemento.toEvent()` (pass to `Event.reconstruct(...)`)
- [x] Extend `Event.reconstruct(...)` signature to accept the nullable `orisId` parameter
- [x] Add `existsByOrisId(int orisId)` query method to `EventJdbcRepository` (Spring Data derived query or `@Query`)
- [x] Implement `existsByOrisId(int orisId)` in `EventRepositoryAdapter` delegating to `EventJdbcRepository`
- [x] Add `@DataJdbcTest` repository test verifying that `existsByOrisId` returns `true` for a saved event with the given `orisId` and `false` otherwise, and that the unique constraint rejects a duplicate `orisId`

## Backend - Application Service

- [x] Add method `Event importEventFromOris(int orisId)` to `EventManagementService` interface
- [x] Implement `importEventFromOris(int orisId)` in `EventManagementServiceImpl`:
  1. Call `eventRepository.existsByOrisId(orisId)` → throw `DuplicateOrisImportException` (409) if `true`
  2. Call `orisApiClient.getEventDetails(orisId)` → throw `EventNotFoundException` (404) if ORIS returns no data
  3. Map `EventDetails` fields: `name`, `date` → `eventDate`, `place` → `location`, `org1().abbr()` / `org2().abbr()` with `"---"` fallback → `organizer`, `"https://oris.ceskyorientak.cz/Zavod?id={orisId}"` → `websiteUrl`
  4. Create event via `Event.createFromOris(...)` and save via `eventRepository.save(...)`
- [x] Inject `OrisApiClient` into `EventManagementServiceImpl` (required dependency — the service is only wired when ORIS profile is active, but `Optional<OrisApiClient>` is acceptable if needed)
- [x] Add unit test for `EventManagementServiceImpl.importEventFromOris` covering: successful import, duplicate rejection (409), ORIS event not found (404)

## Backend - REST API

- [x] Add `POST /api/events/import` endpoint to `EventController`:
  - Method name: `importEvent(@RequestBody Event.ImportCommand command)`
  - Requires `@HasAuthority(Authority.EVENTS_MANAGE)`
  - Returns `201 Created` with `Location` header pointing to the new event (`/api/events/{id}`)
- [x] Inject `Optional<OrisApiClient>` into `EventController` constructor (used only to decide whether to expose the import affordance)
- [x] Add `importFromOris` affordance to the self link of the `listEvents` response — affordance is added only when `orisApiClient.isPresent()` is `true`:
  ```java
  if (orisApiClient.isPresent()) {
      selfLink = selfLink.andAffordances(klabisAfford(methodOn(EventController.class).importEvent(null)));
  }
  ```
- [x] Register `DuplicateOrisImportException` in the global exception handler (or via `@ExceptionHandler`) to return HTTP 409
- [x] Add `@WebMvcTest` controller test for `importEvent` covering: 201 with Location header on success, 403 without `EVENTS:MANAGE`, 409 on duplicate, 404 when ORIS event not found
- [x] Add `@WebMvcTest` controller test verifying that `importFromOris` affordance appears in `listEvents` response when `OrisApiClient` bean is present and is absent when it is not present
- [x] Create `OrisController` in `com.klabis.oris` module (annotated `@OrisIntegrationComponent`, `@RestController`, `@PrimaryAdapter`):
  - `GET /api/oris/events` with `@HasAuthority(Authority.EVENTS_MANAGE)`
  - Calls `orisApiClient.getEventList(OrisEventListFilter.EMPTY.withDateFrom(LocalDate.now()).withDateTo(LocalDate.now().plusYears(1)))`
  - Returns `List<OrisEventSummary>` where `OrisEventSummary` is a record `(int id, String name, LocalDate date)`
- [x] Add `@WebMvcTest` test for `OrisController` verifying 200 with correct payload and 403 without `EVENTS:MANAGE`

## Frontend

- [ ] Add `"Importovat z ORIS"` button to `EventsPage.tsx` — rendered conditionally based on the presence of `_templates.importFromOris` in the HAL response; clicking opens `ImportOrisEventModal`
- [ ] Add localization keys to `labels.ts`: `templates.importFromOris` (button label), `dialogs.importFromOris` (modal title), error messages for 409 conflict and generic error
- [ ] Create `src/components/events/ImportOrisEventModal.tsx` component with props: `isOpen: boolean`, `onClose: () => void`, `importHref: string` (URL from `_templates.importFromOris.target`)
- [ ] Implement fetch of `GET /api/oris/events` inside `ImportOrisEventModal` on open — show loading indicator while fetching
- [ ] Render select box with options formatted as `"{date} — {name}"` after successful fetch; no option pre-selected; submit button disabled when no option is selected
- [ ] Show `"Žádné závody k importu"` message and disable submit button when `GET /api/oris/events` returns an empty list
- [ ] Show error message and disable submit button when `GET /api/oris/events` fails
- [ ] Implement submit: `POST importHref` with `{ orisId: selectedId }`, show loading state on submit button
- [ ] On 201 response, close modal and redirect to event detail URL from the `Location` response header
- [ ] On 409 response, keep modal open and display `"Tento závod již byl importován"` error message
- [ ] On any other error response, keep modal open and display the generic error message
- [ ] Handle "Zrušit" / close button — closes modal without sending any request
