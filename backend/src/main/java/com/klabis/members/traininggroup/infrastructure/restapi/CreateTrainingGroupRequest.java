package com.klabis.members.traininggroup.infrastructure.restapi;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record CreateTrainingGroupRequest(
        @NotBlank String name,
        @NotNull UUID trainerId,
        @NotNull @Min(0) Integer minAge,
        @NotNull @Min(0) Integer maxAge
) {
}
