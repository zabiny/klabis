package club.klabis.events.domain;

import club.klabis.events.domain.forms.EventRegistrationForm;
import club.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static club.klabis.events.domain.DomainEventConditions.*;
import static club.klabis.events.domain.EventConditions.*;
import static club.klabis.events.domain.EventTestFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.AggregatedRootTestUtils.assertThatDomainEventsOf;

@DisplayName("Tests for Event class")
class EventTest {

    ZoneId ZONE_PRAGUE = ZoneId.of("Europe/Prague");

    @Nested
    @DisplayName("EventManagementCommand tests")
    class EditMethodTest {

        @Test
        @DisplayName("Should correctly update Event properties with given form")
        void shouldUpdateEventProperties() {
            // Arrange
            Event event = new Competition("Some", LocalDate.now());
            LocalDate testDate = LocalDate.of(2025, 7, 25);
            ZonedDateTime registrationDeadline = LocalDate.of(2025, 7, 22).atStartOfDay(ZONE_PRAGUE);
            MemberId coordinator = new MemberId(1);
            EventManagementCommand command = new EventManagementCommand(
                    "Updated Event Name",
                    "Updated Location",
                    testDate,
                    "Updated Organizer",
                    registrationDeadline,
                    coordinator, Competition.Category.categories("D12"), null
            );

            // Act
            event.apply(command);

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
    @DisplayName("registerMember method tests")
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
    @DisplayName("cancelMemberRegistration method tests")
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

    @Nested
    @DisplayName("setEventDate method tests")
    class SetEventDateTests {

        @Test
        @DisplayName("Should preserve deadline when moving event to later date")
        void shouldPreserveDeadlineWhenMovingEventToLaterDate() {
            // Arrange
            LocalDate initialDate = LocalDate.now();
            Competition competition = new Competition("Test Event", initialDate);
            // Registration deadline is auto-set to initialDate by constructor
            ZonedDateTime initialDeadline = competition.getRegistrationDeadline();
            clearDomainEvents(competition);

            LocalDate newDate = LocalDate.now().plusDays(10);

            // Act
            competition.setEventDate(newDate);

            // Assert
            assertThat(competition)
                    .has(hasEventDate(newDate));
            // Deadline should NOT be adjusted when moving event to a later date (after current deadline)
            assertThat(competition.getRegistrationDeadline())
                    .isEqualTo(initialDeadline);
        }

        @Test
        @DisplayName("Should adjust deadline when event date is moved before current deadline")
        void shouldAdjustDeadlineWhenEventDateMovedBeforeCurrentDeadline() {
            // Arrange
            LocalDate originalDate = LocalDate.now().plusDays(10);
            Competition competition = Competition.newEvent("Test", originalDate, DEFAULT_CATEGORIES);
            ZonedDateTime originalDeadline = club.klabis.shared.config.Globals.toZonedDateTime(originalDate.minusDays(2));
            competition.closeRegistrationsAt(originalDeadline);
            clearDomainEvents(competition);

            // Act - Move event to earlier date (before deadline)
            LocalDate newDate = originalDate.minusDays(5);
            competition.setEventDate(newDate);

            // Assert - Deadline should be adjusted to event start
            assertThat(competition.getRegistrationDeadline())
                    .isBeforeOrEqualTo(club.klabis.shared.config.Globals.toZonedDateTime(newDate));
        }

        @Test
        @DisplayName("Should NOT adjust deadline when event date is moved after current deadline")
        void shouldNotAdjustDeadlineWhenEventDateMovedAfterCurrentDeadline() {
            // Arrange
            LocalDate originalDate = LocalDate.now().plusDays(10);
            Competition competition = Competition.newEvent("Test", originalDate, DEFAULT_CATEGORIES);
            ZonedDateTime deadline = club.klabis.shared.config.Globals.toZonedDateTime(originalDate.minusDays(3));
            competition.closeRegistrationsAt(deadline);
            clearDomainEvents(competition);

            // Act - Move event to later date
            LocalDate newDate = originalDate.plusDays(5);
            competition.setEventDate(newDate);

            // Assert - Deadline should remain unchanged
            assertThat(competition.getRegistrationDeadline()).isEqualTo(deadline);
        }

        @Test
        @DisplayName("Should publish EventDateChangedEvent when date changes")
        void shouldPublishEventDateChangedEvent() {
            // Arrange
            Competition competition = createOpenCompetition();
            clearDomainEvents(competition);
            LocalDate newDate = LocalDate.now().plusDays(14);

            // Act
            competition.setEventDate(newDate);

            // Assert
            assertThatDomainEventsOf(competition, EventDateChangedEvent.class)
                    .hasSize(1)
                    .first()
                    .has(forEvent(competition.getId()));
        }

        @Test
        @DisplayName("Should throw exception when date is null")
        void shouldThrowExceptionWhenDateIsNull() {
            // Arrange
            Competition competition = createOpenCompetition();

            // Act & Assert
            assertThatThrownBy(() -> competition.setEventDate(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Event date must not be null");
        }
    }

    @Nested
    @DisplayName("closeRegistrations method tests")
    class CloseRegistrationsTests {

        @Test
        @DisplayName("Should successfully set registration deadline")
        void shouldSetRegistrationDeadline() {
            // Arrange
            Competition competition = createOpenCompetition();
            ZonedDateTime newDeadline = club.klabis.shared.config.Globals.toZonedDateTime(DEFAULT_EVENT_DATE.minusDays(1));

            // Act
            competition.closeRegistrationsAt(newDeadline);

            // Assert
            assertThat(competition).has(hasRegistrationDeadline(newDeadline));
        }

        @Test
        @DisplayName("Should publish EventRegistrationsDeadlineChangedEvent when deadline changes")
        void shouldPublishDeadlineChangedEvent() {
            // Arrange
            Competition competition = createOpenCompetition();
            clearDomainEvents(competition);
            ZonedDateTime newDeadline = club.klabis.shared.config.Globals.toZonedDateTime(DEFAULT_EVENT_DATE.minusDays(1));

            // Act
            competition.closeRegistrationsAt(newDeadline);

            // Assert
            assertThatDomainEventsOf(competition, EventRegistrationsDeadlineChangedEvent.class)
                    .hasSize(1)
                    .first()
                    .has(forEventDeadline(competition.getId()));
        }

        @Test
        @DisplayName("Should throw exception when deadline is after event start")
        void shouldThrowExceptionWhenDeadlineAfterEventStart() {
            // Arrange
            LocalDate eventDate = LocalDate.now().plusDays(7);
            Competition competition = Competition.newEvent("Test", eventDate, DEFAULT_CATEGORIES);
            ZonedDateTime invalidDeadline = club.klabis.shared.config.Globals.toZonedDateTime(eventDate.plusDays(1));

            // Act & Assert
            assertThatThrownBy(() -> competition.closeRegistrationsAt(invalidDeadline))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot set registration deadline after event start");
        }

        @Test
        @DisplayName("Should allow deadline equal to event start (boundary condition)")
        void shouldAllowDeadlineEqualToEventStart() {
            // Arrange
            LocalDate eventDate = LocalDate.now().plusDays(7);
            Competition competition = Competition.newEvent("Test", eventDate, DEFAULT_CATEGORIES);
            ZonedDateTime deadlineAtEventStart = club.klabis.shared.config.Globals.toZonedDateTime(eventDate);

            // Act & Assert - Should not throw
            assertThatCode(() -> competition.closeRegistrationsAt(deadlineAtEventStart))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("linkWithOris method tests")
    class LinkWithOrisTests {

        @Test
        @DisplayName("Should successfully link ORIS ID when not previously linked")
        void shouldLinkOrisIdWhenNotPreviouslyLinked() {
            // Arrange
            Competition competition = createOpenCompetition();
            OrisId orisId = DEFAULT_ORIS_ID;

            // Act
            Event result = competition.linkWithOris(orisId);

            // Assert
            assertThat(competition).has(hasOrisId(orisId));
            assertThat(result).isSameAs(competition); // Fluent API
        }

        @Test
        @DisplayName("Should be idempotent when linking with same ORIS ID")
        void shouldBeIdempotentWhenLinkingWithSameOrisId() {
            // Arrange
            Competition competition = createOpenCompetition();
            OrisId orisId = DEFAULT_ORIS_ID;
            competition.linkWithOris(orisId);

            // Act & Assert - Should not throw
            assertThatCode(() -> competition.linkWithOris(orisId))
                    .doesNotThrowAnyException();
            assertThat(competition).has(hasOrisId(orisId));
        }

        @Test
        @DisplayName("Should throw exception when relinking with different ORIS ID")
        void shouldThrowExceptionWhenRelinkingWithDifferentOrisId() {
            // Arrange
            Competition competition = createOpenCompetition();
            OrisId firstOrisId = new OrisId(111);
            OrisId secondOrisId = new OrisId(222);
            competition.linkWithOris(firstOrisId);

            // Act & Assert
            assertThatThrownBy(() -> competition.linkWithOris(secondOrisId))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Attempt to link event")
                    .hasMessageContaining("already assigned orisId");
        }

        @Test
        @DisplayName("Should return self for fluent chaining")
        void shouldReturnSelfForFluentChaining() {
            // Arrange
            Competition competition = createOpenCompetition();

            // Act
            Event result = competition.linkWithOris(DEFAULT_ORIS_ID);

            // Assert
            assertThat(result).isSameAs(competition);
        }
    }

    @Nested
    @DisplayName("updateCost method tests")
    class UpdateCostTests {

        @Test
        @DisplayName("Should update cost and publish EventCostChangedEvent")
        void shouldUpdateCostAndPublishEvent() {
            // Arrange
            Competition competition = createOpenCompetition();
            clearDomainEvents(competition);
            club.klabis.finance.domain.MoneyAmount newCost = club.klabis.finance.domain.MoneyAmount.of(new BigDecimal(
                    "200.00"));

            // Act
            competition.updateCost(newCost);

            // Assert
            assertThat(competition).has(hasCost(newCost));
            assertThatDomainEventsOf(competition, EventCostChangedEvent.class)
                    .hasSize(1)
                    .first()
                    .has(forEventCost(competition.getId()));
        }

        @Test
        @DisplayName("Should update from zero to non-zero cost")
        void shouldUpdateFromZeroToNonZero() {
            // Arrange
            Competition competition = createOpenCompetition();
            assertThat(competition).has(hasNoCost());

            // Act
            club.klabis.finance.domain.MoneyAmount newCost = club.klabis.finance.domain.MoneyAmount.of(new BigDecimal(
                    "100.00"));
            competition.updateCost(newCost);

            // Assert
            assertThat(competition).has(hasCost(newCost));
        }

        @Test
        @DisplayName("Should update from non-zero to zero cost")
        void shouldUpdateFromNonZeroToZero() {
            // Arrange
            Competition competition = createOpenCompetition();
            competition.updateCost(club.klabis.finance.domain.MoneyAmount.of(new BigDecimal("150.00")));

            // Act
            competition.updateCost(club.klabis.finance.domain.MoneyAmount.ZERO);

            // Assert
            assertThat(competition).has(hasNoCost());
        }

        @Test
        @DisplayName("Should handle null cost gracefully")
        void shouldHandleNullCost() {
            // Arrange
            Competition competition = createOpenCompetition();

            // Act
            competition.updateCost(null);

            // Assert
            assertThat(competition).has(hasNoCost());
        }
    }

    @Nested
    @DisplayName("changeRegistration method tests")
    class ChangeRegistrationTests {

        @Test
        @DisplayName("Should successfully update existing registration")
        void shouldUpdateExistingRegistration() {
            // Arrange
            Competition competition = createOpenCompetition();
            competition.registerMember(MEMBER_1, formForCategory(CATEGORY_D12));
            EventRegistrationForm newForm = createRegistrationForm(CATEGORY_H21.name(), "999999");

            // Act
            competition.changeRegistration(MEMBER_1, newForm);

            // Assert
            assertThat(competition.getRegistrationForMember(MEMBER_1))
                    .isPresent()
                    .get()
                    .extracting(Registration::getCategory, Registration::getSiNumber)
                    .containsExactly(CATEGORY_H21, "999999");
        }

        @Test
        @DisplayName("Should throw exception when attempting to change registration after deadline")
        void shouldThrowExceptionWhenRegistrationsClosed() {
            // Arrange
            Competition competition = createClosedCompetition();
            EventRegistrationForm newForm = defaultRegistrationForm();

            // Act & Assert
            assertThatThrownBy(() -> competition.changeRegistration(MEMBER_1, newForm))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("Cannot change registration for event, registrations are already closed");
        }

        @Test
        @DisplayName("Should throw exception when member is not registered")
        void shouldThrowExceptionWhenMemberNotRegistered() {
            // Arrange
            Competition competition = createOpenCompetition();
            EventRegistrationForm newForm = defaultRegistrationForm();

            // Act & Assert
            assertThatThrownBy(() -> competition.changeRegistration(MEMBER_1, newForm))
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("NOT registered");
        }
    }

    @Nested
    @DisplayName("areRegistrationsOpen method tests")
    class AreRegistrationsOpenTests {

        @Test
        @DisplayName("Should return true when deadline is in future")
        void shouldReturnTrueWhenDeadlineInFuture() {
            // Arrange
            Competition competition = createOpenCompetition();

            // Act & Assert
            assertThat(competition).has(hasRegistrationsOpen());
        }

        @Test
        @DisplayName("Should return false when deadline is in past")
        void shouldReturnFalseWhenDeadlineInPast() {
            // Arrange
            Competition competition = createClosedCompetition();

            // Act & Assert
            assertThat(competition).has(hasRegistrationsClosed());
        }

        @Test
        @DisplayName("Should return false when deadline is now (boundary condition)")
        void shouldReturnFalseWhenDeadlineIsNow() {
            // Arrange
            LocalDate eventDate = LocalDate.now().plusDays(1);
            Competition competition = Competition.newEvent("Test", eventDate, DEFAULT_CATEGORIES);
            ZonedDateTime deadlineNow = ZonedDateTime.now().minusSeconds(1);
            competition.closeRegistrationsAt(deadlineNow);

            // Act & Assert
            assertThat(competition.areRegistrationsOpen()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCost method tests")
    class GetCostTests {

        @Test
        @DisplayName("Should return empty when cost is null")
        void shouldReturnEmptyWhenCostIsNull() {
            // Arrange
            Competition competition = createOpenCompetition();
            competition.updateCost(null);

            // Act & Assert
            assertThat(competition).has(hasNoCost());
        }

        @Test
        @DisplayName("Should return empty when cost is zero (per TODO comment)")
        void shouldReturnEmptyWhenCostIsZero() {
            // Arrange
            Competition competition = createOpenCompetition();
            competition.updateCost(club.klabis.finance.domain.MoneyAmount.ZERO);

            // Act & Assert
            assertThat(competition).has(hasNoCost());
        }

        @Test
        @DisplayName("Should return cost when non-zero")
        void shouldReturnCostWhenNonZero() {
            // Arrange
            club.klabis.finance.domain.MoneyAmount cost = club.klabis.finance.domain.MoneyAmount.of(new BigDecimal(
                    "150.00"));
            Competition competition = createOpenCompetition();
            competition.updateCost(cost);

            // Act & Assert
            assertThat(competition.getCost())
                    .isPresent()
                    .contains(cost);
        }
    }

    @Nested
    @DisplayName("Thread-safety tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should generate unique IDs in concurrent environment")
        void shouldGenerateUniqueIdsInConcurrentEnvironment() throws Exception {
            // Arrange
            int threadCount = 100;
            int idsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            Set<Event.Id> allIds = Collections.synchronizedSet(new HashSet<>());

            // Act - Create events concurrently
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < idsPerThread; j++) {
                        Competition competition = new Competition("Test", LocalDate.now().plusDays(1));
                        allIds.add(competition.getId());
                    }
                }));
            }

            // Wait for all threads to complete
            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdown();

            // Assert - All IDs should be unique
            assertThat(allIds).hasSize(threadCount * idsPerThread);
        }
    }
}