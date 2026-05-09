## Why

Z review aplikace 2026-04-29 vyplynuly tři související potřeby v oblasti přihlášek na akce, které mají společný rys: **autorizační rozlišení mezi běžným členem a vedoucím akce / uživatelem s `EVENTS:REGISTRATIONS`**. Řešíme je společně, protože sdílí stejný autorizační princip, dotčené UI plochy a model citlivosti údajů:

1. **N9 — Privacy datumu přihlášení.** Současně je `registration time` viditelný všem autentizovaným uživatelům v tabulce přihlášek (spec scénář *User views registration list for an event*). Pro běžné členy je to citlivá informace (kdo byl rychlý, kdo "se zase rozhoupal na poslední chvíli") bez praktického přínosu. Vedoucí akce a uživatelé s `EVENTS:REGISTRATIONS` ho legitimně potřebují (řazení FCFS pro kapacitně omezené akce, kontrola pořadí).
2. **N10 — Řazení v tabulce přihlášek.** Tabulka aktuálně nemá popsané řazení (default je pořadí přihlášení). Uživatelé chtějí klikatelně řadit podle jména, příjmení, kategorie a — pro autorizované — i podle data přihlášení.
3. **N11 — Tisk/export seznamu pro ubytování.** Vedoucí akce potřebuje pro vyřízení ubytování seznam přihlášených s osobními údaji (jméno, příjmení, **číslo občanského průkazu, datum platnosti**, datum narození, adresa). Datový model už obsahuje `IdentityCard(cardNumber, validityDate)` na `Member` (nikoli "číslo OP", ale `identityCard`), takže není třeba měnit datový model — jen přidat autorizovaný export view. Údaje jsou citlivé (GDPR), proto musí být vidět jen vedoucí akce + `EVENTS:REGISTRATIONS`.

Společný kontext: všechny tři poznámky upravují tabulku přihlášek na detailu akce (resp. její související export view) a sdílí jediný autorizační princip „**vedoucí akce + uživatel s `EVENTS:REGISTRATIONS`** vidí víc než běžný člen". Dnes spec rozlišuje pouze druhou skupinu (autoritu); pojem "vedoucí akce" je v UI prezentován jako sloupec `coordinator` v eventu, ale jako autorizační princip teprve potřebuje formální definici.

## What Changes

### N9 — Privacy datumu přihlášení v tabulce přihlášek

- **Modifikace existujícího requirementu** *List Event Registrations* v `event-registrations`:
  - Běžný člen v tabulce vidí: jméno, příjmení, kategorie (pokud akce má kategorie). **Bez data přihlášení.**
  - Vedoucí akce (coordinator daného eventu) **a** uživatelé s `EVENTS:REGISTRATIONS` autoritou vidí navíc sloupec „čas přihlášení".
- **Detail vlastní přihlášky** (Requirement *View Own Registration*) zůstává nezměněný — uživatel svůj vlastní čas přihlášení vidí.

### N10 — Řazení v tabulce přihlášek

- **Nový požadavek** v `event-registrations`: tabulka přihlášek umožňuje řazení podle sortable sloupců:
  - Pro všechny: jméno, příjmení, kategorie (pokud akce má kategorie).
  - Pro vedoucího akce + `EVENTS:REGISTRATIONS`: navíc datum přihlášení.
- Default sort: datum přihlášení ascending (zachovává dosavadní FCFS view) — i pro běžné členy, ale jako default pořadí, ne jako viditelný sloupec.

### N11 — Export seznamu pro ubytování

- **Nový požadavek** v `event-registrations`: vedoucí akce nebo uživatel s `EVENTS:REGISTRATIONS` může vygenerovat „**Seznam pro ubytování**" se sloupci jméno, příjmení, číslo identity card (`IdentityCard.cardNumber`), datum platnosti identity card (`IdentityCard.validityDate`), datum narození (`Member.dateOfBirth`), adresa (`Member.address`).
- Backend přidá JSON endpoint `GET /api/events/{eventId}/registrations/accommodation-list`, dostupný pouze autorizovaným uživatelům. Link na endpoint se zveřejní jako affordance v detailu eventu (jen pro autorizované).
- Frontend přidá novou route, která data z endpointu načte a zobrazí v print-friendly layoutu (CSS `@media print` + `window.print()`). Uživatel si stránku vytiskne / vyexportuje do PDF přes browser print dialog. Nepřidává server-side PDF generaci v této iteraci (KISS).
- Pro členy bez vyplněného `identityCard` se v exportu zobrazí cell „neuvedeno" (nebrání to generování seznamu, jen upozorňuje na chybějící data).

### Společné: formální zavedení principu „vedoucí akce" jako autorizační role

- **Modifikace** `event-registrations` (případně `events`): vedoucí akce daného eventu (`event.coordinator`) má pro daný event stejná oprávnění jako uživatel s `EVENTS:REGISTRATIONS`, ale pouze pro „svůj" event, ne globálně. Tj. spec-level autorizační kontrola vrací `true`, pokud uživatel je `coordinator` daného eventu **NEBO** má `EVENTS:REGISTRATIONS`.
- Toto je nutná předpoklad pro N9 a N11 — bez toho nelze rozlišit "vedoucí akce" od "běžného člena".

## Capabilities

### New Capabilities

Žádné. Vše je rozšíření existujících `event-registrations` a (pro vedoucí akce) `events`.

### Modified Capabilities

- `event-registrations`:
  - *List Event Registrations* — privacy datumu přihlášení; nový sortable model
  - Nový requirement *Generate Accommodation List* (export pro ubytování) — pokrývá i viditelnost UI akce „Seznam pro ubytování" v detailu eventu
  - Formální zavedení autorizačního principu „vedoucí akce" pro registrace

## Impact

- **Backend kód:**
  - Field-level autorizace v DTO pro registration list — sloupec `registrationTime` viditelný jen pro `EVENTS:REGISTRATIONS` nebo `event.coordinator`. Implementace přes existující `@OwnerVisible` + `@OwnerId` (žádná nová anotace; viz design.md Decision 1).
  - Nový JSON endpoint pro accommodation list — detail v design.md.
- **Frontend kód:**
  - Tabulka přihlášek: skrýt sloupec na základě HAL+FORMS metadata (sloupec se nezobrazí, pokud server nevrací příslušné pole).
  - Sortable headers v tabulce přihlášek (TanStack Query + KlabisTable).
  - Nová route „Seznam pro ubytování" — načítá data z accommodation-list endpointu, render print-friendly (CSS `@media print` + `window.print()`). Accessible přes affordance v detail eventu.
- **Dokumentace:** rozšířit existující sekci `@OwnerVisible` v `backend-patterns` skill o příklad „vedoucí akce" (sibling `@OwnerId` na eventCoordinatorId).
- **GDPR:** Nová expozice citlivých údajů (číslo identity card, adresa, datum narození) — omezit přístup na coordinator + `EVENTS:REGISTRATIONS`, log accessu (out of scope této change, navrhnout follow-up).

## Open Questions

- ~~Princip "vedoucí akce" — anotace nebo SpEL?~~ — **vyřešeno (2026-04-29):** reuse existujícího `@OwnerVisible` + `@OwnerId` patternu, žádná nová anotace.
- ~~Co když event nemá nastaveného `coordinator` — kdo vidí citlivé údaje?~~ — **vyřešeno (2026-05-08):** jen uživatelé s `EVENTS:REGISTRATIONS` autoritou. Vyplývá přirozeně z `@OwnerVisible` mechanismu — když `eventCoordinatorId` je `null`, žádný uživatel se na něj nematchne.
- Forma exportu — zatím print-friendly HTML. Potřeba PDF / CSV / Excel? Návrh: do follow-up proposalu, pokud uživatelé budou chtít.
