# New Aggregate / Module Checklist

Walk the layers in order when adding a new aggregate or Spring Modulith module. Each item names
*what* to create; the linked reference is the only place that says *how* — read it before the step.

## 1. Domain Layer — `domain-layer.md`

- [ ] `<Aggregate>Id` record implementing `Identifier` (root package if other modules reference it)
- [ ] Aggregate root extending `KlabisAggregateRoot<A, ID>`
  - [ ] Nested `@RecordBuilder` command records
  - [ ] Business factory method(s) + `reconstruct()`
  - [ ] One method per command
  - [ ] PATCH: `UpdateX.from(Aggregate)` baseline factory
- [ ] Value objects as records with compact-constructor validation
- [ ] `<Aggregate>Repository` interface annotated `@Port`
- [ ] Domain exceptions under the right base class (400 vs 404)
- [ ] Domain events for significant state changes — `domain-events.md`

## 2. Application Layer — `domain-layer.md`

- [ ] `@PrimaryPort` interface (+ nested command record when the operation spans aggregates/modules)
- [ ] `@Service` implementation, `@Transactional` methods
- [ ] PATCH: `prefilledUpdateCommand(<Aggregate>Id)` on the port
- [ ] Port consumed by another module → `<module>.application` named interface (`SKILL.md`)

## 3. OpenAPI Spec — `klabis-api-spec` skill

- [ ] Paths, request/response schemas, `x-klabis-authority`, field-security extensions
- [ ] Regenerate backend `*Api` interface and frontend types

## 4. REST Adapter

- [ ] Controller `implements <X>Api` — only the annotations listed in "Spec-First" (`rest-controller.md`)
- [ ] PATCH: hand-written `toCommand(request, prefilled, …)` overlay mapper (`rest-controller.md`)
- [ ] `Converter<S,T>` per DTO↔domain conversion (`dto-mapping.md`)
- [ ] `HalResponseContext.setDomain(...)` + `ModelWithDomainPostprocessor` with state-driven affordances (`hateoas.md`)
- [ ] Collection-level affordances / root navigation postprocessors, if needed (`hateoas.md`)
- [ ] Field-level visibility and write authorization — `field-security.md`

## 5. JDBC Adapter — `jdbc-adapter.md`

- [ ] `<Aggregate>Memento` (`@Table`, `Persistable<UUID>`, event delegation)
- [ ] `<Aggregate>JdbcRepository`
- [ ] `<Aggregate>RepositoryAdapter` (`@SecondaryAdapter @Repository`)
- [ ] Table DDL added to the existing `V001__initial_schema.sql` — do not create new migration files

## 6. Cross-Module Integration (if needed) — `domain-events.md`

- [ ] Consumed events in the module root package
- [ ] `@ApplicationModuleListener` in the consumer's `infrastructure/listeners/`
- [ ] Aggregate sharing identity with `User` (1:0-1): create the user first, derive `<Aggregate>Id` via `fromUserId(...)`

## 7. Tests — `testing-guide.md`

- [ ] `<Aggregate>TestDataBuilder` and `<Aggregate>Assert`
- [ ] Domain unit tests, service unit tests
- [ ] Memento round-trip test, repository test
- [ ] Controller test on the module's shared `@<Module>WebMvcTest` (incl. security and links/affordances in the response)
- [ ] Converter tests
- [ ] Integration test (happy path) and one E2E lifecycle test
