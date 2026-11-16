package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import org.springframework.util.Assert;

/**
 * A person's nationality, expressed as the {@link Country} whose citizenship they hold.
 */
@ValueObject
public record Nationality(Country country) {

    public Nationality {
        Assert.notNull(country, "Nationality country is required");
    }

    public static Nationality of(String code) {
        return new Nationality(Country.of(code));
    }

    public static Nationality ofNullable(String code) {
        return code == null ? null : of(code);
    }

    public static String codeOf(Nationality nationality) {
        return nationality == null ? null : nationality.code();
    }

    public String code() {
        return country.code();
    }

    public boolean isCzech() {
        return "CZ".equals(code());
    }

    @Override
    public String toString() {
        return code();
    }
}
