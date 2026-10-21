# Domain Events

Event structure and cross-module listeners. Events are registered by the aggregate and
published by the memento during save.


## Event Structure

```java
public record MemberCreatedEvent(
        UUID eventId,             // always include — idempotency
        MemberId memberId,        // aggregate id
        LocalDate dateOfBirth,    // only because the groups listener reads it
        Instant occurredAt
) {
    public static MemberCreatedEvent fromAggregate(Member member) { ... }
}
```

Carry ids plus only what current listeners actually read; listeners load the rest through the owning module's port. This keeps events stable when the aggregate grows and keeps PII out of the Modulith outbox (`event_publication`) — add a field only when a listener needs it. Do not create an event nobody listens to.

Events consumed by other modules live in the module root package (public API); module-internal events may stay in `domain/`.

## Cross-Module Event Listeners

```java
@PrimaryAdapter
@Component
public class MemberEventsListener {

    @ApplicationModuleListener
    public void on(MemberCreatedEvent event) {
        // React to cross-module domain event
    }
}
```

Use `@ApplicationModuleListener` (Spring Modulith) for cross-module event handling. Use `@PrimaryAdapter` on ALL inbound adapters — REST controllers AND event listeners.
