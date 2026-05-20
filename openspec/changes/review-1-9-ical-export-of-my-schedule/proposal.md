## Why

Z review aplikace 2026-04-29: členové klubu chtějí mít akce, na kterých budou, viditelné v jejich osobním kalendáři (Google Calendar, Apple Calendar, Outlook, …) bez ručního přepisování dat. Aktuálně musí uživatel pro každou akci ručně vytvořit kalendářovou událost, vyplnit datum, lokaci a uložit. Pokud akci později zruší organizátor, nebo se uživatel odhlásí, externí kalendář ručně neaktualizuje.

Standardní řešení: **iCalendar feed (RFC 5545)** — Klabis vystaví URL, kterou uživatel jednou přidá do svého kalendáře, a pak se kalendář automaticky aktualizuje (typicky 1× za hodinu / den, podle klienta). Změny v Klabisu (přihlášení, odhlášení, zrušení akce, přidání role koordinátora) se promítnou bez další ruční akce uživatele.

**Sémantika feedu odpovídá filtru „Můj rozvrh" v aplikaci (proposal `calendar-my-schedule-filter`, archivováno 2026-05-20):** feed obsahuje akce, kde je uživatel buď účastníkem (aktivní registrace), nebo koordinátorem. Stejný princip „kde fyzicky budu / kde mám zodpovědnost" jako v UI.

## What Changes

### Rozšíření capability `calendar-items` o iCalendar feed

iCalendar feed je nová schopnost modulu `calendar` — žádný samostatný `ical-export` modul nevzniká. Modul `calendar` už dnes konzumuje `EventScheduleQuery` pro web filtr „Můj rozvrh"; iCal feed je druhý výstup té samé domény.

- **Subscribe URL pro „Můj rozvrh":** `GET /ical/my-schedule.ics?token=<userToken>` vrací iCalendar feed (`text/calendar`) obsahující VEVENT pro každou akci, na které uživatel buď má aktivní registraci, nebo je koordinátorem. Sjednocení obou množin (deduplicated by event id) — totéž, co `EventScheduleQuery.findEventIdsForMemberSchedule`.
- **Autentizace přes personal access token (PAT)** — kalendářové aplikace neumějí OAuth2 flow; potřebujeme dlouhožijící token v URL. Token je per-user, randomly generovaný. Lze rotate / revoke.
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
  - Token žije v **samostatné tabulce** vlastněné modulem `calendar` (viz Impact) — `User` aggregát v `common.users` se nemění.

### Out of scope

- Veřejný iCal feed pro celý klub — out of scope (pokud bude potřeba, samostatný proposal s permissions).
- iCal pro všechny `calendar-items` (klubový kalendář, deadliny, manuální položky) — out of scope. Feed pokrývá pouze stejnou množinu jako web filter „Můj rozvrh": event-date položky pro events, kde má uživatel registraci nebo koordinátorskou roli.
- Family-member registrations („akce mých dětí v mém kalendáři") — totožně jako u web filtru: out of scope, případně follow-up.
- Deputy / zástupný koordinátor — viz proposal `#83` (question stage). Když přibude, rozšíří jak web filter, tak iCal feed současně přes `EventScheduleQuery`.
- Server-side push notifikace přes WebPush — pro live updates kalendáře by stačilo, ale subscribe-feed pattern je standard a nevyžaduje speciální klient.

## Capabilities

### New Capabilities

<!-- None. iCal feed je rozšíření existující capability calendar-items. -->

### Modified Capabilities

- `calendar-items`: přidána iCalendar subscribe feed schopnost — feed URL `GET /ical/my-schedule.ics?token=...` se sémantikou „Můj rozvrh", per-user token management.
- `members` (drobně): User profile / nastavení — nová sekce „Kalendářový feed" s tlačítkem „Zobrazit URL" + „Vygenerovat nový token".

## Impact

### Změny mimo modul `calendar`

- **`events` modul — otevření `events.domain` pro public access:**
  - Modul `calendar` potřebuje plný `Event` aggregát (status pro `STATUS:CANCELLED`, coordinator pro `Role: koordinátor`). To dnes `EventData` neposkytuje a `events.domain` je modul-interní.
  - Řešení: `events.domain` (zejména interface `Events` a aggregát `Event`) se otevře pro čtení ostatním modulům — `Events.findById` / `Events.findAll` se stanou součástí veřejného API modulu `events`. Modul `calendar` je bude konzumovat přímo (stejný vztah, jaký už má přes `EventScheduleQuery`).
  - Žádné rozšíření `EventData` není potřeba — `calendar` použije `Events` interface a fetchne celý `Event`.
- **`common.ui` — `SpaFallbackFilter`:** přidat `/ical` do `EXCLUDED_PREFIXES`, aby GET na `/ical/my-schedule.ics` s `Accept: text/html` nebyl forwardován na SPA shell. Triviální jednořádková změna, **nezávislá na review-1-1** (dělá se v rámci této change).
- **Security — iCal-token autentizační strategie (filter ve `calendar.infrastructure`):** žádný samostatný filter chain. Stávající resource-server filter chain dostane nový `AuthenticationFilter` zařazený **před** `BearerTokenAuthenticationFilter`. Filter působí jen na `/ical/**`: pokud request nese query param `token`, vytvoří iCal-token `Authentication` a deleguje na vlastní `AuthenticationProvider` (validace přes token-službu modulu `calendar`); jinak nic nedělá a request propadne na standardní OAuth2 autentizaci. Mimo `/ical/**` se chování nemění — `?token=` nemůže obejít OAuth2.
  - `IcalTokenAuthenticationFilter` + `IcalTokenAuthenticationProvider` žijí v `com.klabis.calendar.infrastructure.security` — jsou to security adaptery modulu `calendar`, ne sdílené primitivy.
  - Do `common.security` se zasahuje pouze pokud chybí extension point pro přidání filtru do chainu napříč moduly — pak se zavede minimální hook. Jinak `common.security` zůstává nedotčený.

### Token storage — separátní tabulka (žádná změna `common.users`)

- `User` aggregát v `common.users` se **nemění**.
- Nová tabulka vlastněná modulem `calendar`, např. `calendar_feed_token`:
  - `user_id` (FK / reference na `users.id`) — vlastník tokenu.
  - `token_hash` — cryptographically hashed token (jako hesla).
  - `token_lookup` — non-secret prefix tokenu pro indexovaný lookup (constant-time validace, viz design).
  - `last_set_at` — kdy byl token naposledy vygenerován (zobrazeno v profilu).
  - feed details dle potřeby (např. budoucí per-feed konfigurace).
- Migrace: nová tabulka (do `V001` in-place, viz Review #1 — H2 bez perzistentních dat).
- Domain + service modulu `calendar`: aggregát/entita pro feed token, služba `generate / regenerate / validate`.

### Backend kód (modul `calendar`)

- Nový endpoint `GET /ical/my-schedule.ics` — accept query param `token`, ověří proti tabulce, vyhledá user → `MemberId` → events „Můj rozvrh" (registrations ∪ coordinator role) přes `EventScheduleQuery.findEventIdsForMemberSchedule(memberId, from, to)`, načte plné `Event` aggregáty přes `Events`, sestaví iCalendar response.
- Výchozí rozsah dat feedu: poslední 30 dní + následujících 12 měsíců (configurable). Bez date range parametru — kalendářové klienty refreshují periodicky, full window stačí.
- REST endpoint pro management tokenu (zobrazení obscured / regenerace): `GET /api/me/ical-token`, `POST /api/me/ical-token`.
- iCalendar serialization — ruční (pole jsou jednoduchá, žádná knihovna navíc).

### Frontend kód

- V profilu uživatele přidat sekci „Kalendářový feed":
  - Tlačítko „Zobrazit URL" — ukáže URL s tokenem, button „Kopírovat".
  - Tlačítko „Vygenerovat nový token" — confirm dialog → regenerate.
  - Help text: „Přidejte tuto URL do svého kalendáře (Google Calendar, Outlook, Apple Calendar…). Kalendář se bude automaticky aktualizovat, jak budete přihlašovat / odhlašovat akce, nebo když vám organizátor přidá / odebere roli koordinátora."

### Bezpečnost

- Token je jediný gating mechanismus pro `/ical/my-schedule.ics` — pokud někdo URL zachytí, vidí účast i koordinátorské role uživatele. Pro Klabis je to akceptovatelné riziko (data nejsou ultra-citlivá), ale uživatel to musí vědět.
- Audit log není potřeba (low-stakes data).
- Rate limiting — jeden uživatel / klient by neměl polling > 1× za 15 min. Resilience4j (existing infrastructure) per-token rate limit. **Out of scope této change** (přidat až pokud bude problém).

## Resolved Decisions

- **URL forma — query parameter.** `GET /ical/my-schedule.ics?token=<token>`. Varianta path param (`/ical/<token>/my-schedule.ics`) zamítnuta — query param je běžný a univerzální, autentizační strategie (filter podle přítomnosti `token` query parametru, viz Impact) na něm staví.

## Open Questions

- **Cache headers:** `Cache-Control: no-store` (live feed) vs. krátký max-age (např. 600s, sníží zátěž)? Default: `max-age=600` — kalendářoví klienti polluji obvykle 1× za hodinu, krátká cache server-side šetří.
- **Klubový čas akcí:** event má jen datum (`LocalDate`), ne čas. iCal whole-day event funguje, ale uživatelé mohou chtít vidět čas (start závodu typicky známý). Nech do iCal `DTSTART;VALUE=DATE` (whole-day) — pokud později přibude čas v Event aggregátu, iCal export se naturalně rozšíří.
