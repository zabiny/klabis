package com.klabis.events.infrastructure.restapi;

import org.springframework.hateoas.server.core.Relation;

import java.time.Instant;

@Relation(collectionRelation = "registrationDtoList")
public record RegistrationSummaryDto(
        String firstName,
        String lastName,
        String category,
        Instant registeredAt
) {
}
