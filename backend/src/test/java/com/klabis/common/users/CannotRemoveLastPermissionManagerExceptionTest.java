package com.klabis.common.users;

import com.klabis.common.users.authorization.CannotRemoveLastPermissionManagerException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CannotRemoveLastPermissionManagerException tests")
class CannotRemoveLastPermissionManagerExceptionTest {

    @Nested
    @DisplayName("Constructor: default (no args)")
    class DefaultConstructor {

        @Test
        @DisplayName("should create exception with default message")
        void shouldCreateExceptionWithDefaultMessage() {
            // When
            CannotRemoveLastPermissionManagerException exception =
                    new CannotRemoveLastPermissionManagerException();

            // Then
            assertThat(exception.getMessage())
                    .contains("Cannot remove MEMBERS:PERMISSIONS")
                    .contains("last active user")
                    .contains("permission management capability");
        }

        @Test
        @DisplayName("should have clear error message explaining business rule")
        void shouldHaveClearErrorMessageExplainingBusinessRule() {
            // When
            CannotRemoveLastPermissionManagerException exception =
                    new CannotRemoveLastPermissionManagerException();

            // Then - Message should explain the business rule clearly
            assertThat(exception.getMessage())
                    .contains("Cannot remove")
                    .contains("MEMBERS:PERMISSIONS")
                    .contains("grant")
                    .contains("another user");
        }

        @Test
        @DisplayName("should be catchable as RuntimeException")
        void shouldBeCatchableAsRuntimeException() {
            // Given & When
            RuntimeException caughtException = null;
            try {
                throw new CannotRemoveLastPermissionManagerException();
            } catch (RuntimeException e) {
                caughtException = e;
            }

            // Then - Should be catchable
            assertThat(caughtException).isNotNull();
            assertThat(caughtException).isInstanceOf(CannotRemoveLastPermissionManagerException.class);
        }
    }

    @Nested
    @DisplayName("Constructor: with message")
    class ConstructorWithMessage {

        @Test
        @DisplayName("should create exception with custom message")
        void shouldCreateExceptionWithCustomMessage() {
            // Given
            String customMessage = "Custom error message for last admin removal";

            // When
            CannotRemoveLastPermissionManagerException exception =
                    new CannotRemoveLastPermissionManagerException(customMessage);

            // Then
            assertThat(exception.getMessage()).isEqualTo(customMessage);
        }
    }

    @Nested
    @DisplayName("Exception type")
    class ExceptionType {

        @Test
        @DisplayName("should be a runtime exception")
        void shouldBeRuntimeException() {
            // Given & When
            CannotRemoveLastPermissionManagerException exception =
                    new CannotRemoveLastPermissionManagerException();

            // Then
            assertThat(exception).isInstanceOf(RuntimeException.class);
        }
    }
}
