package com.klabis.usergroups.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.MemberId;
import com.klabis.usergroups.MemberAssignedToTrainingGroupEvent;
import com.klabis.usergroups.UserGroupId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
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

    @RecordBuilder
    public record CreateTrainingGroup(String name, MemberId trainer, AgeRange ageRange) {
        public CreateTrainingGroup {
            Assert.hasText(name, "Group name is required");
            Assert.notNull(trainer, "Trainer is required");
            Assert.notNull(ageRange, "AgeRange is required");
        }
    }

    public static TrainingGroup create(CreateTrainingGroup command) {
        UserGroupId id = new UserGroupId(UUID.randomUUID());
        return new TrainingGroup(id, command.name(), Set.of(command.trainer()), Set.of(), command.ageRange());
    }

    public static TrainingGroup reconstruct(UserGroupId id, String name, Set<MemberId> owners,
                                            Set<GroupMembership> members, AgeRange ageRange,
                                            AuditMetadata auditMetadata) {
        TrainingGroup group = new TrainingGroup(id, name, owners, members, ageRange);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @RecordBuilder
    public record UpdateAgeRange(MemberId requestingMember, AgeRange newAgeRange) {}

    public Set<MemberId> getTrainers() {
        return getOwners();
    }

    public void addTrainer(MemberId trainer) {
        Assert.notNull(trainer, "Trainer MemberId is required");
        addOwner(trainer);
    }

    public void removeTrainer(MemberId trainer) {
        Assert.notNull(trainer, "Trainer MemberId is required");
        removeOwner(trainer);
    }

    public void replaceTrainers(Set<MemberId> trainers) {
        Assert.notEmpty(trainers, "At least one trainer is required");
        Set<MemberId> current = new HashSet<>(getOwners());
        for (MemberId toAdd : trainers) {
            if (!current.contains(toAdd)) {
                addOwner(toAdd);
            }
        }
        for (MemberId toRemove : current) {
            if (!trainers.contains(toRemove)) {
                removeOwner(toRemove);
            }
        }
    }

    @Override
    public void addMember(AddMember command) {
        requireOwner(command.requestingMember());
        addMemberInternal(command.memberToAdd());
        registerEvent(new MemberAssignedToTrainingGroupEvent(
                command.memberToAdd(), getId(), getName(), Instant.now()));
    }

    public void assignEligibleMember(MemberId memberId) {
        addMemberInternal(memberId);
        registerEvent(new MemberAssignedToTrainingGroupEvent(memberId, getId(), getName(), Instant.now()));
    }

    public AgeRange getAgeRange() {
        return ageRange;
    }

    public void updateAgeRange(UpdateAgeRange command) {
        requireOwner(command.requestingMember());
        updateAgeRange(command.newAgeRange());
    }

    public void updateAgeRange(AgeRange newAgeRange) {
        Assert.notNull(newAgeRange, "AgeRange is required");
        this.ageRange = newAgeRange;
    }

    public void removeMember(MemberId memberId) {
        super.removeMember(memberId);
    }

    public boolean matchesByAge(LocalDate dateOfBirth) {
        Assert.notNull(dateOfBirth, "Date of birth is required");
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return ageRange.includes(age);
    }
}
