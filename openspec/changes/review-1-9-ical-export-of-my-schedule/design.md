## Context

Klabis aktuálně nemá žádnou integraci s externími kalendáři. Členové si musí akce přepisovat ručně. Standardní pattern je iCalendar subscribe feed (`text/calendar`, RFC 5545) — uživatel přidá URL jednou, externí kalendář si ji periodicky tahá a sám aktualizuje VEVENT záznamy.

Hlavní výzva: autentizace. Kalendářové aplikace (Google Calendar, Apple Calendar, Outlook) neumějí OAuth2 flow ani Bearer hlavičky. URL s tokenem (Personal Access Token, PAT) v query stringu je jediný realistický způsob.

Sémantika obsahu feedu odpovídá filtru „Můj rozvrh" v aplikaci (`calendar-my-schedule-filter`, archivováno 2026-05-20): sjednocení akcí, kde uživatel má aktivní registraci, a akcí, kde je event coordinator. Cíl je, aby externí kalendář ukazoval to samé, co web filter — žádná druhá definice „mého rozvrhu".

## Goals / Non-Goals

**Goals:**
- Subscribe URL `/ical/my-schedule.ics?token=...` vrací iCal feed s „Mým rozvrhem" daného uživatele (participant ∪ coordinator).
- Token je per-user, hashed v DB, regenerovatelný uživatelem.
- Feed je deterministický (UID = eventId) — re-fetch updatuje existující eventy v kalendáři, neduplikuje.
- Zrušená akce → VEVENT.STATUS:CANCELLED → externí kalendář ji označí škrtnutím / odstraní.
- Odhlášený uživatel (a zároveň ne koordinátor téže akce) → VEVENT zmizí z feedu → externí kalendář ji odstraní při dalším refreshi.
- Přidaná koordinátorská role → VEVENT se objeví ve feedu na další refresh.
- Reuse `EventScheduleQuery.findEventIdsForMemberSchedule` — žádná duplikace OR semantiky v `ical-export` modulu.

**Non-Goals:**
- Veřejný feed bez tokenu.
- iCal pro klubový kalendář (`calendar-items` — manuální položky, deadliny). Stejně jako web filter „Můj rozvrh", feed nese pouze event-date položky.
- Server-side push (CalDAV).
- Multi-user feed (admin vidí přihlášky všech členů).
- Family-member registrations.
- Deputy / zástupný koordinátor (čeká na proposal `#83`).
- Ne-UTF-8 character handling (Klabis je UTF-8 napříč).

## Decisions

### Decision 1: PAT — per-user single token, hashed v DB, opaque string

User aggregát dostane field `iCalAccessTokenHash: Optional<String>` (hashed) + `iCalAccessTokenLastSet: Optional<Instant>` (audit / displayed v UI jako „naposledy regenerováno").

Token je opaque base64url string (32 bytes random = 256 bit). Generování:
```
String raw = base64Url(SecureRandom 32 bytes);
String hash = passwordEncoder.encode(raw);  // BCrypt or Argon2
```
DB uchovává jen hash. Raw token se uživateli zobrazí **jen jednou** při generování — pokud zapomene, musí regenerovat.

**Alternative considered:**
- *Plain text token v DB* — leaky, pokud někdo pumpne DB.
- *Stateless JWT s expirací* — kalendářové klienty neumějí refresh, expirace by feed přerušila každých N dní. JWT stateless = nemožno revoke jediný token bez rotace klíčů.

### Decision 2: Token regenerace = single token per user (overwrite)

Uživatel má v daný okamžik 0 nebo 1 token. „Vygenerovat nový token" overwrite-uje hash a invaliduje předchozí URL. Žádný history / multiple tokens — KISS.

### Decision 3: URL forma — query param `?token=`

`https://api.klabis.otakar.io/ical/my-schedule.ics?token=<base64url>`

Test alternativ:
- Path param `/ical/<token>/my-schedule.ics` — některé kalendářové klienty (Google Calendar) ho přijmou bez problémů, ale je méně standardní pattern.
- Query param je univerzální. Default zvolen tato varianta.

### Decision 4: Obsah feedu = „Můj rozvrh" sjednocení, reuse `EventScheduleQuery`

Místo dvou samostatných query (registrations, coordinated events) a unionu v `ical-export` modulu se použije existující port `EventScheduleQuery.findEventIdsForMemberSchedule(memberId, from, to)` z `com.klabis.events`. Vrací `Set<EventId>` v jednom SQL query (date overlap + `coordinator_id = :id OR EXISTS (registration with member_id = :id)`).

`ical-export` modul potom:
1. `Set<EventId> ids = eventScheduleQuery.findEventIdsForMemberSchedule(member, from, to)`
2. `List<Event> events = events.findAllByIds(ids)` (nebo loop — záleží na existujícím portu)
3. Serializace VEVENT.

**Důsledek pro architekturu:** `ical-export` modul závisí na `events` (stejně jako `calendar-items`). Žádná nová logika union; změna sémantiky „Můj rozvrh" v jednom místě se projeví v obou consumerech (web + iCal).

**Determinace role v DESCRIPTION:** pokud feed potřebuje rozlišit „participant" vs „koordinátor" v textu VEVENT.DESCRIPTION, musí to udělat sám (`EventScheduleQuery` to nepředává, jen IDs). Implementace: pro každé event id zkontroluj `event.coordinatorId == memberId` (data už načtená v `events`) — žádná další DB query.

### Decision 5: Výchozí časové okno feedu — [-30 dní, +12 měsíců]

Feed nemá date range parametr (kalendářoví klienti by ho neuměli nastavit). Server vyplní výchozí okno: posledních 30 dní (členové nechtějí zmizet z kalendáře ihned po události) + 12 měsíců dopředu. Konfigurovatelné property `klabis.ical.window.past=P30D`, `klabis.ical.window.future=P12M`.

### Decision 6: iCalendar generování — manuální (žádná knihovna)

Pole jsou triviální (UID, SUMMARY, DTSTART, DTEND, LOCATION, DESCRIPTION, URL, STATUS). Knihovna `biweekly` přidá závislost a kompletní RFC 5545 podporu, kterou nepotřebujeme. Ruční StringBuilder + escape rules (CRLF, `\,`, `\n`):

```
BEGIN:VCALENDAR
VERSION:2.0
PRODID:-//Klabis//Klabis Member Portal//CS
BEGIN:VEVENT
UID:<eventId>@klabis
DTSTAMP:<now in UTC>
DTSTART;VALUE=DATE:<event date>
DTEND;VALUE=DATE:<event date + 1 day>
SUMMARY:<escaped name>
LOCATION:<escaped location>
URL:<event detail URL>
DESCRIPTION:<escaped description (incl. Role: koordinátor when applicable)>
STATUS:CANCELLED|CONFIRMED
END:VEVENT
...
END:VCALENDAR
```

Unit test pokrývá escape rules a deterministic UID.

**Alternative considered:**
- `biweekly` library — robustnější, ale zbytečná složitost pro náš subset.
- Spring template engine — overkill.

### Decision 7: Cache headers — `max-age=600`

Kalendářoví klienti typicky polluji 1× za hodinu nebo víckrát. Server-side cache 10 minut šetří DB queries; uživatel vidí změnu max po 10 min latence — akceptovatelné.

`Cache-Control: max-age=600, public, no-transform`. Žádné per-user cache (každý token má jiný feed obsah, ETag by se nehodil).

### Decision 8: Frontend — sekce v profilu uživatele

Stránka `MyProfile` (member detail self-view) má novou sekci „Kalendářový feed":
- Pokud token zatím není vygenerován: tlačítko „Vytvořit kalendářový feed" → POST regenerate, ukáže URL.
- Pokud token existuje: ukáže URL (zamaskovanou: `https://api.klabis.otakar.io/ical/my-schedule.ics?token=••••••••token1234`); button „Zobrazit celou URL" + „Kopírovat" + „Vygenerovat nový token" (s confirm dialog).
- Help text s instrukcemi pro Google Calendar, Apple Calendar, Outlook (krátké screenshot-less how-to). Zmíní, že feed obsahuje účast i koordinátorské role (konzistence s filtrem „Můj rozvrh" v hlavním kalendáři).

## Risks / Trade-offs

- **[Risk] Token leak (uživatel veřejně publikuje URL)** → Mitigation: regenerace invaliduje starou URL. Help text v UI varuje. Token v query stringu může být v server logs — server logs ale neukládáme token surface (audit log). Pokud by se ukázalo problémem, přesun na Authorization Bearer s OAuth2 flow je možný.
- **[Risk] Polling load** → Mitigation: cache 10 min + rate limit follow-up.
- **[Risk] Deterministický UID = pokud Klabis EventId změní (nemělo by se stávat), kalendář ztratí historii** → Mitigation: UUID je stabilní per-event aggregate, žádné re-create.
- **[Risk] Sémantická drift mezi web filtrem a iCal feedem** → Mitigation: oba consumeři používají `EventScheduleQuery.findEventIdsForMemberSchedule`. Změna semantiky (např. přidání deputy coordinator přes `#83`) se projeví v jediné portu a tím v obou výstupech současně.
- **[Trade-off] Žádný iCal pro klubový kalendář** — uživatelé chtějí všechny akce, ne jen vlastní účast/koordinaci? Web filter „Můj rozvrh" je výchozí OFF, takže uživatelé v hlavním kalendáři běžně vidí všechny akce. Pro iCal je „Můj rozvrh" záměrně default — kalendář externího uživatele nemá zaplevelovat klubovými akcemi, na kterých nehraje žádnou roli. Pokud bude poptávka, follow-up `my-club.ics` feed nebo parameter.

## Migration Plan

1. **DB migration:** přidat sloupce `ical_token_hash VARCHAR(255) NULL`, `ical_token_last_set TIMESTAMP NULL` na `users` tabulce.
2. **Domain + service:** User aggregát rozšířit, `IcalTokenService` (generate/regenerate/validate).
3. **REST endpoint:** `GET /ical/my-schedule.ics?token=...` (mimo `/api/` prefix, žádné OAuth2 — token je gating).
4. **REST API pro management:** `POST /api/me/ical-token/regenerate`, `GET /api/me/ical-token` (vrací `{ url: "...?token=...", lastSetAt: ... }`).
5. **Frontend:** sekce v profilu.
6. **Smoke test:** vygenerovat token, otevřít URL v prohlížeči (HTTPS), ověřit iCal output včetně koordinátorské role v DESCRIPTION, přidat do Google Calendar, ověřit zobrazení.

## Open Questions

- **`/ical/...` mimo `/api/` prefix vs. `/api/ical/...`** — kalendářové klienty potřebují URL bez auth header. `/api/` prefix v naší aplikaci je rezervovaný pro OAuth2 protected endpointy. Alternativa: `/ical/` jako vlastní top-level path. Default: vlastní top-level cesta `/ical/...`. Vyžaduje úpravu serving routingu (může se shodovat s proposalem 1.1 — SPA filter musí `/ical/` vyloučit).
- **Spring Security chain pro `/ical/...`** — vlastní filter chain, který bypassuje JWT auth a deleguje na `IcalTokenService.validate(token)`.
- **Co když user nemá žádné registrace ani koordinátorskou roli?** Feed obsahuje jen `BEGIN:VCALENDAR / END:VCALENDAR` bez VEVENT — validní empty calendar.
