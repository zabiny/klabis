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

### Decision 1: SI prefill — frontend čte z aktuálního usera, ne z server-side template default

Spring HATEOAS HAL+FORMS umožňuje populovat `value` na fieldu v template, což by realizovalo prefill server-side. Ale:
- Hodnota je per-user (current authenticated member), což znamená, že server by musel render template v kontextu konkrétního usera.
- Cache HAL response → cross-user leakage rizika, pokud není opatrné.
- Frontend už beztak fetchne current member detail (header zobrazuje jméno).

**Volba:** frontend čte `Member.siCardNumber` z `useCurrentMember()` (TanStack Query, cached) a nastaví Formik `initialValues.siCardNumber` z této hodnoty. Hookem se to ošetří jednou v `EventRegistrationForm`. Pokud je `siCardNumber` undefined nebo null, použije se prázdný string (current behaviour).

**Alternative considered:**
- *Server-side prefill přes HAL+FORMS template `value` field* — vyžaduje user-aware template rendering; current code base toho nedělá; přidá komplexitu.
- *Klient si pamatuje poslední použité SI číslo v localStorage* — nedeterministický (po výměně čipu za sezónu by uložená hodnota byla zastaralá). `Member.siCardNumber` je single source of truth.

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
- **[Risk] N2 — pokud uživatel nemá v profilu `siCardNumber` field exposed kvůli `@OwnerVisible` (např. `useCurrentMember` neobsahuje SI číslo), prefill nefunguje** → Mitigation: ověřit, že current user response obsahuje `siCardNumber` — pokud ne, rozšířit DTO. Probably ok, protože je to vlastní data uživatele (`@OwnerVisible` matchuje).
- **[Trade-off] Hardcoded variant mapping v frontendu** vs. server-side — méně flexibilní, ale jednoznačnější a bez server-side komplexity. Při dalším škálování lze přejít na server-side metadata.

## Migration Plan

1. **N2:** modifikovat `EventRegistrationForm` — number lines kódu. Test pro prefill behavior.
2. **K2:** rozšířit Button / Table renderer o variant prop, přidat `actionVariants.ts` mapping, update existing action buttons. Storybook / unit tests pro každý variant.
3. **Smoke test po deployi:** ověřit prefill pro uživatele s/bez SI v profilu; ověřit barvy v tabulce akcí.

## Open Questions

- **Existuje Action sloupec i v jiných tabulkách (members, calendar, groups)?** Pokud ano, zda K2 rozšířit i tam — out of scope, samostatný proposal.
