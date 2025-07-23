package club.klabis.events.domain;

import club.klabis.events.domain.events.EventEditedEvent;
import club.klabis.events.domain.forms.EventEditationForm;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.domain.members.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.AggregatedRootTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests for Event class")
class EventTest {

    @Nested
    @DisplayName("edit method tests")
    class EditMethodTest {

        @Test
        @DisplayName("Should correctly update Event properties with given form")
        void shouldUpdateEventProperties() {
            // Arrange
            Event event = new Event();
            LocalDate testDate = LocalDate.of(2025, 7, 22);
            LocalDate registrationDeadline = LocalDate.of(2025, 7, 25);
            Member.Id coordinator = new Member.Id(1);
            EventEditationForm form = new EventEditationForm(
                    "Updated Event Name",
                    "Updated Location",
                    testDate,
                    "Updated Organizer",
                    registrationDeadline,
                    coordinator
            );

            // Act
            event.edit(form);

            // Assert
            assertThat(event.getName()).isEqualTo("Updated Event Name");
            assertThat(event.getLocation()).isEqualTo("Updated Location");
            assertThat(event.getDate()).isEqualTo(testDate);
            assertThat(event.getOrganizer()).isEqualTo("Updated Organizer");
            assertThat(event.getRegistrationDeadline()).isEqualTo(registrationDeadline);
            assertThat(event.getCoordinator()).contains(coordinator);
        }

        @Test
        @DisplayName("Should publish EventEditedEvent when edit is called")
        void shouldPublishEventEditedEvent() {
            // Arrange
            Event event = new Event();
            EventEditationForm form = new EventEditationForm(
                    "Test Event",
                    "Test Location",
                    LocalDate.of(2025, 7, 23),
                    "Test Organizer",
                    LocalDate.of(2025, 7, 24),
                    null
            );

            // Act
            event.edit(form);

            // Assert
            AggregatedRootTestUtils.assertThatDomainEventsOf(event)
                    .hasSize(1)
                    .first()
                    .isInstanceOf(EventEditedEvent.class);
        }
    }

    @Nested
    @DisplayName("addEventRegistration method tests")
    class AddEventRegistrationMethodTest {

        @Test
        @DisplayName("Should successfully add a registration when all conditions are met")
        void shouldAddEventRegistrationSuccessfully() {
            // Arrange
            Event event = Event.newEvent(new EventEditationForm(
                    "Event Name",
                    "Location",
                    LocalDate.now().plusDays(1),
                    "Organizer",
                    LocalDate.now().plusDays(1),
                    null
            ));
            Member.Id memberId = new Member.Id(1);
            EventRegistrationForm form = new EventRegistrationForm(memberId, "SI12345");

            // Act
            event.addEventRegistration(form);

            // Assert
            assertThat(event.getEventRegistrations()).contains(memberId);
        }

        @Test
        @DisplayName("Should throw exception when registering after the deadline")
        void shouldThrowExceptionAfterDeadline() {
            // Arrange
            Event event = Event.newEvent(new EventEditationForm(
                    "Event Name",
                    "Location",
                    LocalDate.now(),
                    "Organizer",
                    LocalDate.now().minusDays(1),
                    null
            ));
            Member.Id memberId = new Member.Id(1);
            EventRegistrationForm form = new EventRegistrationForm(memberId, "SI12345");

            // Act / Assert
            assertThatThrownBy(() -> event.addEventRegistration(form))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot add new registration to event, registrations are already closed");
        }

        @Test
        @DisplayName("Should throw exception when member is already registered")
        void shouldThrowExceptionForDuplicateRegistration() {
            // Arrange
            Event event = Event.newEvent(new EventEditationForm(
                    "Event Name",
                    "Location",
                    LocalDate.now().plusDays(1),
                    "Organizer",
                    LocalDate.now().plusDays(1),
                    null
            ));
            Member.Id memberId = new Member.Id(1);
            EventRegistrationForm form = new EventRegistrationForm(memberId, "SI12345");

            // Act
            event.addEventRegistration(form);

            // Assert
            assertThatThrownBy(() -> event.addEventRegistration(form))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("already signed up");
        }
    }

    @Nested
    @DisplayName("removeEventRegistration method tests")
    class RemoveEventRegistrationMethodTest {

        @Test
        @DisplayName("Should successfully remove a registration when all conditions are met")
        void shouldRemoveRegistrationSuccessfully() {
            // Arrange
            Event event = Event.newEvent(new EventEditationForm(
                    "Event Name",
                    "Location",
                    LocalDate.now().plusDays(1),
                    "Organizer",
                    LocalDate.now().plusDays(1),
                    null
            ));
            Member.Id memberId = new Member.Id(1);
            EventRegistrationForm form = new EventRegistrationForm(memberId, "SI12345");
            event.addEventRegistration(form);

            // Act
            event.removeEventRegistration(memberId);

            // Assert
            assertThat(event.getEventRegistrations()).doesNotContain(memberId);
        }

        @Test
        @DisplayName("Should throw exception when removing registration after the deadline")
        void shouldThrowExceptionAfterDeadline() {
            // Arrange
            Event event = Event.newEvent(new EventEditationForm(
                    "Event Name",
                    "Location",
                    LocalDate.now().plusDays(10),
                    "Organizer",
                    LocalDate.now().plusDays(4),
                    null
            ));
            Member.Id memberId = new Member.Id(1);
            EventRegistrationForm form = new EventRegistrationForm(memberId, "SI12345");
            event.addEventRegistration(form);
            event.closeRegistrations(LocalDate.now().minusDays(1));

            // Act / Assert
            assertThatThrownBy(() -> event.removeEventRegistration(memberId))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot remove registration from event, registrations are already closed");
        }

        @Test
        @DisplayName("Should throw exception when member is not registered")
        void shouldThrowExceptionWhenMemberNotRegistered() {
            // Arrange
            Event event = Event.newEvent(new EventEditationForm(
                    "Event Name",
                    "Location",
                    LocalDate.now().plusDays(1),
                    "Organizer",
                    LocalDate.now().plusDays(1),
                    null
            ));
            Member.Id memberId = new Member.Id(1);

            // Act / Assert
            assertThatThrownBy(() -> event.removeEventRegistration(memberId))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("NOT registered");
        }
    }
}