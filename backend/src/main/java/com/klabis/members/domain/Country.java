package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * A country identified by its ISO 3166-1 alpha-2 code, normalized to upper case.
 */
@ValueObject
public record Country(String code) {

    private static final String ISO_3166_ALPHA_2_PATTERN = "^[A-Za-z]{2}$";

    public Country {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
        code = code.trim().toUpperCase();
        if (!code.matches(ISO_3166_ALPHA_2_PATTERN)) {
            throw new IllegalArgumentException("Country must be a valid ISO 3166-1 alpha-2 code (2 letters)");
        }
    }

    public static Country of(String code) {
        return new Country(code);
    }

    @Override
    public String toString() {
        return code;
    }
}
