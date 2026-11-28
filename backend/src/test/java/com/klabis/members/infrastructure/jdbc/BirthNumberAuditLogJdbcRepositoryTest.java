package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.BirthNumberAccessedEvent.BirthNumberAction;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BirthNumberAuditLogJdbcRepository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class})
)
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class BirthNumberAuditLogJdbcRepositoryTest {

    @Autowired
    private BirthNumberAuditLogJdbcRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should persist who did what to which member and when")
    void shouldPersistAuditEntry() {
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Instant occurredAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        auditLogRepository.save(BirthNumberAuditLogMemento.of(
                userId, memberId, BirthNumberAction.MODIFY_BIRTH_NUMBER, occurredAt));

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT * FROM members.birth_number_audit_log WHERE member_id = ?", memberId);
        assertThat(row.get("user_id")).isEqualTo(userId);
        assertThat(row.get("action")).isEqualTo("MODIFY_BIRTH_NUMBER");
        assertThat(((Timestamp) row.get("occurred_at")).toInstant()).isEqualTo(occurredAt);
    }
}
