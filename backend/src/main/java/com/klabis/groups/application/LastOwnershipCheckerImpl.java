package com.klabis.groups.application;

import com.klabis.groups.common.domain.FamilyGroupFilter;
import com.klabis.groups.common.domain.FreeGroupFilter;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspensionRequestedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@PrimaryAdapter
@Component
public class LastOwnershipCheckerImpl {

    private final FamilyGroupRepository familyGroupRepository;
    private final FreeGroupRepository freeGroupRepository;
    private final TrainingGroupRepository trainingGroupRepository;

    LastOwnershipCheckerImpl(FamilyGroupRepository familyGroupRepository,
                             FreeGroupRepository freeGroupRepository,
                             TrainingGroupRepository trainingGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
        this.freeGroupRepository = freeGroupRepository;
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @EventListener
    void onMemberSuspensionRequested(MemberSuspensionRequestedEvent event) {
        MemberId memberId = event.memberId();

        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId))
                .filter(group -> group.isLastParent(memberId))
                .ifPresent(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FamilyGroup.TYPE_DISCRIMINATOR));

        freeGroupRepository.findAll(FreeGroupFilter.all().withOwnerOrMemberIs(memberId)).stream()
                .filter(group -> group.isLastOwner(memberId))
                .forEach(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FreeGroup.TYPE_DISCRIMINATOR));

        trainingGroupRepository.findAll(TrainingGroupFilter.all().withTrainerIs(memberId)).stream()
                .filter(group -> group.isLastTrainer(memberId))
                .forEach(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        TrainingGroup.TYPE_DISCRIMINATOR));
    }
}
