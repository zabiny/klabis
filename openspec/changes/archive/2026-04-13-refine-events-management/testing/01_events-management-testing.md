# Events Management Refinement - QA Testing

## Scenarios

### ORIS Import & Location Handling
- [x] **EVT-1**: ORIS import dialog zobrazuje radio buttons (JM/M/ČR), při změně výběru se reload seznam
- [x] **EVT-2**: Vytvoření akce bez lokace → úspěch, detail akce skryje řádek lokace

### Row-Level Management Actions (Admin)
- [x] **EVT-3**: V seznamu akcí — DRAFT akce zobrazuje pro admina: Upravit + Publikovat + Zrušit
- [x] **EVT-4**: V seznamu akcí — ACTIVE akce zobrazuje pro admina: Upravit + Zrušit
- [x] **EVT-5**: V seznamu akcí — FINISHED nebo CANCELLED akce nezobrazuje žádné management akce

### Authorization
- [x] **EVT-6**: Jako běžný člen (ZBM9500) — v seznamu akcí jen registrovat/odregistrovat, žádné management

### "Finish" Button Removed
- [x] **EVT-7**: V detailu akce není žádné tlačítko "Ukončit akci"

### Calendar Description
- [x] **EVT-8**: Publikovaná akce bez lokace → v kalendáři/detailu žádný stray " - " prefix v popisu

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| EVT-1 | PASS | Dialog zobrazuje radio buttons JM/M/ČR; přepnutí na ČR načte jiný seznam závodů |
| EVT-2 | PASS | Akce vytvořena bez lokace; detail skryje řádek lokace (pouze Datum konání + Pořadatel) |
| EVT-3 | PASS | DRAFT řádek zobrazuje: Upravit + Publikovat + Zrušit akci |
| EVT-4 | PASS | ACTIVE řádek zobrazuje: Upravit + Zrušit akci (+ Přihlásit se pro registraci) |
| EVT-5 | PASS | CANCELLED řádek má prázdnou buňku Akce — žádné management tlačítka |
| EVT-6 | PASS | ZBM9500 vidí pouze Přihlásit se u ACTIVE akce; žádné management akce; Status sloupec skrytý |
| EVT-7 | PASS | Detail ACTIVE akce: pouze Upravit + Zrušit akci + Přihlásit se; žádné "Ukončit akci" |
| EVT-8 | PASS | Kalendářová položka pro ACTIVE akci bez lokace má description="Klabis" (jen organizátor, žádný " - " prefix) |

**Výsledek: 8/8 PASS — vše funguje správně.**
