# Membership Fee Tiers - QA Testing

## Scenarios

### Tier Catalog — Navigation & Display
- [ ] **NAV-1**: Admin vidí odkaz "Membership fee tiers" v navigaci
- [ ] **NAV-2**: Kliknutím na odkaz se zobrazí seznam tierů
- [ ] **NAV-3**: Seznam obsahuje sloupce s názvem tieru a ročním poplatkem

### Tier Creation
- [ ] **CREATE-1**: Admin vidí tlačítko "Přidat tier" na stránce seznamu
- [ ] **CREATE-2**: Formulář pro vytvoření obsahuje pouze pole Název a Roční poplatek (bez pravidel)
- [ ] **CREATE-3**: Po vytvoření tieru je přesměrován na detail tieru
- [ ] **CREATE-4**: Nově vytvořený tier má prázdný seznam pravidel

### Tier Edit & Delete
- [ ] **EDIT-1**: Na detailu tieru je tlačítko Upravit
- [ ] **EDIT-2**: Formulář pro úpravu obsahuje Název a Roční poplatek
- [ ] **EDIT-3**: Po uložení změn se zobrazí aktualizované hodnoty
- [ ] **DELETE-1**: Na detailu tieru je tlačítko Smazat
- [ ] **DELETE-2**: Po smazání je přesměrován na seznam tierů

### Rule Management — Add
- [ ] **ADD-RULE-1**: Na detailu tieru je tlačítko "Přidat pravidlo" (jen pro admin)
- [ ] **ADD-RULE-2**: Formulář pro přidání pravidla obsahuje pole: Typ závodu, Žebříček (dropdown z ORIS), Typ pravidla, Hodnota
- [ ] **ADD-RULE-3**: Žebříček je dropdown (ne free-text) s opcemi z ORIS
- [ ] **ADD-RULE-4**: Po přidání pravidla se zobrazí v tabulce pravidel
- [ ] **ADD-RULE-5**: Přidání duplicitní kombinace (eventType + ranking) je odmítnuto

### Rule Management — Edit
- [ ] **EDIT-RULE-1**: Každý řádek tabulky pravidel má tlačítko pro úpravu (pencil ikona)
- [ ] **EDIT-RULE-2**: Formulář pro úpravu obsahuje pouze hodnotu (Typ závodu a Žebříček jsou read-only / key fields)
- [ ] **EDIT-RULE-3**: Po uložení se zobrazí aktualizovaná hodnota v tabulce

### Rule Management — Delete
- [ ] **DELETE-RULE-1**: Každý řádek tabulky pravidel má tlačítko pro smazání (X ikona)
- [ ] **DELETE-RULE-2**: Po smazání pravidlo zmizí z tabulky

### Event Type Display
- [ ] **DISPLAY-1**: Sloupec "Typ závodu" v tabulce pravidel zobrazuje název (ne UUID)

### Authorization
- [ ] **AUTH-1**: Člen (ZBM9500) nevidí tlačítka pro správu tierů (Přidat, Upravit, Smazat)
- [ ] **AUTH-2**: Člen nevidí tlačítka pro správu pravidel (Přidat pravidlo, edit/delete per řádek)

---

## Results

### Iteration 1
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | Odkaz "Katalog tierů" viditelný v navigaci |
| NAV-2 | PASS | Seznam tierů se zobrazuje |
| NAV-3 | PASS | Sloupce Název a Roční poplatek přítomny |
| CREATE-1 | PASS | Tlačítko "Přidat tier" viditelné |
| CREATE-2 | PASS | Formulář obsahuje pouze Název, Roční poplatek, Měnu |
| CREATE-3 | PASS | Po vytvoření přesměrován na detail |
| CREATE-4 | PASS | Nový tier má prázdný seznam pravidel |
| EDIT-1 | PASS | Tlačítko Upravit přítomno |
| DELETE-1 | PASS | Tlačítko Smazat tier přítomno |
| ADD-RULE-1 | PASS | Tlačítko "Přidat pravidlo…" přítomno |
| ADD-RULE-2 | FAIL | eventTypeId zobrazuje dropdown členů místo typů závodů; ruleType je free-text místo dropdown |
| ADD-RULE-3 | PASS | rankingShortName je dropdown z ORIS |
| ADD-RULE-4 | SKIP | Blokováno ADD-RULE-2 |
| ADD-RULE-5 | SKIP | Blokováno ADD-RULE-2 |
| EDIT-RULE-1 | SKIP | Žádná pravidla k editaci |
| EDIT-RULE-2 | SKIP | — |
| EDIT-RULE-3 | SKIP | — |
| DELETE-RULE-1 | SKIP | Žádná pravidla k smazání |
| DELETE-RULE-2 | SKIP | — |
| DISPLAY-1 | SKIP | Žádná pravidla |
| EDIT-2 | SKIP | — |
| EDIT-3 | SKIP | — |
| DELETE-2 | SKIP | — |
| AUTH-1 | SKIP | — |
| AUTH-2 | SKIP | — |

### Iteration 2
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | Odkaz "Katalog tierů" viditelný v navigaci |
| NAV-2 | PASS | Seznam tierů se zobrazuje |
| NAV-3 | PASS | Sloupce Název a Roční poplatek přítomny |
| CREATE-1 | PASS | Tlačítko "Přidat tier" viditelné |
| CREATE-2 | PASS | Formulář obsahuje Název, Roční poplatek, Měna |
| CREATE-3 | PASS | Po vytvoření přesměrován na detail tieru |
| CREATE-4 | PASS | Nový tier má prázdný seznam pravidel |
| EDIT-1 | PASS | Tlačítko Upravit přítomno |
| DELETE-1 | PASS | Tlačítko Smazat tier přítomno |
| ADD-RULE-1 | PASS | Tlačítko "Přidat pravidlo…" přítomno |
| ADD-RULE-2 | FAIL | Labely polí jsou technické (eventTypeId, rankingShortName, ruleType, percentage) místo česky |
| ADD-RULE-3 | PASS | rankingShortName je dropdown z ORIS; eventTypeId je dropdown Závod/Trénink; ruleType je dropdown PERCENTAGE/FIXED_AMOUNT |
| ADD-RULE-4 | FAIL | Po přidání pravidla naviguje na URL `/rules/{eventTypeId}/{ranking}` bez registrované route → fallback stránka |
| ADD-RULE-5 | SKIP | Blokováno ADD-RULE-4 |
| EDIT-RULE-1 | SKIP | Blokováno ADD-RULE-4 |
| EDIT-RULE-2 | SKIP | — |
| EDIT-RULE-3 | SKIP | — |
| DELETE-RULE-1 | SKIP | Blokováno ADD-RULE-4 |
| DELETE-RULE-2 | SKIP | — |
| DISPLAY-1 | SKIP | Blokováno ADD-RULE-4 |
| EDIT-2 | SKIP | — |
| EDIT-3 | SKIP | — |
| DELETE-2 | SKIP | — |
| AUTH-1 | SKIP | — |
| AUTH-2 | SKIP | — |

### Iteration 3
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | |
| NAV-2 | PASS | |
| NAV-3 | PASS | |
| CREATE-1 | PASS | |
| CREATE-2 | PASS | |
| CREATE-3 | PASS | |
| CREATE-4 | PASS | |
| EDIT-1 | PASS | |
| EDIT-2 | PASS | Formulář zobrazuje Název a Roční poplatek; editTier template obsahuje i pole pro pravidla (minor) |
| EDIT-3 | PASS | Aktualizovaný název se zobrazí po uložení |
| DELETE-1 | PASS | |
| DELETE-2 | PASS | Po smazání přesměrován na seznam |
| ADD-RULE-1 | PASS | |
| ADD-RULE-2 | FAIL | Pole `eventTypeId`, `rankingShortName`, `ruleType` stále zobrazují technické názvy (ISSUE-4 částečně opraveno — `percentage` → "Procento spoluúčasti (%)" ✓) |
| ADD-RULE-3 | PASS | Dropdowny fungují správně |
| ADD-RULE-4 | PASS | Po přidání pravidla zůstane na detail tieru, tabulka se aktualizuje |
| ADD-RULE-5 | PASS | Duplicitní pravidlo odmítnuto s chybovou zprávou (zpráva je EN, minor) |
| EDIT-RULE-1 | PASS | Tlačítka Upravit/Smazat jsou přítomna v každém řádku |
| EDIT-RULE-2 | PASS | Formulář obsahuje pouze hodnotu (percentage, fixedAmount, ruleType) — klíčová pole read-only |
| EDIT-RULE-3 | PASS | Aktualizovaná hodnota se zobrazí v tabulce |
| DELETE-RULE-1 | PASS | Confirm dialog se otevře |
| DELETE-RULE-2 | PASS | Pravidlo zmizí z tabulky po smazání |
| DISPLAY-1 | FAIL | Sloupec "Typ závodu" zobrazuje UUID místo názvu (eventTypeId není přeložen) |
| AUTH-1 | PASS | Člen nevidí tlačítko "Přidat tier" |
| AUTH-2 | PASS | Člen nevidí Upravit/Smazat tier ani Přidat/Upravit/Smazat pravidlo |

### Iteration 4
| Scenario | Result | Note |
|----------|--------|------|
| NAV-1 | PASS | |
| NAV-2 | PASS | |
| NAV-3 | PASS | |
| CREATE-1 | PASS | |
| CREATE-2 | PASS | |
| CREATE-3 | PASS | |
| CREATE-4 | PASS | |
| EDIT-1 | PASS | |
| EDIT-2 | PASS | |
| EDIT-3 | PASS | |
| DELETE-1 | PASS | |
| DELETE-2 | PASS | |
| ADD-RULE-1 | PASS | |
| ADD-RULE-2 | PASS | Všechna pole mají česky labely; dropdowny správné |
| ADD-RULE-3 | PASS | |
| ADD-RULE-4 | PASS | |
| ADD-RULE-5 | PASS | |
| EDIT-RULE-1 | PASS | |
| EDIT-RULE-2 | PASS | |
| EDIT-RULE-3 | PASS | |
| DELETE-RULE-1 | PASS | |
| DELETE-RULE-2 | PASS | |
| DISPLAY-1 | PASS | Sloupec "Typ závodu" zobrazuje název event typu (ne UUID) |
| AUTH-1 | PASS | |
| AUTH-2 | PASS | |

**Všechny scénáře prošly. QA testování dokončeno.**

### Issues found in Iteration 3

**ISSUE-8 (DISPLAY-1): eventTypeId zobrazuje UUID místo názvu event typu**
- Tabulka pravidel zobrazuje `rule.eventTypeId` jako UUID string
- Backend vrací `_links.eventType` link na event type resource — ale frontend ho nepoužívá
- Fix (frontend): v `RuleRow` komponentě přidat lookup event type názvu přes `_links.eventType` link, nebo přidat hook `useEventTypes()` pro lookup UUID → název

**ISSUE-9 (ADD-RULE-2): Technické field labely `eventTypeId`, `rankingShortName`, `ruleType`**
- Fix (frontend): přidat překlady do `labels.ts` pro `eventTypeId` → "Typ závodu", `rankingShortName` → "Žebříček", `ruleType` → "Typ pravidla"

### Issues found in Iteration 2 (backend)

**ISSUE-5 (DISPLAY-1): `eventTypeId` v rules response je plain UUID string**
- `GET /api/membership-fee-tiers/{id}/rules` vrací `"eventTypeId": "a66b4c3a-..."` jako string
- Frontend nemůže zobrazit název event typu — zobrazuje UUID
- Fix (backend): `PaymentRuleResponse.eventTypeId` by měl být serializován jako objekt `{"value": "uuid"}` (aktuální `EventTypeReference`) — nebo backend přidat název přímo do response. Přidej také `_links.eventType` link na event type resource.
- Aktuálně `EventTypeReference` se serializuje jako `{"value": "uuid"}` — ale frontend čte `rule.eventTypeId` jako string. Buď frontend si přizpůsobit nebo backend serializovat jinak.

**ISSUE-6 (DISPLAY-1): `rankingShortName` je číselný kód "2" místo textu "Žebříček A"**
- Backend ukládá a vrací interní ORIS kód ranku (číslo) místo textového jména
- Fix (backend): `rankingShortName` by měl vrátit textový název ranku odpovídající zobrazovaným opcím v addRule formuláři (např. "Žebříček A"), nebo frontend musí mapovat kód na název.

**ISSUE-7 (EDIT-RULE-2): `editRule` template nemá inline options pro `ruleType`**
- `_templates.editRule` obsahuje `ruleType` jako free-text bez inline options
- Fix (backend): `PaymentRuleDetailsPostprocessor` musí přidat inline options pro `ruleType` v `editRule` affordanci (stejně jako `addRule`)

### Issues found in Iteration 2

**ISSUE-3 (ADD-RULE-4): Navigace na rule URL po přidání pravidla**
- Po POST addRule backend vrací HTTP 201 s Location header na GET rule endpoint (`/api/membership-fee-tiers/{id}/rules/{eventTypeId}/{ranking}`)
- Frontend `HalFormsForm` naviguje na Location URL → React Router nemá route pro `/membership-fee-tiers/:id/rules/:eventTypeId/:ranking`
- Zobrazí se fallback stránka místo tier detailu
- Fix (frontend): v addRule modal nastavit `navigateOnSuccess={false}` a po úspěchu volat `route.refetch()` nebo navigovat zpět na tier URL. Tier detail page musí po addRule invalidovat/refetchovat rules subresource.
- Alternativa: přidat `navigateOnSuccess={false}` do `HalFormModal` volaného pro addRule v `MembershipFeeTierDetailPage.tsx`

**ISSUE-4 (ADD-RULE-2 labely): Technické názvy polí v addRule formuláři**
- Pole `eventTypeId`, `rankingShortName`, `ruleType`, `percentage` zobrazují technické názvy
- Fix (frontend): přidat české překlady do `labels.ts` pro tyto field names; `getFieldLabel` je voláno z `HalFormsInput`

### Issues found in Iteration 1

**ISSUE-1 (ADD-RULE-2): eventTypeId zobrazuje dropdown členů**
- Backend posílá `eventTypeId` s typem `UUID` bez inline options
- Frontend `KlabisFieldsFactory` mapuje `UUID` → `memberIdFieldRenderer` → dropdown členů
- Správně by měl zobrazit dropdown typů závodů (z `/api/event-types`)
- Fix: backend posílat `eventTypeId` s typem `EventTypeReference` a inline options z event types; nebo frontend speciální handling

**ISSUE-2 (ADD-RULE-2): ruleType je free-text místo dropdown**
- Backend posílá `ruleType` s typem `text` bez inline options
- Správně by měl být dropdown: PERCENTAGE / FIXED_AMOUNT
- Fix: backend přidat inline options pro ruleType v addRule affordance
