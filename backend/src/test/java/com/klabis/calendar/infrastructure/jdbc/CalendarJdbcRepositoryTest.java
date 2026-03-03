package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.CleanupTestData;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.events.EventId;
import org.jmolecules.ddd.annotation.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CalendarItem aggregate with Spring Data JDBC.
 * <p>
 * Tests cover:
 * - CRUD operations (save, findById, delete)
 * - Calendar item with all fields
 * - Date range queries (multi-day items, boundary dates)
 * - Event-linked calendar items (findByEventId)
 * - Audit metadata population
 */
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
        @DisplayName("should save and find manual calendar item")
        void shouldSaveAndFindManualCalendarItem() {
            // Given
            CalendarItem calendarItem = CalendarItem.create(
                    "Summer Training Camp",
                    "Weekly training at City Park - Coach John",
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 7)
            );

            // When
            CalendarItem saved = calendarRepository.save(calendarItem);
            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            CalendarItem retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getName()).isEqualTo("Summer Training Camp");
            assertThat(retrieved.getDescription()).isEqualTo("Weekly training at City Park - Coach John");
            assertThat(retrieved.getStartDate()).isEqualTo(LocalDate.of(2026, 7, 1));
            assertThat(retrieved.getEndDate()).isEqualTo(LocalDate.of(2026, 7, 7));
            assertThat(retrieved.getEventId()).isNull();
            assertThat(retrieved.isEventLinked()).isFalse();

            assertThat(retrieved.getAuditMetadata()).isNotNull();
            assertThat(retrieved.getAuditMetadata().createdAt()).isNotNull();
            assertThat(retrieved.getAuditMetadata().createdBy()).isNotNull();
            assertThat(retrieved.getAuditMetadata().version()).isZero();
        }

        @Test
        @DisplayName("should save and find event-linked calendar item")
        void shouldSaveAndFindEventLinkedCalendarItem() {
            // Given
            EventId eventId = new EventId(TEST_EVENT_1_ID);
            CalendarItem calendarItem = CalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "City Orienteering Championship",
                    "Prague - OOB\nhttps://example.com",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );

            // When
            CalendarItem saved = calendarRepository.save(calendarItem);
            Optional<CalendarItem> found = calendarRepository.findById(saved.getId());

            // Then
            assertThat(found).isPresent();
            CalendarItem retrieved = found.get();
            assertThat(retrieved.getId()).isEqualTo(saved.getId());
            assertThat(retrieved.getEventId()).isEqualTo(eventId);
            assertThat(retrieved.isEventLinked()).isTrue();
        }

        @Test
        @DisplayName("should return empty when calendar item not found")
        void shouldReturnEmptyWhenCalendarItemNotFound() {
            // Given
            CalendarItemId nonExistentId = CalendarItemId.generate();

            // When
            Optional<CalendarItem> found = calendarRepository.findById(nonExistentId);

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDateRange() - date range queries")
    class FindByDateRangeTests {

        @Test
        @DisplayName("should find items within date range")
        void shouldFindItemsWithinDateRange() {
            // Given - create items in June
            CalendarItem item1 = CalendarItem.create(
                    "Event 1",
                    "Description 1",
                    LocalDate.of(2026, 6, 5),
                    LocalDate.of(2026, 6, 5)
            );
            calendarRepository.save(item1);

            CalendarItem item2 = CalendarItem.create(
                    "Event 2",
                    "Description 2",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );
            calendarRepository.save(item2);

            CalendarItem item3 = CalendarItem.create(
                    "Event 3",
                    "Description 3",
                    LocalDate.of(2026, 6, 25),
                    LocalDate.of(2026, 6, 25)
            );
            calendarRepository.save(item3);

            // When - query middle of month
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 20)
            );

            // Then - only item2 is in range
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Event 2");
        }

        @Test
        @DisplayName("should include items on boundary dates")
        void shouldIncludeItemsOnBoundaryDates() {
            // Given
            CalendarItem itemOnStart = CalendarItem.create(
                    "Event on start date",
                    "Description",
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 1)
            );
            calendarRepository.save(itemOnStart);

            CalendarItem itemOnEnd = CalendarItem.create(
                    "Event on end date",
                    "Description",
                    LocalDate.of(2026, 6, 30),
                    LocalDate.of(2026, 6, 30)
            );
            calendarRepository.save(itemOnEnd);

            // When
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)
            );

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactlyInAnyOrder("Event on start date", "Event on end date");
        }

        @Test
        @DisplayName("should find multi-day items that span across boundaries")
        void shouldFindMultiDayItemsThatSpanAcrossBoundaries() {
            // Given - multi-day item spanning May-June-July
            CalendarItem multiDay = CalendarItem.create(
                    "Summer Camp",
                    "Multi-week event",
                    LocalDate.of(2026, 5, 25),
                    LocalDate.of(2026, 7, 5)
            );
            calendarRepository.save(multiDay);

            // When - query only June
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)
            );

            // Then - multi-day item should be included
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Summer Camp");
            assertThat(result.get(0).getStartDate()).isEqualTo(LocalDate.of(2026, 5, 25));
            assertThat(result.get(0).getEndDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        }

        @Test
        @DisplayName("should find items that start before range and end within range")
        void shouldFindItemsThatStartBeforeRangeAndEndWithinRange() {
            // Given
            CalendarItem item = CalendarItem.create(
                    "Overlapping Event",
                    "Description",
                    LocalDate.of(2026, 5, 20),
                    LocalDate.of(2026, 6, 10)
            );
            calendarRepository.save(item);

            // When
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)
            );

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Overlapping Event");
        }

        @Test
        @DisplayName("should find items that start within range and end after range")
        void shouldFindItemsThatStartWithinRangeAndEndAfterRange() {
            // Given
            CalendarItem item = CalendarItem.create(
                    "Overlapping Event",
                    "Description",
                    LocalDate.of(2026, 6, 20),
                    LocalDate.of(2026, 7, 10)
            );
            calendarRepository.save(item);

            // When
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)
            );

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Overlapping Event");
        }

        @Test
        @DisplayName("should return empty list when no items in range")
        void shouldReturnEmptyListWhenNoItemsInRange() {
            // Given - item in July
            CalendarItem item = CalendarItem.create(
                    "July Event",
                    "Description",
                    LocalDate.of(2026, 7, 15),
                    LocalDate.of(2026, 7, 15)
            );
            calendarRepository.save(item);

            // When - query June
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)
            );

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should sort results by start date and name")
        void shouldSortResultsByStartDateAndName() {
            // Given
            CalendarItem item1 = CalendarItem.create(
                    "B Event",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );
            calendarRepository.save(item1);

            CalendarItem item2 = CalendarItem.create(
                    "A Event",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );
            calendarRepository.save(item2);

            CalendarItem item3 = CalendarItem.create(
                    "C Event",
                    "Description",
                    LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 10)
            );
            calendarRepository.save(item3);

            // When
            List<CalendarItem> result = calendarRepository.findByDateRange(
                    LocalDate.of(2026, 6, 1),
                    LocalDate.of(2026, 6, 30)
            );

            // Then - sorted by date first, then by name
            assertThat(result).hasSize(3);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactly("C Event", "A Event", "B Event");
        }
    }

    @Nested
    @DisplayName("findByEventId() - event-linked items")
    class FindByEventIdTests {

        @Test
        @DisplayName("should find calendar item by event ID")
        void shouldFindCalendarItemByEventId() {
            // Given
            EventId eventId = new EventId(TEST_EVENT_1_ID);
            CalendarItem calendarItem = CalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "City Championship",
                    "Prague - OOB",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    null
            );
            calendarRepository.save(calendarItem);

            // When
            Optional<CalendarItem> found = calendarRepository.findByEventId(eventId);

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getEventId()).isEqualTo(eventId);
            assertThat(found.get().getName()).isEqualTo("City Championship");
        }

        @Test
        @DisplayName("should return empty when no calendar item linked to event")
        void shouldReturnEmptyWhenNoCalendarItemLinkedToEvent() {
            // Given
            EventId eventId = new EventId(TEST_EVENT_2_ID);

            // When
            Optional<CalendarItem> found = calendarRepository.findByEventId(eventId);

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("delete() - deletion")
    class DeleteTests {

        @Test
        @DisplayName("should delete calendar item")
        void shouldDeleteCalendarItem() {
            // Given
            CalendarItem calendarItem = CalendarItem.create(
                    "Event to Delete",
                    "Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );
            CalendarItem saved = calendarRepository.save(calendarItem);

            // When
            calendarRepository.delete(saved);

            // Then
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
            // Given
            CalendarItem calendarItem = CalendarItem.create(
                    "Test Event",
                    "Test Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // When
            CalendarItem saved = calendarRepository.save(calendarItem);

            // Then
            assertThat(saved.getAuditMetadata()).isNotNull();
            assertThat(saved.getAuditMetadata().createdAt()).isNotNull();
            assertThat(saved.getAuditMetadata().createdBy()).isNotNull();
            assertThat(saved.getAuditMetadata().lastModifiedAt()).isNotNull();
            assertThat(saved.getAuditMetadata().lastModifiedBy()).isNotNull();
            assertThat(saved.getAuditMetadata().version()).isZero();
        }
    }
}
