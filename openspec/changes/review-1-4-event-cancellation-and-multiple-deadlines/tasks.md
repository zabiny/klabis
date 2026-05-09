## 1. Domain model — RegistrationDeadlines value object (Red)

- [ ] 1.1 Create `RegistrationDeadlines` value object record under `com.klabis.events.domain` with `Optional<LocalDate> deadline1, deadline2, deadline3` and validation invariants (sequenciality, monotonie, ≤ event date when used by Event)
- [ ] 1.2 Unit tests for `RegistrationDeadlines`: empty / single / two / three deadlines, missing-prerequisite (d2 without d1), out-of-order, helper methods `last()`, `nextRelevant(today)`, `registrationsOpen(today)`
- [ ] 1.3 Run new unit tests via test-runner — must pass

## 2. Domain model — Event aggregate (Green)

- [ ] 2.1 Replace `Event.registrationDeadline: Optional<LocalDate>` with `Event.registrationDeadlines: RegistrationDeadlines` in the aggregate; update create/update commands and factories accordingly
- [ ] 2.2 Add `Event.cancellationReason: Optional<String>`; modify `Event.cancel()` to accept `Optional<String> reason` (max 500 chars validation in command record)
- [ ] 2.3 Update internal usages of the deadline field (`Event.canRegister`, `Event.canEdit`, `EventFilter` if it filters on deadline) to use the new value object
- [ ] 2.4 Domain unit tests cover: cancel with/without reason, cancel reason length limit; registrations open across multiple deadlines (single, sequential, all-passed)
- [ ] 2.5 Run domain tests via test-runner — must pass

## 3. Persistence layer

- [ ] 3.1 Update `V001__initial_schema.sql`: add columns `cancellation_reason VARCHAR(500) NULL`, `registration_deadline_2 DATE NULL`, `registration_deadline_3 DATE NULL` to events table; verify the existing `registration_deadline` column can be reinterpreted as `registration_deadline_1` (or rename via migration if needed)
- [ ] 3.2 Update `EventMemento` to map all three deadline columns and the cancellation reason; reconstruct `RegistrationDeadlines` in `toEvent()`
- [ ] 3.3 Persistence integration tests (TestContainer Postgres): persist an event with 3 deadlines + cancellation reason, reload, assert equality
- [ ] 3.4 Run persistence tests via test-runner — must pass

## 4. ORIS import

- [ ] 4.1 Extend the ORIS-to-Event mapping to read `EntryDate2` and `EntryDate3` from the upstream ORIS payload and pass them into `RegistrationDeadlines`
- [ ] 4.2 If ORIS feed contains out-of-order deadlines, importer fails loudly (logs the violation, returns 422 to the manager) — no silent fallback
- [ ] 4.3 Integration test (mocked ORIS client): event with EntryDate1 only → 1 deadline; event with EntryDate1+2+3 → 3 deadlines; event with EntryDate1+3 (no 2) → import error
- [ ] 4.4 Run ORIS-related tests via test-runner

## 5. REST API + HAL forms

- [ ] 5.1 Update create/update Event request DTOs to accept `deadline1`, `deadline2`, `deadline3` (each optional)
- [ ] 5.2 Update cancel Event request DTO to accept optional `cancellationReason`
- [ ] 5.3 Update response DTO to expose all set deadlines and the cancellation reason
- [ ] 5.4 Update HAL-Forms templates exposed via the controller affordances — new fields with proper labels and constraints
- [ ] 5.5 Controller integration tests for new fields; ensure backward compatibility for clients still sending only `deadline1` (or the legacy `registrationDeadline` field if that name was used in the API surface)

## 6. Frontend — event form + cancel dialog + detail view

- [ ] 6.1 Update event create/update form (Formik) to include three deadline fields (date pickers) with sequenciality validation client-side; render server-side validation errors gracefully
- [ ] 6.2 Update cancel dialog to include an optional textarea "Důvod zrušení" (max 500 chars, char counter)
- [ ] 6.3 Update event detail page: add "Uzávěrky přihlášek" section listing all set deadlines chronologically, highlighting the currently relevant one; for cancelled events, show a "Akce byla zrušena" block with the reason if set
- [ ] 6.4 Update events table column "Uzávěrka": show the relevant deadline; if more deadlines exist, render a small badge/icon with tooltip listing the others; for cancelled rows with reason, surface the reason as a tooltip on the status indicator
- [ ] 6.5 Update `src/localization/labels.ts` with new labels: `deadline1`, `deadline2`, `deadline3`, `cancellationReason`, etc.
- [ ] 6.6 Frontend tests: form validation, deadline rendering in detail and table

## 7. End-to-end verification

- [ ] 7.1 Deploy to `https://api.klabis.otakar.io`
- [ ] 7.2 Browser test: create a new (manual) event with all three deadlines; verify table column behaviour, detail page listing, and registration open/closed semantics across deadlines
- [ ] 7.3 Browser test: import an ORIS event that has multiple entry dates (use a real ORIS event id from the production data — pick one before deploy); verify all deadlines imported correctly
- [ ] 7.4 Browser test: cancel an event with reason "Zrušeno kvůli počasí"; verify reason appears on event detail and as tooltip on row in events table
- [ ] 7.5 Browser test: cancel an event without reason; verify no reason text shown but cancellation works

## 8. Documentation

- [ ] 8.1 Update `docs/developerManual` with the new ORIS deadline mapping table
- [ ] 8.2 Sync the spec change into `openspec/specs/events/spec.md` after archiving
