## Context

Backend již dnes do HAL+FORMS reprezentace finančního účtu (endpoint `GET /api/members/{memberId}/account`) přidává link s relací `accountOwner`, který směřuje na detail vlastníka účtu (`GET /api/members/{memberId}`). Reprezentace člena vrací mimo jiné `firstName`, `lastName` a `registrationNumber`. Frontend tento link prozatím nevyužívá — `MemberAccountManagePage.tsx` ani `FinancesPage.tsx` identitu vlastníka uživateli nezobrazují.

Z UX hlediska to vede k riziku záměny účtů u finance managera (transakce na špatném členovi) a k matoucímu kontextu na vlastní stránce „Finance".

## Goals / Non-Goals

**Goals:**
- Frontend zobrazí v hlavičce obou stránek **jméno, příjmení a registrační číslo** majitele účtu.
- Identita majitele se získá výhradně **následováním HAL linku `accountOwner`** — žádné hardcoded URL, žádné odvozování z URL routy.
- Vlastník se načítá vlastním React Query dotazem, aby šel sdílet cache s ostatními místy, kde se vlastník už mohl načíst (např. detail člena).

**Non-Goals:**
- Žádné změny v backendu. Link `accountOwner` i `MemberController.getMember` zůstávají beze změny.
- Nezobrazujeme jiné údaje o členovi než jméno, příjmení a registrační číslo (žádné kontakty, datum narození apod. — to patří do detailu člena).
- Stránka se nesnaží sama o sobě rozhodovat, jestli jde o „vlastní" nebo „cizí" účet. Hlavička je stejná pro oba pohledy; rozdíl je pouze v tom, koho `accountOwner` link označuje.
- Neřešíme rodičovský pohled na účty dětí (mimo MVP).

## Decisions

### Identitu vlastníka načíst z `accountOwner` linku, ne přímo z URL routy

**Proč:** Klabis frontend důsledně dodržuje HATEOAS — žádné hardcoded URL. URL routa `/members/:memberId/account` se navíc do budoucna může změnit (např. `/finance/accounts/:accountId`), zatímco `accountOwner` link je stabilní kontrakt z backendu.

**Alternativa zvážena:** Vytáhnout `memberId` z route params (`useParams`) a zavolat existující detail-hook. Zamítnuto — porušuje HATEOAS princip a vázalo by frontend na konkrétní tvar URL.

### Sdílet existující hook pro načtení dat z HAL linku

**Proč:** V projektu už existují obecné hooky pro načítání HAL resource z linku (`useHalPageData`, případně utility v `src/api/hateoas.ts`). Pro tento případ stačí GET resourcu na URL z linku `accountOwner` — žádný nový mechanismus není potřeba.

**Alternativa zvážena:** Vlastní `useAccountOwner` hook. Zamítnuto jako předčasná abstrakce — jde o jeden GET dotaz na dvou místech.

### Zobrazení v hlavičce stránky, nad zůstatkem

**Proč:** Hlavička je první, co uživatel přečte. Identita majitele je sémanticky „titulek" stránky — proto patří nad zůstatek (`BalanceCard`), ne pod něj nebo do sidebaru.

**Formát:** `Jan Novák (ZBM1234)` — křestní jméno, příjmení, závorka s registračním číslem. Sjednoceno s formátem v `MembersPage` a `FinanceTransactionDialog`.

### Chování při chybějícím `accountOwner` linku

**Proč:** Obranný kód není potřeba — link je součástí backend kontraktu a jeho absenci pokrývá test backendu (`MemberAccountControllerTest`). Pokud by link přesto chyběl (např. defekt backendu nebo budoucí refaktoring), frontend prostě hlavičku s identitou nevykreslí (graceful degradation), stránka zůstane funkční.

**Alternativa zvážena:** Hodit error / skeleton donekonečna. Zamítnuto — to by udělalo stránku nepoužitelnou kvůli pouze kosmetické informaci.

## Risks / Trade-offs

- **[Riziko] Dvojí síťový request při načtení stránky** (účet + vlastník) → Mitigace: oba dotazy běží paralelně přes React Query, vlastník se cachuje a sdílí cache s detailem člena, kde už mohl být načten. Pro fast follow-up lze později přidat `?embed=accountOwner` na backendu, pokud to bude bolet.
- **[Riziko] Flicker hlavičky při pomalém načtení vlastníka** → Mitigace: vykreslit Skeleton pro identitu vlastníka samostatně, zatímco zbytek stránky se renderuje. Zůstatek je primární informace a nemá čekat na identitu.
- **[Trade-off] GDPR / vidění cizí identity:** Pohled na cizí účet má dnes pouze finance manager (autorizace `FINANCE:MANAGE`), který už dnes vidí jméno + registrační číslo v member listu a v transaction dialogu. Žádné nové úniky dat.
