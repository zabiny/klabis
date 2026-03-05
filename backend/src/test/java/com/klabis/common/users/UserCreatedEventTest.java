package com.klabis.common.users;

import com.klabis.common.users.domain.AccountStatus;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserCreatedEvent tests")
class UserCreatedEventTest {

    private static final UUID TEST_USER_UUID = UUID.randomUUID();
    private static final UserId TEST_USER_ID = new UserId(TEST_USER_UUID);
    private static final String TEST_USERNAME = "ZBM9001";
    private static final AccountStatus TEST_STATUS = AccountStatus.PENDING_ACTIVATION;

    @Nested
    @DisplayName("Static factory method create()")
    class CreateFactoryMethod {

        @Test
        @DisplayName("should create event with generated ID and current timestamp")
        void shouldCreateEventWithGeneratedIdAndTimestamp() {
            // When
            UserCreatedEvent event = UserCreatedEvent.create(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS
            );

            // Then
            assertThat(event.eventId()).isNotNull();
            assertThat(event.userId()).isEqualTo(TEST_USER_ID);
            assertThat(event.username()).isEqualTo(TEST_USERNAME);
            assertThat(event.accountStatus()).isEqualTo(TEST_STATUS);
            assertThat(event.occurredAt()).isNotNull();
            assertThat(event.occurredAt()).isBefore(Instant.now().plusMillis(100)); // Within last 100ms
        }

        @Test
        @DisplayName("should generate unique event IDs")
        void shouldGenerateUniqueEventIds() {
            // When
            UserCreatedEvent event1 = UserCreatedEvent.create(TEST_USER_ID, TEST_USERNAME, TEST_STATUS);
            UserCreatedEvent event2 = UserCreatedEvent.create(TEST_USER_ID, TEST_USERNAME, TEST_STATUS);

            // Then
            assertThat(event1.eventId()).isNotEqualTo(event2.eventId());
        }
    }

    @Nested
    @DisplayName("Canonical constructor with explicit values")
    class CanonicalConstructor {

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
                    TEST_TIMESTAMP,
                    null
            );

            // Then
            assertThat(event.eventId()).isEqualTo(TEST_EVENT_ID);
            assertThat(event.userId()).isEqualTo(TEST_USER_ID);
            assertThat(event.username()).isEqualTo(TEST_USERNAME);
            assertThat(event.accountStatus()).isEqualTo(TEST_STATUS);
            assertThat(event.occurredAt()).isEqualTo(TEST_TIMESTAMP);
        }

        @Test
        @DisplayName("should throw exception for null eventId")
        void shouldThrowExceptionForNullEventId() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    null,
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now(),
                    null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Event ID is required");
        }

        @Test
        @DisplayName("should throw exception for null userId")
        void shouldThrowExceptionForNullUserId() {
            assertThatThrownBy(() -> new UserCreatedEvent(
                    UUID.randomUUID(),
                    (UserId) null,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now(),
                    null
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
                    Instant.now(),
                    null
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
                    Instant.now(),
                    null
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
                    null,
                    null
            ))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Occurred at timestamp is required");
        }

        @Test
        @DisplayName("should allow null email (optional PII)")
        void shouldAllowNullEmail() {
            assertThatNoException().isThrownBy(() -> new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now(),
                    null
            ));
        }
    }

    @Nested
    @DisplayName("fromAggregate() factory method")
    class FromAggregateFactoryMethod {

        @Test
        @DisplayName("should create event from User aggregate")
        void shouldCreateEventFromUser() {
            // Given
            User user = User.reconstruct(TEST_USER_ID, TEST_USERNAME, "hashedPassword", AccountStatus.PENDING_ACTIVATION);

            // When
            UserCreatedEvent event = UserCreatedEvent.fromAggregate(user);

            // Then
            assertThat(event.userId()).isEqualTo(TEST_USER_ID);
            assertThat(event.username()).isEqualTo(TEST_USERNAME);
            assertThat(event.accountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
            assertThat(event.eventId()).isNotNull(); // Generated
            assertThat(event.occurredAt()).isNotNull(); // Current time
        }
    }

    @Nested
    @DisplayName("fromAggregateWithEmail() factory method")
    class FromAggregateWithEmailFactoryMethod {

        @Test
        @DisplayName("should create event with email from User aggregate")
        void shouldCreateEventWithEmailFromUser() {
            // Given
            User user = User.reconstruct(TEST_USER_ID, TEST_USERNAME, "hashedPassword", AccountStatus.PENDING_ACTIVATION);
            String email = "test@example.com";

            // When
            UserCreatedEvent event = UserCreatedEvent.fromAggregateWithEmail(user, email);

            // Then
            assertThat(event.userId()).isEqualTo(TEST_USER_ID);
            assertThat(event.username()).isEqualTo(TEST_USERNAME);
            assertThat(event.accountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
            assertThat(event.email()).isPresent().contains(email);
            assertThat(event.eventId()).isNotNull(); // Generated
            assertThat(event.occurredAt()).isNotNull(); // Current time
        }
    }

    @Nested
    @DisplayName("email() method")
    class EmailMethod {

        @Test
        @DisplayName("should return Optional with email when present")
        void shouldReturnOptionalWithEmailWhenPresent() {
            UserCreatedEvent event = new UserCreatedEvent(
                    UUID.randomUUID(),
                    TEST_USER_ID,
                    TEST_USERNAME,
                    TEST_STATUS,
                    Instant.now(),
                    "test@example.com"
            );

            assertThat(event.email()).isPresent().contains("test@example.com");
        }

        @Test
        @DisplayName("should return empty Optional when email is null")
        void shouldReturnEmptyOptionalWhenEmailIsNull() {
            UserCreatedEvent event = UserCreatedEvent.create(TEST_USER_ID, TEST_USERNAME, TEST_STATUS);

            assertThat(event.email()).isEmpty();
        }
    }

    @Nested
    @DisplayName("isPendingActivation() method")
    class IsPendingActivationMethod {

        @Test
        @DisplayName("should return true when account status is PENDING_ACTIVATION")
        void shouldReturnTrueWhenPendingActivation() {
            UserCreatedEvent event = UserCreatedEvent.create(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.PENDING_ACTIVATION
            );

            assertThat(event.isPendingActivation()).isTrue();
        }

        @Test
        @DisplayName("should return false when account status is ACTIVE")
        void shouldReturnFalseWhenActive() {
            UserCreatedEvent event = UserCreatedEvent.create(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.ACTIVE
            );

            assertThat(event.isPendingActivation()).isFalse();
        }

        @Test
        @DisplayName("should return false when account status is SUSPENDED")
        void shouldReturnFalseWhenSuspended() {
            UserCreatedEvent event = UserCreatedEvent.create(
                    TEST_USER_ID,
                    TEST_USERNAME,
                    AccountStatus.SUSPENDED
            );

            assertThat(event.isPendingActivation()).isFalse();
        }
    }

    @Nested
    @DisplayName("toString() method")
    class ToStringMethod {

        @Test
        @DisplayName("should not include PII in toString output")
        void shouldNotIncludePIIInToString() {
            UserCreatedEvent event = UserCreatedEvent.create(
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

    }
}
