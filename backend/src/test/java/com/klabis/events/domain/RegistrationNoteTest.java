package com.klabis.events.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RegistrationNote value object")
class RegistrationNoteTest {

    @Test
    @DisplayName("trims surrounding whitespace")
    void trimsText() {
        assertThat(RegistrationNote.ofNullable("  hello  ").text()).isEqualTo("hello");
    }

    @Test
    @DisplayName("null and blank input mean no note")
    void blankMeansNoNote() {
        assertThat(RegistrationNote.ofNullable(null)).isNull();
        assertThat(RegistrationNote.ofNullable("")).isNull();
        assertThat(RegistrationNote.ofNullable("   \n ")).isNull();
    }

    @Test
    @DisplayName("accepts exactly 255 characters")
    void acceptsMaxLength() {
        assertThat(RegistrationNote.ofNullable("a".repeat(255)).text()).hasSize(255);
    }

    @Test
    @DisplayName("rejects text longer than 255 characters")
    void rejectsOverLength() {
        assertThatThrownBy(() -> RegistrationNote.ofNullable("a".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("counts code points, not UTF-16 units, against the limit")
    void countsCodePoints() {
        assertThat(RegistrationNote.ofNullable("\uD83D\uDE00".repeat(255))).isNotNull();
        assertThatThrownBy(() -> RegistrationNote.ofNullable("\uD83D\uDE00".repeat(256)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("toString does not reveal the text")
    void toStringIsMasked() {
        assertThat(RegistrationNote.ofNullable("secret").toString()).doesNotContain("secret");
    }

    @Test
    @DisplayName("RegisterCommand toString does not reveal the notes")
    void registerCommandToStringIsMasked() {
        Event.RegisterCommand command = new Event.RegisterCommand("123456", null, false, false,
                RegistrationNote.ofNullable("secret-internal"), RegistrationNote.ofNullable("secret-oris"), false);

        assertThat(command.toString()).doesNotContain("secret-internal", "secret-oris");
    }

    @Test
    @DisplayName("truncatingOf trims, turns blank into no note and cuts to the maximum length in code points")
    void truncatingOf() {
        assertThat(RegistrationNote.truncatingOf(null)).isNull();
        assertThat(RegistrationNote.truncatingOf("   ")).isNull();
        assertThat(RegistrationNote.truncatingOf("  x  ").text()).isEqualTo("x");
        assertThat(RegistrationNote.truncatingOf("a".repeat(300)).text()).hasSize(RegistrationNote.MAX_LENGTH);
        assertThat(RegistrationNote.truncatingOf("\uD83D\uDE00".repeat(300)).text().codePointCount(0,
                RegistrationNote.truncatingOf("\uD83D\uDE00".repeat(300)).text().length())).isEqualTo(255);
        assertThat(RegistrationNote.truncatingOf("a".repeat(254) + " bbb").text()).isEqualTo("a".repeat(254));
    }
}
