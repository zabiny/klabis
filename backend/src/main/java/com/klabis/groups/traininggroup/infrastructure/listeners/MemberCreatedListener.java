package com.klabis.groups.traininggroup.infrastructure.listeners;

import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberCreatedEvent;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.List;

@PrimaryAdapter
@Component
class MemberCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(MemberCreatedListener.class);

    private final TrainingGroupRepository trainingGroupRepository;

    MemberCreatedListener(TrainingGroupRepository trainingGroupRepository) {
        this.trainingGroupRepository = trainingGroupRepository;
    }

    @ApplicationModuleListener
    void on(MemberCreatedEvent event) {
        List<TrainingGroup> matchingGroups = trainingGroupRepository.findAll(TrainingGroupFilter.all()).stream()
                .filter(group -> group.matchesByAge(event.dateOfBirth()))
                .toList();

        if (matchingGroups.isEmpty()) {
            return;
        }

        log.info("Auto-assigning new member {} to {} training group(s)", event.memberId(), matchingGroups.size());

        matchingGroups.forEach(group -> {
            group.assignEligibleMember(event.memberId());
            trainingGroupRepository.save(group);
        });
    }
}
