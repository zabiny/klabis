package com.klabis.calendar.eventhandlers;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.EventCancelledEvent;
import com.klabis.events.EventId;
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

@DisplayName("EventCancelledEventHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class EventCancelledEventHandlerTest {

    @Mock
    private CalendarRepository calendarRepositoryMock;

    private EventCancelledEventHandler testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new EventCancelledEventHandler(calendarRepositoryMock);
    }

    @Test
    @DisplayName("should delete calendar item when event is cancelled")
    void shouldDeleteCalendarItemWhenEventIsCancelled() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID()); // event-123");
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

        EventCancelledEvent event = new EventCancelledEvent(eventId, Instant.now());

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.of(existingItem));

        // When
        testedSubject.handle(event);

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
        EventId eventId = EventId.of(UUID.randomUUID()); // event-789");
        EventCancelledEvent event = new EventCancelledEvent(eventId, Instant.now());

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarRepositoryMock, never()).delete(any());
    }
}
