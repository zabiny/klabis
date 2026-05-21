package com.klabis.calendar.infrastructure.restapi;

import com.klabis.E2ETest;
import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GET /ical/my-schedule.ics.
 * <p>
 * Uses full Spring Boot context to test the complete flow: token auth filter → controller → iCal renderer.
 * The iCal token is generated via IcalTokenPort and used as the ?token= parameter.
 */
@E2ETest
@DisplayName("IcalFeedController Integration Tests")
class IcalFeedControllerTest {

    private static final UUID USER_UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UserId USER_ID = new UserId(USER_UUID);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IcalTokenPort icalTokenPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String validRawToken;

    @BeforeEach
    void setUpUserAndToken() {
        jdbcTemplate.execute("DELETE FROM calendar_feed_token WHERE user_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'");
        jdbcTemplate.execute("DELETE FROM members WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'");
        jdbcTemplate.execute("DELETE FROM users WHERE id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'");
        jdbcTemplate.execute("INSERT INTO users (id, user_name, password_hash, account_status, created_at, modified_at, version) " +
                "VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ZBM8100', 'hash', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
        jdbcTemplate.execute("INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) " +
                "VALUES ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'ZBM8100', 'Test', 'Member', '1990-01-01', 'CZ', 'MALE', 'test@test.cz', '+420111111111', 'Street 1', 'City', '10000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)");
        validRawToken = icalTokenPort.generateOrRotate(USER_ID).rawToken();
    }

    @Nested
    @DisplayName("Authentication")
    class AuthenticationTests {

        @Test
        @DisplayName("valid token → 200 with Content-Type: text/calendar")
        void validToken_returns200WithCalendarContentType() throws Exception {
            mockMvc.perform(get("/ical/my-schedule.ics").param("token", validRawToken))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                    .andExpect(content().string(containsString("BEGIN:VCALENDAR")))
                    .andExpect(content().string(containsString("END:VCALENDAR")));
        }

        @Test
        @DisplayName("invalid token → 401 Unauthorized")
        void invalidToken_returns401() throws Exception {
            mockMvc.perform(get("/ical/my-schedule.ics").param("token", "thisisaninvalidtoken1234567890"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("missing token → 401 Unauthorized (falls through to OAuth2, rejects unauthenticated)")
        void missingToken_returns401() throws Exception {
            mockMvc.perform(get("/ical/my-schedule.ics"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token param on /api/** → does not bypass OAuth2 (401 without Bearer)")
        void tokenOnApiPath_doesNotBypassOAuth2() throws Exception {
            mockMvc.perform(get("/api/calendar-items").param("token", validRawToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Response")
    class ResponseTests {

        @Test
        @DisplayName("Cache-Control header → max-age=600, public, no-transform")
        void response_includesCacheControlHeader() throws Exception {
            mockMvc.perform(get("/ical/my-schedule.ics").param("token", validRawToken))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Cache-Control", containsString("max-age=600")))
                    .andExpect(header().string("Cache-Control", containsString("public")))
                    .andExpect(header().string("Cache-Control", containsString("no-transform")));
        }

        @Test
        @DisplayName("user with empty schedule → 200 with empty calendar (no VEVENTs)")
        void emptySchedule_returns200WithEmptyCalendar() throws Exception {
            String body = mockMvc.perform(get("/ical/my-schedule.ics").param("token", validRawToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("BEGIN:VCALENDAR");
            assertThat(body).contains("END:VCALENDAR");
            assertThat(body).doesNotContain("BEGIN:VEVENT");
        }
    }

    @Nested
    @DisplayName("Feed content with events")
    class WithEventsTests {

        @BeforeEach
        void insertEvents() {
            jdbcTemplate.execute("DELETE FROM event_registrations WHERE member_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'");
            jdbcTemplate.execute("DELETE FROM events WHERE id IN ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'dddddddd-dddd-dddd-dddd-dddddddddddd')");
            // Coordinator event (2 months from now, within the 12-month future window)
            jdbcTemplate.execute("INSERT INTO events (id, name, event_date, location, organizer, status, event_coordinator_id, created_at, created_by, modified_at, modified_by, version) " +
                    "VALUES ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'Koordinovaná akce', DATEADD(MONTH, 2, CURRENT_DATE), 'Praha', 'ZBM', 'ACTIVE', 'cccccccc-cccc-cccc-cccc-cccccccccccc', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 0)");
            // Cancelled event with registration (3 months from now, within the 12-month future window)
            jdbcTemplate.execute("INSERT INTO events (id, name, event_date, location, organizer, status, created_at, created_by, modified_at, modified_by, version) " +
                    "VALUES ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'Zrušená akce', DATEADD(MONTH, 3, CURRENT_DATE), 'Brno', 'ZBM', 'CANCELLED', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP, 'system', 0)");
            jdbcTemplate.execute("INSERT INTO event_registrations (id, event_id, member_id, si_card_number) " +
                    "VALUES ('11111111-1111-1111-1111-111111111111', 'dddddddd-dddd-dddd-dddd-dddddddddddd', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '0000000')");
        }

        @AfterEach
        void deleteEvents() {
            jdbcTemplate.execute("DELETE FROM event_registrations WHERE id = '11111111-1111-1111-1111-111111111111'");
            jdbcTemplate.execute("DELETE FROM events WHERE id IN ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'dddddddd-dddd-dddd-dddd-dddddddddddd')");
        }

        @Test
        @DisplayName("coordinator event → VEVENT with Role: koordinátor in DESCRIPTION")
        void coordinatorEvent_appearsWithCoordinatorRoleInDescription() throws Exception {
            String body = mockMvc.perform(get("/ical/my-schedule.ics").param("token", validRawToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("BEGIN:VEVENT");
            assertThat(body).contains("Koordinovaná akce");
            assertThat(body).contains("Role: koordinátor");
        }

        @Test
        @DisplayName("cancelled event with registration → STATUS:CANCELLED in VEVENT")
        void cancelledEvent_hasStatusCancelledInVEvent() throws Exception {
            String body = mockMvc.perform(get("/ical/my-schedule.ics").param("token", validRawToken))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("Zrušená akce");
            assertThat(body).contains("STATUS:CANCELLED");
        }
    }
}
