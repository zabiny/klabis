package com.klabis.events.infrastructure;

import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.AuthorizationSnapshotLoader;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Event coordinator grants in the authorization snapshot")
class EventCoordinatorGrantsIntegrationTest {

    @Autowired
    private AuthorizationSnapshotLoader snapshotLoader;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserId givenMember(String registrationNumber) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO members.members (id, registration_number, first_name, last_name, date_of_birth, nationality,
                    gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by,
                    modified_at, modified_by, version)
                VALUES (?, ?, 'Test', 'Member', '2000-01-01', 'CZ', 'MALE', ?, '+420111111111', 'Street 1', 'City 1',
                    '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)
                """, id, registrationNumber, registrationNumber.toLowerCase() + "@example.com");
        return new UserId(id);
    }

    private Event givenEventCoordinatedBy(UserId... coordinators) {
        LinkedHashSet<MemberId> members = new LinkedHashSet<>();
        for (UserId coordinator : coordinators) {
            members.add(MemberId.fromUserId(coordinator));
        }
        return eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                .name("Event " + UUID.randomUUID())
                .eventDate(LocalDate.of(2026, 9, 1))
                .organizer("OOB")
                .coordinators(members)
                .build()));
    }

    @Test
    @DisplayName("a coordinator holds editing and registration reading over their event and over no other")
    void shouldGrantCoordinatorBothAuthoritiesOverOwnEventOnly() {
        UserId coordinator = givenMember("ZBM8001");
        UserId other = givenMember("ZBM8002");
        Event coordinated = givenEventCoordinatedBy(coordinator);
        Event foreign = givenEventCoordinatedBy(other);

        AuthorizationSnapshot snapshot = snapshotLoader.loadFor(coordinator);

        TargetRef own = TargetRef.event(coordinated.getId().value());
        TargetRef notOwn = TargetRef.event(foreign.getId().value());
        assertThat(snapshot.has(Authority.EVENTS_EDIT, own)).isTrue();
        assertThat(snapshot.has(Authority.EVENTS_REGISTRATIONS_READ, own)).isTrue();
        assertThat(snapshot.has(Authority.EVENTS_EDIT, notOwn)).isFalse();
        assertThat(snapshot.has(Authority.EVENTS_REGISTRATIONS_READ, notOwn)).isFalse();
        assertThat(snapshot.overAll()).doesNotContain(Authority.EVENTS_EDIT, Authority.EVENTS_REGISTRATIONS_READ);
    }

    @Test
    @DisplayName("a member who coordinates nothing holds no coordinator grant")
    void shouldGrantNothingToNonCoordinator() {
        UserId member = givenMember("ZBM8003");
        givenEventCoordinatedBy(givenMember("ZBM8004"));

        AuthorizationSnapshot snapshot = snapshotLoader.loadFor(member);

        assertThat(snapshot.targetsOf(Authority.EVENTS_EDIT)).isEmpty();
        assertThat(snapshot.targetsOf(Authority.EVENTS_REGISTRATIONS_READ)).isEmpty();
    }
}
