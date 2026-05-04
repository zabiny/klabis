## Context

Reprodukováno proti deploy `https://api.klabis.otakar.io`: developer manuál na `/docs/**` není dosažitelný v prohlížeči. I po úspěšném přihlášení do SPA navigace na `/docs/index.html` vrací **HTTP 200 se SPA shell** (network log: jediný `GET /docs/index.html → 200`, vykreslí se React Router 404 page „Stránka nenalezena"). Backend má sice `docsFilterChain` (`@Order(3)`, security matcher `/docs/**`) s redirectem nepřihlášených na `/login`, ale po přihlášení browser request neskončí na `ResourceHttpRequestHandler`u s docs HTML — místo toho ho přebírá `SpaFallbackController` přes `@GetMapping` regex match a vrací `index.html`.

Souvisí s tím i pozorovaná 404 chyba `GET /api/silent-renew.html?error=login_required&...` v networku prohlížeče. Tato chyba má jiný root cause (frontend `authorizedFetch` přidává prefix `/api/` před URL, takže silent renew callback se hledá na nesprávné cestě) a je řešena samostatným proposalem. Tento change se omezí na backend serving routing a developer manuál.

Současný `SpaFallbackController` (`backend/src/main/java/com/klabis/common/ui/SpaFallbackController.java`) používá širokou regex množinu:
```
"/", "/{path:(?!h2-console|docs)[^\\.]*}",
"/{path1:(?!h2-console|docs)[^\\.]*}/{path2:[^\\.]*}",
"/{path1:(?!h2-console|docs)[^\\.]*}/{path2}/{path3:[^\\.]*}"
```
Snaha vyloučit `/docs` přes negative lookahead je křehká: vyhovuje jen pro paths bez tečky a maximálně do tří úrovní zanoření. Cokoliv hlubšího nebo s tečkou (např. `/docs/index.html` — tečka v posledním segmentu) vyhovuje *vstupnímu* segmentu jen díky tomu, že regex `(?!h2-console|docs)` matchuje všechno ostatní; ovšem celý ResourceUrlProvider mechanismus a precedence matchování s static resource handlerem je nečitelná. Reálný behavior závisí na pořadí registrace handlerů, content-type negotiation a state cookie/session.

Cílem je zjednodušit pravidlo na **jediný princip**: SPA shell se vrací **POUZE pro requesty, které žádný předchozí handler neuměl obsloužit**, a NIKDY pro cesty, kde existuje vyhrazený handler (Swagger UI, OpenAPI, dev manuál, OAuth2, silent-renew, statické frontend assety, API).

## Goals / Non-Goals

**Goals:**
- `GET /docs/index.html` (po přihlášení do session-auth) vrátí HTML developer manuálu, ne SPA shell.
- `GET /swagger-ui.html`, `/swagger-ui/index.html`, `/v3/api-docs/**` zůstávají funkční (regrese-free).
- `GET /silent-renew.html` (přímý request bez prefixu) zůstává funkční — slouží jako baseline test pro filter (regrese-free).
- `GET /` a libovolná SPA route (`/events`, `/members/{id}`, …) vrací React shell pro client-side routing.
- `GET /api/foo-bar-neexistuje` s `Accept: application/json` vrací JSON 404, ne HTML SPA shell.
- Pravidlo „kdy se vrací SPA shell" je vyjádřeno jediným servlet `Filter`em (KISS) místo regexové whitelist v `@GetMapping`.

**Non-Goals:**
- Frontend silent renew bug `GET /api/silent-renew.html?... → 404` — root cause je v `authorizedFetch` (prefix `/api/`), řeší samostatný proposal.
- Skutečné obnovení access tokenu přes refresh token (Spring AS public-client constraint, viz memory `project_oauth2_silent_renew.md`) — samostatný strategický proposal.
- Přepsání `docsFilterChain` na JWT-aware autorizaci (uživatel přihlášený v SPA nemá session) — UX zlepšení doporučené v review, ale řeší se samostatným proposalem; tento change se omezuje na to, aby se developer manuál vůbec dostal k browseru po session login.
- Redesign autorizace pro `/h2-console/**` (zachovat současné chování).

## Decisions

### Decision 1: SPA serving přepsat z `@Controller` na servlet `Filter` s nejnižší prioritou

**Volba:** `OncePerRequestFilter` registrovaný jako bean s `@Order(Ordered.LOWEST_PRECEDENCE)`, který běží jako poslední v chainu. Filter zachytí response wrapper, předá request dál (`chain.doFilter`), a po zpracování:
- Pokud status je 404 (případně 405) **a** request pasuje na pravidlo „je to SPA route" (Accept obsahuje `text/html`, cesta neza­číná některým z vyhrazených prefixů), forward na `/index.html`.
- Jinak ponechá původní odpověď.

**Pravidlo „je to SPA route":**
- Vyloučené prefixy: `/api/`, `/swagger-ui/`, `/v3/api-docs`, `/docs/`, `/oauth2/`, `/.well-known/`, `/actuator/`, `/h2-console/`, `/login`, `/logout`, `/error`.
- Vyloučené přesné cesty: `/swagger-ui.html`, `/silent-renew.html`, jakýkoli soubor s extension odpovídající statickému assetu (`.js`, `.css`, `.map`, `.ico`, `.png`, `.jpg`, `.jpeg`, `.svg`, `.webp`, `.woff`, `.woff2`, `.ttf`, `.json`, `.webmanifest`, `.txt`).
- `Accept` hlavička musí obsahovat `text/html` (klient evidentně chce HTML stránku — typicky browser navigation request).

**Rationale (proč filter, ne controller):**
- Filter má jediný regex-free invariant: „SPA shell jen jako fallback po 404", což je čitelné a testovatelné.
- Controller s `@GetMapping` mapuje URL pattern *před* tím, než se vůbec pokusí o ResourceHttpRequestHandler — proto současné pravidlo závisí na pořadí a precedenci matchování. Filter naopak doslova *vidí* výsledek ostatních handlerů.
- Spring Boot už `Filter` model podporuje first-class — vyhneme se vlastním `HandlerInterceptor`um nebo overrid­ům `WebMvcConfigurer.addResourceHandlers`.

**Alternative considered:**
- *Přidat další explicitní `@GetMapping` excludes do `SpaFallbackController`* — křehké, regex jen narůstá, problém se vrací s každým novým handlerem.
- *`DispatcherServlet.setThrowExceptionIfNoHandlerFound(true)` + custom `@ControllerAdvice` na `NoHandlerFoundException`* — funguje, ale ovlivňuje i API chain (pro `/api/**` chceme JSON 404, ne forward na SPA). Filter je jednoznačnější.
- *Přesměrovat `/docs/**` mimo backend (např. nginx)* — zvyšuje composition complexity, vyžaduje nové infra change a nevyřeší to dev prostředí (Vite + bootRun).

### Decision 2: Pořadí filterchainů v Spring Security beze změny; SPA filter běží mimo security chain

SPA filter se registruje jako MVC-level filter (po Spring Security chainu), takže `docsFilterChain` (`@Order(3)`, redirectuje neautentizovaného na `/login`) i ostatní bezpečnostní vrstvy běží bez modifikace. Po úspěšné autentizaci session se `/docs/index.html` projde authorization, projde k `ResourceHttpRequestHandler`u, ten vrátí 200 HTML — SPA filter nezasahuje (status není 404).

V dnešním stavu se response 200 nikdy nedostane k `ResourceHttpRequestHandler`u, protože `SpaFallbackController` přebírá GET na `/docs/index.html` přes regex match v `@GetMapping`. Po přepisu na filter zmizí konkurence o pattern matching a request padne přirozeně na ResourceHttpRequestHandler.

### Decision 3: Smoke test nového routingu jako integration test

Přidat `WebContentRoutingIntegrationTest` (podobný stylem `MonitoringEndpointsTests`), který:
- Bez auth: `/swagger-ui.html` → 302 → `/swagger-ui/index.html` → 200 HTML (Swagger).
- Bez auth: `/v3/api-docs` → 200 JSON.
- Bez auth: `/silent-renew.html` → 200 HTML (klient OIDC silent renew).
- Bez auth: `/docs/index.html` → 302 → `/login` (security chain dělá svou práci).
- Po session login: `/docs/index.html` → 200 HTML (developer manuál, ne SPA shell — ověřit přítomností konkrétního markeru z dev manuálu, např. `<title>Klabis Developer Manual</title>`).
- Bez auth: `/` → 200 HTML (SPA shell — marker `<div id="root">`).
- Bez auth: `/events` (SPA route) → 200 HTML (SPA shell, ne 404).
- Bez auth: `/api/neexistuje-ts` s `Accept: application/json` → 404 JSON (ne HTML).

Test slouží i jako regrese-guard pro budoucí změny.

## Risks / Trade-offs

- **[Risk] Filter pořadí vs. ostatní servlet filtry** → Mitigation: explicitně `@Order(Ordered.LOWEST_PRECEDENCE)` + integration test ověří, že filter běží *po* `ResourceHttpRequestHandler`u (status už je nastaven). Pokud by `Spring Security` nebo jiný custom filter zasahoval do response status, ověřit v testu.
- **[Risk] Status 404 z `/api/**` by se mohl interpretovat jako „forward na SPA"** → Mitigation: explicitní vyloučení `/api/` prefixu v SPA route ruleu + test scénáře pro `/api/foo-bar-neexistuje` s JSON Accept.
- **[Risk] Static resource cesty bez Accept header** (`curl -I` apod.) → Mitigation: SPA filter vyžaduje `Accept: text/html` přítomný v requestu — non-browser klient bez tohoto headeru dostane vždy raw 404.
- **[Trade-off] Ztrácíme kompletní seznam SPA route přímo v kódu** → Mitigation: SPA route už dnes neudržujeme v backendu (React Router je single source), filter je jen passthrough; tím je seznam vyloučených cest jediný regulérní mass v kódu.
- **[Risk] Test `WebContentRoutingIntegrationTest` po session loginu** vyžaduje stub session — Mitigation: použít `@AutoConfigureMockMvc` s `MockMvc` + `httpBasic`/`formLogin` test helper z Spring Security Test, případně `TestRestTemplate` s session cookie.

## Migration Plan

1. **Implementace:** Vytvořit `SpaFallbackFilter` (component, `OncePerRequestFilter`), zaregistrovat jako bean s `@Order(Ordered.LOWEST_PRECEDENCE)`. Smazat `SpaFallbackController`. Ponechat `FrontendController` (kratká `@GetMapping("/auth/callback")` cesta) — řeší se v rámci OAuth2 flow, nepatří k SPA fallbacku.
2. **Test:** Přidat `WebContentRoutingIntegrationTest` (acceptance scénáře z Decision 3).
3. **Manuální ověření:** Po deployi proti `https://api.klabis.otakar.io`:
   - Login do SPA → otevřít `/docs/index.html` → musí se zobrazit developer manuál (nikoli SPA 404).
   - Otevřít `/swagger-ui.html` → Swagger UI.
   - V SPA prohlížeči nechat běžet 5+ min, sledovat console — silent renew error 404 by se neměl objevit.
4. **Rollback:** revert commit; serving routing se vrátí ke starému `SpaFallbackController`u.

## Open Questions

- **Po fixu: zůstane developer manuál stále chráněn jen session-auth?** Ano (out of scope). Pokud se z review ukáže, že JWT-přihlášení uživatelé chtějí docs bez znovu-loginu, vznikne samostatný proposal pro JWT-aware `docsFilterChain`.
- **Existují další statické resources v `backend/src/main/resources/static/`, které review nezachytil a SPA filter by mohl pohltit?** Ověřit `ls backend/src/main/resources/static/` před implementací — vyloučené prefixy/extensions musí pokrýt všechno tam přítomné.
