## Why

Stránka finančního účtu člena má dva drobné UX nedostatky, které ztěžují audit transakcí: nadpis stránky je obecný ("Účet člena") místo srozumitelného pojmenování, a u jednotlivých transakcí není vidět, kdo je do systému zaznamenal. Bez údaje o tom, kdo transakci pořídil, není možné dohledat odpovědnost za záznamy ani efektivně řešit reklamace.

## What Changes

- Změnit H1 nadpis stránky finančního účtu člena z "Účet člena" na "Finance".
- V seznamu transakcí (jak na stránce vlastního účtu, tak na stránce účtu jiného člena pro finance managera) zobrazit u každé transakce jméno uživatele, který transakci zaznamenal.
- Pokud uživatele, který transakci zaznamenal, nelze určit (smazaný účet, nedohledatelný odkaz), zobrazit pomlčku ("—").
- Backend: u každé transakce v HAL+FORMS reprezentaci poskytnout HAL link na uživatele, který transakci zaznamenal (`_links.recordedBy`). Stávající ploché pole `recordedBy: UUID` zůstává — link je doplnění, ne náhrada (z pohledu klienta je UUID stále užitečné jako identifikátor).

## Capabilities

### New Capabilities

(žádné)

### Modified Capabilities

- `member-accounts`: doplnit požadavek na zobrazení jména pořizovatele transakce v historii a sjednocení nadpisu stránky na "Finance".

## Impact

- Backend, modul `finance`: `TransactionResource` doplnit o `_links.recordedBy` směřující na `/api/members/{id}` (HAL link, ne embedded).
- Frontend: `MemberAccountManagePage` (titulek) a `FinancesPage`/`TransactionsTable` (sloupec se jménem pořizovatele).
- Frontend: znovupoužít existující komponentu `MemberName` (nebo `MemberNameWithRegNumber`) pro vykreslení jména z linku.
- Žádná změna datového modelu — `Transaction.recordedBy` už v doméně existuje.
- Žádné dopady na autorizaci ani GDPR (jméno pořizovatele transakce smí vidět každý, kdo už vidí transakci samotnou).
