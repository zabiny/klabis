## Why

Kategorie eventu jsou dnes prostý `List<String>` — název je zároveň identitou. Registrace na kategorii odkazuje jejím názvem, takže **jakákoli změna názvu kategorii tiše odpojí od registrací**. Nejde o hypotetický problém: `OrisEventImportService` má kvůli tomu obranné varování `warnIfSyncRemovesCategoriesWithRegistrations` a spec `event-categories` má vlastní scénář popisující, že sync může kategorii odebrat, zatímco registrace na ni osiří.

Přitom ORIS stabilní identitu **poskytuje** — `EventClass` má `id` a `EventDetails.classes()` je `Map<String, EventClass>` klíčovaná právě jím; registrace v ORIS odkazuje číselně přes `EventEntry.classId`. Náš import z celého `EventClass` vytáhne jen `name()` a ID zahodí. Name-based model je tedy artefakt našeho importu, ne tvar zdroje.

S připravovanou cenotvorbou (`event-registration-pricing`) cena roste na významu: odpojená kategorie už neznamená jen chybný štítek, ale špatně spočítané vstupné. Doplňkové služby ve stejném changi z těchto důvodů identitu dostávají — kategorie mají zůstat konzistentní.

## What Changes

- **Kategorie získává stabilní identitu:** `EventCategory` se strukturou `id` (lokální UUID) + volitelný `orisId` + `name` + volitelná `fee` — stejný tvar, jaký má `SupplementaryService` v changi `event-registration-pricing`. **BREAKING** — mění tvar pole `categories` z `List<String>`.
- **Registrace odkazuje kategorii přes ID:** `EventRegistration.category: String` → `categoryId: EventCategoryId`. Přejmenování kategorie nadále neovlivní existující registrace. **BREAKING** — mění tvar registračního API.
- **ORIS sync páruje podle `orisId`:** Import naplní `orisId` z `EventClass.id`. Sync pak kategorii dohledá podle něj, takže přejmenování v ORIS už kategorii neodebere a nezpůsobí osiření registrací. Obranné varování zůstává jen pro kategorie skutečně z ORIS zmizelé.
- **`fee` je součástí struktury:** Tento change vlastní celý tvar `EventCategory` včetně volitelné ceny. Change `event-registration-pricing` na něj v části kategorií navazuje místo aby ji definoval sám.
- **`CategoryPreset` zůstává name-based:** Preset je jen šablona názvů pro předvyplnění formuláře — nemá registrace ani vazbu na ORIS, takže identita by tam nic neřešila. Beze změny.

## Capabilities

### Modified Capabilities

- `event-categories`: Kategorie přestává být řetězec a stává se položkou se stabilní identitou, volitelným ORIS identifikátorem a volitelnou cenou. Mění se scénáře ORIS importu a syncu — nově párování podle `orisId` místo podle názvu, včetně chování při přejmenování kategorie v ORIS.
- `event-registrations`: Registrace odkazuje na kategorii přes identifikátor místo názvu; přejmenování kategorie zachovává vazbu registrace.

## Impact

- **Backend — events modul (doména):** Nový value object `EventCategoryId`, `EventCategory` (id + orisId + name + fee) nahrazuje `String` v `Event.categories`. `EventRegistration.category` → `categoryId`. Validace unikátnosti názvu i ID v rámci eventu.
- **Backend — persistence:** `EventMemento` a `EventRegistrationMemento`, migrace sloupců pro kategorie a odkaz z registrace.
- **Backend — ORIS import:** `OrisEventImportService.extractCategories()` nově zachová `EventClass.id` jako `orisId` a bude párovat podle něj; `warnIfSyncRemovesCategoriesWithRegistrations` se přepíše na ID-based porovnání.
- **Backend — domain eventy:** `EventCreatedEvent` a `EventUpdatedEvent` nesou `List<String> categories`; `RegistrationEditedEvent` nese název kategorie. Tvar se mění.
- **Backend — REST/HAL-FORMS:** `CreateEventRequest`, `UpdateEventRequest`, `EventDto`, `EventSummaryDto`, `RegistrationDto`, `RegistrationSummaryDto`, `EditRegistrationRequest`, `RegistrationDtoMapper`, `RegistrationSortApplier` (řazení podle kategorie nyní přes název dohledaný z ID).
- **Frontend:** ~21 souborů pracujících s kategoriemi — formuláře eventu, výběr kategorie při registraci, zobrazení a řazení v tabulkách registrací.
- **Migrace dat:** Existující názvy kategorií → položky s vygenerovaným ID a `orisId = null`; registrace se napárují podle dnešního názvu. Kategorie osiřelé už dnes se zaloguje.
- **Vztah k `event-registration-pricing`:** Tento change přebírá definici `EventCategory` včetně `fee`. V pricing changi se D1/D2 zjednoduší na odkaz sem a jeho migrační krok pro kategorie odpadá. **Je vhodné implementovat tento change první.**
- **Mimo rozsah:** `CategoryPreset`; import cen kategorií z ORIS (`EventClass.fee`, `manualFee*` odstupňované podle termínu přihlášky); kapacita kategorie (`entryLimit`).
