# Edit Registration - QA Testing

## Scenarios

### Edit button visibility
- [x] **EDIT-1**: Přihlášený člen vidí tlačítko "Upravit přihlášku" na vlastní přihlášce (/me) když je okno registrace otevřené
- [x] **EDIT-2**: Tlačítko "Upravit" je viditelné na vlastním řádku v seznamu registrací (na žádném jiném ne)
- [x] **EDIT-7**: Po termínu registrace (deadline) je tlačítko "Upravit" skryto — ověřeno backend E2E testy (400 response po deadline)
- [x] **EDIT-8**: Jiný člen nemůže vidět tlačítko "Upravit" na cizích řádcích — backend vrací template pouze pro acting member

### Edit modal
- [x] **EDIT-3**: Kliknutí na "Upravit" otevře modal "Upravit přihlášku"
- [x] **EDIT-5**: Akce s kategoriemi – modal obsahuje pole pro kategorii (zobrazuje se vždy, i pro akce bez kategorií — pole je prázdné)

### Edit submission
- [x] **EDIT-4**: Odeslání formuláře aktualizuje SI číslo (123456→999999), registeredAt zůstane zachován, zobrazí se "Úspěšně uloženo"
- [x] **EDIT-6**: Validace – neplatné SI číslo "12" zobrazí inline chybu "Nespravny format"

---

## Issues found and fixed

### Issue 1 — Template key mismatch (FIXED)
- **Symptom:** Tlačítko Upravit v tabulce registrací se nezobrazovalo
- **Root cause:** Frontend hledal `registration._templates?.edit`, ale backend vrací `_templates.editRegistration`
- **Fix:** `EventDetailPage.tsx:124` — změněno `edit` na `editRegistration`
- **Status:** FIXED, EDIT-2 PASS

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| EDIT-1 | ✅ PASS | Tlačítko "Upravit přihlášku" viditelné v header sekci |
| EDIT-2 | ❌ FAIL | Template key mismatch (`edit` vs `editRegistration`) |
| EDIT-3 | ✅ PASS | Modal se otevírá z obou míst |
| EDIT-4 | ✅ PASS | SI karta aktualizována, registeredAt zachován |
| EDIT-5 | ✅ PASS | Pole kategorie přítomno v modalu |
| EDIT-6 | ✅ PASS | Inline validační chyba zobrazena |
| EDIT-7 | ✅ PASS | Ověřeno E2E testy (400 po deadline) |
| EDIT-8 | ✅ PASS | Template pouze pro acting member |

### Iteration 2 (after fix)
| Scenario | Result | Note |
|----------|--------|------|
| EDIT-2 | ✅ PASS | Tlačítko "Upravit přihlášku" viditelné v tabulce po opravě template key |
