package com.klabis.members.traininggroup.application;

import com.klabis.common.patch.PatchField;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.util.Set;

@RecordBuilder
public record UpdateTrainingGroupCommand(
        PatchField<String> name,
        PatchField<Integer> minAge,
        PatchField<Integer> maxAge,
        PatchField<Set<MemberId>> trainers
) {
    public UpdateTrainingGroupCommand {
        Assert.notNull(name, "name patch field is required");
        Assert.notNull(minAge, "minAge patch field is required");
        Assert.notNull(maxAge, "maxAge patch field is required");
        Assert.notNull(trainers, "trainers patch field is required");
        Assert.isTrue(
                minAge.isProvided() == maxAge.isProvided(),
                "minAge and maxAge must both be provided or both be absent"
        );
    }
}
