## 1. Backend — extrakce sdílené logiky zpracování

- [x] 1.1 Extrahovat metody `processPublication`, `freezeGroups`, `chargeYearlyFees` z `CampaignEndProcessingPortImpl` do nové třídy `CampaignProcessor` (balíček `application`)
- [x] 1.2 Upravit `CampaignEndProcessingPortImpl` aby delegoval na `CampaignProcessor`
- [x] 1.3 Ověřit, že stávající testy `CampaignEndProcessingPortTest` stále prochází

## 2. Backend — nový port a endpoint pro manuální uzavření

- [ ] 2.1 Napsat test pro `ManualCampaignClosePort.closeCampaign(campaignId)` — scénáře: úspěšné uzavření, 409 při opakovaném volání
- [ ] 2.2 Vytvořit rozhraní `ManualCampaignClosePort` (`@PrimaryPort`) s metodou `closeCampaign(FeeSelectionCampaignId)`
- [ ] 2.3 Implementovat `ManualCampaignClosePortImpl` s využitím `CampaignProcessor`
- [ ] 2.4 Napsat test pro `POST /api/fee-selection-campaigns/{id}/close` v `FeeSelectionCampaignControllerTest` — scénáře: 204 pro aktivní kampaň, 409 pro již uzavřenou, 403 bez autority
- [ ] 2.5 Přidat endpoint `POST /{id}/close` do `FeeSelectionCampaignController` (`@HasAuthority(MEMBERS_MANAGE)`, vrací 204)

## 3. Backend — HAL affordance na detailu kampaně

- [ ] 3.1 Přidat affordance `close` na self-link v `FeeSelectionCampaignDetailsPostprocessor` — podmíněnou `!campaign.isClosed(today) && campaign.getDeadlineProcessedAt() == null`
- [ ] 3.2 Ověřit v controlleru testu, že affordance je přítomna pouze pro aktivní nezpracované kampaně

## 4. Frontend — tlačítko uzavření kampaně

- [ ] 4.1 Přidat tlačítko „Uzavřít kampaň" na stránku detailu kampaně renderované z HAL+FORMS affordance `close`
- [ ] 4.2 Ověřit, že tlačítko se nezobrazí na detailu uzavřené kampaně
