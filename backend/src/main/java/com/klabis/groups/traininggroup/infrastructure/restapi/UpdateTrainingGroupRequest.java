package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.members.MemberId;
import jakarta.validation.Valid;

import java.util.List;

record UpdateTrainingGroupRequest(
        PatchField<String> name,
        @Valid PatchField<AgeRangeRequest> ageRange,
        PatchField<List<MemberId>> trainers
) {
    PatchField<AgeRange> ageRangeDomain() {
        return ageRange.map(r -> new AgeRange(r.minAge(), r.maxAge()));
    }
}
