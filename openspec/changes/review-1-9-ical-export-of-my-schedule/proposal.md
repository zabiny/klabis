## Why

Z review aplikace 2026-04-29: členové klubu chtějí mít akce, na kterých budou, viditelné v jejich osobním kalendáři (Google Calendar, Apple Calendar, Outlook, …) bez ručního přepisování dat. Aktuálně musí uživatel pro každou akci ručně vytvořit kalendářovou událost, vyplnit datum, lokaci a uložit. Pokud akci později zruší organizátor, nebo se uživatel odhlásí, externí kalendář ručně neaktualizuje.

Standardní řešení: **iCalendar feed (RFC 5545)** — Klabis vystaví URL, kterou uživatel jednou přidá do svého kalendáře, a pak se kalendář automaticky aktualizuje (typicky 1× za hodinu / den, podle klienta). Změny v Klabisu (přihlášení, odhlášení, zrušení akce, přidání role koordinátora) se promítnou bez další ruční akce uživatele.

**Sémantika feedu odpovídá filtru „Můj rozvrh" v aplikaci (proposal `calendar-my-schedule-filter`, archivováno 2026-05-20):** feed obsahuje akce, kde je uživatel buď účastníkem (aktivní registrace), nebo koordinátorem. Stejný princip „kde fyzicky budu / kde mám zodpovědnost" jako v UI.

## What Changes

### Nová capability `ical-export`

- **Subscribe URL pro „Můj rozvrh":** `GET /ical/my-schedule.ics?token=<userToken>` vrací iCalendar feed (`text/calendar`) obsahující VEVENT pro každou akci, na které uživatel buď má aktivní registraci, nebo je koordinátorem. Sjednocení obou množin (deduplicated by event id) — totéž, co `EventScheduleQuery.findEventIdsForMemberSchedule` v backendu.
- **Autentizace přes personal access token (PAT)** — kalendářové aplikace neumějí OAuth2 flow; potřebujeme dlouhožijící token v URL. Token je per-user, randomly generovaný, uchovávaný v profilu uživatele. Lze rotate / revoke.
- **Mapování:**
  - VEVENT.UID = Klabis EventId (deterministické, aby update existujícího eventu nezdvojil event v kalendáři).
  - VEVENT.SUMMARY = event.name.
  - VEVENT.DTSTART = event.eventDate (whole-day event nebo s časem podle dat — Klabis dnes má jen datum, takže whole-day).
  - VEVENT.LOCATION = event.location (pokud nastaveno).
  - VEVENT.DESCRIPTION = `Pořadatel: <organizer>\n\nDetail v Klabisu: <event detail URL>` + (pokud Klabis má webovou URL akce) `Web akce: <eventWebUrl>` + (pokud uživatel je koordinátorem) `Role: koordinátor`.
  - VEVENT.URL = Klabis event detail URL.
  - VEVENT.STATUS = `CANCELLED` pokud event status = CANCELLED; jinak default (CONFIRMED).
- **Klientské použití:** uživatel jde do svého profilu / nastavení, klikne „Zobrazit subscribe URL pro kalendář", zkopíruje URL, přidá do svého kalendáře. URL obsahuje token; uživatel ji nesmí veřejně sdílet.
- **Token management:**
  - Token se generuje jednou (lazy) — při prvním kliknutí na „Vytvořit kalendářový feed" v profilu.
  - „Vygenerovat nový token" akce — invaliduje starou URL (přestane fungovat), vytvoří novou. Použije se, pokud uživatel URL omylem zveřejnil.
  - Tokeny jsou stored hashed v DB (jako hesla — viz `members` spec o cryptographic hashing).

### Out of scope

- Veřejný iCal feed pro celý klub — out of scope (pokud bude potřeba, samostatný proposal s permissions).
- iCal pro všechny `calendar-items` (klubový kalendář, deadliny, manuální položky) — out of scope. Feed pokrývá pouze stejnou množinu jako web filter „Můj rozvrh": event-date položky pro events, kde má uživatel registraci nebo koordinátorskou roli.
- Family-member registrations („akce mých dětí v mém kalendáři") — totožně jako u web filtru: out of scope, případně follow-up.
- Deputy / zástupný koordinátor — viz proposal `#83` (question stage). Když přibude, rozšíří jak web filter, tak iCal feed současně přes `EventScheduleQuery`.
- Server-side push notifikace přes WebPush — pro live updates kalendáře by stačilo, ale subscribe-feed pattern je standard a nevyžaduje speciální klient.

## Capabilities

### New Capabilities

- `ical-export` — subscribe URL feed pro „Můj rozvrh" ve formátu iCalendar.

### Modified Capabilities

- `members` (drobně):
  - User profile / nastavení: nová sekce „Kalendářový feed" s tlačítkem „Zobrazit URL" + „Vygenerovat nový token".
- `users`:
  - Modifikace user aggregátu o personal access token field (cryptographically hashed).

## Impact

- **Backend kód:**
  - Nový endpoint `GET /ical/my-schedule.ics` — accept query param `token`, ověří proti DB, vyhledá user → events „Můj rozvrh" (registrations ∪ coordinator role) přes existující `EventScheduleQuery.findEventIdsForMemberSchedule(memberId, from, to)`, sestaví iCalendar response.
  - Výchozí rozsah dat feedu: poslední 30 dní + následujících 12 měsíců (configurable). Bez date range parametru — kalendářové klienty refreshují periodicky, full window stačí.
  - User aggregát rozšířit o `iCalToken` (hashed string + token preview pro UI):
    - Migration: nový sloupec.
    - Service: `generateIcalToken(userId)` (idempotent or rotates), `validateIcalToken(rawToken) -> Optional<UserId>`.
  - REST endpoint pro management tokenu (zobrazení obscured / regenerace): `GET /api/me/ical-token`, `POST /api/me/ical-token/regenerate`.
  - iCalendar serialization (existující knihovna `biweekly` nebo manuálně — pole jsou jednoduchá, manuál = méně závislostí).
- **Frontend kód:**
  - V profilu uživatele přidat sekci „Kalendářový feed":
    - Tlačítko „Zobrazit URL" — ukáže URL s tokenem, button „Kopírovat".
    - Tlačítko „Vygenerovat nový token" — confirm dialog → regenerate.
    - Help text: „Přidejte tuto URL do svého kalendáře (Google Calendar, Outlook, Apple Calendar…). Kalendář se bude automaticky aktualizovat, jak budete přihlašovat / odhlašovat akce, nebo když vám organizátor přidá / odebere roli koordinátora."
- **Reused infrastructure:**
  - `EventScheduleQuery` (port v `com.klabis.events`, dnes konzumovaný `calendar` modulem pro web filter „Můj rozvrh") — iCal modul ho použije se stejnou semantikou. Žádný nový query port, žádná duplicitní logika union(participant, coordinator).
- **Bezpečnost:**
  - Token je jediný gating mechanismus pro `/ical/my-schedule.ics` — pokud někdo URL zachytí, vidí účast i koordinátorské role uživatele. Pro Klabis je to akceptovatelné riziko (data nejsou ultra-citlivá), ale uživatel to musí vědět.
  - Audit log není potřeba (low-stakes data).
  - Rate limiting — jeden uživatel / klient by neměl polling > 1× za 15 min. Resilience4j (existing infrastructure) per-token rate limit. **Out of scope této change** (přidat až pokud bude problém).

## Open Questions

- **URL forma:** `/ical/my-schedule.ics?token=<token>` vs. `/ical/<token>/my-schedule.ics` — některé kalendářové klienty mají potíže s query stringy. Test obojího před deployem. Default: query param (běžný a flexibilní).
- **Cache headers:** `Cache-Control: no-store` (live feed) vs. krátký max-age (např. 600s, sníží zátěž)? Default: `max-age=600` — kalendářoví klienti polluji obvykle 1× za hodinu, krátká cache server-side šetří.
- **Klubový čas akcí:** event má jen datum (`LocalDate`), ne čas. iCal whole-day event funguje, ale uživatelé mohou chtít vidět čas (start závodu typicky známý). Nech do iCal `DTSTART;VALUE=DATE` (whole-day) — pokud později přibude čas v Event aggregátu, iCal export se naturalně rozšíří.
