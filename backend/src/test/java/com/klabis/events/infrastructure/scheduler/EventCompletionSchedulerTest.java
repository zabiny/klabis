package com.klabis.events.infrastructure.scheduler;

import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventBuilder;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.domain.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * TDD Test for EventCompletionScheduler (RED phase).
 * <p>
 * Tests scheduler behavior:
 * - Finds ACTIVE events with past date and transitions them to FINISHED
 * - DRAFT events with past date are NOT affected
 * - Idempotent execution (running twice produces same result)
 * - Graceful handling when no events need completion
 * - Individual event failure should not stop processing others
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventCompletionScheduler")
class EventCompletionSchedulerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventCompletionScheduler scheduler;

    @Nested
    @DisplayName("completeExpiredEvents() method")
    class CompleteExpiredEventsMethod {

        @Test
        @DisplayName("should complete ACTIVE events with past dates")
        void shouldCompleteActiveEventsWithPastDates() {
            // Arrange
            LocalDate today = LocalDate.of(2025, 2, 1);
            LocalDate pastDate = LocalDate.of(2025, 1, 15);

            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 1").eventDate(pastDate).location("Location 1").organizer("Organizer 1").build());
            event1.publish(); // Make ACTIVE

            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 2").eventDate(pastDate).location("Location 2").organizer("Organizer 2").build());
            event2.publish(); // Make ACTIVE

            List<Event> activeEventsWithPastDate = List.of(event1, event2);

            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(activeEventsWithPastDate));

            // Act
            scheduler.completeExpiredEvents(today);

            // Assert
            verify(eventRepository).findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged());

            // Verify each event was transitioned to FINISHED
            assertThat(event1.getStatus()).isEqualTo(EventStatus.FINISHED);
            assertThat(event2.getStatus()).isEqualTo(EventStatus.FINISHED);

            // Verify each event was saved
            verify(eventRepository, times(2)).save(any(Event.class));
            verify(eventRepository).save(event1);
            verify(eventRepository).save(event2);
        }

        @Test
        @DisplayName("should not affect DRAFT events with past dates")
        void shouldNotAffectDraftEventsWithPastDates() {
            // Arrange
            LocalDate today = LocalDate.of(2025, 2, 1);

            // Filter returns only ACTIVE events — DRAFT events are not included by definition
            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // Act
            scheduler.completeExpiredEvents(today);

            // Assert
            verify(eventRepository).findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged());
            // No saves should occur since no ACTIVE events were found
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should handle no events to complete gracefully")
        void shouldHandleNoEventsToCompleteGracefully() {
            // Arrange
            LocalDate today = LocalDate.of(2025, 2, 1);

            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // Act
            scheduler.completeExpiredEvents(today);

            // Assert
            verify(eventRepository).findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged());
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should be idempotent - running twice produces same result")
        void shouldBeIdempotentRunningTwiceProducesSameResult() {
            // Arrange
            LocalDate today = LocalDate.of(2025, 2, 1);
            LocalDate pastDate = LocalDate.of(2025, 1, 15);

            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(pastDate).location("Location").organizer("Organizer").build());
            event.publish(); // Make ACTIVE

            // First run - returns the event; second run - no events
            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of(event)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // Act - First run
            scheduler.completeExpiredEvents(today);

            // Assert - Event is FINISHED
            assertThat(event.getStatus()).isEqualTo(EventStatus.FINISHED);
            verify(eventRepository).save(event);

            // Reset mock to clear invocation counts
            reset(eventRepository);
            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(Collections.emptyList())); // No events since all are already FINISHED

            // Act - Second run
            scheduler.completeExpiredEvents(today);

            // Assert - No additional saves (idempotent)
            verify(eventRepository).findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged());
            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should continue processing if individual event save fails")
        void shouldContinueProcessingIfIndividualEventSaveFails() {
            // Arrange
            LocalDate today = LocalDate.of(2025, 2, 1);
            LocalDate pastDate = LocalDate.of(2025, 1, 15);

            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 1").eventDate(pastDate).location("Location 1").organizer("Organizer 1").build());
            event1.publish();

            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 2").eventDate(pastDate).location("Location 2").organizer("Organizer 2").build());
            event2.publish();

            Event event3 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 3").eventDate(pastDate).location("Location 3").organizer("Organizer 3").build());
            event3.publish();

            List<Event> events = List.of(event1, event2, event3);

            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(events));

            // Simulate failure when saving event2
            when(eventRepository.save(event1)).thenReturn(event1);
            when(eventRepository.save(event2)).thenThrow(new RuntimeException("Database error"));
            when(eventRepository.save(event3)).thenReturn(event3);

            // Act
            scheduler.completeExpiredEvents(today);

            // Assert
            // All events should have been transitioned to FINISHED
            assertThat(event1.getStatus()).isEqualTo(EventStatus.FINISHED);
            assertThat(event2.getStatus()).isEqualTo(EventStatus.FINISHED);
            assertThat(event3.getStatus()).isEqualTo(EventStatus.FINISHED);

            // All events should have been attempted to save (3 times)
            verify(eventRepository, times(3)).save(any(Event.class));
            verify(eventRepository).save(event1);
            verify(eventRepository).save(event2);
            verify(eventRepository).save(event3);
        }

        @Test
        @DisplayName("should use current date when called without parameters")
        void shouldUseCurrentDateWhenCalledWithoutParameters() {
            // Arrange
            Event event = Event.create(EventCreateEventBuilder.builder().name("Event").eventDate(LocalDate.of(2025, 1, 15)).location("Location").organizer("Organizer").build());
            event.publish();

            when(eventRepository.findAll(any(EventFilter.class), eq(Pageable.unpaged())))
                    .thenReturn(new PageImpl<>(List.of(event)));

            // Act
            scheduler.completeExpiredEvents();

            // Assert
            verify(eventRepository).findAll(any(EventFilter.class), eq(Pageable.unpaged()));
            verify(eventRepository).save(event);
            assertThat(event.getStatus()).isEqualTo(EventStatus.FINISHED);
        }

        @Test
        @DisplayName("should handle repository query failure gracefully")
        void shouldHandleRepositoryQueryFailureGracefully() {
            // Arrange
            LocalDate today = LocalDate.of(2025, 2, 1);

            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act - should not throw exception
            scheduler.completeExpiredEvents(today);

            // Assert
            verify(eventRepository).findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged());
            // No save should be attempted since query failed
            verify(eventRepository, never()).save(any(Event.class));
        }
    }
}
