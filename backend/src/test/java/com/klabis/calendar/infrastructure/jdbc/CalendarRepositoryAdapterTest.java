package com.klabis.calendar.infrastructure.jdbc;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarFilter;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.relational.core.query.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CalendarRepositoryAdapter Unit Tests")
@ExtendWith(MockitoExtension.class)
class CalendarRepositoryAdapterTest {

    @Mock
    private CalendarJdbcRepository jdbcRepositoryMock;

    @Mock
    private JdbcAggregateTemplate jdbcAggregateTemplateMock;

    private CalendarRepositoryAdapter testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new CalendarRepositoryAdapter(jdbcRepositoryMock, jdbcAggregateTemplateMock);
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
    @DisplayName("findByFilter()")
    class FindByFilterTests {

        @Test
        @DisplayName("should delegate to JdbcAggregateTemplate with date-range-only criteria when filter has no item types or event IDs")
        void shouldDelegateToTemplateWithDateRangeCriteriaWhenNoRestrictions() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);

            ManualCalendarItem item = ManualCalendarItem.reconstruct(
                    CalendarItemId.generate(), "Event 1", "Description 1",
                    LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10),
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of(CalendarMemento.from(item)));

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate);
            List<CalendarItem> result = testedSubject.findByFilter(filter, Sort.unsorted());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Event 1");
            verify(jdbcAggregateTemplateMock).findAll(any(Query.class), eq(CalendarMemento.class));
        }

        @Test
        @DisplayName("should apply kind filter in Criteria when filter restricts item types")
        void shouldApplyKindCriteriaWhenItemTypesRestricted() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);

            ManualCalendarItem item = ManualCalendarItem.reconstruct(
                    CalendarItemId.generate(), "Manual Event", null,
                    LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 10),
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of(CalendarMemento.from(item)));

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate)
                    .withItemTypes(Set.of(CalendarItemKind.MANUAL));
            List<CalendarItem> result = testedSubject.findByFilter(filter, Sort.unsorted());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Manual Event");
            verify(jdbcAggregateTemplateMock).findAll(any(Query.class), eq(CalendarMemento.class));
        }

        @Test
        @DisplayName("should apply event_id filter in Criteria when filter restricts event IDs")
        void shouldApplyEventIdCriteriaWhenEventIdsRestricted() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            EventId eventId = new EventId(UUID.randomUUID());

            EventCalendarItem item = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(), "Event Date Item", null,
                    LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15),
                    eventId, CalendarItemKind.EVENT_DATE,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of(CalendarMemento.from(item)));

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate)
                    .withEventIds(Set.of(eventId));
            List<CalendarItem> result = testedSubject.findByFilter(filter, Sort.unsorted());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Event Date Item");
            verify(jdbcAggregateTemplateMock).findAll(any(Query.class), eq(CalendarMemento.class));
        }

        @Test
        @DisplayName("should apply both kind and event_id filters in Criteria when both are restricted")
        void shouldApplyBothKindAndEventIdCriteriaWhenBothRestricted() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            EventId eventId = new EventId(UUID.randomUUID());

            EventCalendarItem item = EventCalendarItem.reconstruct(
                    CalendarItemId.generate(), "My Schedule Event", null,
                    LocalDate.of(2026, 6, 15), LocalDate.of(2026, 6, 15),
                    eventId, CalendarItemKind.EVENT_DATE,
                    new AuditMetadata(Instant.now(), "test", Instant.now(), "test", 0L));
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of(CalendarMemento.from(item)));

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate)
                    .withItemTypes(Set.of(CalendarItemKind.EVENT_DATE))
                    .withEventIds(Set.of(eventId));
            List<CalendarItem> result = testedSubject.findByFilter(filter, Sort.unsorted());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("My Schedule Event");
            verify(jdbcAggregateTemplateMock).findAll(any(Query.class), eq(CalendarMemento.class));
        }

        @Test
        @DisplayName("should return empty list when JdbcAggregateTemplate returns no results")
        void shouldReturnEmptyListWhenNoResults() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of());

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate);
            List<CalendarItem> result = testedSubject.findByFilter(filter, Sort.unsorted());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should apply default sort (start_date ASC, name ASC) when Sort is unsorted")
        void shouldApplyDefaultSortWhenSortIsUnsorted() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of());

            ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate);
            testedSubject.findByFilter(filter, Sort.unsorted());

            verify(jdbcAggregateTemplateMock).findAll(queryCaptor.capture(), eq(CalendarMemento.class));
            Sort sort = queryCaptor.getValue().getSort();
            assertThat(sort.isSorted()).isTrue();
            List<Sort.Order> orders = sort.toList();
            assertThat(orders).hasSize(2);
            assertThat(orders.get(0).getProperty()).isEqualTo("start_date");
            assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
            assertThat(orders.get(1).getProperty()).isEqualTo("name");
            assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.ASC);
        }

        @Test
        @DisplayName("should translate domain property names to column names in sort")
        void shouldTranslateDomainPropertyNamesToColumnNamesInSort() {
            LocalDate startDate = LocalDate.of(2026, 6, 1);
            LocalDate endDate = LocalDate.of(2026, 6, 30);
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of());

            ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);

            CalendarFilter filter = CalendarFilter.dateRange(startDate, endDate);
            testedSubject.findByFilter(filter, Sort.by("startDate").ascending().and(Sort.by("name")));

            verify(jdbcAggregateTemplateMock).findAll(queryCaptor.capture(), eq(CalendarMemento.class));
            Sort sort = queryCaptor.getValue().getSort();
            List<Sort.Order> orders = sort.toList();
            assertThat(orders.get(0).getProperty()).isEqualTo("start_date");
        }
    }

    @Nested
    @DisplayName("findByEventId()")
    class FindByEventIdTests {

        @Test
        @DisplayName("should delegate to JdbcAggregateTemplate with event_id criteria")
        void shouldDelegateToTemplateWithEventIdCriteria() {
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

            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of(CalendarMemento.from(calendarItem)));

            List<CalendarItem> result = testedSubject.findByEventId(eventId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).isInstanceOf(EventCalendarItem.class);
            assertThat(((EventCalendarItem) result.get(0)).getEventId()).isEqualTo(eventId);
            verify(jdbcAggregateTemplateMock).findAll(any(Query.class), eq(CalendarMemento.class));
        }

        @Test
        @DisplayName("should return empty list when no calendar item linked to event")
        void shouldReturnEmptyListWhenNoCalendarItemLinkedToEvent() {
            EventId eventId = new EventId(UUID.randomUUID());
            when(jdbcAggregateTemplateMock.findAll(any(Query.class), eq(CalendarMemento.class)))
                    .thenReturn(List.of());

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

        @Test
        @DisplayName("kind column maps correctly: EVENT_REGISTRATION_DATE memento roundtrips as EventCalendarItem with correct kind")
        void mementoKindColumnMapsEventRegistrationDateCorrectly() {
            EventId eventId = new EventId(UUID.randomUUID());
            LocalDate deadline = LocalDate.of(2026, 5, 1);
            EventCalendarItem registrationDeadline = EventCalendarItem.createForRegistrationDeadline(
                    "City Championship", eventId, deadline);

            CalendarMemento memento = CalendarMemento.from(registrationDeadline);
            assertThat(memento.kind()).isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);

            CalendarItem roundtripped = memento.toCalendarItem();
            assertThat(roundtripped).isInstanceOf(EventCalendarItem.class);
            EventCalendarItem roundtrippedEvent = (EventCalendarItem) roundtripped;
            assertThat(roundtrippedEvent.getKind()).isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);
            assertThat(roundtrippedEvent.getEventId()).isEqualTo(eventId);
            assertThat(roundtrippedEvent.getName()).isEqualTo("Přihlášky - City Championship");
            assertThat(roundtrippedEvent.getDescription()).isNull();
            assertThat(roundtrippedEvent.getStartDate()).isEqualTo(deadline);
            assertThat(roundtrippedEvent.getEndDate()).isEqualTo(deadline);
        }
    }

    @Nested
    @DisplayName("save() — EVENT_REGISTRATION_DATE kind")
    class SaveEventRegistrationDateTests {

        @Test
        @DisplayName("should convert EventCalendarItem with EVENT_REGISTRATION_DATE kind to memento, save, and convert back")
        void shouldConvertRegistrationDeadlineItemToMementoSaveAndConvertBack() {
            EventId eventId = new EventId(UUID.randomUUID());
            LocalDate deadline = LocalDate.of(2026, 5, 10);
            EventCalendarItem calendarItem = EventCalendarItem.createForRegistrationDeadline(
                    "Sprint Race", eventId, deadline);

            CalendarMemento savedMemento = CalendarMemento.from(calendarItem);
            when(jdbcRepositoryMock.save(any(CalendarMemento.class))).thenReturn(savedMemento);

            CalendarItem result = testedSubject.save(calendarItem);

            assertThat(result).isInstanceOf(EventCalendarItem.class);
            EventCalendarItem resultEvent = (EventCalendarItem) result;
            assertThat(resultEvent.getKind()).isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);
            assertThat(resultEvent.getEventId()).isEqualTo(eventId);
            assertThat(resultEvent.getName()).isEqualTo("Přihlášky - Sprint Race");
            assertThat(resultEvent.getDescription()).isNull();
            assertThat(resultEvent.getStartDate()).isEqualTo(deadline);
            assertThat(resultEvent.getEndDate()).isEqualTo(deadline);
            verify(jdbcRepositoryMock).save(any(CalendarMemento.class));
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
