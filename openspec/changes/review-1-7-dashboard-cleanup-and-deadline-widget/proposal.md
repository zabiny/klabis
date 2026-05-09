## Why

Z review aplikace 2026-04-29 vyplynuly dvě úpravy domovského dashboardu, které navzájem souvisí (oba mění layout dashboardu) a spolu vytváří lepší uživatelský zážitek členů klubu:

1. **K3 — Odstranit welcome blok.** Na dashboardu se zobrazuje nadpis „Vítejte v Klabis, [křestní jméno]" + podtitulek „Moderní systém pro správu členského klubu". V interní aplikaci pro vlastní členy klubu, kde jediný relevantní obsah je „kdy mám další závod" a „kde mi končí přihláška", je marketingový claim na první obrazovce zbytečný šum. Dashboard má začínat užitečným obsahem.
2. **N8 — Blok „Končící přihlášky tento týden".** Aktuálně dashboard ukazuje jen „Moje nadcházející akce" (3 nejbližší akce, na které je uživatel přihlášen). Členové ale chtějí být upozorněni i na **akce, na které ještě nejsou přihlášeni a kde brzy končí uzávěrka**. Aktuální stav: uživatel musí pravidelně chodit do seznamu akcí a kontrolovat to ručně. Návrh: nový widget na dashboardu „Končící přihlášky tento týden", který zobrazuje aktivní akce s deadlinem do 7 dní, na které uživatel ještě není přihlášen.

Společný kontext: oba bloky jsou na stejné stránce (`/`); uvolnění místa po welcome bloku (K3) přirozeně dává prostor novému deadline widgetu (N8). Implementačně se dotýkají stejné stránky `Dashboard.tsx` (případně `UserDashboard.tsx`).

## What Changes

### K3 — Odstranit welcome blok z dashboardu

- **Modifikace** capability `dashboard`:
  - Z home dashboardu se odstraňuje:
    - Nadpis „Vítejte v Klabis, [jméno]"
    - Podtitulek „Moderní systém pro správu členského klubu"
- Stránka začíná přímo prvním obsahovým widgetem („Moje nadcházející akce", „Končící přihlášky tento týden", existující shortcut karty „Můj profil" / „Akce" — viz Open Questions).

### N8 — Blok „Končící přihlášky tento týden"

- **Nový requirement** v capability `dashboard`:
  - Widget „Končící přihlášky tento týden" zobrazuje aktivní (status ACTIVE) akce, kde:
    - registrační deadline připadá na rozmezí dnes → +7 dní (včetně) — bere se nejbližší budoucí deadline (v souladu s proposalem 1.4 o více deadlinech)
    - uživatel **ještě není přihlášen**
  - Widget zobrazuje max 5 akcí, řazených podle deadlinu vzestupně (nejdřívější končící první).
  - Každý řádek zobrazuje: název akce, datum konání, deadline (formátovaný jako „Uzávěrka: DD. MM."), tlačítko „Přihlásit se".
  - Klik na řádek otevře detail akce (jako u Moje nadcházející akce).
  - Empty state (žádné akce odpovídající kritériím): widget se vůbec nezobrazí — neukazujeme prázdné upomínky, šetříme dashboard place.
  - Pro uživatele bez member profile se widget nezobrazí (analogicky existujícímu Upcoming Registrations widget).

### Shortcut „Zobrazit všechny" u nového widgetu

- **Konzistentně s existujícím *Shortcut From Widget to Full Events List***: pokud existuje > 5 odpovídajících akcí, na konci widgetu se zobrazí „Zobrazit všechny" → otevře events list s filtrem „deadline tento týden, kde nejsem přihlášen" (přesný filter design v design.md).

## Capabilities

### New Capabilities

Žádné.

### Modified Capabilities

- `dashboard`:
  - Odstranění welcome bloku (K3) — nový/upravený požadavek o tom, co tvoří úvodní obsah dashboardu.
  - Nový requirement *Upcoming Deadlines Widget* (N8).

## Impact

- **Backend kód:**
  - **N8:** nový endpoint nebo rozšíření existujícího events listu o filter „deadline-within-N-days AND not-registered-by-me". Implementace přes existující `EventFilter` (rozšířit o `deadlineWithin: Optional<Period>`, `notRegisteredBy: Optional<MemberId>` — flag místo `registeredBy`).
  - Alternativa: nový endpoint `/api/dashboard/upcoming-deadlines` který deleguje na events service. KISS — použít rozšíření existujícího filteru.
- **Frontend kód:**
  - **K3:** odstranit welcome heading + subtitle z `Dashboard.tsx`.
  - **N8:** nová komponenta `UpcomingDeadlinesWidget` (analogie `UpcomingRegistrationsWidget`); integrace do dashboardu.
  - Přidat loading / empty / error states.
- **Lokalizace:** nové labely („Končící přihlášky tento týden", „Uzávěrka", …).

## Open Questions

- **Layout dashboardu po odstranění welcome bloku:** mají zůstat shortcut karty („Můj profil", „Akce")? Aktuální design řeší to jako úvodní layout. Po odstranění welcome je vhodné přehodnotit — buď ponechat (shortcut k základním akcím), nebo zrušit (uživatel má hlavní menu vlevo). Default: ponechat, přemístit pod widgety. Ověřit s uživatelem.
- **Admin dashboard varianta:** memory `project_dashboard.md` zmiňuje „AdminDashboard vs UserDashboard". Má i AdminDashboard welcome blok? Pokud ano, řešit též (rozšířit scope poznámky K3). Ověřit při implementaci.
