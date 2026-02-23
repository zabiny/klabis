package com.klabis.events;

import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
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
            WebsiteUrl websiteUrl = WebsiteUrl.of("https://example.com/race");
            UserId coordinatorId = new UserId(UUID.randomUUID());

            // Act
            Event event = Event.create(name, eventDate, location, organizer, websiteUrl, coordinatorId);

            // Assert
            EventAssert.assertThat(event)
                    .hasIdNotNull()
                    .hasName(name)
                    .hasDate(eventDate)
                    .hasLocation(location)
                    .hasOrganizer(organizer)
                    .hasWebsiteUrl(websiteUrl)
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
            Event event = Event.create(name, eventDate, location, organizer, null, null);

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
            // Arrange
            LocalDate eventDate = LocalDate.of(2025, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    null,
                    eventDate,
                    "Location",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when name is blank")
        void shouldFailWhenNameIsBlank() {
            // Arrange
            LocalDate eventDate = LocalDate.of(2025, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    "   ",
                    eventDate,
                    "Location",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail when eventDate is null")
        void shouldFailWhenEventDateIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    "Event Name",
                    null,
                    "Location",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventDate");
        }

        @Test
        @DisplayName("should fail when location is null")
        void shouldFailWhenLocationIsNull() {
            // Arrange
            LocalDate eventDate = LocalDate.of(2025, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    "Event Name",
                    eventDate,
                    null,
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("location");
        }

        @Test
        @DisplayName("should fail when location is blank")
        void shouldFailWhenLocationIsBlank() {
            // Arrange
            LocalDate eventDate = LocalDate.of(2025, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    "Event Name",
                    eventDate,
                    "   ",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("location");
        }

        @Test
        @DisplayName("should fail when organizer is null")
        void shouldFailWhenOrganizerIsNull() {
            // Arrange
            LocalDate eventDate = LocalDate.of(2025, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    "Event Name",
                    eventDate,
                    "Location",
                    null,
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }

        @Test
        @DisplayName("should fail when organizer is blank")
        void shouldFailWhenOrganizerIsBlank() {
            // Arrange
            LocalDate eventDate = LocalDate.of(2025, 6, 15);

            // Act & Assert
            assertThatThrownBy(() -> Event.create(
                    "Event Name",
                    eventDate,
                    "Location",
                    "   ",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }
    }

    @Nested
    @DisplayName("Lifecycle transitions")
    class LifecycleTransitions {

        @Test
        @DisplayName("should publish event: DRAFT → ACTIVE")
        void shouldPublishEventFromDraftToActive() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            // Act
            event.publish();

            // Assert
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);
        }

        @Test
        @DisplayName("should cancel event: DRAFT → CANCELLED")
        void shouldCancelEventFromDraft() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            // Act
            event.cancel();

            // Assert
            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("should cancel event: ACTIVE → CANCELLED")
        void shouldCancelEventFromActive() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            // Act
            event.cancel();

            // Assert
            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("should finish event: ACTIVE → FINISHED")
        void shouldFinishEventFromActive() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            // Act
            event.finish();

            // Assert
            EventAssert.assertThat(event).hasStatus(EventStatus.FINISHED);
        }

        @Test
        @DisplayName("should fail to finish event from DRAFT")
        void shouldFailToFinishEventFromDraft() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            // Act & Assert
            assertThatThrownBy(() -> event.finish())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from DRAFT to FINISHED");
        }

        @Test
        @DisplayName("should allow idempotent publish (ACTIVE to ACTIVE)")
        void shouldAllowIdempotentPublish() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            // Act - calling publish() again should be idempotent (no exception)
            event.publish();

            // Assert - status remains ACTIVE
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);
        }

        @ParameterizedTest(name = "{3}")
        @MethodSource("illegalTransitions")
        @DisplayName("should reject illegal state transition")
        void shouldRejectIllegalStateTransition(Consumer<Event> setup, Consumer<Event> action, String expectedMessage, String displayName) {
            // Arrange
            Event event = EventTestDataBuilder.anEvent().build();
            setup.accept(event);

            // Act & Assert
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
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            String newName = "Updated Event";
            LocalDate newDate = LocalDate.of(2025, 7, 20);
            String newLocation = "Updated Location";
            String newOrganizer = "Updated Organizer";
            WebsiteUrl newWebsiteUrl = WebsiteUrl.of("https://updated.com");
            UserId newCoordinatorId = new UserId(UUID.randomUUID());

            // Act
            event.update(newName, newDate, newLocation, newOrganizer, newWebsiteUrl, newCoordinatorId);

            // Assert
            EventAssert.assertThat(event)
                    .hasName(newName)
                    .hasDate(newDate)
                    .hasLocation(newLocation)
                    .hasOrganizer(newOrganizer)
                    .hasWebsiteUrl(newWebsiteUrl)
                    .hasEventCoordinatorId(newCoordinatorId)
                    .hasStatus(EventStatus.DRAFT);
        }

        @Test
        @DisplayName("should update event in ACTIVE status")
        void shouldUpdateEventInActiveStatus() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );
            event.publish();
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            String newName = "Updated Event";
            LocalDate newDate = LocalDate.of(2025, 7, 20);
            String newLocation = "Updated Location";
            String newOrganizer = "Updated Organizer";

            // Act
            event.update(newName, newDate, newLocation, newOrganizer, null, null);

            // Assert
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
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );
            event.publish();
            event.finish();
            EventAssert.assertThat(event).hasStatus(EventStatus.FINISHED);

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "New Name",
                    LocalDate.of(2025, 7, 20),
                    "New Location",
                    "New Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot update event in FINISHED status");
        }

        @Test
        @DisplayName("should fail to update event in CANCELLED status")
        void shouldFailToUpdateEventInCancelledStatus() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );
            event.cancel();
            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "New Name",
                    LocalDate.of(2025, 7, 20),
                    "New Location",
                    "New Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot update event in CANCELLED status");
        }

        @Test
        @DisplayName("should fail to update with null name")
        void shouldFailToUpdateWithNullName() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    null,
                    LocalDate.of(2025, 7, 20),
                    "Location",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with blank name")
        void shouldFailToUpdateWithBlankName() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "   ",
                    LocalDate.of(2025, 7, 20),
                    "Location",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name");
        }

        @Test
        @DisplayName("should fail to update with null eventDate")
        void shouldFailToUpdateWithNullEventDate() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "Event Name",
                    null,
                    "Location",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("eventDate");
        }

        @Test
        @DisplayName("should fail to update with null location")
        void shouldFailToUpdateWithNullLocation() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "Event Name",
                    LocalDate.of(2025, 7, 20),
                    null,
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("location");
        }

        @Test
        @DisplayName("should fail to update with blank location")
        void shouldFailToUpdateWithBlankLocation() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "Event Name",
                    LocalDate.of(2025, 7, 20),
                    "   ",
                    "Organizer",
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("location");
        }

        @Test
        @DisplayName("should fail to update with null organizer")
        void shouldFailToUpdateWithNullOrganizer() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "Event Name",
                    LocalDate.of(2025, 7, 20),
                    "Location",
                    null,
                    null,
                    null
            ))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizer");
        }

        @Test
        @DisplayName("should fail to update with blank organizer")
        void shouldFailToUpdateWithBlankOrganizer() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 6, 15),
                    "Original Location",
                    "Original Organizer",
                    null,
                    null
            );

            // Act & Assert
            assertThatThrownBy(() -> event.update(
                    "Event Name",
                    LocalDate.of(2025, 7, 20),
                    "Location",
                    "   ",
                    null,
                    null
            ))
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
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish(); // Make event ACTIVE
            EventAssert.assertThat(event).hasStatus(EventStatus.ACTIVE);

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            // Act
            event.registerMember(memberId, siCardNumber);

            // Assert
            assertThat(event.getRegistrations()).hasSize(1);
            assertThat(event.findRegistration(memberId)).isPresent();
        }

        @Test
        @DisplayName("should fail to register member when event is DRAFT")
        void shouldFailToRegisterMemberWhenEventIsDraft() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            EventAssert.assertThat(event).hasStatus(EventStatus.DRAFT);

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            // Act & Assert
            assertThatThrownBy(() -> event.registerMember(memberId, siCardNumber))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration is only allowed for ACTIVE events");
        }

        @Test
        @DisplayName("should fail to register member when event is FINISHED")
        void shouldFailToRegisterMemberWhenEventIsFinished() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();
            event.finish();
            EventAssert.assertThat(event).hasStatus(EventStatus.FINISHED);

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            // Act & Assert
            assertThatThrownBy(() -> event.registerMember(memberId, siCardNumber))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration is only allowed for ACTIVE events");
        }

        @Test
        @DisplayName("should fail to register member when event is CANCELLED")
        void shouldFailToRegisterMemberWhenEventIsCancelled() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.cancel();
            EventAssert.assertThat(event).hasStatus(EventStatus.CANCELLED);

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");

            // Act & Assert
            assertThatThrownBy(() -> event.registerMember(memberId, siCardNumber))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Registration is only allowed for ACTIVE events");
        }

        @Test
        @DisplayName("should prevent duplicate registration for same member")
        void shouldPreventDuplicateRegistrationForSameMember() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber);

            // Act & Assert
            assertThatThrownBy(() -> event.registerMember(memberId, SiCardNumber.of("654321")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Duplicate registration not allowed");
        }

        @Test
        @DisplayName("should unregister member before event date")
        void shouldUnregisterMemberBeforeEventDate() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber);
            assertThat(event.getRegistrations()).hasSize(1);

            LocalDate currentDate = LocalDate.of(2025, 7, 9); // One day before event

            // Act
            event.unregisterMember(memberId, currentDate);

            // Assert
            assertThat(event.getRegistrations()).isEmpty();
            assertThat(event.findRegistration(memberId)).isEmpty();
        }

        @Test
        @DisplayName("should fail to unregister member on event date")
        void shouldFailToUnregisterMemberOnEventDate() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber);

            LocalDate currentDate = LocalDate.of(2025, 7, 10); // Same as event date

            // Act & Assert
            assertThatThrownBy(() -> event.unregisterMember(memberId, currentDate))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot unregister on or after event date");
        }

        @Test
        @DisplayName("should fail to unregister member after event date")
        void shouldFailToUnregisterMemberAfterEventDate() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber);

            LocalDate currentDate = LocalDate.of(2025, 7, 11); // One day after event

            // Act & Assert
            assertThatThrownBy(() -> event.unregisterMember(memberId, currentDate))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Cannot unregister on or after event date");
        }

        @Test
        @DisplayName("should fail to unregister member that is not registered")
        void shouldFailToUnregisterMemberThatIsNotRegistered() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());
            LocalDate currentDate = LocalDate.of(2025, 7, 9);

            // Act & Assert
            assertThatThrownBy(() -> event.unregisterMember(memberId, currentDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Member is not registered for this event");
        }

        @Test
        @DisplayName("should find registration by memberId when registered")
        void shouldFindRegistrationByMemberIdWhenRegistered() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber = SiCardNumber.of("123456");
            event.registerMember(memberId, siCardNumber);

            // Act
            var foundRegistration = event.findRegistration(memberId);

            // Assert
            assertThat(foundRegistration).isPresent();
            assertThat(foundRegistration.get().memberId()).isEqualTo(memberId);
            assertThat(foundRegistration.get().siCardNumber()).isEqualTo(siCardNumber);
        }

        @Test
        @DisplayName("should return empty when finding non-existent registration")
        void shouldReturnEmptyWhenFindingNonExistentRegistration() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId = new UserId(UUID.randomUUID());

            // Act
            var foundRegistration = event.findRegistration(memberId);

            // Assert
            assertThat(foundRegistration).isEmpty();
        }

        @Test
        @DisplayName("should return unmodifiable list of registrations")
        void shouldReturnUnmodifiableListOfRegistrations() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();

            UserId memberId1 = new UserId(UUID.randomUUID());
            UserId memberId2 = new UserId(UUID.randomUUID());
            SiCardNumber siCardNumber1 = SiCardNumber.of("123456");
            SiCardNumber siCardNumber2 = SiCardNumber.of("654321");

            event.registerMember(memberId1, siCardNumber1);
            event.registerMember(memberId2, siCardNumber2);

            // Act
            var registrations = event.getRegistrations();

            // Assert
            assertThat(registrations).hasSize(2);

            // Verify list is unmodifiable
            assertThatThrownBy(() -> registrations.add(EventRegistration.create(new UserId(UUID.randomUUID()),
                    SiCardNumber.of("111111"))))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Domain Events (getDomainEvents, clearDomainEvents)")
    class DomainEvents {

        @Test
        @DisplayName("should register EventCreatedEvent when event is created")
        void shouldRegisterEventCreatedEventWhenCreated() {
            // Arrange & Act
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    WebsiteUrl.of("https://test.com"),
                    new UserId(UUID.randomUUID())
            );

            // Assert
            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventCreatedEvent.class);

            EventCreatedEvent createdEvent = (EventCreatedEvent) domainEvents.get(0);
            assertThat(createdEvent.getEventId()).isEqualTo(event.getId());
            assertThat(createdEvent.getName()).isEqualTo("Test Event");
            assertThat(createdEvent.getEventDate()).isEqualTo(LocalDate.of(2025, 7, 10));
            assertThat(createdEvent.getLocation()).isEqualTo("Test Location");
            assertThat(createdEvent.getOrganizer()).isEqualTo("Test Organizer");
        }

        @Test
        @DisplayName("should register EventPublishedEvent when event is published")
        void shouldRegisterEventPublishedEventWhenPublished() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.clearDomainEvents(); // Clear creation event

            // Act
            event.publish();

            // Assert
            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventPublishedEvent.class);

            EventPublishedEvent publishedEvent = (EventPublishedEvent) domainEvents.get(0);
            assertThat(publishedEvent.getEventId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("should register EventCancelledEvent when event is cancelled")
        void shouldRegisterEventCancelledEventWhenCancelled() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.clearDomainEvents(); // Clear creation event

            // Act
            event.cancel();

            // Assert
            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventCancelledEvent.class);

            EventCancelledEvent cancelledEvent = (EventCancelledEvent) domainEvents.get(0);
            assertThat(cancelledEvent.getEventId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("should register EventFinishedEvent when event is finished")
        void shouldRegisterEventFinishedEventWhenFinished() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            event.publish();
            event.clearDomainEvents(); // Clear previous events

            // Act
            event.finish();

            // Assert
            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventFinishedEvent.class);

            EventFinishedEvent finishedEvent = (EventFinishedEvent) domainEvents.get(0);
            assertThat(finishedEvent.getEventId()).isEqualTo(event.getId());
        }

        @Test
        @DisplayName("should clear domain events after clearDomainEvents is called")
        void shouldClearDomainEventsAfterClear() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );
            assertThat(event.getDomainEvents()).hasSize(1);

            // Act
            event.clearDomainEvents();

            // Assert
            assertThat(event.getDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("should register EventUpdatedEvent when event is updated")
        void shouldRegisterEventUpdatedEventWhenUpdated() {
            // Arrange
            Event event = Event.create(
                    "Original Event",
                    LocalDate.of(2025, 7, 10),
                    "Original Location",
                    "Original Organizer",
                    WebsiteUrl.of("https://original.com"),
                    null
            );
            event.clearDomainEvents(); // Clear creation event

            // Act
            event.update(
                    "Updated Event",
                    LocalDate.of(2025, 7, 15),
                    "Updated Location",
                    "Updated Organizer",
                    WebsiteUrl.of("https://updated.com"),
                    null
            );

            // Assert
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
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    WebsiteUrl.of("https://test.com"),
                    null
            );
            event.clearDomainEvents();

            // Act
            event.update(
                    "Updated Event",
                    LocalDate.of(2025, 7, 15),
                    "Updated Location",
                    "Updated Organizer",
                    null, // No website URL
                    null
            );

            // Assert
            List<Object> domainEvents = event.getDomainEvents();
            EventUpdatedEvent updatedEvent = (EventUpdatedEvent) domainEvents.get(0);
            assertThat(updatedEvent.websiteUrl()).isNull();
        }

        @Test
        @DisplayName("should accumulate multiple domain events")
        void shouldAccumulateMultipleDomainEvents() {
            // Arrange
            Event event = Event.create(
                    "Test Event",
                    LocalDate.of(2025, 7, 10),
                    "Test Location",
                    "Test Organizer",
                    null,
                    null
            );

            // Act
            event.publish(); // Adds EventPublishedEvent

            // Assert
            List<Object> domainEvents = event.getDomainEvents();
            assertThat(domainEvents).hasSize(2);
            assertThat(domainEvents.get(0)).isInstanceOf(EventCreatedEvent.class);
            assertThat(domainEvents.get(1)).isInstanceOf(EventPublishedEvent.class);
        }
    }
}
