# Design

## Context

Calendar sharing UI is driven entirely by the `calendarFeed` block in `frontend/src/localization/labels.ts`, consumed by `CalendarPage.tsx` (button, dialog title) and `CalendarFeedSection.tsx` (dialog body). Backend (`IcalTokenService`, `CalendarFeedToken`), API paths (`/api/me/ical-token`) and the `ical-token` HAL link carry no user-visible text. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- All user-visible wording in the dialog uses "odkaz"; entry point is "Zpřístupnit kalendář".

**Non-Goals:**
- Renaming code identifiers, `calendarFeed` label key, component names, API paths, DB entities or the HAL link `ical-token`.
- Any functional change.

## Decisions

**Labels only, keep identifiers.** Change string values in `labels.ts`; keep `calendarFeed` key and component names. Renaming internals would touch backend/tests for zero user benefit (KISS).

**Proposed wording** (Czech):

| Key | New value |
|---|---|
| `button`, `dialogTitle` | Zpřístupnit kalendář |
| `createButton` | Vytvořit odkaz pro kalendář |
| `regenerateButton` | Vytvořit nový odkaz |
| `confirmRegenerateTitle` | Vytvořit nový odkaz? |
| `confirmRegenerateAction` | Vytvořit |
| `confirmRegenerateMessage` | Předchozí odkaz přestane fungovat. Všechna zařízení, která ho používají, se přestanou synchronizovat, dokud nezadáte nový odkaz. |
| `generatedAt` | Odkaz vytvořen |
| `fullUrlLabel` | Odkaz na kalendář |
| `fullUrlNote` | Tento odkaz uvidíte pouze nyní. Uložte ho nebo přidejte do svého kalendáře. Pokud ho ztratíte, vytvořte nový odkaz. |
| `maskedUrlLabel` | Odkaz na kalendář (zamaskovaný) |
| `helpText` | Přidejte tento odkaz do svého kalendáře — … (rest unchanged, "URL" → "odkaz") |
| `helpMyScheduleNote` | Kalendář obsahuje stejné akce jako filtr „Můj rozvrh" … |

`intro` and `help*` per-app lines ("Přidat URL") stay if they refer to the external app's own UI wording; only own-wording is changed.

## Risks / Trade-offs

- Existing tests match old strings → update in the same task.
- Backend `static/assets` bundle holds old strings until the next production frontend publish → handled by the regular `publish-frontend-resources` step, not part of this change.
