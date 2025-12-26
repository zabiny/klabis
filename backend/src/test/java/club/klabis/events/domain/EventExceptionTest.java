package club.klabis.events.domain;

import club.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static club.klabis.events.domain.EventTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests for EventException domain exception")
class EventExceptionTest {

    @Nested
    @DisplayName("Factory method tests")
    class FactoryMethodTests {

        @Test
        @DisplayName("Should create AlreadySignedUpException with correct message and type")
        void shouldCreateAlreadySignedUpException() {
            // Arrange
            Event.Id eventId = new Event.Id(123);
            MemberId memberId = MEMBER_1;

            // Act
            EventException exception = EventException.createAlreadySignedUpException(eventId, memberId);

            // Assert
            assertThat(exception)
                    .isNotNull()
                    .isInstanceOf(EventException.class)
                    .hasMessageContaining("already signed up")
                    .hasMessageContaining(String.valueOf(memberId.value()))
                    .hasMessageContaining(String.valueOf(eventId.value()));
            assertThat(exception.getType()).isEqualTo(EventException.Type.MEMBER_ALREADY_REGISTERED);
            assertThat(exception.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("Should create MemberNotRegisteredException with correct message and type")
        void shouldCreateMemberNotRegisteredException() {
            // Arrange
            Event.Id eventId = new Event.Id(456);
            MemberId memberId = MEMBER_2;

            // Act
            EventException exception = EventException.createMemberNotRegisteredForEventException(eventId, memberId);

            // Assert
            assertThat(exception)
                    .isNotNull()
                    .hasMessageContaining("NOT registered")
                    .hasMessageContaining(String.valueOf(memberId.value()))
                    .hasMessageContaining(String.valueOf(eventId.value()));
            assertThat(exception.getType()).isEqualTo(EventException.Type.MEMBER_NOT_REGISTERED);
            assertThat(exception.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("Should create EventNotFoundException with correct message and type")
        void shouldCreateEventNotFoundException() {
            // Arrange
            Event.Id eventId = new Event.Id(789);

            // Act
            EventException exception = EventException.createEventNotFoundException(eventId);

            // Assert
            assertThat(exception)
                    .isNotNull()
                    .hasMessageContaining("was not found")
                    .hasMessageContaining(String.valueOf(eventId.value()));
            assertThat(exception.getType()).isEqualTo(EventException.Type.EVENT_NOT_FOUND);
            assertThat(exception.getEventId()).isEqualTo(eventId);
        }

        @Test
        @DisplayName("Should create CategoriesUpdateRejectedException with correct message and type")
        void shouldCreateCategoriesUpdateRejectedException() {
            // Arrange
            Event.Id eventId = new Event.Id(111);
            Set<Competition.Category> blockedCategories = Set.of(CATEGORY_D12, CATEGORY_H21);

            // Act
            EventException exception = EventException.createCategoriesUpdateRejectedException(eventId,
                    blockedCategories);

            // Assert
            assertThat(exception)
                    .isNotNull()
                    .hasMessageContaining("Cannot remove categories")
                    .hasMessageContaining("used in registrations");
            assertThat(exception.getType()).isEqualTo(EventException.Type.BLOCKED_CATEGORIES);
            assertThat(exception.getEventId()).isEqualTo(eventId);
        }
    }

    @Nested
    @DisplayName("Message content tests")
    class MessageContentTests {

        @Test
        @DisplayName("Should include event ID in exception message")
        void shouldIncludeEventIdInMessage() {
            // Arrange
            Event.Id eventId = new Event.Id(999);

            // Act
            EventException exception = EventException.createEventNotFoundException(eventId);

            // Assert
            assertThat(exception.getMessage()).contains(String.valueOf(eventId.value()));
        }

        @Test
        @DisplayName("Should include member ID in exception message")
        void shouldIncludeMemberIdInMessage() {
            // Arrange
            Event.Id eventId = new Event.Id(123);
            MemberId memberId = new MemberId(555);

            // Act
            EventException exception = EventException.createAlreadySignedUpException(eventId, memberId);

            // Assert
            assertThat(exception.getMessage()).contains(String.valueOf(memberId.value()));
        }

        @Test
        @DisplayName("Should include blocked categories in exception message")
        void shouldIncludeBlockedCategoriesInMessage() {
            // Arrange
            Event.Id eventId = new Event.Id(123);
            Set<Competition.Category> blockedCategories = Set.of(CATEGORY_D12, CATEGORY_H21);

            // Act
            EventException exception = EventException.createCategoriesUpdateRejectedException(eventId,
                    blockedCategories);

            // Assert
            assertThat(exception.getMessage())
                    .contains(CATEGORY_D12.name())
                    .contains(CATEGORY_H21.name());
        }

        @Test
        @DisplayName("Should format message correctly with event and member IDs")
        void shouldFormatMessageCorrectly() {
            // Arrange
            Event.Id eventId = new Event.Id(100);
            MemberId memberId = new MemberId(200);

            // Act
            EventException exception = EventException.createMemberNotRegisteredForEventException(eventId, memberId);

            // Assert
            String expectedMessage = "Member with ID '200' is NOT registered to event with ID '100'";
            assertThat(exception.getMessage()).isEqualTo(expectedMessage);
        }
    }
}
