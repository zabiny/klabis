package com.klabis.events.infrastructure.restapi;

import com.klabis.common.ui.HalForms;
import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.Instant;

@RecordBuilder
public record RegistrationDto(
        @HalForms(access = HalForms.Access.READ_ONLY) String firstName,
        @HalForms(access = HalForms.Access.READ_ONLY) String lastName,
        String siCardNumber,
        @HalForms(access = HalForms.Access.READ_ONLY) String category,
        @HalForms(access = HalForms.Access.READ_ONLY) Instant registeredAt
) {
}
