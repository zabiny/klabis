package com.klabis.membershipfees.application;

import com.klabis.events.application.MemberRegistrationSanctionPort;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeYearPublicationRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class AdminFeeAssignmentService implements AdminFeeAssignmentPort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeYearPublicationRepository publicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MemberRegistrationSanctionPort sanctionPort;

    AdminFeeAssignmentService(MembershipFeeGroupRepository groupRepository,
                              FeeYearPublicationRepository publicationRepository,
                              ApplicationEventPublisher eventPublisher,
                              MemberRegistrationSanctionPort sanctionPort) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
        this.eventPublisher = eventPublisher;
        this.sanctionPort = sanctionPort;
    }

    @Transactional
    @Override
    public void assignLevel(AssignFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        publicationRepository.findByYear(command.year())
                .orElseThrow(() -> new FeeYearPublicationNotFoundException(command.year()));

        groupRepository.findByMemberAndYear(command.targetMemberId(), command.year())
                .filter(currentGroup -> !currentGroup.getId().equals(command.groupId()))
                .ifPresent(currentGroup -> {
                    currentGroup.removeMember(command.targetMemberId());
                    groupRepository.save(currentGroup);
                });

        targetGroup.addMember(command.targetMemberId(), LocalDate.now(), AssignmentSource.ADMIN_ASSIGNMENT, command.adminId());
        groupRepository.save(targetGroup);

        if (sanctionPort.isMemberBlocked(command.targetMemberId())) {
            eventPublisher.publishEvent(new MemberFeeSelectionResolvedEvent(command.targetMemberId(), command.year()));
        }
    }
}
