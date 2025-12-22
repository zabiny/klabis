# DDD Code Patterns & Examples

Real code examples from the project demonstrating patterns.

## Pattern 1: Value Objects with Records

### ✅ Good: Immutable Value Object

```java
public record DateRange(LocalDate start, LocalDate end) {
    public DateRange {
        Assert.isTrue(!start.isAfter(end), "Start date must not be after end date");
    }

    public boolean includes(LocalDate date) {
        return !start.isAfter(date) && !end.isBefore(date);
    }

    public boolean includes(ZonedDateTime dateTime) {
        return includes(Globals.toLocalDate(dateTime));
    }
}
```

### ❌ Bad: Mutable Class

```java
public class DateRange {
    private LocalDate start;
    private LocalDate end;

    public void setStart(LocalDate start) {
        this.start = start;  // Can change at any time
    }
}
```

## Pattern 2: Entity Identity

### ✅ Good: Nested Id Record with Factory

```java
@Entity
public class CalendarItem {
    @Identity
    private Id id;

    public record Id(long value) {
        static long MAX_VALUE = 0L;

        static synchronized Id newId() {
            return new Id(MAX_VALUE++);
        }

        @Override
        public String toString() {
            return "CALENDARITEM_%d".formatted(value);
        }
    }

    public static CalendarItem create() {
        return new CalendarItem(Id.newId(), LocalDateTime.now());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CalendarItem) obj;
        return this.id == that.id;  // Identity-based
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
```

### ❌ Bad: Using State for Equality

```java
@Override
public boolean equals(Object obj) {
    // Wrong! Two items are equal only if all fields match
    return this.id == that.id &&
           this.status.equals(that.status) &&
           this.name.equals(that.name);
}
```

## Pattern 3: Aggregate Command Handling

### ✅ Good: Enforce Rules in Aggregate

```java
@AggregateRoot
public class Calendar extends AbstractAggregateRoot<Calendar> {
    private CalendarPeriod period;
    private Set<CalendarItem> items = new HashSet<>();

    public CalendarItem handle(CreateCalendarItemCommand command) {
        // Enforce business rule
        if (!period.includes(command.start()) && !period.includes(command.end())) {
            throw new IllegalArgumentException("Requested item is not placed in this calendar");
        }

        // Create and add
        CalendarItem result = CalendarItem.calendarItem(
            Globals.toZonedDateTime(command.start()),
            Globals.toZonedDateTime(command.end())
        ).withNote(command.note());

        this.items.add(result);
        return result;
    }

    public Set<CalendarItem> getItems() {
        return Collections.unmodifiableSet(items);
    }
}
```

### ❌ Bad: Skipping Validation

```java
public CalendarItem handle(CreateCalendarItemCommand command) {
    // No validation - what if item is outside period?
    CalendarItem result = CalendarItem.create(command);
    this.items.add(result);
    return result;
}
```

## Pattern 4: Factory Methods

### ✅ Good: Factory Methods for Common Cases

```java
public record CreateCalendarItemCommand(LocalDate start, LocalDate end, String note) {
    // Direct factory
    public static CreateCalendarItemCommand task(LocalDate date, String note) {
        return new CreateCalendarItemCommand(date, date, note);
    }

    public static CreateCalendarItemCommand range(LocalDate start, LocalDate end, String note) {
        return new CreateCalendarItemCommand(start, end, note);
    }
}

// Usage
var dailyTask = CreateCalendarItemCommand.task(LocalDate.now(), "Daily standup");
var eventPeriod = CreateCalendarItemCommand.range(start, end, "Conference");
```

### ❌ Bad: Repetitive Construction

```java
// Repeated everywhere
var cmd = new CreateCalendarItemCommand(
    LocalDate.now(),
    LocalDate.now(),
    "Daily standup"
);

// vs
var cmd = CreateCalendarItemCommand.task(LocalDate.now(), "Daily standup");
```

## Pattern 5: Application Service Transaction Boundaries

### ✅ Good: Transaction Wraps Aggregate Read-Modify-Save

```java
@Service
@Component
public class CalendarService {
    private final CalendarRepository calendarRepository;

    @Transactional
    public CalendarItem createCalendarItem(CreateCalendarItemCommand command) {
        // Read aggregate within transaction
        Calendar c = calendarRepository.readCalendar(command.getPeriod());

        // Modify aggregate (enforces business rules)
        CalendarItem item = c.handle(command);

        // Save aggregate within same transaction
        return calendarRepository.save(c);
    }
}
```

### ❌ Bad: Separate Transactions

```java
public CalendarItem createCalendarItem(CreateCalendarItemCommand command) {
    // Separate read - may get stale data
    Calendar c = calendarRepository.readCalendar(command.getPeriod());

    // Execute command outside transaction
    CalendarItem item = c.handle(command);

    // Save in different transaction - inconsistency risk
    return calendarRepository.save(c);
}
```

## Pattern 6: Repository Interface (Secondary Port)

### ✅ Good: Domain-Centric Interface

```java
public interface CalendarRepository extends DataRepository<CalendarItem, CalendarItem.Id> {
    // Aggregate loading
    Calendar readCalendar(Calendar.CalendarPeriod period);

    // Domain-specific queries
    Optional<EventCalendarItem> findEventItem(
        Event.Id eventId,
        EventCalendarItem.Type type
    );

    // Range queries
    Collection<CalendarItem> getCalendar(Calendar.CalendarPeriod period);
}
```

### ❌ Bad: Technical Interface

```java
public interface ItemRepository {
    // Too generic - doesn't express intent
    List<Object> query(String sql);
    void persist(Item item);
    void delete(long id);
}
```

## Pattern 7: Event-Driven Integration

### ✅ Good: React to External Events

```java
@Component
class EventListeners {
    private final CalendarRepository calendarRepository;

    @EventListener(EventDateChangedEvent.class)
    public void onEventDateChanged(EventDateChangedEvent event) {
        Event aggregate = event.getAggregate();

        // Find existing or create new
        EventCalendarItem item = calendarRepository
            .findEventItem(aggregate.getId(), EventCalendarItem.Type.EVENT_DATE)
            .map(existingItem -> {
                existingItem.reschedule(
                    Globals.toZonedDateTime(aggregate.getDate()),
                    Globals.toZonedDateTime(aggregate.getDate())
                );
                return existingItem;
            })
            .orElseGet(() -> EventCalendarItem.eventDayItem(aggregate));

        calendarRepository.save(item);
    }
}
```

### ❌ Bad: Tight Coupling Between Aggregates

```java
public class EventDomainService {
    private final EventRepository eventRepo;
    private final CalendarRepository calendarRepo;

    public void changeEventDate(Event.Id eventId, LocalDate newDate) {
        // Directly modifies another aggregate - violates DDD
        Event event = eventRepo.findById(eventId).orElseThrow();
        CalendarItem item = calendarRepo.findById(...).orElseThrow();
        item.reschedule(newDate);  // Business logic scattered
        calendarRepo.save(item);
    }
}
```

## Pattern 8: REST Controller with HATEOAS

### ✅ Good: DTOs with Links, Processors Enrich Response

```java
@ApiController(openApiTagName = "calendar", path = "/calendar-items")
@ExposesResourceFor(CalendarItem.class)
public class CalendarApiController {

    @Relation(collectionRelation = "calendarItems")
    public record CalendarItemDto(
        LocalDate start,
        LocalDate end,
        String note,
        @JsonIgnore Link relatedItem  // Don't include in JSON
    ) {}

    private CalendarItemDto toDto(CalendarItem item) {
        Link relatedItemLink = null;
        if (item instanceof EventCalendarItem eci) {
            relatedItemLink = entityLinks.linkForItemResource(
                Event.class,
                eci.getEventId()
            ).withRel("event");
        }
        return new CalendarItemDto(
            Globals.toLocalDate(item.getStart()),
            Globals.toLocalDate(item.getEnd()),
            item.getNote(),
            relatedItemLink
        );
    }

    @GetMapping
    public CollectionModel<EntityModel<CalendarItemDto>> list() {
        var items = service.getCalendarItems(MONTH, null);
        return modelAssembler.toCollectionModel(items);
    }
}

@Component
class CalendarItemListPostprocessor
    implements RepresentationModelProcessor<CollectionModel<EntityModel<CalendarItemDto>>> {

    @Override
    public CollectionModel<EntityModel<CalendarItemDto>> process(CollectionModel<...> model) {
        model.add(linkTo(methodOn(CalendarApiController.class)
            .getCalendarItems(null, null))
            .withSelfRel()
            .andAffordances(affordBetter(methodOn(...).createCalendarItem(null))));

        return model;
    }
}
```

### ❌ Bad: JSON-Only Response

```java
@GetMapping
public List<CalendarItemDto> list() {
    // Returns raw JSON, no links, no discoverability
    return items.stream().map(this::toDto).collect(toList());
}
```

## Pattern 9: Custom Jackson Converters

### ✅ Good: Type-Safe Serialization

```java
@JacksonComponent
class IdConverter extends ValueSerializer<CalendarItem.Id>
    implements Converter<CalendarItem.Id, String> {

    @Override
    public void serialize(CalendarItem.Id value, JsonGenerator gen, SerializationContext ctxt) {
        ctxt.findValueSerializer(Long.class)
            .serialize(value != null ? value.value() : null, gen, ctxt);
    }

    @Override
    public String convert(CalendarItem.Id source) {
        return source != null ? Long.toString(source.value()) : null;
    }
}

@JacksonComponent
class IdDeserializer extends ValueDeserializer<CalendarItem.Id>
    implements Converter<String, CalendarItem.Id> {

    @Override
    public CalendarItem.Id deserialize(JsonParser p, DeserializationContext ctxt) {
        Long val = p.readValueAs(Long.class);
        return val != null ? new CalendarItem.Id(val) : null;
    }

    @Override
    public CalendarItem.Id convert(String source) {
        return source != null ? new CalendarItem.Id(Long.parseLong(source)) : null;
    }
}
```

### ❌ Bad: String-Based IDs in Domain

```java
public class CalendarItem {
    private String id;  // Type-unsafe, loses meaning

    // Conversions scattered throughout code
    public long idAsLong() {
        return Long.parseLong(id);
    }
}
```

## Pattern 10: Validation in Records vs Methods

### ✅ Good: Validation Where It Belongs

```java
// Record constructor - validates immutable properties
public record CreateItemCommand(String name, LocalDate dueDate) {
    public CreateItemCommand {
        Assert.hasText(name, "Name required");
        Assert.notNull(dueDate, "Due date required");
        Assert.isTrue(!dueDate.isBefore(LocalDate.now()), "Cannot schedule in past");
    }
}

// Aggregate method - enforces business rules
public class MyAggregate {
    public Item create(CreateItemCommand cmd) {
        if (this.items.size() >= maxItems) {
            throw new IllegalArgumentException("Too many items");
        }
        return Item.create(cmd.name(), cmd.dueDate());
    }
}
```

### ❌ Bad: Scattered Validation

```java
public class CreationService {
    public Item create(String name, LocalDate dueDate) {
        // Validation scattered
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name required");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Date required");
        }
        // More validation...

        Item item = new Item(name, dueDate);
        return item;
    }
}
```

## Pattern 11: Fluent API with Builder

### ✅ Good: Chainable Methods Return Self

```java
public class ConfigurableItem extends Item {
    public ConfigurableItem withName(String name) {
        this.name = name;
        return this;
    }

    public ConfigurableItem withDescription(String desc) {
        this.description = desc;
        return this;
    }

    public ConfigurableItem withStatus(Status status) {
        this.status = status;
        return this;
    }
}

// Usage
var item = new ConfigurableItem()
    .withName("Task")
    .withDescription("Important")
    .withStatus(ACTIVE);
```

### ❌ Bad: Getters/Setters

```java
Item item = new Item();
item.setName("Task");
item.setDescription("Important");
item.setStatus(ACTIVE);
// Multiple mutation points, no clear intent
```

## Pattern 12: Optional Handling

### ✅ Good: Expressive Optional Chains

```java
Optional<Item> result = repository.findById(id)
    .filter(item -> item.isActive())
    .map(item -> {
        item.update(cmd);
        return item;
    });

// Create or update pattern
Item item = repository.findById(id)
    .map(existing -> {
        existing.update(cmd);
        return existing;
    })
    .orElseGet(() -> Item.create(cmd));

repository.save(item);
```

### ❌ Bad: Null Checks

```java
Item item = repository.findById(id);
if (item != null && item.isActive()) {
    item.update(cmd);
    repository.save(item);
} else if (item == null) {
    item = Item.create(cmd);
    repository.save(item);
}
```

## Pattern 13: Immutable Collections Return

### ✅ Good: Return Unmodifiable Collections

```java
public class MyAggregate {
    private Set<Item> items = new HashSet<>();

    public Set<Item> getItems() {
        return Collections.unmodifiableSet(items);
    }

    public List<Item> getOrderedItems() {
        return Collections.unmodifiableList(
            items.stream().sorted().collect(toList())
        );
    }
}

// Usage
Set<Item> items = aggregate.getItems();
items.add(new Item());  // Throws UnsupportedOperationException
```

### ❌ Bad: Exposing Mutable Collections

```java
public Set<Item> getItems() {
    return items;  // Anyone can modify!
}

// Usage
aggregate.getItems().add(new Item());  // Silently corrupts aggregate
```

## Pattern 14: DateTime Conversion

### ✅ Good: Consistent Conversion Points

```java
// Domain uses ZonedDateTime
public class Item {
    private ZonedDateTime createdAt = ZonedDateTime.now();
}

// DTO uses LocalDate
public record ItemDto(LocalDate createdDate, ...) {}

// Conversion in controller
private ItemDto toDto(Item item) {
    return new ItemDto(
        Globals.toLocalDate(item.getCreatedAt()),
        ...
    );
}
```

### ❌ Bad: Mixed DateTime Types

```java
public class Item {
    private LocalDateTime createdAt;  // Timezone lost
    private String timezone;  // Reconstructing timezone

    public ZonedDateTime getZonedTime() {
        return ZonedDateTime.of(createdAt, ZoneId.of(timezone));
    }
}
```

## Pattern 15: Error Messages

### ✅ Good: Clear, Actionable Messages

```java
if (!period.includes(date)) {
    throw new IllegalArgumentException(
        "Date %s is not within calendar period %s-%s".formatted(
            date, period.start(), period.end()
        )
    );
}
```

### ❌ Bad: Vague Messages

```java
throw new IllegalArgumentException("Invalid date");
// User has no idea what the issue is
```
