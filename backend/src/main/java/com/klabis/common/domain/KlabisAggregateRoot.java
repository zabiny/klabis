package com.klabis.common.domain;

import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.domain.DomainEvents;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Base class for all aggregate roots in the domain model.
 * <p>
 * Provides common functionality for all aggregates:
 * <ul>
 *   <li>Audit metadata tracking</li>
 *   <li>Domain event registration and publication</li>
 *   <li>Identity-based equality and hashCode</li>
 * </ul>
 * <p>
 * Each aggregate root should:
 * <ul>
 *   <li>Extend this class (e.g., {@code public class Member extends KlabisAggregateRoot<UserId>})</li>
 *   <li>Call {@link #registerEvent(Object)} to register domain events</li>
 *   <li>Delegate to {@link #getAuditMetadata()} for audit information</li>
 * </ul>
 *
 * @param <ID> the type of the aggregate's identifier (e.g., UserId, EventId, UUID)
 */
public abstract class KlabisAggregateRoot<A extends KlabisAggregateRoot<A, ID>, ID> extends AbstractAggregateRoot<A> {

    private AuditMetadata auditMetadata;

    /**
     * Gets the aggregate's unique identifier.
     *
     * @return the aggregate's ID, or null if not yet set
     */
    public abstract ID getId();

    /**
     * Gets the audit metadata for this aggregate.
     *
     * @return the audit metadata, or null if not yet set
     */
    public AuditMetadata getAuditMetadata() {
        return auditMetadata;
    }

    /**
     * Sets the audit metadata for this aggregate.
     * <p>
     * Called by the persistence layer after save to populate audit fields
     * (createdAt, createdBy, lastModifiedAt, lastModifiedBy, version).
     *
     * @param auditMetadata the audit metadata to apply
     */
    public void updateAuditMetadata(AuditMetadata auditMetadata) {
        this.auditMetadata = auditMetadata;
    }

    /**
     * Gets the creation timestamp.
     *
     * @return creation timestamp, or null if not set
     */
    public Instant getCreatedAt() {
        return auditMetadata != null ? auditMetadata.createdAt() : null;
    }

    /**
     * Gets the user who created this aggregate.
     *
     * @return creator user identifier, or null if not set
     */
    public String getCreatedBy() {
        return auditMetadata != null ? auditMetadata.createdBy() : null;
    }

    /**
     * Gets the last modification timestamp.
     *
     * @return last modification timestamp, or null if not set
     */
    public Instant getLastModifiedAt() {
        return auditMetadata != null ? auditMetadata.lastModifiedAt() : null;
    }

    /**
     * Gets the user who last modified this aggregate.
     *
     * @return last modifier user identifier, or null if not set
     */
    public String getLastModifiedBy() {
        return auditMetadata != null ? auditMetadata.lastModifiedBy() : null;
    }

    /**
     * Gets the optimistic locking version.
     *
     * @return version number, or null if not set
     */
    public Long getVersion() {
        return auditMetadata != null ? auditMetadata.version() : null;
    }

    /**
     * Get all domain events registered on this aggregate.
     * <p>
     * Public accessor that returns the domain events list.
     * Annotated with @DomainEvents for Spring Modulith automatic event publishing.
     *
     * @return unmodifiable list of domain events
     */
    @DomainEvents
    public List<Object> getDomainEvents() {
        return List.copyOf(super.domainEvents());
    }

    /**
     * Clears all domain events (typically called after publishing).
     * <p>
     * Public method that delegates to the parent AbstractAggregateRoot class.
     * This can be called by the persistence layer after event publication.
     */
    public void clearDomainEvents() {
        super.clearDomainEvents();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KlabisAggregateRoot<?, ?> that = (KlabisAggregateRoot<?, ?>) o;
        return Objects.equals(getId(), that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
