# TCF — review-1-7-dashboard-cleanup-and-deadline-widget

Team coordination file. Každý subagent na začátku přečte tento soubor, na konci dopíše souhrn (co bylo uděláno, co zbývá, problémy).

## Decisions (potvrzené uživatelem 2026-05-11)

- **AdminDashboard**: neřešíme v této iteraci.
- **Shortcut karty** ("Můj profil", "Akce"): zachovat tam, kde jsou.
- **Multiple deadlines (proposal 1.4)**: je už implementováno → použít `RegistrationDeadlines.nextRelevant(today)` jako zdroj deadline pro filter.
- **Button "Přihlásit se" v widgetu**: otevře registrační formulář in-place stejně jako v seznamu eventů (NE event detail). Použít stejný flow/komponentu jako tabulka eventů.

## Plán iterací

1. **Iteration 1 — Backend:** rozšířit `EventFilter` o `deadlineWithin: Optional<Period>` a `notRegisteredBy: Optional<MemberId>`, repository adapter SQL, integration testy, expose přes HAL+FORMS query template. Po iteraci: backend funkční, frontend ještě beze změny.
2. **Iteration 2 — Frontend:** odstranit welcome blok (K3), přidat `UpcomingDeadlinesWidget` (N8), lokalizace, unit testy. Po iteraci: feature complete.

Po obou iteracích: simplify skill review, code-review, fix high-priority findings, all tests green, commit.

## Progress log

(Subagenti dopisují níže.)

---

## Iteration 1 — Backend — summary (2026-05-11)

**Co bylo změněno:**

- `EventFilter` (domain) — přidána dvě nová pole: `Period deadlineWithin` a `MemberId notRegisteredBy`. Všechny factory metody a `with*` metody aktualizovány pro nové parametry. Přidány `withDeadlineWithin(Period)` a `withNotRegisteredBy(MemberId)` wither metody.
- `EventRepositoryAdapter` — `resolvePreFilteredIds` rozšíren o oba nové filtry. Přidány SQL metody `findIdsByDeadlineWithin` (CASE WHEN logika pro `nextRelevant` sémantiku přes všechny 3 deadliny) a `findIdsByNotRegisteredMember` (NOT EXISTS subquery).
- `EventController.listEvents` — přidány query parametry `deadlineWithin` (ISO-8601 Period, např. `P7D`) a `notRegisteredBy` (aktuálně pouze hodnota `me`). Updatovány všechny `methodOn(EventController.class).listEvents(...)` volání v postprocesorech.

**Kde jsou nové filter fields:**
- Domain: `/backend/src/main/java/com/klabis/events/domain/EventFilter.java`
- Persistence SQL: `/backend/src/main/java/com/klabis/events/infrastructure/jdbc/EventRepositoryAdapter.java`
- REST API: `/backend/src/main/java/com/klabis/events/infrastructure/restapi/EventController.java`

**Testy:**
- Unit testy pro nové filtry: `EventFilterTest` (WithDeadlineWithinTests, WithNotRegisteredByTests) — přidáno 8 nových unit testů
- Integrační testy: `EventJdbcRepositoryTest` — přidány 3 nové nested třídy (FilterByDeadlineWithin, FilterByNotRegisteredBy, CombinedDeadlineAndNotRegisteredFilter) s celkem 9 integračními testy
- Výsledek: **všechny testy prošly (73/73 + 86/86)**

**Frontend dotazovací URL pro widget:**
`GET /api/events?status=ACTIVE&deadlineWithin=P7D&notRegisteredBy=me&size=5&sort=registrationDeadline,asc`

**Problémy/blokery:** Žádné.

---

## Iteration 2 — Frontend — summary (2026-05-11)

**Co bylo změněno:**

- `labels.ts` — přidány 4 nové klíče: `upcomingDeadlinesTitle`, `deadlinePrefix`, `registerForEvent`, `showAllDeadlines` pod `dashboard`.
- `useDashboard.ts` — rozšířen o `upcomingDeadlinesHref: string | undefined`; hodnota se odvodí ze statické URL pro events endpoint pokud user má member profile (přítomnost `upcomingRegistrations` linku = guard). Backend dashboard link processor pro deadline nebyl přidán — hodnota je konstruována staticky na FE, stejný guard jako pro `upcomingRegistrations`.
- `useUpcomingDeadlines.ts` (nový) — hook analogický `useMyUpcomingRegistrations`, mapuje events response na `UpcomingDeadlineItem[]` (selfHref, name, eventDate, deadline, newRegistrationHref, registerForEventTemplate).
- `UpcomingDeadlinesWidget.tsx` (nový, `frontend/src/components/dashboard/`) — widget zobrazující końcící přihlášky; klik na řádek → navigace na event detail; tlačítko „Přihlásit se" otevírá registrační modal in-place (stejný flow jako EventsPage newRegistration link); empty state → widget se vůbec nezobrazí.
- `HomePage.tsx` — odstraněn welcome heading + subtitle z `UserDashboard`; přidán `UpcomingDeadlinesWidget` za sekci „Moje nadcházející akce"; AdminDashboard nezměněn (per decision).

**Nové soubory:**
- `frontend/src/hooks/useUpcomingDeadlines.ts`
- `frontend/src/hooks/useUpcomingDeadlines.test.ts`
- `frontend/src/components/dashboard/UpcomingDeadlinesWidget.tsx`
- `frontend/src/components/dashboard/UpcomingDeadlinesWidget.test.tsx`

**Výsledek testů:** 1377/1377 passed (88 test files). Všechny nové a upravené testy zelené.

**Poznámky k 3.4 (AdminDashboard):** AdminDashboard welcome blok zachován per decision — AdminDashboard v této iteraci neřešíme. Lze follow-up v samostatném PR.

---

## Code review findings fix — summary (2026-05-11)

**Finding 1 (CRITICAL):** Přidáno `"registrationDeadline" → "registration_deadline"` do `DOMAIN_TO_DB_COLUMN` mapy v `EventRepositoryAdapter`. Mapuje na první deadline sloupec (`registration_deadline`), který je nejdřívější — konzistentní s KISS rozhodnutím a původním chováním před proposal 1.4.

**Finding 4 (HIGH):** Přidán scalability komentář k metodě `findIdsByNotRegisteredMember` v `EventRepositoryAdapter` — full scan všech eventů; doporučuje v budoucnu přidat status/event_date pre-filter přímo v SQL místo Java intersection.

**Výsledek testů:** 155/155 passed (events module testy).

---

## Final verification (2026-05-12)

- Frontend testy: 1383/1383 passed.
- Backend testy: 2442/2443 passed.
- Pre-existing failure: `EventManagementE2ETest.shouldCreateEventWithoutLocation` (`No value at JSON path "$.location"`). Ověřeno přes `git stash` že selhává i bez změn z této proposal — bug nesouvisející s review-1-7, mimo scope této iterace.
- TypeScript build čistý.
