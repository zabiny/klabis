package com.klabis.members;

import java.time.LocalDate;

public record MemberAccommodationDto(
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
