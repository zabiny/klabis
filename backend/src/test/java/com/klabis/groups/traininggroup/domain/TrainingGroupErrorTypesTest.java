package com.klabis.groups.traininggroup.domain;

import com.klabis.common.exceptions.InvalidDataException;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.application.MemberAlreadyInTrainingGroupException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Training group error types")
class TrainingGroupErrorTypesTest {

    @Test
    @DisplayName("a member already trained elsewhere names that group and binds the member field")
    void memberAlreadyInTrainingGroup() {
        MemberAlreadyInTrainingGroupException ex = new MemberAlreadyInTrainingGroupException(
                new MemberId(UUID.randomUUID()), new TrainingGroupId(UUID.randomUUID()), "Žáci");

        assertThat(ex.errorType()).isEqualTo("member-already-in-training-group");
        assertThat(ex.field()).isEqualTo("memberId");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("groupName", "Žáci"));
    }

    @Test
    @DisplayName("the member field exists on the add-member request")
    void boundFieldIsRequestProperty() throws Exception {
        assertThat(Arrays.stream(Class.forName("com.klabis.groups.infrastructure.restapi.TrainingGroupAddMemberRequest")
                .getRecordComponents()).map(RecordComponent::getName)).contains("memberId");
    }

    @Test
    @DisplayName("an overlapping age range reports the refused range")
    void overlappingAgeRange() {
        AgeRange.OverlappingAgeRangeException ex = new AgeRange.OverlappingAgeRangeException(new AgeRange(10, 12));

        assertThat(ex.errorType()).isEqualTo("overlapping-age-range");
        assertThat(ex.messageParameters()).containsOnly(Map.entry("minAge", 10), Map.entry("maxAge", 12));
    }

    @Test
    @DisplayName("a maximum age below the minimum is an invalid age range reporting both ages")
    void maxAgeBelowMinAge() {
        assertThatThrownBy(() -> new AgeRange(12, 10))
                .isInstanceOfSatisfying(InvalidAgeRangeException.class, ex -> {
                    assertThat(ex).isInstanceOf(InvalidDataException.class);
                    assertThat(ex.errorType()).isEqualTo("invalid-age-range");
                    assertThat(ex.messageParameters()).containsOnly(Map.entry("minAge", 12), Map.entry("maxAge", 10));
                });
    }

    @Test
    @DisplayName("replacing trainers with none is refused at the trainers field")
    void trainerRequired() {
        TrainingGroup group = TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Žáci",
                new MemberId(UUID.randomUUID()), new AgeRange(10, 12)));

        assertThatThrownBy(() -> group.replaceTrainers(Set.of()))
                .isInstanceOfSatisfying(TrainerRequiredException.class, ex -> {
                    assertThat(ex.errorType()).isEqualTo("trainer-required");
                    assertThat(ex.field()).isEqualTo("trainers");
                    assertThat(ex.messageParameters()).containsOnly(Map.entry("groupName", "Žáci"));
                });
    }

    @Test
    @DisplayName("the trainers field exists on the update request")
    void trainersIsRequestProperty() throws Exception {
        assertThat(Arrays.stream(Class.forName("com.klabis.groups.infrastructure.restapi.UpdateTrainingGroupRequest")
                .getRecordComponents()).map(RecordComponent::getName)).contains("trainers");
    }
}
