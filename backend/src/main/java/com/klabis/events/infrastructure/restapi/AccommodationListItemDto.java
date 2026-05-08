package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;

@Relation(collectionRelation = "accommodationList")
@JsonInclude(JsonInclude.Include.NON_NULL)
record AccommodationListItemDto(
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
