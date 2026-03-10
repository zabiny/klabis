package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;
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

    /**
     * Extracts the date encoded in the birth number.
     * The caller supplies the member's full birth year so the century can be derived.
     * The two-digit year stored in the birth number is combined with that century,
     * meaning a birth number whose two-digit year differs from the member's year will
     * produce a different date and therefore trigger a consistency warning.
     *
     * @param birthYear the member's full birth year (e.g. 1990 or 2000) used to derive the century
     * @return the date encoded in the birth number
     */
    public LocalDate extractDate(int birthYear) {
        String withoutSlash = value.replace("/", "");
        int encodedTwoDigitYear = Integer.parseInt(withoutSlash.substring(0, 2));
        int month = Integer.parseInt(withoutSlash.substring(2, 4));
        int day = Integer.parseInt(withoutSlash.substring(4, 6));

        int century = (birthYear / 100) * 100;
        int actualYear = century + encodedTwoDigitYear;
        int actualMonth = toActualMonth(month);
        return LocalDate.of(actualYear, actualMonth, day);
    }

    /**
     * Returns the gender indicated by the month component of the birth number.
     * Months 01-12 and 21-32 indicate MALE; months 51-62 and 71-82 indicate FEMALE.
     */
    public Gender indicatesGender() {
        String withoutSlash = value.replace("/", "");
        int month = Integer.parseInt(withoutSlash.substring(2, 4));
        return isFemaleMonth(month) ? Gender.FEMALE : Gender.MALE;
    }

    private static int toActualMonth(int month) {
        if (month >= 21 && month <= 32) {
            return month - 20;
        } else if (month >= 51 && month <= 62) {
            return month - 50;
        } else if (month >= 71 && month <= 82) {
            return month - 70;
        }
        return month;
    }

    private static boolean isFemaleMonth(int month) {
        return (month >= 51 && month <= 62) || (month >= 71 && month <= 82);
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
        String monthPart = withoutSlash.substring(2, 4);
        String dayPart = withoutSlash.substring(4, 6);

        int month = Integer.parseInt(monthPart);
        int day = Integer.parseInt(dayPart);

        int adjustedMonth = toActualMonth(month);

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
