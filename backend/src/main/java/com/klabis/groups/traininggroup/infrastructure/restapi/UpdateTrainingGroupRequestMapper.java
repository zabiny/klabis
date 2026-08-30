package com.klabis.groups.traininggroup.infrastructure.restapi;

import com.klabis.groups.infrastructure.restapi.AgeRangeRequest;
import com.klabis.groups.infrastructure.restapi.UpdateTrainingGroupRequest;
import com.klabis.groups.traininggroup.application.UpdateTrainingGroupCommand;
import com.klabis.groups.traininggroup.application.UpdateTrainingGroupCommandBuilder;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.members.MemberId;
import org.openapitools.jackson.nullable.JsonNullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Applies a PATCH {@link UpdateTrainingGroupRequest} onto a fully pre-filled
 * {@link UpdateTrainingGroupCommand} baseline (from
 * {@link com.klabis.groups.traininggroup.application.TrainingGroupManagementPort#prefilledUpdateCommand}).
 * <p>
 * The three PATCH states are resolved here so the application command never sees {@link JsonNullable}:
 * an <em>undefined</em> field leaves the baseline value in place; a <em>present</em> value overrides
 * it. None of the three fields has a cleared state, so a present-null falls through to the command's
 * own {@code Assert} and surfaces as a 400.
 */
class UpdateTrainingGroupRequestMapper {

    private UpdateTrainingGroupRequestMapper() {}

    static UpdateTrainingGroupCommand toCommand(UpdateTrainingGroupRequest request, UpdateTrainingGroupCommand prefilled) {
        var b = UpdateTrainingGroupCommandBuilder.builder(prefilled);
        overlayValue(request.name(), b::name);
        overlayValue(request.ageRange(), v -> b.ageRange(toAgeRange(v)));
        overlayValue(request.trainers(), v -> b.trainers(toMemberIds(v)));
        return b.build();
    }

    /** Present non-null → override the baseline; undefined or present-null → leave the baseline. */
    private static <T> void overlayValue(JsonNullable<T> field, Consumer<T> apply) {
        field.ifPresent(value -> {
            if (value != null) {
                apply.accept(value);
            }
        });
    }

    private static AgeRange toAgeRange(AgeRangeRequest request) {
        return new AgeRange(request.minAge(), request.maxAge());
    }

    private static java.util.Set<MemberId> toMemberIds(List<UUID> trainers) {
        return trainers.stream().map(MemberId::new).collect(Collectors.toSet());
    }
}
