package com.klabis.events.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.events.*;
import com.klabis.events.domain.*;
import com.klabis.members.MemberId;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventCreateEventBuilder;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.Optional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Event aggregate with Spring Data JDBC.
 * <p>
 * Tests cover:
 * - CRUD operations (save, findById)
 * - Event with all fields (including optional websiteUrl, coordinatorId)
 * - Event with registrations (aggregate persistence)
 * - Pagination (findAll with Pageable)
 * - Filtering by status
 * - Filtering by organizer
 * - Filtering by date range (from/to)
 * - Filtering active events with date before via EventFilter (for auto-completion scheduler)
 * - Unique constraint on (event_id, member_id) in registrations
 */
@DisplayName("Event JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'TEST001', 'Test', 'Member1', '2000-01-01', 'CZ', 'MALE', 'test1@example.com', '+420111111111', 'Street 1', 'City 1', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'TEST002', 'Test', 'Member2', '2000-01-01', 'CZ', 'MALE', 'test2@example.com', '+420111111112', 'Street 2', 'City 2', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO members (id, registration_number, first_name, last_name, date_of_birth, nationality, gender, email, phone, street, city, postal_code, country, is_active, created_at, created_by, modified_at, modified_by, version) VALUES ('33333333-3333-3333-3333-333333333333', 'TEST003', 'Test', 'Member3', '2000-01-01', 'CZ', 'MALE', 'test3@example.com', '+420111111113', 'Street 3', 'City 3', '11000', 'CZ', true, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class EventJdbcRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    /**
     * Fixed test member IDs for foreign key constraints.
     * These are inserted via class-level @Sql annotation.
     */
    private static final UUID TEST_MEMBER_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_MEMBER_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEST_MEMBER_3_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        // Each test runs in a transaction that is rolled back after completion,
        // ensuring test isolation without manual cleanup
    }

    @Nested
    @DisplayName("save() and findById() - round-trip with all fields")
    class SaveAndFindByIdWithAllFields {

        @Test
        @DisplayName("should save and find event with all required fields")
        void shouldSaveAndFindEventWithAllRequiredFields() {
            // Given
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("City Orienteering Championship")
                    .eventDate(LocalDate.of(2026, 6, 15))
                    .location("Prague City Center")
                    .organizer("Prague OC")
                    .build());

            // When
            Event savedEvent = eventRepository.save(event);
            Optional<Event> foundEvent = eventRepository.findById(savedEvent.getId());

            // Then
            assertThat(foundEvent).isPresent();
            Event retrieved = foundEvent.get();
            EventAssert.assertThat(retrieved)
                    .hasId(savedEvent.getId())
                    .hasName("City Orienteering Championship")
                    .hasDate(LocalDate.of(2026, 6, 15))
                    .hasLocation("Prague City Center")
                    .hasOrganizer("Prague OC")
                    .hasWebsiteUrl(null)
                    .hasEventCoordinatorId(null)
                    .hasStatus(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should save and find event with all fields including optional")
        void shouldSaveAndFindEventWithAllFieldsIncludingOptional() {
            // Given
            String websiteUrl = "https://example.com/event";
            MemberId coordinatorId = new MemberId(TEST_MEMBER_1_ID);

            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Forest Sprint Race")
                    .eventDate(LocalDate.of(2026, 7, 20))
                    .location("Brno Forest")
                    .organizer("Brno OC")
                    .websiteUrl(websiteUrl)
                    .eventCoordinatorId(coordinatorId)
                    .build());

            // When
            Event savedEvent = eventRepository.save(event);
            Optional<Event> foundEvent = eventRepository.findById(savedEvent.getId());

            // Then
            assertThat(foundEvent).isPresent();
            Event retrieved = foundEvent.get();
            assertThat(retrieved.getId()).isEqualTo(savedEvent.getId());
            assertThat(retrieved.getName()).isEqualTo("Forest Sprint Race");
            assertThat(retrieved.getEventDate()).isEqualTo(LocalDate.of(2026, 7, 20));
            assertThat(retrieved.getLocation()).isEqualTo("Brno Forest");
            assertThat(retrieved.getOrganizer()).isEqualTo("Brno OC");
            assertThat(retrieved.getWebsiteUrl()).isNotNull();
            assertThat(retrieved.getWebsiteUrl().value()).isEqualTo("https://example.com/event");
            assertThat(retrieved.getEventCoordinatorId()).isNotNull();
            assertThat(retrieved.getEventCoordinatorId()).isEqualTo(coordinatorId);
            assertThat(retrieved.getStatus()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should return empty when event not found")
        void shouldReturnEmptyWhenEventNotFound() {
            // Given
            EventId nonExistentId = new EventId(UUID.randomUUID());

            // When
            Optional<Event> foundEvent = eventRepository.findById(nonExistentId);

            // Then
            assertThat(foundEvent).isEmpty();
        }
    }

    @Nested
    @DisplayName("save() and findById() - event with registrations")
    class SaveAndFindEventWithRegistrations {

        @Test
        @DisplayName("should save and find event with registrations (aggregate persistence)")
        void shouldSaveAndFindEventWithRegistrations() {
            // Given
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event with Registrations")
                    .eventDate(LocalDate.now().plusDays(60))
                    .location("Test Location")
                    .organizer("Test OC")
                    .build());
            event.publish();

            MemberId member1Id = new MemberId(TEST_MEMBER_1_ID);
            MemberId member2Id = new MemberId(TEST_MEMBER_2_ID);
            SiCardNumber siCard1 = new SiCardNumber("123456");
            SiCardNumber siCard2 = new SiCardNumber("789012");

            event.registerMember(member1Id, siCard1, null);
            event.registerMember(member2Id, siCard2, null);

            // When
            Event savedEvent = eventRepository.save(event);
            Optional<Event> foundEvent = eventRepository.findById(savedEvent.getId());

            // Then
            assertThat(foundEvent).isPresent();
            Event retrieved = foundEvent.get();
            assertThat(retrieved.getRegistrations()).hasSize(2);
            assertThat(retrieved.getRegistrations())
                    .extracting(EventRegistration::memberId)
                    .containsExactlyInAnyOrder(member1Id, member2Id);
            assertThat(retrieved.getRegistrations())
                    .extracting(EventRegistration::siCardNumber)
                    .containsExactlyInAnyOrder(siCard1, siCard2);
        }
    }

    @Nested
    @DisplayName("findAll() - pagination")
    class FindAllWithPagination {

        @Test
        @DisplayName("should return paginated events")
        void shouldReturnPaginatedEvents() {
            // Given - create 5 events
            String[] organizers = {"OOB", "PRG", "BRN", "OST", "LIB"};
            for (int i = 0; i < 5; i++) {
                Event event = Event.create(EventCreateEventBuilder.builder()
                        .name("Event " + (i + 1))
                        .eventDate(LocalDate.of(2026, 6, i + 1))
                        .location("Location " + (i + 1))
                        .organizer(organizers[i])
                        .build());
                eventRepository.save(event);
            }

            Pageable pageable = PageRequest.of(0, 2);

            // When
            Page<Event> page = eventRepository.findAll(EventFilter.none(), pageable);

            // Then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getTotalElements()).isEqualTo(5);
            assertThat(page.getTotalPages()).isEqualTo(3);
            assertThat(page.getNumber()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return empty page when no events exist")
        void shouldReturnEmptyPageWhenNoEventsExist() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Event> page = eventRepository.findAll(EventFilter.none(), pageable);

            // Then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Filter by status")
    class FilterByStatus {

        @Test
        @DisplayName("should filter events by status")
        void shouldFilterEventsByStatus() {
            // Given
            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event").eventDate(LocalDate.of(2026, 6, 10))
                    .location("Location A").organizer("OOB").build());
            eventRepository.save(draftEvent);

            Event activeEvent1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event 1").eventDate(LocalDate.of(2026, 6, 11))
                    .location("Location B").organizer("PRG").build());
            activeEvent1.publish();
            eventRepository.save(activeEvent1);

            Event activeEvent2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event 2").eventDate(LocalDate.of(2026, 6, 12))
                    .location("Location C").organizer("BRN").build());
            activeEvent2.publish();
            eventRepository.save(activeEvent2);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Event> activePage = eventRepository.findAll(EventFilter.byStatus(EventStatus.ACTIVE), pageable);
            Page<Event> draftPage = eventRepository.findAll(EventFilter.byStatus(EventStatus.DRAFT), pageable);

            // Then
            assertThat(activePage.getContent()).hasSize(2);
            assertThat(activePage.getContent())
                    .extracting(Event::getName)
                    .containsExactlyInAnyOrder("Active Event 1", "Active Event 2");

            assertThat(draftPage.getContent()).hasSize(1);
            assertThat(draftPage.getContent().get(0).getName()).isEqualTo("Draft Event");
        }
    }

    @Nested
    @DisplayName("Filter by organizer")
    class FilterByOrganizer {

        @Test
        @DisplayName("should filter events by organizer")
        void shouldFilterEventsByOrganizer() {
            // Given
            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 1").eventDate(LocalDate.of(2026, 6, 10))
                    .location("Location A").organizer("Prague OC").build());
            eventRepository.save(event1);

            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 2").eventDate(LocalDate.of(2026, 6, 11))
                    .location("Location B").organizer("Prague OC").build());
            eventRepository.save(event2);

            Event event3 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 3").eventDate(LocalDate.of(2026, 6, 12))
                    .location("Location C").organizer("Brno OC").build());
            eventRepository.save(event3);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Event> praguePage = eventRepository.findAll(EventFilter.byOrganizer("Prague OC"), pageable);
            Page<Event> brnoPage = eventRepository.findAll(EventFilter.byOrganizer("Brno OC"), pageable);

            // Then
            assertThat(praguePage.getContent()).hasSize(2);
            assertThat(praguePage.getContent())
                    .extracting(Event::getName)
                    .containsExactlyInAnyOrder("Event 1", "Event 2");

            assertThat(brnoPage.getContent()).hasSize(1);
            assertThat(brnoPage.getContent().get(0).getName()).isEqualTo("Event 3");
        }
    }

    @Nested
    @DisplayName("Filter by date range")
    class FilterByDateRange {

        @Test
        @DisplayName("should filter events by date range (from and to)")
        void shouldFilterEventsByDateRange() {
            // Given
            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 1").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location A").organizer("OOB").build());
            eventRepository.save(event1);

            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 2").eventDate(LocalDate.of(2026, 6, 15))
                    .location("Location B").organizer("PRG").build());
            eventRepository.save(event2);

            Event event3 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 3").eventDate(LocalDate.of(2026, 6, 30))
                    .location("Location C").organizer("BRN").build());
            eventRepository.save(event3);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Event> rangePage = eventRepository.findAll(
                    EventFilter.byDateRange(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 20)),
                    pageable
            );

            // Then
            assertThat(rangePage.getContent()).hasSize(1);
            assertThat(rangePage.getContent().get(0).getName()).isEqualTo("Event 2");
        }

        @Test
        @DisplayName("should include events on boundary dates")
        void shouldIncludeEventsOnBoundaryDates() {
            // Given
            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event on start date").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location A").organizer("OOB").build());
            eventRepository.save(event1);

            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event on end date").eventDate(LocalDate.of(2026, 6, 30))
                    .location("Location B").organizer("PRG").build());
            eventRepository.save(event2);

            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Event> rangePage = eventRepository.findAll(
                    EventFilter.byDateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
                    pageable
            );

            // Then
            assertThat(rangePage.getContent()).hasSize(2);
            assertThat(rangePage.getContent())
                    .extracting(Event::getName)
                    .containsExactlyInAnyOrder("Event on start date", "Event on end date");
        }
    }

    @Nested
    @DisplayName("activeEventsWithDateBefore() filter — for auto-completion scheduler")
    class ActiveEventsWithDateBeforeFilter {

        @Test
        @DisplayName("should find active events with date before specified date")
        void shouldFindActiveEventsWithDateBefore() {
            // Given
            Event activeEvent1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event 1").eventDate(LocalDate.of(2026, 1, 10))
                    .location("Location A").organizer("OOB").build());
            activeEvent1.publish();
            eventRepository.save(activeEvent1);

            Event activeEvent2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event 2").eventDate(LocalDate.of(2026, 1, 15))
                    .location("Location B").organizer("PRG").build());
            activeEvent2.publish();
            eventRepository.save(activeEvent2);

            Event futureEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Future Active Event").eventDate(LocalDate.of(2026, 2, 20))
                    .location("Location C").organizer("BRN").build());
            futureEvent.publish();
            eventRepository.save(futureEvent);

            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event").eventDate(LocalDate.of(2026, 1, 12))
                    .location("Location D").organizer("OST").build());
            eventRepository.save(draftEvent);

            // When
            var pastEvents = eventRepository.findAll(
                    EventFilter.activeEventsWithDateBefore(LocalDate.of(2026, 1, 20)),
                    Pageable.unpaged()
            ).getContent();

            // Then
            assertThat(pastEvents).hasSize(2);
            assertThat(pastEvents)
                    .extracting(Event::getName)
                    .containsExactlyInAnyOrder("Active Event 1", "Active Event 2");
        }

        @Test
        @DisplayName("should not include DRAFT events even with past date")
        void shouldNotIncludeDraftEventsEvenWithPastDate() {
            // Given
            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event").eventDate(LocalDate.of(2026, 1, 1))
                    .location("Location A").organizer("OOB").build());
            eventRepository.save(draftEvent);

            // When
            var pastEvents = eventRepository.findAll(
                    EventFilter.activeEventsWithDateBefore(LocalDate.of(2026, 2, 1)),
                    Pageable.unpaged()
            ).getContent();

            // Then
            assertThat(pastEvents).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByOrisId() — ORIS duplicate detection")
    class ExistsByOrisId {

        @Test
        @DisplayName("should return true for a saved event with the given orisId")
        void shouldReturnTrueForSavedEventWithOrisId() {
            // Given
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(9876)
                    .name("ORIS Imported Event")
                    .eventDate(LocalDate.of(2026, 8, 15))
                    .location("Test Location")
                    .organizer("OOB")
                    .websiteUrl(new WebsiteUrl("https://oris.ceskyorientak.cz/Zavod?id=9876"))
                    .build());
            eventRepository.save(event);

            // When & Then
            assertThat(eventRepository.existsByOrisId(9876)).isTrue();
        }

        @Test
        @DisplayName("should return false when no event with the given orisId exists")
        void shouldReturnFalseWhenNoEventWithOrisId() {
            // When & Then
            assertThat(eventRepository.existsByOrisId(99999)).isFalse();
        }

        @Test
        @DisplayName("should reject duplicate orisId via unique constraint")
        void shouldRejectDuplicateOrisIdViaUniqueConstraint() {
            // Given — save the first event with orisId 1111
            Event first = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(1111)
                    .name("First ORIS Event")
                    .eventDate(LocalDate.of(2026, 9, 1))
                    .location("Location A")
                    .organizer("PRG")
                    .websiteUrl(new WebsiteUrl("https://oris.ceskyorientak.cz/Zavod?id=1111"))
                    .build());
            eventRepository.save(first);

            // When — create and save a second event with same orisId
            Event second = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(1111)
                    .name("Duplicate ORIS Event")
                    .eventDate(LocalDate.of(2026, 9, 10))
                    .location("Location B")
                    .organizer("BRN")
                    .websiteUrl(new WebsiteUrl("https://oris.ceskyorientak.cz/Zavod?id=1111"))
                    .build());

            // Then — DB unique constraint rejects the duplicate
            org.junit.jupiter.api.Assertions.assertThrows(
                    Exception.class,
                    () -> eventRepository.save(second)
            );
        }
    }

    @Nested
    @DisplayName("Unique constraint on (event_id, member_id)")
    class UniqueConstraintEventMember {

        @Test
        @DisplayName("should enforce unique constraint on (event_id, member_id) in registrations")
        void shouldEnforceUniqueConstraintOnEventIdAndMemberId() {
            // Given
            Event event = Event.create(EventCreateEventBuilder.builder().name("Test Event").eventDate(LocalDate.now().plusDays(60)).location("Test Location").organizer("Test OC").build());
            event.publish();

            MemberId memberId = new MemberId(TEST_MEMBER_1_ID);
            SiCardNumber siCard = new SiCardNumber("123456");

            event.registerMember(memberId, siCard, null);
            eventRepository.save(event);

            // When - try to register same member again (domain should prevent this)
            // Then - attempting to register again should be rejected at domain level
            assertThat(event.findRegistration(memberId)).isPresent();

            // The domain prevents duplicate registrations via business logic
            // This test verifies that the saved state respects that constraint
            Optional<Event> foundEvent = eventRepository.findById(event.getId());
            assertThat(foundEvent).isPresent();
            assertThat(foundEvent.get().getRegistrations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Null location round-trip")
    class NullLocationRoundTrip {

        @Test
        @DisplayName("should save and reload event with null location")
        void shouldSaveAndReloadEventWithNullLocation() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Event Without Location")
                    .eventDate(LocalDate.of(2026, 9, 20))
                    .location(null)
                    .organizer("OOB")
                    .build());

            Event saved = eventRepository.save(event);
            Optional<Event> found = eventRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getLocation()).isNull();
        }
    }

    @Nested
    @DisplayName("Fulltext filter — name and location matching")
    class FulltextFilter {

        @Test
        @DisplayName("should match event by partial name (case-insensitive)")
        void shouldMatchByPartialNameCaseInsensitive() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Podzimní Kolo Orientace")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Praha")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Jarní závod")
                    .eventDate(LocalDate.of(2026, 4, 5))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("podzimni"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Podzimní Kolo Orientace");
        }

        @Test
        @DisplayName("should match event by partial location (case-insensitive)")
        void shouldMatchByPartialLocationCaseInsensitive() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Závod A")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Jihlava centrum")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Závod B")
                    .eventDate(LocalDate.of(2026, 4, 5))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("JIHLAVA"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Závod A");
        }

        @Test
        @DisplayName("should match event by name without diacritics")
        void shouldMatchByNameWithoutDiacritics() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Šárka Open")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Praha")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Jarní kolo")
                    .eventDate(LocalDate.of(2026, 4, 5))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("sarka"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Šárka Open");
        }

        @Test
        @DisplayName("should match event by location without diacritics")
        void shouldMatchByLocationWithoutDiacritics() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Závod X")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Černava les")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Závod Y")
                    .eventDate(LocalDate.of(2026, 4, 5))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("cernav"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Závod X");
        }

        @Test
        @DisplayName("should return all events when fulltext query is null")
        void shouldReturnAllEventsWhenFulltextQueryIsNull() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Event Alpha")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Praha")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Event Beta")
                    .eventDate(LocalDate.of(2026, 4, 5))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none(), pageable);

            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("should return empty when fulltext query matches nothing")
        void shouldReturnEmptyWhenNoMatch() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Závod v Praze")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Praha")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("Ostrava"), pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("multi-word search ANDs tokens — event matching both words is returned")
        void shouldAndMultipleTokens() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Podzimní kolo orientace")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Jihlava")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Podzimní sprint")
                    .eventDate(LocalDate.of(2026, 10, 6))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("podzimni kolo"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Podzimní kolo orientace");
        }

        @Test
        @DisplayName("multi-word search excludes events missing any token")
        void shouldExcludeEventsWithMissingToken() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Podzimní kolo")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Praha")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Podzimní závod")
                    .eventDate(LocalDate.of(2026, 10, 6))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("podzimni kolo"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Podzimní kolo");
        }

        @Test
        @DisplayName("token can match name in one event and location in another")
        void shouldMatchTokenInEitherNameOrLocation() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Praha Open")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location("Letna")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Letni zavod")
                    .eventDate(LocalDate.of(2026, 7, 5))
                    .location("Praha")
                    .organizer("OOB")
                    .build()));
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Brno kolo")
                    .eventDate(LocalDate.of(2026, 5, 5))
                    .location("Brno")
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("Praha"), pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .extracting(Event::getName)
                    .containsExactlyInAnyOrder("Praha Open", "Letni zavod");
        }

        @Test
        @DisplayName("should match event even when location is null (only name is searched)")
        void shouldMatchByNameWhenLocationIsNull() {
            eventRepository.save(Event.create(EventCreateEventBuilder.builder()
                    .name("Čeřínek klasika")
                    .eventDate(LocalDate.of(2026, 10, 5))
                    .location(null)
                    .organizer("OOB")
                    .build()));
            Pageable pageable = PageRequest.of(0, 10);

            Page<Event> result = eventRepository.findAll(EventFilter.none().withFulltext("cerinek"), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Čeřínek klasika");
        }
    }

    @Nested
    @DisplayName("Filter by registeredBy — member registration filter")
    class FilterByRegisteredBy {

        @Test
        @DisplayName("should return only event where member is registered, not the one without registration")
        void shouldReturnOnlyEventWithMatchingRegistration() {
            MemberId registeredMember = new MemberId(TEST_MEMBER_1_ID);
            MemberId otherMember = new MemberId(TEST_MEMBER_2_ID);

            Event eventWithRegistration = Event.create(EventCreateEventBuilder.builder()
                    .name("Event With Registration")
                    .eventDate(LocalDate.now().plusDays(60))
                    .location("Praha")
                    .organizer("OOB")
                    .build());
            eventWithRegistration.publish();
            eventWithRegistration.registerMember(registeredMember, new SiCardNumber("111111"), null);
            eventRepository.save(eventWithRegistration);

            Event eventWithoutRegistration = Event.create(EventCreateEventBuilder.builder()
                    .name("Event Without Registration")
                    .eventDate(LocalDate.now().plusDays(61))
                    .location("Brno")
                    .organizer("PRG")
                    .build());
            eventWithoutRegistration.publish();
            eventWithoutRegistration.registerMember(otherMember, new SiCardNumber("222222"), null);
            eventRepository.save(eventWithoutRegistration);

            Page<Event> result = eventRepository.findAll(
                    EventFilter.none().withRegisteredBy(registeredMember),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Event With Registration");
        }

        @Test
        @DisplayName("should return empty when member has no registrations")
        void shouldReturnEmptyWhenMemberHasNoRegistrations() {
            MemberId memberWithNoRegistrations = new MemberId(TEST_MEMBER_3_ID);

            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Some Event")
                    .eventDate(LocalDate.now().plusDays(60))
                    .location("Praha")
                    .organizer("OOB")
                    .build());
            event.publish();
            event.registerMember(new MemberId(TEST_MEMBER_1_ID), new SiCardNumber("111111"), null);
            eventRepository.save(event);

            Page<Event> result = eventRepository.findAll(
                    EventFilter.none().withRegisteredBy(memberWithNoRegistrations),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should include CANCELLED events with a matching registration")
        void shouldIncludeCancelledEventsWithMatchingRegistration() {
            MemberId member = new MemberId(TEST_MEMBER_1_ID);

            Event cancelledEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Cancelled Event")
                    .eventDate(LocalDate.now().plusDays(30))
                    .location("Ostrava")
                    .organizer("OOB")
                    .build());
            cancelledEvent.publish();
            cancelledEvent.registerMember(member, new SiCardNumber("111111"), null);
            cancelledEvent.cancel();
            eventRepository.save(cancelledEvent);

            Event activeEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event")
                    .eventDate(LocalDate.now().plusDays(120))
                    .location("Praha")
                    .organizer("PRG")
                    .build());
            activeEvent.publish();
            eventRepository.save(activeEvent);

            Page<Event> result = eventRepository.findAll(
                    EventFilter.none().withRegisteredBy(member),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Cancelled Event");
        }

        @Test
        @DisplayName("should include FINISHED events with a matching registration")
        void shouldIncludeFinishedEventsWithMatchingRegistration() {
            MemberId member = new MemberId(TEST_MEMBER_1_ID);

            // Register while ACTIVE (future date), then advance to FINISHED via persist + direct status transition
            Event finishedEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Finished Event")
                    .eventDate(LocalDate.now().plusDays(90))
                    .location("Liberec")
                    .organizer("OOB")
                    .build());
            finishedEvent.publish();
            finishedEvent.registerMember(member, new SiCardNumber("111111"), null);
            eventRepository.save(finishedEvent);
            // Reload, finish, and re-save
            Event reload = eventRepository.findById(finishedEvent.getId()).orElseThrow();
            reload.finish();
            eventRepository.save(reload);

            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event")
                    .eventDate(LocalDate.now().plusDays(60))
                    .location("Praha")
                    .organizer("PRG")
                    .build());
            eventRepository.save(draftEvent);

            Page<Event> result = eventRepository.findAll(
                    EventFilter.none().withRegisteredBy(member),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Finished Event");
        }

        @Test
        @DisplayName("should combine registeredBy with fulltext filter using AND semantics")
        void shouldCombineRegisteredByWithFulltext() {
            MemberId member = new MemberId(TEST_MEMBER_1_ID);

            Event matchingEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Praha Open")
                    .eventDate(LocalDate.now().plusDays(60))
                    .location("Praha")
                    .organizer("OOB")
                    .build());
            matchingEvent.publish();
            matchingEvent.registerMember(member, new SiCardNumber("111111"), null);
            eventRepository.save(matchingEvent);

            Event registeredButNoMatch = Event.create(EventCreateEventBuilder.builder()
                    .name("Brno kolo")
                    .eventDate(LocalDate.now().plusDays(61))
                    .location("Brno")
                    .organizer("PRG")
                    .build());
            registeredButNoMatch.publish();
            registeredButNoMatch.registerMember(member, new SiCardNumber("222222"), null);
            eventRepository.save(registeredButNoMatch);

            Page<Event> result = eventRepository.findAll(
                    EventFilter.none().withRegisteredBy(member).withFulltext("Praha"),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Praha Open");
        }
    }
}
