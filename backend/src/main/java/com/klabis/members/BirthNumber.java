package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.regex.Pattern;

@ValueObject
public record BirthNumber(String value) {

    private static final Pattern FORMAT_WITH_SLASH = Pattern.compile("^\\d{6}/\\d{4}$");
    private static final Pattern FORMAT_WITHOUT_SLASH = Pattern.compile("^\\d{10}$");

    public BirthNumber {
        if (value == null || value.trim().isBlank()) {
            throw new IllegalArgumentException("Birth number is required");
        }

        String normalized = normalize(value.trim());

        if (!isValidFormat(normalized)) {
            throw new IllegalArgumentException("Invalid birth number format: must be RRMMDD/XXXX or RRMMDDXXXX");
        }

        validateDate(normalized);
        value = normalized;
    }

    public static BirthNumber of(String value) {
        return new BirthNumber(value);
    }

    private static String normalize(String value) {
        if (FORMAT_WITHOUT_SLASH.matcher(value).matches()) {
            return value.substring(0, 6) + "/" + value.substring(6, 10);
        }
        return value;
    }

    private static boolean isValidFormat(String value) {
        return FORMAT_WITH_SLASH.matcher(value).matches();
    }

    private static void validateDate(String value) {
        String withoutSlash = value.replace("/", "");
        String yearPart = withoutSlash.substring(0, 2);
        String monthPart = withoutSlash.substring(2, 4);
        String dayPart = withoutSlash.substring(4, 6);

        int month = Integer.parseInt(monthPart);
        int day = Integer.parseInt(dayPart);

        int adjustedMonth = month;
        if (month >= 21 && month <= 32) {
            adjustedMonth = month - 20;
        } else if (month >= 51 && month <= 62) {
            adjustedMonth = month - 50;
        }

        if (adjustedMonth < 1 || adjustedMonth > 12) {
            throw new IllegalArgumentException("Invalid month in birth number: " + value);
        }

        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid day in birth number: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
