## Why

Při review aplikace 2026-04-29 byl potvrzen produkční bug v serving routingu: developer manuál na `/docs/**` není v prohlížeči zobrazitelný, i když uživatel má platnou session.

- Reprodukováno proti `https://api.klabis.otakar.io`: po přihlášení do SPA navigace na `/docs/index.html` vrací **HTTP 200 se SPA shell** místo developer manuálu (network log: jediný `GET /docs/index.html → 200`, vykreslí se React Router 404 page „Stránka nenalezena").
- Backend má `docsFilterChain` (`@Order(3)`, security matcher `/docs/**`) s redirectem nepřihlášených na `/login`, ale po session loginu request končí ve `SpaFallbackController`u (regex `(?!h2-console|docs)[^\\.]*` selhává pro paths s tečkou, např. `index.html`) a vrací SPA index.html.

Architektonický kořen: `SpaFallbackController` whitelistuje SPA cesty regexem v `@GetMapping`, přičemž ten regex narostl o negative lookaheads (`docs`, `h2-console`) a podporuje jen 1–3 úrovně zanoření bez tečky. Křehké pravidlo se rozbije při každém novém handleru. Zjednodušení na jediný princip „SPA shell jen pro 404 z předchozích handlerů" odstraňuje celou kategorii budoucích chyb.

**Out of scope (samostatný proposal):**
- Silent renew callback `/api/silent-renew.html` 404 — review jej oznámil jako související, ale pozdější diagnostika ukázala, že prefix `/api/` přidává frontendový `authorizedFetch` wrapper (viz network log `GET /api/silent-renew.html?...→404`). Není to bug serving routingu.
- Skutečné obnovení access tokenu přes refresh token (Spring AS public-client constraint, viz memory `project_oauth2_silent_renew.md`).

## What Changes

- SPA fallback se přepíše z controlleru/handleru na servlet `Filter`, který běží jako poslední v chain a vrací SPA `index.html` pouze tehdy, když žádný předchozí handler request neobsloužil (404 z DispatcherServlet/ResourceHandler).
- Vyloučené cesty (musí být obsluhovány jinými handlery a NIKDY nesmí dostat SPA shell): `/api/`, `/swagger-ui/`, `/swagger-ui.html`, `/v3/api-docs`, `/docs/`, `/oauth2/`, `/.well-known/`, `/actuator/`, `/h2-console/`, `/login`, `/logout`, `/error`, statické frontend resources (`/assets/`, soubory s extension `.js`, `.css`, `.map`, `.ico`, `.png`, `.jpg`, `.jpeg`, `.svg`, `.webp`, `.woff`, `.woff2`, `.ttf`, `.json`, `.webmanifest`, `.txt`, `.html` mimo SPA route — viz design.md).
- Filter respektuje `Accept` hlavičku — pokud klient žádá `application/json` a backend nemá handler, vrací JSON 404 (ne HTML shell), aby API klienti dostali správnou odpověď.
- Dodán smoke test, který staticky ověří, že Swagger UI, OpenAPI JSON a developer manuál (po session loginu) vrací správný obsah, zatímco SPA route (`/`, `/events`) vrací React shell.

## Capabilities

### New Capabilities

Žádné. Funkcionalita serving routingu je infrastrukturní, bez user-facing chování — patří do existujícího `non-functional-requirements`.

### Modified Capabilities

- `non-functional-requirements`: doplnit požadavek na pořadí serving handlerů a explicitní seznam cest, které musí být dostupné mimo SPA fallback (Swagger UI, OpenAPI, developer manuál, OAuth2 endpointy, silent-renew callback). Z user-facing pohledu se jedná o slib, že tyto URL jsou v běžícím systému funkční.

## Impact

- **Backend kód:** odstranit/přepsat existující SPA controller/handler na servlet `Filter` registrovaný s nejnižší prioritou; upravit Spring MVC / web security konfiguraci, aby filter běžel po `DispatcherServlet` a `ResourceHttpRequestHandler`.
- **Smoke testy:** přidat nový integration test (např. `WebContentRoutingIntegrationTest`), který volá `/swagger-ui.html`, `/v3/api-docs`, `/docs/developerManual/index.html`, `/silent-renew.html` a SPA route `/` — verifikuje Content-Type a rozeznatelný obsah.
- **Frontend:** žádná změna, ale je nutné ověřit, že `silent-renew.html` se publikuje do správné serving cesty (build pipeline `publish-frontend-resources`).
- **Dokumentace:** aktualizovat `docs/developerManual` o pořadí serving handlerů (kam jaký request padá).
- **Návazné práce:** strategie pro skutečný silent renew (viz memory `project_oauth2_silent_renew.md`) — samostatný proposal.
