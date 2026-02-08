package com.klabis.members.management;

import com.klabis.members.Gender;

import java.time.LocalDate;

/**
 * Builder class for creating test data for member-related tests.
 * <p>
 * Provides factory methods for creating common test objects with sensible defaults,
 * reducing duplication across test files.
 * <p>
 * All builders use immutable data and return new instances for each call.
 */
final class MemberManagementDtosTestDataBuilder {

    // Test constants
    public static final LocalDate DEFAULT_ADULT_DATE_OF_BIRTH = LocalDate.of(2005, 5, 15);

    private MemberManagementDtosTestDataBuilder() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a default address request for testing.
     *
     * @return a valid AddressRequest with default values
     */
    public static AddressRequest defaultAddressRequest() {
        return new AddressRequest(
                "Hlavní 123",
                "Praha",
                "11000",
                "CZ"
        );
    }

    /**
     * Creates a default minor member registration request with guardian.
     * <p>
     * Uses default values for all fields, includes guardian information.
     *
     * @return a valid RegisterMemberRequest for a minor member with guardian
     */
    public static RegisterMemberRequest defaultMinorWithGuardianDto() {
        return new RegisterMemberRequest(
                "Petra",
                "Nováková",
                LocalDate.now().minusYears(10),
                "CZ",
                Gender.FEMALE,
                "petra.novakova@example.com",
                "+420111222333",
                defaultAddressRequest(),
                defaultGuardianDto()
        );
    }

    /**
     * Creates a default guardian DTO for testing.
     *
     * @return a valid GuardianDTO with default values
     */
    public static GuardianDTO defaultGuardianDto() {
        return new GuardianDTO(
                "Pavel",
                "Novák",
                "PARENT",
                "pavel.novak@example.com",
                "+420987654321"
        );
    }

    /**
     * Creates a member request with custom date of birth.
     *
     * @param dateOfBirth the date of birth
     * @return a valid RegisterMemberRequest with custom date of birth
     */
    public static RegisterMemberRequest registerMemberRequestWithDateOfBirth(LocalDate dateOfBirth) {
        return new RegisterMemberRequest(
                "Jan",
                "Novák",
                dateOfBirth,
                "CZ",
                Gender.MALE,
                "jan.novak@example.com",
                "+420777123456",
                defaultAddressRequest(),
                null
        );
    }

    /**
     * Creates an address request with custom street and city.
     *
     * @param street the street address
     * @param city   the city name
     * @return a valid AddressRequest with custom street and city
     */
    public static AddressRequest addressRequestWithStreetAndCity(String street, String city) {
        return new AddressRequest(street, city, "11000", "CZ");
    }

}
