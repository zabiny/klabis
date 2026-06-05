package com.klabis.groups.traininggroup.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.groups.MemberAssignedToTrainingGroupEvent;
import com.klabis.groups.common.domain.CannotRemoveLastOwnerException;
import com.klabis.groups.common.domain.GroupMembership;
import com.klabis.groups.common.domain.MemberGroup;
import com.klabis.groups.traininggroup.TrainingGroupId;
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

@AggregateRoot
public class TrainingGroup extends MemberGroup<TrainingGroup, TrainingGroupId> {

    public static final String TYPE_DISCRIMINATOR = "TRAINING";

    @Identity
    private final TrainingGroupId id;
    private AgeRange ageRange;

    private TrainingGroup(TrainingGroupId id, String name, Set<MemberId> trainers, Set<GroupMembership> members, AgeRange ageRange) {
        super(name, trainers, members);
        Assert.notNull(id, "TrainingGroupId is required");
        Assert.notNull(ageRange, "AgeRange is required");
        this.id = id;
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
        return new TrainingGroup(id, command.name(), Set.of(command.trainer()), Set.of(), command.ageRange());
    }

    public static TrainingGroup reconstruct(TrainingGroupId id, String name, Set<MemberId> trainers,
                                            Set<GroupMembership> members, AgeRange ageRange,
                                            AuditMetadata auditMetadata) {
        TrainingGroup group = new TrainingGroup(id, name, trainers, members, ageRange);
        group.updateAuditMetadata(auditMetadata);
        return group;
    }

    @Override
    public TrainingGroupId getId() {
        return id;
    }

    public AgeRange getAgeRange() {
        return ageRange;
    }

    public Set<MemberId> getTrainers() {
        return getOwners();
    }

    public boolean hasTrainer(MemberId memberId) {
        return isOwner(memberId);
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
        if (trainers == null || trainers.isEmpty()) {
            throw new CannotRemoveLastOwnerException();
        }
        Set<MemberId> current = Set.copyOf(getTrainers());
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

    public void assignEligibleMember(MemberId memberId) {
        addMember(memberId);
        registerEvent(new MemberAssignedToTrainingGroupEvent(memberId, id, getName(), Instant.now()));
    }

    public void removeMember(MemberId memberId) {
        super.removeMember(memberId);
    }

    public void updateAgeRange(AgeRange newAgeRange) {
        Assert.notNull(newAgeRange, "AgeRange is required");
        this.ageRange = newAgeRange;
    }

    public boolean isLastTrainer(MemberId trainerId) {
        return isLastOwner(trainerId);
    }

    public boolean matchesByAge(LocalDate dateOfBirth) {
        Assert.notNull(dateOfBirth, "Date of birth is required");
        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        return ageRange.includes(age);
    }
}
