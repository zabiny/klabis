## Context

Spec `event-registrations` aktuálně rozlišuje pouze dvě role v rámci registrace na akci: vlastník registrace (přihlášený uživatel) a uživatel s `EVENTS:REGISTRATIONS` autoritou. Citlivá data (`siCardNumber`, `registrationTime` v některých kontextech) jsou ošetřena field-level autorizací přes `@OwnerVisible` + `@HasAuthority`.

`Event` aggregát má atribut `eventCoordinatorId: MemberId` (nullable) — ten označuje vedoucího konkrétní akce. V dnešní implementaci coordinator nemá žádnou speciální autorizační roli — jeho jméno se jen zobrazuje v UI sloupci „Koordinátor" (poznámka K1 v review jej navíc přejmenuje na „Vedoucí", samostatný proposal).

Review 2026-04-29 přineslo tři poznámky (N9, N10, N11), které všechny stojí na stejném autorizačním principu: **vedoucí akce má pro daný event stejná oprávnění jako globální `EVENTS:REGISTRATIONS`, omezeně na svou akci**. Bez formálního zavedení tohoto principu nelze N9/N11 implementovat. Proto je sloučení proposalů přirozené.

## Goals / Non-Goals

**Goals:**
- Zavést autorizační princip „je-li uživatel coordinator daného eventu **NEBO** má `EVENTS:REGISTRATIONS`, vidí citlivá data registrace pro tento event".
- Skrýt sloupec `registrationTime` v tabulce přihlášek pro běžné členy (zachová se ale stejné default sortování FCFS).
- Přidat sortable headers v tabulce přihlášek pro `firstName`, `lastName`, `category`; pro autorizované uživatele i pro `registrationTime`.
- Přidat „Seznam pro ubytování" — print-friendly view s `firstName`, `lastName`, `identityCard.cardNumber`, `identityCard.validityDate`, `dateOfBirth`, `address` pro autorizované uživatele.
- Údaje pro export brát ze `Member` aggregátu (HAL link `member` na registraci → fetch member detail).

**Non-Goals:**
- Server-side PDF generation (KISS: HTML print + browser print dialog stačí).
- Logování přístupů k citlivým údajům (GDPR audit) — samostatný proposal.
- Změna pojmenování `coordinator` → `vedoucí` v API (samostatný cosmetic proposal).
- Multi-column sort, persistence sortu napříč sessiony (samostatný proposal N7 – persistence řazení tabulek).

## Decisions

### Decision 1: Autorizační princip „coordinator OR authority" — reuse existujícího `@OwnerVisible` + `@OwnerId`

**Žádná nová anotace.** Po diskuzi (2026-04-29) zjištěno, že existující field-level mechanismus pokrývá tento case beze změny:

- `@OwnerVisible` na fieldu = pole je vidět vlastníkovi nebo držiteli explicitně uvedené `@HasAuthority`.
- `@OwnerId` na sibling fieldu (record component) = identifikuje, kdo je „vlastník" pro účely srovnání s aktuálně přihlášeným uživatelem.

Pro registration list DTO znamená: `EventRegistrationSummaryDto` bude vystavovat **`eventCoordinatorId: MemberId` jako `@OwnerId`** field (resolve z surrounding eventu při sestavování DTO) a **`registrationTime` jako `@OwnerVisible @HasAuthority(EVENTS_REGISTRATIONS)`**. Logika field-security advice pak vrátí `registrationTime` viditelný, pokud:
- aktuální user je vlastníkem (= jeho `MemberId` matchuje `eventCoordinatorId`), NEBO
- má `EVENTS:REGISTRATIONS` autoritu.

To se sémanticky shoduje s naším požadavkem „vedoucí akce + admin se zvláštní autoritou".

**Příklad DTO (orientační):**
```java
public record EventRegistrationSummaryDto(
    MemberId memberId,
    String firstName,
    String lastName,
    String category,
    @OwnerVisible @HasAuthority(Authority.EVENTS_REGISTRATIONS)
    Instant registrationTime,
    @OwnerId MemberId eventCoordinatorId  // surrounding event's coordinator, populated by mapper
) { }
```

**Plus:**
- Žádný nový kód v `common/security/fieldsecurity/`.
- Konzistentní s ostatními místy v aplikaci, kde je field-level autorizace už používána.
- Žádný nový pattern v `backend-patterns` skill, jen větší příklad pro existující.

**Trade-off:** `eventCoordinatorId` je teď součást registration DTO i pro neautorizované uživatele (ti vidí `eventCoordinatorId`, ale ne `registrationTime`). To je akceptovatelné — `eventCoordinatorId` není citlivá informace; UI ho stejně zobrazuje jako sloupec „Vedoucí akce" v event detailu.

**Edge case — event bez coordinatora:** když `eventCoordinatorId` je `null`, žádný uživatel se na něj nematchne přes `@OwnerId`, takže `registrationTime` (a analogicky accommodation list) vidí pouze držitelé `EVENTS:REGISTRATIONS` autority. Žádné speciální ošetření v kódu — vyplývá přímo z `@OwnerVisible` sémantiky.

**Alternative considered (zamítnuto):**
- *Vlastní `@CoordinatorVisible` anotace* — původní návrh; zamítnut, protože stávající mechanismus pokrývá use case bez nového kódu.
- *Inline `@PreAuthorize` SpEL přímo na DTO* — málo čitelné.
- *Vlastní controller advice s ručním filtrováním DTO* — duplikoval by již existující pattern.

### Decision 2: Skrytí sloupce `registrationTime` v list view přes HAL+FORMS metadata

Frontend `KlabisTable` už dnes podmiňuje rendering sloupců na základě HAL+FORMS template metadat. Server vrátí registration list DTO **bez** `registrationTime` field pro neautorizované uživatele (field-level security advice ho odfiltruje při serializaci JSONu). Frontend table komponenta sloupec automaticky nezobrazí.

Konkrétní implementace: `@OwnerVisible @HasAuthority(EVENTS_REGISTRATIONS)` na `registrationTime` v `EventRegistrationSummaryDto`, sibling field `eventCoordinatorId` označen `@OwnerId` (viz Decision 1).

**Sortování přes registrationTime:** pokud volající není autorizovaný (= ani coordinator daného eventu, ani držitel `EVENTS:REGISTRATIONS`), server `sort=registrationTime` **tiše ignoruje** a aplikuje default sort. Stejná logika pro libovolný neznámý / nepodporovaný sort field — žádné 400/403, prostě fallback na default. Frontend table nezobrazí sortable header pro skryté sloupce, takže k tomuto fallbacku by docházet jen při přímém volání API.

### Decision 3: Default sort = `registrationTime ASC` — i pro běžné členy

Spec dnes neformuluje default sort, ale FCFS pořadí je implicitní (řazení podle vzniku). Změnou skrytí sloupce **registrationTime** pro běžné členy se může otevřít otázka: podle čeho jsou seřazeni?

Rozhodnutí: zachováme `registrationTime ASC` jako server-side default sort i pro skrytý sloupec. Běžný člen tak vidí FCFS pořadí (jméno A, jméno B, jméno C — rychlejší přihlášený nahoře), jen mu chybí sloupec s časy. Pokud ručně klikne na header `firstName`, sort se změní (ascending alphabet). Po refreshi se vrátí default `registrationTime ASC`.

### Decision 4: „Seznam pro ubytování" — JSON endpoint + frontend print-friendly stránka

Endpoint: `GET /api/events/{eventId}/accommodation-list` vrací **JSON (HAL)** se seznamem členů pro ubytování. Print-friendly rendering řeší **frontend** jako samostatnou route, která data načte a zobrazí v layoutu připraveném pro tisk (CSS `@media print`).

- Backend vrací DTO s poli `firstName`, `lastName`, `identityCard.cardNumber`, `identityCard.validityDate`, `dateOfBirth`, `address` per member. Chybějící pole jsou v JSONu `null` — frontend je při renderu nahradí textem „neuvedeno".
- Dostupné jen pro autorizované uživatele (coordinator daného eventu OR `EVENTS:REGISTRATIONS`). Method-level autorizace v controlleru přes `@PreAuthorize("hasAuthority('EVENTS:REGISTRATIONS') or @ownershipResolver.isOwner(#eventId, ...)")` — ověřit precedens v codebase a sjednotit s patternem použitým u jiných „owner OR authority" endpointů.
- Frontend route (např. `/events/:id/accommodation-list`) zobrazí tabulku s přepínačem `@media screen` / `@media print`; tlačítko „Tisknout" volá `window.print()`.

**Alternative considered:**
- *Server-side rendered HTML (Thymeleaf)* — méně kódu, ale odklon od běžného architektonického patternu (FE = SPA, BE = HAL+FORMS API). Mixování server-rendered HTML do API nevyplatí pro jednu stránku.
- *Server-side PDF (PDFBox / iText)* — overkill pro první iteraci.
- *CSV / Excel export* — uživatel zatím nepotvrdil. Pokud bude potřeba, samostatný proposal.

### Decision 5: HAL+FORMS affordance „accommodation-list" na detailu eventu

Event detail response zpřístupní nový link:
```json
"_links": {
  "accommodation-list": { "href": "/api/events/{id}/accommodation-list" }
}
```

Link se přidá do response DTO **pouze pro autorizované uživatele** (existující pattern v `EventSummaryDto`). Frontend pomocí přítomnosti tohoto linku rozhodne o zobrazení akce „Seznam pro ubytování" v action baru detailu eventu; klik naviguje na frontend route, která data z linku načte a zobrazí.

## Risks / Trade-offs

- **[Risk] `eventCoordinatorId` se musí populovat na každém řádku DTO mapperem; pokud chybí, autorizační check selže tiše a coordinator neuvidí `registrationTime`** → Mitigation: integration test pokrývá coordinator path (member matching `eventCoordinatorId` vidí timestamp); mapper má unit test ověřující, že `eventCoordinatorId` je vždy vyplněn ze surrounding eventu.
- **[Risk] GDPR — citlivé údaje (číslo identity card, adresa) v print view** → Mitigation: přístup omezen na `coordinator` + `EVENTS:REGISTRATIONS`. Logování přístupu je out of scope, ale doporučí se follow-up proposal (audit log).
- **[Trade-off] Server-side rendered HTML místo SPA** — drobný odklon od běžného architektonického patternu (frontend = SPA, backend = HAL+FORMS API). Print-friendly stránka tu ale nepatří do SPA flow (uživatel ji otevře v novém tabu, vytiskne, zavře) — KISS.
- **[Risk] Coordinator může vidět citlivá data i u akce, kterou organizoval kdysi dávno** → Mitigation: principiálně OK, je to forever role pro daný event. Pokud bude potřeba expirace, samostatný proposal.

## Migration Plan

1. **Implementace v pořadí (vertical slices):**
   1. Aplikace `@OwnerVisible` + `@OwnerId` na `registrationTime` / `eventCoordinatorId` v `EventRegistrationSummaryDto` (N9).
   2. Sortable headers v tabulce přihlášek (N10).
   3. Accommodation list endpoint + HAL affordance + frontend tlačítko (N11).
2. **Test:** unit testy field-level authorization, integration testy controlleru, browser smoke test.
3. **Rollout:** standard deploy. Žádný migration step pro existující data — IdentityCard už je v modelu, jen se nově zobrazí v exportu.

## Open Questions

- **Kdo nastavuje coordinator?** Aktuálně součást `Event.update` flow; pro tuto change-set nepotřebujeme nic měnit, ale pro „uživatel je vedoucí mé akce" UX pohled je dobré ověřit, že coordinator se v Event UI nastavuje správně.
