package com.klabis.members.infrastructure.jdbc;

import com.klabis.members.BirthNumberAccessedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that audit log entries are correctly persisted
 * when {@link BirthNumberAuditLogMemento} is saved via the JDBC repository.
 */
@DisplayName("BirthNumberAuditService – JDBC Integration Tests")
@DataJdbcTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
     statements = "DELETE FROM birth_number_audit_log")
@ActiveProfiles("test")
class BirthNumberAuditServiceTest {

    @Autowired
    private BirthNumberAuditLogJdbcRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should persist audit entry for VIEW_BIRTH_NUMBER event")
    void shouldPersistViewAuditEntry() {
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.now();

        BirthNumberAuditLogMemento entry = BirthNumberAuditLogMemento.of(
                userId, memberId,
                BirthNumberAccessedEvent.BirthNumberAction.VIEW_BIRTH_NUMBER,
                now
        );

        auditLogRepository.save(entry);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM birth_number_audit_log WHERE member_id = ?", memberId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("user_id")).isEqualTo(userId);
        assertThat(rows.get(0).get("action")).isEqualTo("VIEW_BIRTH_NUMBER");
    }

    @Test
    @DisplayName("should persist audit entry for MODIFY_BIRTH_NUMBER event")
    void shouldPersistModifyAuditEntry() {
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        BirthNumberAuditLogMemento entry = BirthNumberAuditLogMemento.of(
                userId, memberId,
                BirthNumberAccessedEvent.BirthNumberAction.MODIFY_BIRTH_NUMBER,
                Instant.now()
        );

        auditLogRepository.save(entry);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM birth_number_audit_log WHERE member_id = ?", memberId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("action")).isEqualTo("MODIFY_BIRTH_NUMBER");
    }

    @Test
    @DisplayName("should store correct userId and memberId in audit entry")
    void shouldStoreCorrectUserAndMemberIds() {
        UUID userId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        Instant now = Instant.now();

        BirthNumberAuditLogMemento entry = BirthNumberAuditLogMemento.of(
                userId, memberId,
                BirthNumberAccessedEvent.BirthNumberAction.VIEW_BIRTH_NUMBER,
                now
        );

        auditLogRepository.save(entry);

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM birth_number_audit_log WHERE member_id = ?", memberId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("user_id")).isEqualTo(userId);
        assertThat(rows.get(0).get("member_id")).isEqualTo(memberId);
        assertThat(rows.get(0).get("occurred_at")).isNotNull();
    }
}
