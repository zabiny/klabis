package com.klabis.events.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisApiClient.OrisResponse;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.EventEntry;
import com.dpolach.api.orisclient.dto.EventEntryBuilder;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.events.EventCategory;
import com.klabis.events.EventId;
import com.klabis.events.application.EventRegistrationPort;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.EventRegistration;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.RegistrationNote;
import com.klabis.members.MemberId;
import com.klabis.sync.application.SynchronizationPort;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncStatus;
import com.klabis.sync.domain.SyncTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * ORIS entries of club members on the real synchronisation engine and database; only {@code OrisApiClient} is stubbed.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@MockitoBean(types = {OrisApiClient.class, OrisWebUrls.class})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Event registrations on the sync engine - ORIS entries scenarios")
class OrisEventRegistrationSyncScenarioIntegrationTest {

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(660_000);
    private static final int MY_CLUB_ID = 205;

    @Autowired
    private OrisEventRegistrationDiscoveryJob discoveryJob;
    @Autowired
    private SynchronizationPort synchronizationPort;
    @Autowired
    private EventRegistrationPort eventRegistrationPort;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private OrisApiClient orisApiClient;
    @Autowired
    private OrisEventEntries orisEventEntries;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private int orisEventId;
    private int clubUserId;
    private EventId eventId;
    private MemberId memberId;

    @BeforeEach
    void setUp() {
        orisEventId = ORIS_ID_SEQUENCE.incrementAndGet();
        clubUserId = ORIS_ID_SEQUENCE.incrementAndGet();
        orisEventEntries.evict(orisEventId);
        circuitBreakerRegistry.circuitBreaker("sync-adapter").reset();

        UUID memberUuid = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO members.members (id, registration_number, first_name, last_name, date_of_birth, nationality,
                    gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by,
                    modified_at, modified_by, version)
                VALUES (?, ?, 'Jan', 'Novak', '2000-01-01', 'CZ', 'MALE', ?, '+420111111111', 'Street 1', 'City 1',
                    '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)
                """, memberUuid, "ZBM%04d".formatted(clubUserId % 10000), "m" + clubUserId + "@example.com");
        memberId = new MemberId(memberUuid);
        synchronizationPort.enroll(new SyncTarget(SyncEntityType.MEMBER, memberUuid.toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(clubUserId)));

        Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .name("ORIS race " + orisEventId)
                .eventDate(LocalDate.now().plusDays(30))
                .organizer("OOB")
                .categories(List.of(EventCategory.createFromOris("101", "M21"), EventCategory.createFromOris("102", "W21")))
                .build());
        event.publish();
        eventId = eventRepository.save(event).getId();
        synchronizationPort.enroll(new SyncTarget(SyncEntityType.EVENT, eventId.value().toString()),
                new ExternalReference(ExternalSystem.ORIS, String.valueOf(orisEventId)));
    }

    private static EventEntryBuilder entry(int clubUserId) {
        return EventEntryBuilder.builder().id(clubUserId).clubUserId(clubUserId).classId(102).si(123456).rentSi(true)
                .note("ORIS note").createdDateTime(LocalDateTime.of(2026, 3, 1, 12, 0, 0));
    }

    private void stubEntries(EventEntry... entries) {
        Map<String, EventEntry> byId = new LinkedHashMap<>();
        for (EventEntry entry : entries) {
            byId.put(String.valueOf(entry.id()), entry);
        }
        doReturn(new OrisResponse<>(byId, "JSON", "OK", null, "getEventEntries"))
                .when(orisApiClient).getEventEntries(eq(orisEventId), eq(MY_CLUB_ID));
        orisEventEntries.evict(orisEventId);
    }

    private SyncRecord registrationRecord() {
        return synchronizationPort.findByTarget(new SyncTarget(SyncEntityType.EVENT_REGISTRATION,
                eventId.value() + ":" + memberId.value())).orElseThrow();
    }

    @Test
    @DisplayName("discovery brings a club member's entry into the registrations list with category, SI, rental and ORIS note")
    void discoveryBringsEntryIn() {
        stubEntries(entry(clubUserId).build());

        discoveryJob.discover();

        List<EventRegistration> registrations = eventRegistrationPort.listRegistrations(eventId);
        assertThat(registrations).singleElement().satisfies(registration -> {
            assertThat(registration.memberId()).isEqualTo(memberId);
            Event event = eventRepository.findById(eventId).orElseThrow();
            assertThat(event.findCategory(registration.categoryId()).orElseThrow().name()).isEqualTo("W21");
            assertThat(registration.siCardNumber().value()).isEqualTo("123456");
            assertThat(registration.wantsToRentSi()).isTrue();
            assertThat(RegistrationNote.textOrNull(registration.orisNote())).isEqualTo("ORIS note");
            assertThat(registration.registeredAt())
                    .isEqualTo(LocalDateTime.of(2026, 3, 1, 12, 0, 0).atZone(ZoneId.of("Europe/Prague")).toInstant());
        });
        assertThat(registrationRecord().getStatus()).isEqualTo(SyncStatus.IN_SYNC);
    }

    @Test
    @DisplayName("a second pass changes nothing for SI 0, a null RentSI and an over-long note")
    void secondPassIsQuietForUnrepresentableValues() {
        stubEntries(entry(clubUserId).si(0).rentSi(null).note("n".repeat(400)).build());
        discoveryJob.discover();
        SyncRecord first = registrationRecord();
        assertThat(first.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

        SyncRecord second = synchronizationPort.synchronizeNow(first.getId(), "test-user");

        assertThat(second.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(second.getLastSuccessfulSyncAt()).isAfterOrEqualTo(first.getLastSuccessfulSyncAt());
        assertThat(second.getLocal().hash()).isEqualTo(second.getExternal().hash());
        assertThat(second.getBaseline().local().hash()).isEqualTo(first.getBaseline().local().hash());
        EventRegistration registration = eventRegistrationPort.listRegistrations(eventId).get(0);
        assertThat(registration.siCardNumber()).isNull();
        assertThat(registration.wantsToRentSi()).isFalse();
        assertThat(registration.orisNote().text()).hasSize(255);
    }

    @Test
    @DisplayName("an entry of somebody unknown to Klabis is ignored")
    void unknownMemberIsIgnored() {
        stubEntries(entry(clubUserId + 1).build());

        discoveryJob.discover();

        assertThat(eventRegistrationPort.listRegistrations(eventId)).isEmpty();
    }

    @Test
    @DisplayName("an entry in a category the event does not have yet is not brought in and is retried by the next run")
    void unknownCategoryIsRetriedByNextRun() {
        stubEntries(entry(clubUserId).classId(999).build());
        discoveryJob.discover();
        assertThat(eventRegistrationPort.listRegistrations(eventId)).isEmpty();

        Event event = eventRepository.findById(eventId).orElseThrow();
        event.syncFromOris(com.klabis.events.domain.EventSyncFromOrisBuilder.builder()
                .name(event.getName()).eventDate(event.getEventDate()).organizer(event.getOrganizer())
                .registrationDeadlines(event.getRegistrationDeadlines())
                .categories(List.of(EventCategory.createFromOris("101", "M21"),
                        EventCategory.createFromOris("102", "W21"), EventCategory.createFromOris("999", "H55")))
                .build());
        eventRepository.save(event);
        orisEventEntries.evict(orisEventId);
        circuitBreakerRegistry.circuitBreaker("sync-adapter").reset();

        discoveryJob.discover();

        assertThat(eventRegistrationPort.listRegistrations(eventId)).hasSize(1);
    }

    @Test
    @DisplayName("an ORIS change of a brought-in entry reaches the registration on the next pass")
    void orisChangeIsPulled() {
        stubEntries(entry(clubUserId).build());
        discoveryJob.discover();
        SyncRecord record = registrationRecord();

        stubEntries(entry(clubUserId).classId(101).si(654321).build());
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        Event event = eventRepository.findById(eventId).orElseThrow();
        EventRegistration registration = event.findRegistration(memberId).orElseThrow();
        assertThat(event.findCategory(registration.categoryId()).orElseThrow().name()).isEqualTo("M21");
        assertThat(registration.siCardNumber().value()).isEqualTo("654321");
        assertThat(registration.registeredAt()).isBefore(Instant.now());
    }
}
