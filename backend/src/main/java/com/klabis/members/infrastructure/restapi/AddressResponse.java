package com.klabis.members.infrastructure.restapi;

import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record AddressResponse(
        String street,
        String city,
        String postalCode,
        String country
) {
}
