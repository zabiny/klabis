package club.klabis.events.domain;

import club.klabis.events.application.EventManagementForm;
import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests for Event class")
class EventTest {

    ZoneId ZONE_PRAGUE = ZoneId.of("Europe/Prague");

    @Nested
    @DisplayName("edit method tests")
    class EditMethodTest {

        @Test
        @DisplayName("Should correctly update Event properties with given form")
        void shouldUpdateEventProperties() {
            // Arrange
            Event event = new Competition("Some", LocalDate.now());
            LocalDate testDate = LocalDate.of(2025, 7, 25);
            ZonedDateTime registrationDeadline = LocalDate.of(2025, 7, 22).atStartOfDay(ZONE_PRAGUE);
            MemberId coordinator = new MemberId(1);
            EventManagementForm form = new EventManagementForm(
                    "Updated Event Name",
                    "Updated Location",
                    testDate,
                    "Updated Organizer",
                    registrationDeadline,
                    coordinator, Competition.Category.categories("D12"), null
            );

            // Act
            form.apply(event);

            // Assert
            assertThat(event.getName()).isEqualTo("Updated Event Name");
            assertThat(event.getLocation()).isEqualTo("Updated Location");
            assertThat(event.getDate()).isEqualTo(testDate);
            assertThat(event.getOrganizer()).isEqualTo("Updated Organizer");
            assertThat(event.getRegistrationDeadline()).isEqualTo(registrationDeadline);
            assertThat(event.getCoordinator()).contains(coordinator);
        }

        // TODO: any domain events after these changes in Event?
    }

    @Nested
    @DisplayName("addEventRegistration method tests")
    class AddEventRegistrationMethodTest {

        @Test
        @DisplayName("Should successfully add a registration when all conditions are met")
        void shouldRegisterMemberSuccessfully() {
            // Arrange
            Event event = Competition.newEvent("Event Name",
                    LocalDate.now().plusDays(1), Competition.Category.categories("D12"));
            MemberId memberId = new MemberId(1);
            EventRegistrationForm form = new EventRegistrationForm("SI12345", "D12");

            // Act
            event.registerMember(memberId, form);

            // Assert
            assertThat(event.getEventRegistrations()).contains(new Registration(memberId, "SI12345", "D12"));
        }

        @Test
        @DisplayName("Should throw exception when registering after the deadline")
        void shouldThrowExceptionAfterDeadline() {
            // Arrange
            Event event = Competition.newEvent("Event Name",
                    LocalDate.now().minusDays(1), Competition.Category.categories("D12"));
            MemberId memberId = new MemberId(1);
            EventRegistrationForm form = new EventRegistrationForm("SI12345", "P");

            // Act / Assert
            assertThatThrownBy(() -> event.registerMember(memberId, form))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot add new registration to event, registrations are already closed");
        }

        @Test
        @DisplayName("Should throw exception when member is already registered")
        void shouldThrowExceptionForDuplicateRegistration() {
            // Arrange
            Event event = Competition.newEvent("Event Name",
                    LocalDate.now().plusDays(1), Competition.Category.categories("D12"));
            MemberId memberId = new MemberId(1);
            EventRegistrationForm form = new EventRegistrationForm("SI12345", "P");

            // Act
            event.registerMember(memberId, form);

            // Assert
            assertThatThrownBy(() -> event.registerMember(memberId, form))
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
            Event event = Competition.newEvent("Event Name",
                    LocalDate.now().plusDays(1), Competition.Category.categories("D12"));
            MemberId memberId = new MemberId(1);
            EventRegistrationForm form = new EventRegistrationForm("SI12345", "H21");
            event.registerMember(memberId, form);

            // Act
            event.cancelMemberRegistration(memberId);

            // Assert
            assertThat(event.getEventRegistrations()).doesNotContain(new Registration(memberId, "SI12345", "H21"));
        }

        @Test
        @DisplayName("Should throw exception when removing registration after the deadline")
        void shouldThrowExceptionAfterDeadline() {
            // Arrange
            Event event = Competition.newEvent("Event Name",
                    LocalDate.now().plusDays(1), Competition.Category.categories("D12"));
            MemberId memberId = new MemberId(1);
            EventRegistrationForm form = new EventRegistrationForm("SI12345", "P");
            event.registerMember(memberId, form);
            event.closeRegistrationsAt(LocalDate.now().minusDays(1).atStartOfDay(ZONE_PRAGUE));

            // Act / Assert
            assertThatThrownBy(() -> event.cancelMemberRegistration(memberId))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot remove registration from event, registrations are already closed");
        }

        @Test
        @DisplayName("Should throw exception when member is not registered")
        void shouldThrowExceptionWhenMemberNotRegistered() {
            // Arrange
            Event event = Competition.newEvent("Event Name",
                    LocalDate.now().plusDays(1), Competition.Category.categories("D12"));
            MemberId memberId = new MemberId(1);

            // Act / Assert
            assertThatThrownBy(() -> event.cancelMemberRegistration(memberId))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("NOT registered");
        }
    }
}