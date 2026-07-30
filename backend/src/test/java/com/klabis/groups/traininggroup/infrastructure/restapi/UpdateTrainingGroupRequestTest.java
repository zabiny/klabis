package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                PatchField.notProvided(), PatchField.notProvided(), PatchField.of(List.of(trainer)));

        assertThat(request.trainers().throwIfNotProvided()).containsExactly(trainer);
    }

    @Test
    @DisplayName("trainers field is provided but null when HAL forms sends explicit null for untouched list")
    void trainersExplicitlyNull() {
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                PatchField.notProvided(), PatchField.notProvided(), PatchField.of(null));

        assertThat(request.trainers().isProvided()).isTrue();
        assertThat(request.trainers().throwIfNotProvided()).isNull();
    }

    @Test
    @DisplayName("trainers field is not provided when absent from request")
    void trainersNotProvided() {
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                PatchField.notProvided(), PatchField.notProvided(), PatchField.notProvided());

        assertThat(request.trainers().isProvided()).isFalse();
    }

    @Test
    @DisplayName("ageRangeDomain maps AgeRangeRequest to domain AgeRange when provided")
    void ageRangeDomainMapsWhenProvided() {
        // Constructed via the RecordBuilder, not positionally: the generated AgeRangeRequest
        // record's component order (maxAge, minAge) comes from the bundler alphabetizing schema
        // properties and does not match the spec's declaration order (minAge, maxAge).
        AgeRangeRequest ageRangeRequest = AgeRangeRequestBuilder.builder().minAge(10).maxAge(18).build();
        UpdateTrainingGroupRequest request = new UpdateTrainingGroupRequest(
                PatchField.notProvided(), PatchField.of(ageRangeRequest), PatchField.notProvided());

        var ageRange = request.ageRangeDomain().throwIfNotProvided();
        assertThat(ageRange.minAge()).isEqualTo(10);
        assertThat(ageRange.maxAge()).isEqualTo(18);
    }
}
