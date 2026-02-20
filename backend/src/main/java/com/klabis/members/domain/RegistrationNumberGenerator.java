package com.klabis.members.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain service for generating unique registration numbers.
 * <p>
 * Generates registration numbers in format XXXYYDD where:
 * - XXX: Club code (3 characters)
 * - YY: Birth year (last 2 digits)
 * - DD: Sequential number (01-99)
 * <p>
 * Example: ZBM0501 = ZBM club, born 2005, 1st member of that birth year
 */
public class RegistrationNumberGenerator {

    private static final int MAX_SEQUENCE_NUMBER = 99;

    private final String clubCode;
    private final Members members;

    /**
     * Creates a new RegistrationNumberGenerator.
     *
     * @param clubCode         club code (exactly 3 characters)
     * @param members repository for querying existing members
     * @throws IllegalArgumentException if club code is invalid
     */
    public RegistrationNumberGenerator(String clubCode, Members members) {
        if (clubCode == null || clubCode.isBlank()) {
            throw new IllegalArgumentException("Club code is required");
        }
        if (clubCode.length() != 3) {
            throw new IllegalArgumentException("Club code must be exactly 3 characters");
        }

        this.clubCode = clubCode.toUpperCase();
        this.members = Objects.requireNonNull(members, "Member repository is required");
    }

    /**
     * Generates a new registration number for a member.
     *
     * @param dateOfBirth member's date of birth
     * @return generated registration number
     * @throws IllegalArgumentException if dateOfBirth is null
     * @throws IllegalStateException    if maximum sequence number is exceeded
     */
    public RegistrationNumber generate(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required for registration number generation");
        }

        int birthYear = dateOfBirth.getYear();
        int nextSequence = members.countByBirthYear(birthYear);

        if (nextSequence > MAX_SEQUENCE_NUMBER) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot generate registration number for birth year %d: " +
                            "maximum sequence number (%d) exceeded",
                            birthYear,
                            MAX_SEQUENCE_NUMBER
                    )
            );
        }

        // Extract last 2 digits of birth year
        int birthYearTwoDigits = birthYear % 100;

        // Format: XXXYYDD
        String registrationNumberValue = String.format(
                "%s%02d%02d",
                clubCode,
                birthYearTwoDigits,
                nextSequence
        );

        return new RegistrationNumber(registrationNumberValue);
    }
}
