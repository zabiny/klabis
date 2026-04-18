package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarItemCreateCalendarItemBuilder;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.calendar.domain.ManualCalendarItem;
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
        @DisplayName("should convert ManualCalendarItem to memento with MANUAL kind, save, and convert back")
        void shouldConvertManualCalendarItemToMementoSaveAndConvertBack() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Test Event")
                    .description("Test Location - Test Organizer")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            CalendarMemento savedMemento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.save(any(CalendarMemento.class))).thenReturn(savedMemento);

            CalendarItem result = testedSubject.save(calendarItem);

            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(ManualCalendarItem.class);
            assertThat(result.getName()).isEqualTo("Test Event");
            verify(jdbcRepositoryMock).save(any(CalendarMemento.class));
        }

        @Test
        @DisplayName("should convert EventCalendarItem to memento with EVENT_DATE kind, save, and convert back")
        void shouldConvertEventCalendarItemToMementoSaveAndConvertBack() {
            EventId eventId = new EventId(UUID.randomUUID());
            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Championship",
                    "Prague - OOB",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    CalendarItemKind.EVENT_DATE,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            CalendarMemento savedMemento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.save(any(CalendarMemento.class))).thenReturn(savedMemento);

            CalendarItem result = testedSubject.save(calendarItem);

            assertThat(result).isInstanceOf(EventCalendarItem.class);
            assertThat(((EventCalendarItem) result).getEventId()).isEqualTo(eventId);
            verify(jdbcRepositoryMock).save(any(CalendarMemento.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("should find by ID and return ManualCalendarItem when kind is MANUAL")
        void shouldFindByIdAndReturnManualCalendarItem() {
            UUID uuid = UUID.randomUUID();
            CalendarItemId calendarItemId = new CalendarItemId(uuid);

            ManualCalendarItem calendarItem = ManualCalendarItem.reconstruct(
                    calendarItemId,
                    "Test Event",
                    "Test Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            CalendarMemento memento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.findById(uuid)).thenReturn(Optional.of(memento));

            Optional<CalendarItem> result = testedSubject.findById(calendarItemId);

            assertThat(result).isPresent();
            assertThat(result.get()).isInstanceOf(ManualCalendarItem.class);
            assertThat(result.get().getId()).isEqualTo(calendarItemId);
        }

        @Test
        @DisplayName("should return empty when calendar item not found")
        void shouldReturnEmptyWhenCalendarItemNotFound() {
            CalendarItemId calendarItemId = CalendarItemId.generate();
            when(jdbcRepositoryMock.findById(calendarItemId.value())).thenReturn(Optional.empty());

            Optional<CalendarItem> result = testedSubject.findById(calendarItemId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByDateRange()")
    class FindByDateRangeTests {

        @Test
        @DisplayName("should find by date range and convert mementos to CalendarItems")
        void shouldFindByDateRangeAndConvertMementosToCalendarItems() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);

            ManualCalendarItem item1 = ManualCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event 1",
                    "Description 1",
                    LocalDate.of(2026, 6, 10),
                    LocalDate.of(2026, 6, 10),
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            ManualCalendarItem item2 = ManualCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event 2",
                    "Description 2",
                    LocalDate.of(2026, 6, 20),
                    LocalDate.of(2026, 6, 20),
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            List<CalendarMemento> mementos = List.of(
                    CalendarMemento.from(item1),
                    CalendarMemento.from(item2));

            when(jdbcRepositoryMock.findByDateRange(startDate, endDate)).thenReturn(mementos);

            List<CalendarItem> result = testedSubject.findByDateRange(startDate, endDate);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CalendarItem::getName)
                    .containsExactly("Event 1", "Event 2");
            verify(jdbcRepositoryMock).findByDateRange(startDate, endDate);
        }

        @Test
        @DisplayName("should return empty list when no items in date range")
        void shouldReturnEmptyListWhenNoItemsInDateRange() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            when(jdbcRepositoryMock.findByDateRange(startDate, endDate)).thenReturn(List.of());

            List<CalendarItem> result = testedSubject.findByDateRange(startDate, endDate);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByEventId()")
    class FindByEventIdTests {

        @Test
        @DisplayName("should find by event ID and return EventCalendarItem in a list")
        void shouldFindByEventIdAndReturnEventCalendarItemInList() {
            UUID eventUuid = UUID.randomUUID();
            EventId eventId = new EventId(eventUuid);

            EventCalendarItem calendarItem = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(),
                    "Event Name",
                    "Event Description",
                    LocalDate.of(2026, 6, 15),
                    LocalDate.of(2026, 6, 15),
                    eventId,
                    CalendarItemKind.EVENT_DATE,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            CalendarMemento memento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.findByEventId(eventUuid)).thenReturn(List.of(memento));

            List<CalendarItem> result = testedSubject.findByEventId(eventId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isInstanceOf(EventCalendarItem.class);
            assertThat(((EventCalendarItem) result.get(0)).getEventId()).isEqualTo(eventId);
            verify(jdbcRepositoryMock).findByEventId(eventUuid);
        }

        @Test
        @DisplayName("should return empty list when no calendar item linked to event")
        void shouldReturnEmptyListWhenNoCalendarItemLinkedToEvent() {
            EventId eventId = new EventId(UUID.randomUUID());
            when(jdbcRepositoryMock.findByEventId(eventId.value())).thenReturn(List.of());

            List<CalendarItem> result = testedSubject.findByEventId(eventId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("kind column maps correctly: MANUAL memento roundtrips as ManualCalendarItem")
        void mementoKindColumnMapsManualCorrectly() {
            ManualCalendarItem manual = ManualCalendarItem.reconstruct(
                    CalendarItemId.generate(), "Manual", null,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1),
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            CalendarMemento memento = CalendarMemento.from(manual);
            assertThat(memento.kind()).isEqualTo(CalendarItemKind.MANUAL);

            CalendarItem roundtripped = memento.toCalendarItem();
            assertThat(roundtripped).isInstanceOf(ManualCalendarItem.class);
        }

        @Test
        @DisplayName("kind column maps correctly: EVENT_DATE memento roundtrips as EventCalendarItem")
        void mementoKindColumnMapsEventDateCorrectly() {
            EventId eventId = new EventId(UUID.randomUUID());
            EventCalendarItem eventDate = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(), "EventDate", null,
                    LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 1), eventId,
                    CalendarItemKind.EVENT_DATE,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));

            CalendarMemento memento = CalendarMemento.from(eventDate);
            assertThat(memento.kind()).isEqualTo(CalendarItemKind.EVENT_DATE);

            CalendarItem roundtripped = memento.toCalendarItem();
            assertThat(roundtripped).isInstanceOf(EventCalendarItem.class);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("should delete calendar item by ID")
        void shouldDeleteCalendarItemById() {
            ManualCalendarItem calendarItem = ManualCalendarItem.create(CalendarItemCreateCalendarItemBuilder.builder()
                    .name("Test Event")
                    .description("Test Description")
                    .startDate(LocalDate.of(2026, 6, 15))
                    .endDate(LocalDate.of(2026, 6, 15))
                    .build());

            testedSubject.delete(calendarItem);

            verify(jdbcRepositoryMock).deleteById(calendarItem.getId().value());
        }
    }
}
