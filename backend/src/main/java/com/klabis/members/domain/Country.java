package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Locale;
import java.util.Set;

/**
 * A country identified by its ISO 3166-1 alpha-2 code, normalized to upper case.
 */
@ValueObject
public record Country(String code) {

    private static final Set<String> KNOWN_CODES = Set.of(Locale.getISOCountries());

    private static final String ISO_3166_ALPHA_2_PATTERN = "^[A-Za-z]{2}$";

    public Country {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
        code = code.trim().toUpperCase();
        if (!code.matches(ISO_3166_ALPHA_2_PATTERN)) {
            throw new IllegalArgumentException("Country must be a valid ISO 3166-1 alpha-2 code (2 letters)");
        }
        if (!KNOWN_CODES.contains(code)) {
            throw new IllegalArgumentException("Country must be an existing country (ISO 3166-1 alpha-2)");
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
