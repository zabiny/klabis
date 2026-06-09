# Fee Levels & Rules — QA Testing

## Scenarios

### Catalog — List & Navigation
- [x] **LEV-1**: Fee levels list shows existing levels with name and yearly fee columns

### Catalog — Create
- [x] **LEV-2**: Admin creates a level without rules — level appears in list
- [x] **LEV-3**: Admin creates a level with a PERCENTAGE rule — rule visible in detail
- [x] **LEV-4**: Admin creates a level with a FIXED_SURCHARGE rule — rule visible in detail with CZK amount

### Catalog — Detail & Display
- [x] **LEV-5**: Detail page shows rule table with columns: Typ akce, Zkratka žebříčku, Typ pravidla, Hodnota
- [x] **LEV-6**: PERCENTAGE rule displays as "N %" in value column
- [x] **LEV-7**: FIXED_SURCHARGE rule displays as "N CZK" in value column

### Catalog — Edit
- [x] **LEV-8**: Admin edits level name — changes reflected in detail, rules pre-filled in edit form

### Catalog — Delete
- [x] **LEV-9**: Admin deletes a level — redirected to list, level no longer present

### Rules — Validation
- [x] **LEV-10**: Duplicate rule (same eventTypeId + rankingShortName) on same level is rejected by backend

### Authorization
- [x] **LEV-11**: Regular member cannot see create/edit/delete buttons on catalog pages

### Previously skipped scenarios (test data creation)
- [ ] **MEM-3**: Form pre-fills previous year's level as default — SKIP (H2 resets on restart, no 2025 data available)
- [x] **PROF-2**: Profile widget shows prompt to choose when votingOpen=true (created 2026 publication with deadline 2026-12-31)

---

## Issues Found (Iteration 1)

1. **Backend 500 při vytváření úrovně s pravidly** — check constraint `chk_membership_payment_rule_type` selhával při live requests; reproducible pouze v live prostředí (ne v testech). Příčina: Spring DevTools cached třídy ze starého buildu. Vyřešeno restartem backendu. Přidány 3 integration testy `MembershipFeeLevelWithRulesIntegrationTest`.
2. **eventTypeId v rules formuláři jako UUID textbox** — uživatel nemůže zadat UUID ručně. **Fix**: přidán `PaymentRuleFormFields` React component s `useEventTypes` hookem, eventTypeId renderuje jako select z event types API.
3. **ruleType jako volný textbox** — uživatel musí znát přesný string. **Fix**: ruleType renderuje jako select s options PERCENTAGE/FIXED_SURCHARGE a českými labely.
4. **Frontend interface `coParticipationRules` vs backend `rules`** — detail page neprázdno zobrazovala pravidla protože interface čekal `coParticipationRules` ale backend vrací `rules`. **Fix**: sjednocení na `rules`.

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| LEV-1 | PASS | List shows Závodní (1200), Závodní se pravidlem (1500) |
| LEV-2 | PASS | Level without rules created, detail shows correctly |
| LEV-3 | PASS | PERCENTAGE rule created via select (Závod + A + 25%) |
| LEV-4 | PASS | FIXED_SURCHARGE rule created (Závod + LOB + 150 CZK) |
| LEV-5 | PASS | Table: Typ akce, Zkratka žebříčku, Typ pravidla, Hodnota |
| LEV-6 | PASS | PERCENTAGE displays as "25 %" |
| LEV-7 | PASS | FIXED_SURCHARGE displays as "150 CZK" |
| LEV-8 | PASS | Edit: name changed, rules pre-filled with selects |
| LEV-9 | PASS | Delete: redirected to list, level removed |
| LEV-10 | PASS | Duplicate Závod+A rejected with 400 Bad Request |
| LEV-11 | PASS | Member sees list/detail without create/edit/delete |
| MEM-3 | SKIP | H2 resets on restart — no 2025 data persists |
| PROF-2 | PASS | "Probíhá volba úrovně..." prompt shown; after choice "Máte zvolenu úroveň Závodní 1200 Kč" |
