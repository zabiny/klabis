## Context

Klabis backend již implementuje DDD vzory (agregáty, value objects, domain events, repository interfaces jako porty) a
hexagonální architekturu (controllers jako primary adaptery, JDBC implementace jako secondary adaptery). Tyto role ale
nejsou v kódu explicitně vyjádřeny - rozpoznatelné jsou pouze z konvencí pojmenování a umístění v package struktuře.

jMolecules knihovna poskytuje standardizované anotace a marker interfaces pro vyjádření architektonických konceptů přímo
v kódu.

## Goals / Non-Goals

**Goals:**

- Explicitně vyjádřit DDD stereotypy (`@AggregateRoot`, `@ValueObject`, `@DomainEvent`, `@Repository`, `@Service`,
  `@Association`) na existujících třídách
- Vyjádřit hexagonální architekturu (`@PrimaryPort`, `@SecondaryPort`, `@PrimaryAdapter`, `@SecondaryAdapter`) na
  boundary třídách
- Implementovat `Identifier` interface na ID value objects
- Přidat ArchUnit testy ověřující dodržování architektonických pravidel
- Použít jMolecules BOM pro správu verzí

**Non-Goals:**

- Měnit chování existujícího kódu
- Používat `jmolecules-spring` integraci (mapování na Spring stereotypy) - Spring anotace již máme
- Používat vrstvové anotace (`@DomainLayer`, `@InfrastructureLayer`) - hexagonální anotace lépe odpovídají naší
  architektuře
- Přidávat jMolecules `Entity` interface na vnořené entity (pouze anotace `@Entity`)

## Decisions

### 1. jMolecules BOM pro správu verzí

Použijeme `jmolecules-bom` v `dependencyManagement` sekci pom.xml. Aktuální stabilní verze je `2025.0.2`.

**Alternativy:** Explicitní verze na každé závislosti → více údržby, riziko nekompatibilních verzí.

### 2. Hexagonální anotace místo vrstvových

Hexagonální anotace (`@PrimaryPort`, `@SecondaryPort`, `@PrimaryAdapter`, `@SecondaryAdapter`) přesněji popisují roli
tříd na hranicích modulů. Vrstvové anotace (`@DomainLayer` atd.) by duplikovaly informaci, kterou už DDD anotace nesou.

Mapování na existující třídy:

```
@PrimaryPort       → *Service (application services - entry points for use cases)
@SecondaryPort     → EventRepository, MemberRepository, UserRepository (interfaces)
@PrimaryAdapter    → *Controller, *EventHandler
@SecondaryAdapter  → *RepositoryAdapter, *JdbcRepository
```

**Poznámka k query interfaces:** Interfaces jako `Events`, `Members`, `Users` (veřejné query API) NEJSOU `@PrimaryPort`.
Tyto interfaces jsou implementovány `@SecondaryAdapter` třídami (repository adaptery), takže z pohledu hexagonální
architektury nejsou porty - jsou to doménové kontrakty. `@PrimaryPort` patří na service třídy, které jsou vstupním bodem
pro use cases.

**Alternativy:** Vrstvové anotace → jednodušší, ale méně přesné. Problém s třídami na hranici vrstev (repository
interface: doména nebo infrastruktura?). Viz srovnání v explore diskuzi.

### 3. Identifier interface na ID value objects

ID records (`EventId`, `UserId`, `MemberId`) budou implementovat `org.jmolecules.ddd.types.Identifier`. Jde o marker
interface bez metod, takže stačí přidat `implements Identifier` k existujícím records.

```java
// Před
public record EventId(UUID value) { ... }

// Po
@ValueObject
public record EventId(UUID value) implements Identifier { ... }
```

**Alternativy:** Pouze `@ValueObject` anotace bez Identifier → méně expresivní, ArchUnit nemůže typově ověřit, že
asociace používají skutečné identifikátory.

### 4. `@Association` na referencích mezi agregáty

Reference mezi agregáty (např. `MemberId` v `Event` registracích) budou anotovány `@Association`. To dokumentuje, že jde
o referenci přes identifikátor, ne o přímou vazbu.

### 5. `@Identity` na ID polích v aggregate roots

Každý aggregate root musí mít pole s identifikátorem anotované `@Identity`. To explicitně označuje, které pole je
identitou agregátu.

```java
@AggregateRoot
public class Event {
    @Identity
    private final EventId id;
    // ...
}
```

### 6. DDD anotace vs. interfaces

Pro `@AggregateRoot`, `@Entity`, `@ValueObject` a `@DomainEvent` použijeme **anotace**, ne marker interfaces. Důvod:
anotace nevyžadují změnu class hierarchy a jsou méně invazivní. Výjimka: `Identifier` interface na ID records (viz
rozhodnutí 3).

**Alternativy:** Marker interfaces (`AggregateRoot<ID>`, `Entity<Aggregate, ID>`) → silnější typová kontrola, ale
vyžadují generické parametry a mění signaturu tříd.

### 7. ArchUnit testy

Přidáme ArchUnit testy využívající `jmolecules-archunit` modul:

- Agregáty nereferencují jiné agregáty přímo (pouze přes ID/`@Association`)
- Primary adaptery závisí na portech, ne na jiných adaptérech
- Doménový model nezávisí na infrastruktuře
- Všechny repository interfaces jsou anotovány jako `@SecondaryPort`

Testy budou v `src/test/java/com/klabis/architecture/` jako sdílené architektonické testy.

### 8. Scope: všechny tři moduly najednou

Anotace přidáme do všech modulů (`events`, `members`, `users`) v jednom kroku. Nejde o velkou změnu - jen přidání
anotací a implements clause. Inkrementální přístup (modul po modulu) by zbytečně prodlužoval proces.

## Risks / Trade-offs

**[Nová závislost]** → jMolecules je lightweight knihovna (pouze anotace/interfaces, žádný runtime). Minimální riziko.
BOM zajistí kompatibilitu verzí.

**[Identifier interface na records]** → Records v Javě mohou implementovat interfaces, ale nemohou dědit. `Identifier`
je marker interface bez metod, takže žádný konflikt. → Minimální riziko.

**[ArchUnit testy mohou zpočátku selhat]** → Pokud existují skryté architektonické porušení, ArchUnit je odhalí. → To je
feature, ne bug. Opravíme v rámci implementace.

**[Údržba anotací]** → Nové třídy budou muset být správně anotovány. → ArchUnit testy pomohou toto zachytit.
