package com.klabis.calendar.eventhandlers;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.EventId;
import com.klabis.events.EventUpdatedEvent;
import com.klabis.events.WebsiteUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("EventUpdatedEventHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class EventUpdatedEventHandlerTest {

    @Mock
    private CalendarRepository calendarRepositoryMock;

    private EventUpdatedEventHandler testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new EventUpdatedEventHandler(calendarRepositoryMock);
    }

    @Test
    @DisplayName("should update calendar item when event is updated")
    void shouldUpdateCalendarItemWhenEventIsUpdated() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID()); // event-123");
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

        EventUpdatedEvent event = new EventUpdatedEvent(
                eventId,
                "Updated Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "New Prague Center",
                "OOB",
                WebsiteUrl.of("https://example.com/updated"),
                Instant.now()
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handle(event);

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
        EventId eventId = EventId.of(UUID.randomUUID()); // event-456");
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

        EventUpdatedEvent event = new EventUpdatedEvent(
                eventId,
                "Updated Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null,  // Website URL removed
                Instant.now()
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handle(event);

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
        EventId eventId = EventId.of(UUID.randomUUID()); // event-789");

        EventUpdatedEvent event = new EventUpdatedEvent(
                eventId,
                "Non-existent Event",
                LocalDate.of(2024, 5, 10),
                "Location",
                "OOB",
                null,
                Instant.now()
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarRepositoryMock, never()).save(any());
    }
}
