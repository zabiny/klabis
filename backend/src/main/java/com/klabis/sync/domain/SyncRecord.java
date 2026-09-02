package com.klabis.sync.domain;

import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.sync.SyncConflictDetected;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.SyncTerminallyFailed;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * No baseline yet → adopt external (D5). While a standing accepted divergence
     * holds (the baseline pair itself is diverged, D6), any external movement is a
     * conflict, not an inward write — the guard checked before the ordinary table so
     * an accepted local value is never silently overwritten. Otherwise: neither side
     * moved → nothing to do; only the external side moved → write inward; only the
     * local side moved with an outward write available → write outward, or a conflict
     * without that capability (D6); both sides moved to the same value → converged;
     * both sides moved to different values → conflict.
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

        // The inward guard (design.md D4, D6): while an accepted divergence stands,
        // any external movement stops and asks again rather than silently overwriting
        // the accepted local value — even when, as here, only the external side moved
        // relative to its own baseline half.
        if (baseline.isDiverged() && externalChanged) {
            return SyncDecision.conflict();
        }

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
            // A local change that cannot be sent onward is a conflict, not a silent
            // overwrite (design.md D6). OUTWARD is what was attempted and blocked.
            return SyncDecision.conflict(SyncDirection.OUTWARD);
        }

        // Both sides changed since the baseline (design.md D4's last two rows). Equal
        // current hashes is convergence; anything else is a genuine conflict.
        if (currentLocal.matches(currentExternal)) {
            return SyncDecision.converged();
        }
        return SyncDecision.conflict();
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

    /**
     * Records a standing conflict (design.md D4, D6): the current local and external
     * snapshots are kept (not the baseline, which stays as the last agreed state) so a
     * divergence report can name the fields that differ by comparing the three
     * snapshots later. Nothing is written in either direction. A prior acknowledgement
     * is cleared — it was bound to a different collision (design.md D7).
     * <p>
     * Publishes {@link SyncConflictDetected} only when this is a genuinely new
     * collision — the hash pair differs from what the record already held. A repeated
     * call with the same pair (e.g. a resolution rejected twice against the same
     * unmoved collision, or a due-scan re-evaluating a record already in conflict)
     * must not re-announce work that is already stuck and already known about.
     */
    public void recordConflict(SyncSnapshot currentLocal, SyncSnapshot currentExternal, SyncDirection attemptedDirection) {
        Assert.notNull(currentLocal, "currentLocal is required");
        Assert.notNull(currentExternal, "currentExternal is required");

        boolean sameCollisionAsBefore = status == SyncStatus.CONFLICT
                && local != null && external != null
                && currentLocal.matches(local) && currentExternal.matches(external);

        this.local = currentLocal;
        this.external = currentExternal;
        this.status = SyncStatus.CONFLICT;
        this.acknowledgement = null;
        this.dirtySince = null;
        this.nextAttemptDueAt = null;

        if (!sameCollisionAsBefore) {
            registerEvent(SyncConflictDetected.of(id, attemptedDirection, currentLocal.hash(), currentExternal.hash()));
        }
    }

    /**
     * A manager confirms they have seen the current collision (design.md D7). Bound to
     * the hash pair current at that moment so a later resolution can detect whether
     * the collision moved on before the manager acted.
     */
    public void acknowledgeConflict(ConflictAcknowledgement acknowledgement) {
        Assert.notNull(acknowledgement, "acknowledgement is required");
        Assert.state(status == SyncStatus.CONFLICT, "Only a conflicted record can be acknowledged");
        this.acknowledgement = acknowledgement;
    }

    /**
     * Whether an acknowledgement stands and is still current for this record's
     * present hash pair — the guard a resolution must pass before acting
     * (design.md D7).
     */
    public boolean isAcknowledgementCurrent() {
        return acknowledgement != null && local != null && external != null
                && acknowledgement.isCurrentFor(local.hash(), external.hash());
    }

    /**
     * Resolves a conflict by forcing a direction: the freshly read chosen side is
     * written (by the caller, through the adapter) and the baseline is reset from it,
     * exactly like an ordinary successful pass (design.md D7 — a forced `INWARD`
     * resolution follows the same post-write re-read rule as any other inward write).
     * The acknowledgement is cleared and the conflict lifts.
     */
    public void resolveWithDirection(SyncDirection direction, SyncSnapshot local, SyncSnapshot external) {
        recordSuccess(direction, local, external);
        this.acknowledgement = null;
    }

    /**
     * Resolves a conflict by accepting that the two sides deliberately differ
     * (design.md D6): the baseline pair is set to the freshly read snapshots — which
     * may themselves differ — nothing is written in either direction, and the
     * conflict lifts. The inward guard in {@link #decide} then protects this accepted
     * value: any later external movement raises a new conflict instead of silently
     * overwriting it.
     */
    public void acceptDivergence(SyncSnapshot local, SyncSnapshot external) {
        Assert.notNull(local, "local is required");
        Assert.notNull(external, "external is required");

        this.local = local;
        this.external = external;
        this.baseline = SyncBaseline.accepted(local, external);
        this.status = SyncStatus.IN_SYNC;
        this.acknowledgement = null;
        this.dirtySince = null;
        this.nextAttemptDueAt = null;
    }

    /**
     * Records an outage: the external system was unavailable for this attempt
     * (design.md D11). The record is rescheduled at the initial retry delay — not the
     * grown one — since an outage failure counts toward neither the derived failure
     * count nor the backoff delay; which records were attempted during an outage stops
     * mattering. If the record was {@code IN_SYNC} or {@code NEW}, it moves to
     * {@code RETRYING} so a manager sees it as failing rather than settled — the
     * outage itself does not terminate the record, only makes it visibly stuck for
     * now.
     *
     * @throws IllegalStateException if the record is not currently being attempted
     *                                ({@code CONFLICT}, {@code FAILED} or {@code RETIRED}
     *                                records are never passed to this method — the
     *                                caller must have filtered them out before
     *                                attempting the record at all)
     */
    public void recordOutage(Instant nextAttemptDueAt) {
        Assert.notNull(nextAttemptDueAt, "nextAttemptDueAt is required");
        assertBeingAttempted();
        if (status == SyncStatus.NEW || status == SyncStatus.IN_SYNC) {
            this.status = SyncStatus.RETRYING;
        }
        this.nextAttemptDueAt = nextAttemptDueAt;
    }

    /**
     * Records a retryable failure (design.md D10): a transport or server-side error
     * that may pass on its own. Moves the record to {@code RETRYING} — entered from
     * {@code NEW} and {@code IN_SYNC} alike — and schedules the next attempt at the
     * caller-computed backoff delay (design.md D19: initial delay, multiplier,
     * ceiling, derived from the failure count in the attempt history).
     *
     * @throws IllegalStateException if the record is not currently being attempted
     *                                (see {@link #recordOutage})
     */
    public void recordRetryableFailure(Instant nextAttemptDueAt) {
        Assert.notNull(nextAttemptDueAt, "nextAttemptDueAt is required");
        assertBeingAttempted();
        this.status = SyncStatus.RETRYING;
        this.nextAttemptDueAt = nextAttemptDueAt;
    }

    /**
     * Records terminal failure (design.md D10): retryable attempts since the last
     * success or reset reached the configured limit, or a single failure was
     * classified terminal on the spot. The record stops being attempted — no further
     * {@code nextAttemptDueAt} is set, since the scheduler skips a {@code FAILED}
     * record outright — and waits for a manual {@link #reset}. Publishes
     * {@link SyncTerminallyFailed}.
     *
     * @throws IllegalStateException if the record is not currently being attempted
     *                                (see {@link #recordOutage})
     */
    public void recordTerminalFailure(int failedAttempts, String failureReason) {
        assertBeingAttempted();
        this.status = SyncStatus.FAILED;
        this.nextAttemptDueAt = null;
        registerEvent(SyncTerminallyFailed.of(id, failedAttempts, failureReason));
    }

    /**
     * A record must not already be {@code CONFLICT}, {@code FAILED} or
     * {@code RETIRED} when a pass records a failure against it — those states need a
     * manager's decision (resolve, reset) or are permanently out of scope, and a pass
     * must never have been started against them in the first place. This is a
     * programming-error guard, not a normal-flow check: the caller (design.md, the
     * application layer orchestrating a pass) is responsible for never attempting such
     * a record.
     */
    private void assertBeingAttempted() {
        Assert.state(status != SyncStatus.CONFLICT && status != SyncStatus.FAILED && status != SyncStatus.RETIRED,
                () -> "Cannot record a failure against a record in status " + status + " — it must not have been attempted");
    }

    /**
     * A manager restarts a terminally failed record (design.md D10): it returns to
     * service, synchronised again from the next run onwards. There is no counter
     * column to zero — the caller appends a {@code RESET} attempt row so the derived
     * failure count restarts (design.md D10) — this method only clears the record's
     * own stuck state.
     */
    public void reset() {
        Assert.state(status == SyncStatus.FAILED, "Only a terminally failed record can be reset");
        this.status = SyncStatus.IN_SYNC;
        this.nextAttemptDueAt = null;
    }

    /**
     * Per-field attribution of the current divergence, computed by comparing the
     * baseline, local and current external snapshots' decrypted projections in memory
     * — never from stored per-field hashes (design.md D13). Only meaningful while the
     * record is in conflict; empty otherwise.
     */
    public Map<String, ChangedSide> changedSides(SyncProjectionFieldReader fieldReader) {
        Assert.notNull(fieldReader, "fieldReader is required");
        if (local == null || external == null || baseline == null) {
            return Map.of();
        }

        Map<String, Object> baselineFields = fieldReader.fields(baseline.local().projection());
        Map<String, Object> localFields = fieldReader.fields(local.projection());
        Map<String, Object> externalFields = fieldReader.fields(external.projection());

        Set<String> allFieldNames = new LinkedHashSet<>();
        allFieldNames.addAll(baselineFields.keySet());
        allFieldNames.addAll(localFields.keySet());
        allFieldNames.addAll(externalFields.keySet());

        Map<String, ChangedSide> result = new LinkedHashMap<>();
        for (String fieldName : allFieldNames) {
            Object baselineValue = baselineFields.get(fieldName);
            Object localValue = localFields.get(fieldName);
            Object externalValue = externalFields.get(fieldName);

            boolean localMoved = !java.util.Objects.equals(baselineValue, localValue);
            boolean externalMoved = !java.util.Objects.equals(baselineValue, externalValue);

            if (localMoved && externalMoved) {
                result.put(fieldName, ChangedSide.BOTH);
            } else if (localMoved) {
                result.put(fieldName, ChangedSide.LOCAL);
            } else if (externalMoved) {
                result.put(fieldName, ChangedSide.EXTERNAL);
            }
        }
        return result;
    }

    /**
     * The names of the fields that diverged — the keys of {@link #changedSides}, in
     * the same order.
     */
    public List<String> divergedFields(SyncProjectionFieldReader fieldReader) {
        return List.copyOf(changedSides(fieldReader).keySet());
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
