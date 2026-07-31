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
    /**
     * An explicit {@code null} is forwarded as a present null rather than dereferenced here, so the
     * domain's own {@code Assert.notNull} rejects it as a 400. Mapping it would NPE into a 500.
     */
    JsonNullable<AgeRange> ageRangeDomain() {
        return ageRange.map(r -> r == null ? null : new AgeRange(r.minAge(), r.maxAge()));
    }
}
