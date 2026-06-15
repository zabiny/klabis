# Proposal: Product overview stránka — modul Události (Events)

## Průzkum codebase

### Zdroje prozkoumány

- `openspec/specs/events/spec.md` — kompletní specifikace, 1050+ řádků
- `backend/src/main/java/com/klabis/events/domain/` — doménový model (Event, EventStatus, EventRegistration, EventType, CategoryPreset, RegistrationDeadlines, EventRanking, Money, aj.)
- `frontend/src/pages/events/` — EventsPage, EventDetailPage, EventTypesPage, CategoryPresetsPage, AccommodationListPage

### Klíčové byznys koncepty zjištěné průzkumem

**Entity a jejich stavy:**
- `Event` — agregátní root s fieldy: název, datum, místo, pořadatel, web, vedoucí (coordinator), typ, kategorie, termíny přihlášení (1–3), hodnocení soutěže (ranking), základní startovné, status, důvod zrušení, přihlášky
- `EventStatus` — 4 stavy: DRAFT → ACTIVE → FINISHED nebo CANCELLED (přechod do FINISHED je automatický)
- `EventType` — katalog typů akcí (Pohárový závod, Trénink, apod.) zobrazovaný jako barevný odznak
- `CategoryPreset` — šablony kategorií pro rychlé vyplnění formuláře
- `EventRegistration` — přihláška člena na akci (SI číslo, kategorie)

**Klíčové procesy:**
- Ruční tvorba akce (manažer)
- Import ze systému ORIS (dávkový, max N akcí najednou)
- Synchronizace z ORIS — per-event nebo hromadná (všechny budoucí ORIS akce)
- Přihlašování/odhlašování členů (podmíněno statusem a termíny)
- Automatické dokončení akcí po datu konání

**Aktéři:**
- Manažer akcí (oprávnění EVENTS:MANAGE) — vytváří, edituje, publikuje, ruší, importuje, synchronizuje
- Člen klubu — prohlíží seznam, přihlašuje/odhlašuje se, filtruje "Moje přihlášky"
- Systém — automaticky dokončí ACTIVE akce po datu konání

---

## Navržená struktura dokumentu

### 1. Úvod — Co jsou Události
Stručný popis modulu: co umožňuje, kdo ho používá (manažeři akcí a členové), jaké typy akcí se evidují (závody, tréninky, pohárové série). Přehledná tabulka rolí a jejich oprávnění.

### 2. Přehled a filtrování akcí
Popis seznamu akcí — co vidí člen, co navíc vidí manažer (status sloupec). Filtrování: rok, časové okno (Budoucí / Proběhlé / Vše), fulltext, typ akce, vedoucí, Moje přihlášky. Pravidla řazení (nadcházející vzestupně, proběhlé sestupně).

### 3. Životní cyklus akce
Čtyři stavy akce od návrhu po dokončení nebo zrušení. Kdo provádí jaké přechody. Pravidlo automatického dokončení. Možnost uvést důvod zrušení. Callout: přechod do FINISHED provádí systém automaticky — manažer ho nemůže spustit ručně.

### 4. Přihlašování členů
Kdy jsou přihlášky otevřené (status ACTIVE + datum + termíny). Systém až tří registračních termínů — jak fungují sekvenčně. Co se stane po uplynutí posledního termínu. Callout: odhlásit se lze jen dokud jsou přihlášky otevřené.

### 5. Správa akcí manažerem
Tvorba akce ručně (povinná pole, volitelná pole). Editace v DRAFT a ACTIVE. Co je možné u konkrétních statusů (tlačítka Upravit / Publikovat / Zrušit). Přiřazení typu akce a kategorií (vč. šablon kategorií).

### 6. Import a synchronizace z ORIS
Co je ORIS a proč se využívá. Dávkový import (výběr více akcí najednou, limit, výsledkový souhrn). Automatické mapování typu akce, termínů přihlášení, hodnocení soutěže a základního startovného z ORIS. Synchronizace jedné akce i hromadná synchronizace všech budoucích ORIS akcí.

### 7. Typy akcí a katalogy
Katalog typů akcí (Pohárový závod, Trénink, …) — jak se zobrazuje jako barevný odznak. Šablony kategorií (Category Presets) — rychlé vyplnění kategorií ve formuláři. Správa obou katalogů adminem.

### 8. Hodnocení soutěže a startovné
Volitelné atributy akce — hodnocení (ranking série jako Regionální žebříček, Pohár ČR) a základní startovné. Jak se importují z ORIS, jak je může manažer opravit ručně.

---

## Navržené diagramy

### Diagram 1 — Životní cyklus akce (stateDiagram-v2)
**Typ:** `stateDiagram-v2`
**Co zobrazuje:** Čtyři stavy akce (Návrh, Aktivní, Dokončeno, Zrušeno) a přechody mezi nimi — kdo nebo co každý přechod vyvolá (manažer nebo systém automaticky).
**Proč je užitečný:** Stakeholdéři okamžitě vidí, co lze s akcí v daném stavu dělat a co je nevratné (Zrušeno, Dokončeno jsou koncové stavy). Vizuálně jasně odděluje manuální a automatické přechody.

### Diagram 2 — Průběh přihlašování s termíny (flowchart TD)
**Typ:** `flowchart TD`
**Co zobrazuje:** Jak systém rozhoduje o dostupnosti přihlášky — od stavu akce přes datum konání po registrační termíny (1., 2., 3.). Větví se dle toho, zda je nejbližší budoucí termín stále před námi nebo zda již všechny prošly.
**Proč je užitečný:** Pravidla tří termínů jsou pro vedení neintuitivní. Vývojový diagram v jednom pohledu ukazuje, kdy přihlášky otevřít a kdy se automaticky uzavřou — bez nutnosti číst specifikaci.

### Diagram 3 — Průběh importu z ORIS (flowchart TD)
**Typ:** `flowchart TD`
**Co zobrazuje:** Kroky dávkového importu z ORIS — od otevření dialogu, přes výběr akcí (limit, zobrazení jen neimportovaných), spuštění importu, zpracování každé akce samostatně, až po výsledkový souhrn (úspěch / chyba per akce).
**Proč je užitečný:** Import z ORIS je hlavní způsob, jak se akce do systému dostávají. Diagram manažerům i vedení klubu ukazuje, že jedna chyba nepotopí celou dávku, a vysvětluje, proč jsou některé akce v nabídce a jiné ne (filtr již importovaných).

---

## Dvě otázky pro uživatele (jak by byly položeny)

### Otázka A — Struktura

Navrhuji tuto strukturu dokumentu (8 sekcí):

1. **Úvod** — co modul je, kdo ho používá, přehled rolí
2. **Přehled a filtrování akcí** — seznam akcí, filtry, řazení, co vidí člen vs. manažer
3. **Životní cyklus akce** — 4 stavy, přechody, automatické dokončení, zrušení s důvodem
4. **Přihlašování členů** — kdy jsou přihlášky otevřené, jak fungují 1–3 registrační termíny
5. **Správa akcí manažerem** — tvorba, editace, publikování, rušení, přiřazení typů a kategorií
6. **Import a synchronizace z ORIS** — dávkový import, mapování dat, hromadná synchronizace
7. **Typy akcí a katalogy** — katalog typů, šablony kategorií, správa adminem
8. **Hodnocení soutěže a startovné** — ranking, základní startovné, import z ORIS vs. ruční editace

Vyhovuje ti tato struktura, nebo chceš nějakou sekci přidat, sloučit nebo vynechat?

### Otázka B — Diagramy

Navrhuji tři diagramy:

1. **Životní cyklus akce** (stavový diagram) — zobrazuje přechody mezi stavy Návrh → Aktivní → Dokončeno / Zrušeno a kdo každý přechod vyvolá. Pomáhá pochopit, co je nevratné a co dělá systém sám.

2. **Průběh přihlašování s termíny** (vývojový diagram) — ukazuje, jak systém rozhoduje, zda jsou přihlášky otevřené, včetně pravidel pro 1–3 registrační termíny. Klíčové pro pochopení funkce termínů.

3. **Průběh importu z ORIS** (vývojový diagram) — zachycuje celý tok dávkového importu od výběru akcí po výsledkový souhrn s výsledkem pro každou akci zvlášť.

Chceš všechny tři diagramy, nebo jen část? Případně chceš jiný typ vizualizace (např. timeline místo vývojového diagramu)?
