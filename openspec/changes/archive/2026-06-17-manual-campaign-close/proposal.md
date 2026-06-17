## Why

Automatické ukončení kampaně probíhá každý den v 3:00 ráno po uplynutí hlasovacího deadlinu. Administrátor nemá způsob jak kampaň uzavřít okamžitě — například pro potřeby testování nebo mimořádného předčasného ukončení. Chybí manuální ovládání tohoto procesu.

## What Changes

- Na detail aktivní kampaně členských příspěvků se přidá akce „Uzavřít kampaň" (dostupná pouze pro MEMBERS:MANAGE)
- Akce spustí stejnou logiku jako automatické zpracování: zmrazení skupin, výpočet ročních poplatků, sankce pro členy bez volby
- Akce je dostupná pouze pokud kampaň ještě nebyla zpracována (nevzniká duplicitní zpracování)
- Hlasovací deadline zůstává beze změny — slouží jako historický údaj, nekopíruje datum manuálního uzavření

## Capabilities

### New Capabilities

- `membership-fee-campaign-manual-close`: Administrátor může okamžitě uzavřít aktivní kampaň kliknutím na tlačítko v detailu kampaně, bez čekání na automatické noční zpracování.

### Modified Capabilities

- `membership-fees`: Přidání akce uzavření kampaně do detailového pohledu aktivní kampaně (nový scénář).

## Impact

- **Backend**: Nový application port `ManualCampaignClosePort` (sekundární adapter volající sdílenou logiku zpracování z `CampaignEndProcessingPortImpl`); nový REST endpoint `POST /api/fee-selection-campaigns/{id}/close`; nová affordance v `FeeSelectionCampaignDetailsPostprocessor`
- **Frontend**: Tlačítko „Uzavřít kampaň" na stránce detailu aktivní kampaně, renderované z HAL+FORMS affordance
- **Bezpečnost**: Endpoint chráněn `MEMBERS_MANAGE` autoritou
