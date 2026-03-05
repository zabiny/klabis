package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import jakarta.validation.Payload;

import static org.assertj.core.api.Assertions.assertThat;

class PatchFieldPatternValidatorTest {

    private PatchFieldPatternValidator testedSubject;

    @BeforeEach
    void setUp() {
        testedSubject = new PatchFieldPatternValidator();
        testedSubject.initialize(new ValidPatchFieldPattern() {
            @Override
            public String regexp() {
                return "^[A-Z]{2}\\d{4}$";
            }

            @Override
            public String message() {
                return "Invalid format";
            }

            @Override
            public Class<?>[] groups() {
                return new Class<?>[0];
            }

            @Override
            public Class<? extends Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public Class<? extends ValidPatchFieldPattern> annotationType() {
                return ValidPatchFieldPattern.class;
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
    void shouldReturnTrueWhenProvidedValueMatchesPattern() {
        PatchField<String> value = PatchField.of("AB1234");
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenProvidedValueIsNull() {
        PatchField<String> value = PatchField.of(null);
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueDoesNotMatchPattern() {
        PatchField<String> value = PatchField.of("invalid");
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueIsPartialMatch() {
        PatchField<String> value = PatchField.of("AB12345");
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }
}
