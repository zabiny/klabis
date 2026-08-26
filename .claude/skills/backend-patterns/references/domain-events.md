# Domain Events

Event structure and cross-module listeners. Events are registered by the aggregate and
published by the memento during save.


## Event Structure

```java
@DomainEvent
public class MemberCreatedEvent {
    private final UUID eventId;        // Always include for idempotency
    private final MemberId memberId;
    private final Instant occurredAt;
    // ... domain-relevant data (denormalized for listener convenience)

    public static MemberCreatedEvent fromMember(Member member) { ... }

    @Override
    public String toString() {
        // Exclude PII fields (GDPR compliance)
        return "MemberCreatedEvent{eventId=" + eventId + ", memberId=" + memberId + "}";
    }
}
```

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
