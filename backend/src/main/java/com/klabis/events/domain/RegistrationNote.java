package com.klabis.events.domain;

import org.jmolecules.ddd.annotation.ValueObject;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ValueObject
public final class RegistrationNote {

    public static final int MAX_LENGTH = 255;

    private final String text;

    private RegistrationNote(String text) {
        this.text = text;
    }

    /**
     * Blank input means "no note" and yields {@code null}.
     */
    public static @Nullable RegistrationNote ofNullable(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            throw new IllegalArgumentException("Registration note must be at most " + MAX_LENGTH + " characters");
        }
        return new RegistrationNote(trimmed);
    }

    /**
     * For text that is not Klabis input (e.g. taken over from ORIS): never rejects, an over-long text is cut to
     * {@link #MAX_LENGTH} code points.
     */
    public static @Nullable RegistrationNote truncatingOf(@Nullable String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.codePointCount(0, trimmed.length()) > MAX_LENGTH) {
            trimmed = trimmed.substring(0, trimmed.offsetByCodePoints(0, MAX_LENGTH));
        }
        return ofNullable(trimmed);
    }

    public static @Nullable String textOrNull(@Nullable RegistrationNote note) {
        return note == null ? null : note.text;
    }

    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegistrationNote that && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "RegistrationNote[***]";
    }
}
