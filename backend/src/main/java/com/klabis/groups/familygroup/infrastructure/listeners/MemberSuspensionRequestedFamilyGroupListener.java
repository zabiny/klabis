package com.klabis.groups.familygroup.infrastructure.listeners;

import com.klabis.groups.familygroup.domain.FamilyGroup;
import com.klabis.groups.familygroup.domain.FamilyGroupFilter;
import com.klabis.groups.familygroup.domain.FamilyGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspensionRequestedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// TODO: refactor - this should be some kind of "callbac" (port) in members implemented from here as it passes information back
@PrimaryAdapter
@Component
class MemberSuspensionRequestedFamilyGroupListener {

    private final FamilyGroupRepository repository;

    MemberSuspensionRequestedFamilyGroupListener(FamilyGroupRepository repository) {
        this.repository = repository;
    }

    @EventListener
    void on(MemberSuspensionRequestedEvent event) {
        MemberId memberId = event.memberId();

        repository.findOne(FamilyGroupFilter.all().withMemberOrParentIs(memberId))
                .filter(group -> group.isLastParent(memberId))
                .ifPresent(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FamilyGroup.TYPE_DISCRIMINATOR));
    }
}
