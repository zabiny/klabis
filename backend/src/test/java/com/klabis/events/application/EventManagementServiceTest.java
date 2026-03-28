package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.EventId;
import com.klabis.members.MemberId;
import com.klabis.events.WebsiteUrl;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventFilter;
import com.klabis.events.domain.EventRepository;
import com.klabis.events.domain.EventStatus;
import com.klabis.oris.apiclient.OrisApiClient;
import com.klabis.oris.apiclient.dto.EventDetails;
import com.klabis.oris.apiclient.dto.Organizer;
import org.mockito.Mockito;
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

    @Mock
    private OrisApiClient orisApiClient;

    private EventManagementService service;

    @BeforeEach
    void setUp() {
        service = new EventManagementServiceImpl(eventRepository, Optional.of(orisApiClient));
    }

    @Nested
    @DisplayName("createEvent() method")
    class CreateEventMethod {

        @Test
        @DisplayName("should create event with valid command and return EventId")
        void shouldCreateEventWithValidCommand() {
            // Given
            MemberId coordinatorId = new MemberId(UUID.randomUUID());
            Event.EventCommand command = new Event.EventCommand(
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
                    command.eventCoordinatorId()
            );

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
            Event.EventCommand command = new Event.EventCommand(
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
            Event event = Event.create(
                    "Old Name",
                    LocalDate.of(2026, 5, 1),
                    "Old Location",
                    "OOB",
                    null,
                    null
            );

            Event.EventCommand command = new Event.EventCommand(
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
            service.updateEvent(eventId, command);

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

            Event.EventCommand command = new Event.EventCommand(
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
            service.updateEvent(eventId, command);

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

            Event.EventCommand command = new Event.EventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

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
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 1),
                    "Location",
                    "OOB",
                    null,
                    null
            );
            event.cancel();  // Transition to CANCELLED

            Event.EventCommand command = new Event.EventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

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
            Event.EventCommand command = new Event.EventCommand(
                    "Updated Event",
                    LocalDate.of(2026, 6, 15),
                    "New Location",
                    "PRG",
                    null,
                    null
            );

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
            service.cancelEvent(eventId);

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
            service.finishEvent(eventId);

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
            assertThatThrownBy(() -> service.finishEvent(new EventId(eventId)))
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
                    new MemberId(UUID.randomUUID())
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When
            Event result = service.getEvent(eventId);

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
            assertThatThrownBy(() -> service.getEvent(new EventId(eventId)))
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
            when(eventRepository.findAll(EventFilter.none(), pageable)).thenReturn(eventPage);

            // When
            Page<Event> result = service.listEvents(EventFilter.none(), pageable);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Event 1");
            assertThat(result.getContent().get(1).getName()).isEqualTo("Event 2");
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
            when(eventRepository.findAll(EventFilter.byStatus(EventStatus.ACTIVE), pageable)).thenReturn(eventPage);

            // When
            Page<Event> result = service.listEvents(EventFilter.byStatus(EventStatus.ACTIVE), pageable);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(EventStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("importEventFromOris() method")
    class ImportEventFromOrisMethod {

        @Test
        @DisplayName("should import event successfully from ORIS")
        void shouldImportEventSuccessfully() {
            // Given
            int orisId = 9876;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "Spring Sprint", LocalDate.of(2026, 8, 15), "Brno Park", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisApiClient.getEventWebUrl(orisId)).thenCallRealMethod();
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Event result = service.importEventFromOris(orisId);

            // Then
            assertThat(result.getName()).isEqualTo("Spring Sprint");
            assertThat(result.getEventDate()).isEqualTo(LocalDate.of(2026, 8, 15));
            assertThat(result.getLocation()).isEqualTo("Brno Park");
            assertThat(result.getOrganizer()).isEqualTo("OOB");
            assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
            assertThat(result.getOrisId()).isEqualTo(orisId);
            assertThat(result.getWebsiteUrl().value()).isEqualTo("https://oris.ceskyorientak.cz/Zavod?id=9876");
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw DuplicateOrisImportException when DB unique constraint violated")
        void shouldThrowDuplicateExceptionOnConstraintViolation() {
            // Given
            int orisId = 1111;
            Organizer org1 = new Organizer(205, "OOB", "Orel Brno");
            EventDetails details = buildEventDetails(orisId, "Duplicate Event", LocalDate.of(2026, 8, 15), "Location", org1, null);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisApiClient.getEventWebUrl(orisId)).thenCallRealMethod();
            when(eventRepository.save(any(Event.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate oris_id"));

            // When & Then
            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(DuplicateOrisImportException.class);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when ORIS returns no data for the given ID")
        void shouldThrowEventNotFoundWhenOrisReturnsNoData() {
            // Given
            int orisId = 4444;
            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(null, "JSON", "OK", null, "getEvent"));

            // When & Then
            assertThatThrownBy(() -> service.importEventFromOris(orisId))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("should use org2 abbreviation as fallback when org1 is blank")
        void shouldUseOrg2AbbreviationWhenOrg1IsBlank() {
            // Given
            int orisId = 2222;
            Organizer org1 = new Organizer(0, "", "");
            Organizer org2 = new Organizer(206, "PRG", "Prague OC");
            EventDetails details = buildEventDetails(orisId, "Prague Event", LocalDate.of(2026, 9, 1), "Prague", org1, org2);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisApiClient.getEventWebUrl(orisId)).thenCallRealMethod();
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Event result = service.importEventFromOris(orisId);

            // Then
            assertThat(result.getOrganizer()).isEqualTo("PRG");
        }

        @Test
        @DisplayName("should use fallback '---' when both org1 and org2 are blank")
        void shouldUseFallbackWhenBothOrganizersAreBlank() {
            // Given
            int orisId = 3333;
            Organizer org1 = new Organizer(0, null, "");
            Organizer org2 = new Organizer(0, "  ", "");
            EventDetails details = buildEventDetails(orisId, "No Org Event", LocalDate.of(2026, 10, 1), "Unknown", org1, org2);

            when(orisApiClient.getEventDetails(orisId)).thenReturn(
                    new OrisApiClient.OrisResponse<>(details, "JSON", "OK", null, "getEvent"));
            when(orisApiClient.getEventWebUrl(orisId)).thenCallRealMethod();
            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            Event result = service.importEventFromOris(orisId);

            // Then
            assertThat(result.getOrganizer()).isEqualTo("---");
        }

        @Test
        @DisplayName("should throw IllegalStateException when ORIS integration is not active")
        void shouldThrowWhenOrisNotActive() {
            // Given
            EventManagementService serviceWithoutOris = new EventManagementServiceImpl(eventRepository, Optional.empty());

            // When & Then
            assertThatThrownBy(() -> serviceWithoutOris.importEventFromOris(9876))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ORIS integration is not active");
        }

        private EventDetails buildEventDetails(int id, String name, LocalDate date, String place, Organizer org1, Organizer org2) {
            EventDetails details = Mockito.mock(EventDetails.class);
            Mockito.when(details.name()).thenReturn(name);
            Mockito.when(details.date()).thenReturn(date);
            Mockito.when(details.place()).thenReturn(place);
            Mockito.when(details.org1()).thenReturn(org1);
            Mockito.lenient().when(details.org2()).thenReturn(org2);
            return details;
        }
    }
}
