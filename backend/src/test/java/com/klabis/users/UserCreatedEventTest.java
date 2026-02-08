package com.klabis.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserCreatedEvent tests")
class UserCreatedEventTest {

    private static final UUID TEST_USER_ID = UUID.randomUUID();
    private static final String TEST_USERNAME = "ZBM9001";
    private static final AccountStatus TEST_STATUS = AccountStatus.PENDING_ACTIVATION;

    @Nested
    @DisplayName("Constructor with generated eventId")
    class ConstructorWithGeneratedEventId {

        @Test
        @DisplayName("should create event with generated ID and current timestamp")
        void shouldCreateEventWithGeneratedIdAndTimestamp() {
            // When
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS
            );

            // Then
            assertThat(event.getEventId()).isNotNull();
            assertThat(event.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(event.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(event.getAccountStatus()).isEqualTo(TEST_STATUS);
            assertThat(event.getOccurredAt()).isNotNull();
            assertThat(event.getOccurredAt()).isBefore(Instant.now().plusMillis(100)); // Within last 100ms
        }

        @Test
        @DisplayName("should generate unique event IDs")
        void shouldGenerateUniqueEventIds() {
            // When
            UserCreatedEvent event1 = new UserCreatedEvent(TEST_USER_ID, TEST_USERNAME, TEST_STATUS);
            UserCreatedEvent event2 = new UserCreatedEvent(TEST_USER_ID, TEST_USERNAME, TEST_STATUS);

            // Then
            assertThat(event1.getEventId()).isNotEqualTo(event2.getEventId());
        }
    }

    @Nested
    @DisplayName("Constructor with explicit eventId")
    class ConstructorWithExplicitEventId {

        private static final UUID TEST_EVENT_ID = UUID.randomUUID();
        private static final Instant TEST_TIMESTAMP = Instant.parse("2025-01-25T10:00:00Z");

        @Test
        @DisplayName("should create event with explicit values")
        void shouldCreateEventWithExplicitValues() {
            // When
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_EVENT_ID,
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    TEST_TIMESTAMP
            );

            // Then
            assertThat(event.getEventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(event.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(event.getAccountStatus()).isEqualTo(TEST_STATUS);
            assertThat(event.getOccurredAt()).isEqualTo(TEST_TIMESTAMP);
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowExceptionForNullEventId() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    null,
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Event ID is required");
        }

        @Test
        @DisplayName("should throw exception for null userId")
        void shouldThrowExceptionForNullUserId() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    UUID.randomUUID(),
                    null,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("User ID is required");
        }

        @Test
        @DisplayName("should throw exception for null username")
        void shouldThrowExceptionForNullUsername() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    null,
                    TEST_STATUS,
                    Instant.now()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Username is required");
        }

        @Test
        @DisplayName("should throw exception for null accountStatus")
        void shouldThrowExceptionForNullAccountStatus() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    TEST_USERNAME,
                    null,
                    Instant.now()
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Account status is required");
        }

        @Test
        @DisplayName("should throw exception for null occurredAt")
        void shouldThrowExceptionForNullOccurredAt() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Occurred at timestamp is required");
        }
    }

    @Nested
    @DisplayName("fromUser() factory method")
    class FromUserFactoryMethod {

        @Test
        @DisplayName("should create event from User aggregate")
        void shouldCreateEventFromUser() {
            // Given
            UserId userId = new UserId(TEST_USER_ID);
            User user = User.reconstruct(
                    userId,
                    TEST_USERNAME,
                    "hashedPassword",
                    AccountStatus.PENDING_ACTIVATION,
                    true,
                    true,
                    true,
                    true
            );

            // When
            UserCreatedEvent event = UserCreatedEvent.fromUser(user);

            // Then
            assertThat(event.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(event.getUsername()).isEqualTo(TEST_USERNAME);
            assertThat(event.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
            assertThat(event.getEventId()).isNotNull(); // Generated
            assertThat(event.getOccurredAt()).isNotNull(); // Current time
        }
    }

    @Nested
    @DisplayName("isPendingActivation() method")
    class IsPendingActivationMethod {

        @Test
        @DisplayName("should return true when account status is PENDING_ACTIVATION")
        void shouldReturnTrueWhenPendingActivation() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.PENDING_ACTIVATION
            );

            assertThat(event.isPendingActivation()).isTrue();
        }

        @Test
        @DisplayName("should return false when account status is ACTIVE")
        void shouldReturnFalseWhenActive() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.ACTIVE
            );

            assertThat(event.isPendingActivation()).isFalse();
        }

        @Test
        @DisplayName("should return false when account status is SUSPENDED")
        void shouldReturnFalseWhenSuspended() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.SUSPENDED
            );

            assertThat(event.isPendingActivation()).isFalse();
        }
    }

    @Nested
    @DisplayName("Equality methods")
    class EqualityMethods {

        @Test
        @DisplayName("should be equal based on eventId")
        void shouldBeEqualBasedOnEventId() {
            UUID eventId = UUID.randomUUID();
            UserCreatedEvent event1 = new UserCreatedEvent(
                    eventId,
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now()
            );
            UserCreatedEvent event2 = new UserCreatedEvent(
                    eventId,
                    UUID.randomUUID(), // Different user
                    "different",
                    AccountStatus.ACTIVE,
                    Instant.now().plusSeconds(10)
            );

            assertThat(event1).isEqualTo(event2);
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode());
        }

        @Test
        @DisplayName("should not be equal with different eventId")
        void shouldNotBeEqualWithDifferentEventId() {
            UserCreatedEvent event1 = new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now()
            );
            UserCreatedEvent event2 = new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now()
            );

            assertThat(event1).isNotEqualTo(event2);
        }

        @Test
        @DisplayName("should not be equal with null")
        void shouldNotBeEqualWithNull() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS
            );

            assertThat(event).isNotEqualTo(null);
        }

        @Test
        @DisplayName("should not be equal with different type")
        void shouldNotBeEqualWithDifferentType() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS
            );

            assertThat(event).isNotEqualTo("some string");
        }
    }

    @Nested
    @DisplayName("toString() method")
    class ToStringMethod {

        @Test
        @DisplayName("should not include PII in toString output")
        void shouldNotIncludePIIInToString() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS
            );

            String result = event.toString();

            // Should NOT contain username (PII)
            assertThat(result).doesNotContain(TEST_USERNAME);
            // Should contain event ID and user ID
            assertThat(result).contains("eventId=");
            assertThat(result).contains("userId=");
            // Should contain status info
            assertThat(result).contains("accountStatus=");
            assertThat(result).contains("isPendingActivation=");
        }

        @Test
        @DisplayName("should include isPendingActivation flag in toString")
        void shouldIncludePendingActivationFlagInToString() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.PENDING_ACTIVATION
            );

            assertThat(event.toString()).contains("isPendingActivation=true");
        }

        @Test
        @DisplayName("should show false for non-pending status")
        void shouldShowFalseForNonPendingStatus() {
            UserCreatedEvent event = new UserCreatedEvent(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.ACTIVE
            );

            assertThat(event.toString()).contains("isPendingActivation=false");
        }
    }
}
