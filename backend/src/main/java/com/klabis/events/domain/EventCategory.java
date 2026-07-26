package com.klabis.events.domain;

import org.jmolecules.ddd.annotation.Entity;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.util.Optional;

/**
 * A race category offered at an event (e.g. "M21", "W35").
 * <p>
 * Identity is a local {@link EventCategoryId}, stable across renames — this is what lets
 * {@link EventRegistration} keep pointing at the same category even after the event manager
 * (or an ORIS sync) changes its display name.
 * <p>
 * {@code orisId} pairs this category with an ORIS {@code EventClass} for sync matching; it is
 * {@code null} for categories created manually and is never set through the REST API.
 */
@Entity
public record EventCategory(
        @Identity EventCategoryId id,
        String orisId,
        String name,
        Money feeOverride
) {

    public EventCategory {
        Assert.notNull(id, "Event category ID is required");
        Assert.hasText(name, "Event category name is required");
    }

    public static EventCategory create(String name) {
        return new EventCategory(EventCategoryId.generate(), null, name, null);
    }

    public static EventCategory createFromOris(String orisId, String name) {
        return new EventCategory(EventCategoryId.generate(), orisId, name, null);
    }

    public Optional<Money> fee() {
        return Optional.ofNullable(feeOverride);
    }
}
