package com.klabis.calendar.application;

import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.CalendarItemKind;
import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarRepository;
import com.klabis.calendar.domain.EventCalendarItem;
import com.klabis.events.EventData;
import com.klabis.events.EventDataProvider;
import com.klabis.events.EventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CalendarEventSyncService Unit Tests")
@ExtendWith(MockitoExtension.class)
class CalendarEventSyncServiceTest {

    @Mock
    private CalendarRepository calendarRepositoryMock;

    @Mock
    private EventDataProvider eventDataProviderMock;

    private CalendarEventSyncService testedSubject;

    private static final LocalDate EVENT_DATE = LocalDate.of(2024, 6, 15);
    private static final LocalDate DEADLINE_DATE = LocalDate.of(2024, 6, 1);

    @BeforeEach
    void setUp() {
        testedSubject = new CalendarEventSyncService(calendarRepositoryMock, eventDataProviderMock);
    }

    private EventId newEventId() {
        return EventId.of(UUID.randomUUID());
    }

    private EventData eventDataWithoutDeadline(String name) {
        return new EventData(name, EVENT_DATE, "Prague", "OOB", "https://example.com", null);
    }

    private EventData eventDataWithDeadline(String name) {
        return new EventData(name, EVENT_DATE, "Prague", "OOB", "https://example.com", DEADLINE_DATE);
    }

    private EventCalendarItem existingEventDateItem(EventId eventId) {
        return EventCalendarItem.reconstruct(
                CalendarItemId.generate(),
                "Old Event Name",
                "Prague - OOB\nhttps://example.com",
                EVENT_DATE,
                EVENT_DATE,
                eventId,
                CalendarItemKind.EVENT_DATE,
                null);
    }

    private EventCalendarItem existingDeadlineItem(EventId eventId) {
        return EventCalendarItem.reconstruct(
                CalendarItemId.generate(),
                "Přihlášky - Old Event Name",
                null,
                DEADLINE_DATE,
                DEADLINE_DATE,
                eventId,
                CalendarItemKind.EVENT_REGISTRATION_DATE,
                null);
    }

    // ===== handleEventPublished() Tests =====

    @Nested
    @DisplayName("handleEventPublished")
    class HandleEventPublishedTests {

        @Test
        @DisplayName("should create exactly one EVENT_DATE item when event has no deadline")
        void shouldCreateOnlyEventDateItemWhenNoDeadline() {
            EventId eventId = newEventId();
            EventData event = eventDataWithoutDeadline("Spring Boot Workshop");

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(event);

            testedSubject.handleEventPublished(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            EventCalendarItem saved = (EventCalendarItem) captor.getValue();
            assertThat(saved.getKind()).isEqualTo(CalendarItemKind.EVENT_DATE);
            assertThat(saved.getName()).isEqualTo("Spring Boot Workshop");
            assertThat(saved.getStartDate()).isEqualTo(EVENT_DATE);
            assertThat(saved.getEndDate()).isEqualTo(EVENT_DATE);
            assertThat(saved.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should create both EVENT_DATE and EVENT_REGISTRATION_DATE items when event has deadline")
        void shouldCreateBothItemsWhenEventHasDeadline() {
            EventId eventId = newEventId();
            EventData event = eventDataWithDeadline("Spring Boot Workshop");

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(event);

            testedSubject.handleEventPublished(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(2)).save(captor.capture());

            List<EventCalendarItem> saved = captor.getAllValues().stream()
                    .map(EventCalendarItem.class::cast)
                    .toList();

            EventCalendarItem eventDateItem = saved.stream()
                    .filter(i -> i.getKind() == CalendarItemKind.EVENT_DATE)
                    .findFirst().orElseThrow();
            assertThat(eventDateItem.getName()).isEqualTo("Spring Boot Workshop");
            assertThat(eventDateItem.getStartDate()).isEqualTo(EVENT_DATE);
            assertThat(eventDateItem.getEndDate()).isEqualTo(EVENT_DATE);
            assertThat(eventDateItem.getEventId()).isEqualTo(eventId);

            EventCalendarItem deadlineItem = saved.stream()
                    .filter(i -> i.getKind() == CalendarItemKind.EVENT_REGISTRATION_DATE)
                    .findFirst().orElseThrow();
            assertThat(deadlineItem.getName()).isEqualTo("Přihlášky - Spring Boot Workshop");
            assertThat(deadlineItem.getDescription()).isNull();
            assertThat(deadlineItem.getStartDate()).isEqualTo(DEADLINE_DATE);
            assertThat(deadlineItem.getEndDate()).isEqualTo(DEADLINE_DATE);
            assertThat(deadlineItem.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should create calendar item without website URL when not present")
        void shouldCreateCalendarItemWithoutWebsiteUrl() {
            EventId eventId = newEventId();
            EventData eventData = new EventData("Java Meetup", LocalDate.of(2024, 4, 20),
                    "Brno Tech Hub", "OOB", null, null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

            testedSubject.handleEventPublished(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            assertThat(captor.getValue().getDescription()).isEqualTo("Brno Tech Hub - OOB");
            assertThat(captor.getValue().getDescription()).doesNotContain("\n");
        }

        @Test
        @DisplayName("should create calendar item with organizer-only description when event has no location")
        void shouldCreateCalendarItemWithOrganizerOnlyWhenEventHasNoLocation() {
            EventId eventId = newEventId();
            EventData eventData = new EventData("ORIS Event", LocalDate.of(2024, 5, 10),
                    null, "OOB", null, null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

            testedSubject.handleEventPublished(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            assertThat(captor.getValue().getDescription()).isEqualTo("OOB");
            assertThat(captor.getValue().getDescription()).doesNotContain(" - ");
        }

        @Test
        @DisplayName("should create calendar item with null description when all optional fields missing")
        void shouldCreateCalendarItemWithNullDescriptionWhenAllFieldsMissing() {
            EventId eventId = newEventId();
            EventData eventData = new EventData("Minimal Event", LocalDate.of(2024, 6, 1),
                    null, null, null, null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

            testedSubject.handleEventPublished(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            assertThat(captor.getValue().getDescription()).isNull();
        }
    }

    // ===== handleEventUpdated() Tests =====

    @Nested
    @DisplayName("handleEventUpdated")
    class HandleEventUpdatedTests {

        @Test
        @DisplayName("should update EVENT_DATE item when event with no deadline is updated")
        void shouldUpdateEventDateItemWhenEventIsUpdated() {
            EventId eventId = newEventId();
            CalendarItemId calendarItemId = CalendarItemId.generate();

            EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                    calendarItemId, "Old Name", "Old Location - Old Organizer",
                    LocalDate.of(2024, 3, 10), LocalDate.of(2024, 3, 10),
                    eventId, CalendarItemKind.EVENT_DATE, null);

            EventData eventData = new EventData("Updated Spring Boot Workshop",
                    LocalDate.of(2024, 3, 15), "New Prague Center", "OOB",
                    "https://example.com/updated", null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            CalendarItem updated = captor.getValue();
            assertThat(updated.getId()).isEqualTo(calendarItemId);
            assertThat(updated.getName()).isEqualTo("Updated Spring Boot Workshop");
            assertThat(updated.getDescription()).isEqualTo("New Prague Center - OOB\nhttps://example.com/updated");
            assertThat(updated.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
            assertThat(updated.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        }

        @Test
        @DisplayName("should delete EVENT_REGISTRATION_DATE item when deadline is cleared from event")
        void shouldDeleteDeadlineItemWhenDeadlineIsCleared() {
            EventId eventId = newEventId();
            EventCalendarItem eventDateItem = existingEventDateItem(eventId);
            EventCalendarItem deadlineItem = existingDeadlineItem(eventId);

            EventData eventWithoutDeadline = eventDataWithoutDeadline("Spring Boot Workshop");

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(eventDateItem, deadlineItem));
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventWithoutDeadline);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> deletedCaptor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).delete(deletedCaptor.capture());
            assertThat(((EventCalendarItem) deletedCaptor.getValue()).getKind())
                    .isEqualTo(CalendarItemKind.EVENT_REGISTRATION_DATE);

            verify(calendarRepositoryMock, times(1)).save(any());
        }

        @Test
        @DisplayName("should create EVENT_REGISTRATION_DATE item when deadline is added to event")
        void shouldCreateDeadlineItemWhenDeadlineIsAdded() {
            EventId eventId = newEventId();
            EventCalendarItem eventDateItem = existingEventDateItem(eventId);

            EventData eventWithDeadline = eventDataWithDeadline("Spring Boot Workshop");

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(eventDateItem));
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventWithDeadline);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(2)).save(captor.capture());
            verify(calendarRepositoryMock, never()).delete(any());

            List<CalendarItemKind> savedKinds = captor.getAllValues().stream()
                    .map(EventCalendarItem.class::cast)
                    .map(EventCalendarItem::getKind)
                    .toList();
            assertThat(savedKinds).containsExactlyInAnyOrder(
                    CalendarItemKind.EVENT_DATE, CalendarItemKind.EVENT_REGISTRATION_DATE);
        }

        @Test
        @DisplayName("should update both items' labels when event is renamed")
        void shouldUpdateBothItemLabelsWhenEventIsRenamed() {
            EventId eventId = newEventId();
            EventCalendarItem eventDateItem = existingEventDateItem(eventId);
            EventCalendarItem deadlineItem = existingDeadlineItem(eventId);

            EventData renamedEvent = eventDataWithDeadline("Renamed Event");

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(eventDateItem, deadlineItem));
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(renamedEvent);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(2)).save(captor.capture());
            verify(calendarRepositoryMock, never()).delete(any());

            List<EventCalendarItem> saved = captor.getAllValues().stream()
                    .map(EventCalendarItem.class::cast).toList();

            EventCalendarItem updatedDateItem = saved.stream()
                    .filter(i -> i.getKind() == CalendarItemKind.EVENT_DATE).findFirst().orElseThrow();
            assertThat(updatedDateItem.getName()).isEqualTo("Renamed Event");

            EventCalendarItem updatedDeadlineItem = saved.stream()
                    .filter(i -> i.getKind() == CalendarItemKind.EVENT_REGISTRATION_DATE).findFirst().orElseThrow();
            assertThat(updatedDeadlineItem.getName()).isEqualTo("Přihlášky - Renamed Event");
        }

        @Test
        @DisplayName("should self-heal and recreate EVENT_DATE item when it is missing")
        void shouldSelfHealAndRecreateEventDateItemWhenMissing() {
            EventId eventId = newEventId();
            EventData event = eventDataWithoutDeadline("Spring Boot Workshop");

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(event);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            EventCalendarItem created = (EventCalendarItem) captor.getValue();
            assertThat(created.getKind()).isEqualTo(CalendarItemKind.EVENT_DATE);
            assertThat(created.getName()).isEqualTo("Spring Boot Workshop");
            assertThat(created.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("should update calendar item without website URL when not present")
        void shouldUpdateCalendarItemWithoutWebsiteUrl() {
            EventId eventId = newEventId();
            CalendarItemId calendarItemId = CalendarItemId.generate();

            EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                    calendarItemId, "Old Name", "Old Location - OOB\nhttps://old-url.com",
                    LocalDate.of(2024, 4, 15), LocalDate.of(2024, 4, 15),
                    eventId, CalendarItemKind.EVENT_DATE, null);

            EventData eventData = new EventData("Updated Java Meetup", LocalDate.of(2024, 4, 20),
                    "Brno Tech Hub", "OOB", null, null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            assertThat(captor.getValue().getDescription()).isEqualTo("Brno Tech Hub - OOB");
            assertThat(captor.getValue().getDescription()).doesNotContain("\n");
        }

        @Test
        @DisplayName("should update calendar item with organizer-only description when location is cleared")
        void shouldUpdateCalendarItemWithOrganizerOnlyWhenEventLocationBecomesNull() {
            EventId eventId = newEventId();
            CalendarItemId calendarItemId = CalendarItemId.generate();

            EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                    calendarItemId, "Old Name", "Old Location - OOB",
                    LocalDate.of(2024, 4, 15), LocalDate.of(2024, 4, 15),
                    eventId, CalendarItemKind.EVENT_DATE, null);

            EventData eventData = new EventData("Updated Event No Location", LocalDate.of(2024, 5, 10),
                    null, "OOB", null, null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));
            when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

            testedSubject.handleEventUpdated(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).save(captor.capture());

            assertThat(captor.getValue().getDescription()).isEqualTo("OOB");
            assertThat(captor.getValue().getDescription()).doesNotContain(" - ");
        }
    }

    // ===== handleEventCancelled() Tests =====

    @Nested
    @DisplayName("handleEventCancelled")
    class HandleEventCancelledTests {

        @Test
        @DisplayName("should delete both EVENT_DATE and EVENT_REGISTRATION_DATE items when both exist")
        void shouldDeleteBothItemsWhenBothExist() {
            EventId eventId = newEventId();
            EventCalendarItem eventDateItem = existingEventDateItem(eventId);
            EventCalendarItem deadlineItem = existingDeadlineItem(eventId);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(eventDateItem, deadlineItem));

            testedSubject.handleEventCancelled(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(2)).delete(captor.capture());

            List<CalendarItemKind> deletedKinds = captor.getAllValues().stream()
                    .map(EventCalendarItem.class::cast)
                    .map(EventCalendarItem::getKind)
                    .toList();
            assertThat(deletedKinds).containsExactlyInAnyOrder(
                    CalendarItemKind.EVENT_DATE, CalendarItemKind.EVENT_REGISTRATION_DATE);
        }

        @Test
        @DisplayName("should delete only EVENT_DATE item when only it exists")
        void shouldDeleteOnlyEventDateItemWhenOnlyItExists() {
            EventId eventId = newEventId();
            CalendarItemId calendarItemId = CalendarItemId.generate();

            EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                    calendarItemId, "Spring Boot Workshop", "Prague Conference Center - OOB",
                    LocalDate.of(2024, 3, 15), LocalDate.of(2024, 3, 15),
                    eventId, CalendarItemKind.EVENT_DATE, null);

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));

            testedSubject.handleEventCancelled(eventId);

            ArgumentCaptor<CalendarItem> captor = ArgumentCaptor.forClass(CalendarItem.class);
            verify(calendarRepositoryMock, times(1)).delete(captor.capture());

            assertThat(captor.getValue().getId()).isEqualTo(calendarItemId);
        }

        @Test
        @DisplayName("should do nothing when no items exist for event")
        void shouldDoNothingWhenNoItemsExist() {
            EventId eventId = newEventId();

            when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());

            testedSubject.handleEventCancelled(eventId);

            verify(calendarRepositoryMock, never()).delete(any());
        }
    }
}
