package com.klabis.sync.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.sync.SyncRecordId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;

/**
 * The synchronisation state of one Klabis entity against one external system
 * (design.md, Target Domain Model). Owns direction resolution, conflict state, retry
 * scheduling and last-success information.
 * <p>
 * Slice 1 implements enrolment, claiming, {@link #recordSuccess} and direction
 * resolution for: nothing changed, only external changed (inward), and first
 * enrolment (adopt external). Outward writes, convergence and conflict handling are
 * added by later slices on top of this same shape.
 */
@AggregateRoot
public class SyncRecord extends KlabisAggregateRoot<SyncRecord, SyncRecordId> {

    @Identity
    private final SyncRecordId id;
    private final SyncTarget target;
    private final ExternalReference externalReference;
    private SyncStatus status;
    private SyncBaseline baseline;
    private SyncSnapshot local;
    private SyncSnapshot external;
    private ExternalVersionToken externalVersion;
    private Instant dirtySince;
    private Instant claimedAt;
    private ConflictAcknowledgement acknowledgement;
    private Instant nextAttemptDueAt;
    private Instant lastSuccessfulSyncAt;
    private SyncDirection lastDirection;
    private Instant retiredAt;

    private SyncRecord(SyncRecordId id, SyncTarget target, ExternalReference externalReference, SyncStatus status) {
        Assert.notNull(id, "id is required");
        Assert.notNull(target, "target is required");
        Assert.notNull(externalReference, "externalReference is required");
        this.id = id;
        this.target = target;
        this.externalReference = externalReference;
        this.status = status;
    }

    /**
     * Enrols an entity for synchronisation against an external system (design.md D17).
     * The record starts with no baseline; the first pass adopts the external side
     * (D5).
     */
    public static SyncRecord enroll(SyncRecordId id, SyncTarget target, ExternalReference externalReference) {
        return new SyncRecord(id, target, externalReference, SyncStatus.NEW);
    }

    /**
     * Reconstructs a record from persisted state. Used only by the persistence layer.
     */
    public static SyncRecord reconstruct(
            SyncRecordId id,
            SyncTarget target,
            ExternalReference externalReference,
            SyncStatus status,
            SyncBaseline baseline,
            SyncSnapshot local,
            SyncSnapshot external,
            ExternalVersionToken externalVersion,
            Instant dirtySince,
            Instant claimedAt,
            ConflictAcknowledgement acknowledgement,
            Instant nextAttemptDueAt,
            Instant lastSuccessfulSyncAt,
            SyncDirection lastDirection,
            Instant retiredAt
    ) {
        SyncRecord record = new SyncRecord(id, target, externalReference, status);
        record.baseline = baseline;
        record.local = local;
        record.external = external;
        record.externalVersion = externalVersion;
        record.dirtySince = dirtySince;
        record.claimedAt = claimedAt;
        record.acknowledgement = acknowledgement;
        record.nextAttemptDueAt = nextAttemptDueAt;
        record.lastSuccessfulSyncAt = lastSuccessfulSyncAt;
        record.lastDirection = lastDirection;
        record.retiredAt = retiredAt;
        return record;
    }

    /**
     * Marks the record due because a local change was observed (design.md D9).
     * Purely a scheduling signal — never consulted to decide whether a write is safe.
     */
    public void markDirty() {
        this.dirtySince = Instant.now();
    }

    /**
     * Claims the record for the duration of one pass, preventing a concurrent pass
     * from acting on it at the same time (design.md D12). Callers are expected to have
     * already checked {@link #isClaimAvailable(Instant, java.time.Duration)}.
     */
    public void claim(Instant now) {
        this.claimedAt = now;
    }

    public void releaseClaim() {
        this.claimedAt = null;
    }

    public boolean isClaimAvailable(Instant now, java.time.Duration lease) {
        return claimedAt == null || claimedAt.plus(lease).isBefore(now);
    }

    /**
     * Compares the current local/external snapshots against the baseline and decides
     * what, if anything, needs to happen (design.md D4's decision table). Slice 1
     * covers: no baseline yet → adopt external (D5); neither side moved → nothing to
     * do; only the external side moved → write inward.
     */
    public SyncDecision decide(SyncSnapshot currentLocal, SyncSnapshot currentExternal) {
        Assert.notNull(currentLocal, "currentLocal is required");
        Assert.notNull(currentExternal, "currentExternal is required");

        if (baseline == null) {
            return SyncDecision.adoptExternal();
        }

        boolean localChanged = !currentLocal.matches(baseline.local());
        boolean externalChanged = !currentExternal.matches(baseline.external());

        if (!localChanged && !externalChanged) {
            return SyncDecision.nothingToDo();
        }
        if (!localChanged && externalChanged) {
            return SyncDecision.write(SyncDirection.INWARD);
        }

        // Outward writes, convergence and conflict resolution for the remaining rows
        // (local-only changed, both changed) are added in later slices.
        throw new UnsupportedOperationException(
                "Direction resolution for this combination of changes is not yet implemented in this slice");
    }

    /**
     * Records a successful pass: the post-write state becomes both the current
     * snapshot for the written side and the new baseline (design.md D9).
     */
    public void recordSuccess(SyncDirection direction, SyncSnapshot local, SyncSnapshot external) {
        Assert.notNull(direction, "direction is required");
        Assert.notNull(local, "local is required");
        Assert.notNull(external, "external is required");

        this.local = local;
        this.external = external;
        this.baseline = SyncBaseline.reconciled(direction == SyncDirection.INWARD ? local : external);
        this.lastDirection = direction;
        this.lastSuccessfulSyncAt = Instant.now();
        this.status = SyncStatus.IN_SYNC;
        this.dirtySince = null;
        this.nextAttemptDueAt = null;
    }

    public SyncRecordId getId() {
        return id;
    }

    public SyncTarget getTarget() {
        return target;
    }

    public ExternalReference getExternalReference() {
        return externalReference;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public SyncBaseline getBaseline() {
        return baseline;
    }

    public SyncSnapshot getLocal() {
        return local;
    }

    public SyncSnapshot getExternal() {
        return external;
    }

    public ExternalVersionToken getExternalVersion() {
        return externalVersion;
    }

    public void setExternalVersion(ExternalVersionToken externalVersion) {
        this.externalVersion = externalVersion;
    }

    public Instant getDirtySince() {
        return dirtySince;
    }

    public Instant getClaimedAt() {
        return claimedAt;
    }

    public ConflictAcknowledgement getAcknowledgement() {
        return acknowledgement;
    }

    public Instant getNextAttemptDueAt() {
        return nextAttemptDueAt;
    }

    public Instant getLastSuccessfulSyncAt() {
        return lastSuccessfulSyncAt;
    }

    public SyncDirection getLastDirection() {
        return lastDirection;
    }

    public Instant getRetiredAt() {
        return retiredAt;
    }

    /**
     * Retires the record: no longer scanned, but kept with its history and
     * last-synchronisation information intact (design.md D17).
     */
    public void retire() {
        this.status = SyncStatus.RETIRED;
        this.retiredAt = Instant.now();
    }
}
