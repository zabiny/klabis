package com.klabis.membershipfees.application;

import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class AdminFeeAssignmentService implements AdminFeeAssignmentPort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeSelectionCampaignRepository publicationRepository;
    private final ApplicationEventPublisher eventPublisher;

    AdminFeeAssignmentService(MembershipFeeGroupRepository groupRepository,
                              FeeSelectionCampaignRepository publicationRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @Override
    public void assignLevel(AssignFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        publicationRepository.findByYear(command.year())
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(command.year()));

        groupRepository.findByMemberAndYear(command.targetMemberId(), command.year())
                .filter(currentGroup -> !currentGroup.getId().equals(command.groupId()))
                .ifPresent(currentGroup -> {
                    currentGroup.removeMember(command.targetMemberId());
                    groupRepository.save(currentGroup);
                });

        targetGroup.addMember(command.targetMemberId(), LocalDate.now(), AssignmentSource.ADMIN_ASSIGNMENT, command.adminId());
        groupRepository.save(targetGroup);

        // Always publish — listener's unblockMember() is idempotent (no-op when member is not blocked)
        eventPublisher.publishEvent(new MemberFeeSelectionResolvedEvent(command.targetMemberId(), command.year()));
    }
}
