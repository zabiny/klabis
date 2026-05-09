## Why

Z review aplikace 2026-04-29 vyplynuly dvě úpravy v existujících eventy-related funkcích, které mají společný kontext (lifecycle akce + ORIS data) a jsou pro organizátory must-have před sezónou:

1. **N3 — Důvod zrušení akce.** Když manager zruší akci (DRAFT → CANCELLED nebo ACTIVE → CANCELLED), aktuálně se neukládá žádný kontext. Členové, kteří byli přihlášeni, nedostanou důvod, proč se akce nekoná (zhoršené počasí, nemoc pořadatelů, nedostatečný počet přihlášených, …). Zavedení nepovinného textového pole „důvod zrušení" poskytne organizátorovi jednoduchou cestu komunikace bez nutnosti rozesílat e-maily ručně.
2. **N6 — Více termínů uzávěrky přihlášek.** Aktuální model `Registration Deadline` podporuje pouze jeden termín (mapovaný na ORIS `EntryDate1`). ORIS reálně poskytuje až tři termíny (`EntryDate1`, `EntryDate2`, `EntryDate3` — typicky s rostoucím vkladem), které pořadatelé používají k regulaci pozdních přihlášek. Naše akce import zatím tyto pozdější termíny zahazuje, takže organizátoři je v Klabisu nevidí a nemohou na ně přihlásit členy s odpovídajícím vyšším vkladem.

Společný rys obou úprav: rozšiřují existující requirementy v `events` spec (Event Status Lifecycle, Registration Deadline, ORIS Import Includes Registration Deadline) o nová pole / scénáře. Implementačně se dotýkají stejné Event aggregate + ORIS importu + UI editačního formu — proto je sloučím do jedné change.

## What Changes

### N3 — Důvod zrušení akce

- **Modifikace** requirementu *Event Status Lifecycle* v `events`:
  - Při akci „Zrušit akci" (cancel) UI dialog nabídne **nepovinné textové pole „Důvod zrušení"** (max 500 znaků — omezení DB).
  - Hodnota se uloží do nového Event atributu `cancellationReason: Optional<String>`.
  - Důvod se zobrazí v detailu zrušené akce (pokud je vyplněný).
  - Důvod se zobrazí v tabulce akcí jako tooltip / sekundární text na řádku CANCELLED akce.
- **Otevřená otázka:** Notifikovat e-mailem všechny přihlášené členy o zrušení s důvodem? Mimo rozsah této change, navrhnout follow-up.

### N6 — Více termínů uzávěrky přihlášek

- **Modifikace** requirementu *Registration Deadline* v `events`:
  - Event může mít **až 3 sekvenční registration deadlines** — `deadline1`, `deadline2`, `deadline3` (každý optional, ale `deadline2` vyžaduje `deadline1`, `deadline3` vyžaduje `deadline2`; všechny musí být ≤ event date a každý další ≥ předchozí).
  - „Hlavní" deadline pro UI tabulku akcí = nejbližší **budoucí** deadline (nebo poslední, pokud všechny prošly).
  - Přihlášky jsou otevřené, dokud nevyprší **poslední** vyplněný deadline.
- **Modifikace** requirementu *ORIS Import Includes Registration Deadline*:
  - Import bere `EntryDate1`, `EntryDate2`, `EntryDate3` z ORIS a mapuje je na `deadline1`, `deadline2`, `deadline3`.
  - Pokud ORIS zdroj má jen `EntryDate1`, importují se odpovídajícím způsobem (zachovává backward compatibility).
- **Modifikace** *Events Table Display* — sloupec „Uzávěrka" zobrazuje aktuálně relevantní (nejbližší budoucí) deadline; ikona/badge naznačuje, že existují další termíny.
- **Modifikace** *Get Event Detail* — detail akce vypisuje všechny vyplněné termíny chronologicky.

**Nezasahuje:**
- Sémantika rostoucího vkladu (Klabis dnes vklady neeviduje, jen uzávěrky). Pokud tato funkce přijde, samostatný proposal.
- Editace registrace v okně mezi `deadline_n` a `deadline_n+1` — fungují stávající pravidla z `event-registrations` (lze upravit dokud "registrations open").

## Capabilities

### New Capabilities

Žádné. Vše modifikuje existující requirementy.

### Modified Capabilities

- `events`:
  - *Event Status Lifecycle* — přidat optional `cancellationReason` na cancel akci
  - *Registration Deadline* — rozšířit z 1 deadline na max 3 sekvenční
  - *ORIS Import Includes Registration Deadline* — mapovat všechny tři ORIS termíny
  - *Events Table Display* — zobrazení relevantního deadline + indikace dalších termínů
  - *Get Event Detail* — výpis všech vyplněných termínů
- `event-registrations` (drobně):
  - *Edit Own Registration* — pravidlo „registrations open" počítá s posledním vyplněným deadline (ne s prvním). Pokud spec text již používá „deadline" obecně, postačí poznámka v design.md, že multiple deadlines splňují "open" dokud poslední neproběhl.

## Impact

- **Backend kód:**
  - `Event` aggregát: přidat `cancellationReason: Optional<String>`, změnit `registrationDeadline: LocalDate` na strukturu `RegistrationDeadlines(deadline1, deadline2, deadline3)` (value object); úprava `Event.cancel(reason)` API.
  - Migrace `V001__initial_schema.sql`: přidat sloupce `cancellation_reason VARCHAR(500)`, `registration_deadline_2 DATE`, `registration_deadline_3 DATE`. Přejmenovat existující `registration_deadline` na `registration_deadline_1` (nebo zachovat název a chápat jako deadline1).
  - ORIS import: rozšířit mapování o `EntryDate2`, `EntryDate3`.
  - REST DTO + HAL forms: nové fieldy v create/update Event request, nový field v cancel request, response DTO s polem array deadlines.
- **Frontend kód:**
  - Event create/update form: až 3 vstupy pro registration deadlines (s validací sequenciality).
  - Cancel dialog: nepovinné textarea „Důvod zrušení".
  - Event detail: sekce „Uzávěrky přihlášek" s chronologickým výpisem.
  - Event detail (CANCELLED): zobrazit důvod, pokud je vyplněný.
  - Tabulka akcí: sloupec „Uzávěrka" zobrazuje aktuální + ikona „další termíny" jako tooltip.
- **Lokalizace:** rozšířit `src/localization/labels.ts` o nové labely.
- **Dokumentace:** `developerManual` aktualizovat o ORIS deadline mapping.
