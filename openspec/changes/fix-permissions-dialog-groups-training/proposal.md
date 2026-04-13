## Why

Dialog správy oprávnění člena nezobrazuje oprávnění `GROUPS:TRAINING`, které řídí přístup ke správě tréninkových skupin. Oprávnění proto nelze přidělit ani odebrat přes UI — administrátor musí použít jiný způsob, nebo není přidělení vůbec možné.

## What Changes

- Dialog správy oprávnění zobrazuje `GROUPS:TRAINING` jako přepínač s českým popiskem a popisem

## Capabilities

### New Capabilities

*(žádné)*

### Modified Capabilities

- `member-permissions-dialog`: Přidání `GROUPS:TRAINING` do seznamu zobrazovaných oprávnění v dialogu.

## Impact

- Frontend: přidání položky `GROUPS:TRAINING` do seznamu oprávnění v dialogu (popisek + popis)
- Backend: ověřit, že endpoint pro správu oprávnění `GROUPS:TRAINING` přijímá a ukládá
