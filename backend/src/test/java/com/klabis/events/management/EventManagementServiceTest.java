package com.klabis.events.management;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.EventStatus;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.persistence.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventManagementService.
 * <p>
 * Tests event management business logic including creation, updates,
 * status transitions, and querying with proper validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventManagementService Unit Tests")
class EventManagementServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventManagementService service;

    @BeforeEach
    void setUp() {
        service = new EventManagementService(eventRepository);
    }

    @Nested
    @DisplayName("createEvent() method")
    class CreateEventMethod {

        @Test
        @DisplayName("should create event with valid command and return EventId")
        void shouldCreateEventWithValidCommand() {
            // Given
            UUID coordinatorId = UUID.randomUUID();
            CreateEventCommand command = new CreateEventCommand(
                    "Spring Cup 2026",
                    LocalDate.of(2026, 3, 15),
                    "Forest Park",
                    "OOB",
                    "https://example.com/spring-cup",
                    coordinatorId
            );

            Event event = Event.create(
                    command.name(),
                    command.eventDate(),
                    command.location(),
                    command.organizer(),
                    command.websiteUrl() != null ? WebsiteUrl.of(command.websiteUrl()) : null,
                    command.eventCoordinatorId() != null ? new UserId(command.eventCoordinatorId()) : null
            );

            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            UUID eventId = service.createEvent(command);

            // Then
            assertThat(eventId).isNotNull();
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should create event without optional fields")
        void shouldCreateEventWithoutOptionalFields() {
            // Given
            CreateEventCommand command = new CreateEventCommand(
                    "Autumn Race 2026",
                    LocalDate.of(2026, 10, 12),
                    "City Park",
                    "PRG",
                    null,  // no website
                    null   // no coordinator
            );

            Event event = Event.create(
                    command.name(),
                    command.eventDate(),
                    command.location(),
                    command.organizer(),
                    null,
                    null
            );

            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            UUID eventId = service.createEvent(command);

            // Then
            assertThat(eventId).isNotNull();
            verify(eventRepository).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("updateEvent() method")
    class UpdateEventMethod {

        @Test
        @DisplayName("should update event in DRAFT status")
        void shouldUpdateEventInDraftStatus() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Old Name",
                    LocalDate.of(2026, 5, 1),
                    "Old Location",
                    "OOB",
                    null,
                    null
            );

            UpdateEventCommand command = new UpdateEventCommand(
                    "Updated Name",
                    LocalDate.of(2026, 5, 15),
                    "Updated Location",
                    "PRG",
                    "https://updated.com",
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.updateEvent(eventId.value(), command);

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should update event in ACTIVE status")
        void shouldUpdateEventInActiveStatus() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();  // Transition to ACTIVE

            UpdateEventCommand command = new UpdateEventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.updateEvent(eventId.value(), command);

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject update for FINISHED event")
        void shouldRejectUpdateForFinishedEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();
            event.finish();  // Transition to FINISHED

            UpdateEventCommand command = new UpdateEventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When & Then
            assertThatThrownBy(() -> service.updateEvent(eventId.value(), command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("FINISHED");
        }

        @Test
        @DisplayName("should reject update for CANCELLED event")
        void shouldRejectUpdateForCancelledEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.cancel();  // Transition to CANCELLED

            UpdateEventCommand command = new UpdateEventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When & Then
            assertThatThrownBy(() -> service.updateEvent(eventId.value(), command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            UpdateEventCommand command = new UpdateEventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateEvent(eventId, command))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("publishEvent() method")
    class PublishEventMethod {

        @Test
        @DisplayName("should publish event in DRAFT status")
        void shouldPublishDraftEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.publishEvent(eventId.value());

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.publishEvent(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelEvent() method")
    class CancelEventMethod {

        @Test
        @DisplayName("should cancel event in DRAFT status")
        void shouldCancelDraftEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.cancelEvent(eventId.value());

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should cancel event in ACTIVE status")
        void shouldCancelActiveEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.cancelEvent(eventId.value());

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.cancelEvent(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("finishEvent() method")
    class FinishEventMethod {

        @Test
        @DisplayName("should finish event in ACTIVE status")
        void shouldFinishActiveEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.finishEvent(eventId.value());

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.finishEvent(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getEvent() method")
    class GetEventMethod {

        @Test
        @DisplayName("should return event details when event exists")
        void shouldReturnEventDetails() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Forest Park",
                    "OOB",
                    WebsiteUrl.of("https://example.com"),
                    new UserId(UUID.randomUUID())
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When
            EventDto result = service.getEvent(eventId.value());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("Test Event");
            assertThat(result.location()).isEqualTo("Forest Park");
            assertThat(result.organizer()).isEqualTo("OOB");
            assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getEvent(eventId))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listEvents() method")
    class ListEventsMethod {

        @Test
        @DisplayName("should return paginated list of events")
        void shouldReturnPaginatedEvents() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Event event1 = Event.create(
                    "Event 1",
                    LocalDate.of(2026, 6, 1),
                    "Location 1",
                    "OOB",
                    null,
                    null
            );
            Event event2 = Event.create(
                    "Event 2",
                    LocalDate.of(2026, 7, 1),
                    "Location 2",
                    "PRG",
                    null,
                    null
            );

            Page<Event> eventPage = new PageImpl<>(List.of(event1, event2), pageable, 2);
            when(eventRepository.findAll(pageable)).thenReturn(eventPage);

            // When
            Page<EventSummaryDto> result = service.listEvents(pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).name()).isEqualTo("Event 1");
            assertThat(result.getContent().get(1).name()).isEqualTo("Event 2");
        }

        @Test
        @DisplayName("should filter events by status")
        void shouldFilterEventsByStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Event event = Event.create(
                    "Active Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();

            Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);
            when(eventRepository.findByStatus(EventStatus.ACTIVE, pageable)).thenReturn(eventPage);

            // When
            Page<EventSummaryDto> result = service.listEventsByStatus(EventStatus.ACTIVE, pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(EventStatus.ACTIVE);
        }
    }
}
