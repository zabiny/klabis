## Context

Dvě malé frontendové UX úpravy. Backend není dotčen (s jednou drobnou výjimkou — ad hoc HAL+FORMS metadata field, viz Decision 1).

Aktuální stav:
- `EventRegistrationForm` (`frontend/src/components/events/...` nebo podobně) inicializuje pole „SI číslo" prázdné.
- Action tlačítka v `KlabisTable` v `EventsListPage` jsou vykreslována přes HAL affordance přes generic Button component bez color variant — všechna mají defaultní (neutrální) styling.
- Frontend již má `useCurrentMember` / current-user hook (memory `project_navigation_architecture.md` zmiňuje main menu závislé na current user data).

## Goals / Non-Goals

**Goals:**
- N2: registrační form předvyplní SI číslo z profilu, pokud existuje. Hodnota je editovatelná; submit nemění profilové SI.
- K2: action tlačítka v tabulce akcí dostávají sémantické barvy podle účelu akce (primary / destructive / warning / neutral).

**Non-Goals:**
- Sémantické barvy v jiných tabulkách (members, groups, calendar) — out of scope (lze rozšířit v navazujícím proposalu pokud se ukáže, že stejná logika přináší přidanou hodnotu jinde).
- Theming / dark mode adaptace barevné palety — je-li zde rozdíl, řeší se v existující theme infrastructure.
- Server-side rozhodování o button variant (zatím zůstane na frontendu — KISS).

## Decisions

### Decision 1: SI prefill — server-side defaults přes GET registrace s `new=true`

Rewritten 2026-05-10 na základě průzkumu kódu:
- `useCurrentMember()` hook neexistuje. Z auth contextu (`useAuth()`) máme jen JWT claims (firstName, lastName, memberId,…) — nikoliv `siCardNumber`.
- `MemberDetailsResponse` aktuálně nevrací `siCardNumber` (atribut existuje na `EventRegistration`, nikoliv jako veřejné pole detail DTO).
- HAL+FORMS template properties už nativně podporují `value` pro prefill — používá se konzistentně napříč aplikací pro edit formuláře. Pro nový resource (registrace, která ještě neexistuje) lze stejný mechanismus rozšířit přes "new resource defaults" pattern.

**Volba:** GET endpoint pro detail registrace (`GET /api/events/{eventId}/registrations/{memberId}`) přijme volitelný query parametr `new: boolean` (default `false`):
- `new=false` (nebo chybí): zachované stávající chování — 200 OK pokud registrace existuje, 404 jinak.
- `new=true`: server nevyhledává existující registraci, místo toho vrátí 200 OK s "defaults" payload — pro aktuálně autentizovaného uživatele načte `Member.siCardNumber` z profilu a vloží ho do template property `siCardNumber.value`. Ostatní pole zůstanou prázdná / s aplikovanými defaulty z template.

Frontend tedy nepotřebuje znát current user data ani fetchnout member detail — generic `HalFormDisplay` / `useHalFormData` flow se postará o prefill stejně jako u edit formu.

**Affordance mechanizmus (implementační realita):** ideálním stavem by bylo publikovat HAL+FORMS template (např. `newRegistration`) jehož `target` obsahuje `?newRegistration=true`. Spring HATEOAS 2.5.1 ale ve svém `HalFormsTemplateBuilder` aktivně filtruje GET affordances pryč z `_templates` JSON (`!model.hasHttpMethod(HttpMethod.GET)`), takže GET affordance nelze do `_templates` doručit bez forku knihovny.

Implementace proto používá `_links` relaci `newRegistration` ukazující na `GET /api/events/{eventId}/registrations/{memberId}?newRegistration=true`. Frontend `EventsPage` má speciální page-level fetch tohoto linku a předá resource do `HalFormDisplay`, aby se prefill flow simuloval. Pokud budoucí Spring HATEOAS umožní GET affordances v `_templates`, můžeme přepnout na čistě generic flow bez page-level workaroundu.

**Authorization:**
- `new=true` neignoruje `{memberId}` z URL. Server ověří, že autentizovaný uživatel smí zakládat registraci pro daného `{memberId}` — typicky pouze pro sebe sama (memberId == principalMemberId). Pokud aplikace v budoucnu připustí registraci jménem jiného člena (např. trenér za závodníka), kontrola odpovídajícího oprávnění proběhne zde. Při neoprávněném `{memberId}` vrátí 403.
- Defaultní/typický use case: affordance `registerForEvent` cílí na `{memberId}` rovnou autentizovaného principálu. Pole `siCardNumber` v defaults se vždy načítá z profilu právě toho `{memberId}` (tj. komu se registrace zakládá), nikoliv aktora.

**Alternative considered:**
- *Frontend fetchne current member + extrahuje siCardNumber* — vyžaduje rozšíření MemberDetailsResponse o nové pole + nový hook + handle dvou query state v komponentě. Více kódu, více povrchu API.
- *Samostatný resource `/registrations/template`* — sémanticky čistší, ale duplikuje URL pro stejnou operaci. `?new=true` je menší zásah do API.
- *Klient pamatuje poslední použité SI v localStorage* — nedeterministické; `Member.siCardNumber` je single source of truth.

### Decision 2: Action button variant — frontend mapping podle link relation name

Backend HAL response obsahuje affordance link relations s názvy jako `register-for-event`, `unregister-from-event`, `cancel-event`, `publish-event`, `update-event`, `sync-from-oris`. Frontend si interpretuje variant podle relation name přes konfigurační mapping:

```ts
const ACTION_VARIANT: Record<string, ButtonVariant> = {
  'register-for-event': 'primary',
  'publish-event': 'primary',
  'unregister-from-event': 'warning',
  'cancel-event': 'destructive',
  'update-event': 'neutral',
  'sync-from-oris': 'neutral',
  // explicit fallback for unknown relations
};
```

Mapping je centralizovaný v `KlabisTable` action button renderer (nebo v `actionVariants.ts` utility). Přidání nové action vyžaduje update této mapy.

**Alternative considered:**
- *Backend posílá `actionType` v HAL+FORMS metadata* — flexibilnější (server může změnit barvu bez frontend deploye), ale vyžaduje přidání non-standard field do HAL response a vlastní deserialization. Pro MVP overkill.
- *Hardcoded variant per Button component* — nelze, protože `KlabisTable` button rendering je generický (driven HAL affordances).

### Decision 3: Theme tokens pro varianty

Existující theme (Tailwind) typicky definuje `bg-primary`, `bg-destructive`, `bg-warning`, `bg-neutral` jako utility classes. Variant prop na Button → CSS class. Žádný hardcoded hex.

**Color guidelines:**
- Primary / success — výrazná akce s pozitivním výsledkem (přihlásit, publikovat).
- Destructive — akce, která něco trvale ruší (zrušit akci).
- Warning — akce, která něco mění, ale ne destruktivně (odhlásit se — uživatel si to může rozmyslet a zase přihlásit).
- Neutral — bezpečná akce (upravit, synchronizovat — neničí ani nezakládá nic neočekávaného).

## Risks / Trade-offs

- **[Risk] Frontend mapping zaostává za backend changes (nová affordance bez variant entry)** → Mitigation: explicit fallback na `neutral` pro unknown relations; testy ověří, že všechny aktuálně používané relations mají mapping; review checklist zmiňuje update mapy při přidání nové affordance.
- **[Risk] N2 — `new=true` cesta může omylem fungovat i pro neautentizovaného nebo cizího usera** → Mitigation: `{memberId}` se NEignoruje. Server ověří, že autentizovaný principál smí zakládat registraci pro daného `{memberId}` (typicky `principalMemberId == memberId` — sám pro sebe; jiné scénáře vyžadují explicitní oprávnění). Neoprávněný call → 403. Nepřihlášený → 401.
- **[Risk] N2 — current user v profilu nemá `siCardNumber`** → Mitigation: server vrátí prázdnou hodnotu v template property; frontend přirozeně předvyplní prázdné pole (žádná chyba).
- **[Trade-off] Hardcoded variant mapping v frontendu** vs. server-side — méně flexibilní, ale jednoznačnější a bez server-side komplexity. Při dalším škálování lze přejít na server-side metadata.

## Migration Plan

1. **N2:** rozšířit GET registration controller o `new` query param + service metodu vracející defaults pro current usera. Frontend žádná změna v komponentě — affordance URL s `new=true` postačí. Test backend + e2e ověření.
2. **K2:** rozšířit Button / Table renderer o variant prop, přidat `actionVariants.ts` mapping, update existing action buttons. Storybook / unit tests pro každý variant.
3. **Smoke test po deployi:** ověřit prefill pro uživatele s/bez SI v profilu; ověřit barvy v tabulce akcí.

## Open Questions

- **Existuje Action sloupec i v jiných tabulkách (members, calendar, groups)?** Pokud ano, zda K2 rozšířit i tam — out of scope, samostatný proposal.
