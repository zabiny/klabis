package com.klabis.usergroups.infrastructure.restapi;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

record CreateTrainingGroupRequest(
        @NotBlank String name,
        @NotNull @Min(0) Integer minAge,
        @NotNull @Min(0) Integer maxAge
) {
}
