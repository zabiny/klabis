package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.*;
import com.klabis.events.EventId;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Calendar JDBC Repository Tests")
@DataJdbcTest(includeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        value = {Repository.class}))
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@CleanupTestData
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, statements = {
        "INSERT INTO events (id, name, event_date, location, organizer, website_url, event_coordinator_id, status, created_at, created_by, modified_at, modified_by, version) VALUES ('11111111-1111-1111-1111-111111111111', 'Test Event 1', '2026-06-15', 'Prague', 'OOB', 'https://example.com', NULL, 'ACTIVE', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)",
        "INSERT INTO events (id, name, event_date, location, organizer, website_url, event_coordinator_id, status, created_at, created_by, modified_at, modified_by, version) VALUES ('22222222-2222-2222-2222-222222222222', 'Test Event 2', '2026-07-20', 'Brno', 'PRG', 'https://example.com', NULL, 'ACTIVE', CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0)"
})
class CalendarJdbcRepositoryTest {

    @Autowired
    private CalendarRepository calendarRepository;

    private static final UUID TEST_EVENT_1_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEST_EVENT_2_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
    }

    @Nested
    @DisplayName("save() and findById() - round-trip with all fields")
    class SaveAndFindByIdWithAllFields {

        @Test
        @DisplayName("should save and find manual calendar item — kind column set to MANUAL")
        void shouldSaveAndFindManualCalendarItem() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Summer Training Camp")
                    .description("Weekly training at City Park - Coach John")
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 7))
                    .build());

            CalendarItem saved = calendarRepository.save(calendarItem);
            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());

            assertThat(found).isPresent();
            CalendarItem retrieved = found.get();
            assertThat(retrieved).isInstanceOf(ManualCalendarItem.class);
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Summer Training Camp");
            assertThat(retrieved.getDescription()).isEqualTo("Weekly training at City Park - Coach John");
            assertThat(retrieved.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(retrieved.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 7));

            assertThat(retrieved.getAuditMetadata()).isNotNull();
            assertThat(retrieved.getAuditMetadata().createdAt()).isNotNull();
            assertThat(retrieved.getAuditMetadata().createdBy()).isNotNull();
            assertThat(retrieved.getAuditMetadata().version()).isZero();
        }

        @Test
        @DisplayName("should save and find event-linked calendar item — kind column set to EVENT_DATE")
        void shouldSaveAndFindEventCalendarItem() {
            EventId eventId = new EventId(TEST_EVENT_1_ID);
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "City Orienteering Championship",
                    "Prague - OOB\nhttps://example.com",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    CalendarItemKind.EVENT_DATE,
                    null);

            CalendarItem saved = calendarRepository.save(calendarItem);
            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());

            assertThat(found).isPresent();
            CalendarItem retrieved = found.get();
            assertThat(retrieved).isInstanceOf(EventCalendarItem.class);
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(((EventCalendarItem) retrieved).getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should save and find registration deadline calendar item — kind column set to EVENT_REGISTRATION_DATE")
        void shouldSaveAndFindRegistrationDeadlineCalendarItem() {
            EventId eventId = new EventId(TEST_EVENT_1_ID);
            LocalDate deadline = LocalDate.of(2026, 5, 20);
            EventCalendarItem calendarItem = EventCalendarItem.createForRegistrationDeadline(
                    "City Orienteering Championship", eventId, deadline);

            CalendarItem saved = calendarRepository.save(calendarItem);
            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());

            assertThat(found).isPresent();
            CalendarItem retrieved = found.get();
            assertThat(retrieved).isInstanceOf(EventCalendarItem.class);
            EventCalendarItem retrievedEvent = (EventCalendarItem) retrieved;
            assertThat(retrievedEvent.getId()).isEqualTo(saved.getId());
            assertThat(retrievedEvent.getKind()).isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);
            assertThat(retrievedEvent.getName()).isEqualTo("Přihlášky - City Orienteering Championship");
            assertThat(retrievedEvent.getDescription()).isNull();
            assertThat(retrievedEvent.getStartDate()).isEqualTo(deadline);
            assertThat(retrievedEvent.getEndDate()).isEqualTo(deadline);
            assertThat(retrievedEvent.getEventId()).isEqualTo(eventId);
            assertThat(retrievedEvent.getAuditMetadata()).isNotNull();
            assertThat(retrievedEvent.getAuditMetadata().version()).isZero();
        }

        @Test
        @DisplayName("should save and find manual calendar item with null description")
        void shouldSaveAndFindManualCalendarItemWithNullDescription() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Klubová schůze")
                    .description(null)
                    .startDate(LocalDate.of(2026, 7, 1))
                    .endDate(LocalDate.of(2026, 7, 1))
                    .build());

            CalendarItem saved = calendarRepository.save(calendarItem);
            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getDescription()).isNull();
        }

        @Test
        @DisplayName("should return empty when calendar item not found")
        void shouldReturnEmptyWhenCalendarItemNotFound() {
            CalendarItemId nonExistentId = CalendarItemId.generate();

            Optional<CalendarItem> found = calendarRepository.findById(nonExistentId);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDateRange() - date range queries")
    class FindByDateRangeTests {

        @Test
        @DisplayName("should find items within date range")
        void shouldFindItemsWithinDateRange() {
            ManualCalendarItem item1 = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Event 1")
                    .description("Description 1")
                    .startDate(LocalDate.of(2026, 6, 5))
                    .endDate(LocalDate.of(2026, 6, 5))
                    .build());
            calendarRepository.save(item1);

            ManualCalendarItem item2 = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Event 2")
                    .description("Description 2")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());
            calendarRepository.save(item2);

            ManualCalendarItem item3 = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Event 3")
                    .description("Description 3")
                    .startDate(LocalDate.of(2026, 6, 25))
                    .endDate(LocalDate.of(2026, 6, 25))
                    .build());
            calendarRepository.save(item3);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 20));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Event 2");
        }

        @Test
        @DisplayName("should include items on boundary dates")
        void shouldIncludeItemsOnBoundaryDates() {
            ManualCalendarItem itemOnStart = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Event on start date")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 1))
                    .endDate(LocalDate.of(2026, 6, 1))
                    .build());
            calendarRepository.save(itemOnStart);

            ManualCalendarItem itemOnEnd = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Event on end date")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 30))
                    .endDate(LocalDate.of(2026, 6, 30))
                    .build());
            calendarRepository.save(itemOnEnd);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30));

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactlyInAnyOrder("Event on start date", "Event on end date");
        }

        @Test
        @DisplayName("should find multi-day items that span across boundaries")
        void shouldFindMultiDayItemsThatSpanAcrossBoundaries() {
            ManualCalendarItem multiDay = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Summer Camp")
                    .description("Multi-week event")
                    .startDate(LocalDate.of(2026, 5, 25))
                    .endDate(LocalDate.of(2026, 7, 5))
                    .build());
            calendarRepository.save(multiDay);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Summer Camp");
            assertThat(result.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 5, 25));
            assertThat(result.get(0).getEndDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        }

        @Test
        @DisplayName("should find items that start before range and end within range")
        void shouldFindItemsThatStartBeforeRangeAndEndWithinRange() {
            ManualCalendarItem item = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Overlapping Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 5, 20))
                    .endDate(LocalDate.of(2026, 6, 10))
                    .build());
            calendarRepository.save(item);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Overlapping Event");
        }

        @Test
        @DisplayName("should find items that start within range and end after range")
        void shouldFindItemsThatStartWithinRangeAndEndAfterRange() {
            ManualCalendarItem item = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Overlapping Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 20))
                    .endDate(LocalDate.of(2026, 7, 10))
                    .build());
            calendarRepository.save(item);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Overlapping Event");
        }

        @Test
        @DisplayName("should return empty list when no items in range")
        void shouldReturnEmptyListWhenNoItemsInRange() {
            ManualCalendarItem item = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("July Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 7, 15))
                    .endDate(LocalDate.of(2026, 7, 15))
                    .build());
            calendarRepository.save(item);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should sort results by start date and name")
        void shouldSortResultsByStartDateAndName() {
            ManualCalendarItem item1 = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("B Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());
            calendarRepository.save(item1);

            ManualCalendarItem item2 = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("A Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());
            calendarRepository.save(item2);

            ManualCalendarItem item3 = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("C Event")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 10))
                    .endDate(LocalDate.of(2026, 6, 10))
                    .build());
            calendarRepository.save(item3);

            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30));

            assertThat(result).hasSize(3);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactly("C Event", "A Event", "B Event");
        }
    }

    @Nested
    @DisplayName("findByEventId() - returns List")
    class FindByEventIdTests {

        @Test
        @DisplayName("should find calendar item by event ID and return as list")
        void shouldFindCalendarItemByEventIdAsList() {
            EventId eventId = new EventId(TEST_EVENT_1_ID);
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "City Championship",
                    "Prague - OOB",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    CalendarItemKind.EVENT_DATE,
                    null);
            calendarRepository.save(calendarItem);

            List<CalendarItem> found = calendarRepository.findByEventId(eventId);

            assertThat(found).hasSize(1);
            assertThat(found.get(0)).isInstanceOf(EventCalendarItem.class);
            assertThat(((EventCalendarItem) found.get(0)).getEventId()).isEqualTo(eventId);
            assertThat(found.get(0).getName()).isEqualTo("City Championship");
        }

        @Test
        @DisplayName("should return empty list when no calendar item linked to event")
        void shouldReturnEmptyListWhenNoCalendarItemLinkedToEvent() {
            EventId eventId = new EventId(TEST_EVENT_2_ID);

            List<CalendarItem> found = calendarRepository.findByEventId(eventId);

            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete() - deletion")
    class DeleteTests {

        @Test
        @DisplayName("should delete calendar item")
        void shouldDeleteCalendarItem() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Event to Delete")
                    .description("Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());
            CalendarItem saved = calendarRepository.save(calendarItem);

            calendarRepository.delete(saved);

            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("Audit metadata")
    class AuditMetadataTests {

        @Test
        @DisplayName("should populate audit metadata on save")
        void shouldPopulateAuditMetadataOnSave() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Test Event")
                    .description("Test Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            CalendarItem saved = calendarRepository.save(calendarItem);

            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getAuditMetadata().createdAt()).isNotNull();
            assertThat(saved.getAuditMetadata().createdBy()).isNotNull();
            assertThat(saved.getAuditMetadata().lastModifiedAt()).isNotNull();
            assertThat(saved.getAuditMetadata().lastModifiedBy()).isNotNull();
            assertThat(saved.getAuditMetadata().version()).isZero();
        }
    }
}
