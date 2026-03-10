package com.klabis.common.users.infrastructure.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.UserPermissions;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Memento pattern implementation for UserPermissions aggregate persistence.
 * <p>
 * This class acts as a bridge between the pure domain {@link UserPermissions} entity
 * and Spring Data JDBC persistence. It contains:
 * <ul>
 *   <li>All JDBC annotations for persistence</li>
 *   <li>JSON serialization/deserialization of authorities set</li>
 *   <li>Conversion methods to/from UserPermissions</li>
 * </ul>
 * <p>
 * The UserPermissions entity remains a pure domain object without Spring annotations,
 * while this memento handles all infrastructure concerns.
 * <p>
 * Authorities are stored as JSON array string in the database (VARCHAR(1000)).
 * This approach is pragmatic for H2 and can be migrated to PostgreSQL JSONB later.
 */
@Table("user_permissions")
public class UserPermissionsMemento implements Persistable<UUID> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("authorities")
    private String authoritiesJson;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant modifiedAt;

    @LastModifiedBy
    @Column("last_modified_by")
    private String lastModifiedBy;

    @org.springframework.data.annotation.Version
    @Column("version")
    private Long version;

    /**
     * Flag for Persistable interface - tracks if this is a new (unsaved) entity.
     */
    @Transient
    private boolean isNew = true;

    /**
     * Default constructor for Spring Data JDBC.
     */
    protected UserPermissionsMemento() {
    }

    /**
     * Creates a memento from a domain UserPermissions.
     * <p>
     * Uses auditMetadata == null as the sentinel to determine isNew:
     * a null auditMetadata means the aggregate has never been persisted (INSERT),
     * while a non-null auditMetadata means it was loaded from the database (UPDATE).
     *
     * @param permissions the domain UserPermissions
     * @return a new UserPermissionsMemento
     */
    static UserPermissionsMemento from(UserPermissions permissions) {
        UserPermissionsMemento memento = new UserPermissionsMemento();
        memento.userId = permissions.getUserId().uuid();
        memento.authoritiesJson = serializeAuthorities(permissions.getDirectAuthorities());
        memento.version = permissions.getVersion();
        AuditMetadata auditMetadata = permissions.getAuditMetadata();
        memento.isNew = auditMetadata == null;
        if (auditMetadata != null) {
            memento.createdAt = auditMetadata.createdAt();
            memento.createdBy = auditMetadata.createdBy();
            memento.modifiedAt = auditMetadata.lastModifiedAt();
            memento.lastModifiedBy = auditMetadata.lastModifiedBy();
        }
        return memento;
    }

    /**
     * Converts this memento to a domain UserPermissions.
     *
     * @return a UserPermissions domain object
     */
    UserPermissions toUserPermissions() {
        Set<Authority> authorities = deserializeAuthorities(authoritiesJson);
        UserPermissions permissions = UserPermissions.create(new UserId(userId), authorities);
        permissions.setVersion(this.version);
        permissions.updateAuditMetadata(getAuditMetadata());
        return permissions;
    }

    /**
     * Serializes a set of authorities to JSON array string.
     * <p>
     * Format: ["MEMBERS:READ", "TRAINING:VIEW"]
     *
     * @param authorities the set of authorities
     * @return JSON array string
     */
    private static String serializeAuthorities(Set<Authority> authorities) {
        try {
            return objectMapper.writeValueAsString(authorities);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize authorities to JSON", e);
        }
    }

    /**
     * Deserializes a JSON array string to a set of authorities.
     * <p>
     * Handles null/empty strings by returning empty set.
     *
     * @param json the JSON array string
     * @return set of Authority enums
     */
    private static Set<Authority> deserializeAuthorities(String json) {
        if (json == null || json.isBlank()) {
            return Set.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Set<Authority>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize authorities from JSON: " + json, e);
        }
    }

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    // Getters for audit fields

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getModifiedAt() {
        return modifiedAt;
    }

    /**
     * Get the audit metadata as an AuditMetadata value object.
     * <p>
     * Returns null if createdAt is null (new permissions not yet persisted).
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
                this.modifiedAt,
                this.lastModifiedBy,
                this.version
        );
    }
}
