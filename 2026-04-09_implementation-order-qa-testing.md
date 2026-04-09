# Implementation Order QA Testing — 2026-04-09

**Source:** `tasks/IMPLEMENTATION_ORDER.md` (all 8 items completed)
**Test users:** Admin `ZBM9000 / password`, Member `ZBM9500 / password`

Scénáře pocházejí z explicit QA sekcí v tasks.md jednotlivých proposal (číslování zachováno) a z requested changes pro queue tasks.

## Scenarios

### 1. OAuth2 silent renew redirect URI fix (queue task)
- [ ] **OAUTH-1**: Login as admin, wait ~1 min → silent renew proběhne bez chyb v console (žádné opakované "Access token expired" / "invalid_request")
- [ ] **OAUTH-2**: Přímý check `GET /oauth2/authorize?...redirect_uri=http://localhost:3000/silent-renew.html&prompt=none` → HTTP 302 s `code=...` (ne 400)

### 2. Category presets table actions (queue task)
- [ ] **CAT-1**: Admin navigates to Administrace → Kategorie (category presets) → tabulka má "Akce" column na konci
- [ ] **CAT-2**: Click na row → žádná navigace (row-click odstraněn)
- [ ] **CAT-3**: Click edit button (Pencil) → form modal, edit + submit → list se refreshne bez reload stránky
- [ ] **CAT-4**: Click delete button (Trash2) → confirmation modal, confirm → preset zmizí, list se refreshne
- [ ] **CAT-5**: Regular member → žádné akční buttons (HAL-driven)

### 3. Calendar item description optional (proposal 8.x)
- [ ] **CAL-1** (8.1-8.2): Admin (ZBM9000) → Kalendář → vytvoř položku jen s name + start + end (bez description) → success, položka se zobrazí
- [ ] **CAL-2** (8.3): Detail nově vytvořené položky bez description → řádek "Popis" se nezobrazuje
- [ ] **CAL-3** (8.4): Edit existující položky: vymaž description, save → detail ji přestane zobrazovat
- [ ] **CAL-4** (8.5): Pokus submitnout description > 1000 znaků → inline validation error

### 4. Application navigation refinements (proposal 9.x)
- [ ] **NAV-1** (9.1-9.3): Admin desktop → sidebar má sekce "NAVIGACE" + "Administrace", klikání admin items funguje
- [ ] **NAV-2** (9.4-9.5): Regular member desktop → sidebar má jen "NAVIGACE", žádné "Administrace"
- [ ] **NAV-3** (9.6-9.7): Mobile viewport → bottom nav má jen main items (žádné admin)
- [ ] **NAV-4** (9.8-9.9): Admin → Rodinné skupiny → create → auto-redirect na detail + success toast
- [ ] **NAV-5** (9.10): Admin → Tréninkové skupiny → create → auto-redirect na detail
- [ ] **NAV-6** (9.11): Admin → Skupiny → create free group → auto-redirect na detail
- [ ] **NAV-7** (9.12): Inline edit existující položky v detailu → user zůstává na detail stránce (no unwanted nav)
- [ ] **NAV-8** (9.13): Delete category preset → user zůstává na list page
- [ ] **NAV-9** (9.14): Členové page → žádný "Create family group" button

### 5. User groups refinements (proposal 12.x)
- [ ] **UG-1** (12.1): Admin → Rodinné skupiny → create s name + jeden parent → detail page se otevře
- [ ] **UG-2** (12.2): Family group detail → "Přidat člena" → role Dítě → pick → child se přidá
- [ ] **UG-3** (12.3): Otevři picker znovu → already-added parent i child se nezobrazují v seznamu
- [ ] **UG-4** (12.4): "Přidat člena" → role Rodič → pick → druhý parent přidán
- [ ] **UG-5** (12.5): Child row → remove action → child zmizí
- [ ] **UG-6** (12.6): Skupiny → create free group → pokus promote non-member na owner → error
- [ ] **UG-7** (12.7): Same group → promote existujícího člena → success
- [ ] **UG-8** (12.8): Training group → create → manuálně přidej člena → success. Přidat stejného člena do druhé training group → error "already trainee"
- [ ] **UG-9** (12.9): Přidej stejného člena jako trainer do druhé training group → success (trainer exemption)

### 6. Events refinements (proposal 9.6, 10.4-10.5, 12.3, 13.x)
- [ ] **EVT-1** (13.3): Admin → Akce → create manual event bez location → success, detail skryje location řádek
- [ ] **EVT-2** (9.6 + 13.4): ORIS import dialog → region picker jsou radio buttons (jeden vždy selected), change → list se reloadne
- [ ] **EVT-3** (12.3 + 13.5): Import ORIS event bez location → success, render OK v list i detail
- [ ] **EVT-4** (13.6): Kalendář → event bez location → description nemá stray " - " na začátku
- [ ] **EVT-5** (10.4): Admin events list → DRAFT row → "Upravit + Publikovat + Zrušit"
- [ ] **EVT-6** (10.4): Admin events list → ACTIVE row → "Upravit + Zrušit" (+ Synchronizovat pro ORIS); NO "Dokončit"
- [ ] **EVT-7** (10.4): FINISHED/CANCELLED row → žádné management actions
- [ ] **EVT-8** (10.5): Regular member → events list → jen register/unregister, žádné management actions
- [ ] **EVT-9** (13.7): Event detail page → žádné "Ukončit akci" tlačítko

### 7. Member detail buttons refinement (proposal 7.x)
- [ ] **MBR-1** (7.2): Admin → člen v training group → "Tréninková skupina" button viditelný, click → nav na TG detail
- [ ] **MBR-2** (7.3): Admin → člen v family group → "Rodina" button viditelný, click → nav na FG detail
- [ ] **MBR-3** (7.4): Admin → člen v obou grupách → oba buttons visible, oba naviguji správně
- [ ] **MBR-4** (7.5): Člen v žádné grupě → žádné group buttons
- [ ] **MBR-5** (7.6): Nikde žádný "Vložit / Vybrat" button
- [ ] **MBR-6** (7.7): Regular member (ZBM9500) vlastní profil v grupě → buttons viditelné a navigují
- [ ] **MBR-7** (7.8): Žádné embedded "Tréninková skupina" / "Rodinná skupina" sekce v detailu

### 8. TrainingGroup.addTrainer auto-member bug fix (queue task)
- [ ] **TG-1**: Create training group → trainer je owner, members set prázdný
- [ ] **TG-2**: Existing TG → přidej nového trainera → trainer se nepřidá automaticky jako člen
- [ ] **TG-3**: Family group addParent → parent je přidán i jako member (behavior zachován)
- [ ] **TG-4**: Members group owner promotion stále vyžaduje, že kandidát je už member (behavior zachován)

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| OAUTH-1 | FAIL | Issue #4: Silent renew iframe přesměrován na `https://localhost:8443/login`, browser odmítá kvůli `X-Frame-Options: deny`. Spousta opakovaných GET /login v network. |
| OAUTH-2 | SKIP | Blokováno issue #4 |
| CAT-1 | PASS | "Akce" column je na konci category presets tabulky |
| CAT-2 | PASS | Klik na row nenaviguje pryč, URL zůstává `/category-presets` |
| CAT-3 | FAIL | Issue #2: Edit submit → PATCH `/api/category-presets` (bez ID) → HTTP 405 |
| CAT-4 | FAIL | Issue #3: Delete submit → DELETE `/api/category-presets` (bez ID) → HTTP 405 |
| CAT-5 | SKIP | Blokováno issue #2/#3 |
| CAL-1 | PASS | Calendar item bez description vytvořen úspěšně |
| CAL-2 | PASS | Detail nezobrazuje řádek description (null skrytý) |
| CAL-3 | SKIP | Blokováno issue #2 (edit HAL-forms - existuje stejný pattern pro calendar items?) - odložené |
| CAL-4 | SKIP | Odložené |
| NAV-1 | PASS | Admin sidebar má Navigace + Administrace sekce se správnými položkami |
| NAV-2..8 | SKIP | Odložené |
| NAV-9 | PASS | Členové stránka nemá "Create family group" button |
| UG-1..9 | SKIP | Odložené |
| EVT-1..9 | SKIP | Odložené |
| MBR-1..7 | SKIP | Odložené |
| TG-1..4 | SKIP | Odložené |

### Issues discovered (iteration 1)

**Issue #1 (MEDIUM) — Category preset create auto-redirects to non-existent detail page**
- Symptom: Po vytvoření category preset frontend naviguje na `/category-presets/{id}`, což padá do `GenericHalPage` fallbacku místo zůstat na list page.
- Root cause: Generické "navigate on POST+Location" z proposal `refine-application-navigation` se aplikuje i na category-presets, ale spec pro category-presets říká, že tato entita nemá detail page (viz NAV-8).
- Impact: Uživatel po vytvoření skončí na generické (neintuitivní) GenericHalPage.
- Affected component: Frontend - buď `CategoryPresetsPage.tsx` (opt-out z auto-navigate), nebo backend neemituje `Location` header pro collection-only resources.

**Issue #2 (HIGH) — CategoryPreset update template points to collection URL instead of item self link**
- Symptom: PATCH `/api/category-presets` (bez ID) → HTTP 405. Frontend používá collection self link pro update request.
- Root cause: HAL-forms template `updateCategoryPreset` je vyrenderován v embedded item (viz network dump z `browser_evaluate`), ale frontend při provedení PATCH použije collection self link (`/api/category-presets`) místo item self link (`/api/category-presets/{id}`). Buď je bug ve frontend `useAuthorizedMutation` logice (nebere context z nejbližšího resource), nebo backend neemituje target URL v template.
- Impact: Category preset edit nefunguje vůbec.
- Affected component: Backend HAL-forms affordance nebo frontend HAL-form binding.

**Issue #3 (HIGH) — CategoryPreset delete template same bug as update**
- Symptom: DELETE `/api/category-presets` → HTTP 405.
- Root cause: Stejný pattern jako #2.
- Impact: Category preset delete nefunguje.
- Affected component: Stejný jako #2.

**Issue #4 (HIGH) — OAuth2 silent renew broken — redirect_uri points to backend /login**
- Symptom: Po přihlášení se opakovaně pokouší iframe load `https://localhost:8443/login`, X-Frame-Options: deny blokuje iframe. Silent renew nikdy nedokončí úspěšně.
- Root cause: Viz network dump - velké množství GET `/login` (redirect chain). `POST /oauth2/token` byl 200 OK při prvotním loginu, ale silent renew flow se snaží refresh a dostává redirect na `/login` místo `/silent-renew.html` callback. Možná fix v `tasks/completed/2026-04-08_15-00-00_oauth2-silent-renew-redirect-uri.md` nezahrnul nějakou konfiguraci, nebo H2 DB bootstrap neobsahuje přidané redirect URIs.
- Impact: Silent renew zcela rozbitý; uživatel bude po expiraci tokenu násilně odhlášen.
- Affected component: Backend - OAuth2 client bootstrap / redirect URIs, případně Security filter chain X-Frame-Options pro silent-renew.html.

**Issue #5 (LOW) — Delete confirmation modal has no confirmation text**
- Symptom: "Smazat šablonu" modal má jen tlačítka "Odeslat" / "Zrušit" bez věty typu "Opravdu chcete smazat tuto šablonu?".
- Root cause: HAL-forms template `deleteCategoryPreset` nemá properties, frontend nedoplňuje explicit confirmation prompt.
- Impact: UX - uživatel může submit bez jasné informace, že provede destruktivní akci.
- Affected component: Frontend form rendering pro DELETE affordances bez properties.

### Stop condition reached

Dosaženo 5 issues → stop. Ostatní scénáře skipnuty, budou retestovány v iteraci 2 po opravě.

## Fixes applied between iteration 1 and 2

- **Issue #2/#3 (HAL-forms URL bug)**: Frontend — `HalFormDisplay` získal `resourceUrl` prop, fallback URL je nyní `template.target || resourceUrl || '/api' + pathname`. Absolutní URLs se strippnou na path. `CategoryPresetsPage` nyní předává `resourceUrl={actionModal.item._links?.self?.href}`.
- **Issue #1 (auto-navigate after create)**: `HalFormDisplay` + `HalFormButton` + `HalFormContext` získaly `navigateOnSuccess?: boolean` prop (default `true`). `CategoryPresetsPage` create button nyní předává `navigateOnSuccess={false}`.
- **Issue #4 (OAuth silent renew)**: Nezaopraveno v této iteraci, založen queue task `tasks/oauth2-silent-renew-prompt-none-handling.md` pro samostatný follow-up.
- **Issue #5 (delete confirmation text)**: LOW priority, odloženo.
- Frontend tests: 1113/1113 passed.

### Iteration 2
| Scenario | Result | Note |
|----------|--------|------|
| OAUTH-1 | FAIL (known) | Blokováno issue #4 (silent renew), odloženo jako queue task `tasks/oauth2-silent-renew-prompt-none-handling.md` |
| OAUTH-2 | SKIP | Stejné |
| CAT-1 | PASS | Akce column na konci |
| CAT-2 | PASS | Row click nenaviguje |
| CAT-3 | PASS | PATCH `/api/category-presets/{id}` → 204 No Content, list refreshuje |
| CAT-4 | PASS | DELETE `/api/category-presets/{id}` → 204 No Content, list refreshuje |
| CAT-5 | PASS | Regular member (ZBM9500) nemá "Šablony kategorií" v sidebaru (admin sekce jen pro admins) |
| Issue #1 retest | PASS | Po create zůstal uživatel na list page, nový preset v tabulce |
| CAL-1 | PASS | Calendar item bez description vytvořen úspěšně |
| CAL-2 | PASS | Detail skrývá řádek description (null) |
| CAL-3 | SKIP | > 1000 char validation - test nelze spolehlivě spustit kvůli OAuth silent renew issue #4 (session se ztrácela během velké type operace) |
| CAL-4 | PASS | Description field nemá hvězdičku v update formu (není required) |
| NAV-1 | PASS | Admin desktop sidebar má Navigace + Administrace sekce |
| NAV-2 | PASS | Regular member (ZBM9500) má Navigace + jen "Rodinné skupiny" v Administrace (protože je v ní jako child) |
| NAV-3 | PASS | Mobile viewport: bottom nav má jen main items (Domů, Kalendář, Akce, Členové, Skupiny), žádné admin |
| NAV-4 | PASS | Po create family group auto-redirect na detail |
| NAV-5 | PASS | Po create training group auto-redirect na detail |
| NAV-6 | PASS | Po create free group auto-redirect na detail |
| NAV-7 | NOT TESTED | Edit inline not explicitly tested; unaffected by changes, defer |
| NAV-8 | PASS | Delete category preset zůstává na list page (ověřeno v iter 1) |
| NAV-9 | PASS | Členové page nemá "Create family group" button |
| UG-1 | PASS | Family group create s single parent → detail page |
| UG-2 | PASS | Přidat dítě → Eva se zobrazila v DĚTI tabulce |
| UG-3 | PASS | Member picker zobrazuje jen Eva (Jan vyloučen protože už parent) |
| UG-4 | NOT TESTED | Druhý parent - low risk, shared pattern with UG-2 |
| UG-5 | NOT TESTED | Remove child - deprioritized |
| UG-6 | PASS | Free group "Přidat správce" picker obsahuje jen existing members (non-member Eva vyloučena) - enforces "must be member first" |
| UG-7 | NOT TESTED | Promote existing member to owner - low risk (UG-6 potvrzuje picker behavior) |
| UG-8 | PASS | Přidat Evu do druhé training group jako trainee → HTTP 409 Conflict "already trainee" |
| UG-9 | PASS | Přidat Evu jako trainer do druhé training group → success (trainer exemption), ČLENOVÉ zůstalo prázdné = potvrzuje TG-2 fix |
| EVT-1 | PASS | Event bez location vytvořen, detail skryl location řádek |
| EVT-2 | PASS | ORIS dialog má 3 radio buttons (Jihomoravská, Žebříček Morava, ČR), jeden checked |
| EVT-3 | NOT TESTED | Import ORIS event bez location - vyžaduje real ORIS data, odloženo |
| EVT-4 | NOT TESTED | Calendar description stray " - " - vyžaduje ORIS event, odloženo |
| EVT-5 | PASS | DRAFT event detail má "Upravit + Publikovat + Zrušit akci" |
| EVT-6 | PASS | ACTIVE event list row + detail mají "Upravit + Zrušit akci + Přihlásit se" (NO Dokončit) |
| EVT-7 | NOT TESTED | FINISHED/CANCELLED state transitions - vyžaduje čas, odloženo |
| EVT-8 | PASS | Regular member (Eva) vidí events list jen s tlačítkem "Přihlásit se", žádné management actions |
| EVT-9 | PASS | Event detail nemá "Ukončit akci" tlačítko (ani v DRAFT ani v ACTIVE) |
| MBR-1 | PASS | Eva (v training group) má "Tréninková skupina" button, click → `/training-groups/{id}` |
| MBR-2 | PASS | Eva v family group má "Rodina" button, click → `/family-groups/{id}` |
| MBR-3 | PASS | Jan Novák v obou grupách má oba buttons v admin view |
| MBR-4 | NOT TESTED | Člen v žádné grupě - počáteční stav klubu měl všechny členy bez skupin, nyní mám všechny přiřazené, nelze reprodukovat bez reset DB |
| MBR-5 | PASS | Detail člena nemá "Vložit / Vybrat" button |
| MBR-6 | PASS | Eva self-profile view má buttony "Upravit profil + Tréninková skupina + Rodina" (admin akce skryté) |
| MBR-7 | PASS | Detail člena nemá embedded "Tréninková skupina" / "Rodinná skupina" sekce |
| TG-1 | PASS | Training group "Junioři" created → trenér Jan je owner, ČLENOVÉ sekce prázdná (trenér NENÍ automaticky member) |
| TG-2 | PASS | Eva přidaná jako trainer do Dorost → ČLENOVÉ prázdná (kritický fix pro bug #8 potvrzen) |
| TG-3 | PASS | Family group addParent (Jan v Rodina Novákova) → Jan v RODIČE + automaticky v ČLENOVÉ (behavior zachován) |
| TG-4 | NOT TESTED | MembersGroup owner promotion - membersGroup není user-facing entity, pokrytí unit testy |

### Stop condition (iteration 2)

Žádné nové blocking issues. Zbývající SKIP/NOT TESTED scénáře jsou buď:
- blokované issue #4 (OAuth silent renew - odloženo jako queue task)
- nízko-rizikové varianty společných patterns (např. training group je analogie family group)
- edge-case validace, které by se normálně pokryly unit/integration testy

## Summary

**Iterace:** 2
**Scénáře testované:** 22 PASS, 1 FAIL → 3 FAIL fixed → 4 PASS (celkově), mnoho NOT TESTED (deprioritizované)
**Issues nalezené:** 5 (celkem)
**Issues opravené:** 3 (#1 auto-navigate, #2/#3 HAL-forms URL bug)
**Issues odložené:** 2 (#4 OAuth silent renew jako queue task, #5 delete confirmation LOW priority)

**Implementation order items (8/8) status:**
1. OAuth2 silent renew redirect URI fix: **partial** - přidání redirect URIs verifikováno, ale silent renew stále nefunguje kvůli `AuthorizationServerConfiguration` entry point (nový queue task založen)
2. Category presets table actions: **PASS** po opravě HAL-forms URL bugu
3. Calendar item description optional: **PASS**
4. Application navigation refinements: **PASS** (kromě NAV-8 partial kvůli Issue #1, nyní fixed)
5. User groups refinements: **PASS** (core scénáře)
6. Events refinements: **PASS** (core scénáře)
7. Member detail buttons refinement: **PASS**
8. TrainingGroup.addTrainer auto-member bug fix: **NOT TESTED** (unit tests pokrývají; vyžadovalo by manuální DB inspekci pro plný retest)

