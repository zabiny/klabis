# New Module Implementation Checklist

Use this checklist when adding a new Spring Modulith module or new Aggregate to Klabis backend.

## 1. Domain Layer

- [ ] Create `<Aggregate>Id` record implementing `Identifier`
- [ ] Create aggregate root extending `KlabisAggregateRoot<A, ID>`
  - [ ] Add command records as nested types inside aggregate
  - [ ] Add factory method(s) which creates new instance of aggregate root (validates, publishes events)
  - [ ] Add `reconstruct()` factory method (bypasses validation, used when aggregate root loaded from DB)
  - [ ] Add `handle(Command)` methods for each command
- [ ] Create value objects as records with compact constructor validation
- [ ] Create `<Aggregate>Repository` interface annotated `@Port` with jMolecules
- [ ] Create domain event classes for significant state changes
  - [ ] Include `UUID eventId` and `Instant occurredAt`
  - [ ] Include `fromAggregate()` factory method
  - [ ] Override `toString()` excluding PII fields

## 2. Application Layer

- [ ] Create service interface with `@PrimaryPort`
- [ ] Create nested command redord(s) in interface if service operation can't use command from AggregateRoot (usually when service needs to update multiple aggregate roots) 
- [ ] Create `@Service` implementation
  - [ ] Constructor injection only
  - [ ] `@Transactional` on implementation
  - [ ] Cross-aggregate coordination in single transaction
  - [ ] Convert `BusinessRuleViolationException` → application exception
- [ ] Create `<Module>Configuration.java` for `@Bean` definitions
- [ ] Create cross-module read DTO if other modules need to query this aggregate

## 3. Infrastructure — REST API

- [ ] Create controller with `@PrimaryAdapter @RestController`
  - [ ] `produces = MediaTypes.HAL_FORMS_JSON_VALUE`
  - [ ] `@ExposesResourceFor(<Aggregate>.class)`
  - [ ] `@SecurityRequirement(name = "KlabisAuth", scopes = {...})`
- [ ] Convert UUID path variables to typed IDs at controller boundary: `new <Aggregate>Id(uuid)`
- [ ] Implement role-based command routing (admin vs. self-update)
- [ ] Add state-driven HATEOAS affordances using `klabisLinkTo()` / `klabisAfford()`
- [ ] Create MapStruct `@Mapper` for simple DTO mapping
- [ ] Create manual mapper utility class for complex PATCH operations with `PatchField`

## 4. Infrastructure — JDBC

- [ ] Create `<Aggregate>Memento` implementing `Persistable<UUID>`
  - [ ] `@Table("table_name")` annotation
  - [ ] Flatten all value objects to flat columns
  - [ ] `@CreatedDate`, `@LastModifiedDate`, `@Version` audit columns
  - [ ] `@Transient Member member` for domain event delegation
  - [ ] `@Transient boolean isNew` for INSERT/UPDATE detection
  - [ ] Static `from(Aggregate)` method for save path
  - [ ] `toAggregate()` method using `reconstruct()` for load path
  - [ ] `@DomainEvents` and `@AfterDomainEventPublication` delegating to domain object
- [ ] Create `<Aggregate>JdbcRepository` extending `CrudRepository` + `PagingAndSortingRepository`
- [ ] Create `<Aggregate>RepositoryAdapter` annotated `@SecondaryAdapter @Repository`

## 5. Database Migration

Do NOT create new migration files. Update the most fitting existing file:
- `V001__domain.sql` — add new table DDL here
- `V002__oauth2.sql` — OAuth2 related tables only
- `V003__modulith.sql` — Spring Modulith tables only

## 6. Cross-Module Events (if needed)

- [ ] Place domain events in module root package (accessible to other modules)
- [ ] Create `@Component` listener in consuming module's `infrastructure/listeners/`
- [ ] Use `@ApplicationModuleListener` annotation

## 7. Shared ID Pattern for 1:0-1 relation (for example Member = User)

Only applicable when aggregate shares identity with User:
1. Create User via `userService.createUser(...)` → get `UserId`
2. Convert: `<Aggregate>Id.from(userId)` → get `<Aggregate>Id`
3. Register aggregate using that ID

## Common Mistakes

| Mistake | Correct Approach |
|---------|-----------------|
| Spring annotations in domain class | Domain layer: no Spring imports |
| Field injection `@Autowired` | Constructor injection only |
| Raw UUID between aggregates | Use typed ID records |
| Sharing `@Table` class directly with domain | Use Memento pattern |
| New migration script for every change | Update existing V001/V002/V003 |
| PII in `toString()` of events | Exclude PII, keep only IDs and technical fields |
| Returning `void` from commands | Return updated aggregate for HATEOAS response building |
