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
 * Implements enrolment, claiming, {@link #recordSuccess}/{@link #recordConverged} and
 * direction resolution for: nothing changed, only external changed (inward), first
 * enrolment (adopt external), only local changed with an outward write available, and
 * both sides converging on the same value. Conflict handling is added by Slice 3 on
 * top of this same shape.
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
     * what, if anything, needs to happen (design.md D4's decision table).
     * <p>
     * Covered so far: no baseline yet → adopt external (D5); neither side moved →
     * nothing to do; only the external side moved → write inward; only the local side
     * moved with an outward write available → write outward; both sides moved to the
     * same value → converged. A local-only change with no outward write capability is
     * a conflict (D6), added in Slice 3.
     */
    public SyncDecision decide(SyncSnapshot currentLocal, SyncSnapshot currentExternal, SyncCapabilities capabilities) {
        Assert.notNull(currentLocal, "currentLocal is required");
        Assert.notNull(currentExternal, "currentExternal is required");
        Assert.notNull(capabilities, "capabilities is required");

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
        if (localChanged && !externalChanged) {
            if (capabilities.writesExternal()) {
                return SyncDecision.write(SyncDirection.OUTWARD);
            }
            // A local change that cannot be sent onward is a conflict (design.md D6),
            // added in Slice 3.
            throw new UnsupportedOperationException(
                    "Conflict handling for a local change with no outward write capability is added in a later slice");
        }

        // From here on, both sides changed since the baseline (design.md D4's last two
        // rows). Equal current hashes is convergence; anything else is a genuine
        // conflict, including the accepted-divergence guard (D6) that turns a further
        // external change into a conflict even though only the external side moved
        // this time — Slice 3 adds that guard as an earlier branch in this method.
        if (currentLocal.matches(currentExternal)) {
            return SyncDecision.converged();
        }

        throw new UnsupportedOperationException(
                "Conflict handling for sides that changed to different values is added in a later slice");
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

    /**
     * Records an outward write that reached the external system but whose baseline
     * advance had to be skipped, because the local side moved again before the guard
     * re-read (design.md D9). The external side really does hold {@code pushedSnapshot}
     * now, and the write is itself an external change the engine caused — mirroring
     * D9's "an inward write is itself a local change" for the outward direction — so
     * both the external snapshot and the *whole baseline pair* rebase onto it, exactly
     * as an ordinary reconciliation would.
     * <p>
     * Deliberately not {@code SyncBaseline.accepted(local, external)} with unequal
     * halves: that shape is reserved for a standing accepted divergence (design.md D6)
     * and Slice 3's inward guard treats it as such. Using it here for a purely
     * temporary bookkeeping value would make Slice 3 mistake this record for one with
     * a manager-accepted divergence and block a perfectly ordinary inward write. The
     * baseline pair here stays a normal, reconciled (equal-halves) pair.
     * <p>
     * The local side is intentionally left as it was: it is still due — the very edit
     * that raced the guard re-read has not been pushed — and {@code dirtySince}
     * already marks the record for re-evaluation. The next pass then correctly sees
     * only the local side as changed and pushes the newer value outward.
     */
    public void recordOutwardWriteWithSkippedAdvance(SyncSnapshot pushedSnapshot) {
        Assert.notNull(pushedSnapshot, "pushedSnapshot is required");
        this.external = pushedSnapshot;
        this.baseline = SyncBaseline.reconciled(pushedSnapshot);
        this.dirtySince = Instant.now();
    }

    /**
     * Records a converged pass (design.md D4): both sides changed independently but
     * now hold the same value. Nothing is written; both baseline halves rebase onto
     * the shared state, same as an ordinary reconciliation, but with no direction to
     * report — a convergence is not a write.
     */
    public void recordConverged(SyncSnapshot agreedSnapshot) {
        Assert.notNull(agreedSnapshot, "agreedSnapshot is required");

        this.local = agreedSnapshot;
        this.external = agreedSnapshot;
        this.baseline = SyncBaseline.reconciled(agreedSnapshot);
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
