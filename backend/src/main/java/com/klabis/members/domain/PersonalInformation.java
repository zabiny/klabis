package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Value Object representing personal information of a member.
 * <p>
 * Immutable value object containing a member's personal details:
 * - Name (first and last)
 * - Date of birth
 * - Nationality
 * - Gender
 * <p>
 * This value object encapsulates validation logic for personal information
 * and provides utility methods for age calculation.
 */
@ValueObject
public final class PersonalInformation {

    private final PersonName name;
    private final LocalDate dateOfBirth;
    private final Nationality nationality;
    private final Gender gender;

    private PersonalInformation(
            PersonName name,
            LocalDate dateOfBirth,
            Nationality nationality,
            Gender gender) {

        this.name = name;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.gender = gender;
    }

    /**
     * Creates a PersonalInformation value object with validation.
     *
     * @param firstName       member's first name (required, not blank)
     * @param lastName        member's last name (required, not blank)
     * @param dateOfBirth     member's date of birth (required, not in the future)
     * @param nationalityCode member's nationality as ISO 3166-1 code (required)
     * @param gender          member's gender (required)
     * @return PersonalInformation value object
     * @throws IllegalArgumentException if validation fails
     */
    public static PersonalInformation of(
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String nationalityCode,
            Gender gender) {

        validateDateOfBirth(dateOfBirth);
        validateGender(gender);

        PersonName name = PersonName.of(firstName, lastName);
        Nationality nationality = Nationality.of(nationalityCode);

        return new PersonalInformation(
                name,
                dateOfBirth,
                nationality,
                gender
        );
    }

    private static void validateDateOfBirth(LocalDate dateOfBirth) {
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }

        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
    }

    private static void validateGender(Gender gender) {
        if (gender == null) {
            throw new IllegalArgumentException("Gender is required");
        }
    }

    /**
     * Calculates the member's current age based on date of birth.
     *
     * @return age in years
     */
    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Checks if the member is a minor (under 18 years old).
     *
     * @return true if member is under 18, false otherwise
     */
    public boolean isMinor() {
        return getAge() < 18;
    }

    // Getters

    public PersonName getName() {
        return name;
    }

    public String getFirstName() {
        return name.firstName();
    }

    public String getLastName() {
        return name.lastName();
    }

    public String getFullName() {
        return name.fullName();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Returns the nationality as a Nationality value object.
     *
     * @return the nationality value object
     */
    public Nationality getNationality() {
        return nationality;
    }

    /**
     * Returns the nationality as an ISO 3166-1 country code string.
     *
     * @return the country code (e.g., "CZE", "US")
     */
    public String getNationalityCode() {
        return nationality.code();
    }

    public Gender getGender() {
        return gender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersonalInformation that = (PersonalInformation) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(dateOfBirth, that.dateOfBirth) &&
               Objects.equals(nationality, that.nationality) &&
               gender == that.gender;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, dateOfBirth, nationality, gender);
    }

    @Override
    public String toString() {
        return "PersonalInformation{" +
               "name=" + name +
               ", dateOfBirth=" + dateOfBirth +
               ", nationality=" + nationality +
               ", gender=" + gender +
               '}';
    }
}
