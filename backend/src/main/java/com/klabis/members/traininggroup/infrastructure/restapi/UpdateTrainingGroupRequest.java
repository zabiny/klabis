package com.klabis.members.traininggroup.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.members.traininggroup.domain.AgeRange;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

record UpdateTrainingGroupRequest(
        PatchField<String> name,
        @Valid PatchField<AgeRangeRequest> ageRange,
        PatchField<List<String>> trainers
) {
    PatchField<AgeRange> ageRangeDomain() {
        return ageRange.map(r -> new AgeRange(r.minAge(), r.maxAge()));
    }

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
