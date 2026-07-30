package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.members.MemberId;
import jakarta.validation.Valid;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;

record UpdateTrainingGroupRequest(
        JsonNullable<String> name,
        @Valid JsonNullable<AgeRangeRequest> ageRange,
        JsonNullable<List<MemberId>> trainers
) {
    JsonNullable<AgeRange> ageRangeDomain() {
        return ageRange.map(r -> new AgeRange(r.minAge(), r.maxAge()));
    }
}
