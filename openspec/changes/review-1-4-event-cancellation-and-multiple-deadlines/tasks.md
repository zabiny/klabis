## 1. Domain model — RegistrationDeadlines value object (Red)

- [ ] 1.1 Create `RegistrationDeadlines` value object record under `com.klabis.events.domain` with `Optional<LocalDate> deadline1, deadline2, deadline3` and validation invariants (sequenciality, monotonie, ≤ event date when used by Event)
- [ ] 1.2 Unit tests for `RegistrationDeadlines`: empty / single / two / three deadlines, missing-prerequisite (d2 without d1), out-of-order, helper methods `last()`, `nextRelevant(today)`, `registrationsOpen(today)`
- [ ] 1.3 Run new unit tests via test-runner — must pass

## 2. Domain model — Event aggregate (Green)

- [ ] 2.1 Replace `Event.registrationDeadline: Optional<LocalDate>` with `Event.registrationDeadlines: RegistrationDeadlines` in the aggregate; update create/update commands and factories accordingly
- [x] 2.2 Add `Event.cancellationReason: Optional<String>`; modify `Event.cancel()` to accept `Optional<String> reason` (max 500 chars validation in command record)
- [ ] 2.3 Update internal usages of the deadline field (`Event.canRegister`, `Event.canEdit`, `EventFilter` if it filters on deadline) to use the new value object
- [x] 2.4 Domain unit tests cover: cancel with/without reason, cancel reason length limit (IllegalArgumentException at 501 chars, accepts 500)
- [x] 2.5 Run domain tests via test-runner — passed (218/218)

## 3. Persistence layer

- [x] 3.1 Update `V001__initial_schema.sql`: add `cancellation_reason VARCHAR(500) NULL` to events table. (deadline columns deferred to Iter 2)
- [x] 3.2 Update `EventMemento` to map cancellation_reason column; reconstruct reason in `toEvent()`
- [x] 3.3 Persistence integration tests (H2): persist cancellation reason with/without reason, reload, assert equality
- [x] 3.4 Run persistence tests via test-runner — passed (31/31)

## 4. ORIS import

- [ ] 4.1 Extend the ORIS-to-Event mapping to read `EntryDate2` and `EntryDate3` from the upstream ORIS payload and pass them into `RegistrationDeadlines`
- [ ] 4.2 If ORIS feed contains out-of-order deadlines, importer fails loudly (logs the violation, returns 422 to the manager) — no silent fallback
- [ ] 4.3 Integration test (mocked ORIS client): event with EntryDate1 only → 1 deadline; event with EntryDate1+2+3 → 3 deadlines; event with EntryDate1+3 (no 2) → import error
- [ ] 4.4 Run ORIS-related tests via test-runner

## 5. REST API + HAL forms

- [ ] 5.1 Update create/update Event request DTOs to accept `deadlines: List<LocalDate>` s `@Size(min = 1, max = 3)` a sekvenční validací (vlastní validator nebo `@AssertTrue` metoda — sekvenčně rostoucí). Mapping v controlleru: List ↔ doménový `RegistrationDeadlines` VO. Cancel DTO: `cancellationReason: String` s `@Size(max = 500)`.
- [x] 5.2 Update cancel Event request DTO to accept optional `cancellationReason` (`CancelEventRequest` record with `@Size(max=500)`)
- [x] 5.3 Update response DTO to expose `cancellationReason` on `EventDto` and `EventSummaryDto` (deadlines deferred to Iter 3)
- [ ] 5.4 HAL-Forms metadata se generují automaticky ze Spring HATEOAS na základě JSR-303 anotací z task 5.1 (`@Size(min, max)` → `min`/`max` v HAL-Forms property, `List<LocalDate>` → `multiple: true` + `type: date`). Ověřit ve vygenerovaném výstupu (controller integration test), že `deadlines` má `multiple: true`, `min: 1`, `max: 3`, `type: date` a že `cancellationReason` má `maxLength: 500`. Případně doplnit `@PropertyMetadata` pokud jsou potřeba prompty/labels nad rámec automatiky.
- [ ] 5.5 Controller integration tests for new fields. Žádná BC pro legacy `registrationDeadline` název — frontend je jediný klient a upraví se v rámci tohoto change.

## 6. Frontend — event form + cancel dialog + detail view

- [ ] 6.1 Použít existující `HalFormsCollectionField` (renderuje se automaticky když property má `multiple: true` a žádné options/suggest, respektuje `min`/`max`, `frontend/src/components/HalNavigator2/halforms/fields/HalFormsCollectionField.tsx:21`). Form pro create/update event nesmí mít hardcoded deadline1/2/3 — musí čistě vyrenderovat to, co backend pošle v template (date items přes existující case `'date'` v `halFormsFieldsFactory`). Sekvenciální validace dat (rostoucí pořadí) client-side přes Yup nebo backend chyby.
- [ ] 6.2 Update cancel dialog to include an optional textarea "Důvod zrušení" (max 500 chars, char counter)
- [ ] 6.3 Update event detail page: add "Uzávěrky přihlášek" section listing all set deadlines chronologically, highlighting the currently relevant one; for cancelled events, show a "Akce byla zrušena" block with the reason if set
- [ ] 6.4 Update events table column "Uzávěrka": show the relevant deadline; if more deadlines exist, render a small badge/icon with tooltip listing the others; for cancelled rows with reason, surface the reason as a tooltip on the status indicator
- [ ] 6.5 Update `src/localization/labels.ts` with new labels: `deadlines`, `cancellationReason`, případně tlačítka „Přidat uzávěrku" / „Odebrat".
- [ ] 6.6 Frontend tests: sekvenciální validace deadlines, deadline rendering v detail view a tabulce, cancel dialog s reason. (Array renderer `HalFormsCollectionField` má vlastní existující testy — netestovat znovu.)

## 7. End-to-end verification

- [ ] 7.1 Spustit lokální prostředí (`./runLocalEnvironment.sh`) — backend na `https://localhost:8443`, frontend na `http://localhost:3000`. E2E ověření probíhá výhradně lokálně, žádný deploy.
- [ ] 7.2 Browser test (`http://localhost:3000`): create a new (manual) event with all three deadlines; verify table column behaviour, detail page listing, and registration open/closed semantics across deadlines
- [ ] 7.3 Browser test: import an ORIS event s více entry dates (vybrat reálné ORIS event id s EntryDate2/EntryDate3); verify all deadlines imported correctly
- [ ] 7.4 Browser test: cancel an event with reason "Zrušeno kvůli počasí"; verify reason appears on event detail and as tooltip on row in events table
- [ ] 7.5 Browser test: cancel an event without reason; verify no reason text shown but cancellation works

## 8. Documentation

- [ ] 8.1 Sync the spec change into `openspec/specs/events/spec.md` after archiving
