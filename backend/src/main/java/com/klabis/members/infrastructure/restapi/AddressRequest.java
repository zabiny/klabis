package com.klabis.members.infrastructure.restapi;

import com.klabis.members.domain.Address;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "Street is required")
        @Size(max = 200, message = "Street must not exceed 200 characters")
        String street,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must not exceed 100 characters")
        String city,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must not exceed 20 characters")
        @Pattern(regexp = Address.POSTAL_CODE_PATTERN, message = "Postal code must be alphanumeric (hyphens and spaces allowed)")
        String postalCode,

        @NotBlank(message = "Country is required")
        @Pattern(regexp = Address.ISO_3166_ALPHA_2_PATTERN, message = "Country must be a valid ISO 3166-1 alpha-2 code (2 letters)")
        String country
) {
}
