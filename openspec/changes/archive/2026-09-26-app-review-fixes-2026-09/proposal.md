## Why

Aplikační review (2026-09-26) odhalilo čtyři nezávislé mezery mezi tím, co specifikace požadují (nebo co je konzistentní s podobnými poli/vzory jinde v systému), a tím, co je skutečně implementováno: dvě autorizační mezery (menu položka a pole viditelné širšímu okruhu uživatelů, než by mělo) a dvě mezery ve funkčnosti sledování synchronizace s ORIS (stav synchronizace se nezobrazuje tam, kde by měl). Sdružujeme je do jednoho change, protože jde o menší, nezávisle ověřené opravy nalezené ve stejné review session.

## What Changes

- Zvýšit oprávnění potřebné k zobrazení položky „Disciplíny" v hlavním menu a k volání `GET /api/disciplines` z `EVENTS_READ` na `EVENTS_MANAGE`.
- Přidat obecnou podporu pro **link-based HAL-FORMS options** na backendu (odkaz na endpoint místo inline seznamu hodnot) a použít ji pro:
  - výběr disciplín (`disciplineIds`) ve formulářích typu akce (`createEventType`/`updateEventType`), místo dnešního inline seznamu,
  - výběr člena (`memberId` polí) v obecných HAL-FORMS formulářích — nahrazuje dnešní frontendový hardcode odkazu na `/api/members/options`.
- Přidat autorizační ochranu (`MEMBERS:MANAGE`) k poli „aktivní/neaktivní" v detailu člena — dnes je viditelné komukoli s přístupem k detailu, zatímco v seznamu členů je už chráněné.
- Zobrazit stav ORIS synchronizace (ikona/badge) i v seznamu akcí — dnes je viditelný jen v detailu akce.
- **BREAKING** (interní API, ne uživatelské chování): zrušit samostatnou akci „synchronizovat akci z ORIS" (`POST /api/events/{id}/sync-from-oris`) — je nahrazena obecnou akcí synchronizačního enginu, kterou uživatel bude ovládat přes nově viditelný stav synchronizace.
- Zobrazit stav ORIS synchronizace (ikona/badge) u členů — dnes chybí úplně, jak v seznamu členů, tak v detailu člena.

## Capabilities

### New Capabilities

(žádné)

### Modified Capabilities

- `disciplines`: navigace na katalog disciplín a jeho čtení vyžaduje `EVENTS:MANAGE` místo `EVENTS:READ`.
- `members`: aktivní/neaktivní stavový indikátor v detailu člena je viditelný jen uživatelům s `MEMBERS:MANAGE`.
- `events`: stav synchronizace s ORIS je dosažitelný i ze seznamu akcí (dosud jen z detailu); přímé tlačítko „Synchronizovat" v řádku seznamu je nahrazeno stavovým indikátorem synchronizace, po jehož otevření lze synchronizaci spustit — sjednoceno s detailem akce.
- `member-synchronization`: stav synchronizace s ORIS je dosažitelný i ze seznamu členů (dosud jen z detailu).

Poznámka: přechod na link-based HAL-FORMS options (vedle existujícího inline vzoru pro výběr disciplíny a člena) je čistě implementační/transportní detail bez pozorovatelného dopadu na uživatele (stejné hodnoty ve stejném selectu) — je popsán v `design.md`/`tasks.md`, ne jako spec delta. Stejně tak zrušení duplicitního `syncEventFromOris` endpointu je čistě interní (nahrazen ekvivalentní akcí synchronizačního enginu) a nemění, co uživatel vidí nebo může udělat.

## Impact

**Backend:**
- `docs/openapi/spec/events.yaml` — autorita `listDisciplines`; zrušení `POST /api/events/{id}/sync-from-oris`.
- `docs/openapi/spec/members.yaml` — autorita pole `active` v `MemberDetailsResponse`.
- `HalFormsSupport` (common/ui) — nový mechanismus pro link-based options.
- `EventTypeController`, `DisciplineController` — přepojení disciplín na link-options; komentář k autorizaci.
- `EventController` (`EventSummaryPostprocessor`, `EventAffordanceSupport`) — přidání `sync` linku do seznamu, odstranění `syncEventFromOris` affordance.
- `OrisEventController`, `OrisEventImportPort`/`Service`, `EventSyncNeedsResolutionException` — zrušení starého sync endpointu a navazujícího kódu.
- `MemberController` (`MemberSummaryPostprocessor`) — přidání `sync` linku do seznamu členů.
- Testy: `OrisEventImportServiceTest`, `EventControllerTest`, `OrisEventControllerTest` a nové testy pro link-options a nové `sync` linky.

**Frontend:**
- `KlabisFieldsFactory.tsx` (`memberIdFieldRenderer`) — odstranění hardcoded odkazu, spoléhání na `options.link` z backendu.
- `EventsPage.tsx` — již připraveno na `SyncStatusIndicator`, jen potřebuje data z backendu.
- `MembersPage.tsx`, `MemberDetailPage.tsx` — přidání `SyncStatusIndicator`.

**Riziko:** zrušení `syncEventFromOris` je technicky breaking change API kontraktu (odstranění endpointu) — žádný current frontend kód mimo `SyncStatusOverlay`/`synchronizeNow` na něj dnes nespoléhá (ověřeno v review), dopad na uživatele je nulový.
