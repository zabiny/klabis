package com.klabis.events.registration;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.events.Event;
import com.klabis.events.EventId;
import com.klabis.events.SiCardNumber;
import com.klabis.events.persistence.EventRepository;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventRegistrationService.
 * <p>
 * Tests event registration business logic including:
 * - Member registration for events
 * - Duplicate registration handling
 * - Status-based registration constraints
 * - Unregistration before event date
 * - Privacy enforcement (SI card visibility)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventRegistrationService Unit Tests")
class EventRegistrationServiceTest {

    private static final UserId TEST_USER_ID = UserId.newId();
    private static final MemberId TEST_MEMBER_ID = MemberId.fromUserId(TEST_USER_ID);

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Members members;

    private EventRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new EventRegistrationService(eventRepository, members);
    }

    @Nested
    @DisplayName("registerMember() method")
    class RegisterMemberMethod {

        private UUID eventId;
        private Event activeEvent;

        @BeforeEach
        void setUp() {
            eventId = UUID.randomUUID();

            activeEvent = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 15),
                    "Test Location",
                    "OOB",
                    null,
                    null
            );
            activeEvent.publish(); // Make it ACTIVE
        }

        @Test
        @DisplayName("should register member with valid SI card number for ACTIVE event")
        void shouldRegisterMemberWithValidSiCard() {
            // Given
            RegisterForEventCommand command = new RegisterForEventCommand("123456");
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(activeEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(activeEvent);

            // When
            service.registerMember(eventId, TEST_MEMBER_ID, command);

            // Then
            verify(eventRepository).save(any(Event.class));
            assertThat(activeEvent.getRegistrations()).hasSize(1);
            assertThat(activeEvent.getRegistrations().get(0).siCardNumber().value()).isEqualTo("123456");
        }

        @Test
        @DisplayName("should throw exception when member already registered (duplicate registration)")
        void shouldRejectDuplicateRegistration() {
            // Given
            RegisterForEventCommand command = new RegisterForEventCommand("123456");
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(activeEvent));

            // Register member first time
            activeEvent.registerMember(TEST_MEMBER_ID, SiCardNumber.of("123456"));

            // When/Then - second registration should fail
            assertThatThrownBy(() -> service.registerMember(eventId, TEST_MEMBER_ID, command))
                    .isInstanceOf(DuplicateRegistrationException.class)
                    .hasMessageContaining("already registered");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject registration for non-ACTIVE event")
        void shouldRejectRegistrationForNonActiveEvent() {
            // Given
            Event draftEvent = Event.create(
                    "Draft Event",
                    LocalDate.of(2026, 7, 1),
                    "Location",
                    "PRG",
                    null,
                    null
            );
            // Event is in DRAFT status

            RegisterForEventCommand command = new RegisterForEventCommand("123456");
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(draftEvent));

            // When/Then
            assertThatThrownBy(() -> service.registerMember(eventId, TEST_MEMBER_ID, command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("only allowed for ACTIVE events");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should throw exception when event not found")
        void shouldThrowExceptionWhenEventNotFound() {
            // Given
            RegisterForEventCommand command = new RegisterForEventCommand("123456");
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.registerMember(eventId, TEST_MEMBER_ID, command))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("unregisterMember() method")
    class UnregisterMemberMethod {

        private UUID eventId;
        private Event activeEvent;

        @BeforeEach
        void setUp() {
            eventId = UUID.randomUUID();

            activeEvent = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 15),
                    "Test Location",
                    "OOB",
                    null,
                    null
            );
            activeEvent.publish();
            activeEvent.registerMember(TEST_MEMBER_ID, SiCardNumber.of("123456"));
        }

        @Test
        @DisplayName("should unregister member before event date")
        void shouldUnregisterMemberBeforeEventDate() {
            // Given
            LocalDate currentDate = LocalDate.of(2026, 6, 10); // Before event date
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(activeEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(activeEvent);

            // When
            service.unregisterMember(eventId, TEST_MEMBER_ID,currentDate);

            // Then
            verify(eventRepository).save(any(Event.class));
            assertThat(activeEvent.getRegistrations()).isEmpty();
        }

        @Test
        @DisplayName("should reject unregistration on event date")
        void shouldRejectUnregistrationOnEventDate() {
            // Given
            LocalDate eventDate = LocalDate.of(2026, 6, 15); // Same as event date
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(activeEvent));

            // When/Then
            assertThatThrownBy(() -> service.unregisterMember(eventId, TEST_MEMBER_ID,eventDate))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot unregister on or after event date");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject unregistration after event date")
        void shouldRejectUnregistrationAfterEventDate() {
            // Given
            LocalDate afterEventDate = LocalDate.of(2026, 6, 20); // After event date
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(activeEvent));

            // When/Then
            assertThatThrownBy(() -> service.unregisterMember(eventId, TEST_MEMBER_ID,afterEventDate))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot unregister on or after event date");

            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("listRegistrations() method")
    class ListRegistrationsMethod {

        @Test
        @DisplayName("should return list without SI card numbers for privacy")
        void shouldReturnListWithoutSiCardNumbers() {
            // Given
            UUID eventId = UUID.randomUUID();
            UUID member1Id = UUID.randomUUID();
            UUID member2Id = UUID.randomUUID();

            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 15),
                    "Test Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();
            event.registerMember(new MemberId(member1Id), SiCardNumber.of("111111"));
            event.registerMember(new MemberId(member2Id), SiCardNumber.of("222222"));

            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(event));

            MemberDto member1 = new MemberDto(member1Id, "John", "Doe", "doe@email.com");
            MemberDto member2 = new MemberDto(member2Id, "Jane", "Smith", "smith@email.com");

            when(members.findById(new MemberId(member1Id))).thenReturn(Optional.of(member1));
            when(members.findById(new MemberId(member2Id))).thenReturn(Optional.of(member2));

            // When
            List<RegistrationDto> registrations = service.listRegistrations(eventId);

            // Then
            assertThat(registrations).hasSize(2);
            assertThat(registrations.get(0).firstName()).isEqualTo("John");
            assertThat(registrations.get(0).lastName()).isEqualTo("Doe");
            // SI card number should NOT be present in RegistrationDto
            assertThat(registrations.get(1).firstName()).isEqualTo("Jane");
            assertThat(registrations.get(1).lastName()).isEqualTo("Smith");
        }

        @Test
        @DisplayName("should return empty list when no registrations")
        void shouldReturnEmptyListWhenNoRegistrations() {
            // Given
            UUID eventId = UUID.randomUUID();
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 15),
                    "Test Location",
                    "OOB",
                    null,
                    null
            );
            event.publish();

            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(event));

            // When
            List<RegistrationDto> registrations = service.listRegistrations(eventId);

            // Then
            assertThat(registrations).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOwnRegistration() method")
    class GetOwnRegistrationMethod {

        private UUID eventId;
        private Event activeEvent;

        @BeforeEach
        void setUp() {
            eventId = UUID.randomUUID();

            activeEvent = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 15),
                    "Test Location",
                    "OOB",
                    null,
                    null
            );
            activeEvent.publish();
            activeEvent.registerMember(TEST_MEMBER_ID, SiCardNumber.of("123456"));
        }

        @Test
        @DisplayName("should return full registration details including SI card for own registration")
        void shouldReturnOwnRegistrationWithSiCard() {
            // Given
            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(activeEvent));

            MemberDto member = new MemberDto(TEST_USER_ID.uuid(), "John", "Doe", "doe@email.com");
            when(members.findById(TEST_MEMBER_ID)).thenReturn(Optional.of(member));

            // When
            OwnRegistrationDto registration = service.getOwnRegistration(eventId, TEST_MEMBER_ID);

            // Then
            assertThat(registration).isNotNull();
            assertThat(registration.firstName()).isEqualTo("John");
            assertThat(registration.lastName()).isEqualTo("Doe");
            assertThat(registration.siCardNumber()).isEqualTo("123456"); // SI card IS visible
        }

        @Test
        @DisplayName("should throw exception when not registered")
        void shouldThrowExceptionWhenNotRegistered() {
            // Given
            Event eventWithoutRegistration = Event.create(
                    "Test Event",
                    LocalDate.of(2026, 6, 15),
                    "Test Location",
                    "OOB",
                    null,
                    null
            );
            eventWithoutRegistration.publish();

            when(eventRepository.findById(new EventId(eventId))).thenReturn(Optional.of(eventWithoutRegistration));

            // When/Then
            assertThatThrownBy(() -> service.getOwnRegistration(eventId, TEST_MEMBER_ID))
                    .isInstanceOf(RegistrationNotFoundException.class)
                    .hasMessageContaining("not registered");
        }
    }
}
