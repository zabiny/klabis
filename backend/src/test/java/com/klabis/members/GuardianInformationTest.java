package com.klabis.members;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GuardianInformation Value Object Tests")
class GuardianInformationTest {

    @Test
    @DisplayName("Should create valid guardian information")
    void shouldCreateValidGuardianInformation() {
        // Given
        String firstName = "Jane";
        String lastName = "Smith";
        String relationship = "PARENT";
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        // When
        GuardianInformation guardian = new GuardianInformation(
                firstName, lastName, relationship, email, phone
        );

        // Then
        assertThat(guardian.getFirstName()).isEqualTo(firstName);
        assertThat(guardian.getLastName()).isEqualTo(lastName);
        assertThat(guardian.getRelationship()).isEqualTo(relationship);
        assertThat(guardian.getEmail()).isEqualTo(email);
        assertThat(guardian.getEmailValue()).isEqualTo("jane.smith@example.com");
        assertThat(guardian.getPhone()).isEqualTo(phone);
        assertThat(guardian.getPhoneValue()).isEqualTo("+420123456789");
    }

    @Test
    @DisplayName("Should throw exception when relationship is null")
    void shouldThrowWhenRelationshipIsNull() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        assertThatThrownBy(() -> new GuardianInformation(
                "Jane", "Smith", null, email, phone
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guardian relationship is required");
    }

    @Test
    @DisplayName("Should throw exception when relationship is blank")
    void shouldThrowWhenRelationshipIsBlank() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        assertThatThrownBy(() -> new GuardianInformation(
                "Jane", "Smith", "   ", email, phone
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guardian relationship is required");
    }

    @Test
    @DisplayName("Should throw exception when email is null")
    void shouldThrowWhenEmailIsNull() {
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        assertThatThrownBy(() -> new GuardianInformation(
                "Jane", "Smith", "PARENT", null, phone
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guardian email is required");
    }

    @Test
    @DisplayName("Should throw exception when phone is null")
    void shouldThrowWhenPhoneIsNull() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");

        assertThatThrownBy(() -> new GuardianInformation(
                "Jane", "Smith", "PARENT", email, null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Guardian phone is required");
    }

    @Test
    @DisplayName("Should reject invalid email through EmailAddress validation")
    void shouldRejectInvalidEmail() {
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        assertThatThrownBy(() -> {
            EmailAddress invalidEmail = EmailAddress.of("invalid-email");
            new GuardianInformation("Jane", "Smith", "PARENT", invalidEmail, phone);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    @DisplayName("Should reject invalid phone through PhoneNumber validation")
    void shouldRejectInvalidPhone() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");

        assertThatThrownBy(() -> {
            PhoneNumber invalidPhone = PhoneNumber.of("123456789"); // Missing +
            new GuardianInformation("Jane", "Smith", "PARENT", email, invalidPhone);
        })
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phone number must start with +");
    }

    @Test
    @DisplayName("Should trim whitespace from relationship")
    void shouldTrimWhitespaceFromRelationship() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian = new GuardianInformation(
                "Jane", "Smith", "  PARENT  ", email, phone
        );

        assertThat(guardian.getRelationship()).isEqualTo("PARENT");
    }

    @Test
    @DisplayName("Should return email value object when present")
    void shouldReturnEmailValueObject() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        assertThat(guardian.getEmail()).isEqualTo(email);
        assertThat(guardian.getEmailValue()).isEqualTo("jane.smith@example.com");
    }

    @Test
    @DisplayName("Should return phone value object when present")
    void shouldReturnPhoneValueObject() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        assertThat(guardian.getPhone()).isEqualTo(phone);
        assertThat(guardian.getPhoneValue()).isEqualTo("+420123456789");
    }

    @Test
    @DisplayName("Should implement equality correctly")
    void shouldImplementEqualityCorrectly() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian1 = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        GuardianInformation guardian2 = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        GuardianInformation guardian3 = new GuardianInformation(
                "John", "Doe", "LEGAL_GUARDIAN",
                EmailAddress.of("john.doe@example.com"),
                PhoneNumber.of("+420987654321")
        );

        assertThat(guardian1).isEqualTo(guardian2);
        assertThat(guardian1).isNotEqualTo(guardian3);
        assertThat(guardian1.hashCode()).isEqualTo(guardian2.hashCode());
    }

    @Test
    @DisplayName("Should have proper toString without exposing sensitive data")
    void shouldHaveProperToString() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        String str = guardian.toString();

        assertThat(str).contains("GuardianInformation");
        assertThat(str).contains("PARENT");
        // Email and phone should not be exposed in toString
        assertThat(str).doesNotContain("jane.smith@example.com");
        assertThat(str).doesNotContain("+420123456789");
    }

    @Test
    @DisplayName("Should accept LEGAL_GUARDIAN relationship")
    void shouldAcceptLegalGuardianRelationship() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        assertThatCode(() -> new GuardianInformation(
                "Jane", "Smith", "LEGAL_GUARDIAN", email, phone
        )).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should create guardian with PersonName correctly")
    void shouldCreateGuardianWithPersonName() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        assertThat(guardian.getName()).isNotNull();
        assertThat(guardian.getName().firstName()).isEqualTo("Jane");
        assertThat(guardian.getName().lastName()).isEqualTo("Smith");
    }

    @Test
    @DisplayName("Should validate email through EmailAddress value object")
    void shouldValidateEmailThroughValueObject() {
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        // EmailAddress validation happens before GuardianInformation construction
        assertThatThrownBy(() -> EmailAddress.of(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> EmailAddress.of("invalid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate phone through PhoneNumber value object")
    void shouldValidatePhoneThroughValueObject() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");

        // PhoneNumber validation happens before GuardianInformation construction
        assertThatThrownBy(() -> PhoneNumber.of(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PhoneNumber.of("123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should maintain immutability through value objects")
    void shouldMaintainImmutability() {
        EmailAddress email = EmailAddress.of("jane.smith@example.com");
        PhoneNumber phone = PhoneNumber.of("+420123456789");

        GuardianInformation guardian = new GuardianInformation(
                "Jane", "Smith", "PARENT", email, phone
        );

        // Value objects are immutable
        assertThat(guardian.getEmail()).isEqualTo(email);
        assertThat(guardian.getPhone()).isEqualTo(phone);

        // Verify objects are the same instance (immutable reference)
        assertThat(guardian.getEmail()).isSameAs(email);
        assertThat(guardian.getPhone()).isSameAs(phone);
    }
}
