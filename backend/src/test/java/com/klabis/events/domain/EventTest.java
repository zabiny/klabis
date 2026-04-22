package com.klabis.events.domain;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.events.*;
import com.klabis.members.MemberId;
import com.klabis.events.domain.RegistrationNotFoundException;
import com.klabis.events.domain.EventRegistrationCreateEventRegistrationBuilder;
import com.klabis.events.domain.EventUnregisterMemberBuilder;
import com.klabis.events.domain.EventUpdateEventBuilder;
import com.klabis.events.domain.EventEditRegistrationCommandBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Test for Event aggregate root (RED phase).
 * <p>
 * Tests business rules and invariants:
 * - Event creation with required and optional fields
 * - Status lifecycle transitions (DRAFT → ACTIVE → FINISHED)
 * - Status transition validation (prevent invalid transitions)
 * - Event updates allowed only in DRAFT and ACTIVE status
 * - Event updates forbidden in FINISHED and CANCELLED status
 */
@DisplayName("Event Aggregate")
class EventTest {

    private static final LocalDate DEFAULT_DATE = LocalDate.now().plusDays(90);

    private static Event.CreateEvent defaultCreateEvent() {
        return EventCreateEventBuilder.builder()
                .name("Test Event")
                .eventDate(DEFAULT_DATE)
                .location("Test Location")
                .organizer("Test Organizer")
                .build();
    }

    @Nested
    @DisplayName("create() factory method")
    class CreateMethod {

        @Test
        @DisplayName("should create event in DRAFT status with all fields")
        void shouldCreateEventInDraftStatusWithAllFields() {
            // Arrange
            String name = "Mountain Orienteering Race 2025";
            LocalDate eventDate = LocalDate.of(2025, 6, 15);
            String location = "Krkonoše National Park";
            String organizer = "Czech Orienteering Club";
            String websiteUrl = "https://example.com/race";
            MemberId coordinatorId = new MemberId(UUID.randomUUID());

            // Act
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name(name)
                    .eventDate(eventDate)
                    .location(location)
                    .organizer(organizer)
                    .websiteUrl(websiteUrl)
                    .eventCoordinatorId(coordinatorId)
                    .build());

            // Assert
            EventAssert.assertThat(event)
                    .hasIdNotNull()
                    .hasName(name)
                    .hasDate(eventDate)
                    .hasLocation(location)
                    .hasOrganizer(organizer)
                    .hasWebsiteUrl(WebsiteUrl.of(websiteUrl))
                    .hasEventCoordinatorId(coordinatorId)
                    .hasStatus(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should create event with null optional fields (websiteUrl and coordinatorId)")
        void shouldCreateEventWithNullOptionalFields() {
            // Arrange
            String name = "Local Training Event";
            LocalDate eventDate = LocalDate.of(2025, 5, 20);
            String location = "City Park";
            String organizer = "Local Sports Club";

            // Act
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name(name)
                    .eventDate(eventDate)
                    .location(location)
                    .organizer(organizer)
                    .build());

            // Assert
            EventAssert.assertThat(event)
                    .hasName(name)
                    .hasDate(eventDate)
                    .hasLocation(location)
                    .hasOrganizer(organizer)
                    .hasWebsiteUrl(null)
                    .hasEventCoordinatorId(null)
                    .hasStatus(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should fail when name is null")
        void shouldFailWhenNameIsNull() {
            assertThatThrownBy(() -> Event.create(EventCreateEventBuilder.builder()
                    .name(null)
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Location")
                    .organizer("Organizer")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            assertThatThrownBy(() -> Event.create(EventCreateEventBuilder.builder()
                    .name("   ")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Location")
                    .organizer("Organizer")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when eventDate is null")
        void shouldFailWhenEventDateIsNull() {
            assertThatThrownBy(() -> Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(null)
                    .location("Location")
                    .organizer("Organizer")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventDate");
        }

        @Test
        @DisplayName("should create event when location is null")
        void shouldCreateEventWhenLocationIsNull() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location(null)
                    .organizer("Organizer")
                    .build());

            assertThat(event.getLocation()).isNull();
        }

        @Test
        @DisplayName("should create event when location is empty string")
        void shouldCreateEventWhenLocationIsEmptyString() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("")
                    .organizer("Organizer")
                    .build());

            assertThat(event.getLocation()).isEqualTo("");
        }

        @Test
        @DisplayName("should fail when organizer is null")
        void shouldFailWhenOrganizerIsNull() {
            assertThatThrownBy(() -> Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Location")
                    .organizer(null)
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }

        @Test
        @DisplayName("should fail when organizer is blank")
        void shouldFailWhenOrganizerIsBlank() {
            assertThatThrownBy(() -> Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Location")
                    .organizer("   ")
                    .build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }

        @Test
        @DisplayName("should fail when registrationDeadline is after eventDate")
        void shouldFailWhenRegistrationDeadlineIsAfterEventDate() {
            LocalDate eventDate = LocalDate.of(2026, 6, 15);
            LocalDate invalidDeadline = LocalDate.of(2026, 6, 16);

            assertThatThrownBy(() -> Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(eventDate)
                    .location("Location")
                    .organizer("Organizer")
                    .registrationDeadline(invalidDeadline)
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration deadline");
        }

        @Test
        @DisplayName("should allow registrationDeadline equal to eventDate")
        void shouldAllowRegistrationDeadlineEqualToEventDate() {
            LocalDate eventDate = LocalDate.of(2026, 6, 15);

            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Event Name")
                    .eventDate(eventDate)
                    .location("Location")
                    .organizer("Organizer")
                    .registrationDeadline(eventDate)
                    .build());

            assertThat(event.getRegistrationDeadline()).isEqualTo(eventDate);
        }
    }

    @Nested
    @DisplayName("Lifecycle transitions")
    class LifecycleTransitions {

        @Test
        @DisplayName("should publish event: DRAFT → ACTIVE")
        void shouldPublishEventFromDraftToActive() {
            Event event = Event.create(defaultCreateEvent());
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            event.publish();

            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("should cancel event: DRAFT → CANCELLED")
        void shouldCancelEventFromDraft() {
            Event event = Event.create(defaultCreateEvent());
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            event.cancel();

            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("should cancel event: ACTIVE → CANCELLED")
        void shouldCancelEventFromActive() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            event.cancel();

            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("should finish event: ACTIVE → FINISHED")
        void shouldFinishEventFromActive() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            event.finish();

            EventAssert.assertThat(event).hasStatus(EventStatus.FINISHED);
        }

        @Test
        @DisplayName("should fail to finish event from DRAFT")
        void shouldFailToFinishEventFromDraft() {
            Event event = Event.create(defaultCreateEvent());
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            assertThatThrownBy(() -> event.finish())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from DRAFT to FINISHED");
        }

        @Test
        @DisplayName("should allow idempotent publish (ACTIVE to ACTIVE)")
        void shouldAllowIdempotentPublish() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            event.publish();

            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);
        }

        @ParameterizedTest(name = "{3}")
        @MethodSource("illegalTransitions")
        @DisplayName("should reject illegal state transition")
        void shouldRejectIllegalStateTransition(Consumer<Event> setup, Consumer<Event> action, String expectedMessage, String displayName) {
            Event event = EventTestDataBuilder.anEvent().build();
            setup.accept(event);

            assertThatThrownBy(() -> action.accept(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(expectedMessage);
        }

        static Stream<Arguments> illegalTransitions() {
            return Stream.of(
                    Arguments.of(
                            (Consumer<Event>) e -> {
                                e.publish();
                                e.finish();
                            },
                            (Consumer<Event>) Event::publish,
                            "Cannot transition from FINISHED to ACTIVE",
                            "FINISHED -> publish"
                    ),
                    Arguments.of(
                            (Consumer<Event>) Event::cancel,
                            (Consumer<Event>) Event::finish,
                            "Cannot transition from CANCELLED to FINISHED",
                            "CANCELLED -> finish"
                    ),
                    Arguments.of(
                            (Consumer<Event>) e -> {
                                e.publish();
                                e.finish();
                            },
                            (Consumer<Event>) Event::cancel,
                            "Cannot transition from FINISHED to CANCELLED",
                            "FINISHED -> cancel"
                    )
            );
        }
    }

    @Nested
    @DisplayName("update() method")
    class UpdateMethod {

        @Test
        @DisplayName("should update event in DRAFT status")
        void shouldUpdateEventInDraftStatus() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Original Event")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Original Location")
                    .organizer("Original Organizer")
                    .build());
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            String newName = "Updated Event";
            LocalDate newDate = LocalDate.of(2025, 7, 20);
            String newLocation = "Updated Location";
            String newOrganizer = "Updated Organizer";
            String newWebsiteUrl = "https://updated.com";
            MemberId newCoordinatorId = new MemberId(UUID.randomUUID());

            event.update(EventUpdateEventBuilder.builder()
                    .name(newName).eventDate(newDate).location(newLocation)
                    .organizer(newOrganizer).websiteUrl(newWebsiteUrl)
                    .eventCoordinatorId(newCoordinatorId).build());

            EventAssert.assertThat(event)
                    .hasName(newName)
                    .hasDate(newDate)
                    .hasLocation(newLocation)
                    .hasOrganizer(newOrganizer)
                    .hasWebsiteUrl(WebsiteUrl.of(newWebsiteUrl))
                    .hasEventCoordinatorId(newCoordinatorId)
                    .hasStatus(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should update event in ACTIVE status")
        void shouldUpdateEventInActiveStatus() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Original Event")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Original Location")
                    .organizer("Original Organizer")
                    .build());
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            String newName = "Updated Event";
            LocalDate newDate = LocalDate.of(2025, 7, 20);
            String newLocation = "Updated Location";
            String newOrganizer = "Updated Organizer";

            event.update(EventUpdateEventBuilder.builder()
                    .name(newName).eventDate(newDate).location(newLocation)
                    .organizer(newOrganizer).build());

            EventAssert.assertThat(event)
                    .hasName(newName)
                    .hasDate(newDate)
                    .hasLocation(newLocation)
                    .hasOrganizer(newOrganizer)
                    .hasStatus(EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("should fail to update event in FINISHED status")
        void shouldFailToUpdateEventInFinishedStatus() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Original Event")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Original Location")
                    .organizer("Original Organizer")
                    .build());
            event.publish();
            event.finish();
            EventAssert.assertThat(event).hasStatus(EventStatus.FINISHED);

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name("New Name").eventDate(LocalDate.of(2025, 7, 20)).location("New Location").organizer("New Organizer").build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot update event in FINISHED status");
        }

        @Test
        @DisplayName("should fail to update event in CANCELLED status")
        void shouldFailToUpdateEventInCancelledStatus() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Original Event")
                    .eventDate(LocalDate.of(2025, 6, 15))
                    .location("Original Location")
                    .organizer("Original Organizer")
                    .build());
            event.cancel();
            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name("New Name").eventDate(LocalDate.of(2025, 7, 20)).location("New Location").organizer("New Organizer").build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot update event in CANCELLED status");
        }

        @Test
        @DisplayName("should fail to update with null name")
        void shouldFailToUpdateWithNullName() {
            Event event = Event.create(defaultCreateEvent());

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name(null).eventDate(LocalDate.of(2025, 7, 20)).location("Location").organizer("Organizer").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with blank name")
        void shouldFailToUpdateWithBlankName() {
            Event event = Event.create(defaultCreateEvent());

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name("   ").eventDate(LocalDate.of(2025, 7, 20)).location("Location").organizer("Organizer").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with null eventDate")
        void shouldFailToUpdateWithNullEventDate() {
            Event event = Event.create(defaultCreateEvent());

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name("Event Name").eventDate(null).location("Location").organizer("Organizer").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventDate");
        }

        @Test
        @DisplayName("should update event with null location — location becomes null")
        void shouldUpdateEventWithNullLocation() {
            Event event = Event.create(defaultCreateEvent());

            event.update(EventUpdateEventBuilder.builder()
                    .name("Event Name").eventDate(LocalDate.of(2025, 7, 20)).location(null).organizer("Organizer").build());

            assertThat(event.getLocation()).isNull();
        }

        @Test
        @DisplayName("should fail to update with null organizer")
        void shouldFailToUpdateWithNullOrganizer() {
            Event event = Event.create(defaultCreateEvent());

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name("Event Name").eventDate(LocalDate.of(2025, 7, 20)).location("Location").organizer(null).build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }

        @Test
        @DisplayName("should fail to update with blank organizer")
        void shouldFailToUpdateWithBlankOrganizer() {
            Event event = Event.create(defaultCreateEvent());

            assertThatThrownBy(() -> event.update(EventUpdateEventBuilder.builder()
                    .name("Event Name").eventDate(LocalDate.of(2025, 7, 20)).location("Location").organizer("   ").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }
    }

    @Nested
    @DisplayName("Member Registration")
    class MemberRegistration {

        @Test
        @DisplayName("should register member when event is ACTIVE")
        void shouldRegisterMemberWhenEventIsActive() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            event.registerMember(memberId, siCardNumber, null);

            assertThat(event.getRegistrations()).hasSize(1);
            assertThat(event.findRegistration(memberId)).isPresent();
        }

        @Test
        @DisplayName("should fail to register member when event is DRAFT")
        void shouldFailToRegisterMemberWhenEventIsDraft() {
            Event event = Event.create(defaultCreateEvent());
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            assertThatThrownBy(() -> event.registerMember(memberId, siCardNumber, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration is only allowed for ACTIVE events");
        }

        @Test
        @DisplayName("should fail to register member when event is FINISHED")
        void shouldFailToRegisterMemberWhenEventIsFinished() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();
            event.finish();
            EventAssert.assertThat(event).hasStatus(EventStatus.FINISHED);

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            assertThatThrownBy(() -> event.registerMember(memberId, siCardNumber, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration is only allowed for ACTIVE events");
        }

        @Test
        @DisplayName("should fail to register member when event is CANCELLED")
        void shouldFailToRegisterMemberWhenEventIsCancelled() {
            Event event = Event.create(defaultCreateEvent());
            event.cancel();
            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            assertThatThrownBy(() -> event.registerMember(memberId, siCardNumber, null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration is only allowed for ACTIVE events");
        }

        @Test
        @DisplayName("should prevent duplicate registration for same member")
        void shouldPreventDuplicateRegistrationForSameMember() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber, null);

            assertThatThrownBy(() -> event.registerMember(memberId, SiCardNumber.of("654321"), null))
                    .isInstanceOf(DuplicateRegistrationException.class)
                    .hasMessageContaining("already registered");
        }

        @Test
        @DisplayName("should unregister member before event date")
        void shouldUnregisterMemberBeforeEventDate() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event")
                    .eventDate(LocalDate.now().plusDays(1))
                    .location("Test Location")
                    .organizer("Test Organizer")
                    .build());
            event.publish();

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber, null);
            assertThat(event.getRegistrations()).hasSize(1);

            event.unregisterMember(EventUnregisterMemberBuilder.builder().memberId(memberId).build());

            assertThat(event.getRegistrations()).isEmpty();
            assertThat(event.findRegistration(memberId)).isEmpty();
        }

        @Test
        @DisplayName("should fail to unregister member on event date")
        void shouldFailToUnregisterMemberOnEventDate() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = Event.reconstruct(
                    EventId.generate(), "Test Event", LocalDate.now(),
                    "Test Location", "Test Organizer",
                    null, null, null, EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(SiCardNumber.of("123456")).build())),
                    null
            );

            assertThatThrownBy(() -> event.unregisterMember(EventUnregisterMemberBuilder.builder().memberId(memberId).build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("on or after event date");
        }

        @Test
        @DisplayName("should fail to unregister member after event date")
        void shouldFailToUnregisterMemberAfterEventDate() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = Event.reconstruct(
                    EventId.generate(), "Test Event", LocalDate.now().minusDays(1),
                    "Test Location", "Test Organizer",
                    null, null, null, EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(SiCardNumber.of("123456")).build())),
                    null
            );

            assertThatThrownBy(() -> event.unregisterMember(EventUnregisterMemberBuilder.builder().memberId(memberId).build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("on or after event date");
        }

        @Test
        @DisplayName("should fail to unregister member that is not registered")
        void shouldFailToUnregisterMemberThatIsNotRegistered() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event")
                    .eventDate(LocalDate.now().plusDays(1))
                    .location("Test Location")
                    .organizer("Test Organizer")
                    .build());
            event.publish();

            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> event.unregisterMember(EventUnregisterMemberBuilder.builder().memberId(memberId).build()))
                    .isInstanceOf(RegistrationNotFoundException.class)
                    .hasMessageContaining("not registered");
        }

        @Test
        @DisplayName("should find registration by memberId when registered")
        void shouldFindRegistrationByMemberIdWhenRegistered() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();

            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber, null);

            var foundRegistration = event.findRegistration(memberId);

            assertThat(foundRegistration).isPresent();
            assertThat(foundRegistration.get().memberId()).isEqualTo(memberId);
            assertThat(foundRegistration.get().siCardNumber()).isEqualTo(siCardNumber);
        }

        @Test
        @DisplayName("should return empty when finding non-existent registration")
        void shouldReturnEmptyWhenFindingNonExistentRegistration() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();

            MemberId memberId = new MemberId(UUID.randomUUID());

            var foundRegistration = event.findRegistration(memberId);

            assertThat(foundRegistration).isEmpty();
        }

        @Test
        @DisplayName("should return unmodifiable list of registrations")
        void shouldReturnUnmodifiableListOfRegistrations() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();

            MemberId memberId1 = new MemberId(UUID.randomUUID());
            MemberId memberId2 = new MemberId(UUID.randomUUID());
            SiCardNumber siCardNumber1 = SiCardNumber.of("123456");
            SiCardNumber siCardNumber2 = SiCardNumber.of("654321");

            event.registerMember(memberId1, siCardNumber1, null);
            event.registerMember(memberId2, siCardNumber2, null);

            var registrations = event.getRegistrations();

            assertThat(registrations).hasSize(2);

            assertThatThrownBy(() -> registrations.add(EventRegistration.create(
                    EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(new MemberId(UUID.randomUUID()))
                            .siCardNumber(SiCardNumber.of("111111"))
                            .build())))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("should register member with category when event has categories")
        void shouldRegisterMemberWithCategoryWhenEventHasCategories() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race").eventDate(DEFAULT_DATE).location("Forest").organizer("Club")
                    .categories(List.of("M21", "W35"))
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());

            event.registerMember(memberId, SiCardNumber.of("123456"), "M21");

            assertThat(event.findRegistration(memberId)).isPresent();
            assertThat(event.findRegistration(memberId).get().category()).isEqualTo("M21");
        }

        @Test
        @DisplayName("should throw when category is not provided but event has categories")
        void shouldThrowWhenCategoryMissingForEventWithCategories() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race").eventDate(DEFAULT_DATE).location("Forest").organizer("Club")
                    .categories(List.of("M21", "W35"))
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> event.registerMember(memberId, SiCardNumber.of("123456"), null))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Category is required");
        }

        @Test
        @DisplayName("should throw when category is not in event's category list")
        void shouldThrowWhenCategoryNotInEventCategoryList() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race").eventDate(DEFAULT_DATE).location("Forest").organizer("Club")
                    .categories(List.of("M21", "W35"))
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> event.registerMember(memberId, SiCardNumber.of("123456"), "H55"))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("should register without category when event has no categories")
        void shouldRegisterWithoutCategoryWhenEventHasNoCategories() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race").eventDate(DEFAULT_DATE).location("Forest").organizer("Club")
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());

            event.registerMember(memberId, SiCardNumber.of("123456"), null);

            assertThat(event.findRegistration(memberId)).isPresent();
            assertThat(event.findRegistration(memberId).get().category()).isNull();
        }

        @Test
        @DisplayName("should ignore provided category when event has no categories")
        void shouldIgnoreCategoryWhenEventHasNoCategories() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race").eventDate(DEFAULT_DATE).location("Forest").organizer("Club")
                    .build());
            event.publish();
            MemberId memberId = new MemberId(UUID.randomUUID());

            event.registerMember(memberId, SiCardNumber.of("123456"), "M21");

            assertThat(event.findRegistration(memberId)).isPresent();
            assertThat(event.findRegistration(memberId).get().category()).isNull();
        }
    }

    @Nested
    @DisplayName("areRegistrationsOpen()")
    class AreRegistrationsOpen {

        @Test
        @DisplayName("should return true when event is ACTIVE and eventDate is in the future")
        void shouldReturnTrueWhenActiveAndDateInFuture() {
            LocalDate futureDate = LocalDate.now().plusDays(1);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race 2026").eventDate(futureDate).location("Forest").organizer("Club").build());
            event.publish();

            assertThat(event.areRegistrationsOpen()).isTrue();
        }

        @Test
        @DisplayName("should return false when event is ACTIVE but eventDate is today")
        void shouldReturnFalseWhenActiveAndDateIsToday() {
            LocalDate today = LocalDate.now();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race Today").eventDate(today).location("Forest").organizer("Club").build());
            event.publish();

            assertThat(event.areRegistrationsOpen()).isFalse();
        }

        @Test
        @DisplayName("should return false when event is ACTIVE but eventDate is in the past")
        void shouldReturnFalseWhenActiveAndDateInPast() {
            LocalDate pastDate = LocalDate.now().minusDays(1);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Past Race").eventDate(pastDate).location("Forest").organizer("Club").build());
            event.publish();

            assertThat(event.areRegistrationsOpen()).isFalse();
        }

        @Test
        @DisplayName("should return false when event is DRAFT even if eventDate is in the future")
        void shouldReturnFalseWhenDraftAndDateInFuture() {
            LocalDate futureDate = LocalDate.now().plusDays(7);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Future Draft").eventDate(futureDate).location("Forest").organizer("Club").build());

            assertThat(event.areRegistrationsOpen()).isFalse();
        }

        @Test
        @DisplayName("should return false when event is CANCELLED even if eventDate is in the future")
        void shouldReturnFalseWhenCancelledAndDateInFuture() {
            LocalDate futureDate = LocalDate.now().plusDays(7);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Cancelled Race").eventDate(futureDate).location("Forest").organizer("Club").build());
            event.cancel();

            assertThat(event.areRegistrationsOpen()).isFalse();
        }

        @Test
        @DisplayName("should return false when event is FINISHED even if eventDate is in the future")
        void shouldReturnFalseWhenFinishedAndDateInFuture() {
            LocalDate futureDate = LocalDate.now().plusDays(7);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Finished Race").eventDate(futureDate).location("Forest").organizer("Club").build());
            event.publish();
            event.finish();

            assertThat(event.areRegistrationsOpen()).isFalse();
        }

        @Test
        @DisplayName("should return true when ACTIVE event has deadline in the future")
        void shouldReturnTrueWhenActiveAndDeadlineInFuture() {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            LocalDate deadline = LocalDate.now().plusDays(5);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race with Deadline").eventDate(futureDate).location("Forest").organizer("Club")
                    .registrationDeadline(deadline).build());
            event.publish();

            assertThat(event.areRegistrationsOpen()).isTrue();
        }

        @Test
        @DisplayName("should return false when ACTIVE event registration deadline has passed")
        void shouldReturnFalseWhenDeadlinePassed() {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            LocalDate pastDeadline = LocalDate.now().minusDays(1);
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race Closed").eventDate(futureDate).location("Forest").organizer("Club")
                    .registrationDeadline(pastDeadline).build());
            event.publish();

            assertThat(event.areRegistrationsOpen()).isFalse();
        }

        @Test
        @DisplayName("should return false when ACTIVE event registration deadline is today")
        void shouldReturnFalseWhenDeadlineIsToday() {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            LocalDate todayDeadline = LocalDate.now();
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Race Deadline Today").eventDate(futureDate).location("Forest").organizer("Club")
                    .registrationDeadline(todayDeadline).build());
            event.publish();

            assertThat(event.areRegistrationsOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("Domain Events (getDomainEvents, clearDomainEvents)")
    class DomainEvents {

        @Test
        @DisplayName("should register EventCreatedEvent when event is created")
        void shouldRegisterEventCreatedEventWhenCreated() {
            String name = "Test Event";
            LocalDate eventDate = LocalDate.of(2025, 7, 10);
            MemberId coordinatorId = new MemberId(UUID.randomUUID());

            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name(name)
                    .eventDate(eventDate)
                    .location("Test Location")
                    .organizer("Test Organizer")
                    .websiteUrl("https://test.com")
                    .eventCoordinatorId(coordinatorId)
                    .build());

            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventCreatedEvent.class);

            EventCreatedEvent createdEvent = (EventCreatedEvent) domainEvents.get(0);
            assertThat(createdEvent.eventId()).isEqualTo(event.getId());
            assertThat(createdEvent.name()).isEqualTo(name);
            assertThat(createdEvent.eventDate()).isEqualTo(eventDate);
            assertThat(createdEvent.location()).isEqualTo("Test Location");
            assertThat(createdEvent.organizer()).isEqualTo("Test Organizer");
        }

        @Test
        @DisplayName("should register EventPublishedEvent when event is published")
        void shouldRegisterEventPublishedEventWhenPublished() {
            Event event = Event.create(defaultCreateEvent());
            event.clearDomainEvents(); // Clear creation event

            event.publish();

            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventPublishedEvent.class);

            EventPublishedEvent publishedEvent = (EventPublishedEvent) domainEvents.get(0);
            assertThat(publishedEvent.eventId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("should register EventCancelledEvent when event is cancelled")
        void shouldRegisterEventCancelledEventWhenCancelled() {
            Event event = Event.create(defaultCreateEvent());
            event.clearDomainEvents(); // Clear creation event

            event.cancel();

            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventCancelledEvent.class);

            EventCancelledEvent cancelledEvent = (EventCancelledEvent) domainEvents.get(0);
            assertThat(cancelledEvent.eventId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("should register EventFinishedEvent when event is finished")
        void shouldRegisterEventFinishedEventWhenFinished() {
            Event event = Event.create(defaultCreateEvent());
            event.publish();
            event.clearDomainEvents(); // Clear previous events

            event.finish();

            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventFinishedEvent.class);

            EventFinishedEvent finishedEvent = (EventFinishedEvent) domainEvents.get(0);
            assertThat(finishedEvent.eventId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("should clear domain events after clearDomainEvents is called")
        void shouldClearDomainEventsAfterClear() {
            Event event = Event.create(defaultCreateEvent());
            assertThat(event.getDomainEvents()).hasSize(1);

            event.clearDomainEvents();

            assertThat(event.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should register EventUpdatedEvent when event is updated")
        void shouldRegisterEventUpdatedEventWhenUpdated() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Original Event")
                    .eventDate(LocalDate.of(2025, 7, 10))
                    .location("Original Location")
                    .organizer("Original Organizer")
                    .websiteUrl("https://original.com")
                    .build());
            event.clearDomainEvents(); // Clear creation event

            event.update(EventUpdateEventBuilder.builder()
                    .name("Updated Event")
                    .eventDate(LocalDate.of(2025, 7, 15))
                    .location("Updated Location")
                    .organizer("Updated Organizer")
                    .websiteUrl("https://updated.com")
                    .build());

            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventUpdatedEvent.class);

            EventUpdatedEvent updatedEvent = (EventUpdatedEvent) domainEvents.get(0);
            assertThat(updatedEvent.eventId()).isEqualTo(event.getId());
            assertThat(updatedEvent.name()).isEqualTo("Updated Event");
            assertThat(updatedEvent.eventDate()).isEqualTo(LocalDate.of(2025, 7, 15));
            assertThat(updatedEvent.location()).isEqualTo("Updated Location");
            assertThat(updatedEvent.organizer()).isEqualTo("Updated Organizer");
            assertThat(updatedEvent.websiteUrl()).isEqualTo(WebsiteUrl.of("https://updated.com"));
            assertThat(updatedEvent.occurredAt()).isNotNull();
        }

        @Test
        @DisplayName("should register EventUpdatedEvent with null websiteUrl")
        void shouldRegisterEventUpdatedEventWithNullWebsiteUrl() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Test Event")
                    .eventDate(LocalDate.of(2025, 7, 10))
                    .location("Test Location")
                    .organizer("Test Organizer")
                    .websiteUrl("https://test.com")
                    .build());
            event.clearDomainEvents();

            event.update(EventUpdateEventBuilder.builder()
                    .name("Updated Event").eventDate(LocalDate.of(2025, 7, 15))
                    .location("Updated Location").organizer("Updated Organizer").build());

            List<Object> domainEvents = event.getDomainEvents();
            EventUpdatedEvent updatedEvent = (EventUpdatedEvent) domainEvents.get(0);
            assertThat(updatedEvent.websiteUrl()).isNull();
        }

        @Test
        @DisplayName("should accumulate multiple domain events")
        void shouldAccumulateMultipleDomainEvents() {
            Event event = Event.create(defaultCreateEvent());

            event.publish(); // Adds EventPublishedEvent

            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents).hasSize(2);
            assertThat(domainEvents.get(0)).isInstanceOf(EventCreatedEvent.class);
            assertThat(domainEvents.get(1)).isInstanceOf(EventPublishedEvent.class);
        }
    }

    @Nested
    @DisplayName("createFromOris() factory method")
    class CreateFromOrisMethod {

        @Test
        @DisplayName("should create event in DRAFT status with correct field values and non-null orisId")
        void shouldCreateEventFromOrisInDraftStatusWithCorrectFields() {
            int orisId = 9876;
            String name = "Oris Sprint Race";
            LocalDate eventDate = LocalDate.of(2026, 8, 10);
            String location = "Brno City Center";
            String organizer = "OOB";
            WebsiteUrl websiteUrl = WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=9876");

            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(orisId)
                    .name(name)
                    .eventDate(eventDate)
                    .location(location)
                    .organizer(organizer)
                    .websiteUrl(websiteUrl)
                    .build());

            assertThat(event.getId()).isNotNull();
            assertThat(event.getName()).isEqualTo(name);
            assertThat(event.getEventDate()).isEqualTo(eventDate);
            assertThat(event.getLocation()).isEqualTo(location);
            assertThat(event.getOrganizer()).isEqualTo(organizer);
            assertThat(event.getWebsiteUrl()).isEqualTo(websiteUrl);
            assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
            assertThat(event.getOrisId()).isEqualTo(orisId);
        }

        @Test
        @DisplayName("should register EventCreatedEvent on creation")
        void shouldRegisterEventCreatedEvent() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(1234)
                    .name("Test ORIS Event")
                    .eventDate(LocalDate.of(2026, 9, 5))
                    .location("Prague Forest")
                    .organizer("PRG")
                    .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=1234"))
                    .build());

            assertThat(event.getDomainEvents()).hasSize(1);
            assertThat(event.getDomainEvents().get(0)).isInstanceOf(EventCreatedEvent.class);
        }

        @Test
        @DisplayName("should not set eventCoordinatorId (imported events have no coordinator)")
        void shouldNotSetEventCoordinatorId() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(5555)
                    .name("Coordinator-less Event")
                    .eventDate(LocalDate.of(2026, 10, 1))
                    .location("Some Place")
                    .organizer("TST")
                    .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=5555"))
                    .build());

            assertThat(event.getEventCoordinatorId()).isNull();
        }

        @Test
        @DisplayName("should create event from ORIS when location is null")
        void shouldCreateEventFromOrisWhenLocationIsNull() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(7777)
                    .name("Event Without Location")
                    .eventDate(LocalDate.of(2026, 11, 1))
                    .location(null)
                    .organizer("OOB")
                    .websiteUrl(null)
                    .build());

            assertThat(event.getLocation()).isNull();
            assertThat(event.getStatus()).isEqualTo(EventStatus.DRAFT);
        }
    }

    @Nested
    @DisplayName("syncFromOris() domain method")
    class SyncFromOrisMethod {

        private Event.SyncFromOris defaultSyncCommand() {
            return EventSyncFromOrisBuilder.builder()
                    .name("Synced Name")
                    .eventDate(LocalDate.of(2026, 9, 1))
                    .location("Synced Location")
                    .organizer("SYN")
                    .websiteUrl(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=100"))
                    .registrationDeadline(null)
                    .categories(List.of("M21", "W35"))
                    .build();
        }

        @Test
        @DisplayName("should overwrite all event fields and publish EventUpdatedEvent for DRAFT event")
        void shouldOverwriteFieldsAndPublishEventForDraftEvent() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(100)
                    .name("Old Name")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .location("Old Location")
                    .organizer("OLD")
                    .build());

            event.syncFromOris(defaultSyncCommand());

            assertThat(event.getName()).isEqualTo("Synced Name");
            assertThat(event.getEventDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(event.getLocation()).isEqualTo("Synced Location");
            assertThat(event.getOrganizer()).isEqualTo("SYN");
            assertThat(event.getWebsiteUrl()).isEqualTo(WebsiteUrl.of("https://oris.ceskyorientak.cz/Zavod?id=100"));
            assertThat(event.getCategories()).containsExactlyInAnyOrder("M21", "W35");
            assertThat(event.getDomainEvents()).hasSize(2); // create + sync
            assertThat(event.getDomainEvents())
                    .anyMatch(e -> e instanceof com.klabis.events.EventUpdatedEvent);
        }

        @Test
        @DisplayName("should overwrite all event fields for ACTIVE event")
        void shouldOverwriteFieldsForActiveEvent() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(100)
                    .name("Old Name")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .location("Old Location")
                    .organizer("OLD")
                    .build());
            event.publish();

            event.syncFromOris(defaultSyncCommand());

            assertThat(event.getName()).isEqualTo("Synced Name");
            assertThat(event.getStatus()).isEqualTo(EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("should throw IllegalStateException when event is FINISHED")
        void shouldThrowWhenEventIsFinished() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(100)
                    .name("Old Name")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .location("Old Location")
                    .organizer("OLD")
                    .build());
            event.publish();
            event.finish();

            assertThatThrownBy(() -> event.syncFromOris(defaultSyncCommand()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FINISHED");
        }

        @Test
        @DisplayName("should throw IllegalStateException when event is CANCELLED")
        void shouldThrowWhenEventIsCancelled() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(100)
                    .name("Old Name")
                    .eventDate(LocalDate.of(2026, 8, 1))
                    .location("Old Location")
                    .organizer("OLD")
                    .build());
            event.cancel();

            assertThatThrownBy(() -> event.syncFromOris(defaultSyncCommand()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("CANCELLED");
        }

        @Test
        @DisplayName("should throw IllegalStateException when event has no orisId")
        void shouldThrowWhenEventHasNoOrisId() {
            Event event = Event.create(EventCreateEventBuilder.builder()
                    .name("Manual Event")
                    .eventDate(DEFAULT_DATE)
                    .location("Location")
                    .organizer("OOB")
                    .build());

            assertThatThrownBy(() -> event.syncFromOris(defaultSyncCommand()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("orisId");
        }

        @Test
        @DisplayName("should update registrationDeadline from command")
        void shouldUpdateRegistrationDeadline() {
            Event event = Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                    .orisId(100)
                    .name("Old Name")
                    .eventDate(LocalDate.of(2026, 9, 10))
                    .location("Location")
                    .organizer("OLD")
                    .build());

            Event.SyncFromOris commandWithDeadline = EventSyncFromOrisBuilder.builder()
                    .name("Updated")
                    .eventDate(LocalDate.of(2026, 9, 10))
                    .location("Location")
                    .organizer("SYN")
                    .registrationDeadline(LocalDate.of(2026, 9, 5))
                    .categories(List.of())
                    .build();

            event.syncFromOris(commandWithDeadline);

            assertThat(event.getRegistrationDeadline()).isEqualTo(LocalDate.of(2026, 9, 5));
        }
    }

    @Nested
    @DisplayName("editRegistration() – Skupina 1: SI karta")
    class EditRegistrationSiCard {

        private Event activeEventWithFutureDate() {
            return EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(10))
                    .buildPublished();
        }

        private Event activeEventWithRegisteredMember(MemberId memberId, SiCardNumber siCard) {
            Event event = activeEventWithFutureDate();
            event.clearDomainEvents();
            event.registerMember(memberId, siCard, null);
            event.clearDomainEvents();
            return event;
        }

        @Test
        @DisplayName("should update SI card number, preserve id and registeredAt, replace registration in collection")
        void shouldUpdateSiCardPreservingIdAndRegisteredAt() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber originalSiCard = SiCardNumber.of("123456");
            Event event = activeEventWithRegisteredMember(memberId, originalSiCard);

            UUID originalRegistrationId = event.findRegistration(memberId).get().id();
            var originalRegisteredAt = event.findRegistration(memberId).get().registeredAt();

            SiCardNumber newSiCard = SiCardNumber.of("654321");
            event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(newSiCard)
                    .build());

            assertThat(event.getRegistrations()).hasSize(1);
            var updated = event.findRegistration(memberId).orElseThrow();
            assertThat(updated.siCardNumber()).isEqualTo(newSiCard);
            assertThat(updated.id()).isEqualTo(originalRegistrationId);
            assertThat(updated.registeredAt()).isEqualTo(originalRegisteredAt);
        }

        @Test
        @DisplayName("should throw RegistrationNotFoundException when member is not registered")
        void shouldThrowWhenMemberNotRegistered() {
            Event event = activeEventWithFutureDate();
            MemberId unknownMember = new MemberId(UUID.randomUUID());

            assertThatThrownBy(() -> event.editRegistration(unknownMember, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("111111"))
                    .build()))
                    .isInstanceOf(RegistrationNotFoundException.class)
                    .hasMessageContaining("not registered");
        }

        @Test
        @DisplayName("should throw when event is not ACTIVE")
        void shouldThrowWhenEventNotActive() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event draftEvent = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(10))
                    .build();

            assertThatThrownBy(() -> draftEvent.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("111111"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("ACTIVE");
        }

        @Test
        @DisplayName("should throw when registrationDeadline has passed")
        void shouldThrowWhenDeadlinePassed() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCard = SiCardNumber.of("123456");
            Event event = Event.reconstruct(
                    EventId.generate(), "Test Event", LocalDate.now().plusDays(10),
                    "Location", "Organizer",
                    null, null, LocalDate.now().minusDays(1), EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(siCard).build())),
                    null
            );

            assertThatThrownBy(() -> event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("654321"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("deadline");
        }

        @Test
        @DisplayName("should throw when editing on event date")
        void shouldThrowWhenEditingOnEventDate() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCard = SiCardNumber.of("123456");
            Event event = Event.reconstruct(
                    EventId.generate(), "Test Event", LocalDate.now(),
                    "Location", "Organizer",
                    null, null, null, EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(siCard).build())),
                    null
            );

            assertThatThrownBy(() -> event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("654321"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("on or after event date");
        }

        @Test
        @DisplayName("should throw when editing after event date")
        void shouldThrowWhenEditingAfterEventDate() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber siCard = SiCardNumber.of("123456");
            Event event = Event.reconstruct(
                    EventId.generate(), "Test Event", LocalDate.now().minusDays(1),
                    "Location", "Organizer",
                    null, null, null, EventStatus.ACTIVE, null,
                    List.of(),
                    List.of(EventRegistration.create(EventRegistrationCreateEventRegistrationBuilder.builder()
                            .memberId(memberId).siCardNumber(siCard).build())),
                    null
            );

            assertThatThrownBy(() -> event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("654321"))
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("on or after event date");
        }

        @Test
        @DisplayName("should emit RegistrationEditedEvent with old and new snapshots")
        void shouldEmitRegistrationEditedEvent() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            SiCardNumber originalSiCard = SiCardNumber.of("123456");
            Event event = activeEventWithRegisteredMember(memberId, originalSiCard);

            SiCardNumber newSiCard = SiCardNumber.of("654321");
            event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(newSiCard)
                    .build());

            var domainEvents = event.getDomainEvents();
            assertThat(domainEvents).hasSize(1);
            assertThat(domainEvents.get(0)).isInstanceOf(RegistrationEditedEvent.class);

            RegistrationEditedEvent emitted = (RegistrationEditedEvent) domainEvents.get(0);
            assertThat(emitted.eventId()).isEqualTo(event.getId());
            assertThat(emitted.memberId()).isEqualTo(memberId);
            assertThat(emitted.oldSiCardNumber()).isEqualTo(originalSiCard);
            assertThat(emitted.newSiCardNumber()).isEqualTo(newSiCard);
            assertThat(emitted.oldCategory()).isNull();
            assertThat(emitted.newCategory()).isNull();
        }
    }

    @Nested
    @DisplayName("editRegistration() – Skupina 2: kategorie")
    class EditRegistrationCategory {

        private static final List<String> CATEGORIES = List.of("M21", "W35");

        private Event activeEventWithCategories() {
            return EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(10))
                    .withCategories(CATEGORIES)
                    .buildPublished();
        }

        private Event activeEventWithCategoryAndRegisteredMember(MemberId memberId, String category) {
            Event event = activeEventWithCategories();
            event.clearDomainEvents();
            event.registerMember(memberId, SiCardNumber.of("123456"), category);
            event.clearDomainEvents();
            return event;
        }

        @Test
        @DisplayName("should update category for event with categories")
        void shouldUpdateCategoryForEventWithCategories() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = activeEventWithCategoryAndRegisteredMember(memberId, "M21");

            event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("123456"))
                    .category("W35")
                    .build());

            assertThat(event.findRegistration(memberId).get().category()).isEqualTo("W35");
        }

        @Test
        @DisplayName("should throw when category is null for event with categories")
        void shouldThrowWhenCategoryNullForEventWithCategories() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = activeEventWithCategoryAndRegisteredMember(memberId, "M21");

            assertThatThrownBy(() -> event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("123456"))
                    .category(null)
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Category is required");
        }

        @Test
        @DisplayName("should throw when category is blank for event with categories")
        void shouldThrowWhenCategoryBlankForEventWithCategories() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = activeEventWithCategoryAndRegisteredMember(memberId, "M21");

            assertThatThrownBy(() -> event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("123456"))
                    .category("   ")
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Category is required");
        }

        @Test
        @DisplayName("should throw when category is not in the event's category list")
        void shouldThrowWhenCategoryNotInEventList() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = activeEventWithCategoryAndRegisteredMember(memberId, "M21");

            assertThatThrownBy(() -> event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("123456"))
                    .category("H55")
                    .build()))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("not available");
        }

        @Test
        @DisplayName("should ignore provided category for event without categories")
        void shouldIgnoreProvidedCategoryForEventWithoutCategories() {
            MemberId memberId = new MemberId(UUID.randomUUID());
            Event event = EventTestDataBuilder.anEvent()
                    .withDate(LocalDate.now().plusDays(10))
                    .buildPublished();
            event.clearDomainEvents();
            event.registerMember(memberId, SiCardNumber.of("123456"), null);
            event.clearDomainEvents();

            event.editRegistration(memberId, EventEditRegistrationCommandBuilder.builder()
                    .siCardNumber(SiCardNumber.of("654321"))
                    .category("M21")
                    .build());

            assertThat(event.findRegistration(memberId).get().category()).isNull();
        }
    }
}
