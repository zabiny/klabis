# Membership Fees UI - QA Testing

Testování merged administrativní stránky "Členské příspěvky" (proposal `merge-membership-fees-admin-page`).
Cíl: ověřit chování dle specifikace + reportovat chybějící části UI a věci, které uživatel v UI nemůže udělat.

## Scenarios

### Navigation (NAV)
- [ ] **NAV-1**: Admin (ZBM9000) vidí v sekci Administrace jednu položku "Členské příspěvky"
- [ ] **NAV-2**: V menu nejsou samostatné položky "Katalog tierů" / "Kampaně volby členského příspěvku"
- [ ] **NAV-3**: Kliknutí na "Členské příspěvky" otevře merged stránku
- [ ] **NAV-4**: Běžný člen (ZBM9500) položku "Členské příspěvky" v menu nevidí

### Merged page (PAGE)
- [ ] **PAGE-1**: Stránka zobrazuje katalog tierů (sekce 2)
- [ ] **PAGE-2**: Katalog tierů nabízí akci "vytvořit tier" a navigaci na detail tieru
- [ ] **PAGE-3**: Aktivní kampaň je zobrazena inline (rok, deadline, akce změny deadline, fee groups) když existuje
- [ ] **PAGE-4**: Když není aktivní kampaň, sekce je skrytá a místo ní je akce publikace roku
- [ ] **PAGE-5**: Minulé kampaně jsou vypsané; aktivní rok mezi nimi není (žádná duplicita)

### Detail navigation & actions (DETAIL)
- [ ] **DETAIL-1**: Klik na tier otevře detail tieru; "zpět" vede na merged stránku
- [ ] **DETAIL-2**: Klik na minulou kampaň otevře detail kampaně; "zpět" vede na merged stránku
- [ ] **DETAIL-3**: Z aktivní kampaně lze otevřít detail fee group
- [ ] **DETAIL-4**: Akce "změnit deadline" je dostupná a funguje
- [ ] **DETAIL-5**: Akce "publikovat rok" otevře formulář a lze ji dokončit

### Authorization (AUTH)
- [ ] **AUTH-1**: Backend `GET /api/membership-fee-tiers` nese pro admina linky `activeCampaign` (když je aktivní kampaň) a `pastCampaigns`
- [ ] **AUTH-2**: Pro ne-admina backend tyto linky nenese

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | V sekci "Administrace" je jedna položka "Členské příspěvky" |
| NAV-2 | PASS | Žádné samostatné "Katalog tierů" / "Kampaně" v menu |
| NAV-3 | PASS | Kliknutí otevře merged stránku (route `/membership-fee-tiers`) |
| NAV-4 | PASS | Ne-admin ZBM9500 nevidí položku ani sekci Administrace |
| PAGE-1 | PASS | Katalog tierů zobrazuje 3 tiery (Základ/1000, Jen Oblz/3500, All inclusive/5500) |
| PAGE-2 | PASS | Akce "Přidat tier" + navigace na detail tieru přítomny |
| PAGE-3 | PASS | Aktivní kampaň zobrazena inline: rok 2027, deadline, "Změnit deadline", FeeGroupsTable |
| PAGE-4 | PASS | Bez aktivní kampaně sekce skrytá, místo ní tlačítko "Vypsat kampaň" |
| PAGE-5 | PASS | Minulé kampaně vylučují aktivní rok (API `?status=closed` vrací 0, bez filtru 1) |
| DETAIL-1 | **FAIL** | Breadcrumb z detailu tieru → `/membership-fees` → **404** (Issue #1) |
| DETAIL-2 | **FAIL** | Breadcrumb "Zpět" z detailu kampaně → `/membership-fees` → **404** (Issue #1) |
| DETAIL-3 | PASS | Detail fee group "Základ" otevřen (poplatek, status, pravidla, členové) |
| DETAIL-4 | PASS | "Změnit deadline" funguje, inline sekce se aktualizuje (31.12 → 30.11.2026) |
| DETAIL-5 | PASS | "Vypsat kampaň" vytvoří kampaň, redirect na detail, toast "Úspěšně uloženo" |
| AUTH-1 | PASS | Admin: tiers nese `pastCampaigns` vždy + `activeCampaign` když je aktivní kampaň |
| AUTH-2 | PASS | Ne-admin: tiers nese pouze `self`, žádné campaign linky |

## Issues nalezené v iteraci 1

### Issue #1 (FUNKČNÍ BUG) — Breadcrumb/navigace z detailů vede na neexistující route `/membership-fees` → 404
- **Popis**: Merged stránka je v `App.tsx` zaregistrovaná na route `/membership-fee-tiers` (řádek 118) a menu na ni odkazuje. Ale všechny "zpět"/breadcrumb odkazy a redirecty po editaci míří na `/membership-fees`, která jako route NEEXISTUJE → uživatel skončí na 404.
- **Postižená místa**:
  - `FeeSelectionCampaignDetailPage.tsx:47` — breadcrumb "← Zpět na seznam" → `/membership-fees`
  - `MembershipFeeTierDetailPage.tsx:32,36` — oba breadcrumb odkazy → `/membership-fees`
  - `MembershipFeeTierDetailPage.tsx:115` — `navigate('/membership-fees')` po úspěšné editaci tieru
- **Reprodukce**: detail kampaně/tieru → klik na breadcrumb "Zpět"/"Členské příspěvky" → 404 "Stránka nenalezena"
- **Příčina**: Nesoulad mezi designem (URL měla být `/membership-fees`) a implementací routingu (route ponechána na starém `/membership-fee-tiers`). Buď chybí přejmenování route, nebo je třeba opravit cílové URL na `/membership-fee-tiers`.
- **Dopad**: blokuje DETAIL-1, DETAIL-2 (návrat na merged stránku z detailů). Spec scénář "Returning from a campaign detail goes back to the membership fees page" je porušen.
- **Komponenta**: Frontend.

## Pozorování (drobnosti, ne blokující — k posouzení)

### Obs-A — Nelokalizované labely ve formuláři "Vypsat kampaň"
Pole formuláře publikace kampaně se zobrazují s technickými názvy `levelIds` a `year` místo českých labelů ("Uzávěrka hlasování" je lokalizováno správně). Drobný UI/lokalizační defekt. Komponenta: Frontend.

### Obs-B — Breadcrumb detailu fee group vede na `/membership-fee-groups`
Na detailu fee group ("Základ") breadcrumb "← Zpět na seznam" odkazuje na `/membership-fee-groups`, což pravděpodobně není zaregistrovaná route (neexistuje seznam skupin). Mimo hlavní scope merge proposalu (group detail page nebyla měněna), ale stojí za ověření. Komponenta: Frontend.

### Obs-C — Tlačítko "Vypsat kampaň" bez vizuální sekce
Když není aktivní kampaň, místo sekce "Aktivní kampaň" se zobrazí jen plovoucí tlačítko "Vypsat kampaň" hned pod nadpisem stránky, bez nadpisu sekce / vysvětlujícího textu ("Žádná aktivní kampaň"). Funkčně OK (PAGE-4 PASS), ale UX by mohlo být čitelnější. Komponenta: Frontend.

## Chybějící části UI / co uživatel v UI nemůže udělat

Porovnání spec capability `membership-fees` vůči implementovanému UI (nad rámec merge proposalu, ale součást zadání):

1. **Editace katalogového tieru** — detail tieru má tlačítko "Upravit" (existuje). ✅
2. **Smazání tieru** — detail tieru má "Smazat tier". ✅
3. **Správa pravidel spoluúčasti** — detail tieru umožňuje "Přidat pravidlo / Upravit / Smazat pravidlo". ✅
4. **Emergency assignment (admin přiřadí/změní tier členovi i po deadline)** — detail fee group má "Přiřadit člena"; otestováno nebylo do hloubky (prázdná skupina), ale afordance existuje. ⚠️ k ověření koncového flow.
5. **Editace publikovaného levelu do prvního příplatku** — detail fee group má "Upravit" + status "Editovatelná"; neověřeno zamykání po příplatku (chybí testovací data s příplatkem).
6. **Žádné UI pro ukončení/uzavření kampaně** — spec rozlišuje aktivní vs uzavřená kampaň (deadline v minulosti), uzavření je automatické dle deadline. V UI není explicitní akce "uzavřít kampaň" — to odpovídá doméně (uzavření je dáno uplynutím deadline), není to chybějící UI.
7. **Minulé kampaně nešlo otestovat v UI** — v testovacích datech není žádná uzavřená kampaň (closed=0), takže navigaci do detailu minulé kampaně z merged stránky (sekce 3) nešlo ověřit prakticky; ověřeno jen přes API filtr a empty state. ⚠️

---

### Iteration 2 (po opravě Issue #1)
| Scenario | Result | Note |
|----------|--------|------|
| DETAIL-1 | PASS | Breadcrumb "Členské příspěvky" z detailu tieru → merged page (`/membership-fee-tiers`), žádná 404 |
| DETAIL-2 | PASS (oprava potvrzena v kódu) | FeeSelectionCampaignDetailPage.tsx:47 breadcrumb opraven na `/membership-fee-tiers` |

**Issue #1 — VYŘEŠENO.** Breadcrumby/redirecty `/membership-fees` → `/membership-fee-tiers` (existující route shodná s API cestou kvůli HalRouteContext). Route v App.tsx NEbyla měněna (fetch by jinak šel na neexistující `/api/membership-fees`).

**Regrese: žádná.** 14 selhávajících FE testů (MembershipFeeGroupDetailPage 10×, MembershipFeeTierDetailPage 3×, MembershipFeesAdminPage 1×) ověřeno proti čistému HEAD — **selhávají i bez mých změn (pre-existující)**, viz Issue #3.

## Issues z uživatelského reportu (iterace 2)

### Issue #2 (FUNKČNÍ BUG) — Přiřazený/přihlášený člen se nezobrazuje v seznamu členů fee group
- **Popis**: Po přidání (resp. přihlášení) člena k membership fee se člen nezobrazí v seznamu členů na detailu fee group; pouze "Počet členů" daného tieru je správně (ukazuje 1).
- **Reprodukce**: merged page ukazuje skupinu "Základ" s počtem členů **1**, ale detail fee group dříve ukazoval "Žádní členové ve skupině."
- **Dopad**: admin nevidí, kteří členové jsou ve skupině; rozporuje spec "renders members section".
- **Komponenta**: k prošetření — backend (chybí embedded members v group detail response) nebo frontend (nerenderuje embedded members).

**Issue #2 — VYŘEŠENO (iterace 2).** Backend `GET /api/membership-fee-groups/{id}` nyní vrací `_embedded.members` (jméno, registrační číslo, datum vstupu, zdroj přiřazení MEMBER_CHOICE/ADMIN_ASSIGNMENT) přes cross-module `Members` API. Frontend renderuje tabulku členů. Ověřeno v prohlížeči: přiřazen "Tomáš Král (ZBM8800)" → zobrazen v sekci ČLENOVÉ s "Přiřazeno adminem". Testy: backend 254/254 (membershipfees), frontend 1867/1867.

### Issue #4 (CHYBĚJÍCÍ FUNKCE) — Editovatelná kampaň neumožňuje přidat/upravit/odebrat fee tier
- **Popis**: Editovatelná kampaň by měla umožnit přidat/upravit/odebrat membership fee tier (pokud v dané skupině není zaregistrovaný člen). V UI taková akce chybí — detail kampaně i inline aktivní kampaň zobrazují jen seznam skupin bez možnosti přidat/odebrat tier z kampaně.
- **Dopad**: admin nemůže upravit složení tierů aktivní (editovatelné) kampaně.
- **Komponenta**: k prošetření — pravděpodobně chybí backend afordance + frontend UI. Může vyžadovat openspec proposal (rozšíření funkcionality).

### Issue #3 (TESTY NESYNCHRONIZOVANÉ S IMPLEMENTACÍ) — 14 pre-existujících FE test failures
- **Popis**: Po ručních změnách v posledních commitech nebyly aktualizovány testy. Selhává 14 testů (ověřeno na čistém HEAD, nesouvisí s opravou Issue #1):
  - `MembershipFeeGroupDetailPage.test.tsx` (10×): `useHalRoute must be used within a component wrapped by HalRouteProvider` — testy nemají wrapper provider (implementace přešla na `useHalPageData`/`HalRouteContext`).
  - `MembershipFeeTierDetailPage.test.tsx` (3×): nadpis je uppercase "PRAVIDLA SPOLUÚČASTI" (test hledá "Pravidla spoluúčasti"); "Žebříček A" label; "přidat pravidlo" footer button — implementace se změnila.
  - `MembershipFeesAdminPage.test.tsx` (1×): chybí `data-testid="hal-form-modal"` po kliknutí na publishYear.
- **Komponenta**: Frontend (testy).

**Issue #3 — VYŘEŠENO (iterace 2).** Testy synchronizovány s aktuální implementací (HalRouteProvider/useAuthorizedFetch mock pro group detail, uppercase nadpis + addRule template na rules kolekci pro tier detail, assertce na displayHalForm místo modal testid). Frontend 1867/1867 prochází.
