## 1. Domain: ranking and base entry fee

- [x] 1.1 Add `EventRanking` value object (`levelId`, `shortName`, `name`) and events-local `Money` value object (`amount`, `currency`) in events domain — do NOT depend on `finance.domain.Money` (Modulith boundary)
- [x] 1.2 Add `ranking` (nullable `EventRanking`) and `baseEntryFee` (nullable `Money`) fields to `Event` aggregate; update `reconstruct(...)`
- [x] 1.3 Extend `CreateEventFromOris` and `SyncFromOris` commands with ranking and baseEntryFee; apply them in `Event.createFromOris(...)` and `syncFromOris(...)`
- [x] 1.4 Extend `UpdateEvent` command and `Event.update(...)` to allow manual setting of ranking and baseEntryFee
- [x] 1.5 Write/extend `Event` unit tests (TDD): create-from-oris, sync overwrite, manual update, absent values

## 2. Persistence

- [x] 2.1 Add nullable columns `level_id`, `level_short_name`, `level_name`, `base_entry_fee_amount`, `base_entry_fee_currency` to `events` table in `V001__initial_schema.sql`
- [x] 2.2 Map new fields in `EventMemento.from(Event)` and `toEvent()`
- [x] 2.3 Extend repository/memento test coverage to round-trip ranking and baseEntryFee

## 3. ORIS adapter mapping

- [x] 3.1 Map `EventDetails.level()` → `EventRanking` in `OrisEventImportService` (import and sync paths)
- [x] 3.2 Derive `baseEntryFee` as `Money(MAX(parsed EventClass.fee()), EventDetails.currency())`; skip empty/non-parsable fees; default currency CZK when ORIS currency blank/invalid; absent when no usable fee
- [x] 3.3 Write `OrisEventImportService` tests (TDD): ranking mapped, MAX fee chosen, zero/blank variants ignored, absent ranking/fee handled

## 4. REST API

- [x] 4.1 Add `ranking` and `baseEntryFee` to `EventDto` (ranking exposed with short label and name; baseEntryFee read/edit)
- [x] 4.2 Make ranking and baseEntryFee editable in the HAL+FORMS edit template; map them in `UpdateEventRequest`
- [x] 4.3 Extend controller/web-layer tests for ranking and baseEntryFee on detail and update

## 5. Frontend

- [x] 5.1 Show ranking (short label badge) and base entry fee (amount with currency) on the event detail page (events list stays unchanged)
- [x] 5.2 Add ranking and base entry fee fields to the event edit form (driven by HAL+FORMS template)
- [x] 5.3 Add/extend frontend tests for ranking and fee display on the event detail

## 6. Verification

- [ ] 6.1 Run backend tests (test-runner skill) and fix failures
- [ ] 6.2 Run frontend tests (test-runner skill) and fix failures
- [ ] 6.3 QA: import/sync a real ORIS event, verify ranking + base entry fee appear on the event detail
- [ ] 6.4 Code review before commit
