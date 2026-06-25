package com.klabis.events.infrastructure.jdbc;

import com.klabis.events.EventId;
import com.klabis.events.application.EventScheduleQuery;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SecondaryAdapter
class EventScheduleQueryImpl implements EventScheduleQuery {

    private final NamedParameterJdbcTemplate namedJdbc;

    EventScheduleQueryImpl(NamedParameterJdbcTemplate namedJdbc) {
        this.namedJdbc = namedJdbc;
    }

    @Override
    public Set<EventId> findEventIdsForMemberSchedule(MemberId memberId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT e.id FROM events.events e
                WHERE e.event_date >= :from
                  AND e.event_date <= :to
                  AND (
                      EXISTS (
                          SELECT 1 FROM events.event_coordinators c
                          WHERE c.event_id = e.id AND c.member_id = :memberId
                      )
                      OR EXISTS (
                          SELECT 1 FROM events.event_registrations r
                          WHERE r.event_id = e.id AND r.member_id = :memberId
                      )
                  )
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("from", from)
                .addValue("to", to)
                .addValue("memberId", memberId.uuid());

        return namedJdbc.query(sql, params, (rs, rowNum) -> rs.getObject(1, UUID.class))
                .stream()
                .map(EventId::new)
                .collect(Collectors.toSet());
    }
}
