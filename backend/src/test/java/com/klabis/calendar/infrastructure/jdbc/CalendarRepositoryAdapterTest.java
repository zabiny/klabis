package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.common.domain.AuditMetadata;
import com.klabis.events.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CalendarRepositoryAdapter.
 * <p>
 * Tests cover:
 * - save() - conversion and delegation
 * - findById() - conversion and delegation
 * - findByDateRange() - conversion and delegation
 * - findByEventId() - conversion and delegation
 * - delete() - delegation
 */
@DisplayName("CalendarRepositoryAdapter Unit Tests")
@ExtendWith(MockitoExtension.class)
class CalendarRepositoryAdapterTest {

    @Mock
    private CalendarJdbcRepository jdbcRepositoryMock;

    private CalendarRepositoryAdapter testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new CalendarRepositoryAdapter(jdbcRepositoryMock);
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("should convert CalendarItem to memento, save, and convert back")
        void shouldConvertCalendarItemToMementoSaveAndConvertBack() {
            // Given
            CalendarItem calendarItem = CalendarItem.create(
                    "Test Event",
                    "Test Location - Test Organizer",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            CalendarMemento savedMemento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.save(any(CalendarMemento.class))).thenReturn(savedMemento);

            // When
            CalendarItem result = testedSubject.save(calendarItem);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Event");
            assertThat(result.getDescription()).isEqualTo("Test Location - Test Organizer");
            verify(jdbcRepositoryMock).save(any(CalendarMemento.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should find by ID and convert memento to CalendarItem")
        void shouldFindByIdAndConvertMementoToCalendarItem() {
            // Given
            UUID uuid = UUID.randomUUID();
            CalendarItemId calendarItemId = new CalendarItemId(uuid);

            CalendarItem calendarItem = CalendarItem.reconstruct(
                    calendarItemId,
                    "Test Event",
                    "Test Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    null,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L)
            );

            CalendarMemento memento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.findById(uuid)).thenReturn(Optional.of(memento));

            // When
            Optional<CalendarItem> result = testedSubject.findById(calendarItemId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(calendarItemId);
            assertThat(result.get().getName()).isEqualTo("Test Event");
            verify(jdbcRepositoryMock).findById(uuid);
        }

        @Test
        @DisplayName("should return empty when calendar item not found")
        void shouldReturnEmptyWhenCalendarItemNotFound() {
            // Given
            CalendarItemId calendarItemId = CalendarItemId.generate();
            when(jdbcRepositoryMock.findById(calendarItemId.value())).thenReturn(Optional.empty());

            // When
            Optional<CalendarItem> result = testedSubject.findById(calendarItemId);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcRepositoryMock).findById(calendarItemId.value());
        }
    }

    @Nested
    @DisplayName("findByDateRange()")
    class FindByDateRangeTests {

        @Test
        @DisplayName("should find by date range and convert mementos to CalendarItems")
        void shouldFindByDateRangeAndConvertMementosToCalendarItems() {
            // Given
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);

            CalendarItem item1 = CalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event 1",
                    "Description 1",
                    LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 10),
                    null,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L)
            );

            CalendarItem item2 = CalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event 2",
                    "Description 2",
                    LocalDate.of(2026, 6, 20),
                    LocalDate.of(2026, 6, 20),
                    null,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L)
            );

            List<CalendarMemento> mementos = List.of(
                    CalendarMemento.from(item1),
                    CalendarMemento.from(item2)
            );

            when(jdbcRepositoryMock.findByDateRange(startDate, endDate)).thenReturn(mementos);

            // When
            List<CalendarItem> result = testedSubject.findByDateRange(startDate, endDate);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactly("Event 1", "Event 2");
            verify(jdbcRepositoryMock).findByDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("should return empty list when no items in date range")
        void shouldReturnEmptyListWhenNoItemsInDateRange() {
            // Given
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            when(jdbcRepositoryMock.findByDateRange(startDate, endDate)).thenReturn(List.of());

            // When
            List<CalendarItem> result = testedSubject.findByDateRange(startDate, endDate);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcRepositoryMock).findByDateRange(startDate, endDate);
        }
    }

    @Nested
    @DisplayName("findByEventId()")
    class FindByEventIdTests {

        @Test
        @DisplayName("should find by event ID and convert memento to CalendarItem")
        void shouldFindByEventIdAndConvertMementoToCalendarItem() {
            // Given
            UUID eventUuid = UUID.randomUUID();
            EventId eventId = new EventId(eventUuid);

            CalendarItem calendarItem = CalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event Name",
                    "Event Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L)
            );

            CalendarMemento memento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.findByEventId(eventUuid)).thenReturn(Optional.of(memento));

            // When
            Optional<CalendarItem> result = testedSubject.findByEventId(eventId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getEventId()).isEqualTo(eventId);
            verify(jdbcRepositoryMock).findByEventId(eventUuid);
        }

        @Test
        @DisplayName("should return empty when calendar item not found by event ID")
        void shouldReturnEmptyWhenCalendarItemNotFoundByEventId() {
            // Given
            EventId eventId = new EventId(UUID.randomUUID());
            when(jdbcRepositoryMock.findByEventId(eventId.value())).thenReturn(Optional.empty());

            // When
            Optional<CalendarItem> result = testedSubject.findByEventId(eventId);

            // Then
            assertThat(result).isEmpty();
            verify(jdbcRepositoryMock).findByEventId(eventId.value());
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should delete calendar item by ID")
        void shouldDeleteCalendarItemById() {
            // Given
            CalendarItem calendarItem = CalendarItem.create(
                    "Test Event",
                    "Test Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15)
            );

            // When
            testedSubject.delete(calendarItem);

            // Then
            verify(jdbcRepositoryMock).deleteById(calendarItem.getId().value());
        }
    }
}
