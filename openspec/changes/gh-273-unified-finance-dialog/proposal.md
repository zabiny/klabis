# Proposal: Unified Finance Dialog (gh-273)

## Why

Po prvním nasazení finance modulu do provozu se ukázalo, že současné UX pro připsání/stržení částky na účtu člena používá dva oddělené overlays (jeden pro deposit, jeden pro charge), což vede ke zbytečnému kontextovému přepínání. Finance manageři opakovaně provádějí podobné operace na různých členech a aktuální flow vyžaduje navigaci na detail účtu člena, i když uživatel chce jen rychle zaúčtovat jednu transakci. Sloučení obou operací do jednoho dialogu s přímou dostupností ze seznamu členů zrychluje běžnou administrativní práci.

## What Changes

- **UI: Sloučený finanční dialog** — místo dvou samostatných overlays (deposit, charge) jeden dialog s tabs pro přepínání mezi "Připsání vkladu" a "Stržení částky". Dialog je soběstačná komponenta dostávající jen HAL link na member-account; balance, jméno člena a dostupné operace si načte sama.
- **UI: Akce v seznamu členů** — ikonu `PiggyBank` (navigace na účet) v seznamu členů odstranit a nahradit ikonou `Banknote`, která otevírá sloučený dialog pro rychlou transakci. Z member listu zaniká navigace na stránku finančního účtu.
- **UI: Akce na detailu člena** — tlačítko otevírající stránku účtu zachovat (navigace + label), ale změnit ikonu z `PiggyBank` na `Banknote` kvůli vizuální konzistenci v rámci financí.
- **UI: Stránka účtu člena** — dvě tlačítka (deposit, charge) nahradit jedním tlačítkem s ikonou `Banknote`, které otevírá stejný sloučený dialog.
- **Backend: nový HAL link** — `MemberAccountPostprocessor` vrátí na member-account resource link rel `accountOwner` ukazující na příslušný member resource. Dialog ho používá pro načtení jména a registrationNumber člena.
- **Graceful degradation** — pokud uživatel má autoritu jen na jednu operaci, dialog skryje tabs a zobrazí jen formulář pro dostupnou operaci. Pokud nemá ani jednu, ikona/tlačítko se nezobrazí (zachování dnešního pattern).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `member-accounts`: Mění UI prezentaci pro deposit/charge operace (sloučený dialog s tabs místo dvou overlays) a způsob navigace na finanční účet ze seznamu členů (ikona v listu → otevírá dialog, ne navigace; navigace nově jen z detailu člena). Backend: přidání HAL linku `accountOwner` na member-account representation.

## Impact

**Frontend:**
- `frontend/src/pages/finances/MemberAccountManagePage.tsx` — nahradit dvojici `HalFormButton` jedním tlačítkem otevírajícím sloučený dialog.
- `frontend/src/pages/members/MembersPage.tsx` — odstranit `PiggyBank` ikonu s navigací, přidat `Banknote` ikonu otevírající sloučený dialog.
- `frontend/src/pages/members/MemberDetailPage.tsx` — zaměnit ikonu `PiggyBank` za `Banknote` u tlačítka pro otevření účtu.
- **Nový komponent** `FinanceTransactionDialog` (umístění např. `frontend/src/components/finance/`) — soběstačná komponenta s vlastním fetch flow.
- `frontend/src/localization/labels.ts` — případné doplnění labelu pro sloučený dialog ("Vložit / Vybrat", title pro tabs).

**Backend:**
- `backend/src/main/java/com/klabis/finance/infrastructure/restapi/MemberAccountController.java` (`MemberAccountPostprocessor`) — přidat link rel `accountOwner` ukazující na members controller.

**API kontrakt:**
- Member-account response nově obsahuje `_links.accountOwner` — additive change, žádný breaking impact.

**Závislosti:**
- Frontend dialog závisí na novém HAL linku `accountOwner` z backendu. Implementační pořadí: nejdřív backend link, pak frontend dialog.

**Bez dopadu:**
- Reverse transaction flow (na stránce účtu, řádek transakce) zůstává beze změny.
- Backend deposit/charge endpointy a jejich kontrakty zůstávají beze změny.
- Autorizace: žádné nové autority. Spoléhá na existující pravidlo, že každý přihlášený člen vidí basic member info (firstName, lastName, registrationNumber).
