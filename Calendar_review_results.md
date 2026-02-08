# Calendar Feature Code Review

## Executive Summary

The calendar feature demonstrates good adherence to DDD principles with clear separation of concerns. However, there are
several architectural inconsistencies and maintainability issues that need addressing, particularly around aggregate
design, entity hierarchy, and cross-domain integration patterns.

## Critical Issues

### 1. CalendarItem as Aggregate Root - Questionable Design

**Location:** `CalendarItem.java:14-15`

**Issue:** `CalendarItem` is marked as `@AggregateRoot` and extends `AbstractAggregateRoot<CalendarItem>`, but it
doesn't exhibit true aggregate characteristics. An aggregate should:

- Enforce business invariants
- Manage a cluster of related entities
- Have meaningful business rules

Currently, `CalendarItem` is more of a simple entity with no child entities or complex invariants.

**Impact:**

- Violates DDD principles by misusing aggregate pattern
- Creates confusion about transactional boundaries
- Makes the domain model harder to understand

**Recommendation:**

- Either promote `Calendar` to be the aggregate root (containing `CalendarItem` entities), OR
- If `CalendarItem` truly needs to be an aggregate, remove `Calendar` class and handle periods differently
- Based on the current implementation, `Calendar` appears to be a read model/query result, not a true aggregate

### 2. Calendar Class Lacks Aggregate Characteristics

**Location:** `Calendar.java:16-95`

**Issue:** The `Calendar` class is missing `@AggregateRoot` annotation but behaves like a read model. It:

- Has no identity field
- Cannot be persisted
- Has no business methods for modifying state
- Acts only as a container for query results

**Impact:**

- Ambiguous role in the domain model
- Cannot track which calendar instance you're working with
- No clear transactional boundary

**Recommendation:**
If `Calendar` should be the aggregate root:

```java
@AggregateRoot
public class Calendar extends AbstractAggregateRoot<Calendar> {
    @Identity
    private Id id;
    private CalendarPeriod period;
    private Set<CalendarItem> items = new HashSet<>();

    public record Id(long value) { /* ... */ }

    // Business methods
    public CalendarItem addItem(CreateCalendarItemCommand cmd) {
        CalendarItem item = CalendarItem.calendarItem(...);
        items.add(item);
        return item;
    }
}
```

### 3. EventCalendarItem Inheritance Creates Tight Coupling

**Location:** `EventCalendarItem.java:8-50`

**Issues:**

- Factory methods `registrationsDeadlineItem()` and `eventDayItem()` accept full `Event` aggregate (line 17, 26)
- Creates dependency from Calendar domain to Events domain
- TODO comments acknowledge this is wrong (line 16, 25)
- Hardcoded Czech text in domain layer (line 21, 30)

**Impact:**

- Violates bounded context isolation
- Makes testing harder (need to create Event instances)
- Language mixing in domain layer

**Recommendation:**

```java
// Remove Event parameter from factory methods
public static EventCalendarItem registrationsDeadlineItem(
    Event.Id eventId,
    ZonedDateTime deadline,
    String eventName) {
    return new EventCalendarItem(
        Id.newId(),
        deadline,
        deadline,
        "%s - Uzaverka prihlasek".formatted(eventName),
        eventId,
        Type.EVENT_REGISTRATION_DEADLINE
    );
}

// Move Czech text to configuration/i18n
private static final String REGISTRATION_DEADLINE_TEMPLATE = "%s - Uzaverka prihlasek";
```

### 4. Missing @Identity Annotation

**Location:** `Calendar.java:18`

**Issue:** The `period` field in `Calendar` could be considered an identity, but there's no `@Identity` annotation
anywhere in the class. This violates the DDD pattern where aggregates must have explicit identity.

**Impact:**

- Cannot distinguish between different calendar instances
- Repository cannot properly implement identity-based queries
- Unclear lifecycle management

### 5. Entity vs Aggregate Confusion with CalendarItem

**Location:** `CalendarItem.java:14-15`

**Issue:** `CalendarItem` is marked as `@AggregateRoot`, but in `Calendar.java:19` it's used as a child entity in a
collection. This is contradictory:

- If it's an aggregate root, it should be independently persisted and referenced by ID
- If it's an entity, it should be marked with `@Entity` and owned by a parent aggregate

**Current inconsistency:**

```java
// Calendar.java - treats CalendarItem as child entity
private Set<CalendarItem> items = new HashSet<>();

// CalendarItem.java - declares itself as aggregate root
@AggregateRoot
public class CalendarItem extends AbstractAggregateRoot<CalendarItem>
```

**Recommendation:**
Choose one approach:

**Option A: CalendarItem as Aggregate Root (current approach)**

```java
// Calendar becomes a read model
public class Calendar {
    private CalendarPeriod period;
    private Set<CalendarItem.Id> itemIds; // Store IDs only
}
```

**Option B: Calendar as Aggregate Root (recommended)**

```java
@AggregateRoot
public class Calendar {
    @Identity
    private Id id;
    private Set<CalendarItem> items; // CalendarItem becomes @Entity
}

@Entity
public class CalendarItem { // Remove @AggregateRoot
    @Identity
    private Id id;
    // ...
}
```

## Major Issues

### 6. CalendarService Bypasses Aggregate Business Logic

**Location:** `CalendarService.java:35-42`

**Issue:** The `createCalendarItem` method creates and configures the aggregate directly instead of using aggregate
methods:

```java
public CalendarItem createCalendarItem(CreateCalendarItemCommand command) {
    CalendarItem result = CalendarItem.calendarItem(
            Globals.toZonedDateTime(command.start()),
            Globals.toZonedDateTime(command.end()));
    result.updateNote(command.note());
    return calendarRepository.save(result);
}
```

**Impact:**

- Business logic leaks into application service
- CalendarItem doesn't handle its own creation command
- Pattern inconsistency with DDD best practices

**Recommendation:**

```java
// In CalendarItem
public static CalendarItem fromCommand(CreateCalendarItemCommand command) {
    CalendarItem item = calendarItem(
        Globals.toZonedDateTime(command.start()),
        Globals.toZonedDateTime(command.end())
    );
    if (command.note() != null) {
        item.updateNote(command.note());
    }
    return item;
}

// In CalendarService
@Transactional
public CalendarItem createCalendarItem(CreateCalendarItemCommand command) {
    CalendarItem item = CalendarItem.fromCommand(command);
    return calendarRepository.save(item);
}
```

### 7. Missing Business Invariant Validation

**Location:** `CalendarItem.java:62-67`

**Issue:** The `reschedule` method validates that dates are not null, but doesn't validate that `start` is before or
equal to `end`:

```java
public void reschedule(ZonedDateTime newStart, ZonedDateTime newEnd) {
    Assert.notNull(newStart, "Calendar item start date must not be null");
    Assert.notNull(newEnd, "Calendar item end date must not be null");
    // Missing: start <= end validation
    this.start = newStart;
    this.end = newEnd;
}
```

**Impact:**

- Can create invalid calendar items with end before start
- Inconsistent with `CalendarPeriod` validation (line 30)

**Recommendation:**

```java
public void reschedule(ZonedDateTime newStart, ZonedDateTime newEnd) {
    Assert.notNull(newStart, "Calendar item start date must not be null");
    Assert.notNull(newEnd, "Calendar item end date must not be null");
    Assert.isTrue(!newStart.isAfter(newEnd), "Start date must not be after end date");
    this.start = newStart;
    this.end = newEnd;
}
```

### 8. MemberListeners Creates Duplicate Calendar Items

**Location:** `MemberListeners.java:34-43`

**Issue:** Both `onMemberCreated` and `onMemberUpdated` call `handleMemberBirthday`, which always creates new calendar
items. This will create duplicate birthday entries on every member update.

```java
@EventListener(MemberEditedEvent.class)
public void onMemberUpdated(MemberEditedEvent event) {
    handleMemberBirthday(event.getAggregate()); // Creates duplicate!
}

private void handleMemberBirthday(Member member) {
    // TODO: complete this handling
    calendarService.createCalendarItem(...); // Always creates new
}
```

**Impact:**

- Database pollution with duplicate entries
- Performance degradation over time
- Incorrect business behavior

**Recommendation:**

```java
private void handleMemberBirthday(Member member) {
    LocalDate memberBirthDay = member.getDateOfBirth();
    if (memberBirthDay == null) {
        return;
    }

    LocalDate thisYearBirthday = memberBirthDay.withYear(LocalDate.now().getYear());

    // Find or create pattern
    Optional<CalendarItem> existing = calendarRepository
        .findBirthdayItem(member.getId(), thisYearBirthday);

    if (existing.isEmpty()) {
        calendarService.createCalendarItem(
            CreateCalendarItemCommand.task(
                thisYearBirthday,
                "%s %s - Narozeniny".formatted(
                    member.getFirstName(),
                    member.getLastName()
                )
            )
        );
    }
}
```

Note: Requires adding `findBirthdayItem` to repository interface.

### 9. EventListeners Method Naming Inconsistency

**Location:** `EventListeners.java:43-44`

**Issue:** Both event handler methods are named `onDeadlineChanged`:

```java
@EventListener(EventRegistrationsDeadlineChangedEvent.class)
public void onDeadlineChanged(EventRegistrationsDeadlineChangedEvent event) { }

@EventListener(EventDateChangedEvent.class)
public void onDeadlineChanged(EventDateChangedEvent event) { } // Wrong name!
```

**Impact:**

- Confusing code - second method name is misleading
- Harder to debug and trace

**Recommendation:**

```java
@EventListener(EventDateChangedEvent.class)
public void onEventDateChanged(EventDateChangedEvent event) {
    // ...
}
```

## Moderate Issues

### 10. Unused Private Method

**Location:** `EventListeners.java:25-27`

**Issue:** Method `setCalendarScheduleFromEvent` is defined but never called:

```java
private void setCalendarScheduleFromEvent(CalendarItem item, LocalDate date) {
    item.reschedule(toZonedDateTime(date), toZonedDateTime(date));
}
```

**Impact:**

- Dead code clutters the codebase
- Suggests incomplete refactoring

**Recommendation:** Remove if truly unused, or use it to reduce duplication.

### 11. CalendarPeriod.includes() Logic Error

**Location:** `Calendar.java:33-35`

**Issue:** The `includes` method has incorrect boundary logic:

```java
public boolean includes(LocalDate date) {
    return periodStart.isBefore(date) && periodEnd.isAfter(date) || periodStart.equals(date);
}
```

Problems:

- Doesn't include `periodEnd` when `date.equals(periodEnd)`
- Logic is hard to read due to operator precedence
- Missing parentheses make intent unclear

**Impact:**

- Items on the last day of period might be excluded
- Off-by-one errors in queries

**Recommendation:**

```java
public boolean includes(LocalDate date) {
    return !periodStart.isAfter(date) && !periodEnd.isBefore(date);
}
```

Or more explicitly:

```java
public boolean includes(LocalDate date) {
    return (date.isEqual(periodStart) || date.isAfter(periodStart))
        && (date.isEqual(periodEnd) || date.isBefore(periodEnd));
}
```

### 12. Repository Method Naming Inconsistency

**Location:** `CalendarRepository.java:17-19`

**Issue:** Three different naming patterns for similar operations:

- `findEventItem` (line 15) - returns `Optional<EventCalendarItem>`
- `getCalendar` (line 17) - returns `Collection<CalendarItem>`
- `readCalendar` (line 19) - returns `Calendar`

**Impact:**

- Unclear semantic differences between `get` and `read`
- Inconsistent with DDD patterns where `read` typically loads full aggregate

**Recommendation:**
Standardize naming:

```java
public interface CalendarRepository extends DataRepository<CalendarItem, CalendarItem.Id> {
    Optional<EventCalendarItem> findEventItem(Event.Id eventId, EventCalendarItem.Type itemType);
    List<CalendarItem> findItemsInPeriod(Calendar.CalendarPeriod period);
    Calendar readCalendarForPeriod(Calendar.CalendarPeriod period); // More descriptive
}
```

### 13. Missing Transactional Annotations

**Location:** `EventListeners.java:29-54`, `MemberListeners.java:23-43`

**Issue:** Event listener methods perform database writes but lack `@Transactional` annotations.

**Impact:**

- Unclear transaction boundaries
- Risk of partial updates
- Potential data inconsistency

**Recommendation:**

```java
@Transactional
@EventListener(EventRegistrationsDeadlineChangedEvent.class)
public void onDeadlineChanged(EventRegistrationsDeadlineChangedEvent event) {
    // ...
}
```

### 14. CreateCalendarItemCommand Missing Validation

**Location:** `CreateCalendarItemCommand.java:8`

**Issue:** The command record validates `@NotNull` for dates but doesn't validate that `start <= end`:

```java
public record CreateCalendarItemCommand(
    @NotNull LocalDate start,
    @NotNull LocalDate end,
    String note) {
```

**Impact:**

- Invalid commands can reach the domain layer
- Validation happens too late in the flow

**Recommendation:**

```java
public record CreateCalendarItemCommand(
    @NotNull LocalDate start,
    @NotNull LocalDate end,
    String note) {

    public CreateCalendarItemCommand {
        Assert.notNull(start, "Start date must not be null");
        Assert.notNull(end, "End date must not be null");
        Assert.isTrue(!start.isAfter(end), "Start date must not be after end date");
    }

    public static CreateCalendarItemCommand task(LocalDate date, String note) {
        return new CreateCalendarItemCommand(date, date, note);
    }
}
```

### 15. Unnecessary result Variable

**Location:** `CalendarInMemoryRepository.java:36-37`

**Issue:**

```java
Calendar result = new Calendar(calendarPeriod, getCalendar(calendarPeriod));
return result;
```

**Recommendation:**

```java
return new Calendar(calendarPeriod, getCalendar(calendarPeriod));
```

## Minor Issues

### 16. Inconsistent Factory Method Naming

**Location:** `CalendarItem.java:39, 45, 73`

**Issue:** Mix of different factory method styles:

- `calendarItem()` - noun form (line 39)
- `task()` - noun form (line 45)
- `todayTask()` - combination form (line 73)

**Recommendation:** Standardize to descriptive names:

```java
public static CalendarItem timespan(ZonedDateTime start, ZonedDateTime end)
public static CalendarItem dayTask(LocalDate day, String note)
public static CalendarItem todayTask(String note)
```

### 17. Magic String in Id.toString()

**Location:** `CalendarItem.java:35`

**Issue:** Hardcoded format string:

```java
return "CALENDARITEM_%d".formatted(value);
```

**Recommendation:**

```java
private static final String ID_FORMAT = "CALENDARITEM_%d";

@Override
public String toString() {
    return ID_FORMAT.formatted(value);
}
```

### 18. Id Generation Thread-Safety Concerns

**Location:** `CalendarItem.java:27-31`

**Issue:** Uses `synchronized` keyword for ID generation:

```java
static long MAX_VALUE = 0L;

static synchronized Id newId() {
    return new Id(MAX_VALUE++);
}
```

**Impact:**

- Not suitable for production (resets on restart)
- Synchronization overhead
- Won't work in distributed systems

**Recommendation:** This is acceptable for in-memory testing but should be documented as such, or use database-generated
IDs.

### 19. Protected No-Args Constructors Not Documented

**Location:** `CalendarItem.java:51-53`, `EventCalendarItem.java:34-35`

**Issue:** Protected no-args constructors exist for JPA/persistence framework compatibility but lack documentation:

```java
protected CalendarItem() {

}
```

**Recommendation:**

```java
/**
 * Protected no-args constructor for persistence framework.
 * Do not use directly - use factory methods instead.
 */
protected CalendarItem() {
}
```

### 20. Unused FindPredicate in CalendarInMemoryRepository

**Location:** `CalendarInMemoryRepository.java:42-54`

**Issue:** `FindPredicate` is a separate class but only used once in line 31. Could be simplified with lambda.

**Recommendation:**

```java
@Override
default Collection<CalendarItem> getCalendar(Calendar.CalendarPeriod calendarPeriod) {
    return findAll(item ->
        calendarPeriod.includes(item.getStart()) ||
        calendarPeriod.includes(item.getEnd())
    );
}
```

## Positive Aspects

### Strengths

1. **Good use of Records**: `CalendarPeriod`, `Id`, `CreateCalendarItemCommand` properly use Java records for value
   objects
2. **Proper annotations**: `@AggregateRoot`, `@Identity`, `@Repository`, `@SecondaryPort` correctly applied
3. **Immutable collections**: `getItems()` returns unmodifiable set (Calendar.java:85)
4. **Factory methods**: Good use of static factory methods for object creation
5. **Separation of concerns**: Clear separation between domain, infrastructure, and API layers
6. **Proper DTO usage**: `CalendarItemDto` correctly separates API contract from domain model
7. **HATEOAS implementation**: Good hypermedia link generation in controller
8. **Test coverage**: Comprehensive API controller tests

### Code Organization

- Clear package structure following hexagonal architecture
- Domain classes in `club.klabis.calendar` package
- Infrastructure in `club.klabis.calendar.infrastructure`
- Proper use of jMolecules annotations for architectural clarity

## Maintainability Assessment

### Ease of Understanding: 7/10

**Positive:**

- Clear naming conventions
- Good separation of concerns
- Factory methods make intent clear

**Negative:**

- Ambiguous aggregate boundaries confuse the model
- Missing documentation on key classes
- Inconsistent method naming patterns

### Ease of Modification: 6/10

**Positive:**

- Small, focused methods
- Good use of dependency injection
- Testable structure

**Negative:**

- Tight coupling between Calendar and Events domains (EventCalendarItem)
- Unclear transaction boundaries
- Missing business logic in aggregates

### Ease of Testing: 8/10

**Positive:**

- Good test coverage on API layer
- Mockable dependencies
- Factory methods simplify test setup

**Negative:**

- Domain logic in service layer harder to test in isolation
- Event listeners lack unit tests

## Recommendations Priority

### High Priority (Fix Immediately)

1. ✅ Fix CalendarItem/Calendar aggregate design (Issue #1, #2, #5)
2. ✅ Fix MemberListeners duplicate creation bug (Issue #8)
3. ✅ Add validation for start <= end in CalendarItem.reschedule() (Issue #7)
4. ✅ Fix CalendarPeriod.includes() boundary logic (Issue #11)

### Medium Priority (Fix in Next Sprint)

5. ✅ Refactor EventCalendarItem to remove Event dependency (Issue #3)
6. ✅ Move command handling to aggregate (Issue #6)
7. ✅ Add @Transactional to event listeners (Issue #13)
8. ✅ Fix event listener method naming (Issue #9)
9. ✅ Add validation to CreateCalendarItemCommand (Issue #14)

### Low Priority (Technical Debt)

10. Remove unused method in EventListeners (Issue #10)
11. Standardize repository method naming (Issue #12)
12. Add documentation to protected constructors (Issue #19)
13. Refactor FindPredicate to lambda (Issue #20)
14. Standardize factory method naming (Issue #16)

## Conclusion

The calendar feature demonstrates solid fundamentals in DDD and hexagonal architecture but suffers from architectural
ambiguity around aggregate design. The most critical issue is the unclear relationship between `Calendar` and
`CalendarItem` - they need to have a clear aggregate root/entity hierarchy.

The codebase is generally maintainable and well-structured, with good test coverage and separation of concerns. However,
addressing the aggregate design issues will significantly improve code clarity and reduce confusion for future
developers.

**Overall Grade: B-**

The feature works but needs architectural refinement to fully align with DDD best practices and improve long-term
maintainability.
