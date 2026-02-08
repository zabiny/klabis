package com.klabis.members.management;

import com.klabis.members.Address;

/**
 * Response DTO for address information.
 * <p>
 * This DTO is used to transfer address data from the application layer to the presentation layer.
 * It represents the same data as the domain {@link Address} value object.
 *
 * @param street     street address
 * @param city       city name
 * @param postalCode postal code
 * @param country    ISO 3166-1 alpha-2 country code (2 letters, uppercase)
 */
record AddressResponse(
        String street,
        String city,
        String postalCode,
        String country
) {
    /**
     * Creates an AddressResponse from a domain Address value object.
     * <p>
     * This method is null-safe and will return null if the input address is null.
     *
     * @param address the domain Address value object (may be null)
     * @return AddressResponse DTO, or null if address is null
     */
    public static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.street(),
                address.city(),
                address.postalCode(),
                address.country()
        );
    }
}
