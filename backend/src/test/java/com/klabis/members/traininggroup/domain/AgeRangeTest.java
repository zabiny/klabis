package com.klabis.members.traininggroup.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AgeRange")
class AgeRangeTest {

    @Nested
    @DisplayName("constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("should reject negative minAge")
        void shouldRejectNegativeMinAge() {
            assertThatThrownBy(() -> new AgeRange(-1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject maxAge less than minAge")
        void shouldRejectMaxAgeLessThanMinAge() {
            assertThatThrownBy(() -> new AgeRange(10, 5))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should allow equal minAge and maxAge")
        void shouldAllowEqualMinAndMax() {
            AgeRange range = new AgeRange(10, 10);
            assertThat(range.minAge()).isEqualTo(10);
            assertThat(range.maxAge()).isEqualTo(10);
        }

        @Test
        @DisplayName("should allow zero minAge")
        void shouldAllowZeroMinAge() {
            AgeRange range = new AgeRange(0, 5);
            assertThat(range.minAge()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("includes()")
    class IncludesMethod {

        private final AgeRange range = new AgeRange(10, 20);

        @Test
        @DisplayName("should return true for age at minimum boundary")
        void shouldReturnTrueAtMinBoundary() {
            assertThat(range.includes(10)).isTrue();
        }

        @Test
        @DisplayName("should return true for age at maximum boundary")
        void shouldReturnTrueAtMaxBoundary() {
            assertThat(range.includes(20)).isTrue();
        }

        @Test
        @DisplayName("should return true for age within range")
        void shouldReturnTrueForAgeWithinRange() {
            assertThat(range.includes(15)).isTrue();
        }

        @Test
        @DisplayName("should return false for age below minimum")
        void shouldReturnFalseForAgeBelowMin() {
            assertThat(range.includes(9)).isFalse();
        }

        @Test
        @DisplayName("should return false for age above maximum")
        void shouldReturnFalseForAgeAboveMax() {
            assertThat(range.includes(21)).isFalse();
        }
    }

    @Nested
    @DisplayName("overlaps()")
    class OverlapsMethod {

        private final AgeRange base = new AgeRange(10, 20);

        @Test
        @DisplayName("should return false when other range is entirely below")
        void shouldReturnFalseWhenOtherIsEntirelyBelow() {
            assertThat(base.overlaps(new AgeRange(0, 9))).isFalse();
        }

        @Test
        @DisplayName("should return false when other range is entirely above")
        void shouldReturnFalseWhenOtherIsEntirelyAbove() {
            assertThat(base.overlaps(new AgeRange(21, 30))).isFalse();
        }

        @Test
        @DisplayName("should return true when other range fully contains this range")
        void shouldReturnTrueWhenFullyContained() {
            assertThat(base.overlaps(new AgeRange(5, 25))).isTrue();
        }

        @Test
        @DisplayName("should return true when this range fully contains other range")
        void shouldReturnTrueWhenContainsOther() {
            assertThat(base.overlaps(new AgeRange(12, 18))).isTrue();
        }

        @Test
        @DisplayName("should return true when other range partially overlaps from below")
        void shouldReturnTrueWhenPartialOverlapFromBelow() {
            assertThat(base.overlaps(new AgeRange(5, 15))).isTrue();
        }

        @Test
        @DisplayName("should return true when other range partially overlaps from above")
        void shouldReturnTrueWhenPartialOverlapFromAbove() {
            assertThat(base.overlaps(new AgeRange(15, 25))).isTrue();
        }

        @Test
        @DisplayName("should return true when ranges share only min boundary")
        void shouldReturnTrueWhenAdjacentAtMin() {
            assertThat(base.overlaps(new AgeRange(0, 10))).isTrue();
        }

        @Test
        @DisplayName("should return true when ranges share only max boundary")
        void shouldReturnTrueWhenAdjacentAtMax() {
            assertThat(base.overlaps(new AgeRange(20, 30))).isTrue();
        }

        @Test
        @DisplayName("should return false for strictly adjacent ranges without shared boundary")
        void shouldReturnFalseForStrictlyAdjacentRanges() {
            assertThat(base.overlaps(new AgeRange(0, 9))).isFalse();
            assertThat(base.overlaps(new AgeRange(21, 30))).isFalse();
        }
    }
}
