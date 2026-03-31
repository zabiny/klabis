package com.klabis.usergroups.infrastructure.restapi;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record UpdateAgeRangeRequest(
        @NotNull @Min(0) Integer minAge,
        @NotNull @Min(0) Integer maxAge
) {
}
