> **Pořadí vůči jiným changům:** Tento change vlastní tvar `EventCategory` včetně `fee`. Implementovat **před** `event-registration-pricing` a po dokončení upravit jeho D1/D2 na odkaz sem (viz 6.4).

> **TDD:** Každý úkol s produkčním kódem začíná failing testem (Red → Green → Refactor). Doména 100 % pokrytí, zbytek >80 %.

## 1. Kategorie se stabilní identitou (end-to-end)

Vertikální řez: doména → persistence → API → frontend, aby kategorie měla ID a šla vytvořit/editovat. Registrace zatím zůstává name-based.

- [x] 1.1 Test: `EventCategoryId` generuje a rovná se podle hodnoty; `EventCategory` vyžaduje název a přijímá `orisId`/`fee` jako volitelné
- [x] 1.2 Implementovat `EventCategoryId` (value object) a `EventCategory` (entity: id, orisId, name, fee)
- [x] 1.3 Test: `Event` odmítne dvě kategorie se stejným názvem a dvě se stejným `orisId`
- [x] 1.4 Změnit `Event.categories` na `List<EventCategory>` včetně validací; upravit `Event.update()` a factory metody
- [x] 1.5 Test: `EventMemento` uloží a načte kategorie včetně `orisId` a `fee` (round-trip)
- [x] 1.6 Rozšířit `EventMemento` + DDL pro kategorie (úprava V001 dle konvence projektu)
- [x] 1.7 Test: `POST`/`PUT /api/events` přijme kategorie s `id` (update), bez `id` (nová) a odebrání chybějících
- [x] 1.8 Upravit `CreateEventRequest`, `UpdateEventRequest`, `UpdateEventRequestMapper`, `EventDto`, `EventSummaryDto` na strukturované kategorie
- [x] 1.9 Aktualizovat HAL-FORMS afordance `createEvent` / `updateEvent` (pole `categories` jako pole objektů)
- [x] 1.10 Frontend: formulář vytvoření/editace eventu pracuje s objekty kategorií (id, name, fee) místo řetězců
- [x] 1.11 Frontend: detail eventu zobrazuje kategorie včetně případné ceny
- [x] 1.12 Ověřit scénáře: „Renaming a category keeps existing registrations attached", „Category name must be unique within an event", „Event manager sets a fee on a category"

## 2. Registrace odkazuje kategorii přes ID (end-to-end)

- [x] 2.1 Test: registrace se vytvoří s `categoryId`; přejmenování kategorie na eventu vazbu zachová
- [x] 2.2 Změnit `EventRegistration.category` na `categoryId: EventCategoryId`; upravit `Event.registerMember()` a `editRegistration()`
- [x] 2.3 Test: registrace s `categoryId`, které na eventu neexistuje, se přečte s prázdnou kategorií (osiřelá)
- [x] 2.4 Rozšířit `EventRegistrationMemento` + DDL pro odkaz na kategorii
- [x] 2.5 Test: `POST`/`PUT` registrace přijímá `categoryId` a odmítne ID, které eventu nepatří
- [x] 2.6 Upravit `EditRegistrationRequest`, `RegistrationDto`, `RegistrationSummaryDto`, `RegistrationDtoMapper` (response nese `id` + dohledaný `name`, u osiřelé `null`)
- [x] 2.7 Upravit `RegistrationSortApplier` — řazení podle názvu dohledaného z ID, osiřelé seskupené
- [x] 2.8 Aktualizovat HAL-FORMS afordance `register` / `editRegistration` (`categoryId` s inline options: value = id, prompt = název)
- [x] 2.9 Frontend: výběr kategorie při registraci a editaci posílá `categoryId`
- [x] 2.10 Frontend: seznam registrací a „Moje přihláška" zobrazují název kategorie, u osiřelé prázdnou hodnotu
- [x] 2.11 Ověřit scénáře z `event-registrations`: přejmenování, odebrání kategorie, re-výběr členem, řazení s osiřelými

## 3. Datová migrace

- [ ] 3.1 Test: migrace přidělí ID existujícím názvům a napáruje registrace podle názvu
- [ ] 3.2 Test: registrace s názvem, který na eventu neexistuje, dostane `categoryId = null` a je zalogována
- [ ] 3.3 Implementovat migraci (kategorie → ID + `orisId = null`, registrace → `categoryId`) včetně přehledu dotčených registrací
- [ ] 3.4 Rozhodnout otevřenou otázku z designu: dohledat `orisId` z ORIS API, nebo jen zalogovat doporučení sync spustit
- [ ] 3.5 Ověřit migraci na kopii reálných dat (počty kategorií a registrací před/po sedí)

## 4. ORIS import a sync párovaný podle `orisId`

- [ ] 4.1 Test: import z ORIS naplní `orisId` z `EventClass.id`
- [ ] 4.2 Upravit `OrisEventImportService.extractCategories()` — zachovat `EventClass.id`, nejen `name()`
- [ ] 4.3 Test: sync kategorie přejmenované v ORIS aktualizuje název a zachová registrace
- [ ] 4.4 Test: sync nemaže kategorie s `orisId = null` (ručně přidané na ORIS eventu)
- [ ] 4.5 Implementovat párování podle `orisId` v syncu (shoda → update, bez shody → nová, chybějící → odebrat)
- [ ] 4.6 Přepsat `warnIfSyncRemovesCategoriesWithRegistrations()` na ID-based porovnání
- [ ] 4.7 Ověřit scénáře: „Sync renames a category that has registrations", „Sync keeps manually added categories", „Sync removes a category that has registrations"

## 5. Domain eventy

- [ ] 5.1 Test: `EventCreatedEvent` a `EventUpdatedEvent` nesou strukturované kategorie
- [ ] 5.2 Upravit `EventCreatedEvent`, `EventUpdatedEvent` (`List<String>` → strukturované položky)
- [ ] 5.3 Upravit `RegistrationEditedEvent` na `categoryId`
- [ ] 5.4 Zkontrolovat a upravit konzumenty těchto událostí napříč moduly (kalendář, notifikace)

## 6. Dokončení

- [ ] 6.1 Spustit celou testovou sadu backendu a frontendu (přes `developer:test-runner`)
- [ ] 6.2 Ověřit pokrytí: doména 100 %, celkově >80 %
- [ ] 6.3 Code review před commitem (`code-review` skill)
- [ ] 6.4 Upravit `event-registration-pricing`: D1/D2 nahradit odkazem na tento change, odstranit jeho migrační krok pro kategorie a sjednotit tvar `EventCategory`
- [ ] 6.5 Ruční QA v prohlížeči na `http://localhost:3000`: vytvoření eventu s kategoriemi a cenou, přejmenování kategorie s registracemi, odebrání kategorie, sync z ORIS
