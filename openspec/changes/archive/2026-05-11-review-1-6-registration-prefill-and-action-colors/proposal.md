## Why

Z review aplikace 2026-04-29 vyplynuly dvě malé UX úpravy v oblasti seznamu akcí, které lze doručit společně rychle:

1. **N2 — Předvyplnění čísla čipu při přihlašování na akci.** Aktuálně musí uživatel při každé přihlášce ručně přepisovat své SI číslo z paměti. Většina členů ho má vyplněné v profilu (`Member.siCardNumber`), takže registrační formulář by ho měl předvyplnit. Editovat lze libovolně (např. když si na konkrétní závod vypůjčuje cizí čip).
2. **K2 — Sémantické barvy action tlačítek v tabulce akcí.** Action sloupec aktuálně obsahuje neutrální tlačítka „Upravit / Publikovat / Zrušit akci / Přihlásit se / Odhlásit se z akce" — všechna stejně vypadající. Při procházení tabulky je obtížné rychle rozeznat, co dělá co. Sémantické barvy (primární zelená pro „Přihlásit / Publikovat", destruktivní červená pro „Zrušit", warning žlutá pro „Odhlásit se", neutrální pro „Upravit") výrazně zlepší orientaci.

Společný kontext: obě úpravy se týkají frontendové vrstvy (forms a table). Backend není nutno měnit pro K2; pro N2 stačí frontend načíst hodnotu z `Member.siCardNumber` (přístupné přes existující `me` link / current user resource) a předvyplnit Formik form.

## What Changes

### N2 — Předvyplnění SI čísla z profilu

- **Modifikace** *Register for Event* requirementu v `event-registrations`:
  - Když uživatel otevře registrační form a má v profilu `siCardNumber`, pole „SI číslo" je předvyplněno touto hodnotou.
  - Hodnotu lze volně přepsat — nemění to profil uživatele, jen aktuální registraci.
  - Pokud uživatel v profilu nemá `siCardNumber`, pole zůstává prázdné (jako dnes).
- **Mechanizmus:** GET endpoint pro detail registrace dostane volitelný boolean parametr `new` (default `false`). Při `new=false` (nebo neuvedeno) zachová stávající chování — 404 pokud registrace neexistuje. Při `new=true` server vrátí „defaults" pro novou registraci aktuálního uživatele na daný závod (siCardNumber předvyplněné z profilu, ostatní pole prázdná/výchozí). Affordance `registerForEvent` (nebo navazující navigační link) ukáže URL s `new=true`, takže frontend dostane prefill bez znalosti current user dat.

### K2 — Sémantické barvy action tlačítek v eventech

- **Modifikace** *Events Table Display* / *Row-Level Management Actions* v `events`:
  - Action tlačítka dostávají sémantické barevné varianty:
    - „Přihlásit se", „Publikovat" → **primary / success** (typicky zelená, primary brand color)
    - „Zrušit akci" → **destructive** (červená)
    - „Odhlásit se z akce" → **warning** (žlutá / oranžová)
    - „Upravit", „Synchronizovat" → **neutral / secondary** (zachová current look)
- **Specifikace zachycuje princip**, nikoli konkrétní hex barvy — implementace si zvolí v rámci theme tokens.

## Capabilities

### New Capabilities

Žádné.

### Modified Capabilities

- `event-registrations`:
  - *Register for Event* — přidat scénář o předvyplnění SI čísla z profilu (N2).
- `events`:
  - *Events Table Display* / *Row-Level Management Actions* — přidat scénář o vizuálním rozlišení action tlačítek (K2).

## Impact

- **Backend kód:**
  - **N2:** GET endpoint pro detail registrace přijme query parametr `new: boolean` (default false). Při `new=true` se neprovádí lookup existující registrace, ale vrátí se „defaults" payload pro aktuálního usera (siCardNumber z `Member.siCardNumber`, ostatní pole prázdná). Affordance `registerForEvent` na detailu eventu odkazuje na tento endpoint s `new=true`.
- **Frontend kód:**
  - **N2:** `EventRegistrationForm` (resp. generický HalFormDisplay flow) následuje affordance link, který vrátí prefill payload. Existující `getInitialValues()` mapping z template properties pak již obsahuje siCardNumber a Formik předvyplní pole bez další úpravy frontendu.
  - **K2:** rozšířit `KlabisTable` action button rendering o variant prop (primary / destructive / warning / neutral); HAL+FORMS template metadata mohou nést `actionType` field, který frontend mapuje na variant. Nebo: hardcoded mapping v frontendu na základě link relation name (jednodušší, méně server-side změn).
- **Lokalizace:** žádné nové labely.
- **Tests:** frontend tests pro Formik prefill a button color variants.

## Open Questions

- **Kde žije rozhodnutí o variantě (server vs. frontend)?** Tipuji frontend mapping podle link relation name (`register-for-event` → primary, `unregister-from-event` → warning, `cancel-event` → destructive, …). Server-side mapping by vyžadoval nový HAL+FORMS metadata field. Detail řeší design.md.
- **Pojmenování / tvar query parametru `new`:** zda použít `?new=true` nebo alternativní semantiku (např. `?prefill=true`, samostatný resource `/registrations/template`). Vybráno: `new=true` na stejném GET endpointu — minimalizuje povrch API a frontend si vystačí s jednou variantou affordance URL.
