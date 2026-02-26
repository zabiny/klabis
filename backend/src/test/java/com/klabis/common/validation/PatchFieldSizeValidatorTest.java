package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatchFieldSizeValidatorTest {

    private PatchFieldSizeValidator testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new PatchFieldSizeValidator();
        testedSubject.initialize(new ValidPatchFieldSize() {
            @Override
            public int min() {
                return 2;
            }

            @Override
            public int max() {
                return 10;
            }

            @Override
            public String message() {
                return "Size must be between {min} and {max}";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends ValidPatchFieldSize> annotationType() {
                return ValidPatchFieldSize.class;
            }
        });
    }

    @Test
    void shouldReturnTrueWhenPatchFieldIsNotProvided() {
        assertThat(testedSubject.isValid(PatchField.notProvided(), null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenPatchFieldIsNull() {
        assertThat(testedSubject.isValid(null, null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenProvidedValueIsWithinBounds() {
        PatchField<String> value = PatchField.of("valid");
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenProvidedValueIsNull() {
        PatchField<String> value = PatchField.of(null);
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueIsTooShort() {
        PatchField<String> value = PatchField.of("a");
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueIsTooLong() {
        PatchField<String> value = PatchField.of("this is too long");
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }

    @Test
    void shouldReturnTrueWhenProvidedValueIsAtMinBoundary() {
        PatchField<String> value = PatchField.of("ab");
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenProvidedValueIsAtMaxBoundary() {
        PatchField<String> value = PatchField.of("0123456789");
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }
}
