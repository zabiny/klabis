## 1. Rename FeeYearPublication → FeeSelectionCampaign (backend)

> **Pro přejmenování tříd použít JetBrains IDEA MCP `rename_refactoring`** (zachová všechny reference). NEpoužívat ruční find/replace. Pozn.: nesmí běžet v git worktree (viz feedback `jetbrains-tools`).

- [ ] 1.1 Přejmenovat doménový objekt `FeeYearPublication` → `FeeSelectionCampaign` a `FeeYearPublicationId` → `FeeSelectionCampaignId` (rename refactoring)
- [ ] 1.2 Přejmenovat `FeeYearPublicationRepository` → `FeeSelectionCampaignRepository`, `FeeYearPublicationManagementPort` → `FeeSelectionCampaignManagementPort`, `FeeYearPublicationManagementService` → `FeeSelectionCampaignManagementService` (rename refactoring)
- [ ] 1.3 Přejmenovat REST controller a DTO třídy (`FeeYearPublication*` → `FeeSelectionCampaign*`) rename refactoringem; URL path `/publications` → `/campaigns` upravit ručně
- [ ] 1.4 Přejmenovat exception třídy rename refactoringem (`FeeYearPublicationNotFoundException` → `FeeSelectionCampaignNotFoundException`, `DuplicateYearPublicationException` → `ActiveCampaignExistsException`)
- [ ] 1.5 Aktualizovat DB migraci: přejmenovat tabulku `fee_year_publication` → `fee_selection_campaign`
- [ ] 1.6 Aktualizovat persistence (memento, JDBC repository) pro nový název tabulky
- [ ] 1.7 Spustit testy a opravit kompilační chyby

## 2. Doménová logika: changeDeadline

- [ ] 2.1 Napsat testy pro `changeDeadline(LocalDate newDeadline, LocalDate today)`: deadline v budoucnosti OK, deadline = dnes OK, deadline v minulosti → výjimka, uzavřená kampaň → výjimka
- [ ] 2.2 Implementovat `changeDeadline(LocalDate newDeadline, LocalDate today)` na doménovém objektu

## 3. Validace při zakládání kampaně

- [ ] 3.1 Napsat testy pro `FeeSelectionCampaignManagementService.publishYear()`: deadline v minulosti → `DeadlineNotInFutureException`, existuje aktivní kampaň → `ActiveCampaignExistsException`
- [ ] 3.2 Přidat `findActive(LocalDate today)` metodu na `FeeSelectionCampaignRepository`
- [ ] 3.3 Implementovat validace v `publishYear()`: deadline > today, žádná aktivní kampaň
- [ ] 3.4 Přidat `DeadlineNotInFutureException` a mapování na `400` v exception handleru

## 4. Aplikační vrstva: changeDeadline operace

- [ ] 4.1 Napsat testy pro `changeDeadline` operaci v service: delegace na doménový objekt, uložení
- [ ] 4.2 Přidat `changeDeadline(FeeSelectionCampaignId id, ChangeDeadlineCommand command)` na port a implementovat v service

## 5. REST API: nová affordance

- [ ] 5.1 Napsat controller test pro `PATCH /campaigns/{id}/deadline`: úspěch, deadline v minulosti (400), kampaň uzavřená (409)
- [ ] 5.2 Implementovat endpoint `PATCH /campaigns/{id}/deadline` s affordancí `changeDeadline` (přítomna pouze pro aktivní kampaně)
- [ ] 5.3 Přidat validaci `ActiveCampaignExistsException` → `409 Conflict` v exception handleru
- [ ] 5.4 Spustit všechny backend testy

## 6. Frontend: přejmenování a nové akce

- [ ] 6.1 Přejmenovat frontend stránky rename refactoringem (JetBrains IDEA MCP): `FeeYearPublicationsPage` → `FeeSelectionCampaignsPage`, `FeeYearPublicationDetailPage` → `FeeSelectionCampaignDetailPage`
- [ ] 6.2 Aktualizovat `labels.ts`: "Vypsání pro rok" → "Kampaň volby členského příspěvku", všechny odvozené texty
- [ ] 6.3 Aktualizovat routy v `App.tsx` pro nové URL `/campaigns`
- [ ] 6.4 Přidat UI akci "Změnit deadline" na detail stránce kampaně (HalFormButton pro affordanci `changeDeadline`)
- [ ] 6.5 Spustit frontend testy a opravit

## 7. Spec aktualizace a E2E test

- [ ] 7.1 Syncnout delta spec do hlavní `openspec/specs/membership-fees/spec.md` (spustit `openspec sync-specs`)
- [ ] 7.2 Playwright E2E test KOMPLETNÍHO životního cyklu kampaně dle celé `membership-fees` specifikace — nejen změn z tohoto proposalu. POZOR: tato funkcionalita ještě neprošla E2E testem a očekává se, že část bude nefunkční; nalezené chyby reportovat a opravit (případně rozhodnout o follow-up):
  - Zakládání kampaně administrátorem (výběr úrovní, deadline)
  - Validace při zakládání: odmítnutí při existující aktivní kampani, deadline pouze v budoucnosti
  - Změna deadline aktivní kampaně (vč. odmítnutí data v minulosti)
  - Výběr úrovně členem před deadline + změna výběru
  - Předvyplnění loňské úrovně jako default
  - Zobrazení aktuální úrovně na profilu člena
  - Emergency assignment administrátorem (i po deadline)
  - Editace published level snapshotu (před prvním surcharge)
  - Detail kampaně a seznam kampaní (HAL navigace, affordance)
