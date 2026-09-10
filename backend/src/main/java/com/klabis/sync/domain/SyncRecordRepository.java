package com.klabis.sync.domain;

import com.klabis.sync.SyncRecordId;
import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@SecondaryPort
public interface SyncRecordRepository {

    SyncRecord save(SyncRecord record);

    Optional<SyncRecord> findById(SyncRecordId id);

    /**
     * A record is unique per (target, external system) — a Klabis entity could in
     * principle be enrolled against more than one external system, so the system is
     * required alongside the target.
     */
    Optional<SyncRecord> findByTargetAndSystem(SyncTarget target, ExternalSystem system);

    /**
     * Every record still eligible to be attempted by the scheduler — the nightly full
     * pass re-compares each one (design.md D10, D17): the only way an external change
     * that announces itself nowhere gets noticed. Excludes {@code RETIRED} (D17) and
     * {@code FAILED} (D10 — terminally failed records are "skipped by the scheduler"
     * until a manager resets them). {@code CONFLICT} stays included: D7 has a standing
     * conflict recomputed on every pass.
     */
    List<SyncRecord> findAllActive();

    /**
     * Every non-retired record, {@code FAILED} included — backs the manual
     * {@code all-upcoming} bulk pass (design.md, "Existing operations, unchanged in
     * shape": "still honouring the... {@code CONFLICT}/{@code FAILED} skip (those
     * records are counted and reported, not attempted)"). Unlike {@link #findAllActive},
     * which the scheduler needs {@code FAILED}-free to keep {@code runScheduledPass}'s
     * own assertion from tripping, the manual pass must see a terminally failed record
     * so it can report it rather than silently omitting it.
     */
    List<SyncRecord> findAllNonRetired();

    /**
     * Records due for the frequent due scan (design.md D10): dirty (a local change
     * was observed) or whose {@code nextAttemptDueAt} has passed. One indexed query;
     * costs nothing when no record is due.
     * <p>
     * A record with no baseline yet, {@code CONFLICT} or {@code FAILED} is never
     * picked up here without any special-casing needed: {@link SyncRecord#recordConflict}
     * and {@link SyncRecord#recordTerminalFailure} both clear {@code dirtySince} and
     * {@code nextAttemptDueAt}, so such a record simply never matches "dirty or due"
     * (design.md D7 — a standing conflict is still recomputed by the nightly full
     * pass, just not by this frequent scan). {@code RETIRED} and freshly claimed
     * records are excluded explicitly.
     */
    List<SyncRecord> findDueForScan(Instant now, java.time.Duration claimLease);
}
