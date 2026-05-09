# review-1-4 — Event cancellation reason & multiple deadlines — QA Testing

## Scenarios

### Create event with multiple deadlines (N6)
- [x] **CREATE-1**: Form to create a new event renders 1 visible deadline input by default and offers an "add" affordance up to 3 (HalFormsCollectionField from `multiple: true` + `max: 3`)
- [x] **CREATE-2**: Manager can create an event with three deadlines (e.g. d1 = today+10, d2 = today+15, d3 = today+20, event date = today+30); event is created successfully
- [x] **CREATE-3**: Server rejects out-of-order deadlines (d2 < d1) with form-level error
- [x] **CREATE-4**: Server rejects deadline > event date with form-level error

### Detail view (N6 + N3)
- [x] **DETAIL-1**: Detail page of the 3-deadline event lists all three deadlines chronologically under "Uzávěrky přihlášek"
- [x] **DETAIL-2**: Currently relevant deadline (earliest future) is visually highlighted with `aktuální` badge
- [x] **DETAIL-3**: For an event with single deadline, only that deadline is shown (no badge clutter)

### Events table (N6 + N3)
- [x] **TABLE-1**: Events list "Uzávěrka" column shows the relevant deadline date for the multi-deadline event
- [x] **TABLE-2**: For a multi-deadline event, an indicator (badge / +N icon) signals there are additional deadlines and tooltip lists the others
- [x] **TABLE-3**: For a single-deadline event, no indicator badge appears

### Cancel event with reason (N3)
- [x] **CANCEL-1**: Cancel dialog includes an optional textarea labeled "Důvod zrušení" with max 500 chars and a char counter
- [x] **CANCEL-2**: Cancelling an event with reason "Zrušeno kvůli počasí" succeeds; status becomes CANCELLED; reason is recorded
- [x] **CANCEL-3**: Detail page of the cancelled event shows the cancellation reason in a "Akce byla zrušena" block
- [x] **CANCEL-4**: Events list row for the cancelled event surfaces the reason via tooltip on the cancelled status indicator
- [x] **CANCEL-5**: Cancelling an event without reason succeeds; no reason text rendered anywhere

### ORIS import multi-deadlines (skipped)
- [ ] **ORIS-1**: SKIP — covered by backend integration tests `OrisEventImportServiceTest` (3 cases incl. fail-loud)

---

## Results

### Iteration 1

| Scenario | Result | Note |
|----------|--------|------|
| CREATE-1 | PARTIAL | Add/remove buttons render and respect min/max — but **no field label** ("Uzávěrky přihlášek" or similar) above the deadlines collection. Two unlabeled "Přidat" buttons appear at the top of the form (one per collection field: `categories`, `deadlines`). Same applies to pre-existing `categories` collection — pre-existing UX gap, not introduced by this proposal. Functional behaviour OK. |
| CREATE-2 | PASS | Created "QA Test - 3 deadliny" with deadlines 2026-05-19/24/29, event date 2026-06-08. |
| CREATE-3 | PASS | API returns 400 with `fieldErrors.deadlinesOrdered = "Deadlines must be in non-decreasing order"`. |
| CREATE-4 | PASS | API returns 400 with `detail = "Registration deadline must be on or before event date"`. |
| DETAIL-1 | PASS | "UZÁVĚRKY PŘIHLÁŠEK" section lists 19.5, 24.5, 29.5 chronologically. |
| DETAIL-2 | PASS | "19. 5. 2026" carries `aktuální` badge (today = 2026-05-09, so first future deadline). |
| DETAIL-3 | PASS | Existing single-deadline events render only the date (e.g. Žďárský pohár row shows just "11. 5. 2026"). |
| TABLE-1 | PASS | "QA Test - 3 deadliny" row shows "19. 5. 2026" (relevant). |
| TABLE-2 | PASS | `+2` badge with `title="24. 5. 2026, 29. 5. 2026"` tooltip. |
| TABLE-3 | PASS | Žďárský pohár (single deadline) renders just date, no `+N` badge. |
| CANCEL-1 | FAIL | Textarea labeled "Důvod zrušení (volitelné)" present, but **no character counter** rendered (spec asks for one). Also dialog title shows raw template name `cancelEvent` instead of localized "Zrušit akci". |
| CANCEL-2 | PASS | Submitted with "Zrušeno kvůli počasí"; status → "Zrušeno". |
| CANCEL-3 | PASS | Detail page shows "AKCE BYLA ZRUŠENA" block with reason text. |
| CANCEL-4 | PASS | Status cell on row shows "Zrušeno" with `title="Zrušeno kvůli počasí"`. |
| CANCEL-5 | PASS | Cancelling "Trénink – terén Černava" without reason → status "Zrušeno", no tooltip. |

## Discovered issues

### Issue 1 (HIGH, frontend) — Cancel dialog: missing char counter and localized title

**Where:** Cancel-event modal opened from events table or detail page.

**Expected (per spec / tasks 6.2):** Optional textarea "Důvod zrušení" with **max 500 chars** and **char counter**.

**Actual:**
- Textarea is present and labeled correctly.
- **No char counter** is rendered.
- Dialog header reads raw template name `cancelEvent` rather than a Czech label like "Zrušit akci". (Likely missing from `localization/labels.ts` `templates` map — pre-existing pattern fault but the new template inherits it.)

**Impact:** CANCEL-1 fails. UX regression vs. tasks.md 6.2.

**Component:** Frontend (HalForms cancel dialog wiring + labels).

### Issue 2 (MEDIUM, frontend) — Collection fields render without a label

**Where:** Create/edit event form. Both `categories` (pre-existing) and `deadlines` (new in this proposal) are rendered as a bare "Přidat" button followed by item rows when items exist. There is no visible field heading like "Uzávěrky přihlášek" or "Kategorie".

**Root cause:** `HalFormsCollectionField` renders the heading only when `prop.prompt` is set (see `frontend/src/components/HalNavigator2/halforms/fields/HalFormsCollectionField.tsx:92`). Backend HAL-Forms does not send a prompt, and this component lacks the `getFieldLabel(prop.name)` fallback that other field components (`HalFormsInput`, `HalFormsCheckboxGroup`, `HalFormsMemberId`) use.

**Impact:** CREATE-1 only partial. Users see an isolated "Přidat" button with no context for what they are adding. The *new* `deadlines` field inherits the same gap. It is technically pre-existing but the new spec explicitly assumes a "Uzávěrky přihlášek" / "Přidat uzávěrku" label (tasks 6.5).

**Suggested fix:** In `HalFormsCollectionField.tsx:92`, replace `prop.prompt &&` with `(prop.prompt || getFieldLabel(prop.name)) &&` and use the same fallback in the rendered span. Add `deadlines` (and `categories`) to `labels.fields` if not already present.

**Component:** Frontend.

---

## Summary

- **15 scenarios planned, 14 executed (1 skipped — ORIS import covered by backend tests).**
- Iteration 1: 12 PASS, 1 PARTIAL, 1 FAIL → fixes delegated.

### Iteration 2 — verify fixes

| Scenario | Result | Note |
|----------|--------|------|
| CREATE-1 | PASS | "Kategorie" and "Uzávěrky přihlášek" labels render above the respective collection sections (HalFormsCollectionField now falls back to `getFieldLabel(prop.name)`). |
| CANCEL-1 | PASS | Cancel dialog title now "Zrušení akce"; textarea renders (backend annotated `@HalForms(formInputType="textarea")`); char counter shows `0 / 500` when empty (`HalFormsTextArea` falls back to `prop.max` when `prop.maxLength` undefined — backend serializes `@Size(max=500)` as `max`). |

All scenarios PASS. Frontend tests 1338/1338, backend tests 2415/2416 (1 pre-existing failure unrelated). Multi-deadline + cancellation reason features fully verified end-to-end.
