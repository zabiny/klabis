## Context

`EventRegistrationDialog` dnes přijímá od volajícího osm propů (`isOpen`, `mode`, `template`, `event{name, eventDate, location, deadlines, sharedTransportEnabled, sharedAccommodationEnabled}`, `prefillHref`, `initialValuesHref`, `onClose`, `onRegistered?`). Každé ze čtyř vstupních míst (EventDetailPage edit/new, EventsPage, UpcomingDeadlinesWidget) musí samo najít šablonu submitu, event kontext i prefill href — přestože reprezentace registrace nese všechna data (jméno, SI čip, kategorii) i link na event a event nese kontext i `registerForEvent` affordanci.

Klíčová mezera: prefill odpověď (`getRegistration?newRegistration=true`) dnes nese `editRegistration` a `unregisterFromEvent` affordance — `RegistrationDetailsPostprocessor` neumí prefill od existující registrace rozeznat, protože obě větve `EventRegistrationController.getRegistration` setují tentýž `RegistrationView(event, memberId)`. `editRegistration` na prefillu je navíc chybná affordance (míří na neexistující registraci) a `unregisterFromEvent` na prefillu odporuje specifikaci („present when the caller is the registered member").

## Goals / Non-Goals

**Goals:**

- Jediný datový vstup dialogu: `registration: Link | null` (null = zavřeno); new režim vstupuje přes `newRegistration` link eventu.
- Frontend odvozuje režim a šablonu submitu z affordance v reprezentaci registrace (registerForEvent ⇒ new, editRegistration ⇒ edit).
- Backend prefill odpověď nese koherentní affordanci `registerForEvent` (s prompted `categoryId` options) místo `editRegistration`/`unregisterFromEvent`.
- Volající (detail, seznam, dashboard) se zjednoduší na předání linku.

**Non-Goals:**

- Žádná změna registračních pravidel, payloadu, validací ani oprávnění submitu.
- Žádná změna affordancí na eventu (detail i seznam řádků zůstávají, jak jsou).
- Žádný univerzální „member profile" hook — jméno pořád pochází z reprezentace registrace.

## Decisions

- **Režim a šablonu určuje affordance v reprezentaci, ne volající.** Hook `useRegistrationDialogData` vybere `_templates.registerForEvent` ⇒ new, jinak `_templates.editRegistration` ⇒ edit, jinak error stav. Alternativa (ponechat `mode` prop nebo sniffovat `?newRegistration=true` v href) byla zamítnuta: první porušuje cíl jediného vstupu, druhá spoléhá na formát URL místo na hypermediální kontrakt. Podmínkou je backend změna níže.
- **`RegistrationView` získá příznak prefill.** `RegistrationView(Event event, MemberId memberId, boolean prefill)` — `new=true` větev controlleru setuje `true`, existující registrace i list `false`. `RegistrationDetailsPostprocessor` podle něj větví: prefill ⇒ self bez edit/unregister affordancí + `registerForEvent` affordance; existující ⇒ dnešní chování. Alternativa (dvě různé domain třídy) odmítnuta jako zbytečný nový typ pro jeden bit.
- **Sanction parita.** `RegistrationDetailsPostprocessor` injektuje `MemberRegistrationSanctionPort` a na prefillu gate-uje `registerForEvent` na `!isMemberBlocked` (stejně jako `EventDetailsPostprocessor`/`EventSummaryPostprocessor`). Bez toho by zablokovaný člen dostal použitelnou POST affordanci přímým URL na prefill. Gate podmínka je `EventAffordanceSupport.shouldOfferRegistration(event)` (ACTIVE + otevřené registrace), tedy stejná sémantika jako na eventu.
- **Frontend: hook + prezentacional dialog.** `useRegistrationDialogData(registration: Link | null)` provádí zřetězené dotazy (registrace → `event` link → event; pattern `FinanceTransactionDialog`), dialog konzumuje hook interně a prezentační část tvoří vnitřní komponentu téhož souboru. Alternativa (hook volá volající a předává data) by vrátila složitost volajícím, kterou chceme odstranit.
- **Chybějící affordance = error stav dialogu.** Při `mode === undefined` po načtení zobrazí dialog chybový Alert bez footeru (label `prefillLoadError`). Dnes by se v takovém případě otevřel formulář s chybnou `editRegistration` šablonou — nové chování je korektní a testované.
- **Event kontext se dofetchnuvá, i když ho volající má.** Řádky seznamu a widgetu event data nesou, ale dialog je přijímá jen přes link — úmyslně za cenu jednoho GET navíc při otevření. Volající tím přestanou vědět cokoli o registračním formuláři.

## REST API ( změna kontraktu )

Endpoint: `GET /api/events/{eventId}/registrations/{memberId}?newRegistration=true` (prefill) — response `RegistrationDto` + HAL.

| Affordance | Před změnou (prefill) | Po změně (prefill) | Existující registrace (new=false) |
|---|---|---|---|
| `_templates.registerForEvent` | chybí | **přibývá** — POST, prompted `categoryId` options; jen při otevřených registracích a nezablokovaném členu | chybí |
| `_templates.editRegistration` | přítomna (chybně) | **odstraňuje se** | beze změny (jen při otevřených registracích) |
| `_templates.unregisterFromEvent` | přítomna (proti specifikaci) | **odstraňuje se** | beze změny (jen vlastní, při otevřených registracích) |
| `_links.self`, `_links.event` | beze změny | beze změny (self i nadále míří na `new=false` URL) | beze změny |

## Risks / Trade-offs

- [Viditelná změna UX: prefill na eventu se zavřenými registracemi] → dnes by se otevřel formulář s chybnou šablonou, po změně error Alert; pokryto testem i QA scénářem.
- [Jeden GET navíc při každém otevření dialogu] → event se dofetchnuvá vždy (`staleTime/gcTime 0` jako dnes); objem odpovědi malý, otevření dialogu je interakce uživatele.
- [Dialog bez explicitního `isOpen` prop] → `registration === null` nahrazuje zavřený stav; volající drží v state `Link | null` místo boolean + uložených dat.
- [Sanction check přidává závislost postprocessoru na portu] → konstruktorová injekce, stejný vzor jako `EventDetailsPostprocessor`; test mockem.

## Migration Plan

Backend a frontend jde dohromady v jednom nasazení (frontend přestane číst `editRegistration` z prefillu — jediný konzument). Rollback = revert obou sliceů. Dev-data a starší klienti: starý frontend s novým backendem si v new režimu vezme `registerForEvent` šablonu z eventu, jak dnes — nedochází k rozbití.

## Open Questions

(none)
