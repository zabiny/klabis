package com.klabis.calendar.application;

import com.klabis.calendar.domain.CalendarItem;
import com.klabis.calendar.domain.CalendarItemId;
import com.klabis.calendar.domain.CalendarRepository;
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
import java.util.Optional;
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
    @DisplayName("should create calendar item when event is published")
    void shouldCreateCalendarItemWhenEventIsPublished() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());

        EventData eventData = new EventData(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague Conference Center",
                "OOB",
                "https://example.com/workshop"
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        // When
        testedSubject.handleEventPublished(eventId);

        // Then
        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem savedItem = calendarItemCaptor.getValue();
        assertThat(savedItem.getName()).isEqualTo("Spring Boot Workshop");
        assertThat(savedItem.getDescription()).isEqualTo("Prague Conference Center - OOB\nhttps://example.com/workshop");
        assertThat(savedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(savedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(savedItem.getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("should create calendar item without website URL when not present")
    void shouldCreateCalendarItemWithoutWebsiteUrl() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());

        EventData eventData = new EventData(
                "Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null  // No website URL
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        // When
        testedSubject.handleEventPublished(eventId);

        // Then
        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem savedItem = calendarItemCaptor.getValue();
        assertThat(savedItem.getDescription()).isEqualTo("Brno Tech Hub - OOB");
        assertThat(savedItem.getDescription()).doesNotContain("\n");
    }

    @Test
    @DisplayName("should skip creation when calendar item already exists (idempotent)")
    void shouldSkipCreationWhenCalendarItemAlreadyExists() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());

        CalendarItem existingItem = CalendarItem.reconstruct(
                CalendarItemId.generate(),
                "Existing Item",
                "Location - Organizer",
                LocalDate.of(2024, 5, 10),
                LocalDate.of(2024, 5, 10),
                eventId,
                null
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handleEventPublished(eventId);

        // Then
        verify(calendarRepositoryMock, never()).save(any());
        verify(eventDataProviderMock, never()).getEventData(any());
    }

    // ===== handleEventUpdated() Tests =====

    @Test
    @DisplayName("should update calendar item when event is updated")
    void shouldUpdateCalendarItemWhenEventIsUpdated() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        CalendarItem existingItem = CalendarItem.reconstruct(
                calendarItemId,
                "Old Name",
                "Old Location - Old Organizer",
                LocalDate.of(2024, 3, 10),
                LocalDate.of(2024, 3, 10),
                eventId,
                null
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handleEventUpdated(
                eventId,
                "Updated Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "New Prague Center",
                "OOB",
                "https://example.com/updated"
        );

        // Then
        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem updatedItem = calendarItemCaptor.getValue();
        assertThat(updatedItem.getId()).isEqualTo(calendarItemId);
        assertThat(updatedItem.getName()).isEqualTo("Updated Spring Boot Workshop");
        assertThat(updatedItem.getDescription()).isEqualTo("New Prague Center - OOB\nhttps://example.com/updated");
        assertThat(updatedItem.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(updatedItem.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 15));
        assertThat(updatedItem.getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("should update calendar item without website URL when not present")
    void shouldUpdateCalendarItemWithoutWebsiteUrl() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        CalendarItem existingItem = CalendarItem.reconstruct(
                calendarItemId,
                "Old Name",
                "Old Location - Old Organizer\nhttps://old-url.com",
                LocalDate.of(2024, 4, 15),
                LocalDate.of(2024, 4, 15),
                eventId,
                null
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handleEventUpdated(
                eventId,
                "Updated Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null  // Website URL removed
        );

        // Then
        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).save(calendarItemCaptor.capture());

        CalendarItem updatedItem = calendarItemCaptor.getValue();
        assertThat(updatedItem.getDescription()).isEqualTo("Brno Tech Hub - OOB");
        assertThat(updatedItem.getDescription()).doesNotContain("\n");
    }

    @Test
    @DisplayName("should skip update when calendar item not found (idempotent)")
    void shouldSkipUpdateWhenCalendarItemNotFound() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());

        // When
        testedSubject.handleEventUpdated(
                eventId,
                "Non-existent Event",
                LocalDate.of(2024, 5, 10),
                "Location",
                "OOB",
                null
        );

        // Then
        verify(calendarRepositoryMock, never()).save(any());
    }

    // ===== handleEventCancelled() Tests =====

    @Test
    @DisplayName("should delete calendar item when event is cancelled")
    void shouldDeleteCalendarItemWhenEventIsCancelled() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        CalendarItemId calendarItemId = CalendarItemId.generate();

        CalendarItem existingItem = CalendarItem.reconstruct(
                calendarItemId,
                "Spring Boot Workshop",
                "Prague Conference Center - OOB",
                LocalDate.of(2024, 3, 15),
                LocalDate.of(2024, 3, 15),
                eventId,
                null
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handleEventCancelled(eventId);

        // Then
        ArgumentCaptor<CalendarItem> calendarItemCaptor = ArgumentCaptor.forClass(CalendarItem.class);
        verify(calendarRepositoryMock).delete(calendarItemCaptor.capture());

        CalendarItem deletedItem = calendarItemCaptor.getValue();
        assertThat(deletedItem.getId()).isEqualTo(calendarItemId);
        assertThat(deletedItem.getEventId()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("should skip deletion when calendar item not found (idempotent)")
    void shouldSkipDeletionWhenCalendarItemNotFound() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());

        // When
        testedSubject.handleEventCancelled(eventId);

        // Then
        verify(calendarRepositoryMock, never()).delete(any());
    }
}
