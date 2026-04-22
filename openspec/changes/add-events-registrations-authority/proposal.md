## Why

Dnes je oprávnění `EVENTS:MANAGE` přetížené — řídí zároveň správu událostí (vytvoření/úpravu/publikaci/zrušení akce, správu category presets, viditelnost DRAFT akcí) i přístup k přihláškám cizích členů (zobrazení a editace konkrétní přihlášky). Chceme umožnit, aby v klubu existovala samostatná role „registrační technik" — člen, který spravuje přihlášky ostatních (opravuje SI číslo, upravuje kategorii), aniž by měl práva upravovat samotné akce. Rozdělení také umožní v budoucnu udělovat tato oprávnění přes skupiny (např. zástupci koordinátora konkrétní akce).

## What Changes

- **Nové oprávnění** `EVENTS:REGISTRATIONS` (kontextové) — umožňuje držiteli zobrazit a editovat přihlášky libovolného člena. Není součástí standardních oprávnění běžného uživatele.
- **BREAKING**: Oprávnění potřebné pro GET/PUT přihlášky cizího člena se mění z `EVENTS:MANAGE` na `EVENTS:REGISTRATIONS`. Stávající držitelé `EVENTS:MANAGE` toto oprávnění automaticky neztrácí pro akce; pokud mají spravovat i cizí přihlášky, musí dostat i `EVENTS:REGISTRATIONS`.
- **Změna scope** `EVENTS:MANAGE` z kontextového na globální. Správa akcí a category presets je admin-level aktivita, která nedává smysl udělovat přes skupiny.
- **Editace vlastní přihlášky** zůstává dostupná členovi i bez `EVENTS:REGISTRATIONS` (přes `@OwnerVisible`).
- **Affordance `editRegistration` v seznamu přihlášek** — přidávat na všechny řádky (při otevřených registracích). Framework `klabisAfford` automaticky filtruje podle `@HasAuthority` + `@OwnerVisible` na cílové metodě, takže běžný člen uvidí tlačítko jen u své řádky, držitel `EVENTS:REGISTRATIONS` u všech.
- **Frontend — po úspěšném přihlášení na akci** zůstává uživatel na detailu akce; seznam přihlášek se pouze obnoví. Dosavadní klientské přesměrování na URL z `Location` headeru se ruší (stránka detailu přihlášky v aplikaci neexistuje). Backend ponechává `Location` header na nově vytvořenou registraci beze změny.
- **Dialog editace oprávnění** (`PermissionsDialog`) nabízí `EVENTS:REGISTRATIONS` jako samostatně volitelnou položku s vlastním popiskem.

## Capabilities

### New Capabilities

_žádné_

### Modified Capabilities

- `users`: rozšířit výčet platných authorities o `EVENTS:REGISTRATIONS`.
- `event-registrations`: autorizace přístupu k přihláškám cizích členů vyžaduje `EVENTS:REGISTRATIONS`; affordance `editRegistration` se zobrazuje i držiteli tohoto oprávnění u cizích přihlášek; po úspěšné registraci na akci uživatel zůstává na detailu akce.
- `member-permissions-dialog`: dialog nabízí `EVENTS:REGISTRATIONS` jako volitelné oprávnění s lokalizovaným popiskem.

## Impact

**Backend**
- `backend/src/main/java/com/klabis/common/users/Authority.java` — nová hodnota `EVENTS_REGISTRATIONS`, změna scope u `EVENTS_MANAGE`.
- `backend/src/main/java/com/klabis/events/infrastructure/restapi/EventRegistrationController.java` — přepojení `@HasAuthority` na `getRegistration`, přidání `@HasAuthority` na `editRegistration`, odstranění ruční kontroly vlastníka v `buildRegistrationItems`.
- Testy: `EventRegistrationControllerTest`, `AuthorityValidatorTest`, `JwtParamsTest`.
- **Nedotčeno**: `EventController`, `EventSummaryDto`, `CategoryPresetController` — `EVENTS:MANAGE` zde zůstává.

**Frontend**
- `frontend/src/components/members/PermissionsDialog.tsx` — nová položka v `PERMISSION_COLORS`.
- `frontend/src/localization/labels.ts` — lokalizace pro `EVENTS:REGISTRATIONS`.
- `frontend/src/pages/events/EventDetailPage.tsx` a/nebo `EventsPage.tsx` — po `registerForEvent` invalidovat query místo přesměrování.
- Regenerace `frontend/src/api/klabisApi.d.ts` (`npm run openapi`) po změně backendu.

**Data / migrace**
- Žádná migrace schématu. Oprávnění jsou ukládána jako řetězce; členové, kteří mají dnes `EVENTS:MANAGE` pro spravování přihlášek, budou potřebovat administrátorské přidání `EVENTS:REGISTRATIONS` (projekt je zatím pre-produkční, BootstrapDataLoader aktualizovat podle potřeby).

**API**
- OpenAPI popisy `getRegistration` a `editRegistration` se aktualizují — požadované oprávnění se mění na `EVENTS:REGISTRATIONS`.
- Klient, který dnes volá `GET /api/events/{eventId}/registrations/{memberId}` s tokenem nesoucím pouze `EVENTS:MANAGE`, dostane po nasazení 403, pokud nevlastní přihlášku.
