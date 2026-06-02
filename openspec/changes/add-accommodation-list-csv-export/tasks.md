## 1. Backend — CSV generation

- [ ] 1.1 Add `org.apache.commons:commons-csv` dependency to the backend build (D2)
- [ ] 1.2 Write a failing test for the accommodation-list CSV renderer: given a list of accommodation items, it produces CSV with `;` delimiter, a Czech header row (`Jméno;Příjmení;Číslo OP;Platnost OP;Datum narození;Adresa`), one row per member, and the address rendered as a single combined column matching the print layout (D3, D4)
- [ ] 1.3 Implement the CSV renderer to pass 1.2 (Apache Commons CSV, delimiter `;`, header, combined address)
- [ ] 1.4 Add a failing test asserting missing identity card number / validity date / date of birth produce empty cells (not "neuvedeno") in the CSV (D5)
- [ ] 1.5 Make 1.4 pass (null values map to empty cells)
- [ ] 1.6 Add a failing test asserting the rendered bytes start with a UTF-8 BOM so Czech Excel shows diacritics correctly; implement BOM prefix to pass

## 2. Backend — content negotiation endpoint

- [ ] 2.1 Write a failing controller test: `GET /api/events/{eventId}/accommodation-list` with `Accept: text/csv` returns `200 text/csv`, a `Content-Disposition: attachment` header with filename `ubytovani-{slug}.csv`, and the CSV body (D1, D7)
- [ ] 2.2 Write a failing test: the same endpoint with `Accept: application/prs.hal-forms+json` still returns the existing HAL collection unchanged (regression guard for D1)
- [ ] 2.3 Extend the accommodation-list controller method to negotiate on `Accept`: serve CSV for `text/csv`, HAL for the existing media type, reusing the existing item-assembly logic (no duplication)
- [ ] 2.4 Implement the event-name slugification for the download filename (diacritics and spaces to hyphens) and set `Content-Disposition` (D7)
- [ ] 2.5 Write a failing test: a user who is neither the event coordinator nor holds EVENTS:REGISTRATIONS receives an authorization error for the `text/csv` request; confirm the existing authorization check covers the CSV path

## 3. Frontend — download action

- [ ] 3.1 Add a "Stáhnout CSV" button to `AccommodationListPage` next to the existing "Tisknout" action (hidden in print view)
- [ ] 3.2 Implement the download handler: authorized `fetch` of the accommodation-list link with `Accept: text/csv`, build a `Blob`, create an object URL, trigger a `<a download>` click, and revoke the URL afterwards (D6)
- [ ] 3.3 Derive the download filename from the `Content-Disposition` response header
- [ ] 3.4 Add the "Stáhnout CSV" button label to the localization labels

## 4. Verification

- [ ] 4.1 Run backend tests (CSV renderer + controller negotiation + authorization) and confirm all green
- [ ] 4.2 Manually verify in the browser: coordinator downloads CSV, opens it in MS Excel (Czech locale) with correct diacritics, header row present, missing values empty, address in one column
- [ ] 4.3 Manually verify a non-authorized user sees neither the "Stáhnout CSV" action nor can download via direct request
