## Why

Stránky zobrazující finanční účet (jak vlastní `Finance`, tak detail účtu kteréhokoli člena pro finance managera) v současnosti neukazují, **komu** účet patří. Při pohledu finance managera na cizí účet z administrace tak chybí potvrzení identity vlastníka (lze omylem provést transakci na nesprávném účtu); na vlastní stránce `Finance` zase chybí kontext typu „pro koho jsou tato data" — zvlášť důležité pro rodiče přihlášené pod sebou, kteří v budoucnu mohou spravovat účty více členů domácnosti.

## What Changes

- Stránka **Účet člena** (`/members/:memberId/account`, pohled finance managera) v hlavičce zobrazí jméno, příjmení a registrační číslo majitele účtu.
- Stránka **Moje finance** (`/finances`, pohled přihlášeného člena na vlastní účet) v hlavičce zobrazí jméno, příjmení a registrační číslo přihlášeného člena.
- Identita majitele se získá výhradně přes HATEOAS — frontend bude následovat `accountOwner` link, který backend již dnes do HAL+FORMS odpovědi přidává v `MemberAccountController`. Backend tedy nebude měněn.

## Capabilities

### New Capabilities
<!-- žádné nové capability -->

### Modified Capabilities
- `member-accounts`: Doplnit požadavek, že stránka zobrazující finanční účet ukazuje vedle zůstatku a historie také identitu majitele účtu (jméno, příjmení, registrační číslo) — jak pro pohled finance managera, tak pro pohled člena na vlastní účet.

## Impact

- **Frontend:** `MemberAccountManagePage.tsx` a `FinancesPage.tsx` — doplnění hlavičky s identitou majitele.
- **Backend:** Beze změny. `accountOwner` link v `MemberAccountController` se již dnes posílá a směřuje na `MemberController.getMember`, který vrací `firstName`, `lastName`, `registrationNumber`.
- **API:** Beze změny.
- **Lokalizace:** Případně doplnit popisky v `frontend/src/localization/labels.ts` (např. `finance.accountOwnerLabel`).
- **Specs:** Drobná aktualizace `openspec/specs/member-accounts/spec.md`.
