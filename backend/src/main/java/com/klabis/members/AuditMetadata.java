package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;
import java.util.Objects;

/**
 * Value object containing audit metadata for an entity.
 * <p>
 * Encapsulates common audit fields: creation timestamp, creator,
 * last modification timestamp, modifier, and optimistic locking version.
 *
 * @param createdAt      timestamp when the entity was created
 * @param createdBy      identifier of who created the entity
 * @param lastModifiedAt timestamp when the entity was last modified
 * @param lastModifiedBy identifier of who last modified the entity
 * @param version        optimistic locking version
 */
@ValueObject
public record AuditMetadata(
        Instant createdAt,
        String createdBy,
        Instant lastModifiedAt,
        String lastModifiedBy,
        Long version
) {
    public AuditMetadata {
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    /**
     * Creates a new AuditMetadata with updated modification timestamp.
     *
     * @param modifiedBy identifier of who modified the entity
     * @return new AuditMetadata with updated modification fields
     */
    public AuditMetadata markModified(String modifiedBy) {
        return new AuditMetadata(
                this.createdAt,
                this.createdBy,
                Instant.now(),
                modifiedBy,
                this.version
        );
    }

    /**
     * Creates a new AuditMetadata with incremented version.
     *
     * @return new AuditMetadata with incremented version
     */
    public AuditMetadata incrementVersion() {
        return new AuditMetadata(
                this.createdAt,
                this.createdBy,
                this.lastModifiedAt,
                this.lastModifiedBy,
                this.version == null ? 1L : this.version + 1
        );
    }
}
