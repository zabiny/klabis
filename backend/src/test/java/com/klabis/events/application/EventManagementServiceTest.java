package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;
import com.klabis.events.domain.*;
import com.klabis.members.MemberId;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EventManagementPort.
 * <p>
 * Tests event management business logic including creation, updates,
 * status transitions, and querying with proper validation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventManagementPort Unit Tests")
class EventManagementServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventManagementPort service;

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
            MemberId coordinatorId = new MemberId(UUID.randomUUID());
            Event.CreateEvent command = EventCreateEventBuilder.builder()
                    .name("Spring Cup 2026")
                    .eventDate(LocalDate.of(2026, 3, 15))
                    .location("Forest Park")
                    .organizer("OOB")
                    .websiteUrl("https://example.com/spring-cup")
                    .eventCoordinatorId(coordinatorId)
                    .build();

            Event event = Event.create(command);
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            Event result = service.createEvent(command);

            // Then
            assertThat(result).isEqualTo(event);
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should create event without optional fields")
        void shouldCreateEventWithoutOptionalFields() {
            // Given
            Event.CreateEvent command = EventCreateEventBuilder.builder()
                    .name("Autumn Race 2026")
                    .eventDate(LocalDate.of(2026, 10, 12))
                    .location("City Park")
                    .organizer("PRG")
                    .build();

            Event event = Event.create(command);
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            Event result = service.createEvent(command);

            // Then
            assertThat(result).isEqualTo(event);
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
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Old Name").eventDate(LocalDate.of(2026, 5, 1))
                    .location("Old Location").organizer("OOB").build());

            Event.UpdateEvent command = EventUpdateEventBuilder.builder()
                    .name("Updated Name").eventDate(LocalDate.of(2026, 5, 15))
                    .location("Updated Location").organizer("PRG")
                    .websiteUrl("https://updated.com").build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.updateEvent(eventId, command);

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should update event in ACTIVE status")
        void shouldUpdateEventInActiveStatus() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            event.publish();

            Event.UpdateEvent command = EventUpdateEventBuilder.builder()
                    .name("Updated Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("New Location").organizer("PRG").build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.updateEvent(eventId, command);

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject update for FINISHED event")
        void shouldRejectUpdateForFinishedEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            event.publish();
            event.finish();

            Event.UpdateEvent command = EventUpdateEventBuilder.builder()
                    .name("Updated Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("New Location").organizer("PRG").build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When & Then
            assertThatThrownBy(() -> service.updateEvent(eventId, command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("FINISHED");
        }

        @Test
        @DisplayName("should reject update for CANCELLED event")
        void shouldRejectUpdateForCancelledEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            event.cancel();

            Event.UpdateEvent command = EventUpdateEventBuilder.builder()
                    .name("Updated Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("New Location").organizer("PRG").build();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When & Then
            assertThatThrownBy(() -> service.updateEvent(eventId, command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            Event.UpdateEvent command = EventUpdateEventBuilder.builder()
                    .name("Updated Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("New Location").organizer("PRG").build();

            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.updateEvent(new EventId(eventId), command))
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
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.publishEvent(eventId);

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
            assertThatThrownBy(() -> service.publishEvent(new EventId(eventId)))
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
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.cancelEvent(eventId);

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should cancel event in ACTIVE status")
        void shouldCancelActiveEvent() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            event.publish();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            // When
            service.cancelEvent(eventId);

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
            assertThatThrownBy(() -> service.cancelEvent(new EventId(eventId)))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getEvent() method")
    class GetEventMethod {

        @Test
        @DisplayName("should return event details when event exists and caller can manage events")
        void shouldReturnEventDetails() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Forest Park").organizer("OOB")
                    .websiteUrl("https://example.com")
                    .eventCoordinatorId(new MemberId(UUID.randomUUID()))
                    .build());

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When
            Event result = service.getEvent(eventId, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("Test Event");
            assertThat(result.getLocation()).isEqualTo("Forest Park");
            assertThat(result.getOrganizer()).isEqualTo("OOB");
            assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event does not exist")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(eventRepository.findById(any(EventId.class))).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> service.getEvent(new EventId(eventId), true))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should hide DRAFT event from non-manager")
        void shouldThrowEventNotFoundForDraftEventWhenCannotManage() {
            // Given
            EventId eventId = EventId.generate();
            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

            // When & Then
            assertThatThrownBy(() -> service.getEvent(eventId, false))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should return ACTIVE event to non-manager")
        void shouldReturnActiveEventForNonManager() {
            // Given
            EventId eventId = EventId.generate();
            Event activeEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            activeEvent.publish();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(activeEvent));

            // When
            Event result = service.getEvent(eventId, false);

            // Then
            assertThat(result.getStatus()).isEqualTo(EventStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("listEvents() method")
    class ListEventsMethod {

        @Test
        @DisplayName("manager sees all events including DRAFT when filter is none()")
        void shouldReturnPaginatedEventsForManager() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 1").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location 1").organizer("OOB").build());
            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 2").eventDate(LocalDate.of(2026, 7, 1))
                    .location("Location 2").organizer("PRG").build());

            Page<Event> eventPage = new PageImpl<>(List.of(event1, event2), pageable, 2);
            when(eventRepository.findAll(EventFilter.none(), pageable)).thenReturn(eventPage);

            // When
            Page<Event> result = service.listEvents(EventFilter.none(), pageable, true);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Event 1");
            assertThat(result.getContent().get(1).getName()).isEqualTo("Event 2");
        }

        @Test
        @DisplayName("non-manager sees events excluding DRAFT when filter is none()")
        void shouldExcludeDraftEventsForNonManager() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Event activeEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            activeEvent.publish();

            EventFilter expectedFilter = EventFilter.byNotHavingStatus(EventStatus.DRAFT);
            Page<Event> eventPage = new PageImpl<>(List.of(activeEvent), pageable, 1);
            when(eventRepository.findAll(expectedFilter, pageable)).thenReturn(eventPage);

            // When
            Page<Event> result = service.listEvents(EventFilter.none(), pageable, false);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("non-manager requesting DRAFT status explicitly gets empty page")
        void shouldReturnEmptyPageWhenNonManagerRequestsDraftOnly() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Event> result = service.listEvents(EventFilter.byStatus(EventStatus.DRAFT), pageable, false);

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("manager requesting DRAFT status gets DRAFT events")
        void shouldReturnDraftEventsForManagerRequestingDraft() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());

            Page<Event> eventPage = new PageImpl<>(List.of(draftEvent), pageable, 1);
            when(eventRepository.findAll(EventFilter.byStatus(EventStatus.DRAFT), pageable)).thenReturn(eventPage);

            // When
            Page<Event> result = service.listEvents(EventFilter.byStatus(EventStatus.DRAFT), pageable, true);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should filter events by ACTIVE status for both manager and non-manager")
        void shouldFilterEventsByActiveStatus() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Active Event").eventDate(LocalDate.of(2026, 6, 1))
                    .location("Location").organizer("OOB").build());
            event.publish();

            Page<Event> eventPage = new PageImpl<>(List.of(event), pageable, 1);
            when(eventRepository.findAll(EventFilter.byStatus(EventStatus.ACTIVE), pageable)).thenReturn(eventPage);

            // When
            Page<Event> result = service.listEvents(EventFilter.byStatus(EventStatus.ACTIVE), pageable, false);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(EventStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("finishExpiredActiveEvents() method")
    class FinishExpiredActiveEventsMethod {

        @Test
        @DisplayName("should finish all ACTIVE events with past dates and save each one")
        void shouldFinishActiveEventsWithPastDates() {
            // Given
            LocalDate today = LocalDate.of(2025, 2, 1);
            LocalDate pastDate = LocalDate.of(2025, 1, 15);

            Event event1 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 1").eventDate(pastDate).location("Location 1").organizer("OOB").build());
            event1.publish();

            Event event2 = Event.create(EventCreateEventBuilder.builder()
                    .name("Event 2").eventDate(pastDate).location("Location 2").organizer("PRG").build());
            event2.publish();

            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(event1, event2)));
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            service.finishExpiredActiveEvents(today);

            // Then
            assertThat(event1.getStatus()).isEqualTo(EventStatus.FINISHED);
            assertThat(event2.getStatus()).isEqualTo(EventStatus.FINISHED);
            verify(eventRepository).save(event1);
            verify(eventRepository).save(event2);
        }

        @Test
        @DisplayName("should not finish any event when no ACTIVE events with past dates exist")
        void shouldNotFinishAnyEventWhenNoneExpired() {
            // Given
            LocalDate today = LocalDate.of(2025, 2, 1);
            when(eventRepository.findAll(EventFilter.activeEventsWithDateBefore(today), Pageable.unpaged())).thenReturn(new PageImpl<>(List.of()));

            // When
            service.finishExpiredActiveEvents(today);

            // Then
            verify(eventRepository, never()).save(any(Event.class));
        }
    }

}
