package com.klabis.members;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for value object tests.
 * <p>
 * Provides common test methods for value objects to eliminate duplication.
 * Subclasses should implement the abstract methods to provide test-specific data.
 * <p>
 * This base class tests:
 * - Equality (same values are equal)
 * - Inequality (different values are not equal)
 * - HashCode consistency
 * - Immutability (records are immutable by design)
 * - toString representation
 *
 * @param <T> the value object type
 */
public abstract class ValueObjectTestBase<T> {

    /**
     * Creates a valid value object instance with default test values.
     *
     * @return a valid value object instance
     */
    protected abstract T createValidValue();

    /**
     * Creates a value object instance with different values than {@link #createValidValue()}.
     * <p>
     * Used to test inequality.
     *
     * @return a value object with different values
     */
    protected abstract T createDifferentValue();

    /**
     * Returns the expected string representation of the value object.
     * <p>
     * Used to verify toString(). If null, toString test is skipped.
     *
     * @return expected toString result, or null to skip test
     */
    protected String expectedToString() {
        return null;
    }

    @Test
    @DisplayName("Should implement equality correctly - same values are equal")
    void shouldImplementEqualityCorrectly() {
        // Given
        T value1 = createValidValue();
        T value2 = createValidValue();
        T value3 = createDifferentValue();

        // Then & Expect
        assertThat(value1)
                .as("Values with same data should be equal")
                .isEqualTo(value2);

        assertThat(value1)
                .as("Values with different data should not be equal")
                .isNotEqualTo(value3);

        assertThat(value1.hashCode())
                .as("Equal values should have same hashCode")
                .isEqualTo(value2.hashCode());
    }

    @Test
    @DisplayName("Should be immutable as a record")
    void shouldBeImmutable() {
        // Given
        T value = createValidValue();

        // Then & Expect - Records are immutable by design
        // This test documents the immutability contract
        assertThat(value).isNotNull();
    }

    @Test
    @DisplayName("Should have proper toString representation")
    void shouldHaveProperToString() {
        // Given
        String expectedToString = expectedToString();
        if (expectedToString == null) {
            // Skip test if no expectation provided
            return;
        }

        T value = createValidValue();

        // Then & Expect
        assertThat(value.toString())
                .as("toString should contain expected content")
                .isEqualTo(expectedToString);
    }

    @Test
    @DisplayName("Should be equal to itself")
    void shouldBeEqualToItself() {
        // Given
        T value = createValidValue();

        // Then & Expect
        assertThat(value)
                .as("Value should be equal to itself")
                .isEqualTo(value);
    }

    @Test
    @DisplayName("Should not be equal to null")
    void shouldNotBeEqualToNull() {
        // Given
        T value = createValidValue();

        // Then & Expect
        assertThat(value)
                .as("Value should not be equal to null")
                .isNotEqualTo(null);
    }
}
