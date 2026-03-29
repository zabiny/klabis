package com.klabis.calendar.infrastructure.listeners;

import com.klabis.calendar.application.CalendarEventSyncPort;
import com.klabis.events.*;
import com.klabis.events.domain.Event;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.EventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@DisplayName("EventsEventListener Unit Tests")
@ExtendWith(MockitoExtension.class)
class EventsEventListenerTest {

    @Mock
    private CalendarEventSyncPort calendarEventSyncPortMock;

    private EventsEventListener testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new EventsEventListener(calendarEventSyncPortMock);
    }

    @Test
    @DisplayName("should delegate to service when event is published")
    void shouldDelegateToServiceWhenEventIsPublished() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        Event aggregate = Event.reconstruct(eventId, "Test", LocalDate.now(), "Location", "OOB", null, null, null, EventStatus.ACTIVE, null, List.of(), null);
        EventPublishedEvent event = EventPublishedEvent.fromAggregate(aggregate);

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventPublished(eventId);
    }

    @Test
    @DisplayName("should delegate to service when event is updated")
    void shouldDelegateToServiceWhenEventIsUpdated() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        EventUpdatedEvent event = new EventUpdatedEvent(
                UUID.randomUUID(),
                eventId,
                "Updated Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "New Prague Center",
                "OOB",
                WebsiteUrl.of("https://example.com/updated"),
                Instant.now()
        );

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventUpdated(
                eventId,
                "Updated Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "New Prague Center",
                "OOB",
                "https://example.com/updated"
        );
    }

    @Test
    @DisplayName("should delegate to service with null website URL when not present")
    void shouldDelegateToServiceWithNullWebsiteUrl() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        EventUpdatedEvent event = new EventUpdatedEvent(
                UUID.randomUUID(),
                eventId,
                "Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null,
                Instant.now()
        );

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventUpdated(
                eventId,
                "Java Meetup",
                LocalDate.of(2024, 4, 20),
                "Brno Tech Hub",
                "OOB",
                null
        );
    }

    @Test
    @DisplayName("should delegate to service when event is cancelled")
    void shouldDelegateToServiceWhenEventIsCancelled() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        Event aggregate = Event.reconstruct(eventId, "Test", LocalDate.now(), "Location", "OOB", null, null, null, EventStatus.CANCELLED, null, List.of(), null);
        EventCancelledEvent event = EventCancelledEvent.fromAggregate(aggregate);

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventCancelled(eventId);
    }
}
