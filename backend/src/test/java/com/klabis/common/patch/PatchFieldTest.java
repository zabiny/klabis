package com.klabis.common.patch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PatchField<T> Tests")
class PatchFieldTest {

    @Nested
    @DisplayName("PatchField.of() - Factory method for provided values")
    class OfMethodTests {

        @Test
        @DisplayName("Should create PatchField with provided value")
        void shouldCreatePatchFieldWithProvidedValue() {
            PatchField<String> patchField = PatchField.of("test value");

            assertThat(patchField.isProvided()).isTrue();
        }

        @Test
        @DisplayName("Should create PatchField with null value (provided with null)")
        void shouldCreatePatchFieldWithNullValue() {
            PatchField<String> patchField = PatchField.of(null);

            assertThat(patchField.isProvided()).isTrue();
            assertThat(patchField.get()).isNull();
        }

        @Test
        @DisplayName("Should create PatchField with empty string")
        void shouldCreatePatchFieldWithEmptyString() {
            PatchField<String> patchField = PatchField.of("");

            assertThat(patchField.isProvided()).isTrue();
            assertThat(patchField.get()).isEmpty();
        }
    }

    @Nested
    @DisplayName("PatchField.notProvided() - Factory method for absent values")
    class NotProvidedMethodTests {

        @Test
        @DisplayName("Should create PatchField that is not provided")
        void shouldCreatePatchFieldThatIsNotProvided() {
            PatchField<String> patchField = PatchField.notProvided();

            assertThat(patchField.isProvided()).isFalse();
        }

        @Test
        @DisplayName("Should return singleton instance for same type")
        void shouldReturnSingletonInstanceForSameType() {
            PatchField<String> notProvided1 = PatchField.notProvided();
            PatchField<String> notProvided2 = PatchField.notProvided();

            assertThat(notProvided1).isSameAs(notProvided2);
        }

        @Test
        @DisplayName("Should return singleton instance across different types")
        void shouldReturnSingletonInstanceAcrossDifferentTypes() {
            PatchField<String> notProvidedString = PatchField.notProvided();
            PatchField<Integer> notProvidedInteger = PatchField.notProvided();

            assertThat(notProvidedString).isSameAs(notProvidedInteger);
        }
    }

    @Nested
    @DisplayName("isProvided() - Check if value was provided")
    class IsProvidedTests {

        @Test
        @DisplayName("Should return true for provided value")
        void shouldReturnTrueForProvidedValue() {
            PatchField<String> patchField = PatchField.of("value");

            assertThat(patchField.isProvided()).isTrue();
        }

        @Test
        @DisplayName("Should return false for not provided")
        void shouldReturnFalseForNotProvided() {
            PatchField<String> patchField = PatchField.notProvided();

            assertThat(patchField.isProvided()).isFalse();
        }
    }

    @Nested
    @DisplayName("get() - Retrieve the value")
    class GetTests {

        @Test
        @DisplayName("Should return value when provided")
        void shouldReturnValueWhenProvided() {
            PatchField<String> patchField = PatchField.of("test value");

            assertThat(patchField.get()).isEqualTo("test value");
        }

        @Test
        @DisplayName("Should return null when provided with null")
        void shouldReturnNullWhenProvidedWithNull() {
            PatchField<String> patchField = PatchField.of(null);

            assertThat(patchField.get()).isNull();
        }

        @Test
        @DisplayName("Should throw NoSuchElementException when not provided")
        void shouldThrowExceptionWhenNotProvided() {
            PatchField<String> patchField = PatchField.notProvided();

            assertThatThrownBy(patchField::get)
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessage("No value provided");
        }
    }

    @Nested
    @DisplayName("ifProvided() - Execute action if value was provided")
    class IfProvidedTests {

        @Test
        @DisplayName("Should execute action when value is provided")
        void shouldExecuteActionWhenValueIsProvided() {
            PatchField<String> patchField = PatchField.of("test value");
            AtomicBoolean executed = new AtomicBoolean(false);
            AtomicReference<String> capturedValue = new AtomicReference<>();

            patchField.ifProvided(value -> {
                executed.set(true);
                capturedValue.set(value);
            });

            assertThat(executed.get()).isTrue();
            assertThat(capturedValue.get()).isEqualTo("test value");
        }

        @Test
        @DisplayName("Should execute action even when value is null")
        void shouldExecuteActionEvenWhenValueIsNull() {
            PatchField<String> patchField = PatchField.of(null);
            AtomicBoolean executed = new AtomicBoolean(false);
            AtomicReference<String> capturedValue = new AtomicReference<>();

            patchField.ifProvided(value -> {
                executed.set(true);
                capturedValue.set(value);
            });

            assertThat(executed.get()).isTrue();
            assertThat(capturedValue.get()).isNull();
        }

        @Test
        @DisplayName("Should not execute action when value is not provided")
        void shouldNotExecuteActionWhenValueIsNotProvided() {
            PatchField<String> patchField = PatchField.notProvided();
            AtomicBoolean executed = new AtomicBoolean(false);

            patchField.ifProvided(value -> executed.set(true));

            assertThat(executed.get()).isFalse();
        }

        @Test
        @DisplayName("Should throw NullPointerException when action is null")
        void shouldThrowNullPointerExceptionWhenActionIsNull() {
            PatchField<String> patchField = PatchField.of("value");

            assertThatThrownBy(() -> patchField.ifProvided(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("map() - Transform provided values")
    class MapTests {

        @Test
        @DisplayName("Should map provided value to another type")
        void shouldMapProvidedValueToAnotherType() {
            PatchField<String> patchField = PatchField.of("abcd");

            PatchField<Integer> mapped = patchField.map(String::length);

            assertThat(mapped.isProvided()).isTrue();
            assertThat(mapped.get()).isEqualTo(4);
        }

        @Test
        @DisplayName("Should map provided null value")
        void shouldMapProvidedNullValue() {
            PatchField<String> patchField = PatchField.of(null);

            PatchField<String> mapped = patchField.map(value -> value == null ? "fallback" : value);

            assertThat(mapped.isProvided()).isTrue();
            assertThat(mapped.get()).isEqualTo("fallback");
        }

        @Test
        @DisplayName("Should return not provided when original value was not provided")
        void shouldReturnNotProvidedWhenOriginalValueWasNotProvided() {
            PatchField<String> patchField = PatchField.notProvided();
            AtomicBoolean mapperInvoked = new AtomicBoolean(false);

            PatchField<Integer> mapped = patchField.map(value -> {
                mapperInvoked.set(true);
                return value.length();
            });

            assertThat(mapperInvoked.get()).isFalse();
            assertThat(mapped.isProvided()).isFalse();
            assertThat(mapped).isSameAs(PatchField.notProvided());
        }

        @Test
        @DisplayName("Should throw NullPointerException when mapper is null and value is provided")
        void shouldThrowNullPointerExceptionWhenMapperIsNullAndValueIsProvided() {
            PatchField<String> patchField = PatchField.of("value");

            assertThatThrownBy(() -> patchField.map(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should keep not provided when mapper is null and value is not provided")
        void shouldKeepNotProvidedWhenMapperIsNullAndValueIsNotProvided() {
            PatchField<String> patchField = PatchField.notProvided();

            PatchField<Integer> mapped = patchField.map(null);

            assertThat(mapped.isProvided()).isFalse();
            assertThat(mapped).isSameAs(PatchField.notProvided());
        }
    }
}
