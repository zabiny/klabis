package com.klabis.members;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Address Value Object Tests")
class AddressTest extends ValueObjectTestBase<Address> {

    @Override
    protected Address createValidValue() {
        return Address.of("Hlavní 123", "Praha", "11000", "CZ");
    }

    @Override
    protected Address createDifferentValue() {
        return Address.of("Different", "Brno", "60200", "CZ");
    }

    @Override
    protected String expectedToString() {
        // Records have specific toString format, we verify it contains key values
        return "Address{street='Hlavní 123', city='Praha', postalCode='11000', country='CZ'}";
    }

    @Test
    @DisplayName("Should create valid address with all fields")
    void shouldCreateValidAddress() {
        // Given
        String street = "Hlavní 123";
        String city = "Praha";
        String postalCode = "11000";
        String country = "CZ";

        // When
        Address address = Address.of(street, city, postalCode, country);

        // Then
        assertThat(address.street()).isEqualTo(street);
        assertThat(address.city()).isEqualTo(city);
        assertThat(address.postalCode()).isEqualTo(postalCode);
        assertThat(address.country()).isEqualTo(country);
    }

    @Nested
    @DisplayName("Street Validation")
    class StreetValidation {

        @Test
        @DisplayName("Should throw exception when street is null")
        void shouldThrowWhenStreetIsNull() {
            assertThatThrownBy(() -> Address.of(null, "Praha", "11000", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Street is required");
        }

        @Test
        @DisplayName("Should throw exception when street is blank")
        void shouldThrowWhenStreetIsBlank() {
            assertThatThrownBy(() -> Address.of("   ", "Praha", "11000", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Street is required");
        }

        @Test
        @DisplayName("Should throw exception when street exceeds maximum length")
        void shouldThrowWhenStreetExceedsMaxLength() {
            String longStreet = "a".repeat(201);
            assertThatThrownBy(() -> Address.of(longStreet, "Praha", "11000", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Street must not exceed 200 characters");
        }

        @Test
        @DisplayName("Should accept street at maximum length")
        void shouldAcceptStreetAtMaxLength() {
            String maxStreet = "a".repeat(200);
            assertThatCode(() -> Address.of(maxStreet, "Praha", "11000", "CZ"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("City Validation")
    class CityValidation {

        @Test
        @DisplayName("Should throw exception when city is null")
        void shouldThrowWhenCityIsNull() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", null, "11000", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City is required");
        }

        @Test
        @DisplayName("Should throw exception when city is blank")
        void shouldThrowWhenCityIsBlank() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "   ", "11000", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City is required");
        }

        @Test
        @DisplayName("Should throw exception when city exceeds maximum length")
        void shouldThrowWhenCityExceedsMaxLength() {
            String longCity = "a".repeat(101);
            assertThatThrownBy(() -> Address.of("Hlavní 123", longCity, "11000", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("City must not exceed 100 characters");
        }

        @Test
        @DisplayName("Should accept city at maximum length")
        void shouldAcceptCityAtMaxLength() {
            String maxCity = "a".repeat(100);
            assertThatCode(() -> Address.of("Hlavní 123", maxCity, "11000", "CZ"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Postal Code Validation")
    class PostalCodeValidation {

        @Test
        @DisplayName("Should throw exception when postal code is null")
        void shouldThrowWhenPostalCodeIsNull() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", null, "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Postal code is required");
        }

        @Test
        @DisplayName("Should throw exception when postal code is blank")
        void shouldThrowWhenPostalCodeIsBlank() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "   ", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Postal code is required");
        }

        @Test
        @DisplayName("Should throw exception when postal code exceeds maximum length")
        void shouldThrowWhenPostalCodeExceedsMaxLength() {
            String longPostalCode = "a".repeat(21);
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", longPostalCode, "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Postal code must not exceed 20 characters");
        }

        @Test
        @DisplayName("Should accept postal code at maximum length")
        void shouldAcceptPostalCodeAtMaxLength() {
            String maxPostalCode = "a".repeat(20);
            assertThatCode(() -> Address.of("Hlavní 123", "Praha", maxPostalCode, "CZ"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception for invalid postal code with special characters")
        void shouldThrowForInvalidPostalCodeWithSpecialCharacters() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "110@00", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Postal code must be alphanumeric");

            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "110!00", "CZ"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Postal code must be alphanumeric");
        }

        @Test
        @DisplayName("Should accept valid postal code with hyphen")
        void shouldAcceptValidPostalCodeWithHyphen() {
            assertThatCode(() -> Address.of("Hlavní 123", "Praha", "110-00", "CZ"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid postal code with space")
        void shouldAcceptValidPostalCodeWithSpace() {
            assertThatCode(() -> Address.of("Hlavní 123", "Praha", "110 00", "CZ"))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Country Validation")
    class CountryValidation {

        @Test
        @DisplayName("Should throw exception when country is null")
        void shouldThrowWhenCountryIsNull() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "11000", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country is required");
        }

        @Test
        @DisplayName("Should throw exception when country is blank")
        void shouldThrowWhenCountryIsBlank() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "11000", "   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country is required");
        }

        @Test
        @DisplayName("Should accept valid 2-letter country codes including XX")
        void shouldAcceptValid2LetterCountryCodes() {
            // XX is a valid 2-letter format even though it's not a real country code
            // We validate format, not existence in ISO registry
            assertThatCode(() -> Address.of("Hlavní 123", "Praha", "11000", "XX"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should throw exception for invalid country code with numbers")
        void shouldThrowForInvalidCountryCodeWithNumbers() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "11000", "12"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country must be a valid ISO 3166-1 alpha-2 code");
        }

        @Test
        @DisplayName("Should throw exception for invalid country code with 3 letters")
        void shouldThrowForInvalidCountryCodeWith3Letters() {
            assertThatThrownBy(() -> Address.of("Hlavní 123", "Praha", "11000", "ABC"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country must be a valid ISO 3166-1 alpha-2 code");
        }

        @Test
        @DisplayName("Should accept valid ISO 3166-1 alpha-2 country codes")
        void shouldAcceptValidISOCountryCodes() {
            assertThatCode(() -> Address.of("Hlavní 123", "Praha", "11000", "CZ"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> Address.of("Main St", "New York", "10001", "US"))
                    .doesNotThrowAnyException();
            assertThatCode(() -> Address.of("High St", "London", "SW1A", "GB"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should normalize country code to uppercase")
        void shouldNormalizeCountryCodeToUppercase() {
            Address address = Address.of("Hlavní 123", "Praha", "11000", "cz");
            assertThat(address.country()).isEqualTo("CZ");
        }
    }

    @Nested
    @DisplayName("Field Normalization")
    class FieldNormalization {

        @Test
        @DisplayName("Should trim whitespace from all fields")
        void shouldTrimWhitespaceFromAllFields() {
            Address address = Address.of("  Hlavní 123  ", "  Praha  ", "  11000  ", "  CZ  ");
            assertThat(address.street()).isEqualTo("Hlavní 123");
            assertThat(address.city()).isEqualTo("Praha");
            assertThat(address.postalCode()).isEqualTo("11000");
            assertThat(address.country()).isEqualTo("CZ");
        }
    }
}
