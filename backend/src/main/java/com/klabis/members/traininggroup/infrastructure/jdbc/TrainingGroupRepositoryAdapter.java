package com.klabis.members.traininggroup.infrastructure.jdbc;

import com.klabis.members.MemberId;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupId;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SecondaryAdapter
@Repository
class TrainingGroupRepositoryAdapter implements TrainingGroupRepository {

    private final TrainingGroupJdbcRepository jdbcRepository;

    TrainingGroupRepositoryAdapter(TrainingGroupJdbcRepository jdbcRepository) {
        this.jdbcRepository = jdbcRepository;
    }

    @Override
    public TrainingGroup save(TrainingGroup group) {
        return jdbcRepository.save(TrainingGroupMemento.from(group)).toTrainingGroup();
    }

    @Override
    public Optional<TrainingGroup> findById(TrainingGroupId id) {
        return jdbcRepository.findById(id.value()).map(TrainingGroupMemento::toTrainingGroup);
    }

    @Override
    public Optional<TrainingGroup> findGroupForMember(MemberId memberId) {
        return jdbcRepository.findByMemberId(memberId.uuid()).map(TrainingGroupMemento::toTrainingGroup);
    }

    @Override
    public List<TrainingGroup> findGroupsForTrainer(MemberId trainerId) {
        return jdbcRepository.findByTrainerId(trainerId.uuid()).stream()
                .map(TrainingGroupMemento::toTrainingGroup)
                .toList();
    }

    @Override
    public List<TrainingGroup> findAll() {
        List<TrainingGroup> result = new ArrayList<>();
        jdbcRepository.findAll().forEach(m -> result.add(m.toTrainingGroup()));
        return result;
    }

    @Override
    public void delete(TrainingGroupId id) {
        jdbcRepository.deleteById(id.value());
    }
}
