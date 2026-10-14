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
import com.klabis.events.domain.EventRegisterCommandBuilder;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.RegistrationNote;
import com.klabis.members.MemberId;
import com.klabis.sync.application.SynchronizationPort;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncRecord;
import com.klabis.sync.domain.SyncResolution;
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
    @DisplayName("a member registered in Klabis and entered in ORIS ends with one registration showing the ORIS category")
    void klabisRegistrationIsAdopted() {
        Event event = eventRepository.findById(eventId).orElseThrow();
        event.registerMember(memberId, EventRegisterCommandBuilder.builder()
                .siCardNumber("654321")
                .categoryId(categoryIdOf(event, "M21"))
                .internalNote(RegistrationNote.ofNullable("internal"))
                .wantsSharedTransport(true)
                .build());
        eventRepository.save(event);
        Instant klabisTime = eventRepository.findById(eventId).orElseThrow()
                .findRegistration(memberId).orElseThrow().registeredAt();
        stubEntries(entry(clubUserId).classId(102).build());

        discoveryJob.discover();

        Event reloaded = eventRepository.findById(eventId).orElseThrow();
        assertThat(reloaded.getRegistrations()).hasSize(1);
        EventRegistration registration = reloaded.findRegistration(memberId).orElseThrow();
        assertThat(reloaded.findCategory(registration.categoryId()).orElseThrow().name()).isEqualTo("W21");
        assertThat(registration.siCardNumber().value()).isEqualTo("123456");
        assertThat(RegistrationNote.textOrNull(registration.orisNote())).isEqualTo("ORIS note");
        assertThat(registration.registeredAt()).isEqualTo(klabisTime);
        assertThat(RegistrationNote.textOrNull(registration.internalNote())).isEqualTo("internal");
        assertThat(registration.wantsSharedTransport()).isTrue();
        assertThat(registrationRecord().getStatus()).isEqualTo(SyncStatus.IN_SYNC);
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

    private EventRegistration currentRegistration() {
        return eventRepository.findById(eventId).orElseThrow().findRegistration(memberId).orElseThrow();
    }

    private void editInKlabis(java.util.function.UnaryOperator<Event.EditRegistrationCommand> change) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        EventRegistration registration = event.findRegistration(memberId).orElseThrow();
        Event.EditRegistrationCommand current = new Event.EditRegistrationCommand(registration.siCardNumber(),
                registration.categoryId(), registration.wantsSharedTransport(), registration.wantsSharedAccommodation(),
                registration.internalNote(), registration.orisNote(), registration.wantsToRentSi());
        eventRegistrationPort.editRegistration(eventId, memberId, change.apply(current));
    }

    private static com.klabis.events.EventCategoryId categoryIdOf(Event event, String name) {
        return event.getCategories().stream().filter(c -> c.name().equals(name)).findFirst().orElseThrow().id();
    }

    private SyncRecord scheduledPass(SyncRecord record) {
        Object service = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(synchronizationPort);
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "runScheduledPass", record.getId());
    }

    private SyncRecord broughtIn() {
        stubEntries(entry(clubUserId).build());
        discoveryJob.discover();
        return registrationRecord();
    }

    @Test
    @DisplayName("an entry deleted in ORIS leaves the registration in place, marked missing in ORIS, and the record in step")
    void orisDeletionMarksRegistration() {
        SyncRecord record = broughtIn();

        stubEntries();
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(eventRegistrationPort.listRegistrations(eventId)).singleElement()
                .satisfies(registration -> assertThat(registration.missingInOris()).isTrue());
        assertThat(synchronizeAgain(record).getLocal().hash()).isEqualTo(after.getLocal().hash());
    }

    private SyncRecord synchronizeAgain(SyncRecord record) {
        return synchronizationPort.synchronizeNow(record.getId(), "test-user");
    }

    @Test
    @DisplayName("an entry re-created in ORIS clears the mark and the registration takes the ORIS values")
    void orisRecreationClearsMark() {
        SyncRecord record = broughtIn();
        stubEntries();
        synchronizationPort.synchronizeNow(record.getId(), "test-user");

        stubEntries(entry(clubUserId).classId(101).si(654321).build());
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        EventRegistration registration = currentRegistration();
        assertThat(registration.missingInOris()).isFalse();
        assertThat(registration.siCardNumber().value()).isEqualTo("654321");
    }

    @Test
    @DisplayName("a category edited in Klabis raises a conflict and the Klabis value is kept")
    void klabisCategoryEditConflicts() {
        SyncRecord record = broughtIn();
        Event event = eventRepository.findById(eventId).orElseThrow();

        editInKlabis(c -> new Event.EditRegistrationCommand(c.siCardNumber(), categoryIdOf(event, "M21"),
                c.wantsSharedTransport(), c.wantsSharedAccommodation(), c.internalNote(), c.orisNote(), c.wantsToRentSi()));
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.CONFLICT);
        assertThat(currentRegistration().categoryId()).isEqualTo(categoryIdOf(event, "M21"));
    }

    @Test
    @DisplayName("an ORIS note edited in Klabis raises a conflict")
    void klabisOrisNoteEditConflicts() {
        SyncRecord record = broughtIn();

        editInKlabis(c -> new Event.EditRegistrationCommand(c.siCardNumber(), c.categoryId(),
                c.wantsSharedTransport(), c.wantsSharedAccommodation(), c.internalNote(),
                RegistrationNote.ofNullable("changed in Klabis"), c.wantsToRentSi()));
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.CONFLICT);
    }

    @Test
    @DisplayName("the internal note and shared-service choices never cause a conflict")
    void internalNoteAndSharedChoicesCauseNothing() {
        SyncRecord record = broughtIn();

        editInKlabis(c -> new Event.EditRegistrationCommand(c.siCardNumber(), c.categoryId(), true, true,
                RegistrationNote.ofNullable("internal"), c.orisNote(), c.wantsToRentSi()));
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(RegistrationNote.textOrNull(currentRegistration().internalNote())).isEqualTo("internal");
    }

    @Test
    @DisplayName("unregistering in Klabis while the ORIS entry exists raises a conflict, and the conflict resolves itself once ORIS deletes the entry")
    void unregistrationConflictSelfResolves() {
        SyncRecord record = broughtIn();

        eventRegistrationPort.unregisterMember(eventId, memberId);
        SyncRecord conflicted = synchronizationPort.synchronizeNow(record.getId(), "test-user");
        assertThat(conflicted.getStatus()).isEqualTo(SyncStatus.CONFLICT);

        stubEntries();
        SyncRecord after = scheduledPass(record);

        assertThat(after.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(eventRegistrationPort.listRegistrations(eventId)).isEmpty();
    }

    @Test
    @DisplayName("INWARD on an unregistration conflict re-creates the registration from ORIS")
    void inwardRecreatesUnregisteredRegistration() {
        SyncRecord record = broughtIn();
        eventRegistrationPort.unregisterMember(eventId, memberId);
        assertThat(synchronizationPort.synchronizeNow(record.getId(), "test-user").getStatus())
                .isEqualTo(SyncStatus.CONFLICT);

        synchronizationPort.acknowledgeConflict(record.getId(), "test-user");
        SyncRecord resolved = synchronizationPort.resolveConflict(record.getId(), SyncResolution.INWARD, "test-user");

        assertThat(resolved.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        EventRegistration registration = currentRegistration();
        assertThat(registration.siCardNumber().value()).isEqualTo("123456");
        assertThat(RegistrationNote.textOrNull(registration.orisNote())).isEqualTo("ORIS note");
        assertThat(registration.internalNote()).isNull();
        assertThat(registration.wantsSharedTransport()).isFalse();
    }

    @Test
    @DisplayName("OUTWARD is not offered for a registration conflict")
    void outwardNotOffered() {
        SyncRecord record = broughtIn();
        eventRegistrationPort.unregisterMember(eventId, memberId);
        SyncRecord conflicted = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(synchronizationPort.supportedResolutions(conflicted))
                .doesNotContain(SyncResolution.OUTWARD)
                .contains(SyncResolution.INWARD, SyncResolution.ACCEPT_DIVERGENCE);
    }

    @Test
    @DisplayName("a difference kept with ACCEPT_DIVERGENCE raises a new conflict when ORIS changes again")
    void acceptedDivergenceAsksAgainOnOrisChange() {
        SyncRecord record = broughtIn();
        Event event = eventRepository.findById(eventId).orElseThrow();
        editInKlabis(c -> new Event.EditRegistrationCommand(c.siCardNumber(), categoryIdOf(event, "M21"),
                c.wantsSharedTransport(), c.wantsSharedAccommodation(), c.internalNote(), c.orisNote(), c.wantsToRentSi()));
        assertThat(synchronizationPort.synchronizeNow(record.getId(), "test-user").getStatus())
                .isEqualTo(SyncStatus.CONFLICT);

        synchronizationPort.acknowledgeConflict(record.getId(), "test-user");
        SyncRecord accepted = synchronizationPort.resolveConflict(record.getId(), SyncResolution.ACCEPT_DIVERGENCE, "test-user");
        assertThat(accepted.getStatus()).isEqualTo(SyncStatus.IN_SYNC);

        stubEntries(entry(clubUserId).si(654321).build());
        SyncRecord after = synchronizationPort.synchronizeNow(record.getId(), "test-user");

        assertThat(after.getStatus()).isEqualTo(SyncStatus.CONFLICT);
    }
}
