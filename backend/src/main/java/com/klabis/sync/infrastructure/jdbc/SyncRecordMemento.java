package com.klabis.sync.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.encryption.EncryptedString;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.domain.*;
import com.klabis.sync.infrastructure.SyncProjectionCodec;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.UUID;

/**
 * Memento pattern implementation for {@link SyncRecord} persistence (design.md D13).
 * <p>
 * Each {@link SyncSnapshot} flattens to a projection column (encrypted, via
 * {@link EncryptedString}) and a plaintext hash column. {@code baseline_external_*}
 * are null while the baseline halves are identical — the common case — and populated
 * only for a standing accepted divergence.
 */
@Table(schema = "sync", value = "sync_record")
class SyncRecordMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("entity_type")
    private String entityType;

    @Column("entity_id")
    private String entityId;

    @Column("external_system")
    private String externalSystem;

    @Column("external_id")
    private String externalId;

    @Column("status")
    private String status;

    @Column("baseline_local_projection")
    private EncryptedString baselineLocalProjection;

    @Column("baseline_local_hash")
    private String baselineLocalHash;

    @Column("baseline_external_projection")
    private EncryptedString baselineExternalProjection;

    @Column("baseline_external_hash")
    private String baselineExternalHash;

    @Column("local_projection")
    private EncryptedString localProjection;

    @Column("local_hash")
    private String localHash;

    @Column("external_projection")
    private EncryptedString externalProjection;

    @Column("external_hash")
    private String externalHash;

    @Column("external_version_token")
    private String externalVersionToken;

    @Column("dirty_since")
    private Instant dirtySince;

    @Column("claimed_at")
    private Instant claimedAt;

    @Column("acknowledged_local_hash")
    private String acknowledgedLocalHash;

    @Column("acknowledged_external_hash")
    private String acknowledgedExternalHash;

    @Column("acknowledged_at")
    private Instant acknowledgedAt;

    @Column("acknowledged_by")
    private String acknowledgedBy;

    @Column("next_attempt_due_at")
    private Instant nextAttemptDueAt;

    @Column("last_successful_sync_at")
    private Instant lastSuccessfulSyncAt;

    @Column("last_direction")
    private String lastDirection;

    @Column("retired_at")
    private Instant retiredAt;

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
    @Column("modified_by")
    private String modifiedBy;

    @Version
    @Column("version")
    private Long version;

    @Transient
    private boolean isNew = true;

    protected SyncRecordMemento() {
    }

    static SyncRecordMemento from(SyncRecord record) {
        Assert.notNull(record, "record must not be null");

        SyncRecordMemento memento = new SyncRecordMemento();
        memento.id = record.getId().value();
        memento.entityType = record.getTarget().entityType().name();
        memento.entityId = record.getTarget().entityId();
        memento.externalSystem = record.getExternalReference().system().name();
        memento.externalId = record.getExternalReference().externalId();
        memento.status = record.getStatus().name();

        SyncBaseline baseline = record.getBaseline();
        if (baseline != null) {
            memento.baselineLocalProjection = toEncrypted(baseline.local());
            memento.baselineLocalHash = baseline.local().hash().value();
            if (baseline.isDiverged()) {
                memento.baselineExternalProjection = toEncrypted(baseline.external());
                memento.baselineExternalHash = baseline.external().hash().value();
            }
        }

        memento.localProjection = toEncrypted(record.getLocal());
        memento.localHash = record.getLocal() != null ? record.getLocal().hash().value() : null;
        memento.externalProjection = toEncrypted(record.getExternal());
        memento.externalHash = record.getExternal() != null ? record.getExternal().hash().value() : null;

        memento.externalVersionToken = record.getExternalVersion() != null ? record.getExternalVersion().value() : null;

        memento.dirtySince = record.getDirtySince();
        memento.claimedAt = record.getClaimedAt();

        ConflictAcknowledgement acknowledgement = record.getAcknowledgement();
        if (acknowledgement != null) {
            memento.acknowledgedLocalHash = acknowledgement.acknowledgedLocalHash().value();
            memento.acknowledgedExternalHash = acknowledgement.acknowledgedExternalHash().value();
            memento.acknowledgedAt = acknowledgement.acknowledgedAt();
            memento.acknowledgedBy = acknowledgement.acknowledgedBy();
        }

        memento.nextAttemptDueAt = record.getNextAttemptDueAt();
        memento.lastSuccessfulSyncAt = record.getLastSuccessfulSyncAt();
        memento.lastDirection = record.getLastDirection() != null ? record.getLastDirection().name() : null;
        memento.retiredAt = record.getRetiredAt();

        AuditMetadata auditMetadata = record.getAuditMetadata();
        memento.isNew = auditMetadata == null;
        if (auditMetadata != null) {
            memento.createdAt = auditMetadata.createdAt();
            memento.createdBy = auditMetadata.createdBy();
            memento.modifiedAt = auditMetadata.lastModifiedAt();
            memento.modifiedBy = auditMetadata.lastModifiedBy();
            memento.version = auditMetadata.version();
        }

        return memento;
    }

    SyncRecord toSyncRecord(SyncProjectionType projectionType) {
        SyncEntityType type = SyncEntityType.valueOf(this.entityType);
        SyncTarget target = new SyncTarget(type, this.entityId);
        ExternalReference externalReference = new ExternalReference(ExternalSystem.valueOf(this.externalSystem), this.externalId);

        SyncSnapshot local = toSnapshot(localProjection, localHash, type, projectionType);
        SyncSnapshot external = toSnapshot(externalProjection, externalHash, type, projectionType);

        SyncBaseline baseline = null;
        if (baselineLocalHash != null) {
            SyncSnapshot baselineLocal = toSnapshot(baselineLocalProjection, baselineLocalHash, type, projectionType);
            // baseline_external_* is null unless a divergence was accepted (design.md
            // D6) — in the common case the baseline pair is equal halves, so we reuse
            // baselineLocal for both rather than storing (and decrypting) it twice. This
            // relies on the SyncBaseline.reconciled(snapshot) invariant that an ordinary
            // reconciliation always sets both halves to the SAME snapshot; if a future
            // slice (e.g. conflict resolution) ever reconciles the baseline with two
            // *different but hash-equal* snapshots, this reconstruction would still be
            // correct value-wise, but keep this in mind if that invariant ever changes.
            SyncSnapshot baselineExternal = baselineExternalHash != null
                    ? toSnapshot(baselineExternalProjection, baselineExternalHash, type, projectionType)
                    : baselineLocal;
            baseline = new SyncBaseline(baselineLocal, baselineExternal);
        }

        ConflictAcknowledgement acknowledgement = null;
        if (acknowledgedLocalHash != null) {
            acknowledgement = new ConflictAcknowledgement(
                    SyncHash.of(acknowledgedLocalHash),
                    SyncHash.of(acknowledgedExternalHash),
                    acknowledgedAt,
                    acknowledgedBy
            );
        }

        SyncRecord record = SyncRecord.reconstruct(
                new SyncRecordId(this.id),
                target,
                externalReference,
                SyncStatus.valueOf(this.status),
                baseline,
                local,
                external,
                externalVersionToken != null ? new ExternalVersionToken(externalVersionToken) : null,
                dirtySince,
                claimedAt,
                acknowledgement,
                nextAttemptDueAt,
                lastSuccessfulSyncAt,
                lastDirection != null ? SyncDirection.valueOf(lastDirection) : null,
                retiredAt
        );
        record.updateAuditMetadata(new AuditMetadata(createdAt, createdBy, modifiedAt, modifiedBy, version));
        return record;
    }

    private static EncryptedString toEncrypted(SyncSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return EncryptedString.of(SyncProjectionCodec.toCanonicalJson(snapshot.projection()));
    }

    private static SyncSnapshot toSnapshot(EncryptedString projectionColumn, String hashColumn, SyncEntityType entityType, SyncProjectionType projectionType) {
        if (hashColumn == null || projectionColumn == null) {
            return null;
        }
        SyncProjection projection = SyncProjectionCodec.fromCanonicalJson(projectionColumn.value(), projectionType.classFor(entityType));
        return SyncSnapshot.reconstruct(projection, SyncHash.of(hashColumn));
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
