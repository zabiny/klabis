# Calendar Item - Volitelný popis - QA Testing

## Scenarios

### Vytváření a zobrazení
- [x] **CAL-1**: Vytvoření calendar item bez popisu → položka se zobrazí v kalendáři
- [x] **CAL-2**: Detail položky bez popisu → sekce/řádek s popisem se nezobrazuje
- [x] **CAL-3**: Editace položky, smazání popisu → detail přestane zobrazovat popis
- [x] **CAL-4**: Zadání popisu > 1000 znaků → inline validační chyba
- [x] **CAL-5**: Tlačítko "Vytvořit" se nezobrazuje uživateli bez oprávnění CALENDAR:MANAGE

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| CAL-1 | PASS | Item "QA Test - bez popisu" created without description, appeared on Apr 20 in calendar |
| CAL-2 | PASS | Detail table shows no description row when item has no description |
| CAL-3 | PASS | After clearing description in edit form, detail table no longer shows description row |
| CAL-4 | PASS | Submitting description > 1000 chars shows alert "description: Calendar item description must not exceed 1000 characters" |
| CAL-5 | PASS | Regular member (ZBM9500) sees no "Přidat položku" button on calendar page |

### Iteration 2 (2026-04-13)
| Scenario | Result | Note |
|----------|--------|------|
| CAL-1 | PASS | Item "CAL-1 Test bez popisu" created on Apr 25 without description; appears in calendar and detail shows no description row |
| CAL-2 | PASS | Detail of item created without description shows only id, name, startDate, endDate — no description row |
| CAL-3 | PASS | Created "CAL-3 Test s popisem" with description, then edited and cleared it; detail no longer shows description row after save |
| CAL-4 | PASS | Submitting description of 1001 chars shows alert "description: Calendar item description must not exceed 1000 characters"; form stays open, item not created |
| CAL-5 | PASS | Regular member (ZBM9500) sees no "Přidat položku" button on calendar page |
