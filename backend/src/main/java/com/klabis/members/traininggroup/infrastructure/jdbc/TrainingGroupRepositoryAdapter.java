package com.klabis.members.traininggroup.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.groups.infrastructure.jdbc.GroupJdbcRepository;
import com.klabis.members.groups.infrastructure.jdbc.GroupMemento;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.jmolecules.ddd.annotation.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SecondaryAdapter
@Repository
class TrainingGroupRepositoryAdapter implements TrainingGroupRepository {

    private final GroupJdbcRepository jdbcRepository;

    TrainingGroupRepositoryAdapter(GroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public TrainingGroup save(TrainingGroup group) {
        return jdbcRepository.save(GroupMemento.fromTrainingGroup(group)).toTrainingGroup();
    }

    @Override
    public Optional<TrainingGroup> findById(TrainingGroupId id) {
        return jdbcRepository.findByIdAndType(id.value(), TrainingGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toTrainingGroup);
    }

    @Override
    public Optional<TrainingGroup> findGroupForMember(MemberId memberId) {
        return jdbcRepository.findByMemberIdAndType(memberId.value(), TrainingGroup.TYPE_DISCRIMINATOR)
                .map(GroupMemento::toTrainingGroup);
    }

    @Override
    public List<TrainingGroup> findGroupsForTrainer(MemberId trainerId) {
        return jdbcRepository.findByTrainerIdAndType(trainerId.value(), TrainingGroup.TYPE_DISCRIMINATOR)
                .stream().map(GroupMemento::toTrainingGroup).toList();
    }

    @Override
    public List<TrainingGroup> findAll() {
        return jdbcRepository.findAllByType(TrainingGroup.TYPE_DISCRIMINATOR)
                .stream().map(GroupMemento::toTrainingGroup).toList();
    }

    @Override
    public boolean existsOverlappingAgeRange(int minAge, int maxAge, TrainingGroupId excludeId) {
        UUID excludeUuid = excludeId != null ? excludeId.value() : null;
        return jdbcRepository.existsOverlappingAgeRangeForType(minAge, maxAge, excludeUuid, TrainingGroup.TYPE_DISCRIMINATOR);
    }

    @Override
    public void delete(TrainingGroupId id) {
        jdbcRepository.deleteByIdAndType(id.value(), TrainingGroup.TYPE_DISCRIMINATOR);
    }
}
