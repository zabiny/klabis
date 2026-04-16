package com.klabis.members.traininggroup.infrastructure.restapi;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

record AgeRangeRequest(
        @NotNull @Min(0) Integer minAge,
        @NotNull @Min(0) Integer maxAge
) {
}
