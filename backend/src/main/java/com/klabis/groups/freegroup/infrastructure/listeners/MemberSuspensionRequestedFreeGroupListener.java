package com.klabis.groups.freegroup.infrastructure.listeners;

import com.klabis.groups.freegroup.domain.FreeGroup;
import com.klabis.groups.freegroup.domain.FreeGroupFilter;
import com.klabis.groups.freegroup.domain.FreeGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.MemberSuspensionRequestedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

// TODO: refactor - this should be some kind of "callbac" (port) in members implemented from here as it passes information back
@PrimaryAdapter
@Component
class MemberSuspensionRequestedFreeGroupListener {

    private final FreeGroupRepository repository;

    MemberSuspensionRequestedFreeGroupListener(FreeGroupRepository repository) {
        this.repository = repository;
    }

    @EventListener
    void on(MemberSuspensionRequestedEvent event) {
        MemberId memberId = event.memberId();

        repository.findAll(FreeGroupFilter.all().withOwnerOrMemberIs(memberId)).stream()
                .filter(group -> group.isLastOwner(memberId))
                .forEach(group -> event.addBlockingGroup(
                        group.getId().uuid().toString(),
                        group.getName(),
                        FreeGroup.TYPE_DISCRIMINATOR));
    }
}
