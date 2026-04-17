package com.klabis.groups.traininggroup.application;

import com.klabis.common.patch.PatchField;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.util.Set;

@RecordBuilder
public record UpdateTrainingGroupCommand(
        PatchField<String> name,
        PatchField<AgeRange> ageRange,
        PatchField<Set<MemberId>> trainers
) {
    public UpdateTrainingGroupCommand {
        Assert.notNull(name, "name patch field is required");
        Assert.notNull(ageRange, "ageRange patch field is required");
        Assert.notNull(trainers, "trainers patch field is required");
    }
}
