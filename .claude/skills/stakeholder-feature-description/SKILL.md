---
name: stakeholder-feature-description
description: Vytváří standalone HTML stránku s product overview popisem konkrétní části aplikace Klabis pro product ownera a stakeholdery. Použij tento skill kdykoli uživatel řekne "vytvoř product overview", "popiš funkcionalitu X pro stakeholdery", "udělej stránku pro product ownera", "vytvoř přehled modulu X", "zdokumentuj feature X pro vedení", nebo chce HTML dokument popisující jak nějaká část Klabisu funguje z byznys pohledu.
---

# Stakeholder Feature Description

Skill vytváří standalone HTML stránku (`docs/<feature>-overview.html`) popisující funkcionalitu zadané části Klabisu pro smíšené publikum: vedení klubu, správce a finanční stakeholdery.

## Parametry (pevně dané)

Tyto volby jsou baked-in — neptej se na ně:

- **Účel**: Feature overview — co systém umí, pravidla, kdo co může
- **Cílová skupina**: Smíšené publikum — vedení klubu, správci, finance
- **Hloubka**: Byznys workflow + klíčová pravidla a omezení; bez implementačních detailů (žádné třídy, REST endpointy, technické termíny)
- **Jazyk obsahu**: Česky
- **Vizuální styl**: Wiki/interní dokumentace (viz sekce HTML šablona níže)

Uživatel zadá jen: **kterou část aplikace popsat** (např. "membership fees", "events", "members").

---

## Workflow

### Krok 1: Průzkum codebase

Prozkoumej zadanou oblast ve třech zdrojích paralelně:

**OpenSpec specs** (`openspec/specs/<feature>/spec.md`):
- Jaké scénáře jsou specifikovány? Co musí systém umět?
- Jaká jsou byznys pravidla a omezení (deadline, limity, podmínky)?
- Kdo jsou aktéři (admin, člen, systém)?

**Backend doménový model** (`backend/src/main/java/com/klabis/<feature>/domain/`):
- Jaké jsou aggregate roots a jejich stavy (enum fields)?
- Jaké jsou domain eventy?
- Existují stavové přechody (status field s více hodnotami → kandidát na stavový diagram)?
- Existují schedulery nebo automatické procesy?

**Frontend stránky** (`frontend/src/pages/<feature>/`):
- Jaké stránky/sekce existují pro admin a pro člena?
- Jaký je hlavní workflow z pohledu uživatele?

### Krok 2: Navrhni strukturu a diagramy

Na základě průzkumu navrhni:

**Struktura dokumentu** — seznam 5–8 sekcí. Každá sekce má:
- Název (česky)
- 1–2 věty co obsahuje

Typické sekce (přizpůsob dle modulu):
- Úvod — co funkce je, proč existuje, kdo ji používá
- [Správa hlavní entity] — CRUD, pravidla, omezení
- [Hlavní workflow] — jak proces probíhá krok za krokem
- [Akce člena] — co může dělat řadový člen
- [Správa adminem] — co může dělat správce navíc
- Automatizace — co systém dělá sám (schedulery, eventy)
- Audit & historie — záznamy, přehledy

**Návrh diagramů** — pro každý navrhovaný diagram uveď:
- Typ: `flowchart TD` (průběh procesu) nebo `stateDiagram-v2` (životní cyklus entity)
- Co zobrazuje (1 věta)
- Proč je užitečný pro stakeholdery

Kandidáti na diagram:
- Entita s vícehodnotovým status fieldem → stavový diagram
- Vícekrokový proces přes čas (deadline, scheduler) → flowchart timeline
- Workflow s větvením (člen vs. admin, podmínky) → flowchart

### Krok 3: Dvě otázky pro uživatele

Polož obě otázky najednou v jedné zprávě:

**Otázka A — Struktura:**
Předlož navržené sekce jako číslovaný seznam s krátkým popisem každé. Zeptej se zda vyhovuje nebo chce něco upravit.

**Otázka B — Diagramy:**
Předlož navržené diagramy s popisem co každý zobrazuje. Zeptej se zda chce všechny, jen část, nebo jiné.

Vyčkej na odpověď před generováním HTML.

### Krok 4: Vygeneruj HTML stránku

Po potvrzení struktury vytvoř soubor `docs/<feature-name>-overview.html`.

---

## HTML šablona (technické požadavky)

```html
<!DOCTYPE html>
<html lang="cs">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>[Název funkce] — Klabis Product Overview</title>
  <script src="https://cdn.tailwindcss.com"></script>
  <script src="https://unpkg.com/lucide@latest/dist/umd/lucide.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
</head>
```

**Layout:**
- Fixed sidebar vlevo, šířka ~240px (`w-60`), výška 100vh, scroll
- Hlavní obsah vpravo s paddingem, scrolluje nezávisle
- Mobilní layout: hamburger tlačítko, sidebar jako overlay

**Sidebar:**
- Logo/název projektu nahoře ("Klabis")
- Podtitulek s názvem stránky (např. "Členské příspěvky") a "Product overview"
- Navigační položky: každá má Lucide ikonu + název sekce
- Aktivní sekce zvýrazněna (indigo pozadí) pomocí IntersectionObserver
- Smooth scroll na klik

**Barevná paleta:**
- Pozadí stránky: `bg-slate-50`
- Sidebar: `bg-white` s border pravou stranou (`border-slate-200`)
- Hlavní obsah: bílé karty (`bg-white rounded-lg shadow-sm`)
- Primární akcent: `indigo-600` (aktivní nav, nadpisy sekcí, ikony)
- Text: `slate-700` (tělo), `slate-900` (nadpisy)
- Callout boxy: `bg-blue-50 border-l-4 border-blue-400` pro důležité informace
- Upozornění/sankce: `bg-amber-50 border-amber-400`

**Obsah sekcí:**
- Každá sekce má číslovaný heading s ikonou (`<h2>`)
- Úvodní odstavec vysvětlující co sekce pokrývá
- Tabulky pro přehledy (sloupce jasně pojmenované česky)
- Callout boxy pro klíčová pravidla a omezení
- Příkladová data v tabulkách (smyšlená ale realistická)
- Mermaid diagramy v ohraničeném kontejneru s nadpisem

**Inicializace JS:**
```javascript
mermaid.initialize({ startOnLoad: true, theme: 'neutral' });
lucide.createIcons();
// IntersectionObserver pro aktivní sekci v sidebaru
```

**Footer:** "Klabis © [rok]"

---

## Pravidla pro obsah

- **Vyhýbej se technickým termínům**: žádné "aggregate root", "REST endpoint", "scheduler", "domain event". Místo toho: "systém automaticky", "každý den systém kontroluje", "po termínu systém provede"
- **Příkladová data**: tabulky mají realistická příkladová data (jména úrovní, částky v CZK, data)
- **Callout boxy** pro: pravidla s jedinou možnou hodnotou ("jen jedna aktivní kampaň"), nevratné akce ("odhlášení z eventů se neobnoví"), automatické procesy
- **Aktivní hlas**: "Admin vytvoří kampaň" místo "Kampaň je vytvořena adminem"
- **Konkrétní**: "do 31. března" místo "do stanoveného termínu" kde to dává smysl

---

## Příklad (membership fees)

Výsledná stránka: `docs/membership-fees-overview.html`

Sekce: Úvod → Katalog úrovní → Kampaň výběru → Volba člena → Správa adminem → Automatizace → Audit & historie

Diagramy:
1. `flowchart TD` — průběh kampaně od publikování přes deadline po sankce
2. `stateDiagram-v2` — životní cyklus publikované úrovně: EDITABLE → FROZEN
