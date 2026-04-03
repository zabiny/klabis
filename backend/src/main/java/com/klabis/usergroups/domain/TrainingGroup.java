package com.klabis.usergroups.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.time.Period;
import java.util.Set;
import java.util.UUID;

public class TrainingGroup extends UserGroup {

    public static final String TYPE_DISCRIMINATOR = "TRAINING";

    private AgeRange ageRange;

    private TrainingGroup(UserGroupId id, String name, Set<MemberId> owners, Set<GroupMembership> members,
                          AgeRange ageRange) {
        super(id, name, owners, members);
        Assert.notNull(ageRange, "AgeRange is required for TrainingGroup");
        this.ageRange = ageRange;
    }

    @Override
    public String typeDiscriminator() {
        return TYPE_DISCRIMINATOR;
    }

    public record CreateTrainingGroup(String name, MemberId owner, AgeRange ageRange) {
        public CreateTrainingGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(owner, "Owner is required");
            Assert.notNull(ageRange, "AgeRange is required");
        }
    }

    public static TrainingGroup create(CreateTrainingGroup command) {
        UserGroupId id = new UserGroupId(UUID.randomUUID());
        return new TrainingGroup(id, command.name(), Set.of(command.owner()), Set.of(), command.ageRange());
    }

    public static TrainingGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                            Set<GroupMembership> members, AgeRange ageRange,
                                            AuditMetadata auditMetadata) {
        TrainingGroup group = new TrainingGroup(id, name, owners, members, ageRange);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    public AgeRange getAgeRange() {
        return ageRange;
    }

    public void updateAgeRange(AgeRange newAgeRange) {
        Assert.notNull(newAgeRange, "AgeRange is required");
        this.ageRange = newAgeRange;
    }

    public boolean matchesByAge(LocalDate dateOfBirth) {
        Assert.notNull(dateOfBirth, "Date of birth is required");
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return ageRange.includes(age);
    }
}
