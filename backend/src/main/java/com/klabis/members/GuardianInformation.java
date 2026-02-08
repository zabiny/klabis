package com.klabis.members;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/**
 * Value Object representing guardian information for minor members.
 * <p>
 * Immutable value object containing guardian's contact details.
 * Required for members under 18 years of age.
 */
@ValueObject
public final class GuardianInformation {

    private final PersonName name;
    private final String relationship; // PARENT, LEGAL_GUARDIAN
    private final EmailAddress email;
    private final PhoneNumber phone;

    public GuardianInformation(
            String firstName,
            String lastName,
            String relationship,
            EmailAddress email,
            PhoneNumber phone) {

        this.name = PersonName.of(firstName, lastName);

        if (relationship == null || relationship.isBlank()) {
            throw new IllegalArgumentException("Guardian relationship is required");
        }
        if (email == null) {
            throw new IllegalArgumentException("Guardian email is required");
        }
        if (phone == null) {
            throw new IllegalArgumentException("Guardian phone is required");
        }

        this.relationship = relationship.trim();
        this.email = email;
        this.phone = phone;
    }

    public PersonName getName() {
        return name;
    }

    public String getFirstName() {
        return name.firstName();
    }

    public String getLastName() {
        return name.lastName();
    }

    public String getRelationship() {
        return relationship;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public String getEmailValue() {
        return email.value();
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public String getPhoneValue() {
        return phone.value();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GuardianInformation that = (GuardianInformation) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(relationship, that.relationship) &&
               Objects.equals(email, that.email) &&
               Objects.equals(phone, that.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, relationship, email, phone);
    }

    @Override
    public String toString() {
        return "GuardianInformation{" +
               "name=" + name +
               ", relationship='" + relationship + '\'' +
               '}';
    }
}
