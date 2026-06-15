package com.klabis.membershipfees.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;
import java.util.UUID;

abstract class AbstractMembershipFeeMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    protected UUID id;

    @CreatedDate
    @Column("created_at")
    protected Instant createdAt;

    @CreatedBy
    @Column("created_by")
    protected String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    protected Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    protected String lastModifiedBy;

    @Version
    @Column("version")
    protected Long version;

    @Transient
    protected boolean isNew = true;

    protected AbstractMembershipFeeMemento() {
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    protected void applyAudit(AuditMetadata auditMetadata) {
        if (auditMetadata != null) {
            this.createdAt = auditMetadata.createdAt();
            this.createdBy = auditMetadata.createdBy();
            this.lastModifiedAt = auditMetadata.lastModifiedAt();
            this.lastModifiedBy = auditMetadata.lastModifiedBy();
            this.version = auditMetadata.version();
        }
    }

    protected AuditMetadata toAuditMetadata() {
        return createdAt != null
                ? new AuditMetadata(createdAt, createdBy, lastModifiedAt, lastModifiedBy, version)
                : null;
    }
}
