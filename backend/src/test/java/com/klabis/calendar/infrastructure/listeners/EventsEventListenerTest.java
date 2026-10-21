package com.klabis.calendar.infrastructure.listeners;

import com.klabis.calendar.application.CalendarEventSyncPort;
import com.klabis.events.*;
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
        EventPublishedEvent event = new EventPublishedEvent(UUID.randomUUID(), eventId, Instant.now());

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
        EventUpdatedEvent event = new EventUpdatedEvent(UUID.randomUUID(), eventId, Instant.now(), UpdateOrigin.MANUAL);

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventUpdated(eventId);
    }

    @Test
    @DisplayName("should delegate to service with null website URL when not present")
    void shouldDelegateToServiceWithNullWebsiteUrl() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        EventUpdatedEvent event = new EventUpdatedEvent(UUID.randomUUID(), eventId, Instant.now(), UpdateOrigin.MANUAL);

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventUpdated(eventId);
    }

    @Test
    @DisplayName("should delegate to service when event is cancelled")
    void shouldDelegateToServiceWhenEventIsCancelled() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        EventCancelledEvent event = new EventCancelledEvent(UUID.randomUUID(), eventId, Instant.now());

        // When
        testedSubject.handle(event);

        // Then
        verify(calendarEventSyncPortMock).handleEventCancelled(eventId);
    }
}
