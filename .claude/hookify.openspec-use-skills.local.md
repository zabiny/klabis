---
name: openspec-use-skills
enabled: false
event: file
conditions:
  - field: file_path
    operator: contains
    pattern: openspec/
action: warn
---

**Přímá úprava OpenSpec souborů je zakázána.**

Soubory v `./openspec/` smíš upravovat pouze prostřednictvím příslušných skillů:

- `openspec-new-change` — nový change proposal
- `openspec-propose` — vytvoření návrhu
- `openspec-ff-change` — rychlé vytvoření artefaktů
- `openspec-apply-change` — implementace tasků
- `openspec-sync-specs` — synchronizace specs
- `openspec-archive-change` — archivace change
- `openspec-ext:implement-proposal` — implementace přes subagenty
- `openspec-ext:task-queue` / `openspec-ext:task-queue-process` — fronta tasků

Použij správný skill místo přímé editace souboru.
