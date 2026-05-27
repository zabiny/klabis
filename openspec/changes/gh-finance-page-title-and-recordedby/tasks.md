## 1. Backend: Expose recorder as HAL link on TransactionResource

- [x] 1.1 Write a failing test for `TransactionResource` HATEOAS serialization asserting that `_links.recordedBy` points to `/api/members/{recordedByUserId}` for a transaction with a known recorder
- [x] 1.2 Add the `recordedBy` link to `TransactionResource` in the finance module's REST representation (controller/representation model assembler) while keeping the existing flat `recordedBy` UUID field intact
- [x] 1.3 Verify existing finance controller/integration tests still pass (no regression on the flat field or other links)

## 2. Frontend: Page heading "Finance"

- [x] 2.1 Change H1 of the member financial account page from "Účet člena" to "Finance" (prefer reusing/updating the existing label in `src/localization/labels.ts`; if the heading is a JSX literal, refactor it to use `labels.ts`)
- [x] 2.2 Update or add frontend test asserting the H1 reads "Finance" on the financial account page

## 3. Frontend: Show recorder name in the transactions table

- [x] 3.1 Add a "Zaznamenal" (recorded by) column to the `TransactionsTable` rendered in `FinancesPage`
- [x] 3.2 For each transaction, follow `_links.recordedBy.href` via React Query and render the recorder's full name using the existing `MemberName` component (deduplicate by href so the same recorder is fetched only once per page)
- [x] 3.3 When `_links.recordedBy` is missing or the lookup fails (e.g. 404), render a dash ("—") in the cell
- [x] 3.4 Render the recorder name as plain text (not a clickable link)
- [x] 3.5 Add/extend frontend test covering: column is present, name renders for a resolvable link, dash renders when the link is missing

## 4. Verify end-to-end and archive

- [ ] 4.1 Run full backend test suite (`./gradlew test`) and confirm all tests pass
- [ ] 4.2 Run full frontend test suite (`npm test`) and confirm all tests pass
- [ ] 4.3 Manually verify on `http://localhost:3000` that: H1 reads "Finance"; transactions table shows the recorder column with real names; missing recorder renders "—"
