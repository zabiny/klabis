## Context

Dashboard (`/`) je první obrazovka po přihlášení. Aktuální `dashboard` capability má dva requirementy: *Upcoming Registrations Widget* a *Shortcut From Widget to Full Events List*. Stránka navíc obsahuje (mimo aktuální spec) welcome heading + subtitle a shortcut karty „Můj profil" + „Akce" — ty zatím spec nezachycuje.

Z review 2026-04-29 vyplynuly dvě úpravy: odstranit welcome blok (K3), přidat widget pro končící přihlášky (N8). Implementačně sloučitelné, protože obě mění stejnou stránku.

Memory `project_dashboard.md` zmiňuje rozdíl AdminDashboard vs UserDashboard. Spec dnes hovoří jen o "home dashboard" obecně — implementace má conditional rendering podle role. Tento change proposal se týká **UserDashboard** (běžný člen). AdminDashboard řešíme až po ověření, jak vypadá (Open Question).

## Goals / Non-Goals

**Goals:**
- Welcome blok (heading + subtitle) odstraněn z UserDashboard.
- Nový widget „Končící přihlášky tento týden" zobrazený na UserDashboard.
- Backend endpoint / filter umožňující dotaz „akce s deadlinem do N dní, na které nejsem přihlášený".

**Non-Goals:**
- Redesign dashboard layoutu obecně (skip welcome → drop místa pro nový widget je natural fit; další reorg je out of scope).
- Push notifikace / e-maily o končících deadlines (samostatný proposal).
- Konfigurovatelné okno (uživatel si chce nastavit „upozornění 3 dny předem") — KISS, fixní 7 dní.
- Zahrnout zrušené akce nebo akce, které proběhly — widget je čistě "akce co mě stále zajímají".

## Decisions

### Decision 1: N8 backend — rozšířit existující `EventFilter`, ne nový endpoint

`EventFilter` už dnes podporuje různé filtry (status, organizer, dateFrom/To, registeredBy, fulltext). Přidat:
- `deadlineWithin: Optional<Period>` — přijímá `Period.ofDays(7)` (today → today+7).
- `notRegisteredBy: Optional<MemberId>` — alternativa k `registeredBy`, vrací akce, kde DANÝ člen NENÍ v registracích.

Frontend dashboard widget pak volá `GET /api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc`.

**Alternative considered:**
- *Nový dedicated endpoint `/api/dashboard/upcoming-deadlines`* — duplikuje filtering logiku; navíc bychom potřebovali HAL+FORMS specific pro něj. Rozšíření `EventFilter` je idiomatické s existujícím listingem.
- *Frontend post-filter (fetch all + filter)* — nelze; counts mohou být velké, paginace + serverside filter je správně.

### Decision 2: N8 widget — empty state se nezobrazí

Když uživatel nemá žádné končící přihlášky, widget vůbec není vidět. Důvod: prázdný widget na dashboardu je šum — neupozorňuje na nic, jen zabírá místo. UI je čistší, když se widget objevuje jen v okamžiku, kdy je relevantní.

Toto je odlišnost od existujícího *Upcoming Registrations Widget*, který má explicit empty state s CTA „Prohlédnout nadcházející akce klubu" — tam empty state má smysl, je to motivační prvek pro nového uživatele bez přihlášek. Pro deadline widget je ale „nic neutíká" pozitivní stav, ne prázdné upomínky.

### Decision 3: Vazba na proposal 1.4 (multiple deadlines)

Po deployi proposalu 1.4 bude event mít až 3 deadlines. Widget musí používat **nejbližší budoucí** deadline z rangu (`RegistrationDeadlines.nextRelevant(today)`). Backend filter `deadlineWithin` musí počítat nejbližší budoucí deadline, ne jen `deadline1`.

Implementační pořadí: tento proposal (1.7) předpokládá, že 1.4 už běží na produkci, nebo prochází implementací paralelně. Pokud 1.4 zatím není deployed, dočasně bere `deadline1` (= legacy `registrationDeadline`). Spec ale formuluje obecně „nejbližší relevantní deadline", aby přechod proběhl bez další spec úpravy.

### Decision 4: K3 — welcome blok jen z UserDashboard

Odstranění welcome heading + subtitle se týká UserDashboard. AdminDashboard zatím nezasahujeme (Open Question, ověřit ručně po deployi).

Spec change formulovat tak, že *celý dashboard začíná prvním widgetem* — nezávislé na rolích. Pokud admin dashboard má welcome, vypadne taky. Konkrétní implementace závisí na current code base (jeden component / dva components / role-conditional rendering).

## Risks / Trade-offs

- **[Risk] N8 záplava end-pointu pokud má uživatel mnoho akcí v okolí** → Mitigation: server side filter + paginace + LIMIT 5 výsledků v widgetu. Žádný issue.
- **[Risk] Widget zmizí když uživatel má 0 akcí v 7-day window — uživatel netuší, že feature existuje** → Mitigation: Decision 2 to akceptuje. Potřebujeme empty state? Pokud uživatelé budou hledat „kde je ten widget", zvážit follow-up s explicit empty state.
- **[Risk] K3 odstraní personalizaci (welcome s jménem)** → Mitigation: jméno uživatele je v topbar (button „Jan Novák [ZBM9000]") — personalizace zůstává tam, kde dává smysl.

## Migration Plan

1. **Backend (N8):** rozšířit `EventFilter` o `deadlineWithin` a `notRegisteredBy`; přidat odpovídající SQL kritéria v repository adapteru. Integration test pro nové filtry.
2. **Frontend (K3):** odstranit welcome heading + subtitle z `Dashboard.tsx` (nebo `UserDashboard.tsx`).
3. **Frontend (N8):** nová komponenta `UpcomingDeadlinesWidget` — fetch s TanStack Query, render rows, tlačítko „Přihlásit se" které deleguje na existující registration flow.
4. **Lokalizace:** nové labely.
5. **Smoke test:** uživatel se 2 končícími přihláškami v 7-day okně vidí widget; uživatel s 0 nevidí.

## Open Questions

- **Admin dashboard:** existuje? Má vlastní welcome blok? Ověřit při implementaci a případně rozšířit K3 i tam.
- **Shortcut karty „Můj profil", „Akce"** — zachovat nebo odstranit po odstranění welcome bloku? Default: zachovat (Decision: necháme jak je, neměníme rozsah).
- **„Přihlásit se" tlačítko ve widgetu** — má dělat in-place registraci (jako tabulka akcí) nebo otevřít event detail? Default: otevře event detail (registrace s kategoriemi může vyžadovat výběr — komplexní in-place není KISS).
