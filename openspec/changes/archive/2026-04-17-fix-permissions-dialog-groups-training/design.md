## Context

Dialog správy oprávnění zobrazuje oprávnění jako seznam přepínačů s českými popisky. Seznam oprávnění je definován na frontendu — pro každé oprávnění existuje popisek a popis. Oprávnění `GROUPS:TRAINING` v tomto seznamu chybí, přestože backend ho podporuje.

## Goals / Non-Goals

**Goals:**
- `GROUPS:TRAINING` je zobrazeno v dialogu správy oprávnění jako přepínač s českým popiskem a popisem

**Non-Goals:**
- Změna logiky ukládání oprávnění
- Přidání dalších oprávnění mimo `GROUPS:TRAINING`

## Decisions

### Frontend: přidání položky do statického seznamu oprávnění

Seznam oprávnění v dialogu je staticky definován na frontendu (mapování kód oprávnění → popisek + popis). Stačí přidat položku pro `GROUPS:TRAINING`.

Český popisek: **"Správa tréninkových skupin"**
Popis: **"Umožňuje vytvářet a spravovat tréninkové skupiny a jejich členy."**

## Risks / Trade-offs

- Nízké riziko — jde o čistě aditivní změnu na frontendu bez dopadu na ostatní funkce.
