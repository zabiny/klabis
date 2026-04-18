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

    @BeforeEach
    void setUp() {
        testedSubject = new CalendarEventSyncService(calendarRepositoryMock, eventDataProviderMock);
    }

    // ===== handleEventPublished() Tests =====

    @Test
    @DisplayName("should create EventCalendarItem when event is published")
    void shouldCreateCalendarItemWhenEventIsPublished() {
        EventId eventId = EventId.of(UUID.randomUUID());

        EventData eventData = new EventData(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague Conference Center",
                "OOB",
                "https://example.com/workshop",
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventPublished(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem savedItem = calendarItemCaptor.getValue();
        assertThat(savedItem).isInstanceOf(EventCalendarItem.class);
        assertThat(savedItem.getName()).isEqualTo("Spring Boot Workshop");
        assertThat(savedItem.getDescription()).isEqualTo("Prague Conference Center - OOB\nhttps://example.com/workshop");
        assertThat(savedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(savedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(((EventCalendarItem) savedItem).getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("should create calendar item without website URL when not present")
    void shouldCreateCalendarItemWithoutWebsiteUrl() {
        EventId eventId = EventId.of(UUID.randomUUID());

        EventData eventData = new EventData(
                "Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventPublished(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem savedItem = calendarItemCaptor.getValue();
        assertThat(savedItem.getDescription()).isEqualTo("Brno Tech Hub - OOB");
        assertThat(savedItem.getDescription()).doesNotContain("\n");
    }

    @Test
    @DisplayName("should skip creation when EventCalendarItem already exists (idempotent)")
    void shouldSkipCreationWhenCalendarItemAlreadyExists() {
        EventId eventId = EventId.of(UUID.randomUUID());

        EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                CalendarItemId.generate(),
                "Existing Item",
                "Location - Organizer",
                LocalDate.of(2024, 5, 10),
                LocalDate.of(2024, 5, 10),
                eventId,
                CalendarItemKind.EVENT_DATE,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));

        testedSubject.handleEventPublished(eventId);

        verify(calendarRepositoryMock, never()).save(any());
        verify(eventDataProviderMock, never()).getEventData(any());
    }

    // ===== handleEventUpdated() Tests =====

    @Test
    @DisplayName("should update EventCalendarItem when event is updated")
    void shouldUpdateCalendarItemWhenEventIsUpdated() {
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                calendarItemId,
                "Old Name",
                "Old Location - Old Organizer",
                LocalDate.of(2024, 3, 10),
                LocalDate.of(2024, 3, 10),
                eventId,
                CalendarItemKind.EVENT_DATE,
                null);

        EventData eventData = new EventData(
                "Updated Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "New Prague Center",
                "OOB",
                "https://example.com/updated",
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventUpdated(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem updatedItem = calendarItemCaptor.getValue();
        assertThat(updatedItem.getId()).isEqualTo(calendarItemId);
        assertThat(updatedItem.getName()).isEqualTo("Updated Spring Boot Workshop");
        assertThat(updatedItem.getDescription()).isEqualTo("New Prague Center - OOB\nhttps://example.com/updated");
        assertThat(updatedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(updatedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(((EventCalendarItem) updatedItem).getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("should update calendar item without website URL when not present")
    void shouldUpdateCalendarItemWithoutWebsiteUrl() {
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                calendarItemId,
                "Old Name",
                "Old Location - Old Organizer\nhttps://old-url.com",
                LocalDate.of(2024, 4, 15),
                LocalDate.of(2024, 4, 15),
                eventId,
                CalendarItemKind.EVENT_DATE,
                null);

        EventData eventData = new EventData(
                "Updated Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventUpdated(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem updatedItem = calendarItemCaptor.getValue();
        assertThat(updatedItem.getDescription()).isEqualTo("Brno Tech Hub - OOB");
        assertThat(updatedItem.getDescription()).doesNotContain("\n");
    }

    @Test
    @DisplayName("should skip update when EventCalendarItem not found (idempotent)")
    void shouldSkipUpdateWhenCalendarItemNotFound() {
        EventId eventId = EventId.of(UUID.randomUUID());

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());

        testedSubject.handleEventUpdated(eventId);

        verify(calendarRepositoryMock, never()).save(any());
        verify(eventDataProviderMock, never()).getEventData(any());
    }

    @Test
    @DisplayName("should create calendar item with organizer-only description when event has no location")
    void shouldCreateCalendarItemWithOrganizerOnlyWhenEventHasNoLocation() {
        EventId eventId = EventId.of(UUID.randomUUID());

        EventData eventData = new EventData(
                "ORIS Event Without Location",
                LocalDate.of(2024, 5, 10),
                null,
                "OOB",
                null,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventPublished(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem savedItem = calendarItemCaptor.getValue();
        assertThat(savedItem.getDescription()).isEqualTo("OOB");
        assertThat(savedItem.getDescription()).doesNotContain(" - ");
    }

    @Test
    @DisplayName("should update calendar item with organizer-only description when event location becomes null")
    void shouldUpdateCalendarItemWithOrganizerOnlyWhenEventLocationBecomesNull() {
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                calendarItemId,
                "Old Name",
                "Old Location - OOB",
                LocalDate.of(2024, 4, 15),
                LocalDate.of(2024, 4, 15),
                eventId,
                CalendarItemKind.EVENT_DATE,
                null);

        EventData eventData = new EventData(
                "Updated Event No Location",
                LocalDate.of(2024, 5, 10),
                null,
                "OOB",
                null,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventUpdated(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem updatedItem = calendarItemCaptor.getValue();
        assertThat(updatedItem.getDescription()).isEqualTo("OOB");
        assertThat(updatedItem.getDescription()).doesNotContain(" - ");
    }

    @Test
    @DisplayName("should create calendar item with null description when event has no location, organizer, or website")
    void shouldCreateCalendarItemWithNullDescriptionWhenAllFieldsMissing() {
        EventId eventId = EventId.of(UUID.randomUUID());

        EventData eventData = new EventData(
                "Minimal Event",
                LocalDate.of(2024, 6, 1),
                null,
                null,
                null,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        testedSubject.handleEventPublished(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem savedItem = calendarItemCaptor.getValue();
        assertThat(savedItem.getDescription()).isNull();
    }

    // ===== handleEventCancelled() Tests =====

    @Test
    @DisplayName("should delete EventCalendarItem when event is cancelled")
    void shouldDeleteCalendarItemWhenEventIsCancelled() {
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        EventCalendarItem existingItem = EventCalendarItem.reconstruct(
                calendarItemId,
                "Spring Boot Workshop",
                "Prague Conference Center - OOB",
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 3, 15),
                eventId,
                CalendarItemKind.EVENT_DATE,
                null);

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of(existingItem));

        testedSubject.handleEventCancelled(eventId);

        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).delete(calendarItemCaptor.capture());

        CalendarItem deletedItem = calendarItemCaptor.getValue();
        assertThat(deletedItem.getId()).isEqualTo(calendarItemId);
        assertThat(((EventCalendarItem) deletedItem).getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("should skip deletion when EventCalendarItem not found (idempotent)")
    void shouldSkipDeletionWhenCalendarItemNotFound() {
        EventId eventId = EventId.of(UUID.randomUUID());

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(List.of());

        testedSubject.handleEventCancelled(eventId);

        verify(calendarRepositoryMock, never()).delete(any());
    }
}
