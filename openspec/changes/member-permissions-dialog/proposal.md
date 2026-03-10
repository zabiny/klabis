## Why

Backend API pro správu oprávnění uživatelů (`GET/PUT /api/users/{id}/permissions`) je plně implementováno, ale ve frontendové aplikaci chybí uživatelské rozhraní. Tlačítko "Správa oprávnění" na stránce detailu člena aktuálně naviguje na URL místo otevření dialogu — funkce je tedy nedostupná.

## What Changes

- Tlačítko "Správa oprávnění" v `MemberDetailPage` se změní z navigačního linku na button otevírající modální dialog
- Nová React komponenta `PermissionsDialog` zobrazí seznam dostupných oprávnění jako toggle přepínače
- Dialog načte aktuální oprávnění uživatele a umožní jejich úpravu přímo v kontextu stránky člena

## Capabilities

### New Capabilities

- `member-permissions-dialog`: Modální dialog pro správu oprávnění uživatele přístupný z detailu člena — zobrazuje dostupná oprávnění jako toggle přepínače, načítá aktuální stav přes HATEOAS link a odesílá změny přes PUT endpoint

### Modified Capabilities

- `members`: Změna způsobu přístupu k funkci správy oprávnění — z navigace na modal dialog

## Impact

- **Frontend:** `MemberDetailPage.tsx`, nová komponenta `PermissionsDialog`
- **Backend:** Žádné změny — stávající API (`GET/PUT /api/users/{id}/permissions`) se využívá beze změny
- **API:** Žádné breaking changes
