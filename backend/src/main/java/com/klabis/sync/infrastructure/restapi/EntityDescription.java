package com.klabis.sync.infrastructure.restapi;

import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.Link;

import java.util.Optional;

/**
 * Readable name of a synchronised entity and, when the caller may see it, the link to the entity's own resource.
 */
public record EntityDescription(String label, @Nullable Link entityLink) {

    public EntityDescription {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Entity label must not be blank");
        }
    }

    public static EntityDescription of(String label, Optional<Link> entityLink) {
        return new EntityDescription(label, entityLink.orElse(null));
    }
}
