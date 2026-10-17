package com.klabis.sync.infrastructure.jdbc;

import com.klabis.sync.SyncAttemptId;
import com.klabis.sync.SyncRecordId;
import com.klabis.sync.application.AttemptFilter;
import com.klabis.sync.application.AttemptView;
import com.klabis.sync.application.SyncHistoryQuery;
import com.klabis.sync.domain.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;
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

    SyncHistoryQueryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Page<AttemptView> listAttempts(AttemptFilter filter, Pageable pageable) {
        String where = filter.includeUnchanged() ? "" : "WHERE " + UNCHANGED_RULE + "\n";

        Long total = jdbc.queryForObject("SELECT COUNT(*) " + FROM + where, new MapSqlParameterSource(), Long.class);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        List<AttemptView> content = jdbc.query(
                SELECT + FROM + where + "ORDER BY a.started_at DESC, a.id DESC LIMIT :limit OFFSET :offset",
                params, ATTEMPT_VIEW);

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private static final RowMapper<AttemptView> ATTEMPT_VIEW = (rs, rowNum) -> {
        String direction = rs.getString("direction");
        String localHash = rs.getString("local_hash");
        String externalHash = rs.getString("external_hash");
        Timestamp startedAt = rs.getTimestamp("started_at");

        SyncAttempt attempt = SyncAttempt.reconstruct(
                new SyncAttemptId(rs.getObject("id", UUID.class)),
                new SyncRecordId(rs.getObject("sync_record_id", UUID.class)),
                startedAt.toInstant(),
                SyncTriggerKind.valueOf(rs.getString("trigger")),
                direction != null ? SyncDirection.valueOf(direction) : null,
                SyncOutcome.valueOf(rs.getString("outcome")),
                localHash != null ? SyncHash.of(localHash) : null,
                externalHash != null ? SyncHash.of(externalHash) : null,
                rs.getString("failure_reason"),
                rs.getString("acting_user"));
        SyncTarget target = new SyncTarget(
                SyncEntityType.valueOf(rs.getString("entity_type")), rs.getString("entity_id"));
        return new AttemptView(attempt, target, rs.getString("external_id"));
    };
}
