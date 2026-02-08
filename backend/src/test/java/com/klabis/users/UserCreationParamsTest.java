package com.klabis.users;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserCreationParams tests")
class UserCreationParamsTest {

    @Nested
    @DisplayName("Builder pattern")
    class BuilderPattern {

        @Test
        @DisplayName("should build with all required fields")
        void shouldBuildWithAllRequiredFields() {
            UserCreationParams params = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .build();

            assertThat(params.username()).isEqualTo("ZBM0501");
            assertThat(params.passwordHash()).isEqualTo("$2a$10$hashed");
            assertThat(params.authorities()).containsExactly(Authority.MEMBERS_READ);
            assertThat(params.getEmail()).isEmpty();
        }

        @Test
        @DisplayName("should build with email")
        void shouldBuildWithEmail() {
            UserCreationParams params = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .email("user@example.com")
                    .build();

            assertThat(params.username()).isEqualTo("ZBM0501");
            assertThat(params.getEmail()).contains("user@example.com");
        }

        @Test
        @DisplayName("should support multiple authorities")
        void shouldSupportMultipleAuthorities() {
            Set<Authority> authorities = Set.of(
                    Authority.MEMBERS_READ,
                    Authority.MEMBERS_CREATE,
                    Authority.MEMBERS_UPDATE
            );

            UserCreationParams params = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(authorities)
                    .build();

            assertThat(params.authorities()).isEqualTo(authorities);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("should throw when username is null")
        void shouldThrowWhenUsernameIsNull() {
            assertThatThrownBy(() -> UserCreationParams.builder()
                    .username(null)
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Username is required");
        }

        @Test
        @DisplayName("should throw when passwordHash is null")
        void shouldThrowWhenPasswordHashIsNull() {
            assertThatThrownBy(() -> UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash(null)
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Password hash is required");
        }

        @Test
        @DisplayName("should throw when authorities are null")
        void shouldThrowWhenAuthoritiesAreNull() {
            assertThatThrownBy(() -> UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(null)
                    .build())
                    .isInstanceOf(NullPointerException.class)
                    .hasMessage("Authorities are required");
        }

        @Test
        @DisplayName("should allow null email (optional)")
        void shouldAllowNullEmail() {
            UserCreationParams params = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .email(null)
                    .build();

            assertThat(params.getEmail()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Immutability (Record contract)")
    class Immutability {

        @Test
        @DisplayName("should be immutable")
        void shouldBeImmutable() {
            UserCreationParams params = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .build();

            // Records have equals/hashCode based on components
            UserCreationParams params2 = new UserCreationParams(
                    "ZBM0501",
                    "$2a$10$hashed",
                    Set.of(Authority.MEMBERS_READ),
                    null
            );

            assertThat(params).isEqualTo(params2);
        }

        @Test
        @DisplayName("should differ when email is different")
        void shouldDifferWhenEmailIsDifferent() {
            UserCreationParams params1 = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .email("user1@example.com")
                    .build();

            UserCreationParams params2 = UserCreationParams.builder()
                    .username("ZBM0501")
                    .passwordHash("$2a$10$hashed")
                    .authorities(Set.of(Authority.MEMBERS_READ))
                    .email("user2@example.com")
                    .build();

            assertThat(params1).isNotEqualTo(params2);
        }
    }
}
