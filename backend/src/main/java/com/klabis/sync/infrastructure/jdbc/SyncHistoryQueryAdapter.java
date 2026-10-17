package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncAttemptId;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.AttemptFilter;
import com.klabis.sync.application.AttemptView;
import com.klabis.sync.application.RecordView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
class SyncHistoryQueryAdapter implements SyncHistoryQuery {

    private static final String UNCHANGED_RULE = "NOT (a.outcome = 'SUCCESS' AND a.direction IS NULL AND a.trigger <> 'MANUAL')";

    private static final String SELECT = """
            SELECT a.id, a.sync_record_id, a.started_at, a.trigger, a.direction, a.outcome,
                   a.local_hash, a.external_hash, a.failure_reason, a.acting_user,
                   r.entity_type, r.entity_id, r.external_id
            """;

    private static final String FROM = """
            FROM sync.sync_attempt a
            JOIN sync.sync_record r ON r.id = a.sync_record_id
            """;

    private final NamedParameterJdbcTemplate jdbc;
    private final SyncAttemptRepository attemptRepository;

    SyncHistoryQueryAdapter(NamedParameterJdbcTemplate jdbc, SyncAttemptRepository attemptRepository) {
        this.jdbc = jdbc;
        this.attemptRepository = attemptRepository;
    }

    @Override
    public Page<AttemptView> listAttempts(AttemptFilter filter, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        if (!filter.includeUnchanged()) {
            conditions.add(UNCHANGED_RULE);
        }
        if (filter.entityType() != null) {
            conditions.add("r.entity_type = :entityType");
            params.addValue("entityType", filter.entityType().name());
        }
        if (filter.outcome() != null) {
            conditions.add("a.outcome = :outcome");
            params.addValue("outcome", filter.outcome().name());
        }
        if (filter.from() != null) {
            conditions.add("a.started_at >= :from");
            params.addValue("from", Timestamp.from(filter.from()));
        }
        if (filter.to() != null) {
            conditions.add("a.started_at < :to");
            params.addValue("to", Timestamp.from(filter.to()));
        }
        return page(conditions, params, pageable);
    }

    @Override
    public Page<AttemptView> listAttemptsOf(SyncRecordId recordId, boolean includeUnchanged, Pageable pageable) {
        MapSqlParameterSource params = new MapSqlParameterSource("recordId", recordId.value());
        List<String> conditions = new ArrayList<>();
        conditions.add("a.sync_record_id = :recordId");
        if (!includeUnchanged) {
            conditions.add(UNCHANGED_RULE);
        }
        return page(conditions, params, pageable);
    }

    private Page<AttemptView> page(List<String> conditions, MapSqlParameterSource params, Pageable pageable) {
        String where = conditions.isEmpty() ? "" : "WHERE " + String.join(" AND ", conditions) + "\n";

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FROM + where, params, Long.class);

        params.addValue("limit", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<AttemptView> content = jdbc.query(
                SELECT + FROM + where + "ORDER BY a.started_at DESC, a.id DESC LIMIT :limit OFFSET :offset",
                params, ATTEMPT_VIEW);

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    @Override
    public List<RecordView> listRecordsIn(Set<SyncStatus> statuses) {
        if (statuses.isEmpty()) {
            return List.of();
        }
        MapSqlParameterSource params = new MapSqlParameterSource(
                "statuses", statuses.stream().map(SyncStatus::name).toList());
        List<RecordRow> rows = jdbc.query(RECORD_SELECT, params, SyncHistoryQueryAdapter::toRecordRow);
        return rows.stream().map(this::toRecordView).toList();
    }

    private static final String RECORD_SELECT = """
            SELECT r.id, r.entity_type, r.entity_id, r.external_id, r.status, r.last_successful_sync_at
            FROM sync.sync_record r
            WHERE r.status IN (:statuses)
            ORDER BY r.entity_type, r.entity_id
            """;

    private record RecordRow(SyncRecordId id, RecordView view) {
    }

    private static RecordRow toRecordRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastSuccess = rs.getTimestamp("last_successful_sync_at");
        return new RecordRow(
                new SyncRecordId(rs.getObject("id", UUID.class)),
                new RecordView(
                        new SyncTarget(SyncEntityType.valueOf(rs.getString("entity_type")), rs.getString("entity_id")),
                        rs.getString("external_id"),
                        SyncStatus.valueOf(rs.getString("status")),
                        lastSuccess != null ? lastSuccess.toInstant() : null,
                        0));
    }

    private RecordView toRecordView(RecordRow row) {
        int failed = SyncAttempts.failedSinceLastSuccess(attemptRepository.findByRecordIdOrderByStartedAtDesc(row.id()));
        RecordView view = row.view();
        return new RecordView(view.target(), view.externalId(), view.status(), view.lastSuccessfulSyncAt(), failed);
    }

    private static final RowMapper<AttemptView> ATTEMPT_VIEW = (rs, rowNum) -> {
        SyncTarget target = new SyncTarget(
                SyncEntityType.valueOf(rs.getString("entity_type")), rs.getString("entity_id"));
        return new AttemptView(SyncAttemptMemento.fromRow(rs).toSyncAttempt(), target, rs.getString("external_id"));
    };
}
