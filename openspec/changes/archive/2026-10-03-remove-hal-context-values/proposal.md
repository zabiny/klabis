## Why

Několik hodnot v `HalResponseContext` už nemá důvod existovat. Kontext vznikl kvůli potížím postprocessorů s inicializací a kvůli tomu, aby `@MvcComponent` postprocessor nevynucoval mock portu v každém `@WebMvcTest`; obojí po zavedení `<Module>WebMvcTest` neplatí. Konkrétně `EventController.getAccommodationList` skládá odpověď sám: načítá událost, kontroluje oprávnění, filtruje registrace, dohledává údaje členů a `eventId` předává postprocessorům přes `HalResponseContext.setContext(new AccommodationListContext(eventId))`. Controller by měl data dostat z aplikační vrstvy a postprocessory by měly číst vše z domény, ne z requestového kontextu. Původní důvod kontextu (potíže postprocessorů s inicializací) už neplatí.

## What Changes

- Nová metoda aplikační služby v modulu `events` vrací record `AccommodationList(eventId, eventName, rows)`; řádek nese registraci a údaje člena.
- Do služby se přesouvá načtení události, kontrola oprávnění/sdílené ubytování, filtr `wantsSharedAccommodation` a dohledání `MemberAccommodationDto`.
- `EventController` (HAL i CSV endpoint) jen volá službu a mapuje výsledek na DTO.
- `AccommodationListPostprocessor` a `AccommodationListItemPostprocessor` čtou `eventId` z domény (řádek/seznam), `AccommodationListContext` se odstraňuje.
- `ClubKeyHeld`: `MemberOrisImportAffordancePostprocessor` dostane `Optional<MemberDiscoveryPort>` a `OrisClubKeyManagementPort` a stav klíče si zjistí sám; `MemberController.listMembers` kontext přestane nastavovat.
- `EnrolledMemberIds` / `EnrolledEventIds` / `EnrolledDisciplineIds` v detailech: `MemberDetailsPostprocessor`, `EventDetailsPostprocessor`, `DisciplineDetailsPostprocessor` si přes `SynchronizationPort.findByTarget` zjistí párování sami; controllery detail kontext nenastavují.
- Mimo rozsah: `MemberLegalGuardianGroup` (odloženo), `RegistrationsCollectionContext` a seznamové varianty `Enrolled*Ids` (dávkový `findActiveByTargets` by se změnil na N dotazů).

## No Behavior Change Justification

Odpovědi (JSON i CSV), linky `event` a `self`, autorizace i chybové stavy zůstávají stejné; mění se jen místo, kde se data skládají. Prověřeno: `openspec/specs/events/spec.md` (seznam ubytování) zůstává beze změny.

## Impact

- `events.application` (nová metoda/port + record), `events.infrastructure.restapi.EventController` a dva postprocessory.
- Testy: `EventControllerTest` (mock služby místo portů v controlleru), nový test služby.
- Bez změny API a OpenAPI specifikace.
