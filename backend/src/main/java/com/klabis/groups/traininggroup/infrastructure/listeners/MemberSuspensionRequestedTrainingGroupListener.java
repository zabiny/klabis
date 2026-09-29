package com.klabis.groups.traininggroup.infrastructure.listeners;

import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspensionRequestedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
// TODO: refactor - this should be some kind of "callbac" (port) in members implemented from here as it passes information back
@PrimaryAdapter
@Component
class MemberSuspensionRequestedTrainingGroupListener {

    private final TrainingGroupRepository repository;

    MemberSuspensionRequestedTrainingGroupListener(TrainingGroupRepository repository) {
        this.repository = repository;
    }

    @EventListener
    void on(MemberSuspensionRequestedEvent event) {
        MemberId memberId = event.memberId();

        repository.findAll(TrainingGroupFilter.all().withTrainerIs(memberId)).stream()
                .filter(group -> group.isLastTrainer(memberId))
                .forEach(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        TrainingGroup.TYPE_DISCRIMINATOR));
    }
}
