package com.klabis.groups.traininggroup.infrastructure;

import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.groups.traininggroup.TrainingGroupId;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TrainingGroupRelationshipSource")
class TrainingGroupRelationshipSourceTest {

    private final GroupsStub groups = new GroupsStub();
    private final TrainingGroupRelationshipSource source = new TrainingGroupRelationshipSource(groups);

    private static final MemberId TRAINER = memberId();
    private static final MemberId OTHER_TRAINER = memberId();

    private static MemberId memberId() {
        return new MemberId(UUID.randomUUID());
    }

    private TrainingGroup groupOf(MemberId trainer) {
        TrainingGroup group = TrainingGroup.create(
                new TrainingGroup.CreateTrainingGroup("Žáci", trainer, new AgeRange(10, 12)));
        groups.save(group);
        return group;
    }

    @Test
    @DisplayName("grants a trainer group ownership over the groups they train and over no other")
    void shouldGrantTrainerOverTrainedGroupsOnly() {
        TrainingGroup trained = groupOf(TRAINER);
        TrainingGroup coTrained = groupOf(OTHER_TRAINER);
        coTrained.addTrainer(TRAINER);
        groupOf(OTHER_TRAINER);

        var grants = source.grantsOf(TRAINER.toUserId());

        assertThat(grants).containsOnlyKeys(Authority.GROUPS_OWNER);
        assertThat(grants.get(Authority.GROUPS_OWNER)).containsExactlyInAnyOrder(
                TargetRef.group(trained.getId().uuid()), TargetRef.group(coTrained.getId().uuid()));
    }

    @Test
    @DisplayName("grants a trainee nothing")
    void shouldNotGrantTrainee() {
        MemberId trainee = memberId();
        TrainingGroup group = groupOf(TRAINER);
        group.addTrainee(trainee);

        assertThat(source.grantsOf(trainee.toUserId())).isEmpty();
    }

    @Test
    @DisplayName("grants a removed trainer nothing")
    void shouldGrantNothingToRemovedTrainer() {
        TrainingGroup group = groupOf(TRAINER);
        group.addTrainer(OTHER_TRAINER);
        group.removeTrainer(TRAINER);

        assertThat(source.grantsOf(TRAINER.toUserId())).isEmpty();
        assertThat(source.grantsOf(OTHER_TRAINER.toUserId())).isNotEmpty();
    }

    @Test
    @DisplayName("grants nothing to a user who trains nothing")
    void shouldGrantNothingToNonTrainer() {
        groupOf(TRAINER);

        assertThat(source.grantsOf(new UserId(UUID.randomUUID()))).isEmpty();
    }

    private static class GroupsStub implements TrainingGroupRepository {

        private final List<TrainingGroup> stored = new ArrayList<>();

        @Override
        public TrainingGroup save(TrainingGroup group) {
            stored.add(group);
            return group;
        }

        @Override
        public Optional<TrainingGroup> findById(TrainingGroupId id) {
            return stored.stream().filter(group -> group.getId().equals(id)).findFirst();
        }

        @Override
        public List<TrainingGroup> findAll(TrainingGroupFilter filter) {
            return stored.stream()
                    .filter(group -> filter.trainerIs() == null || group.hasTrainer(filter.trainerIs()))
                    .toList();
        }

        @Override
        public Optional<TrainingGroup> findOne(TrainingGroupFilter filter) {
            return findAll(filter).stream().findFirst();
        }

        @Override
        public boolean exists(TrainingGroupFilter filter) {
            return !findAll(filter).isEmpty();
        }

        @Override
        public void delete(TrainingGroupId id) {
            stored.removeIf(group -> group.getId().equals(id));
        }
    }
}
