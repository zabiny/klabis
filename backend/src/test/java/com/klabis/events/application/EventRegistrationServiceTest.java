package com.klabis.events.application;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventRegistrationPort.
 * <p>
 * Tests event registration business logic including:
 * - Member registration for events
 * - Duplicate registration handling
 * - Status-based registration constraints
 * - Unregistration before event date
 * - Privacy enforcement (SI card visibility)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventRegistrationPort Unit Tests")
class EventRegistrationServiceTest {

    private static final UserId TEST_USER_ID = UserId.newId();
    private static final MemberId TEST_MEMBER_ID = MemberId.fromUserId(TEST_USER_ID);

    @Mock
    private EventRepository eventRepository;

    private EventRegistrationPort service;

    @BeforeEach
    void setUp() {
        service = new EventRegistrationService(eventRepository);
    }

    @Nested
    @DisplayName("registerMember() method")
    class RegisterMemberMethod {

        private EventId eventId;
        private Event activeEvent;

        @BeforeEach
        void setUp() {
            eventId = EventId.generate();

            activeEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("Test Location").organizer("OOB").build());
            activeEvent.publish(); // Make it ACTIVE
        }

        @Test
        @DisplayName("should register member with valid SI card number for ACTIVE event")
        void shouldRegisterMemberWithValidSiCard() {
            // Given
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(activeEvent));
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
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(activeEvent));

            // Register member first time
            activeEvent.registerMember(TEST_MEMBER_ID, SiCardNumber.of("123456"), null);

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
            Event draftEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Draft Event").eventDate(LocalDate.of(2026, 7, 1))
                    .location("Location").organizer("PRG").build());
            // Event is in DRAFT status

            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(draftEvent));

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
            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.registerMember(eventId, TEST_MEMBER_ID, command))
                    .isInstanceOf(EventNotFoundException.class);

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject registration when registration deadline has passed")
        void shouldRejectRegistrationWhenDeadlinePassed() {
            // Given
            Event eventWithPastDeadline = Event.create(EventCreateEventBuilder.builder()
                    .name("Deadline Event").eventDate(LocalDate.of(2026, 9, 15))
                    .location("Test Location").organizer("OOB")
                    .registrationDeadline(LocalDate.of(2026, 3, 1)).build());
            eventWithPastDeadline.publish();

            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithPastDeadline));

            // When/Then
            assertThatThrownBy(() -> service.registerMember(eventId, TEST_MEMBER_ID, command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration deadline has passed");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should allow registration when registration deadline is in the future")
        void shouldAllowRegistrationWhenDeadlineInFuture() {
            // Given
            Event eventWithFutureDeadline = Event.create(EventCreateEventBuilder.builder()
                    .name("Future Deadline Event").eventDate(LocalDate.now().plusDays(60))
                    .location("Test Location").organizer("OOB")
                    .registrationDeadline(LocalDate.now().plusDays(30)).build());
            eventWithFutureDeadline.publish();

            Event.RegisterCommand command = EventRegisterCommandBuilder.builder().siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithFutureDeadline));
            when(eventRepository.save(any(Event.class))).thenReturn(eventWithFutureDeadline);

            // When
            service.registerMember(eventId, TEST_MEMBER_ID, command);

            // Then
            verify(eventRepository).save(any(Event.class));
        }

        @Test
        @DisplayName("should register member with valid category when event has categories")
        void shouldRegisterMemberWithValidCategory() {
            // Given
            Event eventWithCategories = Event.create(EventCreateEventBuilder.builder()
                    .name("Category Event").eventDate(LocalDate.now().plusDays(30))
                    .location("Test Location").organizer("OOB")
                    .categories(List.of("M21", "W35"))
                    .build());
            eventWithCategories.publish();

            Event.RegisterCommand command = EventRegisterCommandBuilder.builder()
                    .siCardNumber("123456").category("M21").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithCategories));
            when(eventRepository.save(any(Event.class))).thenReturn(eventWithCategories);

            // When
            service.registerMember(eventId, TEST_MEMBER_ID, command);

            // Then
            verify(eventRepository).save(any(Event.class));
            assertThat(eventWithCategories.findRegistration(TEST_MEMBER_ID)).isPresent();
            assertThat(eventWithCategories.findRegistration(TEST_MEMBER_ID).get().category()).isEqualTo("M21");
        }

        @Test
        @DisplayName("should throw when category required but not provided")
        void shouldThrowWhenCategoryRequiredButNotProvided() {
            // Given
            Event eventWithCategories = Event.create(EventCreateEventBuilder.builder()
                    .name("Category Event").eventDate(LocalDate.now().plusDays(30))
                    .location("Test Location").organizer("OOB")
                    .categories(List.of("M21", "W35"))
                    .build());
            eventWithCategories.publish();

            Event.RegisterCommand command = EventRegisterCommandBuilder.builder()
                    .siCardNumber("123456").build();
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithCategories));

            // When/Then
            assertThatThrownBy(() -> service.registerMember(eventId, TEST_MEMBER_ID, command))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Category is required");

            verify(eventRepository, never()).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("unregisterMember() method")
    class UnregisterMemberMethod {

        private EventId eventId;
        private Event activeEvent;

        @BeforeEach
        void setUp() {
            eventId = EventId.generate();

            activeEvent = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.now().plusDays(5))
                    .location("Test Location").organizer("OOB").build());
            activeEvent.publish();
            activeEvent.registerMember(TEST_MEMBER_ID, SiCardNumber.of("123456"), null);
        }

        @Test
        @DisplayName("should unregister member before event date")
        void shouldUnregisterMemberBeforeEventDate() {
            // Given — event date is in the future (set up in @BeforeEach)
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(activeEvent));
            when(eventRepository.save(any(Event.class))).thenReturn(activeEvent);

            // When
            service.unregisterMember(eventId, TEST_MEMBER_ID);

            // Then
            verify(eventRepository).save(any(Event.class));
            assertThat(activeEvent.getRegistrations()).isEmpty();
        }

        @Test
        @DisplayName("should reject unregistration on event date")
        void shouldRejectUnregistrationOnEventDate() {
            // Given — event date is today so unregistration is not allowed
            Event eventOnToday = Event.reconstruct(
                    EventId.generate(), "Today Event", LocalDate.now(),
                    "Test Location", "OOB",
                    null, null, null, EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(TEST_MEMBER_ID).siCardNumber(SiCardNumber.of("123456")).build())),
                    null
            );
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventOnToday));

            // When/Then
            assertThatThrownBy(() -> service.unregisterMember(eventId, TEST_MEMBER_ID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("on or after event date");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject unregistration after event date")
        void shouldRejectUnregistrationAfterEventDate() {
            // Given — event date is in the past so unregistration is not allowed
            Event pastEvent = Event.reconstruct(
                    EventId.generate(), "Past Event", LocalDate.now().minusDays(1),
                    "Test Location", "OOB",
                    null, null, null, EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(TEST_MEMBER_ID).siCardNumber(SiCardNumber.of("123456")).build())),
                    null
            );
            when(eventRepository.findById(eventId)).thenReturn(Optional.of(pastEvent));

            // When/Then
            assertThatThrownBy(() -> service.unregisterMember(eventId, TEST_MEMBER_ID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("on or after event date");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should reject unregistration when registration deadline has passed")
        void shouldRejectUnregistrationWhenDeadlinePassed() {
            // Given — deadline is in the past; reconstruct() bypasses domain validation to set up this state
            Event eventWithPastDeadline = Event.reconstruct(
                    EventId.generate(),
                    "Deadline Event",
                    LocalDate.now().plusDays(30),
                    "Test Location",
                    "OOB",
                    null,
                    null,
                    LocalDate.now().minusDays(1),
                    EventStatus.ACTIVE,
                    null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(TEST_MEMBER_ID).siCardNumber(SiCardNumber.of("123456")).build())),
                    null
            );

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithPastDeadline));

            // When/Then
            assertThatThrownBy(() -> service.unregisterMember(eventId, TEST_MEMBER_ID))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration deadline has passed");

            verify(eventRepository, never()).save(any(Event.class));
        }

        @Test
        @DisplayName("should allow unregistration before both event date and deadline")
        void shouldAllowUnregistrationBeforeDeadline() {
            // Given — event date and deadline are both in the future
            Event eventWithFutureDeadline = Event.create(EventCreateEventBuilder.builder()
                    .name("Future Deadline Event").eventDate(LocalDate.now().plusDays(60))
                    .location("Test Location").organizer("OOB")
                    .registrationDeadline(LocalDate.now().plusDays(30)).build());
            eventWithFutureDeadline.publish();
            eventWithFutureDeadline.registerMember(TEST_MEMBER_ID, SiCardNumber.of("123456"), null);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(eventWithFutureDeadline));
            when(eventRepository.save(any(Event.class))).thenReturn(eventWithFutureDeadline);

            // When
            service.unregisterMember(eventId, TEST_MEMBER_ID);

            // Then
            verify(eventRepository).save(any(Event.class));
            assertThat(eventWithFutureDeadline.getRegistrations()).isEmpty();
        }
    }

    @Nested
    @DisplayName("listRegistrations() method")
    class ListRegistrationsMethod {

        @Test
        @DisplayName("should return list of EventRegistration domain objects")
        void shouldReturnListOfEventRegistrations() {
            // Given
            EventId eventId = EventId.generate();
            UUID member1Id = UUID.randomUUID();
            UUID member2Id = UUID.randomUUID();

            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("Test Location").organizer("OOB").build());
            event.publish();
            event.registerMember(new MemberId(member1Id), SiCardNumber.of("111111"), null);
            event.registerMember(new MemberId(member2Id), SiCardNumber.of("222222"), null);

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When
            List<EventRegistration> registrations = service.listRegistrations(eventId);

            // Then
            assertThat(registrations).hasSize(2);
            assertThat(registrations).extracting(r -> r.memberId().uuid())
                    .containsExactlyInAnyOrder(member1Id, member2Id);
        }

        @Test
        @DisplayName("should return empty list when no registrations")
        void shouldReturnEmptyListWhenNoRegistrations() {
            // Given
            EventId eventId = EventId.generate();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event").eventDate(LocalDate.of(2026, 6, 15))
                    .location("Test Location").organizer("OOB").build());
            event.publish();

            when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

            // When
            List<EventRegistration> registrations = service.listRegistrations(eventId);

            // Then
            assertThat(registrations).isEmpty();
        }
    }

}
