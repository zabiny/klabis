package com.klabis.usergroups.infrastructure.listeners;

import com.klabis.members.MemberCreatedEvent;
import com.klabis.usergroups.domain.GroupFilter;
import com.klabis.usergroups.domain.GroupType;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@PrimaryAdapter
public class MemberCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(MemberCreatedListener.class);

    private final UserGroupRepository userGroupRepository;

    MemberCreatedListener(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @ApplicationModuleListener
    void onMemberCreated(MemberCreatedEvent event) {
        log.debug("Processing MemberCreatedEvent for memberId={}", event.memberId());

        userGroupRepository.findAll(GroupFilter.byType(GroupType.TRAINING)).stream()
                .map(group -> (TrainingGroup) group)
                .filter(group -> group.matchesByAge(event.dateOfBirth()))
                .findFirst()
                .ifPresent(group -> assignMemberToGroup(group, event));
    }

    private void assignMemberToGroup(TrainingGroup group, MemberCreatedEvent event) {
        group.assignEligibleMember(event.memberId());
        userGroupRepository.save(group);

        log.info("Auto-assigned member {} to training group '{}' ({})",
                event.memberId(), group.getName(), group.getId());
    }
}
