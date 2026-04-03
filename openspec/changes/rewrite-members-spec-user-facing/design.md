## Context

Specs v `openspec/specs/` vznikaly postupně a kopírovaly styl starších API-centrických specifikací. Referenční implementace správného stylu je `user-groups` spec, která vznikla záměrně jako vzor. Tento design popisuje přístup k sjednocení zbývajících specs.

## Goals / Non-Goals

**Goals:**
- Sjednotit všechny specs do user-facing stylu (vzor: `user-groups/spec.md`)
- Přesunout technické API požadavky do `non-functional-requirements` capability
- Zachovat 100 % funkčních požadavků — mění se pouze forma zápisu
- Vytvořit a dodržet `openspec-writing-guide` skill jako autoritativní pravidla pro budoucí specs

**Non-Goals:**
- Žádné změny funkčních požadavků
- Žádné změny v kódu (backend ani frontend)
- Sjednocení jazyka (specs zůstávají anglicky, jak jsou nyní)

## Decisions

### Rozhodnutí 1: Jedna capability `non-functional-requirements` pro všechny technické detaily

**Proč:** Technické API požadavky (HAL+FORMS, ISO-8601, HATEOAS link structure) jsou sdílené přes všechny bounded contexty — duplicovat je v každé spec by bylo náchylné k nekonzistenci.

**Alternativa:** Každá spec má vlastní sekci "Technical Notes" → zamítnuto, protože mísí uživatelský a technický pohled.

### Rozhodnutí 2: Reorganizace podle UI kontextů (ne API endpointů)

**Proč:** Uživatel přemýšlí v kontextu stránek a akcí, ne v kontextu HTTP metod. Organizace podle UI kontextů zlepšuje čitelnost a přirozeněji odpovídá na otázku "co se stane, když uživatel dělá X".

**Alternativa:** Zachovat stávající strukturu Requirements a jen přepsat scénáře → zamítnuto, protože výsledek by byl stále API-centrický v organizaci.

### Rozhodnutí 3: Přepis po jedné spec, ne celý naráz

**Proč:** Každá spec má jiný rozsah a stav — přepis po jedné umožňuje review a snižuje riziko ztráty požadavků.

```mermaid
graph LR
    members --> events
    events --> event-registrations
    event-registrations --> calendar-items
    calendar-items --> users
    users --> users-authentication
    users-authentication --> email-service
    email-service --> member-permissions-dialog
    member-permissions-dialog --> server-configuration
    server-configuration --> non-functional-requirements
```

### Rozhodnutí 4: `openspec-writing-guide` skill jako autoritativní pravidla

**Proč:** Bez formalizovaných pravidel se nový styl nedodrží konzistentně v budoucích specs. Skill zajišťuje, že pravidla jsou dostupná při každém psaní spec.

## Risks / Trade-offs

- **[Riziko] Ztráta funkčního požadavku při přepisu** → Mitigation: Při přepisu každé spec projít původní spec sekci po sekci a ověřit, že každý funkční požadavek má odpovídající user-facing scénář.
- **[Riziko] Nejednoznačnost co je "technický detail" vs. "funkční požadavek"** → Mitigation: `openspec-writing-guide` obsahuje explicitní checklist; hraniční případy konzultovat s uživatelem.
- **[Trade-off] `non-functional-requirements` spec bude velká** → Přijatelné — jde o referenční dokumentaci, ne operativní spec.
