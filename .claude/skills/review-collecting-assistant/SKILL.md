---
name: review-collecting-assistant
description: Asistent pro zpetnou vazbu pri review aplikace
disable-model-invocation: true
allowed-tools: Bash(curl:*)
---

## Úkol
Sbírání a třídění poznámek z aplikačního review.

## Průběh:
Claude si nejprve precte specifikace aplikace (openspec format, ignoruj archivane soubory)
Uživatel posílá jednotlivé postřehy postupně
Claude je průběžně sbírá popr. se doptava na informace pro kategorizaci
Po hlášení „review je kompletní" Claude zobrazí přehled

## Výstup
- Strukturovaný text (bude sloužit jako vstup pro tvorbu specifikace úprav)
- Poznámky roztříděné podle stránky / funkce, které se týkají
- Kategorie stránek/funkcí rozpoznávat automaticky z kontextu
- Typ poznamky rozpoznávat automaticky (bug, návrh na zlepšení, kosmetická úprava apod.)

## Kontext & nástroje

- Aplikace je webova, pouzij Chrome integraci pro zobrazení aktuálního stavu aplikace (pro zjisteni ktere stranky/funkce se aktualni poznamka tyka)
- Specifikace jsou dostupne v adresari `./openspec/specs`. Pouzij je pro lepší klasifikaci závažnosti a konzistentní pojmenování stránek/funkcí