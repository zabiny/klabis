## Why

Akce ve stavu DRAFT (Koncept) zobrazují sekci přihlášek a odkaz na seznam přihlášek, přestože přihlašování není ve stavu DRAFT dostupné. Navíc formulář pro vytvoření/editaci akce neumožňuje vybrat kategorie z existujících šablon (Category Presets) — kategorie lze zadat pouze ručně, což je nepohodlné a náchylné k chybám.

## What Changes

- Detail akce ve stavu DRAFT nezobrazuje sekci přihlášek ani odkaz na seznam přihlášek
- Formulář vytvoření/editace akce nabízí možnost vybrat kategorie ze šablon (Category Presets) jako alternativu k ručnímu zadání

## Capabilities

### New Capabilities

*(žádné — jde o zpřesnění stávajícího chování)*

### Modified Capabilities

- `events`: Přidání požadavku, že sekce přihlášek a odkaz na přihlášky jsou skryty pro akce ve stavu DRAFT. Přidání požadavku na možnost vybrat kategorie ze šablon při vytvoření/editaci akce.
- `event-registrations`: Upřesnění, že odkaz na seznam přihlášek není dostupný pro akce ve stavu DRAFT.

## Impact

- Backend: HAL link na seznam přihlášek podmíněn stavem akce (pouze ACTIVE, FINISHED)
- Frontend: skrytí sekce přihlášek na detailu akce ve stavu DRAFT; přidání výběru z Category Presets do formuláře akce
