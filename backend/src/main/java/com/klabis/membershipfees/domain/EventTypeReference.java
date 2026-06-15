package com.klabis.membershipfees.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.springframework.util.Assert;

import java.util.UUID;

/**
 * An opaque reference to an event type stored by the events module.
 * This type exists to avoid a dependency on events.EventTypeId from within the
 * membershipfees domain which woulc create cyclic dependency (events needs memberfees to determine registration prices) — membershipfees only needs to store and compare the UUID,
 * not act on the actual EventType aggregate.
 */
@ValueObject
public record EventTypeReference(UUID value) {

    public EventTypeReference {
        Assert.notNull(value, "EventTypeReference value is required");
    }

    public static EventTypeReference of(UUID value) {
        return new EventTypeReference(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
