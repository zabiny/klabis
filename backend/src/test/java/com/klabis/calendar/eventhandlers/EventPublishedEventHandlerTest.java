package com.klabis.calendar.eventhandlers;

import com.klabis.calendar.CalendarItem;
import com.klabis.calendar.CalendarItemId;
import com.klabis.calendar.persistence.CalendarRepository;
import com.klabis.events.EventId;
import com.klabis.events.EventPublishedEvent;
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

@DisplayName("EventPublishedEventHandler Unit Tests")
@ExtendWith(MockitoExtension.class)
class EventPublishedEventHandlerTest {

    @Mock
    private CalendarRepository calendarRepositoryMock;

    @Mock
    private EventDataProvider eventDataProviderMock;

    private EventPublishedEventHandler testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new EventPublishedEventHandler(calendarRepositoryMock, eventDataProviderMock);
    }

    @Test
    @DisplayName("should create calendar item when event is published")
    void shouldCreateCalendarItemWhenEventIsPublished() {
        // Given
        EventId eventId = EventId.of(UUID.randomUUID());
        EventPublishedEvent event = new EventPublishedEvent(eventId, Instant.now());

        EventData eventData = new EventData(
                "Spring Boot Workshop",
                LocalDate.of(2024, 3, 15),
                "Prague Conference Center",
                "OOB",
                WebsiteUrl.of("https://example.com/workshop")
        );

        when(calendarRepositoryMock.findByEventId(eventId)).thenReturn(Optional.empty());
        when(eventDataProviderMock.getEventData(eventId)).thenReturn(eventData);

        // When
        testedSubject.handle(event);

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
        EventPublishedEvent event = new EventPublishedEvent(eventId, Instant.now());

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
        testedSubject.handle(event);

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
        EventPublishedEvent event = new EventPublishedEvent(eventId, Instant.now());

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
        testedSubject.handle(event);

        // Then
        verify(calendarRepositoryMock, never()).save(any());
        verify(eventDataProviderMock, never()).getEventData(any());
    }
}
