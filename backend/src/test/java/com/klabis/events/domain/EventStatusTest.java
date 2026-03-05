package com.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("EventStatus Tests")
class EventStatusTest {

    @Nested
    @DisplayName("Allowed Transitions")
    class AllowedTransitions {

        @Test
        @DisplayName("Should allow transition from DRAFT to ACTIVE")
        void shouldAllowTransitionFromDraftToActive() {
            // Given
            EventStatus currentStatus = EventStatus.DRAFT;
            EventStatus newStatus = EventStatus.ACTIVE;

            // When & Then
            assertThatCode(() -> currentStatus.validateTransition(newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow transition from DRAFT to CANCELLED")
        void shouldAllowTransitionFromDraftToCancelled() {
            // Given
            EventStatus currentStatus = EventStatus.DRAFT;
            EventStatus newStatus = EventStatus.CANCELLED;

            // When & Then
            assertThatCode(() -> currentStatus.validateTransition(newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow transition from ACTIVE to CANCELLED")
        void shouldAllowTransitionFromActiveToCancelled() {
            // Given
            EventStatus currentStatus = EventStatus.ACTIVE;
            EventStatus newStatus = EventStatus.CANCELLED;

            // When & Then
            assertThatCode(() -> currentStatus.validateTransition(newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow transition from ACTIVE to FINISHED")
        void shouldAllowTransitionFromActiveToFinished() {
            // Given
            EventStatus currentStatus = EventStatus.ACTIVE;
            EventStatus newStatus = EventStatus.FINISHED;

            // When & Then
            assertThatCode(() -> currentStatus.validateTransition(newStatus))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should allow staying in same status")
        void shouldAllowStayingInSameStatus() {
            // When & Then
            assertThatCode(() -> EventStatus.DRAFT.validateTransition(EventStatus.DRAFT))
                    .doesNotThrowAnyException();
            assertThatCode(() -> EventStatus.ACTIVE.validateTransition(EventStatus.ACTIVE))
                    .doesNotThrowAnyException();
            assertThatCode(() -> EventStatus.CANCELLED.validateTransition(EventStatus.CANCELLED))
                    .doesNotThrowAnyException();
            assertThatCode(() -> EventStatus.FINISHED.validateTransition(EventStatus.FINISHED))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Rejected Transitions")
    class RejectedTransitions {

        @Test
        @DisplayName("Should reject transition from FINISHED to ACTIVE")
        void shouldRejectTransitionFromFinishedToActive() {
            // Given
            EventStatus currentStatus = EventStatus.FINISHED;
            EventStatus newStatus = EventStatus.ACTIVE;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from FINISHED to ACTIVE");
        }

        @Test
        @DisplayName("Should reject transition from FINISHED to DRAFT")
        void shouldRejectTransitionFromFinishedToDraft() {
            // Given
            EventStatus currentStatus = EventStatus.FINISHED;
            EventStatus newStatus = EventStatus.DRAFT;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from FINISHED to DRAFT");
        }

        @Test
        @DisplayName("Should reject transition from CANCELLED to ACTIVE")
        void shouldRejectTransitionFromCancelledToActive() {
            // Given
            EventStatus currentStatus = EventStatus.CANCELLED;
            EventStatus newStatus = EventStatus.ACTIVE;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from CANCELLED to ACTIVE");
        }

        @Test
        @DisplayName("Should reject transition from CANCELLED to DRAFT")
        void shouldRejectTransitionFromCancelledToDraft() {
            // Given
            EventStatus currentStatus = EventStatus.CANCELLED;
            EventStatus newStatus = EventStatus.DRAFT;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from CANCELLED to DRAFT");
        }

        @Test
        @DisplayName("Should reject transition from ACTIVE to DRAFT")
        void shouldRejectTransitionFromActiveToDraft() {
            // Given
            EventStatus currentStatus = EventStatus.ACTIVE;
            EventStatus newStatus = EventStatus.DRAFT;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from ACTIVE to DRAFT");
        }

        @Test
        @DisplayName("Should reject transition from CANCELLED to FINISHED")
        void shouldRejectTransitionFromCancelledToFinished() {
            // Given
            EventStatus currentStatus = EventStatus.CANCELLED;
            EventStatus newStatus = EventStatus.FINISHED;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from CANCELLED to FINISHED");
        }

        @Test
        @DisplayName("Should reject transition from FINISHED to CANCELLED")
        void shouldRejectTransitionFromFinishedToCancelled() {
            // Given
            EventStatus currentStatus = EventStatus.FINISHED;
            EventStatus newStatus = EventStatus.CANCELLED;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from FINISHED to CANCELLED");
        }

        @Test
        @DisplayName("Should reject transition from DRAFT to FINISHED")
        void shouldRejectTransitionFromDraftToFinished() {
            // Given
            EventStatus currentStatus = EventStatus.DRAFT;
            EventStatus newStatus = EventStatus.FINISHED;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(newStatus))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Cannot transition from DRAFT to FINISHED");
        }
    }

    @Nested
    @DisplayName("Transition Validation")
    class TransitionValidation {

        @Test
        @DisplayName("Should throw exception when new status is null")
        void shouldThrowExceptionWhenNewStatusIsNull() {
            // Given
            EventStatus currentStatus = EventStatus.DRAFT;

            // When & Then
            assertThatThrownBy(() -> currentStatus.validateTransition(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New status cannot be null");
        }
    }

    @Nested
    @DisplayName("Status Properties")
    class StatusProperties {

        @Test
        @DisplayName("Should have DRAFT status")
        void shouldHaveDraftStatus() {
            assertThat(EventStatus.DRAFT).isNotNull();
            assertThat(EventStatus.DRAFT.name()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("Should have ACTIVE status")
        void shouldHaveActiveStatus() {
            assertThat(EventStatus.ACTIVE).isNotNull();
            assertThat(EventStatus.ACTIVE.name()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("Should have CANCELLED status")
        void shouldHaveCancelledStatus() {
            assertThat(EventStatus.CANCELLED).isNotNull();
            assertThat(EventStatus.CANCELLED.name()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should have FINISHED status")
        void shouldHaveFinishedStatus() {
            assertThat(EventStatus.FINISHED).isNotNull();
            assertThat(EventStatus.FINISHED.name()).isEqualTo("FINISHED");
        }
    }
}
