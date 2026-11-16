package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * A person's nationality, expressed as the {@link Country} whose citizenship they hold.
 */
@ValueObject
public record Nationality(Country country) {

    public Nationality {
        Objects.requireNonNull(country, "Nationality country is required");
    }

    public static Nationality of(String code) {
        return new Nationality(Country.of(code));
    }

    public String code() {
        return country.code();
    }

    public boolean isCzech() {
        return "CZ".equals(code());
    }

    public String displayName() {
        return code();
    }

    @Override
    public String toString() {
        return code();
    }
}
