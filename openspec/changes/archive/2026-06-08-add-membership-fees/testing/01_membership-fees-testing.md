# Membership Fees — QA Testing

## Scenarios

### Admin — Catalog Management (Slice 11)
- [x] **CAT-1**: Admin sees membership fees section in navigation menu (Katalog úrovní + Vypsání pro rok)
- [x] **CAT-2**: Admin sees list of fee levels in catalog (customized page, not generic HAL)
- [x] **CAT-3**: Admin can open create new fee level form (customized modal, not generic HAL)
- [x] **CAT-4**: Admin can edit an existing fee level (inline form, pre-filled)
- [x] **CAT-5**: Admin can add payment rules to a level

### Admin — Year Publishing (Slice 11)
- [x] **PUB-1**: Admin sees year publishing page (Vypsání pro rok) with Rok + Uzávěrka voleb columns
- [x] **PUB-2**: Admin can publish levels for a year — form shows fee levels (not members) after fix
- [x] **PUB-3**: Admin sees published year detail with groups table and voting deadline

### Member — Fee Level Choice (Slice 12)
- [x] **MEM-1**: Member sees fee level choice page (customized page, not generic HAL)
- [x] **MEM-2**: Choice form shows available published levels in combobox
- [ ] **MEM-3**: Form pre-fills previous year's level — not testable with current test data (no prior year choice)
- [x] **MEM-4**: After voting deadline, choice page shows warning (tested via 2026 data — no publication, votingOpen=false)

### Member — Profile Widget (Slice 12)
- [x] **PROF-1**: Profile widget shows "ČLENSKÝ PŘÍSPĚVEK" section on profile page
- [ ] **PROF-2**: Profile widget shows prompt to choose when votingOpen=true — not testable without 2026 publication

---

## Issues Found (Iteration 1)

1. **Route prefix wrong** — Pages registered as `/administration/membership-fee-levels` but HAL links point to `/membership-fee-levels` → generic HAL page shown. **Fix**: removed `/administration/` prefix from App.tsx routes.
2. **Template name mismatches** — Frontend used `createMembershipFeeLevel`, `publishFeeYear`, `updateMembershipFeeGroup` but backend exposes `createLevel`, `publishYear`, `editSnapshot`. **Fix**: aligned frontend template names.
3. **Field name mismatch** — `annualFee`/`annualFeeSnapshot` in frontend vs `yearlyFeeAmount` in backend. **Fix**: aligned field names.
4. **Back links wrong** — `/administration/...` back links in detail pages. **Fix**: removed prefix.
5. **publishYear form shows members instead of levels** — `KlabisFieldsFactory` always overrides UUID field options with `/members/options`. **Fix**: skip override when `options.inline` already present; added `klabisAffordWithPromptedOptions` to backend for publishYear and chooseLevel.
6. **FeeYearPublicationDetailPage shows "-" for votingDeadline** — Used `choiceDeadline` field name but backend returns `votingDeadline`. **Fix**: aligned field name.
7. **FeeYearPublicationDetailPage groups not shown** — Expected embedded groups but backend returns `_links.levels` link. **Fix**: added `useAuthorizedQuery` fetch from levels link.
8. **chooseLevel template has no inline options** — membershipFeeGroupId had no options, so choice form was empty. **Fix**: added `klabisAffordWithPromptedOptions` in backend MemberFeeSummaryController.

## Issues Found (Iteration 2)

1. **Katalog úrovní prázdný** — `collectionName="membershipFeeLevelResponseList"` ale backend vrací `membershipFeeLevelSummaryResponseList`. **Fix**: správný klíč v MembershipFeeLevelsPage.
2. **Sloupec Uzávěrka voleb** — stále používalo starý label `choiceDeadline`. **Fix**: opraveno na `votingDeadline`.
3. **Formulář pravidel prázdný (rules)** — `KlabisFieldsFactory` neměl handler pro `PaymentRuleRequest`. **Fix**: přidán handler; pro `multi=true` vrací `null` (HalFormsCollectionField iteruje), pro jednotlivou položku renderuje sub-pole.
4. **Label "rules" nelokalizovaný** — `renderCompositeField` používal `prop.name` jako fallback místo `getFieldLabel`. **Fix**: použije `getFieldLabel(baseName)` kde baseName je bez `.N` indexu.
5. **Dead code v labels.ts** — staré klíče `createMembershipFeeLevel`, `publishFeeYear`, `updateMembershipFeeGroup`, `annualFee`, `choiceDeadline`, `percentage` odstraněny.

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| CAT-1 | PASS | Navigation shows Katalog úrovní + Vypsání pro rok |
| CAT-2 | PASS | Custom table page with sortable columns |
| CAT-3 | PASS | Create modal opens, form submits successfully |
| CAT-4 | PASS | Edit form opens inline with pre-filled values |
| CAT-5 | SKIP | Rules field visible but label not localized ("rules") |
| PUB-1 | PASS | Year publishing page with custom table |
| PUB-2 | PASS | Publish form shows fee levels after fix |
| PUB-3 | PASS | Year detail shows groups table with votingDeadline |
| MEM-1 | PASS | Custom choice page rendered |
| MEM-2 | PASS | Combobox shows published levels |
| MEM-3 | SKIP | No prior year data available |
| MEM-4 | PASS | Shows warning when votingOpen=false |
| PROF-1 | PASS | ČLENSKÝ PŘÍSPĚVEK widget shown on profile |
| PROF-2 | SKIP | Cannot test without 2026 publication data |

### Iteration 2
| Scenario | Result | Note |
|----------|--------|------|
| CAT-1 | PASS | (regression check — still OK) |
| CAT-2 | PASS | List now shows levels after collectionName fix |
| CAT-3 | PASS | Create modal works |
| CAT-4 | PASS | Edit form works |
| CAT-5 | PASS | Rules form: label "Pravidla spoluúčasti", add/remove multiple rules |
| PUB-1 | PASS | Column header now "Uzávěrka hlasování" |
| PUB-2 | PASS | (regression check — still OK) |
| PUB-3 | PASS | (regression check — still OK) |
| MEM-1 | PASS | (regression check — still OK) |
| MEM-2 | PASS | (regression check — still OK) |
| MEM-3 | SKIP | No prior year data available |
| MEM-4 | PASS | (regression check — still OK) |
| PROF-1 | PASS | (regression check — still OK) |
| PROF-2 | SKIP | Cannot test without 2026 publication data |
