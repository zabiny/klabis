---
name: self-contained-registration-dialog
description: EventRegistrationDialog bere jen Link|null; mode/šablonu odvozuje z affordancí reprezentace přes useRegistrationDialogData; nový label noRegistrationAffordance
metadata:
  type: project
---

Frontend slice openspec změny `self-contained-registration-dialog` (implementováno 2026-09-02): `EventRegistrationDialog` přijímá jediný datový vstup `registration: Link | null` (null = zavřeno). Hook `useRegistrationDialogData` zřetězuje GET registrace → `_links.event` → GET eventu; mode = `_templates.registerForEvent` ⇒ 'new', jinak `editRegistration` ⇒ 'edit', jinak error Alert bez submitu.

**Why:** Volající (detail, seznam, dashboard) nesmí vědět nic o registračním formuláři; režim řídí hypermediální affordance, ne prop volajícího. Vyžaduje backend prefill s `registerForEvent` (již hotové).

**How to apply:**
- Chybějící affordance → label `labels.events.registrationModal.noRegistrationAffordance` (přidán; `prefillLoadError` zůstal jen pro fetch chyby — tým-leadovo zadání zmiňovalo prefillLoadError pro oba případy, rozdělení je vědomé rozhodnutí).
- Reset formulářového stavu: efekt s deps na jednotlivých `initialValues.*` polích (ne na objektu — nová identita každý render by mazal validační chyby při submitu).
- Na fetch-chybu dialog stále renderuje SI pole + footer (portovaný test), na chybějící affordanci ne.
- Volající drží v state `Link | null`; na EventDetailPage jsou obě instance dialogu (edit + new) mountované trvale.
- Testovací idiom: `vi.mock('<rel>/api/authorizedFetch')` + dispatch na URL pro GETy a na `options?.method` pro zápisy; EventDetailPage.test má dispatch mock přímo ve vi.mock factory (data definovaná uvnitř factory kvůli hoistingu).
