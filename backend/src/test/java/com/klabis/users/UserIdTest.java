package com.klabis.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserId Value Object Tests")
class UserIdTest {

    @Test
    @DisplayName("Should create UserId with valid UUID")
    void shouldCreateUserIdWithValidUUID() {
        UUID validUUID = UUID.randomUUID();
        UserId userId = new UserId(validUUID);

        assertThat(userId.uuid()).isEqualTo(validUUID);
    }

    @Test
    @DisplayName("Should throw exception when UUID is null in constructor")
    void shouldThrowExceptionWhenUUIDIsNull() {
        assertThatThrownBy(() -> new UserId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID cannot be null");
    }

    @Test
    @DisplayName("Should create UserId from valid UUID string")
    void shouldCreateUserIdFromValidString() {
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        UserId userId = UserId.fromString(uuidString);

        assertThat(userId.uuid()).isEqualTo(UUID.fromString(uuidString));
    }

    @Test
    @DisplayName("Should throw exception when string is null")
    void shouldThrowExceptionWhenStringIsNull() {
        assertThatThrownBy(() -> UserId.fromString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID string cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when string is blank")
    void shouldThrowExceptionWhenStringIsBlank() {
        assertThatThrownBy(() -> UserId.fromString("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID string cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when string is empty")
    void shouldThrowExceptionWhenStringIsEmpty() {
        assertThatThrownBy(() -> UserId.fromString(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UUID string cannot be null or blank");
    }

    @Test
    @DisplayName("Should throw exception when string is not a valid UUID")
    void shouldThrowExceptionWhenStringIsInvalidUUID() {
        assertThatThrownBy(() -> UserId.fromString("not-a-valid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID string");
    }

    @Test
    @DisplayName("Should throw exception when string has invalid UUID format")
    void shouldThrowExceptionWhenStringHasInvalidFormat() {
        assertThatThrownBy(() -> UserId.fromString("550e8400-e29b-41d4-a716"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid UUID string");
    }

    @Test
    @DisplayName("Should consider two UserId instances with same UUID as equal")
    void shouldConsiderSameUUIDsAsEqual() {
        UUID uuid = UUID.randomUUID();
        UserId userId1 = new UserId(uuid);
        UserId userId2 = new UserId(uuid);

        assertThat(userId1).isEqualTo(userId2);
        assertThat(userId1.hashCode()).isEqualTo(userId2.hashCode());
    }

    @Test
    @DisplayName("Should consider two UserId instances with different UUIDs as not equal")
    void shouldConsiderDifferentUUIDsAsNotEqual() {
        UserId userId1 = new UserId(UUID.randomUUID());
        UserId userId2 = new UserId(UUID.randomUUID());

        assertThat(userId1).isNotEqualTo(userId2);
    }

    @Test
    @DisplayName("Should have correct toString representation")
    void shouldHaveCorrectToStringRepresentation() {
        UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UserId userId = new UserId(uuid);

        assertThat(userId.toString()).contains("550e8400-e29b-41d4-a716-446655440000");
    }

    @Test
    @DisplayName("Should return UUID from getter")
    void shouldReturnUUIDFromGetter() {
        UUID uuid = UUID.randomUUID();
        UserId userId = new UserId(uuid);

        assertThat(userId.uuid()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("Should create UserId with random UUID")
    void shouldCreateUserIdWithRandomUUID() {
        UserId userId = new UserId(UUID.randomUUID());

        assertThat(userId.uuid()).isNotNull();
    }
}
