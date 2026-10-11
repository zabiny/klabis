## Context

Controller dnes drží `members` (`findAccommodationDataByIds`), `csvRenderer` a logiku autorizace i filtrování. Postprocessory čtou `eventId` z `HalResponseContext`, protože `EventRegistration` na událost neodkazuje.

## Goals / Non-Goals

**Goals:**
- Controller nesklá data, získá je z aplikační služby.
- Postprocessory berou `eventId` z domény; `AccommodationListContext` zmizí.

**Non-Goals:**
- Ostatní hodnoty v `HalResponseContext` (viz proposal): `MemberLegalGuardianGroup`, `RegistrationsCollectionContext`, seznamové `Enrolled*Ids`.
- Změna chování, API nebo specifikace.

## Decisions

- **Record `AccommodationList(UUID eventId, String eventName, List<AccommodationListRow> rows)`** vrací služba v `events.application`. `eventName` slouží CSV endpointu (název souboru), takže controller nemusí event načítat znovu.
- **`AccommodationListRow(UUID eventId, EventRegistration registration, MemberAccommodationDto memberData)`** je doménový prvek pro `setDomainList` a `ModelWithDomainPostprocessor`; nese `eventId`, takže item postprocessor nepotřebuje kontext.
- **Kolekční postprocessor** (`CollectionModel`) nemá doménu. Controller proto před návratem uloží přes `HalResponseContext.setDomain(accommodationList)` celý record a postprocessor ho čte přes jeho `eventId` (stejný vzor jako `setDomain` u detailů). Pokud to advice pro kolekce neumožní, ověřit při implementaci; fallback je zachovat jen tento jeden kontext.
- **Autorizace** (koordinátor nebo `EVENTS:REGISTRATIONS`, sdílené ubytování zapnuto) se přesouvá do služby beze změny sémantiky (stejné výjimky `AccessDeniedException`), takže stejné status kódy. Aplikační vrstva nesmí záviset na Spring Security (`SecurityArchitectureTest`), proto controller odvodí `CurrentUserData` ze `SecurityContextHolder` a předá ho službě (`getAccommodationList(eventId, caller)`), která podle něj rozhodne (`hasAuthority`, `isMemberOf(event::isCoordinator)`).
- Mapování na `AccommodationListItemDto` zůstává v REST vrstvě (mapper přijímá řádek).

- **Postprocessor si data zjistí sám (`ClubKeyHeld`, detailové `Enrolled*Ids`)**: injektuje porty (`SynchronizationPort`, `OrisClubKeyManagementPort`, `Optional<MemberDiscoveryPort>`). Rozšířené slicy `MembersWebMvcTest` (`sync`) a `EventsWebMvcTest` (`sync`) port už obsahují; `WebMvcMockitoBeans` se doplní jen tam, kde chybí. Počet dotazů se nemění (detail = jeden lookup).
- **Seznamové `Enrolled*Ids` zůstávají**: dávkový `findActiveByTargets` pro stránku by se v řádkovém postprocessoru změnil na N dotazů.

## Risks / Trade-offs

- [Kolekční postprocessor bez domény] → ověřit podporu `setDomain` společně s `setDomainList` v `HalResponseBodyAdvice`; jinak ponechat minimální výjimku a zdokumentovat.
- [Autorizace ve službě závisí na předaném `CurrentUserData`] → identita se bere z `SecurityContextHolder` v controlleru jako dosud; pokrytí unit testy služby a testy 403 v controlleru.
- [Disciplíny: lookup v `getDiscipline` slouží i `DisciplineManagementService.update`] → při implementaci ověřit, zda controller stav párování potřebuje nezávisle na postprocessoru; pokud ano, ponechat lookup ve službě a postprocessor ho dotáže sám.
