## Why

Z review aplikace 2026-04-29 zbývá několik samostatných drobných úprav, které dohromady tvoří „nice-to-have" závěrečný balíček. Sloučím je do jednoho proposalu, protože každá je individuálně příliš malá na samostatný change a všechny jsou self-contained:

1. **N1 — Změna hesla pro přihlášeného uživatele.** Aktuálně Klabis umožňuje pouze počáteční nastavení hesla (přes time-limited token v e-mailu) a reset hesla zapomenutým členem (přes „Zapomenuté heslo" flow s e-mailem). Přihlášený uživatel ale **nemůže** změnit své vlastní heslo z UI — musí použít zapomenuté heslo flow a čekat na e-mail. To je nepříjemné UX. Návrh: tlačítko „Změnit heslo" v profilu uživatele, dialog s poli „Současné heslo" / „Nové heslo" / „Potvrzení nového hesla", validace stejných pravidel jako počáteční nastavení (Password Complexity Validation) + ověření současného hesla.
2. **N4 — Filtr akcí podle roku.** Aktuální time window selector v `events` má jen Budoucí / Proběhlé / Vše. Pro hledání starší akce („kdy se konala Železná Ruda 2024?") musí uživatel přepnout na Vše, scrollovat dolů. Návrh: přidat dropdown „Rok" s hodnotami za posledních 10 let (a budoucí 2 roky). Při výběru roku se time window přepne na Vše a filter dateFrom/dateTo se nastaví na 1.1.YYYY – 31.12.YYYY.
3. **K1 — Přejmenovat label „Koordinátor" na „Vedoucí" v UI akcí.** UI label `Koordinátor` se v klubovém kontextu (orientační běh) používá nestandardně — pořadatelé typicky říkají „vedoucí akce". Změna je čistě UI/lokalizace; backend field `eventCoordinatorId` se nepřejmenovává (zachová ORIS mapování i internou konzistenci kódu).

## What Changes

### N1 — Změna hesla z profilu

- **Modifikace** capability `users`:
  - Nový requirement *Change Password While Authenticated*: přihlášený uživatel může změnit heslo z profilu zadáním současného hesla + nového hesla.
  - Validace stejné jako *Password Complexity Validation*.
  - Ověření současného hesla — pokud se neshoduje, zobrazit chybu (anti-CSRF protection).
  - Po úspěšné změně: žádné okamžité odhlášení (uživatel zůstává přihlášen v current session).
- **Backend:** nový endpoint `POST /api/me/password-change` s body `{ currentPassword, newPassword }`.
- **Frontend:** tlačítko „Změnit heslo" v `MyProfile`, dialog s 3 poli (current, new, confirm).

### N4 — Filtr akcí podle roku

- **Modifikace** *Events Table View / Filter Bar* v `events`:
  - Filter bar dostane nový dropdown „Rok" s hodnotami např. 2018–2027 (dynamický range).
  - Default = current year `Vše` neaktivní.
  - Při výběru roku: `dateFrom = YYYY-01-01`, `dateTo = YYYY-12-31`, time window přepnut na „Vše" (jinak by se filtry mohly bít).
  - Při zrušení (volba „—"): `dateFrom`/`dateTo` reset, time window zpátky na default.

### K1 — Rename UI label „Koordinátor" → „Vedoucí"

- **Modifikace** `events` (UI labely jen):
  - Sloupec v tabulce akcí: header z „Koordinátor" → „Vedoucí".
  - Filter bar: filter label z „Koordinátor" → „Vedoucí".
  - Detail akce: sekce „Koordinátor" → „Vedoucí".
  - Form fields: label „Koordinátor" → „Vedoucí".
  - `src/localization/labels.ts`: záznam pro coordinator přejmenovat.
- **Backend / API:** **žádná změna**. Field `eventCoordinatorId` v API zůstává; jen UI label se mění.

## Capabilities

### New Capabilities

Žádné.

### Modified Capabilities

- `users` — nový requirement *Change Password While Authenticated* (N1).
- `events` — *Events Table View / Filter Bar* + filter podle roku (N4); UI label rename "Koordinátor" → "Vedoucí" (K1, čistě prezentační).

## Impact

- **Backend kód:**
  - **N1:** nový endpoint v users module: `POST /api/me/password-change`. Service ověří current password (přes existing `PasswordEncoder.matches`), spustí `PasswordValidator` na newPassword, uloží nový hash. Audit log entry.
  - **N4:** žádné backend změny — date range filter už `EventFilter` podporuje (`dateFrom`/`dateTo`).
  - **K1:** žádné backend změny.
- **Frontend kód:**
  - **N1:** `MyProfile` rozšířit o sekci „Změna hesla" s dialog form (3 pole + submit). Frontend validation pro complexity (existuje pro setup form, reuse).
  - **N4:** `EventsListPage` filter bar — nový dropdown „Rok"; mapping na `dateFrom`/`dateTo` query params.
  - **K1:** přejmenovat label v `localization/labels.ts` — single change.
- **Lokalizace:** rozšířit labels o nové texty.
- **Tests:** unit + integration testy podle vertikálních slices.
