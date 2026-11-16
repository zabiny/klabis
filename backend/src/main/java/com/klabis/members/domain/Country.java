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

    public Country {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Country is required");
        }
        code = code.trim().toUpperCase();
        if (!KNOWN_CODES.contains(code)) {
            throw new IllegalArgumentException("Country must be an existing country (ISO 3166-1 alpha-2)");
        }
    }

    public static Country of(String code) {
        return new Country(code);
    }

    /**
     * Single null-tolerant String-to-Country conversion for adapters outside the request/response mappers.
     */
    public static Country ofNullable(String code) {
        return code == null ? null : of(code);
    }

    public static String codeOf(Country country) {
        return country == null ? null : country.code();
    }

    @Override
    public String toString() {
        return code;
    }
}
