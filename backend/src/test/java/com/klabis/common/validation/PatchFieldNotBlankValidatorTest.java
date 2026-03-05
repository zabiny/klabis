package com.klabis.common.validation;

import com.klabis.common.patch.PatchField;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PatchFieldNotBlankValidatorTest {

    private final PatchFieldNotBlankValidator testedSubject = new PatchFieldNotBlankValidator();

    @Test
    void shouldReturnTrueWhenPatchFieldIsNotProvided() {
        assertThat(testedSubject.isValid(PatchField.notProvided(), null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenPatchFieldIsNull() {
        assertThat(testedSubject.isValid(null, null)).isTrue();
    }

    @Test
    void shouldReturnTrueWhenProvidedValueIsNotBlank() {
        PatchField<String> value = PatchField.of("valid text");
        assertThat(testedSubject.isValid(value, null)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueIsNull() {
        PatchField<String> value = PatchField.of(null);
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueIsEmpty() {
        PatchField<String> value = PatchField.of("");
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }

    @Test
    void shouldReturnFalseWhenProvidedValueIsBlank() {
        PatchField<String> value = PatchField.of("   ");
        assertThat(testedSubject.isValid(value, null)).isFalse();
    }
}
