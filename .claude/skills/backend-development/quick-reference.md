# DDD Backend Development - Quick Reference

Fast lookup for common patterns and conventions.

## Class Annotations

```java
@AggregateRoot                    // Marks aggregate root
@Entity                           // Marks entity within aggregate
@Identity                         // Marks identity field
@Service                          // Application/domain service
@Component                        // Spring component
@Repository                       // Repository interface (optional, for clarity)
@EventListener(EventClass.class)  // Listens to domain events
@JacksonComponent                 // Custom Jackson serialization
@ApiController                    // REST API entry point
@ExposesResourceFor(Class.class)  // Declares exposed domain type
@Relation(collectionRelation="x") // HATEOAS relation name for DTO
@HasGrant(ApplicationGrant.X)     // Security authorization
@Transactional                    // Transaction boundary (on methods)
@Order(N)                         // Processor execution order
```

## Class Hierarchy

```
Aggregate Extends:
  ↓
AbstractAggregateRoot<T>

Repository Extends:
  ↓
DataRepository<Entity, Id>

Processor Implements:
  ↓
RepresentationModelProcessor<T>

Listener Decorates:
  ↓
@EventListener
```

## Package Structure

```
club/klabis/feature/
├── (Domain classes)
│   ├── AggregateRoot.java           @AggregateRoot
│   ├── Entity.java                  @Entity
│   ├── ValueObject.java             (record)
│   ├── CreateEntityCommand.java     (record)
│   └── EntityRepository.java        (interface, secondary port)
└── infrastructure/
    ├── EntityService.java            @Service, @Component
    ├── EntityApiController.java      @ApiController
    ├── EntityInMemoryRepository.java (implements EntityRepository)
    ├── EventListeners.java           @Component, @EventListener
    └── EntityIdConverter.java        @JacksonComponent
```

## Creating Classes Checklist

### Aggregate Root
```java
@AggregateRoot
public class MyAggregate extends AbstractAggregateRoot<MyAggregate> {
    @Identity
    private AggregateId id;
    private Set<MyEntity> entities = new HashSet<>();

    public MyAggregate(AggregateId id, Set<MyEntity> entities) {
        Assert.notNull(id, "ID required");
        this.id = id;
        this.entities.addAll(entities);
    }

    public static MyAggregate create(AggregateId id) {
        return new MyAggregate(id, new HashSet<>());
    }

    public MyEntity handle(CreateCommand cmd) {
        // Enforce business rules
        MyEntity entity = MyEntity.create(cmd);
        this.entities.add(entity);
        return entity;
    }

    public Set<MyEntity> getEntities() {
        return Collections.unmodifiableSet(entities);
    }
}
```

### Entity
```java
@Entity
public class MyEntity {
    @Identity
    private Id id;
    private String name;

    public record Id(long value) {
        static long MAX_VALUE = 0L;
        static synchronized Id newId() {
            return new Id(MAX_VALUE++);
        }
    }

    public static MyEntity create(String name) {
        return new MyEntity(Id.newId(), name);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        return this.id == ((MyEntity) obj).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### Value Object
```java
public record ValueObject(String field1, LocalDate field2) {
    public ValueObject {
        Assert.hasText(field1, "Field1 required");
        Assert.notNull(field2, "Field2 required");
    }

    public boolean someLogic() {
        return // ...
    }
}
```

### Command
```java
public record CreateCommand(String field1, LocalDate field2) {
    public CreateCommand {
        Assert.hasText(field1, "Field1 required");
    }

    public static CreateCommand shortcut(String field1) {
        return new CreateCommand(field1, LocalDate.now());
    }
}
```

### Repository Interface
```java
public interface MyRepository extends DataRepository<MyEntity, MyEntity.Id> {
    Optional<MyAggregate> readAggregate(AggregateId id);
    List<MyEntity> findAllActive();
    Optional<MyEntity> findByName(String name);
}
```

### Application Service
```java
@Service
@Component
public class MyApplicationService {
    private final MyRepository repository;

    public MyApplicationService(MyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public MyEntity createEntity(CreateCommand cmd) {
        MyAggregate agg = repository.readAggregate(cmd.aggregateId());
        MyEntity entity = agg.handle(cmd);
        return repository.save(entity);
    }
}
```

### REST Controller
```java
@ApiController(openApiTagName = "entities", path = "/entities")
@ExposesResourceFor(MyEntity.class)
@Import(EntityListPostprocessor.class)
public class EntityApiController {

    @Relation(collectionRelation = "entities")
    public record EntityDto(String field1, LocalDate field2, @JsonIgnore Link parentLink) {}

    private EntityDto toDto(MyEntity entity) {
        return new EntityDto(entity.getField1(), entity.getField2(), null);
    }

    @GetMapping
    public CollectionModel<EntityModel<EntityDto>> list() {
        return modelAssembler.toCollectionModel(service.list());
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CreateCommand cmd) {
        MyEntity entity = service.create(cmd);
        return ResponseEntity.created(toSelfLink(entity).toUri()).build();
    }
}
```

### Event Listener
```java
@Component
class DomainEventListeners {
    private final MyRepository repository;

    @EventListener(ExternalDomainEvent.class)
    public void onExternalEvent(ExternalDomainEvent event) {
        MyEntity entity = repository.findById(event.getEntityId())
            .orElseGet(() -> MyEntity.createFrom(event));
        entity.updateFrom(event);
        repository.save(entity);
    }
}
```

## DateTime Handling

| Context | Type | Example |
|---------|------|---------|
| Domain | `ZonedDateTime` | `ZonedDateTime.now()` |
| Value Object | `LocalDate` or `LocalDateTime` | `LocalDate.now()` |
| DTO/API | `LocalDate` | `new LocalDate()` |
| Conversion | `Globals.to*()` | `Globals.toZonedDateTime(date)` |

## Validation

| Where | How | Example |
|-------|-----|---------|
| Record Constructor | `Assert` | `Assert.notNull(id, "msg")` |
| Aggregate Method | Exception | `throw new IllegalArgumentException()` |
| Value Object | `Assert` | `Assert.isTrue(start < end, "msg")` |
| Service Method | Exception or `@Valid` | Validation annotations on DTO |

## Return Types

| Scenario | Return | Example |
|----------|--------|---------|
| Single by ID | `Optional<T>` | `repository.findById(id)` |
| Multiple results | `List<T>` | `repository.findAll()` |
| May not exist | `Optional<T>` | `repository.findByName(name)` |
| Always exists | `T` | `aggregate.getField()` |
| Mutable collection | `Collections.unmodifiable*()` | `Collections.unmodifiableSet(items)` |

## Common Method Patterns

```java
// Factory method
public static MyClass create(...) { }

// Shortcut constructor
public static MyClass shortcut(String name) { }

// Builder/Fluent
public MyClass withField(String value) { return this; }

// Optional handling
obj.findById(id).map(x -> x.update()).orElseGet(() -> create())

// Immutable collection return
return Collections.unmodifiableSet(field)

// Override equals/hashCode
@Override
public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (MyClass) obj;
    return Objects.equals(this.id, that.id);
}

// Override toString
@Override
public String toString() {
    return "MyClass[id=" + id + ", name=" + name + "]";
}
```

## API Response Patterns

```java
// GET List
@GetMapping
public CollectionModel<EntityModel<Dto>> list() {
    return modelAssembler.toCollectionModel(items);
}

// GET Single
@GetMapping("/{id}")
public EntityModel<Dto> get(@PathVariable Id id) {
    return modelAssembler.toEntityModel(item);
}

// POST Create
@PostMapping
public ResponseEntity<Void> create(@RequestBody CreateCmd cmd) {
    Item item = service.create(cmd);
    return ResponseEntity.created(toLink(item).toUri()).build();
}

// PUT Update
@PutMapping("/{id}")
public EntityModel<Dto> update(@PathVariable Id id, @RequestBody UpdateCmd cmd) {
    Item item = service.update(id, cmd);
    return modelAssembler.toEntityModel(item);
}

// DELETE
@DeleteMapping("/{id}")
public ResponseEntity<Void> delete(@PathVariable Id id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
}
```

## HATEOAS Link Building

```java
// Self link
linkTo(methodOn(Controller.class).getMethod(id)).withSelfRel()

// With relation
linkTo(methodOn(Controller.class).updateMethod(id)).withRel("update")

// Template/expansion
linkTo(methodOn(Controller.class).listMethod(null, null))
    .withRel("search")
    .expand("sort", "page")

// With affordances
link.andAffordances(
    afford(methodOn(Controller.class).createMethod(null))
)

// In postprocessor
@Component
class MyPostprocessor implements RepresentationModelProcessor<EntityModel<MyDto>> {
    @Override
    public EntityModel<MyDto> process(EntityModel<MyDto> model) {
        model.add(linkTo(...).withRel("action"));
        return model;
    }
}
```

## Testing Patterns

```java
// Aggregate business rule test
@Test
void cannotCreateInvalidItem() {
    assertThrows(IllegalArgumentException.class, () -> {
        aggregate.handle(invalidCommand);
    });
}

// Repository mock
@Test
void serviceCallsRepository() {
    var repo = mock(MyRepository.class);
    when(repo.findById(id)).thenReturn(Optional.of(item));
    var result = service.get(id);
    verify(repo).findById(id);
}

// End-to-end API test
@Test
@Transactional
void createItemEndToEnd() {
    var cmd = new CreateCommand("test");
    var result = service.create(cmd);
    var found = repository.findById(result.getId());
    assertTrue(found.isPresent());
}
```

## Common Imports

```java
// DDD/Annotations
import org.jmolecules.ddd.annotation.*;
import org.springframework.data.domain.AbstractAggregateRoot;

// Spring
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.event.EventListener;

// HATEOAS
import org.springframework.hateoas.*;
import org.springframework.hateoas.server.*;

// Web
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

// Validation
import org.springframework.util.Assert;

// Data
import java.util.Collections;
import java.util.Optional;

// DateTime
import java.time.LocalDate;
import java.time.ZonedDateTime;
```

## Common Pitfalls & Fixes

| Pitfall | Fix |
|---------|-----|
| Exposing mutable collections | Return `Collections.unmodifiable*()` |
| Using state for entity equality | Base equals on identity only |
| Scattering validation | Validate in constructors and aggregate methods |
| Skipping transactions | Add `@Transactional` to write operations |
| Using DTOs in domain | Use domain objects, convert in controller |
| Event listener side effects | Design for any order, use idempotent operations |
| N+1 queries | Eager load aggregate children with `readAggregate()` |
| Returning HTTP 200 for created items | Use `ResponseEntity.created()` with 201 status |
| Modifying returned collections | Always return unmodifiable |
| Missing HATEOAS links | Use postprocessors for consistent enrichment |
