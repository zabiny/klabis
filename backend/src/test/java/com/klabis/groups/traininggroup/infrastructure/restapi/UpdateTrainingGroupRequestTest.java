package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UpdateTrainingGroupRequest")
class UpdateTrainingGroupRequestTest {

    @Test
    @DisplayName("trainers field carries MemberId list when provided")
    void trainersProvided() {
        MemberId trainer = new MemberId(UUID.randomUUID());
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.of(List.of(trainer)));

        assertThat(request.trainers().orElseThrow()).containsExactly(trainer);
    }

    @Test
    @DisplayName("trainers field is provided but null when HAL forms sends explicit null for untouched list")
    void trainersExplicitlyNull() {
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.of(null));

        assertThat(request.trainers().isPresent()).isTrue();
        assertThat(request.trainers().orElseThrow()).isNull();
    }

    @Test
    @DisplayName("trainers field is not provided when absent from request")
    void trainersNotProvided() {
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());

        assertThat(request.trainers().isPresent()).isFalse();
    }

    @Test
    @DisplayName("ageRangeDomain maps AgeRangeRequest to domain AgeRange when provided")
    void ageRangeDomainMapsWhenProvided() {
        // Constructed via the RecordBuilder, not positionally: the generated AgeRangeRequest
        // record's component order (maxAge, minAge) comes from the bundler alphabetizing schema
        // properties and does not match the spec's declaration order (minAge, maxAge).
        AgeRangeRequest ageRangeRequest = AgeRangeRequestBuilder.builder().minAge(10).maxAge(18).build();
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                JsonNullable.undefined(), JsonNullable.of(ageRangeRequest), JsonNullable.undefined());

        var ageRange = request.ageRangeDomain().orElseThrow();
        assertThat(ageRange.minAge()).isEqualTo(10);
        assertThat(ageRange.maxAge()).isEqualTo(18);
    }

    @Test
    @DisplayName("ageRangeDomain forwards an explicit null instead of dereferencing it")
    void ageRangeDomainForwardsExplicitNull() {
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                JsonNullable.undefined(), JsonNullable.of(null), JsonNullable.undefined());

        var ageRange = request.ageRangeDomain();

        assertThat(ageRange.isPresent()).isTrue();
        assertThat(ageRange.orElseThrow()).isNull();
    }

    @Test
    @DisplayName("ageRangeDomain stays undefined when the field is absent")
    void ageRangeDomainStaysUndefinedWhenAbsent() {
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                JsonNullable.undefined(), JsonNullable.undefined(), JsonNullable.undefined());

        assertThat(request.ageRangeDomain().isPresent()).isFalse();
    }
}
