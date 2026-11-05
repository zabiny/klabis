package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.membershipfees.domain.DeadlineNotInFutureException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeadlineNotInFutureException field binding")
class DeadlineNotInFutureFieldBindingTest {

    @Test
    @DisplayName("start case names the votingDeadline property of the generated start-campaign request")
    void startCaseMatchesRequestProperty() throws NoSuchFieldException {
        String field = DeadlineNotInFutureException.onStart(LocalDate.of(2026, 1, 1)).field();

        assertThat(PublishYearRequest.class.getDeclaredField(field)).isNotNull();
    }

    @Test
    @DisplayName("change case names the votingDeadline property of the generated change-deadline request")
    void changeCaseMatchesRequestProperty() throws NoSuchFieldException {
        String field = DeadlineNotInFutureException.onChange(LocalDate.of(2026, 1, 1)).field();

        assertThat(ChangeDeadlineRequest.class.getDeclaredField(field)).isNotNull();
    }
}
