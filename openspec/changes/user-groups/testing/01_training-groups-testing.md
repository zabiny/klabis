# Training Groups (Slice 2) - QA Testing

## Scenarios

### Autorizace (AUTH)
- [x] **AUTH-1**: Uživatel bez GROUPS:TRAINING nevidí "Tréninkové skupiny" v navigaci
- [x] **AUTH-2**: Uživatel bez GROUPS:TRAINING dostane chybu při přístupu na `/training-groups`
- [x] **AUTH-3**: Admin (s GROUPS:TRAINING) vidí navigační položku a stránku

### Seznam tréninkových skupin (LIST)
- [x] **LIST-1**: Admin vidí seznam všech tréninkových skupin
- [x] **LIST-2**: Prázdný stav — zobrazí se vhodná zpráva
- [x] **LIST-3**: Sloupce: název, min. věk, max. věk, počet členů
- [x] **LIST-4**: Tlačítko pro vytvoření tréninkové skupiny je přítomné

### Vytvoření tréninkové skupiny (CREATE)
- [x] **CREATE-1**: Formulář obsahuje pole: název, minAge, maxAge
- [x] **CREATE-2**: Vytvoření skupiny s platným věkovým rozmezím uspěje
- [x] **CREATE-3**: Vytvoření skupiny s překrývajícím se rozmezím vrátí chybu

### Detail tréninkové skupiny (DETAIL)
- [x] **DETAIL-1**: Detail zobrazuje název, věkové rozmezí, vlastníky, členy
- [x] **DETAIL-2**: Vlastník vidí tlačítka pro úpravu, změnu věkového rozmezí, smazání
- [x] **DETAIL-3**: Přidání člena funguje
- [x] **DETAIL-4**: Odebrání člena funguje
- [x] **DETAIL-5**: Smazání skupiny přesměruje na seznam

### Úprava skupiny (EDIT)
- [x] **EDIT-1**: Přejmenování skupiny funguje
- [x] **EDIT-2**: Změna věkového rozmezí na nepřekrývající se funguje
- [x] **EDIT-3**: Změna věkového rozmezí na překrývající se vrátí chybu

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| AUTH-1 | PASS | Uživatel ZBM9500 nevidí "Tréninkové skupiny" v navigaci |
| AUTH-2 | PASS | Přístup na /training-groups vrátí 403 chybu |
| AUTH-3 | PASS | Admin ZBM9000 vidí navigační položku a stránku |
| LIST-1 | PASS | Seznam skupin zobrazen správně |
| LIST-2 | PASS | Zpráva "Nejste členem žádné tréninkové skupiny." |
| LIST-3 | PASS | Sloupce: Název, Min. věk, Max. věk, Počet členů |
| LIST-4 | PASS | Tlačítko "Vytvořit tréninkovou skupinu" přítomno |
| CREATE-1 | PASS | Formulář obsahuje pole název, minAge, maxAge |
| CREATE-2 | PASS | Skupina vytvořena, redirect na seznam |
| CREATE-3 | PASS | Chyba "Age range overlaps" při překrývajícím se rozmezí |
| DETAIL-1 | PASS | Zobrazuje název, věkové rozmezí, sekci SPRÁVCI a ČLENOVÉ |
| DETAIL-2 | PASS | Vlastník vidí tlačítka Upravit název, Změnit věkové rozmezí, Smazat |
| DETAIL-3 | PASS | Přidání člena funguje, tabulka se obnoví po zavření modálu |
| DETAIL-4 | PASS | Odebrání člena funguje, tabulka se okamžitě aktualizuje |
| DETAIL-5 | PASS | Smazání skupiny přesměruje na /training-groups |
| EDIT-1 | PASS | Přejmenování funguje, nadpis se aktualizuje ihned |
| EDIT-2 | PASS | Změna věkového rozmezí na nepřekrývající se funguje |
| EDIT-3 | PASS | Chyba "Age range overlaps with existing training group range [10-15]" |

**Výsledek Iterace 1: 18/18 PASS** ✅

Opravené problémy během QA:
1. Backend: metoda `renameTrainingGroup` přejmenována na `updateTrainingGroup` (template name shoda)
2. Frontend: `TrainingGroupOwner.ownerId` → `memberId` (shoda s backendem)
3. Backend: přidán `TrainingGroupsRootPostprocessor` pro link v root API
4. Frontend: `TrainingGroupDetail` přesunuto z `_embedded` na top-level (Spring HATEOAS serializace)
5. Frontend: přidán `route.refetch()` po zavření add/remove member modálů
