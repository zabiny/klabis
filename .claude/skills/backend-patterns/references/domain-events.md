# Domain Events

Event structure and cross-module listeners. Events are registered by the aggregate and
published by the memento during save.


## Event Structure

```java
public record MemberCreatedEvent(
        UUID eventId,             // always include — idempotency
        MemberId memberId,
        // ... domain-relevant data, denormalized for listener convenience
        Instant occurredAt
) {
    public static MemberCreatedEvent fromAggregate(Member member) { ... }

    @Override
    public String toString() {    // exclude PII fields (GDPR)
        return "MemberCreatedEvent{eventId=" + eventId + ", memberId=" + memberId + "}";
    }
}
```

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
