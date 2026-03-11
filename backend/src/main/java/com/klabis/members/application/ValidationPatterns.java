package com.klabis.members.application;

public final class ValidationPatterns {

    private ValidationPatterns() {
    }

    public static final String NUMERIC_ONLY_PATTERN = "^[0-9]+$";

    public static final String MESSAGE_COUNTRY_INVALID = "Country must be a valid ISO 3166-1 alpha-2 code (2 letters)";
}
