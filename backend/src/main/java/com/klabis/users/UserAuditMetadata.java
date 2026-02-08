package com.klabis.users;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object containing audit metadata for User entity.
 * <p>
 * Encapsulates common audit fields: creation timestamp, creator,
 * last modification timestamp, modifier, and optimistic locking version.
 *
 * @param createdAt      timestamp when the user was created
 * @param createdBy      identifier of who created the user
 * @param lastModifiedAt timestamp when the user was last modified
 * @param lastModifiedBy identifier of who last modified the user
 * @param version        optimistic locking version
 */
@ValueObject
public record UserAuditMetadata(
        Instant createdAt,
        String createdBy,
        Instant lastModifiedAt,
        String lastModifiedBy,
        Long version
) {
    public UserAuditMetadata {
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }
}
