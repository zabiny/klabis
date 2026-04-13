package com.klabis.members.familygroup.infrastructure.restapi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record CreateFamilyGroupRequest(
        @NotBlank String name,
        @NotNull UUID parent
) {
}
