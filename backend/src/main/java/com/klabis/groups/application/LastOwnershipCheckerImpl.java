package com.klabis.groups.application;

import com.klabis.groups.common.domain.FamilyGroupFilter;
import com.klabis.groups.common.domain.MembersGroupFilter;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.groups.membersgroup.domain.MembersGroup;
import com.klabis.groups.membersgroup.domain.MembersGroupRepository;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspensionRequestedEvent;
import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@SecondaryAdapter
@Component
public class LastOwnershipCheckerImpl {

    private final FamilyGroupRepository familyGroupRepository;
    private final MembersGroupRepository membersGroupRepository;
    private final TrainingGroupRepository trainingGroupRepository;

    LastOwnershipCheckerImpl(FamilyGroupRepository familyGroupRepository,
                             MembersGroupRepository membersGroupRepository,
                             TrainingGroupRepository trainingGroupRepository) {
        this.familyGroupRepository = familyGroupRepository;
        this.membersGroupRepository = membersGroupRepository;
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @EventListener
    @Transactional(readOnly = true)
    void onMemberSuspensionRequested(MemberSuspensionRequestedEvent event) {
        MemberId memberId = event.memberId();

        familyGroupRepository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId))
                .filter(group -> group.isLastParent(memberId))
                .ifPresent(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FamilyGroup.TYPE_DISCRIMINATOR));

        membersGroupRepository.findAll(MembersGroupFilter.all().withOwnerOrMemberIs(memberId)).stream()
                .filter(group -> group.isLastOwner(memberId))
                .forEach(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        MembersGroup.TYPE_DISCRIMINATOR));

        trainingGroupRepository.findAll(TrainingGroupFilter.all().withTrainerIs(memberId)).stream()
                .filter(group -> group.isLastTrainer(memberId))
                .forEach(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        TrainingGroup.TYPE_DISCRIMINATOR));
    }
}
