package com.klabis.usergroups.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import jakarta.validation.constraints.Min;

import java.util.List;
import java.util.UUID;

record UpdateTrainingGroupRequest(
        PatchField<String> name,
        @Min(0) PatchField<Integer> minAge,
        @Min(0) PatchField<Integer> maxAge,
        PatchField<List<String>> trainers
) {
    PatchField<List<UUID>> trainerUuids() {
        return trainers.map(list -> list.stream().map(UUID::fromString).toList());
    }
}
