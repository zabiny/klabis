package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.Address;
import com.klabis.members.management.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for address information.
 * <p>
 * This DTO is used to capture address data in API requests.
 * It follows the same validation rules as the domain {@link Address} value object.
 *
 * @param street     street address (required, not blank, max 200 characters)
 * @param city       city name (required, not blank, max 100 characters)
 * @param postalCode postal code (required, not blank, max 20 characters)
 * @param country    ISO 3166-1 alpha-2 country code (required, 2 letters)
 */
public record AddressRequest(
        @NotBlank(message = "Street is required")
        @Size(max = 200, message = "Street must not exceed 200 characters")
        String street,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        @Pattern(regexp = ValidationPatterns.POSTAL_CODE_PATTERN, message = ValidationPatterns.MESSAGE_POSTAL_CODE_INVALID)
        String postalCode,

        @NotBlank(message = "Country is required")
        @Pattern(regexp = ValidationPatterns.ISO_3166_ALPHA_2_PATTERN, message = ValidationPatterns.MESSAGE_COUNTRY_INVALID)
        String country
) {
}
