package com.klabis.members.traininggroup.infrastructure.restapi;

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
        if (!trainers.isProvided()) {
            return PatchField.notProvided();
        }
        List<String> trainerList = trainers.throwIfNotProvided();
        if (trainerList == null) {
            return PatchField.notProvided();
        }
        return trainers.map(list -> list.stream()
                .map(s -> {
                    try {
                        return UUID.fromString(s);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid trainer UUID: '%s'".formatted(s));
                    }
                })
                .toList());
    }
}
