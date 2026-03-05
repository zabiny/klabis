## Why

Doménový model již dodržuje DDD vzory (agregáty, value objects, domain events, repository interfaces), ale tyto role
nejsou v kódu explicitně vyjádřeny. jMolecules anotace slouží jako živá dokumentace architektury přímo v kódu a umožňují
automatické ověřování architektonických pravidel přes ArchUnit testy.

## What Changes

- Přidání závislostí `jmolecules-ddd`, `jmolecules-hexagonal-architecture` a `jmolecules-archunit` do `pom.xml`
- Anotace DDD stereotypů na existujících třídách:
    - `@AggregateRoot` na agregátech (`Event`, `Member`, `User`)
    - `@ValueObject` na value objects (`EventId`, `PersonName`, `SiCardNumber`, `EmailAddress`, ...)
    - `@DomainEvent` na domain events (`EventCreatedEvent`, `MemberCreatedEvent`, ...)
    - `@Repository` na repository interfaces
    - `@Service` na feature services
    - `@Association` na reference mezi agregáty (přes ID)
- Implementace `Identifier` interface na ID value objects (`EventId`, `UserId`, `MemberId`)
- Anotace hexagonální architektury:
    - `@PrimaryPort` na veřejných API modulů (`Events`, `Members`, `Users`)
    - `@SecondaryPort` na repository interfaces
    - `@PrimaryAdapter` na controllerech a event handlerech
    - `@SecondaryAdapter` na JDBC repository adaptérech
- ArchUnit testy ověřující dodržování architektonických pravidel

## Capabilities

### New Capabilities

- `jmolecules-architecture`: Konfigurace jMolecules závislostí, DDD a hexagonální anotace na existujících třídách,
  `Identifier` interface na ID value objects, ArchUnit testy architektonických pravidel

### Modified Capabilities

(žádné - jde o čistě aditivní změnu bez dopadu na chování)

## Impact

- **Závislosti:** Nové Maven závislosti (`jmolecules-ddd`, `jmolecules-hexagonal-architecture`, `jmolecules-archunit`)
- **Kód:** Anotace a marker interface na existujících třídách ve všech modulech (`events`, `members`, `users`). Žádná
  změna chování.
- **Testy:** Nové ArchUnit testy ověřující architektonická pravidla
- **API:** Beze změny
