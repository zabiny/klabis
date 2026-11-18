## Why

Po otevření tabu s expirovaným tokenem se na stránce zobrazí „Něco se pokazilo", i když silent renew později proběhne a další navigace už funguje. Od zavedení `HalPageGate` (#433) se každá chyba primárního HAL fetche vykresluje přes `ErrorPage`, takže se chyby obnovy tokenu dostaly do popředí. Obnova tokenu je navíc rozdělená mezi dvě místa a v několika případech se chová zbytečně pomalu nebo křehce.

## What Changes

- Příčina prvního selhání na stale tabu se napřed dohledá (diagnostické logování renew a 401 odpovědí); produkční chování lze lokálně simulovat jen částečně (viz design).
- Požadavky se neposílají s již expirovaným tokenem: `authorizedFetch` i `openapi-fetch` middleware (`api/setup.ts`) nejdřív počkají na renew a teprve pak odešlou požadavek. Logika renew + jednorázového opakování po 401 se sjednotí do jednoho modulu.
- Selhání renew není chyba stránky: místo `ErrorPage` s „Něco se pokazilo" vede na login s návratem na původní URL (`returnTo`).
- `silentRenewRetry` neopakuje definitivní odpovědi (`login_required` / „User is not authenticated"); opakuje jen timeout a síťové chyby. Zkracuje se tím čekání zhruba 15 s s overlayem před přesměrováním.
- Po úspěšném renew (`userLoaded`) se authorized dotazy v chybovém stavu refetchnou, takže stránka se z chyby sama vzpamatuje; `ErrorPage` pro chyby načtení nabídne „Zkusit znovu".
- Vlastní `automaticSilentRenew` z oidc-client-ts se sjednotí s `silentRenewRetry` (jeden guard proti souběžným `signinSilent`).

## No Behavior Change Justification

**Specs reviewed:**
- `openspec/specs/users-authentication/spec.md` — popisuje endpointy autorizačního serveru (discovery, token, UserInfo, logout), ne chování SPA při obnově tokenu; nedotčeno.
- `openspec/specs/non-functional-requirements/spec.md` — zmiňuje silent renew jen jako deployment kontext (`prompt=none`, klient `klabis-web-local`); mechanismus se nemění.
- `openspec/specs/application-navigation/spec.md` — nedotčeno.

**Why no spec update is needed:**
Žádná specifikace nepopisuje, co uživatel uvidí při expiraci tokenu. Změna opravuje chybu v interním mechanismu obnovy tokenu a nemění žádný dokumentovaný scénář ani API kontrakt. Pokud se v průběhu ukáže, že „Zkusit znovu" na `ErrorPage` je nová uživatelská funkce hodná spec scénáře, vyčlení se do samostatného spec-driven changu.

## Impact

- Frontend: `api/authorizedFetch.ts`, `api/setup.ts`, `api/klabisUserManager.ts`, `contexts/AuthContext2.tsx`, `contexts/HalRouteContext.tsx`, `pages/ErrorPage.tsx`, `hooks/useAuthorizedFetch.ts`.
- Testy: `authorizedFetch.test.ts`, `klabisUserManager.test.ts`, `tokenRenewalState.test.ts`, E2E scénář obnovy po expiraci.
- Backend se nemění (případná oprava AS session viz `fix-api-session-id-rotation`, mimo rozsah).
