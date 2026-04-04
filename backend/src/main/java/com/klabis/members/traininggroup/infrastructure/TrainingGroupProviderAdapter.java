package com.klabis.members.traininggroup.infrastructure;

import com.klabis.members.MemberId;
import com.klabis.members.TrainingGroupProvider;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@SecondaryAdapter
@Component
class TrainingGroupProviderAdapter implements TrainingGroupProvider {

    private final TrainingGroupRepository trainingGroupRepository;

    TrainingGroupProviderAdapter(TrainingGroupRepository trainingGroupRepository) {
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<TrainingGroupData> findTrainingGroupForMember(MemberId memberId) {
        return trainingGroupRepository.findGroupForMember(memberId)
                .map(group -> new TrainingGroupData(group.getId().value()));
    }
}
