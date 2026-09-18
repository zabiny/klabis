package com.klabis.sync.infrastructure.jdbc;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
interface SyncAttemptJdbcRepository extends CrudRepository<SyncAttemptMemento, UUID> {

    List<SyncAttemptMemento> findBySyncRecordIdOrderByStartedAtDesc(UUID syncRecordId);

    /**
     * Backs the history retention job (design.md D19): prunes {@code sync_attempt}
     * rows only, never {@code sync_record}.
     */
    @Modifying
    @Query("DELETE FROM sync.sync_attempt WHERE started_at < :olderThan")
    int deleteByStartedAtBefore(@Param("olderThan") Instant olderThan);
}
