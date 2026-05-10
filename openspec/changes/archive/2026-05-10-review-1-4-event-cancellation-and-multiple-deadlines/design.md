## Context

`Event` aggregát aktuálně modeluje stav akce přes `EventStatus` (DRAFT / ACTIVE / FINISHED / CANCELLED) s explicitními transition methods (`publish`, `cancel`, automatic finish). Cancel je dnes bezparametrový — žádný kontext se neukládá.

Registration deadline je modelován jako jediný `Optional<LocalDate>` field na `Event`. ORIS import bere `EntryDate1` jako jediný zdroj, případné `EntryDate2`/`EntryDate3` zahazuje.

Z review 2026-04-29 vyplynula potřeba rozšířit oba modely. Obě úpravy jsou nezávislé, ale dotýkají se stejné Event aggregate, stejné migrace V001 a stejného edit form / detail UI — proto jsou v jedné change.

## Goals / Non-Goals

**Goals:**
- Cancel akce může nést volitelný důvod (text, max 500 znaků).
- Event může mít až 3 sekvenční deadlines; ORIS import populuje, co je k dispozici.
- UI: cancel dialog s polem „Důvod zrušení"; create/update form s 3 fields pro deadlines; detail akce vypíše všechny vyplněné deadlines a důvod zrušení; tabulka akcí ukazuje relevantní deadline + indikaci dalších termínů.
- Backward-compatible migrace: existující data s 1 deadline zůstávají platná (mapují se na `deadline1`).

**Non-Goals:**
- Rostoucí vklady / kategoriové ceny vázané na deadlines (Klabis dnes ceny neeviduje).
- Notifikace e-mailem o zrušení akce (samostatný follow-up).
- UI pro „chci se přihlásit na akci až po deadline1" workflow — všechny přihlášky pojedou stejnou cestou, jen lze upravit dokud poslední deadline neproběhl.
- Více než 3 deadlines (ORIS jich nepodporuje víc; KISS).

## Decisions

### Decision 1: Reprezentace více deadlines — value object `RegistrationDeadlines(deadline1, deadline2, deadline3)`

Místo `Optional<LocalDate>` pole přidám immutable record s validací invariantu.

```java
public record RegistrationDeadlines(
        Optional<LocalDate> deadline1,
        Optional<LocalDate> deadline2,
        Optional<LocalDate> deadline3
) {
    public RegistrationDeadlines {
        // deadline_n+1 requires deadline_n
        if (deadline2.isPresent() && deadline1.isEmpty()) { throw ... }
        if (deadline3.isPresent() && deadline2.isEmpty()) { throw ... }
        // sequential ordering: d1 <= d2 <= d3
        deadline1.ifPresent(d1 -> deadline2.ifPresent(d2 -> {
            if (d2.isBefore(d1)) throw ...;
        }));
        // ... atd.
    }

    public Optional<LocalDate> last() { /* d3 if present, else d2, else d1 */ }
    public Optional<LocalDate> nextRelevant(LocalDate today) { /* nejbližší budoucí */ }
    public boolean registrationsOpen(LocalDate today) { /* today <= last() */ }
}
```

Validační invarianty (sequenciality + monotonie) jsou v konstruktoru — domain zajišťuje konzistenci. `Event.create` / `Event.update` přijímá `RegistrationDeadlines`, ne tři samostatné parametry.

**Alternative considered:**
- *List<LocalDate> deadlines* — flexibilní, ale přidává složitost (kolik je `deadlines.size()` deadlines? co když user pošle nesetříděné?). Pevné max 3 ho nečiní lepším.
- *Tři samostatné Optional fieldy přímo na Event* — méně cohesion, validace invariantu mezi fieldy se rozsype napříč Event aggregate.

### Decision 2: Migrace dat — přejmenovat existující sloupec NEBO přidat nové

Aktuální sloupec `registration_deadline DATE` → buď přejmenovat na `registration_deadline_1` (DDL ALTER), nebo zachovat název a chápat ho jako deadline1.

**Volba:** zachovat název `registration_deadline` (= deadline1), přidat dva nové sloupce `registration_deadline_2`, `registration_deadline_3`. Důvody:
- Žádný DDL ALTER pro existující data.
- Memory `feedback_no_parallel_gradle.md` říká, že `V001__initial_schema.sql` se updatuje in-place (nepřidává se nová migrace) — toto je jednodušší.
- Backward-compatible: stávající Event records s vyplněným `registration_deadline` se po migraci načtou jako `RegistrationDeadlines(d1=..., d2=empty, d3=empty)`.

V kódu `EventMemento.toEvent()` builduje `RegistrationDeadlines` ze tří column hodnot.

### Decision 3: ORIS import — best-effort mapping

ORIS `Event.EntryDate1`/`2`/`3` se mapují na `deadline1`/`2`/`3`. Pokud `EntryDate2` není v ORIS feed, deadline2 zůstává empty. Žádná validace pořadí na ORIS straně — ORIS data jsou zdroj pravdy a do invariant se nepíše. Pokud by ORIS feed obsahoval nesetříděné termíny (defensivní programování), `RegistrationDeadlines` konstruktor by hodil výjimku → import by byl odmítnut. Otevřená otázka: zda v takovém případě fallback na nejranější (přijmout jen platnou subseries) nebo selhat. **Default: selhat hlasitě** (loguje, importer dostane error response a může to manuálně opravit) — zachová data integrity.

### Decision 4: UI deadline column v tabulce akcí — „aktuální + indikace"

Sloupec „Uzávěrka" zobrazuje:
- Pokud žádné deadline → prázdná buňka.
- Pokud 1 deadline → jen jeho datum (jako dnes).
- Pokud >1 deadlines → datum aktuálně relevantního (= nejbližší budoucí, nebo poslední, pokud všechny prošly) **+ malá ikona / badge „⊕"** s tooltip „Další termíny: ...".

Detail akce ve své sekci „Uzávěrky přihlášek" vypíše všechny vyplněné termíny chronologicky se značkou, který je aktuálně relevantní.

### Decision 5: Cancel reason — single text field, max 500 znaků

`Event.cancel(reason: Optional<String>)` zaakceptuje volitelný řetězec; uloží se do nového sloupce `cancellation_reason VARCHAR(500)`. UI cancel dialog má textarea s placeholder „Důvod zrušení (volitelné)" a počítadlem znaků.

Zobrazení:
- Detail akce v CANCELLED stavu: sekce „Akce byla zrušena" s textem důvodu, pokud je vyplněn.
- Tabulka akcí: pokud akce je CANCELLED s vyplněným důvodem, řádek nese tooltip „Zrušeno: <důvod>" na badge stavu.

**Alternative considered:**
- *Strukturovaný důvod (enum: weather / illness / low-attendance / other + free text)* — moc strukturované pro první iteraci. Free text postačuje a je univerzální. Pokud se ukáže, že stejné důvody se opakují, lze přidat presets jako enum později.

## Risks / Trade-offs

- **[Risk] Existující ORIS-importované akce s 1 deadline po nasazení neukáží badge „další termíny"** → Mitigation: badge je conditional na počtu vyplněných deadlines; pokud d2/d3 chybí, badge se nezobrazí. Žádná regrese.
- **[Risk] Validation rule "d2 requires d1" je restriktivní — co když organizátor chce jen "rozšířený" deadline bez prvního?** → Mitigation: tato situace je v praxi nepravděpodobná (deadlines představují "early bird → standard → late"). Pokud by se objevila, zvažujeme oslabení invariantu v samostatném proposalu.
- **[Trade-off] `cancellationReason: Optional<String>`** — Optional v doménovém modelu vs. nullable string v DB. Memory `backend-patterns` preferuje Optional v API agregátu, nullable v memento. Konzistentní s ostatními optional fields (`location`, `websiteUrl`).

## Migration Plan

1. **V001 update (H2-only)** — rename sloupce `registration_deadline` → `registration_deadline_1`; přidat `registration_deadline_2 DATE NULL`, `registration_deadline_3 DATE NULL`, `cancellation_reason VARCHAR(500) NULL`. Žádná Flyway migrace — projekt běží na H2 (vč. produkce) bez perzistentních dat, edituje se přímo DDL ve V001.
2. **Domain:** `RegistrationDeadlines` value object + nové `Event.cancel(reason)` overload (zachovat původní bez reason pro BC) + Event field změny.
3. **Persistence:** `EventMemento` rozšířit o 3 deadline columns + cancellation_reason.
4. **ORIS import:** rozšířit mapování o `EntryDate2`/`EntryDate3`.
5. **REST API:** create/update/cancel DTO + HAL forms templates.
6. **Frontend:** form fields, cancel dialog, detail page, table column.
7. **Lokalizace + dokumentace.**
8. **Smoke test po deployi:** vytvořit ORIS-imported akci se 3 deadliny, ověřit zobrazení; zrušit akci s důvodem, ověřit zobrazení důvodu v detailu.

## Open Questions

- ~~**V001 update vs. V004 nová migrace**~~ — **vyřešeno (2026-04-29):** production stále běží na H2 bez perzistentních dat, in-place update `V001__initial_schema.sql` je OK. Žádná separátní migrace. Sloupec `registration_deadline` se přejmenuje na `registration_deadline_1` přímo v DDL.
- **Notifikace e-mailem o zrušení** — out of scope, ale následný proposal by měl počítat s `cancellationReason` jako součástí e-mail těla.
- **„Aktuálně relevantní" deadline definice** — pokud jsou všechny v minulosti, ukazuje se „poslední". Alternativně by šlo řešit explicitním stavem (registrace uzavřeny). Default: poslední, member vidí v UI „uzávěrka 1.5.2025 (uzavřeno)" — KISS.
