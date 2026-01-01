# Backend Feature Implementation Checklist

Use this checklist when implementing a new feature following DDD patterns.

## Phase 1: Domain Analysis

- [ ] Identify aggregate roots and their responsibilities
- [ ] Define entities and value objects
- [ ] List business rules and invariants
- [ ] Map relationships between aggregates
- [ ] Identify commands (user intents)
- [ ] Identify domain events

## Phase 2: Domain Model Implementation

### Aggregate Root
- [ ] Create aggregate class with `@AggregateRoot`
- [ ] Define identity field with `@Identity`
- [ ] Create Identity value object (usually as nested record)
- [ ] Extend `AbstractAggregateRoot<T>`
- [ ] Implement business rule validation in constructor
- [ ] Create factory method `public static AggregateRoot create(...)`

### Entities
- [ ] Mark with `@Entity`
- [ ] Define identity field with `@Identity`
- [ ] Create nested `Id` record with `newId()` factory
- [ ] Implement constructor (protected, with validation)
- [ ] Override `equals()` to use identity only
- [ ] Override `hashCode()` consistently
- [ ] Override `toString()` for debugging
- [ ] Create factory methods for construction

### Value Objects
- [ ] Use Java records for immutability
- [ ] Add validation in compact constructor
- [ ] Include helper methods (e.g., `includes()`, `isValid()`)
- [ ] Override `toString()` if needed for clarity

### Commands
- [ ] Create command record
- [ ] Add validation in compact constructor
- [ ] Create factory methods for common cases
- [ ] Add methods for derived data if needed

### Aggregate Behavior
- [ ] Add command handler: `handle(YourCommand command)`
- [ ] Enforce invariants (throw `IllegalArgumentException`)
- [ ] Modify internal state
- [ ] Return affected entities
- [ ] Return unmodifiable collections: `Collections.unmodifiableSet()`

## Phase 3: Repository Contract

- [ ] Create interface extending `DataRepository<Entity, Id>`
- [ ] Add domain-specific query methods
- [ ] Add aggregate loading method: `readAggregate(Id)`
- [ ] Use domain types in method signatures
- [ ] Document complex queries

## Phase 4: Repository Implementation

- [ ] Create implementation class in infrastructure package
- [ ] Implement all interface methods
- [ ] Implement aggregate loading (fetch with children)
- [ ] Implement domain-specific queries
- [ ] Handle collection filtering and sorting

## Phase 5: Application Service

- [ ] Create service class with `@Service` and `@Component`
- [ ] Inject repository interface
- [ ] Create method for each use case
- [ ] Add `@Transactional` to write operations
- [ ] Call `aggregate.handle(command)` for business logic
- [ ] Save via repository
- [ ] Return domain objects (not DTOs)

## Phase 6: REST API

### Controller
- [ ] Create controller with `@ApiController`
- [ ] Decorate with `@ExposesResourceFor(Entity.class)`
- [ ] Create DTO record with `@Relation`
- [ ] Create `toDto()` conversion method
- [ ] Implement `GET` (single) endpoint
- [ ] Implement `GET` (list) endpoint with filtering
- [ ] Implement `POST` (create) endpoint
- [ ] Add security checks with `@HasGrant`
- [ ] Use `ModelAssembler` for HATEOAS
- [ ] Return `ResponseEntity` with appropriate status

### Representation Processors
- [ ] Create entity processor for single item links
- [ ] Create collection processor for list actions
- [ ] Add links to related resources
- [ ] Include affordances for available operations
- [ ] Order processors with `@Order` if dependencies exist

### Jackson Converters
- [ ] Create `ValueSerializer` for domain value objects if needed
- [ ] Create `ValueDeserializer` for domain value objects
- [ ] Mark with `@JacksonComponent`
- [ ] Handle null values gracefully
- [ ] Implement `Converter` interface for Spring type conversion

## Phase 7: Event Integration (if applicable)

- [ ] Identify external domain events to listen to
- [ ] Create listener class with `@Component`
- [ ] Create `@EventListener` method for each event type
- [ ] Extract data from external event
- [ ] Find existing or create new entity
- [ ] Update entity state
- [ ] Save to repository

## Phase 8: Testing

### Domain Tests
- [ ] Test aggregate creation
- [ ] Test business rule enforcement (negative cases)
- [ ] Test command handling
- [ ] Test value object invariants

### Service Tests
- [ ] Mock repository
- [ ] Test use case flows
- [ ] Verify repository interactions
- [ ] Test error handling

### API Tests
- [ ] Test endpoint status codes
- [ ] Test HATEOAS link presence
- [ ] Test DTO serialization
- [ ] Test request validation
- [ ] Test security authorization

### Integration Tests
- [ ] Test event listener reactions
- [ ] Test cross-domain interactions
- [ ] Test transaction boundaries
- [ ] Test full flow end-to-end

## Phase 9: Documentation

- [ ] Document aggregate structure
- [ ] Document business rules
- [ ] Document available commands
- [ ] Document API endpoints in OpenAPI/Swagger
- [ ] Document event listeners and triggers
- [ ] Add code comments for complex logic
- [ ] Update package-level documentation if needed

## Code Quality Checks

- [ ] No public mutators in aggregates (only through commands)
- [ ] Proper exception handling and messages
- [ ] Validation at domain boundaries
- [ ] Consistent naming conventions
- [ ] No DTOs in domain layer
- [ ] Proper null handling with `@Nullable` annotations
- [ ] Circular dependency checks between packages
- [ ] Proper import organization
- [ ] No TODO comments without tracking

## Security Checks

- [ ] Input validation in DTOs and commands
- [ ] Authorization checks with `@HasGrant`
- [ ] No sensitive data in logs
- [ ] Proper exception messages (no info leakage)
- [ ] SQL injection prevention (using JPA/ORM)
- [ ] XSS prevention in API responses
- [ ] CSRF protection on state-changing operations

## Performance Considerations

- [ ] Avoid N+1 query problems (eager load children)
- [ ] Use pagination for large collections
- [ ] Index frequently queried fields
- [ ] Consider caching for read-heavy operations
- [ ] Profile if performance is critical

## Git Commit Checklist

- [ ] All tests pass
- [ ] Code follows style guide
- [ ] No merge conflicts
- [ ] Commit message is clear and descriptive
- [ ] Related files grouped logically
- [ ] No unintended files committed
