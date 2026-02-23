package com.klabis.common.users.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.users.PasswordSetupToken;
import com.klabis.common.users.TokenHash;
import com.klabis.common.users.UserId;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Memento pattern implementation for PasswordSetupToken aggregate persistence.
 * <p>
 * This class acts as a bridge between the pure domain {@link PasswordSetupToken} entity
 * and Spring Data JDBC persistence. It contains:
 * <ul>
 *   <li>All JDBC annotations for persistence</li>
 *   <li>Flat primitive fields matching the database schema</li>
 *   <li>Conversion methods to/from PasswordSetupToken</li>
 * </ul>
 * <p>
 * The PasswordSetupToken entity remains a pure domain object without Spring annotations,
 * while this memento handles all infrastructure concerns.
 * <p>
 * Note: Unlike User and Member, PasswordSetupToken does not publish domain events
 * or require audit metadata tracking.
 */
@Table("password_setup_tokens")
public class PasswordSetupTokenMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("token_hash")
    private String tokenHash;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("used_at")
    private Instant usedAt;

    @Column("used_by_ip")
    private String usedByIp;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedBy
    @Column("modified_by")
    private String modifiedBy;

    @Version
    @Column("version")
    private Long version;

    // Transient flag for Persistable<UUID>
    @Transient
    private boolean isNew = true;

    /**
     * Default constructor required by Spring Data JDBC.
     */
    protected PasswordSetupTokenMemento() {
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
     * Creates a PasswordSetupTokenMemento from a PasswordSetupToken entity (for save operations).
     *
     * @param token the PasswordSetupToken entity to convert
     * @return a new PasswordSetupTokenMemento with all fields copied from the token
     */
    public static PasswordSetupTokenMemento from(PasswordSetupToken token) {
        PasswordSetupTokenMemento memento = new PasswordSetupTokenMemento();

        // Copy ID
        memento.id = token.getId();

        // Copy user ID
        memento.userId = token.getUserId().uuid();

        // Copy token hash
        memento.tokenHash = token.getTokenHash().getValue();

        // Copy timestamps
        memento.createdAt = token.getCreatedAt();
        memento.expiresAt = token.getExpiresAt();
        memento.usedAt = token.getUsedAt();
        memento.usedByIp = token.getUsedByIp();

        memento.version = token.getVersion();

        // Use Persistable.isNew() to determine INSERT vs UPDATE
        memento.isNew = token.isNew();

        return memento;
    }

    /**
     * Converts this memento to a PasswordSetupToken entity (for load operations).
     * <p>
     * Uses the PasswordSetupToken.reconstruct method to create a PasswordSetupToken instance.
     *
     * @return a PasswordSetupToken entity reconstructed from this memento
     */
    public PasswordSetupToken toPasswordSetupToken() {
        UserId userId = this.userId != null ? new UserId(this.userId) : null;
        TokenHash tokenHash = this.tokenHash != null ? TokenHash.fromHashedValue(this.tokenHash) : null;

        return PasswordSetupToken.reconstruct(
                this.id,
                userId,
                tokenHash,
                this.createdAt,
                this.expiresAt,
                this.usedAt,
                this.usedByIp,
                getAuditMetadata()
        );
    }

    /**
     * Creates AuditMetadata from memento fields (for domain entity reconstruction).
     *
     * @return AuditMetadata instance, or null if createdAt is null
     */
    public AuditMetadata getAuditMetadata() {
        if (this.createdAt == null) return null;
        return new AuditMetadata(
                this.createdAt,
                this.createdBy,
                this.createdAt,  // For password tokens, use createdAt since no separate modifiedAt exists
                this.modifiedBy,
                this.version
        );
    }

    /**
     * Set the isNew flag for this memento.
     * <p>
     * Package-protected method used by the repository to override the isNew flag
     * based on database existence check. This ensures correct INSERT vs UPDATE behavior.
     *
     * @param isNew true if this should be treated as a new entity (INSERT), false for UPDATE
     */
    void setNewFlag(boolean isNew) {
        this.isNew = isNew;
    }
}
