package com.klabis.members;

import java.time.LocalDate;
import java.util.UUID;

public record MemberAccommodationDto(
        UUID memberId,
        String firstName,
        String lastName,
        String identityCardNumber,
        LocalDate identityCardValidityDate,
        LocalDate dateOfBirth,
        String addressStreet,
        String addressCity,
        String addressPostalCode,
        String addressCountry
) {
}
