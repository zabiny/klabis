package com.klabis.common.patch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PatchOverlay")
class PatchOverlayTest {

    private final List<String> applied = new ArrayList<>();

    @Test
    @DisplayName("overlay leaves an undefined field alone")
    void overlayIgnoresUndefinedField() {
        PatchOverlay.overlay(JsonNullable.<String>undefined(), applied::add);

        assertThat(applied).isEmpty();
    }

    @Test
    @DisplayName("overlay applies a present null so the field is cleared")
    void overlayAppliesPresentNull() {
        PatchOverlay.overlay(JsonNullable.<String>of(null), applied::add);

        assertThat(applied).hasSize(1).containsNull();
    }

    @Test
    @DisplayName("overlay applies a present value")
    void overlayAppliesPresentValue() {
        PatchOverlay.overlay(JsonNullable.of("value"), applied::add);

        assertThat(applied).containsExactly("value");
    }

    @Test
    @DisplayName("overlayNonNull leaves an undefined field alone")
    void overlayNonNullIgnoresUndefinedField() {
        PatchOverlay.overlayNonNull(JsonNullable.<String>undefined(), applied::add);

        assertThat(applied).isEmpty();
    }

    @Test
    @DisplayName("overlayNonNull keeps the baseline for a present null")
    void overlayNonNullIgnoresPresentNull() {
        PatchOverlay.overlayNonNull(JsonNullable.<String>of(null), applied::add);

        assertThat(applied).isEmpty();
    }

    @Test
    @DisplayName("overlayNonNull applies a present value")
    void overlayNonNullAppliesPresentValue() {
        PatchOverlay.overlayNonNull(JsonNullable.of("value"), applied::add);

        assertThat(applied).containsExactly("value");
    }
}
