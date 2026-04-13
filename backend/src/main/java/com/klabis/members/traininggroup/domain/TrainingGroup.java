package com.klabis.members.traininggroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.usergroup.GroupMembership;
import com.klabis.common.usergroup.UserGroup;
import com.klabis.members.MemberAssignedToTrainingGroupEvent;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@AggregateRoot
public class TrainingGroup extends KlabisAggregateRoot<TrainingGroup, TrainingGroupId> {

    public static final String TYPE_DISCRIMINATOR = "TRAINING";

    @Identity
    private final TrainingGroupId id;
    private final UserGroup userGroup;
    private AgeRange ageRange;

    private TrainingGroup(TrainingGroupId id, UserGroup userGroup, AgeRange ageRange) {
        Assert.notNull(id, "TrainingGroupId is required");
        Assert.notNull(userGroup, "UserGroup is required");
        Assert.notNull(ageRange, "AgeRange is required");
        this.id = id;
        this.userGroup = userGroup;
        this.ageRange = ageRange;
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
        TrainingGroupId id = new TrainingGroupId(UUID.randomUUID());
        // Trainers are owners but not automatically members — membership is managed separately via assignEligibleMember
        UserGroup userGroup = UserGroup.reconstruct(command.name(), Set.of(command.trainer().toUserId()), Set.of());
        return new TrainingGroup(id, userGroup, command.ageRange());
    }

    public static TrainingGroup reconstruct(TrainingGroupId id, String name, Set<MemberId> trainers,
                                            Set<GroupMembership> members, AgeRange ageRange,
                                            AuditMetadata auditMetadata) {
        Set<UserId> ownerUserIds = trainers.stream()
                .map(MemberId::toUserId)
                .collect(Collectors.toSet());
        UserGroup userGroup = UserGroup.reconstruct(name, ownerUserIds, members);
        TrainingGroup group = new TrainingGroup(id, userGroup, ageRange);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public TrainingGroupId getId() {
        return id;
    }

    public String getName() {
        return userGroup.getName();
    }

    public AgeRange getAgeRange() {
        return ageRange;
    }

    public Set<MemberId> getTrainers() {
        return userGroup.getOwners().stream()
                .map(MemberId::fromUserId)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<GroupMembership> getMembers() {
        return userGroup.getMembers();
    }

    public boolean hasMember(MemberId memberId) {
        return userGroup.hasMember(memberId.toUserId());
    }

    public boolean hasTrainer(MemberId memberId) {
        return userGroup.isOwner(memberId.toUserId());
    }

    public void rename(String newName) {
        userGroup.rename(newName);
    }

    public void addTrainer(MemberId trainer) {
        Assert.notNull(trainer, "Trainer MemberId is required");
        userGroup.addOwner(trainer.toUserId());
    }

    public void removeTrainer(MemberId trainer) {
        Assert.notNull(trainer, "Trainer MemberId is required");
        userGroup.removeOwner(trainer.toUserId());
    }

    public void replaceTrainers(Set<MemberId> trainers) {
        Assert.notEmpty(trainers, "At least one trainer is required");
        Set<MemberId> current = getTrainers();
        for (MemberId toAdd : trainers) {
            if (!current.contains(toAdd)) {
                userGroup.addOwner(toAdd.toUserId());
            }
        }
        for (MemberId toRemove : current) {
            if (!trainers.contains(toRemove)) {
                userGroup.removeOwner(toRemove.toUserId());
            }
        }
    }

    public void addMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        userGroup.addMember(memberId.toUserId());
        registerEvent(new MemberAssignedToTrainingGroupEvent(memberId, id, getName(), Instant.now()));
    }

    public void removeMember(MemberId memberId) {
        Assert.notNull(memberId, "MemberId is required");
        userGroup.removeMember(memberId.toUserId());
    }

    public void assignEligibleMember(MemberId memberId) {
        userGroup.addMember(memberId.toUserId());
        registerEvent(new MemberAssignedToTrainingGroupEvent(memberId, id, getName(), Instant.now()));
    }

    public void updateAgeRange(AgeRange newAgeRange) {
        Assert.notNull(newAgeRange, "AgeRange is required");
        this.ageRange = newAgeRange;
    }

    public boolean isLastTrainer(MemberId trainerId) {
        return userGroup.isLastOwner(trainerId.toUserId());
    }

    public boolean matchesByAge(LocalDate dateOfBirth) {
        Assert.notNull(dateOfBirth, "Date of birth is required");
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return ageRange.includes(age);
    }
}
