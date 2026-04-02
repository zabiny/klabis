package com.klabis.usergroups.infrastructure.listeners;

import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberId;
import com.klabis.usergroups.MemberAssignedToTrainingGroupEvent;
import com.klabis.usergroups.domain.TrainingGroup;
import com.klabis.usergroups.domain.UserGroupRepository;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@PrimaryAdapter
class MemberCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(MemberCreatedListener.class);

    private final UserGroupRepository userGroupRepository;
    private final ApplicationEventPublisher eventPublisher;

    MemberCreatedListener(UserGroupRepository userGroupRepository, ApplicationEventPublisher eventPublisher) {
        this.userGroupRepository = userGroupRepository;
        this.eventPublisher = eventPublisher;
    }

    @ApplicationModuleListener
    void onMemberCreated(MemberCreatedEvent event) {
        log.debug("Processing MemberCreatedEvent for member: {}", event.memberId());

        if (event.dateOfBirth() == null) {
            log.debug("Skipping training group assignment — member {} has no date of birth", event.memberId());
            return;
        }

        MemberId memberId = event.memberId();
        findMatchingTrainingGroup(event).ifPresent(trainingGroup -> {
            trainingGroup.addMember(memberId);
            userGroupRepository.save(trainingGroup);

            MemberAssignedToTrainingGroupEvent assignedEvent = MemberAssignedToTrainingGroupEvent.of(
                    memberId,
                    trainingGroup.getId(),
                    trainingGroup.getName()
            );
            eventPublisher.publishEvent(assignedEvent);

            log.info("Auto-assigned member {} to training group '{}' ({})",
                    memberId, trainingGroup.getName(), trainingGroup.getId());
        });
    }

    private Optional<TrainingGroup> findMatchingTrainingGroup(MemberCreatedEvent event) {
        List<TrainingGroup> trainingGroups = userGroupRepository.findAllTrainingGroups();
        return trainingGroups.stream()
                .filter(group -> group.matchesByAge(event.dateOfBirth()))
                .findFirst();
    }
}
