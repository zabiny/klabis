package com.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegistrationDeadlines value object")
class RegistrationDeadlinesTest {

    private static final LocalDate D1 = LocalDate.of(2026, 3, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 4, 1);
    private static final LocalDate D3 = LocalDate.of(2026, 5, 1);

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("empty — no deadlines")
        void empty() {
            RegistrationDeadlines rd = RegistrationDeadlines.none();

            assertThat(rd.deadline1()).isEmpty();
            assertThat(rd.deadline2()).isEmpty();
            assertThat(rd.deadline3()).isEmpty();
            assertThat(rd.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("single deadline1 only")
        void singleDeadline() {
            RegistrationDeadlines rd = RegistrationDeadlines.single(D1);

            assertThat(rd.deadline1()).contains(D1);
            assertThat(rd.deadline2()).isEmpty();
            assertThat(rd.deadline3()).isEmpty();
        }

        @Test
        @DisplayName("two deadlines — d1 and d2")
        void twoDeadlines() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.empty());

            assertThat(rd.deadline1()).contains(D1);
            assertThat(rd.deadline2()).contains(D2);
            assertThat(rd.deadline3()).isEmpty();
        }

        @Test
        @DisplayName("three deadlines — d1, d2, d3")
        void threeDeadlines() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));

            assertThat(rd.deadline1()).contains(D1);
            assertThat(rd.deadline2()).contains(D2);
            assertThat(rd.deadline3()).contains(D3);
        }

        @Test
        @DisplayName("same-day deadlines are allowed (d1 == d2)")
        void sameDayDeadlinesAllowed() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D1), Optional.empty());

            assertThat(rd.deadline1()).contains(D1);
            assertThat(rd.deadline2()).contains(D1);
        }

        @Test
        @DisplayName("factory of() with all nulls produces empty")
        void factoryOfWithNulls() {
            RegistrationDeadlines rd = RegistrationDeadlines.of(null, null, null);

            assertThat(rd.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("factory of() maps non-null values")
        void factoryOfWithValues() {
            RegistrationDeadlines rd = RegistrationDeadlines.of(D1, D2, null);

            assertThat(rd.deadline1()).contains(D1);
            assertThat(rd.deadline2()).contains(D2);
            assertThat(rd.deadline3()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invariant violations")
    class InvariantViolations {

        @Test
        @DisplayName("d2 without d1 → IllegalArgumentException")
        void deadline2RequiresDeadline1() {
            assertThatThrownBy(() -> new RegistrationDeadlines(
                    Optional.empty(), Optional.of(D2), Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadline2 requires deadline1");
        }

        @Test
        @DisplayName("d3 without d2 → IllegalArgumentException")
        void deadline3RequiresDeadline2() {
            assertThatThrownBy(() -> new RegistrationDeadlines(
                    Optional.of(D1), Optional.empty(), Optional.of(D3)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadline3 requires deadline2");
        }

        @Test
        @DisplayName("d2 before d1 → IllegalArgumentException")
        void deadline2BeforeDeadline1() {
            assertThatThrownBy(() -> new RegistrationDeadlines(
                    Optional.of(D2), Optional.of(D1), Optional.empty()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadline2 must not be before deadline1");
        }

        @Test
        @DisplayName("d3 before d2 → IllegalArgumentException")
        void deadline3BeforeDeadline2() {
            assertThatThrownBy(() -> new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D3), Optional.of(D2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadline3 must not be before deadline2");
        }
    }

    @Nested
    @DisplayName("last()")
    class LastMethod {

        @Test
        @DisplayName("empty → empty")
        void emptyReturnsEmpty() {
            assertThat(RegistrationDeadlines.none().last()).isEmpty();
        }

        @Test
        @DisplayName("single → d1")
        void singleReturnsD1() {
            assertThat(RegistrationDeadlines.single(D1).last()).contains(D1);
        }

        @Test
        @DisplayName("two → d2")
        void twoReturnsD2() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.empty());
            assertThat(rd.last()).contains(D2);
        }

        @Test
        @DisplayName("three → d3")
        void threeReturnsD3() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));
            assertThat(rd.last()).contains(D3);
        }
    }

    @Nested
    @DisplayName("nextRelevant(today)")
    class NextRelevantMethod {

        @Test
        @DisplayName("empty → empty regardless of today")
        void emptyAlwaysEmpty() {
            assertThat(RegistrationDeadlines.none().nextRelevant(D1)).isEmpty();
        }

        @Test
        @DisplayName("single future deadline → returns d1")
        void singleFutureDeadline() {
            LocalDate today = D1.minusDays(1);
            assertThat(RegistrationDeadlines.single(D1).nextRelevant(today)).contains(D1);
        }

        @Test
        @DisplayName("single past deadline → returns d1 (last fallback)")
        void singlePastDeadlineFallback() {
            LocalDate today = D1.plusDays(1);
            assertThat(RegistrationDeadlines.single(D1).nextRelevant(today)).contains(D1);
        }

        @Test
        @DisplayName("d1 passed, d2 future → returns d2")
        void d1PassedD2Future() {
            LocalDate today = D1.plusDays(1);
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.empty());
            assertThat(rd.nextRelevant(today)).contains(D2);
        }

        @Test
        @DisplayName("d1 and d2 passed, d3 future → returns d3")
        void d1AndD2PassedD3Future() {
            LocalDate today = D2.plusDays(1);
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));
            assertThat(rd.nextRelevant(today)).contains(D3);
        }

        @Test
        @DisplayName("all passed → returns last (d3)")
        void allPassedReturnsLast() {
            LocalDate today = D3.plusDays(1);
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));
            assertThat(rd.nextRelevant(today)).contains(D3);
        }
    }

    @Nested
    @DisplayName("registrationsOpen(today)")
    class RegistrationsOpenMethod {

        @Test
        @DisplayName("empty deadlines → always open")
        void emptyAlwaysOpen() {
            assertThat(RegistrationDeadlines.none().registrationsOpen(LocalDate.now())).isTrue();
        }

        @Test
        @DisplayName("single deadline in future → open")
        void singleFutureOpen() {
            assertThat(RegistrationDeadlines.single(D1).registrationsOpen(D1.minusDays(1))).isTrue();
        }

        @Test
        @DisplayName("single deadline on today → still open (inclusive through the deadline day)")
        void singleDeadlineOnTodayStillOpen() {
            assertThat(RegistrationDeadlines.single(D1).registrationsOpen(D1)).isTrue();
        }

        @Test
        @DisplayName("single deadline in past → closed")
        void singlePastClosed() {
            assertThat(RegistrationDeadlines.single(D1).registrationsOpen(D1.plusDays(1))).isFalse();
        }

        @Test
        @DisplayName("three deadlines — open before last deadline")
        void threeDeadlinesOpenBeforeLast() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));
            assertThat(rd.registrationsOpen(D3.minusDays(1))).isTrue();
        }

        @Test
        @DisplayName("three deadlines — still open on last deadline day (inclusive)")
        void threeDeadlinesOpenOnLast() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));
            assertThat(rd.registrationsOpen(D3)).isTrue();
        }

        @Test
        @DisplayName("three deadlines — d1 passed, d2 still open → open")
        void threeDeadlinesD1PassedStillOpen() {
            RegistrationDeadlines rd = new RegistrationDeadlines(
                    Optional.of(D1), Optional.of(D2), Optional.of(D3));
            assertThat(rd.registrationsOpen(D2.minusDays(1))).isTrue();
        }
    }
}
