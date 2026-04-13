## 1. Domain — relax create path (TDD)

- [x] 1.1 Add failing unit test: `CalendarItem.createManual()` succeeds when description is `null`; the resulting aggregate has `description == null`
- [x] 1.2 Add failing unit test: `CalendarItem.createManual()` still rejects missing name, missing start date, missing end date, and end date before start date
- [x] 1.3 Remove `validateDescription()` call from `CalendarItem.createManual()` and delete the now-unused `validateDescription()` helper method
- [x] 1.4 Remove `@NotBlank` from `CreateCalendarItem.description`; keep `@Size(max = 1000)`; make the field Javadoc explicit that null/empty means "no description"
- [x] 1.5 Verify tests 1.1 and 1.2 pass

## 2. Domain — relax update path (TDD)

- [x] 2.1 Add failing unit test: `CalendarItem.update()` succeeds when the update command's description is `null`; the resulting aggregate has `description == null`
- [x] 2.2 Add failing unit test: `CalendarItem.update()` clears an existing description when the new description is `null`
- [x] 2.3 Add failing unit test: `CalendarItem.update()` still throws `CalendarItemReadOnlyException` for event-linked items
- [x] 2.4 Remove `validateDescription()` call from `CalendarItem.update()`
- [x] 2.5 Remove `@NotBlank` from `UpdateCalendarItem.description`; keep `@Size(max = 1000)`
- [x] 2.6 Verify tests 2.1–2.3 pass

## 3. API boundary — normalize blank description to null (TDD)

- [x] 3.1 Add failing `@WebMvcTest` test on `CalendarController` create endpoint: POST with `"description": ""` persists the item with `description == null`
- [x] 3.2 Add failing `@WebMvcTest` test on `CalendarController` create endpoint: POST with `"description": "   "` (whitespace-only) persists the item with `description == null`
- [x] 3.3 Add failing `@WebMvcTest` test on `CalendarController` update endpoint: PUT with `"description": ""` clears the stored description
- [x] 3.4 Implement the normalization: in `CalendarController` (or a small private helper / DTO mapper), convert blank descriptions to `null` before passing the command to the application service
- [x] 3.5 Verify tests 3.1–3.3 pass

## 4. Database schema

- [x] 4.1 In `backend/src/main/resources/db/migration/V001__initial_schema.sql`, change `description TEXT NOT NULL` to `description TEXT NULL` on the `calendar_items` table
- [x] 4.2 Restart the backend locally and confirm Flyway re-applies V001 (in-memory H2 reset); verify the schema has `description` nullable (H2 console or `SELECT` on `INFORMATION_SCHEMA.COLUMNS`)

## 5. Repository round-trip verification (TDD)

- [x] 5.1 Add failing integration test for `CalendarRepositoryAdapter`: save and reload a `CalendarItem` with `description == null`; verify it comes back as `null`
- [x] 5.2 Confirm `CalendarMemento` persists and rehydrates `null` description correctly (no code change expected; the field is already a plain `String`)
- [x] 5.3 Verify test 5.1 passes

## 6. HAL-Forms metadata — verify `required: false`

- [x] 6.1 Add a test on `CalendarController` that inspects the HAL-Forms template for the create action and asserts the `description` property has `required: false`
- [x] 6.2 Add the same assertion for the update template
- [x] 6.3 If `required: true` is still emitted, trace why (Spring HATEOAS picks up `@NotBlank` automatically); with `@NotBlank` removed in tasks 1.4 and 2.5 this should already be correct

## 7. Frontend — handle missing description

- [x] 7.1 Locate the calendar item detail page component (search `frontend/src` for usage of `CalendarItemDto.description`)
- [x] 7.2 Ensure the component renders nothing (no label, no empty value) when `description` is `null` or missing
- [x] 7.3 Verify manually in the running app: create a calendar item without description via the form, open its detail, confirm no stray label or `null` text

## 8. Frontend QA walkthrough

- [x] 8.1 Log in as admin (`ZBM9000`), open the calendar page, click create
- [x] 8.2 Submit the create form with only name, start date, end date → calendar item appears in the calendar
- [x] 8.3 Open the detail of the new item → description is not shown
- [x] 8.4 Edit an existing item: clear its description, save → detail view no longer shows the description
- [x] 8.5 Attempt to submit a description > 1000 characters → inline validation error
- [x] 8.6 Confirm existing tests (`./gradlew test` via test-runner agent, `npm test` via test-runner agent) still pass

## 9. Spec housekeeping

- [ ] 9.1 After merge, the `calendar-items` spec will be updated by the openspec archive step; no manual edits to `openspec/specs/calendar-items/spec.md` required during implementation
