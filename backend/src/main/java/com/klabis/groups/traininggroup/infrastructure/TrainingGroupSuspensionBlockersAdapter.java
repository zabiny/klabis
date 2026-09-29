package com.klabis.groups.traininggroup.infrastructure;

import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.OwnedGroup;
import com.klabis.members.application.MemberOwnedGroupsPort;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.stereotype.Component;

import java.util.List;

@SecondaryAdapter
@Component
class TrainingGroupSuspensionBlockersAdapter implements MemberOwnedGroupsPort {

    private final TrainingGroupRepository repository;

    TrainingGroupSuspensionBlockersAdapter(TrainingGroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OwnedGroup> findGroupsBlockingSuspension(MemberId memberId) {
        return repository.findAll(TrainingGroupFilter.all().withTrainerIs(memberId)).stream()
                .filter(group -> group.isLastTrainer(memberId))
                .map(TrainingGroupSuspensionBlockersAdapter::toOwnedGroup)
                .toList();
    }

    private static OwnedGroup toOwnedGroup(TrainingGroup group) {
        return new OwnedGroup(group.getId().uuid().toString(), group.getName(), TrainingGroup.TYPE_DISCRIMINATOR);
    }
}
