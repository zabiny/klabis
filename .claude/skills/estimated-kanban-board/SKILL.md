---
name: estimated-kanban-board
description: Aktualizuje nebo vytváří docs/implementation-status.html — kanban board který porovnává otevřené Github issues v milestonech MVP/core s aktivními OpenSpec specifikacemi a klasifikuje každý tiket jako TODO/IN_PROGRESS/COMPLETED. Aktualizuje i doprovodný markdown docs/implementation-status.md. Použij tento skill kdykoli uživatel řekne "aktualizuj implementation status", "refresh kanban", "update implementation board", "přehodnoť status implementace", "vygeneruj přehled implementace", nebo požádá o přehled kam se s projektem dostali vůči issues. Používej ho proaktivně i když uživatel přímo neřekne "kanban" — pokud chce audit/snapshot rozpracovanosti vůči Github issues, je to tento skill.
---

# Estimated Kanban Board

Skill, který vytváří a aktualizuje HTML kanban board (`docs/implementation-status.html`) zobrazující odhadnutý stav implementace Github issues vůči OpenSpec specifikacím.

## Kontext projektu

- **Repo**: `zabiny/klabis` (hardcoded v HTML template, neměnit)
- **Spec location**: `openspec/specs/*/spec.md` — aktivní specifikace (ignoruj `openspec/changes/`)
- **Milestones**: `MVP`, `core` — jen tyto dva zajímají
- **Output**: `docs/implementation-status.html` (kanban) + `docs/implementation-status.md` (zrcadlová markdown forma)

## Klasifikační pravidla

Každý otevřený issue dostane právě jeden z těchto statusů:

- **COMPLETED** — záměr tiketu je plně pokryt requirementem ve spec. Typicky existuje spec requirement, jehož scénáře přímo popisují to, co tiket chce. Pokud má tiket navíc label obsahující `BackendCompleted`, je to silný indikátor COMPLETED.
- **IN_PROGRESS** — částečně pokryto. Spec pokrývá hlavní use case ale některé podstatné aspekty tiketu chybí (např. tiket chce "odhlásit z akce + notifikaci" a spec má jen odhlášení). Také když je dostupné jen přes workaround (unregister + re-register místo edit).
- **TODO** — žádný odpovídající spec. Typicky celé tematické okruhy, které v `openspec/specs/` vůbec nefigurují: Finance (platby, příspěvky, dotace, slevy), Web/CMS (novinky, wiki, fotky, časopis, public stránky), Notifications (email, feed), ORIS/ČUS integrace (synchronizace), doprava, ubytování.

Rozhodnutí je úsudkové — čti záměr tiketu a hledej odpovídající requirement. Nestačí keyword match; posuzuj sémantiku.

## Workflow

Pracuj v této posloupnosti. Každý krok dokonči před dalším.

### 1. Ověř gh CLI auth

Spusť `gh auth status`. Pokud vrátí "token is invalid" nebo "not logged in", **zastav se** a požádej uživatele o spuštění `! gh auth login -h github.com` přímo v prompt pod tímto tool output (prefix `!` umožní interaktivní login přímo v session). Pokračuj, až auth projde.

### 2. Načti specifikace a issues paralelně

Vypiš všechny spec soubory:
```bash
ls openspec/specs/
```

Načti issues z obou milestonů (pošli oba `gh` příkazy v jednom message, aby běžely paralelně):
```bash
gh issue list --milestone MVP  --state open --limit 100 --json number,title,milestone,labels
gh issue list --milestone core --state open --limit 100 --json number,title,milestone,labels
```

Output `gh` příkazů může být velký — klidně skonči v `persisted-output`. Pokud ano, parsuj ho přes Python oneliner:
```bash
cat <persisted_file> | python3 -c "import json,sys; [print(f\"#{i['number']}|{i['title']}|{i['milestone']['title']}|{','.join([l['name'] for l in i['labels']])}\") for i in json.load(sys.stdin)]"
```

### 3. Přečti všechny spec soubory

Každý `openspec/specs/*/spec.md` přečti paralelně (více `Read` volání v jednom message). Specs jsou pravda o tom, co je definováno — klasifikace stojí a padá na jejich znalosti. Nepoužívej jen názvy souborů; čti obsah requirementů a scénářů.

### 4. Klasifikuj každý issue

Projdi každý issue a rozhodni status. Pro **COMPLETED** a **IN_PROGRESS** napiš 1–3 věty v **češtině**, které uvádějí konkrétní spec + requirement ("Spec `members` \"Member Update\" — ..."). Pro TODO nech `details: ""`.

Výstup si drž v paměti jako pole JSON objektů:
```json
{"id": 123, "title": "...", "milestone": "MVP"|"core", "status": "TODO"|"IN_PROGRESS"|"COMPLETED", "details": ""}
```

Řaď podle `id` sestupně v rámci celého pole (novější nahoru).

### 5. Aktualizuj HTML

Pokud `docs/implementation-status.html` neexistuje, vytvoř ho z template: `.claude/skills/estimated-kanban-board/assets/implementation-status.template.html`. Zkopíruj přes `cp` (template obsahuje funkční HTML/CSS/JS + prázdný JSON blok) a pak pokračuj jako při update.

Pokud `docs/implementation-status.html` existuje, **nepřepisuj celý soubor**. Zachováš tak případné styling tweaky, které uživatel provedl. Použij Python skript, který bezpečně nahradí **pouze** JSON blok a datový stamp v headeru:

```bash
python3 .claude/skills/estimated-kanban-board/scripts/update_board.py \
  --html docs/implementation-status.html \
  --data <path_to_tmp_json> \
  --snapshot "YYYY-MM-DD HH:MM CEST" \
  --total <počet_issues>
```

Kde `<path_to_tmp_json>` je soubor v `/mnt/ramdisk/klabis/` (nebo `$TMPDIR`) s kompletním JSON polem issues. Nepředávej JSON na command-line, protože bývá dlouhý; piš ho do souboru a skriptu dej cestu.

Skript:
- Najde `<script id="data" type="application/json">…</script>` a nahradí obsah
- Najde `SNAPSHOT · <date>` v header divu `.live` a aktualizuje datum
- Najde `Mapa <N otevřených>` v subtitle a aktualizuje počet
- Vyhodí nenulový exit kód, pokud nějaký ze tří anchor patternů nenajde (=template je poškozený/nekompatibilní)

### 6. Aktualizuj markdown

Regeneruj `docs/implementation-status.md` ze stejných dat. Struktura souboru:

```markdown
# Implementation status as of YYYY-MM-DD HH:MM CEST

Hodnocení je odvozeno výhradně z OpenSpec specifikací v `openspec/specs/` (archiv ignorován).
Issues s tématy, která ve specifikacích vůbec nefigurují (Finance, Web/CMS, CUS/ORIS integrace, Notifications, doprava, ubytování), jsou označeny jako **TODO**, protože neexistuje žádná implementovaná spec.

---

## <title>
ISSUE ID: #<id>
Milestone: <milestone>
Implementation status: **COMPLETED|IN_PROGRESS|TODO**
Details: <details or empty line for TODO>

---

## <next issue>
...
```

Řadit stejně jako v HTML (id desc).

### 7. Shrň uživateli

Krátce (≤100 slov) uveď:
- kolik issues celkem, rozbití podle statusu
- zásadní změny oproti předchozímu stavu (pokud `docs/implementation-status.md` existoval — porovnej)
- cestu k HTML a markdown souboru

## Proč tento skill existuje

Tato klasifikace je primárně **úsudková práce**, ne deterministický script. Spec soubory se mění, issues se přidávají, a rozlišit COMPLETED vs IN_PROGRESS vyžaduje přečíst obsah requirementu a záměr tiketu. Skript by to nezvládl spolehlivě. Role skillu je dát Claudovi strukturovaný pracovní postup, pravidla klasifikace a bezpečné utility pro update HTML, aby mohl výstup generovat rychle a konzistentně.

HTML template nechováme regenerovat každý update — uživatel si může upravit CSS nebo strukturu a těžce odpracované úpravy by se ztratily. Proto ta pravidla "edit, don't rewrite".

## Známé pasti

- **Velký gh output**: Odpověď `gh issue list` s 70+ tickety se snadno dostane do `persisted-output`. Počítej s tím a parsuj přes soubor/Python.
- **Diakritika v titulech**: Issues obsahují české znaky. Piš JSON s UTF-8 a nekoduj je přes `\uXXXX` escapes zbytečně.
- **Escape backticks v details**: Detaily často obsahují \`code\` spans. V JSON escape nepotřebuješ, ale v markdown ano; Python skript pro markdown by měl bacticky přebírat raw.
- **Snapshot date**: Vezmi z `date "+%Y-%m-%d %H:%M %Z"`. Nehádej.
- **Počet v subtitle**: `Mapa <N otevřených> Github issues` — musí souhlasit s delkou JSON pole.

## Soubory skillu

- `SKILL.md` — tento dokument
- `assets/implementation-status.template.html` — kompletní HTML šablona s prázdným JSON blokem, použije se když cílový soubor neexistuje
- `scripts/update_board.py` — Python skript pro bezpečnou aktualizaci HTML (nahrazení JSON bloku + date stamp + total count)
