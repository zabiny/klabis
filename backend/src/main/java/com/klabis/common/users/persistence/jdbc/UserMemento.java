package com.klabis.common.users.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.users.AccountStatus;
import com.klabis.common.users.User;
import com.klabis.common.users.UserId;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Memento pattern implementation for User aggregate persistence.
 * <p>
 * This class acts as a bridge between the pure domain {@link User} entity
 * and Spring Data JDBC persistence. It contains:
 * <ul>
 *   <li>All JDBC annotations for persistence</li>
 *   <li>Flat primitive fields matching the database schema</li>
 *   <li>Conversion methods to/from User</li>
 *   <li>Domain event delegation for Spring Modulith</li>
 * </ul>
 * <p>
 * The User entity remains a pure domain object without Spring annotations,
 * while this memento handles all infrastructure concerns.
 */
@Table("users")
public class UserMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_name")
    private String username;

    @Column("password_hash")
    private String passwordHash;

    @Column("account_status")
    private AccountStatus accountStatus;

    // Audit fields
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    // Transient reference to User for domain event delegation
    @Transient
    private User user;

    // Transient flag for Persistable<UUID>
    @Transient
    private boolean isNew = true;

    /**
     * Default constructor required by Spring Data JDBC.
     */
    protected UserMemento() {
    }

    /**
     * Check if this entity is new (not yet persisted).
     * Used by Spring Data JDBC to determine whether to perform INSERT or UPDATE.
     *
     * @return true if this is a new entity, false if already persisted
     */
    @Override
    public boolean isNew() {
        return this.isNew;
    }

    /**
     * Get the entity's unique identifier.
     *
     * @return the UUID of this entity
     */
    @Override
    public UUID getId() {
        return this.id;
    }

    /**
     * Creates a UserMemento from a User entity (for save operations).
     *
     * @param user the User entity to convert
     * @return a new UserMemento with all fields copied from the User
     */
    public static UserMemento from(User user) {
        UserMemento memento = new UserMemento();

        copyBasicInfo(user, memento);
        copyAuditMetadata(user, memento);

        // Store transient reference to User for domain event delegation
        memento.user = user;

        // Set isNew flag based on whether user has createdAt (new users don't have audit metadata yet)
        memento.isNew = (user.getCreatedAt() == null);

        return memento;
    }

    /**
     * Copies basic user information from User to memento.
     */
    private static void copyBasicInfo(User user, UserMemento memento) {
        memento.id = user.getId() != null ? user.getId().uuid() : null;
        memento.username = user.getUsername();
        memento.passwordHash = user.getPasswordHash();
        memento.accountStatus = user.getAccountStatus();
    }

    /**
     * Copies audit metadata from User to memento.
     */
    private static void copyAuditMetadata(User user, UserMemento memento) {
        if (user.getCreatedAt() != null) {
            memento.createdAt = user.getCreatedAt();
            memento.createdBy = user.getCreatedBy();
            memento.lastModifiedAt = user.getLastModifiedAt();
            memento.lastModifiedBy = user.getLastModifiedBy();
            memento.version = user.getVersion();
        }
    }

    /**
     * Converts this memento to a User entity (for load operations).
     * <p>
     * Uses the User.reconstruct method to create a User instance bypassing validation.
     *
     * @return a User entity reconstructed from this memento
     */
    public User toUser() {
        // Use User.reconstruct method to create User instance
        UserId userId = this.id != null ? new UserId(this.id) : null;
        User user = User.reconstruct(userId, this.username, this.passwordHash, this.accountStatus);

        // Update audit metadata
        if (this.createdAt != null) {
            AuditMetadata auditMetadata = new AuditMetadata(
                    this.createdAt,
                    this.createdBy,
                    this.lastModifiedAt,
                    this.lastModifiedBy,
                    this.version
            );
            user.updateAuditMetadata(auditMetadata);
        }

        // Store transient reference for domain event delegation
        this.user = user;

        return user;
    }

    /**
     * Returns domain events from the associated User entity.
     * <p>
     * Annotated with @DomainEvents to enable Spring Modulith automatic event publishing.
     * Spring Data JDBC will collect and publish these events via the outbox pattern.
     *
     * @return list of domain events from the User entity
     */
    @DomainEvents
    public List<Object> getDomainEvents() {
        if (this.user != null) {
            return this.user.getDomainEvents();
        }
        return List.of();
    }

    /**
     * Clears domain events from the associated User entity.
     * <p>
     * Annotated with @AfterDomainEventPublication to ensure events are cleared
     * after they have been successfully published to the outbox.
     */
    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.user != null) {
            this.user.clearDomainEvents();
        }
    }

    /**
     * Get the audit metadata as an AuditMetadata value object.
     * <p>
     * Returns null if createdAt is null (new user not yet persisted).
     *
     * @return the audit metadata, or null if not available
     */
    public AuditMetadata getAuditMetadata() {
        if (this.createdAt == null) {
            return null;
        }
        return new AuditMetadata(
                this.createdAt,
                this.createdBy,
                this.lastModifiedAt,
                this.lastModifiedBy,
                this.version
        );
    }
}
